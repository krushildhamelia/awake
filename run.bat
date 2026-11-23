@echo off
setlocal
cd /d "%~dp0"

if not exist "target\screen-awake.jar" (
    echo Building screen-awake.jar using Maven...
    call mvn clean package
    if errorlevel 1 (
        echo Build failed! Please check your Maven and Java installation.
        pause
        exit /b 1
    )
)

echo Starting Screen Awake...
java -jar "target\screen-awake.jar" %*
