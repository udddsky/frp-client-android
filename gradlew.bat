@echo off
setlocal

set DIR=%~dp0
cd /d "%DIR%"

if not exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
    echo Gradle wrapper jar not found. Please run 'gradle wrapper' or download the wrapper jar manually.
    exit /b 1
)

set JAVA_EXE=java
if defined JAVA_HOME (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
