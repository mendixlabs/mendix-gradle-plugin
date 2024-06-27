package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.lang.StringBuilder

abstract class GenerateDockerFile : DefaultTask() {

    @get:Input
    abstract val baseImage: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val templateFile: RegularFileProperty

    init {
        baseImage.convention("eclipse-temurin:21.0.3_9-jre-jammy")
        outputFile.convention(project.layout.buildDirectory.file("docker/Dockerfile"))
    }

    @TaskAction
    fun runTask() {
        logger.info("Generating Docker file into ${outputFile.get().asFile.toString()}")

        // apply the template
        val configTemplate = StringBuilder()
        val template = if (templateFile.isPresent) {
            FileInputStream(templateFile.get().asFile)
        } else
            this.javaClass.getResourceAsStream("/mxw/Dockerfile.template")

        val templateReader = BufferedReader(InputStreamReader(template))
        templateReader.use {
            var line = it.readLine()
            while (line != null) {
                configTemplate.appendLine(line)
                line = it.readLine()
            }
        }

        val renderedConfig = configTemplate.toString()
                .replace("%BASE_IMAGE%", baseImage.get())
                .replace("%PROJECT%", project.name)

        FileWriter(outputFile.get().asFile).use { fw ->
            fw.append(renderedConfig)
        }

    }

}
