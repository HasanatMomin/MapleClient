@echo off
title Maple Launcher
java -jar MapleLauncher-1.0.jar
if %errorlevel% neq 0 (
    echo.
    echo ----------------------------------------------------
    echo [ERROR] Failed to start Maple Launcher.
    echo Please make sure Java 21 or newer is installed!
    echo Download Java: https://adoptium.net/
    echo ----------------------------------------------------
    pause
)
