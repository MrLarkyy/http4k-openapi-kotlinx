package io.github.mrlarkyy.http4k.openapi.http4k

import io.github.mrlarkyy.http4k.openapi.http4k.internal.DefinitionRegistry
import io.github.mrlarkyy.http4k.openapi.http4k.internal.DescriptorWalker
import io.github.mrlarkyy.http4k.openapi.http4k.internal.JsonSchemaFactory
import io.github.mrlarkyy.http4k.openapi.http4k.internal.SchemaNames
import io.github.mrlarkyy.http4k.openapi.http4k.internal.WalkedSchema
import io.github.mrlarkyy.http4k.openapi.http4k.internal.schemaRef
import io.github.mrlarkyy.http4k.openapi.http4k.internal.toJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializerOrNull
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.v3.Api
import org.http4k.contract.openapi.v3.OpenApi3ApiRenderer
import org.http4k.format.ConfigurableKotlinxSerialization
import org.http4k.format.KotlinxSerialization
import java.util.concurrent.ConcurrentHashMap

/**
 * An http4k [ApiRenderer] that builds the schemas of `@Serializable` request and response examples from their
 * kotlinx.serialization descriptors and puts them into `components/schemas`.
 *
 * ```
 * val renderer = OpenApi3(apiInfo, KotlinxSerialization, apiRenderer = KotlinxApiRenderer(KotlinxSerialization))
 * ```
 *
 * http4k asks for a schema of the example of each body, so a body needs an example, for instance
 * `receiving(lens to Item("a"))`. A schema is named after the serial name of the type without its package, so
 * `@SerialName("shop.Item")` gives `Item`; the same type in several routes gives one schema, and two types that
 * would share a name fail instead of silently overwriting each other.
 *
 * Supported for now: classes and objects whose properties are non-null, non-optional strings, booleans, ints, longs,
 * doubles or further such classes (also recursive ones). Every other property kind, nullable or optional properties and
 * a [json] with a naming strategy fail with an exception that names the serial name and the property. Examples that are
 * JSON elements, or whose type is not a class or object, are rendered by http4k as without this renderer. An example of
 * a sealed subtype is rendered as that concrete class, without a discriminator.
 *
 * An instance owns its state (name registry and descriptor cache, at most 4096 schemas) and is safe for concurrent use.
 *
 * @param json the http4k kotlinx.serialization format of the contract; its serializers module finds the serializers.
 */
@OptIn(ExperimentalSerializationApi::class)
public class KotlinxApiRenderer
    @JvmOverloads
    public constructor(
        private val json: ConfigurableKotlinxSerialization = KotlinxSerialization,
    ) : ApiRenderer<Api<JsonElement>, JsonElement> {
        private val openApi3 = OpenApi3ApiRenderer(json)
        private val auto = ApiRenderer.Auto<Api<JsonElement>, JsonElement>(json)
        private val walker = DescriptorWalker()
        private val registry = DefinitionRegistry()
        private val factory = JsonSchemaFactory.resolve()
        private val walked = ConcurrentHashMap<SerialDescriptor, WalkedSchema>()

        init {
            require(json.json.configuration.namingStrategy == null) {
                "KotlinxApiRenderer does not support a Json with a namingStrategy yet"
            }
        }

        /** Renders [api] as http4k does for any kotlinx.serialization format. */
        override fun api(api: Api<JsonElement>): JsonElement = openApi3.api(api)

        /**
         * Renders the schema of [obj] from its descriptor when it is an instance of a `@Serializable` class or
         * object, otherwise as http4k does. The result is a reference to the schema, which is named [overrideDefinitionId]
         * or [refModelNamePrefix] followed by the serial name, with every schema it reaches as a definition.
         */
        override fun toSchema(
            obj: Any,
            overrideDefinitionId: String?,
            refModelNamePrefix: String?,
        ): JsonSchema<JsonElement> {
            val descriptor = objectDescriptorOf(obj) ?: return delegate(obj, overrideDefinitionId, refModelNamePrefix)
            val cached = walked[descriptor]
            val schema = cached ?: walker.walk(descriptor)
            val rendered = render(schema, overrideDefinitionId, refModelNamePrefix)
            if (cached == null) walked.putIfAbsent(descriptor, schema)
            return rendered
        }

        private fun objectDescriptorOf(obj: Any): SerialDescriptor? {
            if (obj is JsonElement) return null
            val serializer = json.json.serializersModule.serializerOrNull(obj.javaClass) ?: return null
            return serializer.descriptor.takeIf { it.kind == StructureKind.CLASS || it.kind == StructureKind.OBJECT }
        }

        // http4k probes with an anonymous object whether the renderer tells examples apart; kotlinx cannot serialise one.
        private fun delegate(
            obj: Any,
            overrideDefinitionId: String?,
            refModelNamePrefix: String?,
        ): JsonSchema<JsonElement> {
            val example: Any = if (obj.javaClass.isAnonymousClass) JsonObject(emptyMap()) else obj
            return auto.toSchema(example, overrideDefinitionId, refModelNamePrefix)
        }

        private fun render(
            schema: WalkedSchema,
            overrideDefinitionId: String?,
            refModelNamePrefix: String?,
        ): JsonSchema<JsonElement> {
            fun documentName(name: String): String =
                SchemaNames.sanitize(
                    if (name == schema.root && overrideDefinitionId != null) overrideDefinitionId else refModelNamePrefix.orEmpty() + name,
                )

            val definitions = LinkedHashMap<String, JsonElement>(schema.definitions.size * 2)
            for ((name, definition) in schema.definitions) {
                val documentName = documentName(name)
                registry.register(documentName, definition.descriptor)
                val previous = definitions.put(documentName, definition.model.toJson(::documentName))
                check(previous == null) {
                    "Schema name '$documentName' is used twice by ${SchemaNames.shorten(schema.root)}"
                }
            }
            return factory.create(schemaRef(documentName(schema.root)), definitions)
        }
    }
