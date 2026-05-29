import java.io.File
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val mobileCoreLibraryName = "moni_core"
val androidApiLevel = 28
val androidNdkVersion = "29.0.14206865"

private val cargoWorkspaceDir = rootProject.projectDir
private val cargoWorkspaceTomlFile = cargoWorkspaceDir.resolve("Cargo.toml")
private val cargoWorkspaceLock = cargoWorkspaceDir.resolve("Cargo.lock")
private val cargoTargetDir = cargoWorkspaceDir.resolve("target")
private val releaseManifestFile = cargoWorkspaceDir.resolve("release.properties")
private val releaseManifest = Properties().apply {
    releaseManifestFile.inputStream().use(::load)
}
private val releaseVersionName = releaseManifest.getProperty("release_version")
    ?: error("release.properties 缺少 release_version")
private val releaseVersionCode = releaseManifest.getProperty("android_version_code")
    ?.toIntOrNull()
    ?: error("release.properties 缺少合法的 android_version_code")
private val androidSdkRoot = System.getenv("ANDROID_SDK_ROOT")
    ?: System.getenv("ANDROID_HOME")
    ?: error("缺少 ANDROID_SDK_ROOT 或 ANDROID_HOME")
private val ndkRootDir = File(androidSdkRoot).resolve("ndk/$androidNdkVersion")
private val ndkToolchainBinDir = ndkRootDir.resolve("toolchains/llvm/prebuilt/linux-x86_64/bin")

private val mobileCoreProjectDir = rootProject.file("moni-core")
private val mobileCoreCargoTomlFile = mobileCoreProjectDir.resolve("Cargo.toml")
private val mobileCoreSourcesDir = mobileCoreProjectDir.resolve("src")
private val contractsProjectDir = rootProject.file("moni-contracts")
private val mobileCoreHostLibrary = cargoTargetDir.resolve("release/lib$mobileCoreLibraryName.so")
private val mobileCoreGeneratedBindingsDir = layout.buildDirectory.dir("generated/source/uniffi/main/kotlin")
private val mobileCoreGeneratedJniDir = layout.buildDirectory.dir("generated/jniLibs/mobileCore")
private val mobileCoreArm64Library = cargoTargetDir.resolve("aarch64-linux-android/release/lib$mobileCoreLibraryName.so")
private val mobileCoreX64Library = cargoTargetDir.resolve("x86_64-linux-android/release/lib$mobileCoreLibraryName.so")

fun rustCrateInputs(crateDir: File) = fileTree(crateDir) {
    include("Cargo.toml")
    include("build.rs")
    include("src/**")
}

private val mobileCoreRustInputs = files(
    rustCrateInputs(mobileCoreProjectDir),
    rustCrateInputs(contractsProjectDir),
)

abstract class SyncMobileCoreJniLibsTask : DefaultTask() {
    @get:InputFile
    abstract val arm64LibraryFile: RegularFileProperty

    @get:InputFile
    abstract val x64LibraryFile: RegularFileProperty

    @get:Input
    abstract val libraryFileName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun syncLibraries() {
        syncLibrary(
            abi = "arm64-v8a",
            sourceFile = arm64LibraryFile.get().asFile,
        )
        syncLibrary(
            abi = "x86_64",
            sourceFile = x64LibraryFile.get().asFile,
        )
    }

    private fun syncLibrary(abi: String, sourceFile: File) {
        val targetDir = outputDirectory.get().asFile.resolve(abi)
        targetDir.mkdirs()
        sourceFile.copyTo(targetDir.resolve(libraryFileName.get()), overwrite = true)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.agguy.moni"
    compileSdk = 37
    ndkVersion = androidNdkVersion

    defaultConfig {
        applicationId = "com.agguy.moni"
        minSdk = androidApiLevel
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = releaseVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 当前阶段按项目约定继续生成 debug 签名的 release 包。
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/libjnidispatch.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.documentfile)
    implementation(variantOf(libs.jna) { artifactType("aar") })

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}

val buildMobileCoreHostRelease = tasks.register<Exec>("buildMobileCoreHostRelease") {
    group = "build"
    description = "构建主机侧 moni-core 共享库，用于生成 UniFFI Kotlin 绑定。"
    workingDir = cargoWorkspaceDir
    commandLine("cargo", "build", "-p", "moni-core", "--release")
    inputs.file(cargoWorkspaceTomlFile).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(cargoWorkspaceLock).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(mobileCoreRustInputs).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(mobileCoreHostLibrary)
}

fun registerMobileCoreAndroidBuildTask(
    taskName: String,
    targetTriple: String,
    linkerName: String,
    outputFile: File,
) = tasks.register<Exec>(taskName) {
    group = "build"
    description = "构建 $targetTriple 的 moni-core 动态库。"
    workingDir = cargoWorkspaceDir
    val linkerEnvName = "CARGO_TARGET_${targetTriple.uppercase().replace('-', '_')}_LINKER"
    val ccEnvName = "CC_${targetTriple.replace('-', '_')}"
    val arEnvName = "AR_${targetTriple.replace('-', '_')}"
    val compilerPath = ndkToolchainBinDir.resolve(linkerName).absolutePath
    val arPath = ndkToolchainBinDir.resolve("llvm-ar").absolutePath
    environment(linkerEnvName, compilerPath)
    environment(ccEnvName, compilerPath)
    environment(arEnvName, arPath)
    environment("CC", compilerPath)
    environment("AR", arPath)
    commandLine("cargo", "build", "-p", "moni-core", "--release", "--target", targetTriple)
    inputs.file(cargoWorkspaceTomlFile).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(cargoWorkspaceLock).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(mobileCoreRustInputs).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(outputFile)
}

val buildMobileCoreArm64Release = registerMobileCoreAndroidBuildTask(
    taskName = "buildMobileCoreArm64Release",
    targetTriple = "aarch64-linux-android",
    linkerName = "aarch64-linux-android${androidApiLevel}-clang",
    outputFile = mobileCoreArm64Library,
)

val buildMobileCoreX64Release = registerMobileCoreAndroidBuildTask(
    taskName = "buildMobileCoreX64Release",
    targetTriple = "x86_64-linux-android",
    linkerName = "x86_64-linux-android${androidApiLevel}-clang",
    outputFile = mobileCoreX64Library,
)

val generateMobileCoreBindings = tasks.register<Exec>("generateMobileCoreBindings") {
    group = "build"
    description = "根据 moni-core 共享库生成 UniFFI Kotlin 绑定。"
    dependsOn(buildMobileCoreHostRelease)
    workingDir = cargoWorkspaceDir
    commandLine(
        "cargo",
        "run",
        "-p",
        "moni-core",
        "--features",
        "bindgen",
        "--bin",
        "uniffi-bindgen",
        "generate",
        "--library",
        mobileCoreHostLibrary.absolutePath,
        "--language",
        "kotlin",
        "--out-dir",
        mobileCoreGeneratedBindingsDir.get().asFile.absolutePath,
        "--no-format",
    )
    inputs.file(mobileCoreHostLibrary)
    inputs.files(mobileCoreRustInputs).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(mobileCoreGeneratedBindingsDir)
}

val syncMobileCoreJniLibs = tasks.register<SyncMobileCoreJniLibsTask>("syncMobileCoreJniLibs") {
    group = "build"
    description = "同步 Android 需要的 moni-core 动态库。"
    dependsOn(buildMobileCoreArm64Release, buildMobileCoreX64Release)
    arm64LibraryFile.set(mobileCoreArm64Library)
    x64LibraryFile.set(mobileCoreX64Library)
    libraryFileName.set("lib$mobileCoreLibraryName.so")
    outputDirectory.set(mobileCoreGeneratedJniDir)
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(
            syncMobileCoreJniLibs,
            SyncMobileCoreJniLibsTask::outputDirectory,
        )
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(generateMobileCoreBindings)
    source(mobileCoreGeneratedBindingsDir.get().asFile)
}

tasks.named("preBuild") {
    dependsOn(
        generateMobileCoreBindings,
        syncMobileCoreJniLibs,
    )
}
