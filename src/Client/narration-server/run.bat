@echo off
cd /d "%~dp0.."
java "-Dclient.home=%cd%" -jar "narration-server\narration-server.jar"
if errorlevel 1 pause
