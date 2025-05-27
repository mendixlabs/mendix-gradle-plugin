# Extension configuration

The plugin can be configured using the `mendix` extension block in `build.gradle`. In the
extension the following attributes are available

## Extension parameters

| Parameter   | Type   | Description                                                                                                          |
|-------------|--------|----------------------------------------------------------------------------------------------------------------------|
| `mendixVersion` | String | Version string of the Mendix tools and runtime to use. Defaults to the version found in the .mpr file (same as task `mxGetVersion`). |
| `mprFileName` | String | Name of the project file. Defaults to `App.mpr`. |
| `installPath` | String | Set a path where modeler tools and runtime are installed. Optional. |

Example:

```groovy
mendix {
    mendixVersion = "10.11.0.36903"
    mprFileName = "App.mpr"
}
```

## Project parameters

For added flexibility the following parameters can be overwritten using command line arguments.

| Parameter | Command line flag |
| ----      | ----              |
| `mendixVersion` | `mendixVersion` |
| `installPath`   | `mendixInstallPath` |

This can be used as following

```bat
gradlew.bat -PmendixVersion=10.21.1.94969 -mendixInstallPath=c:\runtimes\ mxbuild
```

## Install path format

When `installPath` is configured the Mendix Gradle Plugin expects both modeler tools and 
runtime to be installed in this folder. `installPath` points to the main folder that contains
a folder (or folders) named equally to the provided `mendixVersion`. 

Hence the structure would look like this.

```
${installPath}
  \- ${mendixVersion}
     \- modeler
     \- runtime 
```

On Windows `installPath` defaults to `C:\Program Files\Mendix`. For other operating systems no default is available. 
To construct this folder structure from the CDN artifacts `mxbuild-<version>.targ.z` and `runtime-<version>.tar.gz`
first unzip the runtime. This gives you a folder named with the version like `<version>\runtime`. Then unzip the
mxbuild file into this created folder.
