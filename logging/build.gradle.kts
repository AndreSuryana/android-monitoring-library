plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.andresuryana.amlib.logging"
    compileSdk = AmlibConfiguration.compileSdk

    defaultConfig {
        group = AmlibConfiguration.amlibGroup
        version = AmlibConfiguration.amlibVersion

        minSdk = AmlibConfiguration.minSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release")
    }
}

dependencies {
    // Kotlin KTX
    implementation(libs.androidx.core.ktx)

    // Core Module
    api(project(":core"))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = AmlibConfiguration.amlibGroup
            artifactId = project.name
            version = AmlibConfiguration.amlibVersion

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories {
        mavenLocal()
    }
}