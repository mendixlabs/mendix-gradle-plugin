# Tasks

These are the tasks available in the plugin. Run `gradlew.bat tasks` to get a short description. Customizing tasks can 
be done by reconfiguring them in the Gradle build file like the following example. Change `mxbuild` for the task that 
is configured. This document described for every task the possible parameters.

```groovy
tasks.named("mxbuild") {
   looseVersionCheck = true
   generateSbom = true
}
```

Any tasks that accepts `mendixVersion` can be overwritten with a project parameter `-PmendixVersion=version` on the
command line.  


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

## `mxInit`

Creates a new project using `mx(.exe)` using the specified Mendix version and project name from `build.gradle`.

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

Runs `mxbuild(.exe)` for the specified Mendix version and project file. It produces a MDA file in the folder
`${project.builddir}/app/`. The execution can be controlled using these parameters.

The `mxbuild` tasks supports incremental builds and uses the state of the MPR file for this. Compiles only
happen when the MPR file is changed or the mda doesn't exist yet.

Execute

```bat
gradlew.bat mxbuild
```

Parameters:

| Parameter               | Type    | Description                                                                                                                     |
|-------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------|
| `mpr`                   | String  | Filename of the projects `.mpr` file. Defaults to `mprFilename` from `build.gradle`.                                            |
| `mendixVersion`         | String  | Mendix version used to run the command. Defaults to `mendixVersion` from `build.gradle`.                                        |
| `generateJavaDebugInfo` | Boolean | Instructs `mxbuild` to compile with Java debug info. Defaults to `false`.                                                       |
| `generateSbom`          | Boolean | Instructs `mxbuild` to create a SBOM. When set the filename will be `${project.name}-sbom.json`. Defaults to `false`.           |
| `looseVersionCheck`     | Boolean | Instructs `mxbuild` not to restrict on the MPRs Mendix version. Defaults to `false`.                                            |
| `writeErrorsFile`       | Boolean | Instructs `mxbuild` not write errors into a file. When set filename will be `${project.name}-errors.json`. Defaults to `false`. |

## `mxGenerateConfig`

Generates a config file that can be used for Mendix 10.11 and higher to start the application. Relies on `mxbuild` to 
have ran. A generic config file will be created containing the App Constants and Scheduled Events specific for this
project. See [App Configuration](config.md) for more information.

Execute:

```bat
gradlew.bat mxGenerateConfig
```

Parameters:

| Parameter    | Type    | Description                                                                                    |
|--------------|---------|------------------------------------------------------------------------------------------------|
| `mda`        | String  | Filename mda that is produced by `mxbuild`.                                                    |
| `templateFile` | String | Provide a different template file to the config file.                                          |
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

Runs the application using the generated configuration file and unzipped MDA. This is an implementation of the Gradle
`JavaExec` task. It assumes `mxbuild` is executed.

Execute:

```bat
gradlew.bat mxRun
```

Parameters: See [JavaExec](https://docs.gradle.org/8.5/dsl/org.gradle.api.tasks.JavaExec.html)

| Parameter   | Type    | Description                                                        |
|-------------|---------|--------------------------------------------------------------------|
| `classpath` | String  | Set to `runtimelauncher.jar` from the Runtime distribution.        |
| `jvmArgs`   | String | Defines JVM arguments. Needs to have `MX_INSTALL_PATH` at minimal. |
| `args`      | String | Passed the MDA deployment folder and config file.                  |


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
