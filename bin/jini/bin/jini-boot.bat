@ECHO OFF
REM jini-boot.bat
REM ANT_HOME=C:\ant\apache-ant-1.7.0
REM JAVA_HOME="C:\Program Files\Java\jdk1.6.0_16"
REM PATH=%PATH%;%JAVA_HOME%\bin;%ANT_HOME\bin
echo jini LUS
cd %IGRID_HOME%\bin\jini\bin
ant -f jini-boot.xml
PAUSE
REM EOF
