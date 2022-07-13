import com.google.protobuf.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    //kotlin("jvm")
    //id("org.jetbrains.dokka")
    id("com.google.protobuf")
}

version = "0.1.0"

dependencies {
    implementation("net.java.dev.jna:jna:5.11.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.2")

    testImplementation(kotlin("test"))
    testRuntimeOnly(project(":kbuttplug-natives"))
}

tasks.jar {
    exclude("*.proto")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.2"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                id("kotlin")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}