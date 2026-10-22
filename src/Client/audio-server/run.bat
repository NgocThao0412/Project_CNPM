@echo off
cd /d "%~dp0.."
java "-Dclient.home=%cd%" -jar "audio-server\audio-server.jar"
if errorlevel 1 pause
