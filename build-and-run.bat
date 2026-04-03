@echo off
REM Build and Run Script for LimelightSim
REM This script builds a fat JAR and executes it

setlocal enabledelayedexpansion

set JAR_PATH=app\build\libs\LimelightSim-1.0-all.jar
set BUILD_ONLY=0
set RUN_ONLY=0

REM Parse command line arguments
for %%A in (%*) do (
    if "%%A"=="-build" set BUILD_ONLY=1
    if "%%A"=="-run" set RUN_ONLY=1
)

echo.
echo =========================================
echo LimelightSim Build and Run Script
echo =========================================
echo.

REM Get the current directory
set PROJECT_ROOT=%~dp0

if %RUN_ONLY% equ 1 (
    goto run
) else if %BUILD_ONLY% equ 1 (
    goto build
) else (
    goto build_and_run
)

:build
echo Building LimelightSim JAR...
echo.
cd /d "%PROJECT_ROOT%"
call gradlew.bat clean build fatJar
if errorlevel 1 (
    echo.
    echo Build failed!
    exit /b 1
)
echo.
echo Build successful!
exit /b 0

:run
cd /d "%PROJECT_ROOT%"
set FULL_JAR_PATH=%PROJECT_ROOT%%JAR_PATH%
if not exist "!FULL_JAR_PATH!" (
    echo JAR file not found at: !FULL_JAR_PATH!
    echo Please build the project first using: build-and-run.bat -build
    exit /b 1
)
echo.
echo Executing JAR: !FULL_JAR_PATH!
echo.
java -jar "!FULL_JAR_PATH!"
exit /b %errorlevel%

:build_and_run
cd /d "%PROJECT_ROOT%"
call gradlew.bat clean build fatJar
if errorlevel 1 (
    echo.
    echo Build failed! Exiting.
    exit /b 1
)
echo.
echo Build successful! Running application...
echo.
set FULL_JAR_PATH=%PROJECT_ROOT%%JAR_PATH%
java -jar "!FULL_JAR_PATH!"
exit /b %errorlevel%

endlocal
