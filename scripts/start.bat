@echo off
echo Starting Chubby Recorder...
echo.

REM Check if Java 21+ is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21 or later from https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "delims=. tokens=1-3" %%v in ("%JAVA_VERSION%") do (
    set MAJOR=%%v
    set MINOR=%%w
)

if %MAJOR% LSS 21 (
    echo ERROR: Java version %JAVA_VERSION% is too old
    echo Chubby Recorder requires Java 21 or later
    echo Please update Java from https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

echo Java version %JAVA_VERSION% detected - OK
echo.

REM Start the application
echo Initializing Chubby Recorder...
echo The application will download and configure OBS Studio automatically.
echo This may take a few minutes on first run.
echo.

if exist "ChubbyRecorder.exe" (
    echo Starting ChubbyRecorder.exe...
    start "" "ChubbyRecorder.exe"
) else if exist "lib\ChubbyRecorder.jar" (
    echo Starting JAR version...
    java -jar "lib\ChubbyRecorder.jar"
) else (
    echo ERROR: ChubbyRecorder executable not found
    echo Please ensure you extracted all files correctly
    pause
    exit /b 1
)

echo.
echo Chubby Recorder is starting...
echo Check the application window for status updates.
