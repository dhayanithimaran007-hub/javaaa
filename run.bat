@echo off
setlocal

echo =======================================================================
echo Smart Campus Complaint Prioritization and Tracking System - Launcher
echo =======================================================================

:: Locate Java
where java >nul 2>nul
if %ERRORLEVEL% equ 0 (
    set JAVA_CMD=java
) else if defined JAVA_HOME (
    set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
) else (
    echo [ERROR] Java runtime not found on PATH or JAVA_HOME.
    echo Please install Java 25 or later and ensure 'java' is in PATH.
    pause
    exit /b 1
)

:: Locate JAR
set JAR_FILE=target\smart-campus-complaint-system-1.0.0.jar
if not exist "%JAR_FILE%" (
    if exist "target\smart-campus-complaint-system-1.0.0-executable.jar" (
        set JAR_FILE=target\smart-campus-complaint-system-1.0.0-executable.jar
    ) else if exist "packaging\smart-campus-app.jar" (
        set JAR_FILE=packaging\smart-campus-app.jar
    ) else (
        echo [INFO] Packaged JAR not found. Building with Maven...
        call mvn clean package -DskipTests
        if %ERRORLEVEL% neq 0 (
            echo [ERROR] Maven build failed.
            pause
            exit /b 1
        )
    )
)

echo Starting application using %JAVA_CMD% ...
%JAVA_CMD% --enable-native-access=ALL-UNNAMED -jar "%JAR_FILE%"
if %ERRORLEVEL% neq 0 (
    echo [INFO] Application exited with code %ERRORLEVEL%.
)

endlocal
