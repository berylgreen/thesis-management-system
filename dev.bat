@echo off
if "%1"=="" (
    echo Usage: dev [start^|stop^|restart^|status]
    exit /b 1
)

powershell -ExecutionPolicy Bypass -File "%~dp0dev.ps1" -Action %1
