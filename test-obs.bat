@echo off
echo Testing OBS Bundle Manager...
echo.

REM Compile the test
echo Compiling test class...
call mvn test-compile

if %errorlevel% neq 0 (
    echo ERROR: Test compilation failed
    pause
    exit /b 1
)

REM Run the test
echo.
echo Running OBS Bundle Manager test...
echo This will download OBS Studio if not already bundled.
echo.

java -cp "target/classes;target/test-classes" com.screenrecorder.test.OBSBundleTest

echo.
echo Test completed. Check the output above for results.
pause
