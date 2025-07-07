@echo off
echo Advanced OBS WebSocket Debugging
echo ==================================
echo.

REM Compile the project first
echo [1/5] Compiling project...
call mvn compile -q

if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo [2/5] Checking for existing OBS processes...
tasklist | findstr obs64.exe
if %errorlevel% equ 0 (
    echo Found existing OBS processes. Terminating...
    taskkill /F /IM obs64.exe >nul 2>&1
    timeout /t 2 >nul
)

echo [3/5] Checking if port 4455 is in use...
netstat -an | findstr :4455
if %errorlevel% equ 0 (
    echo WARNING: Port 4455 is already in use!
    echo Please close any applications using this port.
    pause
)

echo [4/5] Testing network connectivity...
echo Testing localhost connectivity...
ping -n 1 127.0.0.1 >nul
if %errorlevel% neq 0 (
    echo ERROR: Localhost not responding
    pause
    exit /b 1
)

echo [5/5] Running enhanced OBS WebSocket test...
echo.
echo This test will:
echo - Download OBS Studio if needed
echo - Try multiple WebSocket startup methods
echo - Test alternative ports
echo - Provide detailed debug information
echo.

java -cp "target/classes" com.screenrecorder.test.OBSBundleTest

echo.
echo ================================
echo Debug Test Complete
echo ================================
echo.

echo Additional manual checks:
echo 1. Check Windows Firewall for port 4455 blocking
echo 2. Check antivirus for OBS process blocking  
echo 3. Try running as Administrator
echo 4. Check if Visual C++ redistributables are installed
echo.

pause
