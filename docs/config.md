# App Configuration

The `mxWriteConfig` exports configuration store in the project to files on HOCON format. It includes all
Constants and Custom settings for the runtime. From the Database tab Type and URL are exported.

For added flexibility, the settings are also made available as environment variable for additional 
flexibility. This allows for Spring Boot like configuration where (most) settings can be controlled as 
environment variable.

```
# Type: String
"LogicModule.DisplayValue" = "Default"
"LogicModule.DisplayValue" = ${?MX_RUNTIME_PARAMS_MICROFLOWCONSTANTS_LOGICMODULE_DISPLAYVALUE}
```

This allows to start an application like this

```bat
SET MX_RUNTIME_PARAMS_MICROFLOWCONSTANTS_LOGICMODULE_DISPLAYVALUE=Changed
start.bat
```
