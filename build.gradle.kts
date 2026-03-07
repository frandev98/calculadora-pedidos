plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // HOMOLOGACIÓN DE KSP: Acoplado estrictamente a Kotlin 2.1.0
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    alias(libs.plugins.hilt.android) apply false
}