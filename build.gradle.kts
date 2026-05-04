import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.plugin.DetektPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    apply<DetektPlugin>()

    configure<DetektExtension> {
        buildUponDefaultConfig = true
        autoCorrect = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        parallel = true
        ignoreFailures = false
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "17"
        reports {
            html.required.set(true)
            checkstyle.required.set(true)
            sarif.required.set(false)
            markdown.required.set(false)
        }
    }
}

abstract class CargoCheckTask : DefaultTask() {
    @get:Inject abstract val execOperations: ExecOperations

    @get:Internal abstract val workspaceDir: DirectoryProperty

    init {
        group = "verification"
        description = "对 Rust workspace 执行 cargo fmt --check 与 cargo clippy"
    }

    @TaskAction
    fun execute() {
        val dir = workspaceDir.get().asFile
        execOperations.exec {
            workingDir = dir
            commandLine("cargo", "fmt", "--all", "--", "--check")
        }
        execOperations.exec {
            workingDir = dir
            commandLine("cargo", "clippy", "--workspace", "--all-targets", "--", "-D", "warnings")
        }
    }
}

val checkRustAll = tasks.register<CargoCheckTask>("checkRustAll") {
    workspaceDir.set(rootProject.layout.projectDirectory)
}

tasks.register("checkAll") {
    group = "verification"
    description = "同时检查 Kotlin UI 与 Rust 内核的代码质量"
    dependsOn("detekt", checkRustAll)
}
