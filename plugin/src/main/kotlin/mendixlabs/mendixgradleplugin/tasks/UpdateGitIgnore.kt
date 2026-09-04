package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

abstract class UpdateGitIgnore: DefaultTask() {

    @TaskAction
    fun runTask() {
        val gitIgnoreFile = File(project.projectDir, ".gitignore")

        val existingLines = if (gitIgnoreFile.exists()) {
            gitIgnoreFile.readLines().toMutableSet()
        } else {
            mutableSetOf()
        }

        // gitignore values based on what Studio 11.12 writes
        val templateStream = this.javaClass.getResourceAsStream("/mxw/gitignore.template")
            ?: throw IllegalStateException("gitignore.template not found in resources")

        val templateLines = BufferedReader(InputStreamReader(templateStream)).use { reader ->
            reader.readLines()
        }

        val newLines = templateLines.filter { line ->
            line !in existingLines
        }

        if (newLines.isNotEmpty()) {
            gitIgnoreFile.appendText(buildString {
                if (gitIgnoreFile.exists() && gitIgnoreFile.readText().isNotEmpty() && !gitIgnoreFile.readText().endsWith("\n")) {
                    appendLine()
                }
                if (existingLines.isNotEmpty()) {
                    appendLine()
                    appendLine("# Mendix-specific ignores")
                }
                newLines.forEach { line ->
                    appendLine(line)
                }
            })
            logger.lifecycle("Updated .gitignore with ${newLines.size} new entries")
        } else {
            logger.lifecycle(".gitignore already contains all template entries")
        }
    }

}
