plugins {
    kotlin("jvm") version "1.7.10" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "dev.franckyi.kbuttplug"
}