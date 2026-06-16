@echo off
setlocal enabledelayedexpansion

set "DIRNAME=%~dp0"
if "%DIRNAME:~-1%"=="\" set "DIRNAME=%DIRNAME:~0,-1%"

set "MVNW_REPOURL=https://repo.maven.apache.org/maven2"
set "WRAPPER_VERSION=3.3.2"
set "MVN_VERSION=3.9.8"

set "WRAPPER_JAR=%DIRNAME%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPS=%DIRNAME%\.mvn\wrapper\maven-wrapper.properties"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper %WRAPPER_VERSION% ...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/%WRAPPER_VERSION%/maven-wrapper-%WRAPPER_VERSION%.jar' -OutFile '%WRAPPER_JAR%' -UseBasicParsing"
    if not exist "%WRAPPER_JAR%" (
        echo ERROR: Failed to download Maven Wrapper
        exit /b 1
    )
)

set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "MVNW_VERBOSE=false"

if "%MVNW_VERBOSE%" == "true" (
    echo "MVNW_VERBOSE=true is not supported on Windows yet"
)

java -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%DIRNAME%" ^
  "-Dmaven.home=" ^
  "-Dwrapper.properties=%WRAPPER_PROPS%" ^
  "-Dmaven.user.home=%MAVEN_USER_HOME%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
