/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package mendixlabs.mendixgradleplugin

import mendixlabs.mendixgradleplugin.mendix.GetAppVersion
import mendixlabs.mendixgradleplugin.tasks.*
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

abstract class MxGradlePluginExtension(private val project: Project) {
    abstract val mendixVersion: Property<String>
    abstract val mprFileName: Property<String>
    abstract val installPath: Property<String>

    init {            
        mprFileName.convention("App.mpr")
        mendixVersion.convention(
            mprFileName.map { e ->
                if (project.hasProperty("mendixVersion")) {
                    project.properties.get("mendixVersion").toString()
                } else {
                    GetAppVersion(project.layout.projectDirectory.file(e).asFile)
                }
            }
        )
        installPath.convention(
            project.provider {
                if (project.hasProperty("mendixInstallPath")) {
                    project.properties.get("mendixInstallPath").toString()
                } else {
                    ""
                }
            }
        )

    }

}

const val PLUGIN_GROUP_MX = "mendix"

/**
 * Mendix Gradle Plugin
 *
 * Interact with your Mendix project using Gradle commands. The
 * plugin handles getting the correct version installed on the
 * machine when not available and can build a self-contained
 * distribution.
 *
 */
@Suppress("unused")
class MendixGradlePlugin: Plugin<Project> {

    private val appBuildDir = "app"

    override fun apply(project: Project) {
        // add plugin and configure the project
        val extension = project.extensions.create("mendix", MxGradlePluginExtension::class.java, project)

        registerTasks(project, extension)
        addJavaProjectConfig(project, extension);
    }

    /*
     * Adds Java plugin and configuration to the project so that
     * Mendix Java sources and dependencies are included.
     */
    private fun addJavaProjectConfig(project: Project, extension: MxGradlePluginExtension) {
        project.plugins.apply("java")

        // Configure Java source sets to include Mendix javasource directory
        project.extensions.getByType(SourceSetContainer::class.java).named("main") {
            it.java.srcDir(project.layout.projectDirectory.dir("javasource"))
        }
        // include libraries from userlib and vendorlib
        project.dependencies.add("implementation", project.fileTree("userlib") {
            it.include("**/*.jar")
        })
        // TODO: replace vendorlib folder scanning with reading specified dependencies from the MPR file
        project.dependencies.add("implementation", project.fileTree("vendorlib") {
            it.include("**/*.jar")
        })
        // register Mendix runtime API libraries as dependencies
        project.dependencies.add("implementation", project.provider {
            val toolFinder = ToolFinderBuilder()
                .withMendixVersion(extension.mendixVersion.get())
                .withProject(project)
                .build()
            val runtimeLocation = toolFinder.getRuntimeLocation()

            val mendixLibs = arrayOf(
                "com.mendix.json.jar",
                "com.mendix.logging-api.jar",
                "com.mendix.m2ee-api.jar",
                "com.mendix.public-api.jar",
                "org.eclipse.jetty.toolchain.jetty-servlet-api.jar",
                "org.eclipse.jetty.toolchain.jetty-javax-websocket-api.jar")
                .map { e -> File(runtimeLocation, "bundles/${e}") }
                .filter { it.exists() }

            project.files(mendixLibs)
        })
    }

    fun registerTasks(project: Project, extension: MxGradlePluginExtension) {
        // -------------------------------------------------------------------------------------------------------------
        // Project helpers
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<GetMendixVersion>("mxGetVersion", GetMendixVersion::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Read the Mendix version from the projects MPR file."

            task.mprFilename.set(project.layout.projectDirectory.file(extension.mprFileName))
        }

        project.tasks.register<ListVersions>("mxListVersions", ListVersions::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "List available Mendix versions"
        }

        // -------------------------------------------------------------------------------------------------------------
        // Wrap mx tooling
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<MxCommand>("mx", MxCommand::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Call mx(.exe) for configured mendixVersion."

            task.dependsOn("mxEnsureModeler")
            task.outputs.upToDateWhen { false }

            task.tool.set("mx")
            task.mendixVersion.set(extension.mendixVersion)
        }

        project.tasks.register<MxCommand>("mxutil", MxCommand::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Call mxutil(.exe) for configured mendixVersion."

            task.dependsOn("mxEnsureModeler")
            task.outputs.upToDateWhen { false }

            task.tool.set("mxutil")
            task.mendixVersion.set(extension.mendixVersion)
        }

        project.tasks.register<Mxbuild>("mxbuild", Mxbuild::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Call mxbuild(.exe) for configured mendixVersion."

            task.dependsOn("mxEnsureModeler")

            task.mendixVersion.set(extension.mendixVersion)
            task.mpr.set(project.layout.projectDirectory.file(extension.mprFileName))

            // watch these files and folders to make mxbuild incremental
            // file list based on Mendix 10
            task.projectFiles.from(
                    project.layout.projectDirectory.files(
                            extension.mprFileName.get(),
                            "javascriptsource",
                            "javasource",
                            "mlsource",
                            "modules",
                            "ressources",
                            "templates",
                            "theme",
                            "themesource",
                            "userlib",
                            "widgets"
                    )
            )
            task.outputPath.set(project.layout.buildDirectory.dir(appBuildDir))

            task.doLast {
                // create a .files file to copy it into the distribution later,
                // without it the Mendix app won't start.
                project.layout.buildDirectory.file("${appBuildDir}/.files").get().asFile.createNewFile()
            }
        }

        // -------------------------------------------------------------------------------------------------------------
        // Convenience tasks
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<MxCommand>("mxInit", MxCommand::class.java) { task ->
            val appName = extension.mprFileName.map { s -> s.replace(".mpr", "") }

            task.group = PLUGIN_GROUP_MX
            task.description = "Create a new Mendix project using the available 'mendix' config."

            task.dependsOn("mxEnsureModeler")

            task.outputs.upToDateWhen { project.file(extension.mprFileName).exists() }

            task.tool.set("mx")
            task.mendixVersion.set(extension.mendixVersion)
            task.args.set(listOf("create-project",
                    "--app-name", appName.get(),
                    "--output-dir", project.rootDir.absolutePath))
        }

        project.tasks.register<MxCommand>("mxDumpMpr", MxCommand::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Dump MPR to JSON"

            task.dependsOn("mxEnsureModeler")

            task.tool.set("mx")
            task.mendixVersion.set(extension.mendixVersion)
            task.watch.from(project.layout.projectDirectory.file(extension.mprFileName))

            // make a list of providers so that extension.mprFileName evaluates late
            val args = project.objects.listProperty(String::class.java)
            args.add("dump-mpr")
            args.add(extension.mprFileName)
            task.args.set(args)

            task.outputType.set(OutputType.FILE)
            task.outputFile.set(project.layout.buildDirectory.file("${appBuildDir}/${project.name}.json"))
        }

        project.tasks.register<GenerateDockerFile>("mxGenerateDockerfile", GenerateDockerFile::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Generate a Dockerfile for this project."

            task.dependsOn("installMxDist")
        }

        // -------------------------------------------------------------------------------------------------------------
        // Tasks for operations on project
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<WriteConfigs>("mxWriteConfigs", WriteConfigs::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Write config files based on configuration in the project MPR"

            task.dependsOn("mxbuild", "mxDumpMpr")

            val mprJson = project.tasks.getByName("mxDumpMpr").outputs.files.singleFile
            task.mda.set(project.layout.buildDirectory.file("${appBuildDir}/${project.name}.mda"))
            task.mprAsJson.set(mprJson)
            task.outputPath.set(project.layout.buildDirectory.dir(appBuildDir))
        }

        project.tasks.register<Copy>("mxDeployMda", Copy::class.java) {task ->
            task.from(project.zipTree(project.layout.buildDirectory.file("${appBuildDir}/${project.name}.mda")))
            task.into(project.layout.buildDirectory.dir("deployment"))

            task.dependsOn("mxbuild", "mxWriteConfigs")

            task.doLast {
                project.mkdir(project.layout.buildDirectory.dir("deployment/data/files"))
            }
        }

        project.tasks.register<MxRun>("mxRun", MxRun::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Run the Mendix project"

            task.dependsOn("mxEnsureRuntime", "mxWriteConfigs", "mxDeployMda")

            task.classpath(project.provider {
                val toolFinder = ToolFinderBuilder()
                    .withMendixVersion(extension.mendixVersion.get())
                    .withProject(project)
                    .build()
                "${toolFinder.getRuntimeLocation()}/launcher/runtimelauncher.jar"

            })
            task.jvmArguments.set(project.provider {
                val toolFinder = ToolFinderBuilder()
                    .withMendixVersion(extension.mendixVersion.get())
                    .withProject(project)
                    .build()
                listOf("-DMX_INSTALL_PATH=${toolFinder.getRuntimeLocation()}${File.separator}..")
            })

            task.appFolder.set(project.tasks.getByName("mxDeployMda").outputs.files.singleFile)
            task.configFile.set(project.layout.buildDirectory.file("${appBuildDir}${File.separator}Default.conf"))
        }

        // -------------------------------------------------------------------------------------------------------------
        // Download and unpack Mxbuild tasks
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<CDNDownload>("mxInternalDownloadMxbuild", CDNDownload::class.java) { task ->
            task.distribution.set(DistributionType.MODELER)
            task.mendixVersion.set(project.provider { extension.mendixVersion.get() })

            // we'll assume that a project is only used on one operating system
            task.dest.set(project.layout.buildDirectory.file(
                extension.mendixVersion.map { e -> "modeler/${constructMxbuildFilename(DistributionType.MODELER, e)}" }
            ))
        }

        project.tasks.register<Copy>("mxInternalUnpackMxbuild", Copy::class.java) { task ->
            task.dependsOn("mxInternalDownloadMxbuild")

            task.from(project.provider {
                val mxbuildDistributionFile = project.tasks.getByName("mxInternalDownloadMxbuild").outputs.files.singleFile
                project.tarTree(project.resources.gzip(mxbuildDistributionFile))
            })
            task.into(project.provider {
                project.layout.buildDirectory.dir("modeler/${extension.mendixVersion.get()}").get()
            })
        }

        project.tasks.register("mxEnsureModeler") { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Get MxBuild if configured version is not available locally."

            task.dependsOn(project.provider {
                val mxVersion = extension.mendixVersion.get()
                if (mxVersion.isEmpty()) {
                    task.logger.lifecycle("No Mendix version configured, skipping modeler check.")
                    return@provider emptyList<String>()
                }

                val toolFinder = ToolFinderBuilder()
                    .withMendixVersion(mxVersion)
                    .withProject(project)
                    .build()
                val installed = toolFinder.isModelerInstalled()
                task.logger.info("modeler is installed: ${installed}")
                if (!installed) {
                    return@provider "mxInternalUnpackMxbuild"
                }
                return@provider emptyList<String>()
            })
            task.mustRunAfter("mxInternalUnpackMxbuild")
        }

        // -------------------------------------------------------------------------------------------------------------
        // Download and unpack Runtime tasks
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<CDNDownload>("mxInternalDownloadRuntime", CDNDownload::class.java) { task ->
            task.distribution.set(DistributionType.RUNTIME)
            task.mendixVersion.set(extension.mendixVersion)

            task.dest.set(project.layout.buildDirectory.file(
                extension.mendixVersion.map { e -> "modeler/${constructMxbuildFilename(DistributionType.RUNTIME, e)}" }
            ))

        }

        project.tasks.register<Copy>("mxInternalUnpackRuntime", Copy::class.java) { task ->
            task.dependsOn("mxInternalDownloadRuntime")
            task.mustRunAfter("mxInternalUnpackMxbuild")

            task.from(project.provider {
                val runtimeDistributionFile = project.tasks.getByName("mxInternalDownloadRuntime").outputs.files.singleFile
                project.tarTree(project.resources.gzip(runtimeDistributionFile))
            })
            task.into(project.provider {
                // no need to pass to add the version to the path as the runtime distribution root
                // is the version value
                project.layout.buildDirectory.dir("modeler").get()
            })
        }

        project.tasks.register("mxEnsureRuntime").configure { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Get Runtime if configured version is not available locally."

            task.dependsOn(project.provider {
                val mxVersion = extension.mendixVersion.get()
                if (mxVersion.isEmpty()) {
                    task.logger.lifecycle("No Mendix version configured, skipping runtime check.")
                    return@provider emptyList<String>()
                }

                val toolFinder = ToolFinderBuilder()
                    .withMendixVersion(mxVersion)
                    .withProject(project)
                    .build()
                val installed = toolFinder.isRuntimeInstalled()
                task.logger.lifecycle("runtime is installed: ${installed}")
                if (!installed) {
                    return@provider "mxInternalUnpackRuntime"
                }
                return@provider emptyList<String>()
            })
            task.mustRunAfter("mxInternalUnpackRuntime")
        }


        // -------------------------------------------------------------------------------------------------------------
        // Configure the distribution plugin
        // -------------------------------------------------------------------------------------------------------------
        project.tasks.register<GenerateStartScripts>("mxStartScripts", GenerateStartScripts::class.java) { task ->
            task.group = PLUGIN_GROUP_MX
            task.description = "Generate start scripts for the Mendix app."
        }

        // adding a custom distribution creates tasks
        // mxDistZip
        // mxDistTar
        // installMxDist
        project.plugins.apply("distribution")

        val distributions = project.extensions.getByType(DistributionContainer::class.java)
        distributions.register("mx") { dist ->
            dist.distributionBaseName.set(project.name)

            dist.contents { spec ->
                spec.into("app") { appSpec ->
                    appSpec.from(project.zipTree(project.layout.buildDirectory.file("${appBuildDir}/${project.name}.mda")))
                }

                // make sure we can actually run this app
                spec.into("app/data/files") { dataSpec ->
                    dataSpec.from(project.layout.buildDirectory.file("app/.files"))
                }

                spec.into("bin") { binSpec ->
                    binSpec.from(project.layout.buildDirectory.dir("scripts"))
                }

                spec.into("etc") { etcSpec ->
                    etcSpec.from(project.tasks.getByName("mxWriteConfigs").outputs.files.singleFile) {
                        it.include { f -> f.file.name.endsWith(".conf")}
                    }
                }

                spec.into("lib/runtime") { runtimeSpec ->
                    val runtimeDir = extension.mendixVersion.map { version ->
                        val toolFinder = ToolFinderBuilder()
                            .withMendixVersion(version)
                            .withProject(project)
                            .build()
                        toolFinder.getRuntimeLocation()
                    }
                    runtimeSpec.from(runtimeDir)
                }
            }
        }

        project.tasks.named("mxDistZip") { task ->
            task.dependsOn("mxEnsureRuntime", "mxbuild", "mxStartScripts", "mxWriteConfigs")
        }

        project.tasks.named("installMxDist") { task ->
            task.dependsOn("mxEnsureRuntime", "mxbuild", "mxStartScripts", "mxWriteConfigs")
        }

        listOf("mxDistTar", "startScripts").forEach { e -> project.tasks.named(e) { task -> task.enabled = false} }
    }

}
