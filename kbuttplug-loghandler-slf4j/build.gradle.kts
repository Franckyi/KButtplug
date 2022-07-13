import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //kotlin("jvm")
    kotlin("plugin.serialization")
    //id("org.jetbrains.dokka")
}

group = "dev.franckyi.kbuttplug"
version = "0.1.0"

dependencies {
    implementation(project(":kbuttplug"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.slf4j:slf4j-api:1.7.36")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

/*publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}/