@echo off
echo Chubby Recorder - Clean Installation
echo.
echo This will remove the bundled OBS Studio installation to save disk space.
echo You can always re-download it when needed.
echo.

set /p confirm="Are you sure you want to remove bundled OBS? (y/N): "
if /i "%confirm%" neq "y" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo Removing bundled OBS Studio...

if exist "obs-studio" (
    rmdir /s /q "obs-studio"
    echo Bundled OBS Studio removed successfully.
) else (
    echo No bundled OBS Studio installation found.
)

echo.
echo Clean-up complete. Chubby Recorder will download OBS again when needed.
pause
