package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.options.Option

/**
 * Extension of the JavaExec class to add specific argument options
 * for the Mendix java command. To start a Mendix application two
 * arguments are required: app folder and configuration file. These
 * would normally be passed as the `args` parameter in MxRun. These
 * parameters are now set explicitly for this plugin. The user is
 * expected to only set the config file option.
 *
 * NOTE: This tasks makes the JavaExec 'args' argument obsolete.
 */
abstract class MxRun : JavaExec() {

    @get:InputFile
    @get:Option(option = "configFile", description = "Configuration file to use")
    abstract val configFile: RegularFileProperty

    @get:InputDirectory
    abstract val appFolder: RegularFileProperty

    override fun exec() {
        args = listOf(
            appFolder.get().asFile.absolutePath,
            configFile.get().asFile.absolutePath
        )
        super.exec()
    }

}