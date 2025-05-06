package mendixlabs.mendixgradleplugin;

import org.gradle.api.Project
import java.io.File

enum class Arch {
    ARM, INTEL;

    companion object {
        fun current(): Arch {
            val arch = System.getProperty("os.arch")
            if (arch.equals("amd64")) {
                return INTEL
            }
            if (arch.equals("aarch64")) {
                return ARM
            }

            throw RuntimeException("Arch ${arch} not supported")
        }
    }
}

enum class Os {
    LINUX, OSX, WIN;

    companion object {
        fun current(): Os {
            val os = System.getProperty("os.name")
            if (os.lowercase().indexOf("windows") >= 0) {
                return WIN
            }
            if (os.lowercase().equals("linux")) {
                return LINUX
            }

            throw RuntimeException("Os ${os} not supported")
        }
    }

}

interface ToolFinder {
    fun isModelerInstalled(): Boolean
    fun getToolLocation(tool: String): String
    fun getMxLocation(): String
    fun getMxbuildLocation(): String
    fun getMxutilLocation(): String
}

class ToolFinderBuilder {

    lateinit private var mendixVersion: String
    lateinit private var project: Project

    fun withMendixVersion(version: String): ToolFinderBuilder {
        mendixVersion = version
        return this
    }

    fun withProject(project: Project): ToolFinderBuilder {
        this.project = project
        return this
    }

    fun build(): ToolFinder {
        var toolFinder: ToolFinder

        toolFinder = NoToolFounder(mendixVersion)
        toolFinder = DownloadToolFinder(mendixVersion, project, toolFinder)
        toolFinder = WinToolFinder(mendixVersion, toolFinder)
        return toolFinder
    }

}

class WinToolFinder(version: String, val fallback: ToolFinder): ToolFinder {

    private val modelerLocation = "C:/Program Files/Mendix/${version}/modeler"

    override fun isModelerInstalled(): Boolean {
        if (Os.current() != Os.WIN) {
            return fallback.isModelerInstalled()
        }

        val modelerDir = File(modelerLocation)
        return if (modelerDir.isDirectory) true else fallback.isModelerInstalled()
    }

    override fun getToolLocation(tool: String): String {
        if (Os.current() != Os.WIN) {
            return fallback.getToolLocation(tool)
        }

        val filename = if (!tool.endsWith(".exe")) "${tool}.exe" else tool
        val file = File("${modelerLocation}/${filename}")
        if (file.exists()) {
            return file.toString()
        }

        return fallback.getToolLocation(filename)
    }

    override fun getMxLocation(): String {
        return getToolLocation("mx.exe")
    }
    override fun getMxbuildLocation(): String {
        return getToolLocation("mxbuild.exe")
    }

    override fun getMxutilLocation(): String {
        return getToolLocation("mxutil.exe")
    }

}

class DownloadToolFinder(version: String,
                         project: Project,
                         private val fallback: ToolFinder): ToolFinder {

    private val modelerLocation = project.layout.buildDirectory.dir("modeler/${version}/modeler").get().asFile
    // double. should getMxLocation methods be kept and maybe just have getToolLocation
    private val execExtension = if (Os.current() == Os.WIN) ".exe" else ""

    override fun isModelerInstalled(): Boolean {
        val modelerDir = modelerLocation
        return if (modelerDir.isDirectory) true else fallback.isModelerInstalled()
    }

    override fun getToolLocation(tool: String): String {
        val file = File(modelerLocation, tool)
        if (file.exists()) {
            return file.toString()
        }
        return fallback.getToolLocation(tool)
    }

    override fun getMxLocation(): String {
        return getToolLocation("mx${execExtension}")
    }
    override fun getMxbuildLocation(): String {
        return getToolLocation("mxbuild${execExtension}")
    }

    override fun getMxutilLocation(): String {
        return getToolLocation("mxutil${execExtension}")
    }
}

class NoToolFounder(val version: String): ToolFinder {

    override fun isModelerInstalled(): Boolean {
        return false;
    }

    override fun getToolLocation(tool: String): String {
        throw RuntimeException("Modeler ${version} is not installed or '${tool}' tool is not available.")
    }

    override fun getMxLocation(): String {
        throw RuntimeException("Modeler ${version} is not installed or 'mx' tool is not available.")
    }
    override fun getMxbuildLocation(): String {
        throw RuntimeException("Modeler ${version} is not installed or 'mxbuild' is tool not available.")
    }

    override fun getMxutilLocation(): String {
        throw RuntimeException("Modeler ${version} is not installed or 'mxutil' is tool not available.")
    }

}
