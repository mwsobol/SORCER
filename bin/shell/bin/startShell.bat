if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
set JAVACMD=
set JAVACMD=%JAVA_HOME%\bin\java.exe
goto endOfJavaHome
:noJavaHome
set JAVACMD=java.exe
:endOfJavaHome

rem echo %JAVACMD%
SET mypath=%~dp0
SET SHOME_BIN=%mypath:~0,-1%
IF NOT DEFINED SORCER_HOME (
    IF EXIST "%SHOME_BIN%\startShell.bat" (
        SET SORCER_HOME=%SHOME_BIN%\..\..\..
    ) ELSE (
        ECHO Problem setting SORCER_HOME, please set this variable and point it to the main SORCER installation directory!
    )
)


rem Create DataService Root Dir if it doesn't exist
SET DATASERVICE_ROOT_DIR=%TEMP%\sorcer-%USERNAME%\data

IF NOT EXIST "%DATASERVICE_ROOT_DIR%" (
    ECHO "Creating DATASERVICE ROOT Directory: %DATASERVICE_ROOT_DIR%"
    mkdir "%DATASERVICE_ROOT_DIR%"
)

rem read sorcer.env to get the local repo location
rem FOR /F "usebackq tokens=1,2 delims==" %%G IN ("%SORCER_HOME%\configs\sorcer.env") DO (set %%G=%%H)
rem set MVN_REPO=%sorcer.local.repo.location%

rem set "USER_HOME=%HOMEDRIVE%%HOMEPATH%"

rem SETLOCAL EnableDelayedExpansion
rem set MVN_REPO=!MVN_REPO;${user.home}=%USER_HOME%!
rem set MVN_REPO=%MVN_REPO;/=\%
rem IF NOT DEFINED MVN_REPO SET "MVN_REPO=%HOMEDRIVE%%HOMEPATH%\.m2\repository"
rem ENDLOCAL & SET MVN_REPO=%MVN_REPO%
SET MVN_REPO=%HOMEDRIVE%%HOMEPATH%\.m2\repository

set LIB_DIR=%SORCER_HOME%\lib

FOR /F "usebackq tokens=1,2 delims==" %%G IN ("%SORCER_HOME%\configs\versions.properties") DO (set %%G=%%H)

IF NOT DEFINED RIO_HOME (
    SET RIO_HOME=%SORCER_HOME%\rio-%rio.version%
)

set JINI_BASE=
set JINI_BASE=%JINI_BASE%;%LIB_DIR%\river\jsk-platform-%river.version%.jar
set JINI_BASE=%JINI_BASE%;%LIB_DIR%\river\jsk-lib-%river.version%.jar
set JINI_BASE=%JINI_BASE%;%LIB_DIR%\river\serviceui-%river.version%.jar

set SORCER_COMMON=
set SORCER_COMMON=%SORCER_COMMON%;%LIB_DIR%\common\groovy\groovy-all-%groovy.version%.jar
set SORCER_COMMON=%SORCER_COMMON%;%LIB_DIR%\common\plexus-utils-%plexus.version%.jar
set SORCER_COMMON=%SORCER_COMMON%;%LIB_DIR%\common\jansi-%jansi.version%.jar
set SORCER_COMMON=%SORCER_COMMON%;%LIB_DIR%\common\commons-io-%commonsio.version%.jar
set SORCER_COMMON=%SORCER_COMMON%;%LIB_DIR%\common\guava-%guava.version%.jar



set RIO_CLASSPATH=
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\rio-platform-%rio.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\rio-lib-%rio.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\rio-start-%rio.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib-dl\rio-api-%rio.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib-dl\monitor-api-%rio.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\logging\slf4j-api-%slf4j.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\logging\logback-core-%logback.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\logging\logback-classic-%logback.version%.jar
set RIO_CLASSPATH=%RIO_CLASSPATH%;%RIO_HOME%\lib\logging\jul-to-slf4j-%slf4j.version%.jar

set SORCER_PATH=
set SORCER_PATH=%SORCER_PATH%;%LIB_DIR%\sorcer\lib\sorcer-resolving-loader-%sorcer.version%.jar
set SORCER_PATH=%SORCER_PATH%;%LIB_DIR%\sorcer\lib\sorcer-platform-%sorcer.version%.jar
set SORCER_PATH=%SORCER_PATH%;%LIB_DIR%\sorcer\lib\sos-shell-%sorcer.version%.jar
set SORCER_PATH=%SORCER_PATH%;%LIB_DIR%\sorcer\lib-ext\webster-%sorcer.version%.jar


set SHELL_CLASSPATH=
set SHELL_CLASSPATH=%JINI_BASE%;%SORCER_COMMON%;%SORCER_PATH%;%RIO_CLASSPATH%
 
rem Determine webster url
IF "%provider.webster.interface%"=="${localhost}" (
   SET "provider.webster.interface=%COMPUTERNAME%"
)

IF DEFINED %SORCER_WEBSTER_INTERFACE% IF DEFINED %SORCER_WEBSTER_PORT% (
   SET "WEBSTER_URL=http://%SORCER_WEBSTER_INTERFACE%:%SORCER_WEBSTER_PORT%"
) ELSE
   SET "WEBSTER_URL=http://%provider.webster.interface%:%provider.webster.port%"
)

IF NOT DEFINED RIO_HOME SET RIO_HOME=%SORCER_HOME%\rio-%rio.version%
set JAVA_OPTS=
set JAVA_OPTS=-Dsun.net.maxDatagramSockets=1024
set JAVA_OPTS=%JAVA_OPTS% -Dsorcer.env.file="%SORCER_HOME%\configs\sorcer.env"
set JAVA_OPTS=%JAVA_OPTS% -Djava.net.preferIPv4Stack=true
set JAVA_OPTS=%JAVA_OPTS% "-Djava.protocol.handler.pkgs=net.jini.url|sorcer.util.url|org.rioproject.url"
set JAVA_OPTS=%JAVA_OPTS% -Dlogback.configurationFile="%SORCER_HOME%\bin\shell\configs\shell-logging.groovy"
set JAVA_OPTS=%JAVA_OPTS% -Djava.rmi.server.useCodebaseOnly=false
set JAVA_OPTS=%JAVA_OPTS% -Dwebster.tmp.dir="%SORCER_HOME%\data"
set JAVA_OPTS=%JAVA_OPTS% -Dwebster.put.dir="%SORCER_HOME%\data"
set JAVA_OPTS=%JAVA_OPTS% -Dsorcer.version="%sorcer.version%"
set JAVA_OPTS=%JAVA_OPTS% -Dmodeling.version="%modeling.version%"
set JAVA_OPTS=%JAVA_OPTS% -Dsorcer.home="%SORCER_HOME%"
set JAVA_OPTS=%JAVA_OPTS% -DSORCER_HOME="%SORCER_HOME%"
set JAVA_OPTS=%JAVA_OPTS% -Drio.home="%RIO_HOME%"
set JAVA_OPTS=%JAVA_OPTS% -DRIO_HOME="%RIO_HOME%"

REM Turn on debugging if DEBUG is set in env
IF DEFINED DEBUG (
  SET JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000
)
rem ECHO %WEBSTER_URL%
rem ECHO %SHELL_CLASSPATH%
