import io.github.mrlarkyy.http4k.openapi.build.ForbiddenDependencyCheck

plugins {
    id("h4oa.kotlin-jvm")
}

fun String.isClasspathName() = endsWith("CompileClasspath", ignoreCase = true) || endsWith("RuntimeClasspath", ignoreCase = true)

val checkHttp4kFree =
    tasks.register<ForbiddenDependencyCheck>("checkHttp4kFree") {
        description = "Fails when an org.http4k module is on any compile or runtime classpath."
        group = "verification"
        subject.set(project.name)
        forbiddenGroups.add("org.http4k")
        marker.set(layout.buildDirectory.file("h4oa/checkHttp4kFree.ok"))
        configurations
            .matching { it.isCanBeResolved && it.name.isClasspathName() }
            .configureEach { inspect(this) }
    }

tasks.named("check") { dependsOn(checkHttp4kFree) }
