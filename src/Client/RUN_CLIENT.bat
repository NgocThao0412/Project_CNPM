@echo off
cd /d "%~dp0"
set "SGU_MODE=%~1"
if "%SGU_MODE%"=="" set "SGU_MODE=demo"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" -Mode "%SGU_MODE%"
if errorlevel 1 pause
