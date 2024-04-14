package mxw.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class MxBuildTask : DefaultTask() {
//    @get:Input
    @Input
    var messageText: String = ""

    @Input
    var generateJavaDebugInfo: Boolean = false

    @Input
    var generateSbom: Boolean = false

    @Input
    var looseVersionCheck: Boolean = false

    @Input
    var writeErrorsFile: Boolean = false

    @TaskAction
    fun runTask() {
        val ext =  this.project.extensions.getByType<MxGradlePluginExtension>(MxGradlePluginExtension::class.java)
        println("Hello from MxBuildTask: ${messageText} + ${ext.mendixVersion}")
    }
}
