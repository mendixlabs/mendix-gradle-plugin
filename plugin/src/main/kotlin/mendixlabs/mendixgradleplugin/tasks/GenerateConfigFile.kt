package mendixlabs.mendixgradleplugin.tasks

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.*
import java.lang.StringBuilder
import java.util.zip.ZipFile

// Pragmatic generator for a HOCON config file containing all defined
// appconstants and scheduled events for the Mendix project.
abstract class GenerateConfigFile : DefaultTask() {

    @get:InputFile
    abstract val mda: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val templateFile: RegularFileProperty

    @TaskAction
    fun runTask() {
        logger.info("Generating config file into ${outputFile.get().asFile.toString()}")

        val mdaFile = mda.get().asFile
        ZipFile(mdaFile).use {zip ->
            // read metadata.json from the MDA
            val metadata = zip.entries().asSequence().filter { ze -> ze.name.equals("model/metadata.json") }.first()
            val metadataBlob = zip.getInputStream(metadata).readAllBytes()

            // Get the constants & scheduled events
            val json = JsonParser.parseString(metadataBlob.toString(Charsets.UTF_8)).asJsonObject
            val constants = json.get("Constants").asJsonArray.map { e -> e.asJsonObject }
            val scheduledEvents = json.get("ScheduledEvents").asJsonArray.map { e -> e.asJsonObject.get("Name").asString }

            FileWriter(outputFile.get().asFile).use { fw ->
                fw.append(generateConfig(constants, scheduledEvents))
            }
        }
    }

    fun generateConfig(constants: List<JsonObject>, scheduledEvents: List<String>) : String {
        // create the list of microflow constants
        val constantsConfig = StringBuilder()
        val iterator = constants.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next();

            if (!e.get("Description").asString.equals("")) {
                constantsConfig.appendLine("      # ${e.get("Description")}")
            }
            constantsConfig.appendLine("      # Type: ${e.get("Type").asString.replace("\"", "")}")
            constantsConfig.appendLine("      ${e.get("Name")} = ${e.get("DefaultValue")}")
            constantsConfig.appendLine("      ${e.get("Name")} = \${?${constantNameToEnvVar(e.get("Name").asString)}}")

            // add a blank between every constant, except for the last
            if (iterator.hasNext()) {
                constantsConfig.appendLine()
            }
        }

        // create list of scheduled events
        val seValue = scheduledEvents.joinToString(separator = ",")

        // apply the template
        val config = StringBuilder()
        val template = if (templateFile.isPresent) {
            FileInputStream(templateFile.get().asFile)
        } else
            this.javaClass.getResourceAsStream("/mxw/mx-config.template")

        val templateReader = BufferedReader(InputStreamReader(template))
        templateReader.use {
            var line = it.readLine()
            while (line != null) {
                config.appendLine(line)
                line = it.readLine()
            }
        }

        return config.toString()
                .replace("%MICROFLOW_CONSTANTS%", constantsConfig.toString())
                .replace("%SCHEDULED_EVENTS%", seValue)
    }

    fun constantNameToEnvVar(name: String): String {
        return "MX_RUNTIME_PARAMS_MICROFLOWCONSTANTS_${name.replace(".", "_").uppercase()}"
    }

}
