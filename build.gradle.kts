plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

// Add this new block:
subprojects {
    configurations.all {
        resolutionStrategy {
            force("com.squareup:javapoet:1.13.0")

            // Additional dependency constraints for Hilt
            eachDependency {
                if (requested.group == "com.google.dagger") {
                    useVersion(libs.versions.hilt.get())
                }
            }
        }
    }
}