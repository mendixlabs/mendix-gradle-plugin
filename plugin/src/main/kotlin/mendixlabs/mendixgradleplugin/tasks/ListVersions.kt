package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.InputStreamReader
import java.net.URL

abstract class ListVersions : DefaultTask() {

    @get:Input
    abstract val listingsFile: Property<String>

    init {
        listingsFile.convention("https://cdn.mendix.com/listing.txt")
    }

    @TaskAction
    fun runTask() {
        var versions: List<String>
        InputStreamReader(URL(listingsFile.get()).openStream()).use { isr ->
            versions = isr.readLines()
        }

        // filter the list
        versions.filter { e -> e.startsWith("runtime/mendix-") && e.endsWith(".tar.gz") }
                .forEach { e -> logger.lifecycle(e) }
    }
}