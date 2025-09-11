package mendixlabs.mendixgradleplugin.tasks

import mendixlabs.mendixgradleplugin.Arch
import mendixlabs.mendixgradleplugin.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

enum class DistributionType {
    MODELER, RUNTIME
}

fun constructMxbuildFilename(type: DistributionType, version: String): String {
    if (Os.current() == Os.OSX) {
        throw RuntimeException("For Mac OS, please install the Studio Pro from the Marketplace.")
    }

    if (type == DistributionType.RUNTIME) {
        return "mendix-${version}.tar.gz"
    }

    var prefix: String = ""
    if (Os.current() == Os.WIN) {
        prefix = "win-"
    } else if (Os.current() == Os.LINUX && Arch.current() == Arch.ARM) {
        prefix = "arm64-"
    }

    return prefix + "mxbuild-${version}.tar.gz"
}
/**
 * Downloads MxBuild from Mendix CDN taking into account the current operating
 * system and system architecture.
 */
abstract class CDNDownload : DefaultTask() {

    @get:Input
    abstract val distribution: Property<DistributionType>

    @get:Input
    abstract val mendixVersion: Property<String>

    @get:Input
    abstract val cdnLocation: Property<String>

    @get:OutputFile
    abstract val dest : RegularFileProperty

    init {
        cdnLocation.convention("https://cdn.mendix.com/runtime")
    }

    @Internal
    fun getFilename(): String {
        return constructMxbuildFilename(distribution.get(), mendixVersion.get())
    }

    @TaskAction
    fun runTask() {
        logger.lifecycle("Fetching ${getFilename()}")
        download("${cdnLocation.get()}/${getFilename()}", dest.get().asFile)
    }

    fun download(url: String, dest: File) {
        if (dest.exists()) {
            logger.info("File exists on disk")
            return
        }

        BufferedInputStream(URL(url).openStream()).use { bis ->
            FileOutputStream(dest).use { file ->
                val bufferSize = 64 * 1024
                var bytesCopied: Long = 0
                val buffer = ByteArray(bufferSize)
                var bytes = bis.read(buffer)
                while (bytes >= 0) {
                    file.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = bis.read(buffer)
                }
            }
        }
    }

}
