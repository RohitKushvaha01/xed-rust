import com.google.gson.Gson
import java.net.URL
import java.net.URI
import com.google.gson.JsonObject
import java.util.jar.JarFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val sdkMappingRules = layout.buildDirectory.file("sdk-mapping-rules.pro")
val extractedMapping = layout.buildDirectory.file("sdk-mapping.txt")

android {
    namespace = "com.rk.demo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rk.demo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                sdkMappingRules.get().asFile
            )
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        // Should match with Xed-Editor
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
    buildFeatures {
        compose = true
    }
}

tasks.register("extractSdkMapping") {
    description = ""
    dependsOn("downloadLatestJar")
    val jarFile = file("libs/sdk.jar")
    val outFile = extractedMapping.get().asFile
    inputs.file(jarFile)
    outputs.file(outFile)

    doLast {
        JarFile(jarFile).use { jar ->
            val entry = jar.getJarEntry("mapping.txt")
                ?: throw GradleException("mapping.txt not found in sdk.jar")

            jar.getInputStream(entry).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}


tasks.register("generateMappingRules") {
    description = ""
    dependsOn("extractSdkMapping")
    val rulesFile = sdkMappingRules.get().asFile
    val mappingFile = extractedMapping.get().asFile
    inputs.file(mappingFile)
    outputs.file(rulesFile)

    doLast {
        rulesFile.writeText("-applymapping ${mappingFile.absolutePath}")
    }
}

// Always try to match the versions of library to the versions used in Xed-Editor
dependencies {
    // Xed-Editor extension SDK, required to interact with the application, do NOT remove
    compileOnly(files("libs/sdk.jar"))

    // If a library is used in Xed-Editor and your extension is common, then you should use compileOnly. Otherwise, it slows down the app.
    compileOnly(libs.androidx.appcompat)
    compileOnly(libs.material)
    compileOnly(libs.androidx.constraintlayout)
    compileOnly(libs.androidx.navigation.fragment)
    compileOnly(libs.androidx.navigation.ui)
    compileOnly(libs.androidx.navigation.fragment.ktx)
    compileOnly(libs.androidx.navigation.ui.ktx)
    compileOnly(libs.androidx.activity)
    compileOnly(libs.androidx.lifecycle.viewmodel)
    compileOnly(libs.androidx.lifecycle.runtime)
    compileOnly(libs.androidx.activity.compose)
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly(libs.androidx.compose.ui)
    compileOnly(libs.androidx.compose.ui.graphics)
    compileOnly(libs.androidx.compose.material3)
    compileOnly(libs.androidx.navigation.compose)
    compileOnly(libs.utilcode)
    compileOnly(libs.coil.compose)
    compileOnly(libs.gson)
    compileOnly(libs.commons.net)
    compileOnly(libs.okhttp)
    compileOnly(libs.material.motion.compose)
    compileOnly(libs.nanohttpd)
    compileOnly(libs.photoview)
    compileOnly(libs.glide)
    compileOnly(libs.androidx.browser)
    compileOnly(libs.quickjs.android)
    compileOnly(libs.anrwatchdog)
    compileOnly(libs.lsp4j)
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.androidx.documentfile)
    compileOnly(libs.compose.dnd)
    compileOnly(libs.androidx.compose.material.icons.core)
    compileOnly(libs.pine.core)
    compileOnly(libs.androidx.lifecycle.process)
    compileOnly(libs.androidsvg.aar)
}

//  ---------------- below is the code for automatically updating the sdk.jar --------------------

val GITHUB_OWNER = "Xed-Editor"
val GITHUB_REPO = "Xed-Editor"
val TAG_NAME = "sdk-latest"
val ASSET_NAME = "sdk.jar"

val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/tags/$TAG_NAME"
val DOWNLOAD_URL =
    "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$TAG_NAME/$ASSET_NAME"

val timestampFile = project.layout.buildDirectory.file("sdk_updated_at.txt")
val outputFile = project.layout.projectDirectory.file("libs/$ASSET_NAME")

tasks.register<DefaultTask>("downloadLatestJar") {
    outputs.upToDateWhen { false }
    description = "Checks and downloads the latest $ASSET_NAME from GitHub."
    group = "build"

    val out = outputFile.asFile
    val ts = timestampFile.get().asFile
    val apiUrl = API_URL
    val downloadUrl = DOWNLOAD_URL
    val assetName = ASSET_NAME

    outputs.file(out)
    outputs.file(ts)

    doLast {
        out.parentFile.mkdirs()
        ts.parentFile.mkdirs()

        val remoteUpdatedAt: String
        try {
            val connection = URI(apiUrl).toURL().openConnection().apply {
                connectTimeout = 10000
                readTimeout = 10000
            }
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            @Suppress("UNCHECKED_CAST")
            val releaseMap = Gson().fromJson(json, Map::class.java) as Map<String, Any>
            remoteUpdatedAt = releaseMap["updated_at"] as String
        } catch (e: Exception) {
            throw GradleException("Could not check latest release timestamp: ${e.message}", e)
        }

        val storedUpdatedAt = if (ts.exists()) {
            ts.readText().trim()
        } else {
            null
        }

        if (remoteUpdatedAt == storedUpdatedAt) {
            println("✅ $assetName is up to date (Timestamp: $remoteUpdatedAt). Skipping download.")
            return@doLast
        }

        println("Release updated ($storedUpdatedAt -> $remoteUpdatedAt). Downloading new JAR...")

        try {
            val connection = URI(downloadUrl).toURL().openConnection().apply {
                connectTimeout = 15000
                readTimeout = 15000
            }
            connection.getInputStream().use { inputStream ->
                out.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            ts.writeText(remoteUpdatedAt)
            println("Successfully downloaded $assetName to ${out.path}")
        } catch (e: Exception) {
            throw GradleException("Download failed: ${e.message}", e)
        }
    }
}

tasks.register<Delete>("cleanApkOutputs") {
    description = "Clears all generated files and subdirectories from the build/outputs/apk folder."
    group = "cleanup"
    delete(layout.buildDirectory.dir("outputs/apk"))
}

tasks.register<Delete>("cleanOutput") {
    description = "Deletes the output directory."
    group = "cleanup"
    delete(File(rootDir, "output"))
}

tasks.named("preBuild").configure {
    dependsOn("cleanApkOutputs")
    dependsOn("downloadLatestJar")
    dependsOn("generateMappingRules")
}

// --------------- extension metadata ---------------

val manifest = File(rootDir, "manifest.json")
val manifestJson: JsonObject by lazy { Gson().fromJson(manifest.readText(), JsonObject::class.java) }
val extensionName: String by lazy { manifestJson.get("name").asString }
val iconFile = File(rootDir, "icon.png")
val readmeFile = File(rootDir, "README.md")
val changelogFile = File(rootDir, "CHANGELOG.md")

// --------------- generate the final zip file -----------------

tasks.register<Zip>("createFinalZip") {
    dependsOn("cleanOutput")
    dependsOn("assembleDebug")
    dependsOn("assembleRelease")

    archiveFileName.set("$extensionName.zip")

    val apkFiles = fileTree(layout.buildDirectory.dir("outputs/apk")) {
        include("**/*.apk")
    }

    from(apkFiles) {
        eachFile {
            path = name
        }
        includeEmptyDirs = false
    }

    doFirst {
        if (apkFiles.isEmpty) {
            throw GradleException("No APK files found.")
        }
    }

    from(manifest)
    from(iconFile)
    from(readmeFile)
    from(changelogFile)

    destinationDirectory.set(File(rootDir, "output"))
}