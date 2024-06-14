@echo off
@rem ##########################################################################
@rem Mendix startup script for Windows
@rem
@rem Usage:
@rem   start.bat [CONFIG]
@rem
@rem   CONFIG      Optional path to config file. Defaults to etc\app.conf.
@rem
@rem The following environment variables can be set
@rem   JAVA_HOME   Path to the JRE installation
@rem   JAVA_OPTS   Specify Java command line option
@rem
@rem ##########################################################################

@rem Setting base values
set SCRIPT_DIR=%~dp0
set BASE_DIR=%SCRIPT_DIR%..

@rem Processing config param
set CFG_PARAM=%~dpnx1
IF not "%CFG_PARAM%"==""  (
  set CONFIG=%CFG_PARAM%
) else (
  set CONFIG=%BASE_DIR%\etc\app.conf
)

@rem Check if Java is available
if defined JAVA_HOME (
    set JAVA=%JAVA_HOME%\bin\java
)
if not exist %JAVA% (
  echo "java can't be found on JAVA_HOME"
  exit /b 1
)
goto endjavacheck

java -version >NUL 2>&1
if %ERRORLEVEL% neq 0 (
    echo "java can't be found"
    exit /b 1
)
:endjavacheck

@rem Execute app
"%JAVA%" %JAVA_OPTS% -DMX_INSTALL_PATH=%BASE_DIR% -jar %BASE_DIR%\runtime\launcher\runtimelauncher.jar %BASE_DIR%\app %CONFIG%
