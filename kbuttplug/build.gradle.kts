import com.google.protobuf.gradle.*

plugins {
    id("com.google.protobuf")
}

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
