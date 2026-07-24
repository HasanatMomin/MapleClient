@echo off
title MapleLauncher Debug Runner
echo Launching MapleLauncher...
java -jar build/libs/MapleLauncher.jar
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Application closed with an error code: %ERRORLEVEL%
    pause
)
