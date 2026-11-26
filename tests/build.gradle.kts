plugins {
    id("h4oa.kotlin-jvm")
    id("h4oa.version-matrix")
}

dependencies {
    testImplementation(project(":core"))
    testImplementation(project(":http4k"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.java.diff.utils)
}

val approveGolden = providers.gradleProperty("approveGolden").orElse("false")

tasks.withType<Test>().configureEach {
    systemProperty("h4oa.golden.approve", approveGolden.get())
    if (approveGolden.get() == "true") {
        outputs.upToDateWhen { false }
        outputs.cacheIf { false }
    }
}

tasks.named<Test>("test") {
    systemProperty("h4oa.matrix.entry", "lowest")
}

tasks.named<Test>("testCurrent") {
    systemProperty("h4oa.matrix.entry", "current")
    mustRunAfter(tasks.named("test"))
}

tasks.named<Test>("testNewest") {
    systemProperty("h4oa.matrix.entry", "newest")
    mustRunAfter(tasks.named("test"), tasks.named("testCurrent"))
}
