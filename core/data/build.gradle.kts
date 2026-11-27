import java.util.Base64
import java.util.Properties
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val localBillingProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val billingPublicKey = (
    providers.gradleProperty("BILLING_PUBLIC_KEY").orNull
        ?: providers.environmentVariable("FOCUSFARM_BILLING_PUBLIC_KEY").orNull
        ?: localBillingProperties.getProperty("BILLING_PUBLIC_KEY")
).orEmpty().trim()

fun hasValidBillingPublicKey(key: String): Boolean = runCatching {
    val decoded = Base64.getDecoder().decode(key)
    KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decoded))
}.isSuccess

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(hasValidBillingPublicKey(billingPublicKey)) {
            "Release billing requires a valid BILLING_PUBLIC_KEY from Play Console in local.properties, a Gradle property, or FOCUSFARM_BILLING_PUBLIC_KEY."
        }
    }
}

android {
    namespace = "com.focusfarm.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "BILLING_PUBLIC_KEY", "\"$billingPublicKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        named("test") {
            java.srcDirs(
                "../../app/src/test/java/com/focusfarm/app/data/repository",
                "src/test/java",
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    api(libs.billing)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.functions.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
}
