# Tasks

These are the tasks available in the plugin. Run `gradlew.bat tasks` to get a short description. Customizing tasks can 
be done by reconfiguring them in the Gradle build file. The following examples changes the `mxbuild` task. This 
document described for every task the possible parameters. For extension parameters refer to 
[Extension Configuration](extension.md).

```groovy
tasks.named("mxbuild") {
   looseVersionCheck = true
   generateSbom = true
}
```

## `mxGetVersion`

Queries the current Mendix version from the configured project (`.mpr`) file. This works on all MPR files and doesn't 
need tooling. 

`build.gradle`:

```groovy
{
    mprFileName = "App.mpr"
}
```

Execute

```bat
gradlew.bat mxGetVersion
```

## `mxListVersions`

Lists available Mendix versions. 

The command allows to set a filter which is a prefix filter. E.g. `10` or `10.` will
return all MX 10.x.x.x versions. Filtering with `10.6` will return all MX 10.6.x.x 
versions.

Execute

```
gradlew.bat mxListVersions --filter="10." --onlyLastPatch
```

Parameters:

| Parameter     | Type      | Description                                              |
|---------------|-----------|----------------------------------------------------------|
| `filter` | String | Version string prefix filter.                            |
| `listingFile` | String    | Location of `listing.txt` on CDN.                        |
| `onlyLastPatch` | Boolean | When true only the last patch version of major.minor is returned. |

## `mxInit`

Creates a new project using `mx(.exe)` using the specified Mendix version and project name from `build.gradle`.
Note that if the Mendix version is not specified in the plugin configuration, it must be specified using the `-PmendixVersion=<version>` command line flag.

`build.gradle`:

```groovy
{
    mprFileName = "App.mpr"
    mendixVersion = "10.11.0.0000"
}
```

Execute

```bat
gradlew.bat mxInit
```

Parameters:

| Parameter       | Type      | Description                                                                                 |
|-----------------|-----------|---------------------------------------------------------------------------------------------|
| `mprFilename`   | String    | Name of the filename to create. Defaults to `mprFilename` from `build.gradle`.              |
| `mendixVersion` | String    | Mendix version to create the project with. Defaults to `mendixVersion` from `build.gradle`. |


## `mx`

Runs `mx(.exe)` for the specified Mendix version with the specified parameters. This task can be ran directly from the
command line or be used to register a reusable task in the projects `build.gradle`.

Execute

```bat
gradlew.bat mx --arg=--help
```

Multiple `--arg` can be specified for every argument to be passed to `mx`. For large outputs (like `dump-mpr`) the
output can be redirected to a file. 

```bat
gradlew.bat mx --arg=check --arg=App.mpr --output-type=FILE --output-file=check.out
```

Parameters:

| Parameter     | Type            | Description                                   |
|---------------|-----------------|-----------------------------------------------|
| `args`        | List of String  | Specifies the arguments to be passed to `mx`. |
| `output-file` | String | Name of the output file. |
| `output-type` | String | CONSOLE or FILE |


## `mxutil`

Runs `mxutil(.exe)` for the specified Mendix version with the specified parameters. Like `mx` this task accepts 
`--arg` flags to control the options.

Execute

```bat
gradlew.bat mxutil --arg=--help
```

Parameters:

| Parameter     | Type            | Description                                   |
|---------------|-----------------|-----------------------------------------------|
| `args`        | List of String  | Specifies the arguments to be passed to `mx`. |
| `output-file` | String | Name of the output file. |
| `output-type` | String | CONSOLE or FILE |

## `mxbuild`

Runs `mxbuild(.exe)` for the specified Mendix version and project file. It produces a MDA file in the folder `${project.builddir}/app/`. The execution can be controlled using these parameters.

The `mxbuild` tasks supports incremental builds and uses the state of the MPR file for this. Compiles only happen when the MPR file is changed or the mda doesn't exist yet.

Execute

```bat
gradlew.bat mxbuild
```

Parameters:

| Parameter               | Type    | Description                                                                                                                 |
|-------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------|
| `mpr`                   | String  | Filename of the projects `.mpr` file. Defaults to `mprFilename` from `build.gradle`.                                        |
| `mendixVersion`         | String  | Mendix version used to run the command. Defaults to `mendixVersion` from `build.gradle`.                                    |
| `generateJavaDebugInfo` | Boolean | Instructs `mxbuild` to compile with Java debug info. Defaults to `false`.                                                   |
| `generateSbom`          | Boolean | Instructs `mxbuild` to create a SBOM. When set the filename will be `${project.name}-sbom.json`. Defaults to `false`.       |
| `looseVersionCheck`     | Boolean | Instructs `mxbuild` not to restrict on the MPRs Mendix version. Defaults to `false`.                                        |
| `projectFiles`          | File collection | Specifies which files to watch to support incremental builds of the MDA.                                            | 
| `writeErrorsFile`       | Boolean | Instructs `mxbuild` not write errors into a file. When set filename will be `${project.name}-errors.json`. Defaults to `false`. |


Note: When the Mendix version is overwritten using the `-PmendixVersion` command line flag, the `mxbuild` task will most like need to be configured to support loose version checks. This can be done by changing the configuration in `build.gradle` like this:

```groovy
tasks.named("mxbuild") {
    looseVersionCheck = true
}
```

## `mxWriteConfigs`

Extracts project configuration (Settings -> Configurations) into HOCON files that can be 
passed to runtime on start. 

Execute:

```bat
gradlew.bat mxWriteConfigs
```

Parameters:

| Parameter    | Type    | Description                                                                                    |
|--------------|---------|------------------------------------------------------------------------------------------------|
| `configNames` | Set<String> | Set of config names that should be exported. When empty all configs are exported.         |
| `mda`        | String  | Filename mda that is produced by `mxbuild`.                                                    |
| `mprAsJson`  | File    | File name to the project in JSON as result of mxDumpMpr.                                       
| `outputFile` | String | Filename of the config file to be written. Defaults to `app.conf` in the same folder as the MDA. |


## `mxGenerateDockerfile`

Generates a Dockerfile for the current project. It will use the distribution created by `mxDistZip` as input and create
a simple container. See [Building a Docker container](docker.md) for more information. 

Execute:

```bat
gradlew.bat mxGenerateDockerfile
```

Parameters:

| Parameter      | Type    | Description                                                                                      |
|----------------|---------|--------------------------------------------------------------------------------------------------|
| `baseImage`    | String  | The docker base image to use. Defaults to `eclipse-temurin:21.0.3_9-jre-jammy`.                  |
| `templateFile` | String | Provide a template Dockerfile.                                                                   |
| `outputFile`   | String | Filename of the Dockerfile file to be written. Defaults to `docker/Dockerfile` in the build dir. |


## `mxDeployMDA`

Unzips the MDA so that it can be ran using `mxRun`. Assumes `mxbuild` is executed.

Execute:

```bat
gradlew.bat mxDeployMDA
```

Parameters: None

## `mxRun`

Runs the application using the generated configuration file and unzipped MDA. This is an extension of the
JavaExec task. Parameters are preconfigured with default to run the app with default configuration. The 
task is extended with a `configFile` parameter for easy use.  

**NOTE**: Don't use the `args` parameter. The value is set automatically.

Execute:

```bat
gradlew.bat mxRun
```

Parameters: See [JavaExec](https://docs.gradle.org/8.5/dsl/org.gradle.api.tasks.JavaExec.html)

| Parameter   | Type    | Description                                                                                   |
|-------------|---------|-----------------------------------------------------------------------------------------------|
| `appFolder`| Directory | The directory where the compiled app is stored.        |
| `configFile` | File  | The configuration file to use. Defaults to `Default.conf` as generated from the source. |
| `classpath` | String  | Set to `runtimelauncher.jar` from the Runtime distribution.                                   |
| `jvmArgs`   | String | Defines JVM arguments. Needs to have `MX_INSTALL_PATH` at minimal.                            |
| `args`      | String | Content is set based on config options `appFolder` and `configFile`. Don't use this argument. |


## `mxStartScripts`

Creates start scripts for the distribution in `${project.builddir}/scripts`.

Execute

```bat
gradlew.bat mxStartScripts
```

## `mxDistZip`

Creates a ZIP distribution of the application that contains the project, the runtime and config file. Uses Gradles
distribution tasks with custom configuration. 

Execute

```bat
gradlew.bat mxDistZip
```

Parameters: See [Distribution](https://docs.gradle.org/8.5/dsl/org.gradle.api.distribution.Distribution.html).

## `mxEnsureModeler`

Task to download and unpack the Modeler tools needed for this project. Uses the configured `mendixVersion` from 
`build.gradle` as default. This makes tools as `mxbuild` available. 

Execute

```bat
gradlew.bat mxEnsureModeler
```

## `mxEnsureRuntime`

Task to download and unpack the runtime needed for this project. Uses the configured `mendixVersion` from 
`build.gradle` as default. The runtime is needed to build a distribution or run the app.

Execute

```bat
gradlew.bat mxEnsureRuntime
```

## mxDumpMpr

Preconfigured task for `mx.exe dump-mpr` on the specified projects MPR file. JSON file is stored
at `build/mendix/<mpr-name>.json`
