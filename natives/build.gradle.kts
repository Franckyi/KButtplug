plugins {
    java
    signing
    `maven-publish`
}

version = rootProject.property("ffi_version") as String

val native: Configuration by configurations.creating

repositories {
    exclusiveContent {
        forRepositories(
            ivy {
                url = uri("https://github.com/")
                patternLayout {
                    artifact("/[organisation]/buttplug-rs-ffi/releases/download/[revision]/[module].[ext]")
                }
                metadataSources { artifact() }
            }
        )
        filter {
            includeGroup("buttplugio")
        }
    }
}

dependencies {
    native(
        group = "buttplugio",
        name = "buttplug_rs_ffi-lib-win-x64-$version",
        ext = "zip",
        version = "core-$version",
        classifier = "windows"
    )
    native(
        group = "buttplugio",
        name = "buttplug_rs_ffi-lib-macos-x64-$version",
        ext = "zip",
        version = "core-$version",
        classifier = "macos"
    )
    native(
        group = "buttplugio",
        name = "buttplug_rs_ffi-lib-linux-x64-$version",
        ext = "zip",
        version = "core-$version",
        classifier = "linux"
    )
}

java {
    withSourcesJar()
    withJavadocJar()
}

lateinit var publication: MavenPublication

publishing {
    publications {
        publication = create<MavenPublication>("kbuttplugNatives") {
            from(components["java"])
            pom {
                name.set("KButtplug Natives")
                description.set("Buttplug Rust FFI native libraries for Windows, MacOS and Linux")
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
    sign(publishing.publications["kbuttplugNatives"])
}

native.resolvedConfiguration.resolvedArtifacts.forEach {
    tasks.processResources {
        from(zipTree(it.file))
    }

    val plaftormJarTask = tasks.create<Jar>("build${it.classifier!!.capitalize()}") {
        from(zipTree(it.file))
        archiveClassifier.set(it.classifier)
    }

    tasks.build {
        dependsOn(plaftormJarTask)
    }

    publication.artifact(plaftormJarTask)
}
