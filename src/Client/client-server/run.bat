@echo off
cd /d "%~dp0.."
java "-Dclient.home=%cd%" -jar "client-server\client-server.jar"
if errorlevel 1 pause
