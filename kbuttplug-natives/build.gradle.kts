version = "2.0.4"

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

native.resolvedConfiguration.resolvedArtifacts.forEach {
    tasks.processResources {
        from(zipTree(it.file))
    }

    val plaftormJarTask = tasks.create<Jar>("build-${it.classifier}") {
        from(it.file)
        archiveClassifier.set(it.classifier)
    }

    tasks.build {
        dependsOn(plaftormJarTask)
    }
}

java {
    withJavadocJar()
}

