@echo off
pushd
SET mypath=%~dp0
SET SHOME_BIN=%mypath:~0,-1%
IF NOT DEFINED SORCER_HOME (
    IF EXIST "%SHOME_BIN%\sorcer-boot.bat" (
        SET SORCER_HOME=%SHOME_BIN%\..
    ) ELSE (
        ECHO Problem setting SORCER_HOME, please set this variable and point it to the main SORCER installation directory!
    )
)

IF NOT DEFINED SORCER_HOME ( 
  if exist "%CD%\shell\bin\startShell.bat" (
    call common-run.bat
  ) ELSE (
    call "%CD%\shell\bin\startShell.bat"
  )
) ELSE (
  call "%SORCER_HOME%\shell\bin\startShell.bat"
)
rem Use SORCER default if still not found
IF NOT DEFINED NSH_CONF SET NSH_CONF=%SORCER_HOME%\configs\shell\configs\nsh-start.config
rem Use the user nsh start-config file if exists.
IF EXIST "%HOMEDRIVE%%HOMEPATH%\.nsh\configs\nsh-start.config" SET NSH_CONF=%HOMEDRIVE%%HOMEPATH%\.nsh\configs\nsh-start.config

set STARTER_MAIN_CLASS=sorcer.tools.shell.NetworkShell
set SHELL_CLASS=sorcer.tools.shell.NetworkShell
CALL java %JAVA_OPTS% -classpath "%SHELL_CLASSPATH%" -Djava.security.policy="%SORCER_HOME%\configs\shell\policy\shell.policy" -Dsorcer.tools.shell.logDir="%SORCER_HOME%\logs" \-Dprogram.name=NSH -Dnsh.starter.config="%NSH_CONF%" -Djava.util.logging.config.file="%SORCER_HOME%\bin\shell\configs\sorcer.logging" %STARTER_MAIN_CLASS% %*
rem --classpath "%CP%"
popd

