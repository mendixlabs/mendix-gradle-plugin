package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.sql.DriverManager

/**
 * Get Mendix version from MPR file.
 */
abstract class GetMendixVersion : DefaultTask() {

    @get:InputFile
    abstract val mprFilename: RegularFileProperty

    @TaskAction
    fun runTask() {
        val file = mprFilename.get().asFile
        if (!file.exists()) {
            throw RuntimeException("MPR file can't be found: ${mprFilename}")
        }

        DriverManager.getConnection("jdbc:sqlite:${file.toString()}").use { con ->
            con.createStatement().use { statement ->
                val rs = statement.executeQuery ("SELECT _ProductVersion FROM _MetaData")
                if (rs.next()) {
                    project.logger.lifecycle(rs.getString("_ProductVersion"))
                }
            }
        }
    }

}