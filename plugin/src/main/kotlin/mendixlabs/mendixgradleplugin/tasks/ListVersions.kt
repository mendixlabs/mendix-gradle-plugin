package mendixlabs.mendixgradleplugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.InputStreamReader
import java.net.URL

data class Semver(val major: Int, val minor: Int, val patch: Int, val build: Int, val preRelease: String? = null) : Comparable<Semver> {
    override fun compareTo(other: Semver): Int {
        return compareValuesBy(this, other, Semver::major, Semver::minor, Semver::patch, Semver::build, Semver::preRelease)
    }
}

abstract class ListVersions : DefaultTask() {

    @get:Input
    @get:Optional
    @get:Option(
            option = "filter",
            description = "Specify a filter on the listed versions"
    )
    abstract val filter: Property<String>

    @get:Input
    abstract val listingsFile: Property<String>

    @get:Input
    @get:Option(
            option = "onlyLastPatch",
            description = "List for every minor, only the last patch"
    )
    abstract val onlyLastPatch: Property<Boolean>

    init {
        listingsFile.convention("https://cdn.mendix.com/listing.txt")
        onlyLastPatch.convention(false)
    }

    @TaskAction
    fun runTask() {
        var versions: List<String>
        InputStreamReader(URL(listingsFile.get()).openStream()).use { isr ->
            versions = isr.readLines()
        }

        // filter the list
        var regex : Regex? = null
        if (filter.isPresent && !filter.get().isEmpty()) {
            regex = Regex(filter.get())
        }

        val majorMinorRegex = Regex("""([0-9]*)\.([0-9]*)""")
        versions.asSequence()
                .filter { e -> e.startsWith("runtime/mendix-") && e.endsWith(".tar.gz") }
                .map { e -> e.removePrefix("runtime/mendix-")}
                .map { e -> e.removeSuffix(".tar.gz")}
                .filter { e -> regex == null || regex.find(e)?.value?.isEmpty() == false }
                .fold(mutableListOf<String>()) { list, e ->
                    if (onlyLastPatch.get() && list.size > 0) {
                        val last = majorMinorRegex.find(list.last())
                        val cur = majorMinorRegex.find(e)
                        if (last?.value == cur?.value) list.set(list.lastIndex, e) else list.add(e)
                    } else {
                        list.add(e)
                    }
                    list
                }
                .sortedWith(compareBy({ parseSemver(it) }, { it }))
                .forEach { e -> logger.lifecycle(e) }
    }

    fun parseSemver(version: String): Semver {
        val mainAndPreRelease = version.split("-", limit = 2)
        val mainParts = mainAndPreRelease[0].split(".")
        val preRelease = if (mainAndPreRelease.size > 1) mainAndPreRelease[1] else null

        return Semver(
                major = if (mainParts.size >= 1) mainParts[0].toInt() else 0,
                minor = if (mainParts.size >= 2) mainParts[1].toInt() else 0,
                patch = if (mainParts.size >= 3) mainParts[2].toInt() else 0,
                build = if (mainParts.size >= 4) mainParts[3].toInt() else 0,
                preRelease = preRelease
        )
    }

}
