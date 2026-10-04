import com.android.build.api.dsl.ApplicationExtension
import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

configure<ApplicationExtension> {
    namespace = "com.kotonosora.todolist"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kotonosora.todolist"
        minSdk = 24
        versionCode = 16
        versionName = "3.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // No build-time locale filter: androidResources.localeFilters is @Incubating
    // and resConfigs is deprecated with no stable replacement. English-only is
    // enforced at runtime instead (localeConfig + setApplicationLocales("en-US")
    // in MainApplication), so bundled extra languages stay unused.

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "**/*.kotlin_module"
        }
    }
}

tasks.configureEach {
    doLast {
        outputs.files.files.forEach { file ->
            if (file.isDirectory) {
                file.walkTopDown().forEach { f ->
                    if (f.name.contains(":")) {
                        println("Task $name output file with colon: ${f.absolutePath}")
                        f.delete()
                    }
                }
            } else if (file.name.endsWith(".jar") && file.exists()) {
                val tempJar = File(file.parentFile, "temp_${file.name}")
                var modified = false
                try {
                    ZipInputStream(file.inputStream()).use { zis ->
                        ZipOutputStream(tempJar.outputStream()).use { zos ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (!entry.name.contains(":")) {
                                    zos.putNextEntry(ZipEntry(entry.name))
                                    zis.copyTo(zos)
                                    zos.closeEntry()
                                } else {
                                    modified = true
                                    println("Task $name: Removed invalid entry from jar ${file.name}: ${entry.name}")
                                }
                                entry = zis.nextEntry
                            }
                        }
                    }
                    if (modified) {
                        file.delete()
                        tempJar.renameTo(file)
                    } else {
                        tempJar.delete()
                    }
                } catch (_: Exception) {
                    tempJar.delete()
                }
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.work.runtime.ktx)
    implementation(libs.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.richeditor.compose)

    // CameraX & Coil Image Loading
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.gif)
    // Video-frame thumbnails for the media gallery grid (MediaMetadataRetriever +
    // Coil memory/disk cache, off Main). Avoids spinning up an ExoPlayer per cell.
    implementation(libs.coil.video)

    // Media3 ExoPlayer Audio Playback (MP3/AAC/Vorbis/Opus/FLAC/WAV/AMR/MIDI
    // via platform codecs + bundled extractors; undecodable files surface an
    // inline error instead of crashing — see AudioPlayerView/VideoPlayerView).
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // DataStore Preferences & DocumentFile & Serialization
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.serialization.json)

    // Kizitonwose Compose Calendar
    implementation(libs.kizitonwose.calendar.compose)

    // MikePenz Multiplatform Markdown Renderer M3
    implementation(libs.markdown.renderer)
    implementation(libs.markdown.code)
    implementation(libs.markdown.coil3)

    // JGraphT Graph & Force Layout Core
    implementation(libs.jgrapht.core)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.startup)
}
