import com.google.protobuf.gradle.*
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.google.protobuf") version "0.8.19"
    idea
    signing
    `maven-publish`
}

version = "0.1.0"

repositories {
    mavenCentral()
    val github = ivy {
        url = uri("https://github.com/")
        patternLayout {
            artifact("/[organisation]/buttplug-rs-ffi/releases/download/[revision]/[module].[ext]")
        }
        metadataSources { artifact() }
    }
    exclusiveContent {
        forRepositories(github)
        filter {
            includeGroup("buttplugio")
        }
    }
}

val ffiObject: Configuration by configurations.creating

val buttplugFfiVersionSimple = "2.0.4"
val buttplugFfiVersion = "core-$buttplugFfiVersionSimple"

dependencies {
    implementation("net.java.dev.jna:jna:5.11.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.2")

    testImplementation(kotlin("test"))

    ffiObject(
        group = "buttplugio",
        name = "buttplug_rs_ffi-lib-linux-x64-$buttplugFfiVersionSimple",
        ext = "zip",
        classifier = "linux-x86-64",
        version = buttplugFfiVersion
    )
    ffiObject(
        group = "buttplugio",
        name = "buttplug_rs_ffi-lib-macos-x64-$buttplugFfiVersionSimple",
        ext = "zip",
        classifier = "darwin-x86-64",
        version = buttplugFfiVersion
    )
    ffiObject(
        group = "buttplugio",
        name = "buttplug_rs_ffi-lib-win-x64-$buttplugFfiVersionSimple",
        ext = "zip",
        classifier = "win32-x86-64",
        version = buttplugFfiVersion
    )
}

tasks.create<Jar>("sourcesJar") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

// for each shared object:
ffiObject.resolvedConfiguration.resolvedArtifacts.forEach {
    // retrieve the platform name that JNA uses
    val targetFolder = it.classifier
    // unzip into the correct folder
    val unzipTask = tasks.register<Copy>("unzip-$targetFolder") {
        from(zipTree(it.file))
        into(file("$buildDir/generated/source/ffi-objects/main/resources/$targetFolder"))
    }

    val buildNativeTask = tasks.register<Jar>("build-native-$targetFolder") {
        from(zipTree(it.file))
        archiveClassifier.set("natives-$targetFolder")
    }
    // ensure that they're ready in time for resources to be put in the jar
    tasks.processResources {
        dependsOn(unzipTask)
    }

    tasks.build {
        dependsOn(buildNativeTask)
    }

    tasks.jar {
        exclude("$targetFolder")
    }

    tasks.named<Jar>("sourcesJar") {
        exclude("$targetFolder")
    }
}

sourceSets.main {
    resources {
        // include the folder that has the shared object resources
        srcDir(file("${buildDir}/generated/source/ffi-objects/main/resources"))
    }
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

tasks.build {
    dependsOn("sourcesJar")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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