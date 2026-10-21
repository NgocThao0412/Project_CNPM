@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
(for /r src %%f in (*.java) do @echo "%%f") > build\sources.txt
javac -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (echo Compile failed! & pause & exit /b 1)
java -cp build\classes com.sgu.client.ClientApp config.properties
