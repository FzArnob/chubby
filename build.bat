@echo off
echo Building Chubby Recorder with Bundled OBS...
echo.

REM Check if Maven is available
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/download.cgi
    echo.
    pause
    exit /b 1
)

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 21+ from https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

echo Maven and Java detected - proceeding with build...
echo.

REM Clean previous builds
echo Cleaning previous builds...
mvn clean

if %errorlevel% neq 0 (
    echo ERROR: Maven clean failed
    pause
    exit /b 1
)

REM Build the project
echo Building project...
mvn package

if %errorlevel% neq 0 (
    echo ERROR: Maven build failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo Created files:
if exist "target\ChubbyRecorder.exe" (
    echo ✓ target\ChubbyRecorder.exe - Windows executable
) else (
    echo ✗ Windows executable not created
)

if exist "target\ChubbyRecorder-1.0.0.zip" (
    echo ✓ target\ChubbyRecorder-1.0.0.zip - Distribution package
) else (
    echo ✗ Distribution package not created
)

echo.
echo You can now distribute:
echo 1. ChubbyRecorder.exe (single file - users download OBS automatically)
echo 2. ChubbyRecorder-1.0.0.zip (full package with docs and scripts)
echo.

set /p test="Do you want to test the executable now? (y/N): "
if /i "%test%" equ "y" (
    echo.
    echo Starting ChubbyRecorder.exe...
    start "" "target\ChubbyRecorder.exe"
)

echo.
echo Build process complete!
pause
