# Define custom mx tools commands

Using the generic `MxCommand` task custom tasks can be defined in your `build.gradle` file.
The default tasks for `mx` and `mxutil` use it as well.

As specified in the [tasks](tasks.md) document to execute `mx` the following command can be used:

```
gradlew.bat mx --arg=check --arg=App.mpr --output-type=FILE --output-file=check.out
```

With `MxCommand` it is possible to define a your own tasks in `build.gradle`. To translate
the above command to a predefined tasks the following parameters can be used.

| Parameter     | Type            | Description                                                                                                                                  |
|---------------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| `tool`        | Executable name | Name of the tool to execute. The tool must be available in the modeler distribution. The extension `.exe` can be omitted to be OS agnostic.  |
| `args`        | List of String  | Specifies the arguments to be passed to executable defined in `tool`.                                                                        |
| `mendixVersion` | String       | Specifies the Mendix version to use. |
| `watch`       | List of files   | List of files that are watched to help Gradle performing incremental builds.                                                                 | 
| `output-file` | String | Name of the output file.                                                                                                                     |
| `output-type` | String | CONSOLE or FILE                                                                                                                              |

To specify `mxCheck` in your (Groovy) `build.gradle` add this

```
import mendixlabs.mendixgradleplugin.tasks.MxCommand
import mendixlabs.mendixgradleplugin.tasks.OutputType

tasks.register('mxCheck', MxCommand) {
    tool = 'mx'
    args = ["check", mendix.mprFileName.get()]
    mendixVersion = mendix.mendixVersion
    watch.from(project.layout.projectDir.file(mendix.mprFileName.get()))
    outputType = OutputType.FILE
    outputFile = project.layout.buildDir.file("check.out")
}
```

Save the file and execute 

```
gradlew.bat mxCheck
```
