package mendixlabs.mendixgradleplugin.tasks

import mendixlabs.mendixgradleplugin.ToolFinderBuilder
import org.apache.groovy.json.internal.IO
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.BufferedReader
import java.io.FileWriter
import java.io.InputStreamReader

enum class OutputType {
    CONSOLE,
    FILE
}

abstract class MxCommand: DefaultTask() {

    @get:Input
    @get:Option(
            option = "output-type",
            description = "CONSOLE or FILE"
    )
    @get:Optional
    abstract val outputType: Property<OutputType>

    @get:OutputFile
    @get:Option(
            option = "output-file",
            description = "provide file name"
    )
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val tool: Property<String>

    @get:Input
    abstract val mendixVersion: Property<String>

    @get:Input
    @get:Option(
            option = "arg",
            description = "Argument list for mx(.exe). You can provide multiple '--arg' flags."
    )
    abstract val args: ListProperty<String>

    // watch is a trick to specify which files need to be monitored for task execution
    // since this generic task and possible files will be passed via args
    // to always run a task use `outputs.upToDateWhen { false }` in the configuration
    @get:InputFiles
    abstract val watch: ConfigurableFileCollection

    @TaskAction
    fun runTask() {
        val toolFinder = ToolFinderBuilder()
                .withMendixVersion(mendixVersion.get())
                .withProject(project)
                .build()

        val cmd = mutableListOf(toolFinder.getToolLocation(tool.get()))
        cmd.addAll(args.get())
        logger.info("MxCommand executing: " + cmd.joinToString(" "))

        val mx = ProcessBuilder(cmd)
                .directory(project.projectDir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

        val outputReaderThread = Thread {
            val stdOutStream =  BufferedReader(InputStreamReader(mx.inputStream, "UTF-8"))
            stdOutStream.use {

                if (outputType.isPresent && outputType.get() == OutputType.FILE) {
                    val outFile = FileWriter(outputFile.get().asFile)
                    outFile.use {
                        IO.copy(stdOutStream, outFile)
                    }
                } else {
                    var stdout: String = ""
                    while (stdOutStream.readLine()?.also { stdout = it } != null) {
                        logger.lifecycle(stdout)
                    }
                }
            }
        }

        outputReaderThread.start()
        val result = mx.waitFor();
        logger.debug("Process exit code: ${result}")
        outputReaderThread.interrupt();
    }
}