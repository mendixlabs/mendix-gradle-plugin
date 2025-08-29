package mendixlabs.mendixgradleplugin.tasks

import mendixlabs.mendixgradleplugin.Os
import mendixlabs.mendixgradleplugin.ToolFinderBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

abstract class Mxbuild : DefaultTask() {

    @get:InputFile
    abstract val mpr: RegularFileProperty

    @get:Input
    abstract val mendixVersion: Property<String>

    @get:Input
    abstract val generateJavaDebugInfo: Property<Boolean>

    @get:Input
    abstract val generateSbom: Property<Boolean>

    @get:Input
    abstract val looseVersionCheck: Property<Boolean>

    @get:Input
    abstract val writeErrorsFile: Property<Boolean>

    // project files are used by Gradle to track incremental
    // builds and are not used in the task itself
    @get:InputFiles
    abstract val projectFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputPath: DirectoryProperty

    init {
        generateJavaDebugInfo.convention(false)
        generateSbom.convention(false)
        looseVersionCheck.convention(false)
        writeErrorsFile.convention(false)
    }


    // sometimes this task doesn't end. Seems a lot like the some reports in the gradle project
    // that on windows the Exec plugin doesn't exit. Could it be the reading from stdout or stderr?
    @TaskAction
    fun runTask() {
        val mprFile = mpr.get().asFile
        if (!mprFile.exists()) {
            throw RuntimeException("MPR file can't be found: ${mprFile}")
        }

        val toolFinder = ToolFinderBuilder()
                .withMendixVersion(mendixVersion.get())
                .withProject(project)
                .build()
        val mxbuild = ProcessBuilder(toolFinder.getMxbuildLocation())
                .directory(project.projectDir)

        // set all flags
        mxbuild.command().add("--java-home=${getJavaHome()}")
        mxbuild.command().add("--java-exe-path=${getJavaExec()}")

        val mdaName = "${project.name}.mda"
        mxbuild.command().add("--output=${outputPath.file(mdaName).get().asFile}")

        mxbuild.command().add("--project-name=${project.name}")
        val projectVersion = project.version
        if (projectVersion.toString() != "") {
            mxbuild.command().add("--model-version=${projectVersion}")
        }
        if (looseVersionCheck.get()) {
            mxbuild.command().add("--loose-version-check")
        }
        if (generateJavaDebugInfo.get()) {
            mxbuild.command().add("--java-debug-info")
        }
        if (generateSbom.get()) {
            mxbuild.command().add("--generate-sbom")
            mxbuild.command().add("--sbom-output-path=${outputPath.file("${project.name}-sbom.json").get().asFile}")
        }
        if (writeErrorsFile.get()) {
            mxbuild.command().add("--write-errors=${outputPath.file("${project.name}-errors.json").get().asFile}")
        }

        mxbuild.command().add(mprFile.toString())

        // configure output
        mxbuild.redirectOutput(ProcessBuilder.Redirect.PIPE)
        mxbuild.redirectErrorStream(true);

        logger.log(LogLevel.INFO, mxbuild.command().joinToString(" "))

        // start & capture output
        val process = mxbuild.start()
        logger.log(LogLevel.INFO, "process started")

        // capture output in a separate thread as it seems the stream is not always closed
        // or closure is not detected and as a result code keeps hanging on the read
        // naive implementation
        var outputReaderThread = Thread {
            val stdOutStream =  BufferedReader(InputStreamReader(process.inputStream))
            stdOutStream.use {
                var stdout: String = ""
                while ( stdOutStream.readLine()?.also { stdout = it } != null) {
                    logger.lifecycle(stdout)
                }
            }
        }

        // wait and evaluate
        try {
            project.logger.info( "waiting for process to finish")
            outputReaderThread.start()
            val result = process.waitFor()
            outputReaderThread.interrupt()

            if (result >= 1) {
                throw RuntimeException("MxBuild returned with exit code ${result}")
            }
        } catch (ex: InterruptedException) {
        }

    }

    @Internal
    fun getJavaHome(): String {
        // Prefer Gradle's javaHome if set, otherwise use the JVM running Gradle.
        // we'll assume for now this is the same Java version as configured
        // in the Mendix project.
        val gradleJavaHome = project.findProperty("org.gradle.java.home") as? String
        return gradleJavaHome ?: System.getProperty("java.home")
    }

    @Internal
    fun getJavaExec(): String {
        val javaExec = if (Os.current() == Os.WIN) "bin/java.exe" else "bin/java"
        return File(getJavaHome(), javaExec).toString();
    }

}
