# Chubby Screen Recorder

A comprehensive silent screen recorder application built with JavaFX 21 and **bundled OBS Studio**, featuring automatic setup, live preview, multiple recording sources, and flexible output options.

## 🚀 Zero Installation Required

**NEW: Bundled OBS Edition** - No separate software installation needed! Just run the executable and start recording immediately.

## Features

### 🎬 Recording Sources
- **Full Screen Capture**: Record entire display
- **Window Recording**: Select specific windows by title
- **Live Preview**: Real-time preview during recording

### 🔊 Audio Options
- **System Audio**: Record desktop/system audio
- **Microphone**: Optional microphone recording
- **Flexible Audio Sources**: Choose from available audio devices
- **Separate Audio Export**: Export audio as separate file (.aac/.mp3)

### 🖼 Resolution Support
- **1080p (1920x1080)**
- **2K (2560x1440)**
- **4K (3840x2160)**
- **Automatic fallback** to closest supported resolution

### 📁 Output Options
- **MP4 Format**: High-quality video output
- **Custom Directory**: Choose output location
- **Auto-naming**: Timestamps in filenames
- **Combined or Separate**: Video+audio combined or separate files

### ⏺ Recording Controls
- **Record**: Start new recording
- **Pause/Resume**: Pause and resume functionality
- **Stop**: Stop and save recording
- **Real-time Status**: Live recording status and progress

### 📦 Bundled OBS Studio
- **Automatic Download**: OBS Studio downloads automatically on first run
- **Zero Configuration**: Pre-configured for optimal recording
- **Background Operation**: Runs silently without user intervention
- **Professional Quality**: Uses OBS's mature recording engine

## Quick Start (Bundled Version)

### For End Users
1. **Download** `ChubbyRecorder.exe`
2. **Run** the executable
3. **Wait** for OBS Studio to download and configure (first run only)
4. **Start Recording** immediately!

### System Requirements
- Windows 10/11 (64-bit)
- Java 21+ (prompted for download if missing)
- 500MB free disk space (for OBS bundle)
- Internet connection (first run only)

## Prerequisites for Development

### Required Software
1. **Java 21 or higher**
2. **JavaFX 21**
3. **Maven 3.6+**

### No FFmpeg Required
The bundled OBS version eliminates the need for separate FFmpeg installation!

#### macOS
```bash
# Using Homebrew
brew install ffmpeg
```

#### Linux
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install ffmpeg

# CentOS/RHEL
sudo yum install ffmpeg
```

## Building and Running

### Using Maven

1. **Clone/Download** the project
2. **Navigate** to project directory:
   ```cmd
   cd screen-recorder
   ```

3. **Compile** the project:
   ```cmd
   mvn clean compile
   ```

4. **Run** the application:
   ```cmd
   mvn javafx:run
   ```

5. **Package** as JAR:
   ```cmd
   mvn clean package
   ```

### Running the JAR
```cmd
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml,javafx.media -jar target/chubby-screen-recorder-1.0.0-shaded.jar
```

## Usage Guide

### Basic Recording
1. **Launch** the application
2. **Select Video Source**: Choose "Full Screen" or a specific window
3. **Configure Audio**: Enable system audio and/or microphone
4. **Set Resolution**: Choose from 1080p, 2K, or 4K
5. **Choose Output Directory**: Select where to save recordings
6. **Click Record**: Start recording
7. **Use Controls**: Pause/Resume or Stop as needed

### Advanced Options
- **Separate Audio**: Check "Export Audio Separately" for separate video and audio files
- **Audio Sources**: Select specific audio devices from the dropdown
- **Window Recording**: Choose specific windows for focused recording

### Live Preview
The preview section shows a live preview during recording (when supported by the system).

## Project Structure

```
screen-recorder/
├── src/main/java/
│   ├── com/screenrecorder/
│   │   ├── ScreenRecorderApp.java          # Main JavaFX application
│   │   ├── ScreenRecorderController.java   # Main UI controller
│   │   ├── model/
│   │   │   ├── RecordingConfig.java        # Configuration model
│   │   │   ├── RecordingSource.java        # Source representation
│   │   │   └── Resolution.java             # Resolution options
│   │   ├── service/
│   │   │   ├── FFmpegService.java          # FFmpeg integration
│   │   │   └── SystemDiscoveryService.java # System source discovery
│   │   └── util/
│   │       └── ConfigurationManager.java   # Config persistence
│   ├── module-info.java                    # Java module definition
├── src/main/resources/
│   └── main-view.fxml                      # Main UI layout
├── pom.xml                                 # Maven configuration
└── README.md                               # This file
```

## Configuration

The application automatically saves configuration preferences to:
- **Windows**: `%USERPROFILE%\screen-recorder-config.json`
- **macOS/Linux**: `~/screen-recorder-config.json`

## Troubleshooting

### FFmpeg Not Found
- Ensure FFmpeg is installed and in your system PATH
- Test with `ffmpeg -version` in command line
- On Windows, restart your terminal/IDE after adding to PATH

### Audio Issues
- Windows: Ensure "Stereo Mix" is enabled in audio settings
- Check that audio devices are not being used by other applications
- Try different audio sources from the dropdown

### Recording Fails
- Check that output directory has write permissions
- Ensure selected window still exists (for window recording)
- Verify sufficient disk space for recording

### Performance Issues
- Lower resolution for better performance
- Close unnecessary applications
- Ensure adequate RAM and CPU resources

## Known Limitations

- **Live Preview**: May have limited functionality depending on system capabilities
- **Window Recording**: Requires exact window title match
- **Audio Latency**: Some audio sync issues may occur on slower systems
- **Platform Support**: Optimized for Windows, may need adjustments for macOS/Linux

## Development

### Building from Source
```cmd
git clone <repository-url>
cd screen-recorder
mvn clean install
mvn javafx:run
```

### Dependencies
- JavaFX 21 (Controls, FXML, Media)
- Jackson (JSON processing)
- Maven (build system)

## License

This project is provided as-is for educational and development purposes.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## Support

For issues and questions:
1. Check the troubleshooting section
2. Verify FFmpeg installation
3. Check system requirements
4. Review error messages in the status bar
