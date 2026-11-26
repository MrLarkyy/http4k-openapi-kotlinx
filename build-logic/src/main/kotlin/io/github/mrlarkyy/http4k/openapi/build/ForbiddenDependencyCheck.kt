package io.github.mrlarkyy.http4k.openapi.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Fails when a resolved dependency belongs to one of [forbiddenGroups].
 *
 * Inputs are plain strings (resolved component ids per configuration), so the task is configuration-cache safe
 * and up to date while the dependency graph is unchanged.
 */
abstract class ForbiddenDependencyCheck : DefaultTask() {
    /** Name used in the failure message, normally the project name. */
    @get:Input
    abstract val subject: Property<String>

    /** Maven groups that must not appear. */
    @get:Input
    abstract val forbiddenGroups: SetProperty<String>

    /** Resolved `group:artifact:version` ids by configuration name. */
    @get:Input
    abstract val resolvedComponents: MapProperty<String, List<String>>

    /** Written when the check passes. */
    @get:OutputFile
    abstract val marker: RegularFileProperty

    /** Adds the resolved graph of [configuration] to the inspected inputs. */
    fun inspect(configuration: Configuration) {
        resolvedComponents.put(
            configuration.name,
            configuration.incoming.resolutionResult.rootComponent
                .map { componentIds(it) },
        )
    }

    @TaskAction
    fun check() {
        val forbidden = forbiddenGroups.get()
        val violations =
            resolvedComponents.get().toSortedMap().flatMap { (configuration, ids) ->
                ids
                    .filter { id -> id.substringBefore(':') in forbidden }
                    .map { id -> "${subject.get()} must not depend on ${id.substringBefore(':')}: $id in $configuration" }
            }
        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString("\n"))
        }
        marker.get().asFile.apply {
            parentFile.mkdirs()
            writeText("ok\n")
        }
    }

    private fun componentIds(root: ResolvedComponentResult): List<String> {
        val seen = LinkedHashSet<ResolvedComponentResult>()
        val pending = ArrayDeque<ResolvedComponentResult>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val component = pending.removeFirst()
            if (!seen.add(component)) continue
            component.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach { pending.add(it.selected) }
        }
        return seen
            .mapNotNull { component ->
                component.moduleVersion?.let { "${it.group}:${it.name}:${it.version}" }
            }.sorted()
    }
}
