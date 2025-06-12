# Mendix plugin for Gradle

This plugin helps you to work with a Mendix project using Gradle. It provides 
wrappers for the Mendix tools as `mxbuild` and `mx`, making interaction
version agnostic. The plugin also provides run and distribution options. The
plugin works on Windows and Linux. Mac support will be added when CLI tools
are available.

## Experimental

This is an experimental project and comes without warranty and support.

Goal of this project is to experiment with interaction, automation and
distribution of Mendix application. Please provide feedback on these 
topics that could be addressed by this plugin. 

Feedback on your use-case and experience is welcome though as with this 
project I try to experiment on a different approach on interaction, 
automation and shipping a Mendix app.

## Version support for Mendix tools and runtime

This plugin covers two items
* Wrap Mendix CLI tools for easy use
* Build a Mendix app distribution

### CLI tool support

The Gradle plugin supports Mendix CLI tools for various versions and operating
systems. 

| Mendix Version | OS       | Notes                                                                                                                                            | 
|----------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| All            | Windows  | On Windows all versions are supported when they are installed.                                                                                   |
| \> 9.16        | Linux    | For Linux tools tools are always downloaded from CDN. Starting this version tools are compiled as native Linux executables.                      |
| \> 9.23        | Windows  | Starting this version Windows tooling can be donwloaded from CDN. In case not available locally.                                                 |

Note: when running Mendix tooling on Linux it depends on system library 
`libuci-dev` to be installed.

Note: MacOS is not supported as StudioPro Mac doesn't ship CLI tools.

### App distribution support

Version 10.11 of Mendix runtime includes a boot file config option 
(see `mxWriteConfig` and `mxRun`) which doesn't depend on external tools 
like M2EE Tools or Docker Build Pack to run an application. 

Starting version 10.11 the `mxRun` and `mxDistZip` commands are supported
to create a Mendix app distribution package. 


## Installation

Note: installing the plugin locally is only required when using SNAPSHOT 
(development) versions or an unreleased version. Installing locally will push
the plugins into the local Maven repository cache.

Check out this project and the compile and build. The build needs Java 11 to
compile. For Gradle make this available with Java on the PATH or set JAVA_HOME.
The execute

```bat
gradlew.bat build publishToMavenLocal
```

## Create or configure a Mendix project

To use the plugin your Mendix project needs to be a Gradle project. To create 
a project execute

```bat
gradle.bat init --type basic --dsl groovy
```

Answer the values and check if the project is created, `gradlew.bat` and 
`build.gradle` must exist. 

Now open `settings.gradle` to add a bit of plugin management because the
plugin is only available from the local M2 repo. Add the following at the top
of the file:

```groovy
pluginManagement {
    repositories {
        // mavenLocal() // only for development versions
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Then open `build.gradle` and add 

```groovy
plugins {
    id "application"
    id "distribution"
    id "com.mendixlabs.mendix-gradle-plugin" version "0.0.6"
}
 
mendix {
    mendixVersion = "10.11.0.36903"
    mprFileName = "App.mpr"
}
```

See [Extension configuration](docs/extension.md) for plugin configuration details.

For a new project init the project with

```bat
gradlew.bat mxEnsureModeler mxInit
```

For an existing project use the Mendix version that is used for this project
and the Mendix version of the project. In case you are not sure, use

```bat
gradlew.bat mxGetVersion
```

## Versioning

The plugin uses the `version` value from `gradle.properties` in the build 
and distribution tasks as input.

## Usage

Use `gradlew.bat tasks` to view which [tasks](docs/tasks.md) are available. It 
is also possible to specify [custom tasks](docs/mxcommand.md).

Note: task dependencies are not set for `mxbuild`. Mxbuild takes quite some 
time and this plugin doesn't contain a good way yet to determine if the 
project and its associated files have changed. Therefore it is required to
run `mxbuild` manually when the project source has changed.


### Build an app

```bat
gradlew.bat mxbuild
```

### Run an app

Assumes `mxbuild` is executed.

```bat
gradlew.bat mxEnsureRuntime mxWriteConfigs mxDeployMda mxRun
```

### Distribute an app

Assumes `mxbuild` is executed.

```bat
gradlew.bat mxEnsureRuntime mxWriteConfigs mxStartScripts mxDistZip
```

To see the content of the distribution browse to `build\distributions` and 
open the zip. To run the distribution in this project execute.

```bat
gradlew.bat installMxDist
build\install\<app-name>\bin\start.bat
```

## License

Copyright 2024 Mendix Technology BV

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

The license is included in the project as [LICENSE](LICENSE).  