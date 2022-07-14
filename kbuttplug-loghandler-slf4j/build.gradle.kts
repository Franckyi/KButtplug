plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":kbuttplug"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.slf4j:slf4j-api:1.7.36")
}

publishing.publications.getByName<MavenPublication>("mavenJava") {
    pom {
        name.set("KButtplug LogHandler SLF4J")
        description.set("Buttplug LogHandler that forwards log messages to a SLF4J logger")
    }
}