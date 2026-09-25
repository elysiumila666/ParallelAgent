import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}
val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

val dashscopeApiKey = localProperties.getProperty("DASHSCOPE_API_KEY") ?: ""
val deepseekApiKey = localProperties.getProperty("DEEPSEEK_API_KEY") ?: ""
android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.example.parallelagent"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        buildConfigField(
            "String",
            "DASHSCOPE_API_KEY",
            "\"$dashscopeApiKey\""
        )

        buildConfigField(
            "String",
            "DEEPSEEK_API_KEY",
            "\"$deepseekApiKey\""
        )
        applicationId = "com.example.parallelagent"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}