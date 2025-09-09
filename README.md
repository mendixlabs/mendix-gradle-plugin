# Mendix plugin for Gradle

This plugin helps you to work with a Mendix project using Gradle. It provides 
wrappers for the Mendix tools as `mxbuild` and `mx`, making interaction
version agnostic. The plugin also provides run and distribution options. The
plugin works on Windows, Mac and Linux.

Additionally the plugin configures the project as Java application so that IDEs
supporting Gradle, like IntelliJ IDEA, can be used to work with the project.
The configuration adds the project Java source files and necessary dependencies.

## Experimental

This is an experimental project and comes without warranty and support.

This project aims to experiment with the interaction, automation and
distribution of Mendix applications. We welcome feedback on these
aspects that could be addressed by this plugin or in the Mendix platform 
itself.

## Version support for Mendix tools and runtime

This plugin covers two items
* Wrap Mendix CLI tools for easy use
* Build a Mendix app distribution

### CLI tool support

The Gradle plugin supports Mendix CLI tools for various versions and operating
systems. 

| Mendix Version | OS       | Notes                                                                                                                       | 
|----------------|----------|-----------------------------------------------------------------------------------------------------------------------------|
| All            | Windows  | On Windows all versions are supported when they are installed.                                                              |
| \> 9.16        | Linux    | For Linux tools tools are always downloaded from CDN. Starting this version tools are compiled as native Linux executables. |
| \> 9.23        | Windows  | Starting this version Windows tooling can be downloaded from CDN. In case not available locally.                            |
| \> 11.2        | Mac OS   | Studio Pro 11.2 for Mac ships now with CLI tools.                                                                           |

Note: when running Mendix tooling on Linux it depends on system library 
`libuci-dev` to be installed.

### App distribution support

Version 10.11 of Mendix runtime includes a boot file config option 
(see `mxWriteConfig` and `mxRun`) which doesn't depend on external tools 
like M2EE Tools or Docker Build Pack to run an application. 

Starting version 10.11 the `mxRun` and `mxDistZip` commands are supported
to create a Mendix app distribution package. 


## Create or configure a Mendix project

To use the plugin your Mendix project needs to be a Gradle project. To create 
a Gradle project execute `gradle.bat`. On Windows Studio Pro includes a copy
of Gradle, based on th default installation folder the command will be

```bat
"c:\Program Files\Mendix\gradle-8.5\bin\gradle.bat" init --type basic --dsl groovy
```

Answer the values and check if the project is created, `gradlew.bat` and 
`build.gradle` is created.

Open `build.gradle` and add 

```groovy
plugins {
    id "application"
    id "com.mendixlabs.mendix-gradle-plugin" version "0.0.10"
}
 
mendix {
    mendixVersion = "11.2.0.77998"
    mprFileName = "App.mpr"
}
```

See [Extension configuration](docs/extension.md) for plugin configuration details. In case
the Gradle files are added to a folder where a Mendix project already exists, make sure
that `mprFileName` is set to the name of the Mendix project file. In case no MPR file
name is provided `App.mpr` is taken as default.

For a new project initialize with

```bat
gradlew.bat mxInit
```

## Versioning

The plugin uses the `version` value from `gradle.properties` in the build 
and distribution tasks as input to version the artifacts it builds.

## Usage

Use `gradlew.bat tasks` to view which [tasks](docs/tasks.md) are available. It 
is also possible to specify [custom tasks](docs/mxcommand.md).

### Build an app

```bat
gradlew.bat mxbuild
```

### Run an app

```bat
gradlew.bat mxRun
```

### Distribute an app

This generates a ZIP file including all necessary components to run 
a Mendix app. It includes the app, config files, the runtime and 
start scripts.    

```bat
gradlew.bat mxDistZip
```

To see the content of the distribution browse to `build\distributions` and 
open the zip. To run the distribution in this project execute.

```bat
gradlew.bat installMxDist
build\install\<app-name>\bin\start.bat
```

## IDE Support

The plugins configures the project as Java application for IDEs that support
Gradle. Java dependencies are added based on the files present in the `userlib`
and `vendorlib` folder. To manage dependencies use the Java Dependencies option
for the Module Settings inside Studio Pro and then sync the project. Reload 
the project after changing in the IDE to see the changes.

The Java version specified inside the Mendix project is not yet synced. Set the
`java.sourceCompatibility` and `java.targetCompatibility` in the `build.gradle`
for this.

## Developing

This plugin is based on 

* Java 11: This is the Java version supported by Mendix 9 and the early 10 versions.
* Gradle 8.5: Studio Pro ships with this version and hence this version is used as
that is already available on the developers machine. 

To use a development version, check out this project and compile and publish. For Gradle 
make this version available with Java on the PATH or set JAVA_HOME. Then execute the 
following command to publish the plugin into the local maven cache.

```bat
gradlew.bat build publishToMavenLocal
```

To use the development version in a project add the following at the top of 
the `settings.gradle` file:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Then configuration the plugin target project using the version found in the 
`plugin/build.gradle.kts` file.


## License

Copyright 2025 Mendix Technology BV

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
