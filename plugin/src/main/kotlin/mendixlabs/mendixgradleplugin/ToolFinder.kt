package mendixlabs.mendixgradleplugin;

import org.gradle.api.Project
import org.gradle.api.logging.Logger
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
    fun isRuntimeInstalled(): Boolean
    fun getRuntimeLocation(): String
    fun isModelerInstalled(): Boolean
    fun getToolLocation(tool: String): String
    fun getMxLocation(): String
    fun getMxbuildLocation(): String
    fun getMxutilLocation(): String
}

class ToolFinderBuilder {

    lateinit private var mendixVersion: String
    lateinit private var project: Project
    private val paths: ArrayList<File> = ArrayList<File>()

    fun withMendixVersion(version: String): ToolFinderBuilder {
        mendixVersion = version
        return this
    }

    fun withProject(project: Project): ToolFinderBuilder {
        this.project = project
        return this
    }

    fun withPath(path: File): ToolFinderBuilder {
        this.paths.plus(path)
        return this
    }

    fun build(): ToolFinder {
        var toolFinder: ToolFinder

        // add default paths
        // get install path
        val extension: MxGradlePluginExtension = project.extensions.getByName("mendix") as MxGradlePluginExtension
        if (extension.installPath.isPresent) {
            paths.add(File(extension.installPath.get() + "/${mendixVersion}"))
        }
        // default location on window
        if (Os.current() == Os.WIN) {
            paths.add(File("C:/Program Files/Mendix/${mendixVersion}"))
        }
        // default download location
        paths.add(project.layout.buildDirectory.dir("modeler/${mendixVersion}").get().asFile)

        toolFinder = NoToolFounder(mendixVersion)
        toolFinder = PathToolFinder(paths, project.logger, toolFinder)
        return toolFinder
    }

}

class PathToolFinder(val paths: List<File>, val logger: Logger, val fallback: ToolFinder): ToolFinder {

    override fun isRuntimeInstalled(): Boolean {
        logger.debug("Searching for runtime (${paths.size})")
        return paths
            .map { e -> File(e, "runtime") }
            .filter { e -> logger.debug("checking runtime dir: {}", e); e.exists() }.isNotEmpty() || fallback.isModelerInstalled();
    }

    override fun getRuntimeLocation(): String {
        val file = paths.map { folder -> File(folder, "runtime/") }
            .find { e -> logger.debug("checking runtime dir: {}", e); e.exists() }
        if (file == null) {
            return fallback.getRuntimeLocation()
        }
        return file.toString()

    }

    override fun isModelerInstalled(): Boolean {
        logger.debug("Searching for modeler (${paths.size})")
        return paths
            .map { e -> File(e, "modeler") }
            .filter { e -> logger.debug("checking file: {}", e); e.exists() }.isNotEmpty() || fallback.isModelerInstalled();
    }

    override fun getToolLocation(tool: String): String {
        val filename = getOsToolName(tool)

        val file = paths.map { folder -> File(folder, "modeler/${filename}") }
            .find { e -> logger.debug("checking file: {}", e); e.exists() }
        if (file == null) {
            return fallback.getToolLocation(tool)
        }
        return file.toString()
    }

    override fun getMxLocation(): String {
        return getToolLocation("mx")
    }
    override fun getMxbuildLocation(): String {
        return getToolLocation("mxbuild")
    }

    override fun getMxutilLocation(): String {
        return getToolLocation("mxutil")
    }

    private fun getOsToolName(tool: String): String {
        if (Os.current() == Os.WIN && !tool.endsWith(".exe")) {
            return "${tool}.exe"
        }
        return tool
    }

}

class NoToolFounder(val version: String): ToolFinder {

    override fun isRuntimeInstalled(): Boolean {
        return false;
    }

    override fun getRuntimeLocation(): String {
        throw RuntimeException("Runtime ${version} is not installed.")
    }

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
