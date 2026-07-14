plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.nexussu.manager"
    compileSdk = 35
    ndkVersion = "25.1.8937393"

    defaultConfig {
        applicationId = "com.nexussu.manager"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("dev.chrisbanes.haze:haze:1.2.0")
    implementation("dev.chrisbanes.haze:haze-materials:1.2.0")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    
    // ADDED: JSON parsing for Update Checker
    implementation("org.json:json:20240303")
}

// --- AUTO-COMPILE SU BINARY ---
tasks.register<Exec>("compileSuBinary") {
    val ndkDir = android.ndkDirectory.absolutePath
    val osName = System.getProperty("os.name").lowercase()
    val hostTag = when {
        osName.contains("windows") -> "windows-x86_64"
        osName.contains("mac") -> "darwin-x86_64"
        else -> "linux-x86_64"
    }
    val clang = file("$ndkDir/toolchains/llvm/prebuilt/$hostTag/bin/aarch64-linux-android24-clang").absolutePath
    val sourceFile = file("native/su.c")
    val outputDir = file("src/main/assets")
    val outputFile = file("$outputDir/su.bin")
    outputs.file(outputFile)
    inputs.file(sourceFile)
    doFirst {
        outputDir.mkdirs()
        if (!sourceFile.exists()) { throw GradleException("Missing native/su.c file!") }
    }
    commandLine(clang, sourceFile.absolutePath, "-o", outputFile.absolutePath, "-static")
}

// --- AUTO-COMPILE NEXUSSU DAEMON ---
tasks.register<Exec>("compileNexusDaemon") {
    val ndkDir = android.ndkDirectory.absolutePath
    val osName = System.getProperty("os.name").lowercase()
    val hostTag = when {
        osName.contains("windows") -> "windows-x86_64"
        osName.contains("mac") -> "darwin-x86_64"
        else -> "linux-x86_64"
    }
    val clang = file("$ndkDir/toolchains/llvm/prebuilt/$hostTag/bin/aarch64-linux-android24-clang").absolutePath
    val sourceFile = file("native/nexussu_daemon.c")
    val outputDir = file("src/main/assets")
    val outputFile = file("$outputDir/nexussu_daemon")
    outputs.file(outputFile)
    inputs.file(sourceFile)
    doFirst {
        outputDir.mkdirs()
        if (!sourceFile.exists()) { throw GradleException("Missing native/nexussu_daemon.c file!") }
    }
    commandLine(clang, sourceFile.absolutePath, "-o", outputFile.absolutePath, "-static")
}

tasks.named("preBuild").configure {
    dependsOn("compileSuBinary")
    dependsOn("compileNexusDaemon")
}

// --- AUTO-COMPILE BUSYBOX ---
tasks.register<Exec>("compileBusyBox") {
    val ndkDir = android.ndkDirectory.absolutePath
    val osName = System.getProperty("os.name").lowercase()
    val hostTag = when {
        osName.contains("windows") -> "windows-x86_64"
        osName.contains("mac") -> "darwin-x86_64"
        else -> "linux-x86_64"
    }
    val clang = file("$ndkDir/toolchains/llvm/prebuilt/$hostTag/bin/aarch64-linux-android24-clang").absolutePath
    val sourceFile = file("native/busybox.c")
    val outputDir = file("src/main/assets")
    val outputFile = file("$outputDir/busybox.bin")
    outputs.file(outputFile)
    inputs.file(sourceFile)
    doFirst {
        outputDir.mkdirs()
        if (!sourceFile.exists()) { throw GradleException("Missing native/busybox.c file!") }
    }
    commandLine(clang, sourceFile.absolutePath, "-o", outputFile.absolutePath, "-static")
}

// Update preBuild to include BusyBox
tasks.named("preBuild").configure {
    dependsOn("compileSuBinary")
    dependsOn("compileNexusDaemon")
    dependsOn("compileBusyBox") // NEW
}
