package mendixlabs.mendixgradleplugin.tasks

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import java.io.*
import java.lang.StringBuilder
import java.util.Collections
import java.util.zip.ZipFile

abstract class WriteConfigs : DefaultTask() {

    @get:InputFile
    abstract val mda: RegularFileProperty

    @get:InputFile
    abstract val mprAsJson: RegularFileProperty

    @get:Input
    abstract val configNames: SetProperty<String>

    @get:OutputDirectory
    abstract val outputPath: DirectoryProperty

    @TaskAction
    fun runTask() {
        logger.info("Extracting configs from ${mprAsJson.get().asFile.toString()}")

        val metadata = getMetadata()
        val configs = readProjectSettingsFromMpr(mprAsJson.get().asFile)

        configs.forEach { config ->
            if (configNames.get().isNotEmpty() && !configNames.get().contains(config.name)) {
                project.logger.info("Skipping config ${config.name} as it is not present in configNames")
            }
            logger.info("Writing config ${config.name} to file")

            // use the defaults from metadata and overwrite them with
            // specifics from configuration
            val effectConstants = calculateEffectiveConstants(config, metadata)
            val effectiveSettings = calculateEffectiveSettings(config, metadata)

            renderConfig(config, effectConstants, effectiveSettings)
        }
    }

    @Internal
    fun getProjectConfigs() : List<Configuration> {
        val configurations = ArrayList<Configuration>()

        // Get the constants & scheduled events
        FileReader(mprAsJson.asFile.get(), Charsets.UTF_8).use { reader ->
            val mpr = JsonParser.parseReader(reader).asJsonObject
            val units = mpr.get("units").asJsonArray
            val projectSettings = units.find { el -> el.asJsonObject.get("\$Type").asString.equals("Settings\$ProjectSettings") }?.asJsonObject
            val parts = projectSettings?.get("settingsParts")?.asJsonArray
            val configSettings = parts?.find { el -> el.asJsonObject.get("\$Type").asString.equals("Settings\$ConfigurationSettings") }?.asJsonObject
            val configs = configSettings?.get("configurations")?.asJsonArray
            configs?.forEach { e ->

                val config = Gson().fromJson<Configuration>(e, Configuration::class.java)
                configurations.add(config)
            }
        }

        return configurations;
    }

    fun readProjectSettingsFromMpr(jsonMpr: File): List<Configuration> {
        FileReader(jsonMpr, Charsets.UTF_8).use { reader ->
            // use streaming for memory efficiency
            val jsonReader = JsonReader(reader) ;

            jsonReader.beginObject();
            if (jsonReader.hasNext() && jsonReader.nextName() == "units") {
                // search a document type "Settings$ProjectSettings", this should be small enough to load into memory
                // while complete MPRs represented as JSON go into 10's or 100's of Mb.
                jsonReader.beginArray();
                while (jsonReader.hasNext()) {
                    val projectSettings = testForProjectSettings(jsonReader);
                    if (projectSettings != null) {
                        return readConfigurationsFromJson(projectSettings);
                    }
                }
            }
            jsonReader.endObject();
            return Collections.emptyList();
        }
    }

    fun testForProjectSettings(jsonReader: JsonReader): JsonArray? {
        var isProjectSettings = false;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "\$Type" -> {
                    isProjectSettings = jsonReader.nextString() == "Settings\$ProjectSettings"
                }
                "settingsParts" -> {
                    if (isProjectSettings) {
                        return Gson().fromJson<JsonArray>(jsonReader, JsonArray::class.java);
                    }
                }
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject();
        return null;
    }

    fun readConfigurationsFromJson(parts: JsonArray): List<Configuration> {
        val configurations = ArrayList<Configuration>()

        val configSettings = parts.find { el -> el.asJsonObject.get("\$Type").asString.equals("Settings\$ConfigurationSettings") }?.asJsonObject
        val configs = configSettings?.get("configurations")?.asJsonArray
        configs?.forEach { e ->

            val config = Gson().fromJson<Configuration>(e, Configuration::class.java)
            configurations.add(config)
        }

        return configurations;
    }


    @Internal
    fun getMetadata() : MetadataJson {
        val mdaFile = mda.get().asFile
        ZipFile(mdaFile).use { zip ->
            // read metadata.json from the MDA
            val metadata = zip.entries().asSequence().filter { ze -> ze.name.equals("model/metadata.json") }.first()
            val reader = InputStreamReader(zip.getInputStream(metadata))

            // Get the constants & scheduled events
            return Gson().fromJson<MetadataJson>(reader, MetadataJson::class.java)
        }
    }

    private fun calculateEffectiveSettings(config: Configuration, metadata: MetadataJson): Map<String, CustomSetting> {
        // effectiveSettings is the list retrieved from the Configuration
        // but some values are provided as dedicated input field in Studio Pro,
        // these have preference
        val effectiveSettings = config.customSettings.associateBy { it.name }.toMutableMap()
        effectiveSettings["ApplicationRootUrl"] = CustomSetting("", "", "ApplicationRootUrl", config.applicationRootUrl)
        effectiveSettings["DTAPMode"] = CustomSetting("", "", "DTAPMode", "D")
        effectiveSettings["DatabaseType"] = CustomSetting("", "", "DatabaseType", config.databaseType)
        if (config.databaseType == "Hsqldb") {
            effectiveSettings["DatabaseJdbcUrl"] = CustomSetting("", "", "DatabaseJdbcUrl", "jdbc:hsqldb:mem:embeddedDataSource")
        } else {
            effectiveSettings["DatabaseJdbcUrl"] = CustomSetting("", "", "DatabaseJdbcUrl", config.databaseUrl)
        }

        effectiveSettings["MyScheduledEvents"] = CustomSetting("", "", "MyScheduledEvents",  metadata.scheduledEvents.map { it.name }.joinToString(","))
        effectiveSettings["ScheduledEventExecution"] = CustomSetting("", "", "ScheduledEventExecution", "SPECIFIED")
        return effectiveSettings
    }

    private fun calculateEffectiveConstants(config: Configuration, metadata: MetadataJson): Map<String, MetadataConstant> {
        // effectiveConstants is the configuration to be applied using the
        // defaults from metadata.json and then overwrite defaults based on
        // the Configuration that is processed
        val effectiveConstants = metadata.constants.associateBy { it.name }.toMutableMap()
        config.constantValues.forEach { e ->
            val current = effectiveConstants[e.constant]

            if (current != null) {
                if (e.sharedOrPrivateValue.type.equals("Settings\$SharedValue")) {
                    effectiveConstants[e.constant] = MetadataConstant(current.name, current.type, current.description, e.sharedOrPrivateValue.value)
                } else {
                    effectiveConstants[e.constant] = MetadataConstant(current.name, current.type, current.description, "")
                }
            }
        }
        return effectiveConstants
    }


    private fun renderConfig(config: Configuration, constants: Map<String, MetadataConstant>, settings: Map<String, CustomSetting>) {
        // write preference file in HOCON format
        val configBuffer = StringBuilder()
        configBuffer.append("# Config generated from: ").appendLine(config.name)
        configBuffer.appendLine("admin {")
        configBuffer.appendLine("  adminPassword = \"verysecret1\"")
        configBuffer.appendLine("  adminPassword = \${?MX_ADMIN_PASSWORD}")

        configBuffer.append("  addresses = [\"").append(if(config.adminPortOnlyLocal) "127.0.0.1" else "0.0.0.0").appendLine("\"]")
        configBuffer.appendLine("  addresses = \${?MX_ADMIN_ADDRESSES}")
        configBuffer.append("  port = ").appendLine(config.adminPortNumber)
        configBuffer.appendLine("  port = \${?MX_ADMIN_PORT}")

        configBuffer.appendLine("  monitoringPassword = \${?MX_ADMIN_MONITORINGPASSWORD}")

        configBuffer.appendLine("}")
        configBuffer.appendLine()
        configBuffer.appendLine("runtime {")
        configBuffer.appendLine("  http {")

        configBuffer.append("    addresses = [\"").append(if(config.runtimePortOnlyLocal) "127.0.0.1" else "0.0.0.0").appendLine("\"]")
        configBuffer.appendLine("    addresses = \${?MX_RUNTIME_HTTP_ADDRESSES}")
        configBuffer.append("    port = ").appendLine(config.runtimePortNumber)
        configBuffer.appendLine("    port = \${?MX_RUNTIME_HTTP_PORT}")

        configBuffer.appendLine("  }")
        configBuffer.appendLine()
        configBuffer.appendLine("  params {")
        settings.toSortedMap().forEach { e ->
            val envVarName = e.value.name.replace(".", "_").uppercase()

            // if json parson succeeds don't escape
            var isJsonObject = false;
            try {
                val json = JsonParser.parseString(e.value.value)
                isJsonObject = json.isJsonObject || json.isJsonArray
            } catch (ex: Exception) {
            }

            if (isJsonObject) {
                configBuffer.append("    \"").append(e.key).append("\" = ").appendLine(e.value.value);
            } else {
                configBuffer.append("    \"").append(e.key).append("\" = \"").append(e.value.value).appendLine("\"")
            }

            configBuffer.append("    \"").append(e.key).append("\" = \${?MX_RUNTIME_PARAMS_").append(envVarName).appendLine("}")
            configBuffer.appendLine()
        }

        configBuffer.appendLine("    MicroflowConstants {")
        constants.forEach { e ->
            val envVarName = e.key.replace(".", "_").uppercase()

            if (e.value.description != "") {
                BufferedReader(StringReader(e.value.description)).readLines().forEach { line ->
                    configBuffer.append("      # ").appendLine(line)
                }
            }
            configBuffer.append("      # Type: ").appendLine(e.value.type.replace("\"", ""))
            configBuffer.append("      \"").append(e.key).append("\" = \"").append(e.value.defaultValue).appendLine("\"")
            configBuffer.append("      \"").append(e.key).append("\" = \${?MX_RUNTIME_PARAMS_MICROFLOWCONSTANTS_").append(envVarName).appendLine("}")
            configBuffer.appendLine()
        }
        configBuffer.appendLine("    }") // end runtime.params.MicroflowConstants

        configBuffer.appendLine("  }") // end runtime.params


        configBuffer.appendLine("  adminUser = \${?MX_RUNTIME_ADMINUSER}")
        configBuffer.appendLine("  \"license.key\" = \${?MX_RUNTIME_LICENSEKEY}")
        configBuffer.appendLine("  \"debugger.password\" = \${?MX_RUNTIME_DEBUGGERPASSWORD}")

        configBuffer.appendLine("}") // end runtime
        configBuffer.appendLine()

        // append default logging configuration
        configBuffer.appendLine("""
logging = [{
    name = stdout
    type = console
    autoSubscribe = INFO
    levels = {}
}]      """)
        configBuffer.appendLine("logging = \${?MX_LOGGING}")

        FileWriter(File(outputPath.asFile.get(), config.name + ".conf")).use { fw ->
            fw.append(configBuffer.toString())
        }

    }

}

// Metadata Structure
data class MetadataConstant(
        @SerializedName("Name")
        val name: String,
        @SerializedName("Type")
        val type: String,
        @SerializedName("Description")
        val description: String,
        @SerializedName("DefaultValue")
        val defaultValue: String,
)

data class MetadataScheduledEvent(
        @SerializedName("Name")
        val name: String
)

data class MetadataJson(
        @SerializedName("Constants")
        val constants: List<MetadataConstant>,
        @SerializedName("ScheduledEvents")
        val scheduledEvents: List<MetadataScheduledEvent>
)

// MPR Configuration Structure
data class CustomSetting(
        @SerializedName("ID")
        val id: String,
        @SerializedName("\$Type")
        val type: String,
        val name: String,
        val value: String
)

data class SharedValue(
        @SerializedName("ID")
        val id: String,
        @SerializedName("\$Type")
        val type: String,
        val value: String
)

data class ConstantValue(
        @SerializedName("ID")
        val id: String,
        @SerializedName("\$Type")
        val type: String,
        val sharedOrPrivateValue: SharedValue,
        val constant: String
)

data class Configuration(
        @SerializedName("ID")
        val id: String,
        @SerializedName("\$Type")
        val type: String,
        val customSettings: List<CustomSetting>,
        val constantValues: List<ConstantValue>,
        val name: String,
        val applicationRootUrl: String,
        val runtimePortNumber: Int,
        val adminPortNumber: Int,
        val runtimePortOnlyLocal: Boolean,
        val adminPortOnlyLocal: Boolean,
        val maxJavaHeapSize: Int,
        val extraJvmParameters: String,
        val databaseType: String,
        val databaseUrl: String,
        val databaseName: String,
        val databaseUseIntegratedSecurity: Boolean,
        val databaseUserName: String,
        val databasePassword: String
)
