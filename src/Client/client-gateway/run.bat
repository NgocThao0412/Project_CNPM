@echo off
cd /d "%~dp0.."
java "-Dclient.home=%cd%" -jar "client-gateway\client-gateway.jar"
if errorlevel 1 pause
