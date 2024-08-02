import io.gitlab.arturbosch.detekt.Detekt
import org.apache.commons.io.output.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
  alias(libs.plugins.ksp)
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.kotlin.compose.compiler)
  alias(libs.plugins.room)
  alias(libs.plugins.detekt)
}

android {
  namespace = "live.mehiz.mpvkt"
  compileSdk = 34

  defaultConfig {
    applicationId = "live.mehiz.mpvkt"
    minSdk = 21
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }

    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    buildConfigField(
      "String",
      "BUILD_TIME",
      "\"${LocalDateTime.now(ZoneOffset.UTC).format(dateTimeFormatter)}\"",
    )

    buildConfigField("String", "GIT_SHA", getCommitSha())
    buildConfigField("String", "GIT_COUNT", getCommitCount())
  }
  splits {
    abi {
      isEnable = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
      isUniversalApk = true
    }
  }

  buildTypes {
    named("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
    create("preview") {
      initWith(getByName("release"))

      signingConfig = signingConfigs["debug"]
      applicationIdSuffix = ".preview"
      versionNameSuffix = "-${getCommitCount()}"
    }
    named("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-${getCommitCount()}"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
    viewBinding = true
    buildConfig = true
  }
  composeCompiler {
    includeSourceInformation = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3.android)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.compose.constraintlayout)
  implementation(libs.androidx.material3.icons.extended)
  implementation(libs.androidx.documentfile)
  implementation(libs.androidx.compose.animation.graphics)
  implementation(libs.material)
  implementation(libs.androidx.preferences.ktx)
  implementation(libs.mediasession)

  implementation(libs.aniyomi.mpv.lib)
  implementation(libs.aniyomi.ffmpeg.kit)
  implementation(libs.arthentica.smartexceptions)

  implementation(libs.seeker)
  implementation(libs.bundles.koin)
  implementation(libs.bundles.voyager)
  implementation(libs.compose.prefs)

  implementation(libs.room.runtime)
  ksp(libs.room.compiler)
  implementation(libs.room.ktx)

  implementation(libs.detekt.gradle.plugin)
  detektPlugins(libs.detekt.rules.compose)
  detektPlugins(libs.detekt.formatter)

  implementation(libs.kotlinx.immutable.collections)
  implementation(libs.truetype.parser)
}

detekt {
  parallel = true
  allRules = false
  buildUponDefaultConfig = true
  config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.withType<Detekt>().configureEach {
  setSource(files(project.projectDir))
  exclude("**/build/**")
  reports {
    html.required.set(true)
    md.required.set(true)
  }
}

fun getCommitCount(): String = runCommand("git rev-list --count HEAD")
fun getCommitSha(): String = runCommand("git rev-parse --short HEAD")
fun runCommand(command: String): String {
  val stdOut = ByteArrayOutputStream()
  exec {
    commandLine = command.split(' ')
    standardOutput = stdOut
  }
  return String(stdOut.toByteArray()).trim()
}
