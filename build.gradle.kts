plugins {
    kotlin("jvm") version "1.7.10" apply false
    kotlin("plugin.serialization") version "1.7.10" apply false
    id("org.jetbrains.dokka") version "1.7.0" apply false
    id("com.google.protobuf") version "0.8.19" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    group = "dev.franckyi.kbuttplug"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension>("java") {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        withSourcesJar()
    }

    if (project.name != "kbuttplug-natives") {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "org.jetbrains.dokka")

        val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)
        val javadocJar = tasks.register<Jar>("javadocJar") {
            dependsOn(dokkaHtml)
            archiveClassifier.set("javadoc")
            from(dokkaHtml.outputDirectory)
        }

        tasks.named("build") {
            dependsOn(javadocJar)
        }
    }
}