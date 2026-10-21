@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
(for /r "src\main\java" %%f in (*.java) do @echo "%%~ff") > build\sources.txt
javac -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (echo Compile failed! & pause & exit /b 1)
java -cp build\classes com.sgu.client.ClientLauncher config.properties
