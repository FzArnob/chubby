@echo off
echo Testing OBS WebSocket Connection...
echo.

REM First, compile the project
echo Compiling project...
call mvn compile

if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo.
echo Running WebSocket connection test...
echo This will start OBS and test the WebSocket connection.
echo.

java -cp "target/classes" com.screenrecorder.test.OBSBundleTest

echo.
echo Test completed. Check the output above for WebSocket connection status.
pause
