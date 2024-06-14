# App Configuration

The `mxGenerateConfig` produces a minimal configuration file for Mendix runtime 10.11 and above. The template file used
is this the one below. It configures the runtime with a minimal configuration to start in trial mode with an in-memory,
non-persistent, database.

It contains two variables `%MICROFLOW_CONSTANTS%` and `%SCHEDULED_EVENTS%` which will be replaced during generation
must also be used when providing a custom template. 

```
admin {
  adminPassword = "verysecret1"
}

runtime {
  params {
    DTAPMode = "D"
    DatabaseType = "HSQLDB"
    DatabaseJdbcUrl = "jdbc:hsqldb:mem:embeddedDataSource"

    MicroflowConstants {
%MICROFLOW_CONSTANTS%
    }

    MyScheduledEvents = "%SCHEDULED_EVENTS%"
    ScheduledEventExecution = SPECIFIED
  }
}

logging = [{
    name = stdout
    type = console
    autoSubscribe = INFO
    levels = {}
}]
```

For added flexibility, the App Constants are generated like this allowing the configuration to be overwritten by
environment variables like Spring Boot allows to. E.g. your custom template could use the mechanism also for other 
configuration entries.

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
