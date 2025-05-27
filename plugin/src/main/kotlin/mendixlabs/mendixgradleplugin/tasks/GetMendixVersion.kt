package mendixlabs.mendixgradleplugin.tasks

import mendixlabs.mendixgradleplugin.mendix.GetAppVersion
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * Get Mendix version from MPR file.
 */
abstract class GetMendixVersion : DefaultTask() {

    @get:InputFile
    abstract val mprFilename: RegularFileProperty

    @TaskAction
    fun runTask() {
        val file = mprFilename.get().asFile
        project.logger.debug("Reading version from ${file.absolutePath}")
        project.logger.lifecycle(GetAppVersion(file))
    }

}