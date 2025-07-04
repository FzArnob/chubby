# OBS Integration Setup Guide for Chubby Recorder

## Required Libraries (Already Added to pom.xml)

The following Maven dependencies are needed for OBS integration:

```xml
<!-- OBS WebSocket Integration -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>

<!-- JSON processing for OBS API -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- Logging framework -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.7</version>
</dependency>

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.7</version>
</dependency>

<!-- HTTP Client for OBS REST calls -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.2.1</version>
</dependency>
```

## OBS Studio Setup

### 1. Install OBS Studio
- Download OBS Studio 28.0+ from: https://obsproject.com/
- Install with default settings

### 2. Enable WebSocket Server
1. Open OBS Studio
2. Go to **Tools** → **WebSocket Server Settings**
3. Check **"Enable WebSocket server"**
4. Set **Server Port**: `4455` (default)
5. Set **Server Password**: Leave empty for now (or set a password)
6. Click **OK**

### 3. Basic OBS Scene Setup
1. Create a new Scene (or use default)
2. Add a **Display Capture** source for full screen recording
3. Or add **Window Capture** source for specific window recording

## How the Integration Works

### OBSRecordingService Features:
1. **WebSocket Communication**: Connects to OBS via WebSocket API
2. **Recording Control**: Start/Stop/Pause recording through API calls
3. **Status Monitoring**: Real-time recording status updates
4. **Scene Management**: Switch between different recording setups
5. **Output Configuration**: Set recording format and quality

### Key Methods:
- `isOBSAvailable()`: Check if OBS is running
- `testOBSConnection()`: Test WebSocket connection
- `startRecording()`: Begin recording with current config
- `stopRecording()`: Stop current recording
- `togglePause()`: Pause/resume recording

## Quick Start Steps

1. **Install OBS Studio 28+**
2. **Enable WebSocket server** in OBS settings
3. **Start OBS Studio** before running Chubby Recorder
4. **Set up a basic scene** with Display Capture
5. **Run Chubby Recorder** - it will automatically connect to OBS
6. **Click Record** - recording will start in OBS

## Troubleshooting

### "OBS Studio Not Found"
- Make sure OBS Studio is running
- Check that WebSocket server is enabled in OBS settings

### "WebSocket Connection Failed"
- Verify OBS WebSocket server is running (Tools → WebSocket Server Settings)
- Check port 4455 is not blocked by firewall
- Try restarting OBS Studio

### "Recording Failed"
- Ensure OBS has a valid scene with sources
- Check OBS recording settings (File → Settings → Output)
- Verify output path is writable

## Advantages Over FFmpeg

1. **Better Hardware Acceleration**: OBS uses GPU encoding
2. **More Reliable**: Mature recording engine with wide compatibility
3. **Real-time Preview**: See what you're recording in OBS
4. **Professional Features**: Scenes, transitions, filters
5. **Better Audio Handling**: Advanced audio mixing capabilities
6. **Streaming Ready**: Can stream and record simultaneously

## Current Status

✅ **OBS Integration Implemented**: OBSRecordingService is ready
✅ **WebSocket API**: Full OBS WebSocket 5.0 protocol support
✅ **Controller Updated**: UI already uses OBS instead of FFmpeg
✅ **Dependencies Added**: All required libraries in pom.xml
✅ **Error Handling**: Comprehensive error messages and status updates

Your Chubby Recorder is now configured to use OBS Studio for recording instead of FFmpeg!
