plugins {
    id("com.android.application")
}

// En GitHub Actions cada compilación sube el número de versión para que el APK se instale encima del anterior.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "uy.labhoras"
    compileSdk = 36

    defaultConfig {
        applicationId = "uy.labhoras"
        minSdk = 30
        targetSdk = 36
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    signingConfigs {
        create("app") {
            storeFile = file("../keystore/labhoras.jks")
            storePassword = "labhoras"
            keyAlias = "labhoras"
            keyPassword = "labhoras"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("app")
        }
        debug {
            signingConfig = signingConfigs.getByName("app")
        }
    }

    // La interfaz es la app web de la carpeta /web (fuente única).
    sourceSets {
        getByName("main") {
            assets.srcDirs("../../web")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
}
