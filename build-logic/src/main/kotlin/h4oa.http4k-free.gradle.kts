import io.github.mrlarkyy.http4k.openapi.build.ForbiddenDependencyCheck

plugins {
    id("h4oa.kotlin-jvm")
}

val checkHttp4kFree =
    tasks.register<ForbiddenDependencyCheck>("checkHttp4kFree") {
        description = "Fails when an org.http4k module is on any compile or runtime classpath."
        group = "verification"
        subject.set(project.name)
        forbiddenGroups.add("org.http4k")
        marker.set(layout.buildDirectory.file("h4oa/checkHttp4kFree.ok"))
        for (name in listOf("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath")) {
            inspect(configurations.getByName(name))
        }
    }

tasks.named("check") { dependsOn(checkHttp4kFree) }
