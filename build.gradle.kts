plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Kotlin 2.0.21 के लिए KSP का सही वर्ज़न 2.0.21-1.0.28 है
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
