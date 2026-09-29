buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 内置 Kotlin；用 buildscript classpath 将 KGP 提升到 2.3.21（与缓存一致）
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21" apply false
}
