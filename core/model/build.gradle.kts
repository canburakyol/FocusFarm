import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

sourceSets {
    test {
        kotlin.srcDirs(
            "../../app/src/test/java/com/focusfarm/app/domain",
            "src/test/kotlin",
        )
    }
}
