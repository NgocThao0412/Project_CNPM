@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
> build\sources.txt (
    for /r "src\main\java" %%f in (*.java) do @echo "%%~ff"
    for /r "src\test\java" %%f in (*.java) do @echo "%%~ff"
)
javac -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 (echo Compile failed! & pause & exit /b 1)
java -ea -cp build\classes com.sgu.client.tests.ClientTests
pause

java -ea -cp build\classes com.sgu.client.tests.MicroservicesPortsTests
