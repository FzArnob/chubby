# Chubby Recorder - Bundled OBS Distribution Guide

## Overview

Chubby Recorder now includes **bundled OBS Studio** support, allowing you to distribute a completely self-contained screen recording application. Users no longer need to install OBS Studio separately - everything is handled automatically!

## Features

✅ **Automatic OBS Download**: Downloads OBS Studio portable version on first run  
✅ **Zero Installation**: No separate OBS installation required  
✅ **Portable Distribution**: Single executable that works anywhere  
✅ **Automatic Configuration**: Sets up OBS with optimal recording settings  
✅ **Background Operation**: OBS runs silently in the background  
✅ **Native Executable**: Builds to a Windows .exe file using Launch4j  

## How It Works

### First Run Experience
1. User runs `ChubbyRecorder.exe`
2. Application automatically downloads OBS Studio portable (≈300MB)
3. Extracts and configures OBS with optimal settings
4. Starts OBS in background with minimal UI
5. Ready to record immediately!

### Subsequent Runs
- OBS is already bundled - starts immediately
- No downloads needed
- Fast startup time

## Building the Distribution

### Prerequisites
- Java 21+ JDK
- Maven 3.6+
- Internet connection (for downloading OBS during runtime)

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Create executable distribution
mvn package

# This creates:
# - target/ChubbyRecorder.exe (Windows executable)
# - target/ChubbyRecorder-1.0.0.zip (Full distribution package)
```

### Distribution Contents

The distribution package includes:
```
ChubbyRecorder-1.0.0/
├── ChubbyRecorder.exe          # Main executable
├── start.bat                   # Startup script with Java checks
├── clean-obs.bat              # OBS cleanup utility
├── lib/ChubbyRecorder.jar     # Backup JAR file
└── docs/
    ├── README.md
    ├── LICENSE
    └── OBS-INTEGRATION-GUIDE.md
```

## Deployment

### Single File Distribution
Just distribute `ChubbyRecorder.exe` - it will handle everything else automatically.

### Full Package Distribution
Distribute the ZIP file for users who want startup scripts and documentation.

## User Experience

### System Requirements
- Windows 10/11 (64-bit)
- Java 21+ (prompted for download if missing)
- 500MB free disk space (for OBS bundle)
- Internet connection (first run only)

### Installation Process
1. Download `ChubbyRecorder.exe`
2. Run the executable
3. Wait for OBS Studio to download and configure (first run only)
4. Start recording immediately!

### No Installation Required
- No administrator privileges needed
- No registry modifications
- No system-wide installations
- Completely portable

## Technical Details

### OBS Bundle Management
- Downloads OBS Studio 30.0.2 portable version
- Extracts to `obs-studio/` directory next to executable
- Configures WebSocket server automatically
- Manages OBS process lifecycle

### Memory and Storage
- **Executable Size**: ~50MB (fat JAR + launcher)
- **OBS Bundle Size**: ~300MB (downloaded once)
- **Total Footprint**: ~350MB
- **RAM Usage**: ~200MB (application + bundled OBS)

### Security Features
- Downloads from official OBS GitHub releases
- Verifies file integrity
- Sandboxed OBS installation
- No elevation required

## Customization

### OBS Configuration
The bundled OBS is automatically configured with:
- WebSocket server enabled (port 4455)
- Optimal recording settings for desktop capture
- Minimal UI mode
- Auto-start replay buffer

### Build Customization
Edit `pom.xml` to customize:
- Output executable name
- Version information
- Application icons
- Distribution format

## Troubleshooting

### "Java Not Found"
- User needs Java 21+
- `start.bat` provides download link
- Executable shows helpful error message

### "Download Failed"
- Check internet connection
- Verify GitHub access
- Retry - download resumes automatically

### "OBS Won't Start"
- Check available disk space
- Verify Windows compatibility
- Try running as administrator

### Large File Size
- Use `clean-obs.bat` to remove OBS bundle
- Application will re-download when needed
- Consider distribution without bundled OBS for space-constrained scenarios

## Development Notes

### Build Process
1. Maven compiles Java code
2. Shade plugin creates fat JAR
3. Launch4j creates Windows executable
4. Assembly plugin packages distribution

### Testing
```bash
# Test JAR directly
java -jar target/chubby-screen-recorder-1.0.0.jar

# Test executable
./target/ChubbyRecorder.exe

# Test distribution
unzip target/ChubbyRecorder-1.0.0.zip
cd ChubbyRecorder-1.0.0
./ChubbyRecorder.exe
```

## Advanced Features

### Clean Installation
Users can run `clean-obs.bat` to remove bundled OBS and save disk space. OBS will be re-downloaded when needed.

### Silent Mode
The application can be started with minimal user interaction - ideal for automated deployments.

### Configuration Persistence
User preferences and OBS configurations are preserved between updates.

## Comparison with Traditional Approach

| Feature | Traditional (User Installs OBS) | Bundled OBS |
|---------|----------------------------------|-------------|
| User Setup | Complex (install OBS + configure) | Simple (run exe) |
| Distribution Size | ~50MB | ~350MB |
| Dependencies | OBS Studio required | Self-contained |
| Updates | Separate OBS updates needed | Bundled updates |
| Support Complexity | High (OBS version conflicts) | Low (controlled environment) |

## Next Steps

1. **Test the build**: Run `mvn package` to create your distribution
2. **Test deployment**: Try the executable on a clean Windows machine
3. **Distribute**: Share the single .exe file or full ZIP package
4. **Monitor**: Check logs for any download or startup issues

Your Chubby Recorder is now ready for easy distribution with zero installation requirements!
