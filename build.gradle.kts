plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

subprojects {
    fun registerIdeCompatibilityTask() {
        if (tasks.findByName("prepareKotlinBuildScriptModel") == null) {
            tasks.register("prepareKotlinBuildScriptModel") {
                group = "ide"
                description = "Compatibility task for IDE Kotlin build script model requests."
            }
        }
    }

    plugins.withId("org.jetbrains.kotlin.android") {
        registerIdeCompatibilityTask()
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        registerIdeCompatibilityTask()
    }
}
