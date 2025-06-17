package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.*

abstract class GenerateStartScripts: DefaultTask() {

    @TaskAction
    fun runTask() {
        copyFile("start.bat", "scripts", false)
        copyFile("start", "scripts", true)
    }

    private fun copyFile(scriptName: String, outputDir: String, setExecFlag: Boolean) {
        val outDir = project.layout.buildDirectory.dir(outputDir).get().asFile
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        val scriptTarget = File(outDir, scriptName)
        if (setExecFlag) {
            scriptTarget.setExecutable(true, false)
        }

        val scriptSource = this.javaClass.getResourceAsStream("/mxw/$scriptName")
        BufferedInputStream(scriptSource).use { bis ->
            bis.copyTo(FileOutputStream(scriptTarget))
        }
    }

}