package io.github.mrlarkyy.http4k.openapi.http4k

import io.github.mrlarkyy.http4k.openapi.http4k.internal.DefinitionRegistry
import io.github.mrlarkyy.http4k.openapi.http4k.internal.DescriptorWalker
import io.github.mrlarkyy.http4k.openapi.http4k.internal.JsonSchemaAccess
import io.github.mrlarkyy.http4k.openapi.http4k.internal.JsonSchemaFactory
import io.github.mrlarkyy.http4k.openapi.http4k.internal.SchemaNames
import io.github.mrlarkyy.http4k.openapi.http4k.internal.WalkedSchema
import io.github.mrlarkyy.http4k.openapi.http4k.internal.schemaRef
import io.github.mrlarkyy.http4k.openapi.http4k.internal.toJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonElement
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
 * `@SerialName("shop.Item")` gives `Item`; the same type in several routes gives one schema. When two different
 * schemas would get one name and at least one of them was built by this renderer (also when the other one was rendered
 * by http4k), rendering fails with an [IllegalStateException] instead of one schema silently overwriting the other.
 * Two different schemas that were both rendered by http4k keep http4k's behaviour: the last one wins.
 *
 * Supported for now: classes and objects whose properties are non-null, non-optional strings, booleans, ints, longs,
 * doubles or further such classes. Everything else fails with an [IllegalArgumentException] that names the serial name
 * and the property: nullable and optional properties, lists, maps, enums, floats, polymorphic properties, value
 * classes (as property or as example) and a [json] with a naming strategy. A class that refers to itself therefore fails
 * too, because it needs a nullable or collection property.
 *
 * Examples that this renderer cannot read a descriptor from, such as JSON elements, strings, numbers, generic classes
 * or types without a class or object descriptor, are rendered by http4k's [OpenApi3ApiRenderer] as without this
 * renderer, which also covers the probe that http4k renders first. The example of a subtype of a sealed or open
 * hierarchy is rendered as that concrete class, without a discriminator.
 *
 * Use one instance per contract: an instance owns its name registry and descriptor cache (at most 4096 schemas), which
 * are what detect name collisions, and it is safe for concurrent use.
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
        private val walker = DescriptorWalker()
        private val registry = DefinitionRegistry()
        private val factory = JsonSchemaFactory.resolve()
        private val walked = ConcurrentHashMap<Class<*>, WalkedSchema>()

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
            val cached = walked[obj.javaClass]
            val schema =
                cached
                    ?: objectDescriptorOf(obj)?.let(walker::walk)
                    ?: return delegate(obj, overrideDefinitionId, refModelNamePrefix)
            val rendered = render(schema, overrideDefinitionId, refModelNamePrefix)
            if (cached == null) walked.putIfAbsent(obj.javaClass, schema)
            return rendered
        }

        private fun objectDescriptorOf(obj: Any): SerialDescriptor? {
            if (obj is JsonElement) return null
            val serializer = json.json.serializersModule.serializerOrNull(obj.javaClass) ?: return null
            return serializer.descriptor.takeIf { it.kind == StructureKind.CLASS || it.kind == StructureKind.OBJECT }
        }

        private fun delegate(
            obj: Any,
            overrideDefinitionId: String?,
            refModelNamePrefix: String?,
        ): JsonSchema<JsonElement> {
            val schema = openApi3.toSchema(obj, overrideDefinitionId, refModelNamePrefix)
            for ((name, definition) in JsonSchemaAccess.definitions(schema)) {
                registry.register(name, DefinitionRegistry.DELEGATED, definition)
            }
            return schema
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
                val json = definition.model.toJson(::documentName)
                registry.register(documentName, definition.descriptor.serialName, json)
                check(definitions.put(documentName, json) == null) {
                    "Schema name '${SchemaNames.shorten(documentName)}' is used twice by ${SchemaNames.shorten(schema.root)}"
                }
            }
            return factory.create(schemaRef(documentName(schema.root)), definitions)
        }
    }
