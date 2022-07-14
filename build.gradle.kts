import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10" apply false
    kotlin("plugin.serialization") version "1.7.10" apply false
    id("org.jetbrains.dokka") version "1.7.0" apply false
    id("com.google.protobuf") version "0.8.19" apply false
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    idea
    signing
    `maven-publish`
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = project.property("group") as String
    version = project.property("version") as String

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension>("java") {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withSourcesJar()
        withJavadocJar()
    }

    if (project.name != "kbuttplug-natives") {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jetbrains.dokka")

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "17"
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        tasks["javadoc"].enabled = false
        val dokkaHtml by tasks.getting(DokkaTask::class)
        tasks.replace("javadocJar", Jar::class.java).apply {
            dependsOn(dokkaHtml)
            archiveClassifier.set("javadoc")
            from(dokkaHtml.outputDirectory)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
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
        sign(publishing.publications["mavenJava"])
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}