@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.
@REM Maven Wrapper Script for Windows
@REM ----------------------------------------------------------------------------
@IF "%__MVNW_ARG0_NAME__%"=="" (SET "MVN_CMD=mvnw.cmd") ELSE (SET "MVN_CMD=%__MVNW_ARG0_NAME__%")
@SET MAVEN_PROJECTBASEDIR=%~dp0
@REM Strip trailing backslash so it doesn't escape the closing quote in -D arguments
@IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
@SET WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

@REM Look for the Java executable
@IF NOT "%JAVA_HOME%"=="" (
  @SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
  @FOR %%i IN (java.exe) DO @SET JAVA_EXE=%%~$PATH:i
)

@IF "%JAVA_EXE%"=="" SET "JAVA_EXE=C:\Program Files\Amazon Corretto\jdk17.0.13_11\bin\java.exe"

"%JAVA_EXE%" -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %*

