import com.google.protobuf.gradle.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    id("org.jetbrains.dokka") version "1.7.0"
    id("com.google.protobuf") version "0.8.19"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    idea
    signing
    `maven-publish`
}

group = project.property("group") as String
version = project.property("version") as String

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:${rootProject.property("jna_version")}")
    implementation("com.google.protobuf:protobuf-kotlin:${rootProject.property("protobuf_version")}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${rootProject.property("kotlinx_serialization_version")}")
    implementation("org.slf4j:slf4j-api:${rootProject.property("slf4j_version")}")

    testImplementation(kotlin("test"))
    testRuntimeOnly("org.slf4j:slf4j-simple:${rootProject.property("slf4j_version")}")
    testRuntimeOnly(project(":kbuttplug-natives"))
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    exclude("*.proto")
}

tasks.javadoc {
    enabled = false
}

val dokkaHtml by tasks.getting(DokkaTask::class)
tasks.replace("javadocJar", Jar::class.java).apply {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
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

publishing {
    publications {
        create<MavenPublication>("kbuttplug") {
            from(components["java"])
            pom {
                name.set("KButtplug")
                description.set("Kotlin implementation of the Buttplug Rust FFI")
                url.set("https://github.com/Franckyi/KButtplug")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://mit-license.org/")
                    }
                }
                developers {
                    developer {
                        id.set("Franckyi")
                        name.set("Franck Velasco")
                        email.set("franck.velasco@hotmail.fr")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Franckyi/KButtplug.git")
                    developerConnection.set("scm:git:https://github.com/Franckyi/KButtplug.git")
                    url.set("https://github.com/Franckyi/KButtplug")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["kbuttplug"])
}


nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}