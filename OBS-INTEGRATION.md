# OBS Studio Integration Guide for Chubby Recorder

## 🎯 Why Use OBS Studio Instead of FFmpeg?

OBS Studio provides:
- ✅ **Reliable Recording**: Better than FFmpeg for complex scenarios
- ✅ **Advanced Features**: Scene switching, filters, overlays
- ✅ **Hardware Acceleration**: GPU encoding support
- ✅ **Audio Mixing**: Professional audio handling
- ✅ **Streaming Support**: Can stream and record simultaneously
- ✅ **Plugin Ecosystem**: Extensive plugin support

## 📦 Required Dependencies

I've added these dependencies to your `pom.xml`:

```xml
<!-- OBS WebSocket Integration -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>

<!-- Logging for WebSocket -->
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

## 🛠 OBS Studio Setup

### Step 1: Install OBS Studio

1. **Download OBS Studio 28+**
   - Go to: https://obsproject.com/download
   - Download the latest version for Windows
   - Install with default settings

2. **Verify Installation**
   ```cmd
   # Check if OBS is installed
   "C:\Program Files\obs-studio\bin\64bit\obs64.exe" --version
   ```

### Step 2: Enable WebSocket Server

1. **Open OBS Studio**
2. **Go to Tools > WebSocket Server Settings**
3. **Enable WebSocket Server**
   - Check "Enable WebSocket server"
   - Set Port: `4455` (default)
   - Set Password: `your_password` (optional but recommended)
   - Click "Apply" and "OK"

### Step 3: Configure Recording Settings

1. **Go to Settings > Output**
2. **Recording Tab:**
   - Recording Path: Set to your desired folder
   - Recording Format: `mp4`
   - Recording Quality: `High Quality, Medium File Size`
   - Encoder: `x264` or `Hardware (if available)`

3. **Video Tab:**
   - Base Resolution: Your screen resolution
   - Output Resolution: 1920x1080 (or desired)
   - FPS: 30 or 60

4. **Audio Tab:**
   - Sample Rate: 44.1 kHz or 48 kHz
   - Channels: Stereo

## 🔧 Chubby Recorder Integration

### Update Controller to Use OBS Service

Update your `ScreenRecorderController.java`:

```java
// Replace FFmpegService with OBSRecordingService
private final OBSRecordingService obsService;

public ScreenRecorderController() {
    this.obsService = new OBSRecordingService();
    // ... other initialization
}

// Update event handlers
@FXML
private void onRecordClicked() {
    if (obsService.pausedProperty().get()) {
        obsService.togglePause();
    } else {
        startOBSRecording();
    }
}

private void startOBSRecording() {
    // Validate OBS is available
    obsService.isOBSAvailable().thenAccept(available -> {
        if (!available) {
            Platform.runLater(() -> {
                showError("OBS Studio is not running. Please start OBS first.");
            });
            return;
        }
        
        // Start recording
        obsService.startRecording(recordingConfig).thenAccept(success -> {
            if (!success) {
                Platform.runLater(() -> {
                    showError("Failed to start OBS recording");
                });
            }
        });
    });
}
```

### Update FXML Bindings

```java
// Bind to OBS service properties
recordButton.disableProperty().bind(obsService.recordingProperty());
pauseButton.disableProperty().bind(obsService.recordingProperty().not());
stopButton.disableProperty().bind(obsService.recordingProperty().not());
statusLabel.textProperty().bind(obsService.statusProperty());

// Add OBS connection indicator
obsConnectionLabel.textProperty().bind(
    Bindings.when(obsService.obsConnectedProperty())
        .then("OBS Connected")
        .otherwise("OBS Disconnected")
);
```

## 🎮 OBS WebSocket Commands

The `OBSRecordingService` uses these OBS WebSocket commands:

### Basic Recording Control
- `StartRecord` - Start recording
- `StopRecord` - Stop recording  
- `PauseRecord` - Pause recording
- `ResumeRecord` - Resume recording

### Scene and Source Management
- `GetSceneList` - Get available scenes
- `SetCurrentScene` - Switch to a scene
- `GetSourcesList` - Get available sources
- `SetSourceSettings` - Configure source settings

### Stream/Recording Info
- `GetRecordStatus` - Get recording status
- `GetRecordDirectory` - Get output directory
- `GetOutputSettings` - Get encoding settings

## 🚀 Usage Workflow

### 1. Start OBS Studio
```cmd
# Start OBS with WebSocket enabled
"C:\Program Files\obs-studio\bin\64bit\obs64.exe"
```

### 2. Launch Chubby Recorder
```cmd
cd "d:\chubby"
mvn clean compile
mvn javafx:run
```

### 3. Recording Process
1. **Check OBS Status**: Chubby shows "OBS Connected" 
2. **Configure Settings**: Select resolution, sources, etc.
3. **Start Recording**: Click Record button
4. **Monitor Status**: Watch live status updates
5. **Stop Recording**: Click Stop button
6. **Find Files**: Check OBS output directory

## 🎯 Advanced Features

### Scene Templates
Create OBS scenes for different recording scenarios:

1. **Full Screen Scene**
   - Add Display Capture source
   - Configure for full screen

2. **Window Capture Scene**  
   - Add Window Capture source
   - Select specific windows

3. **Multi-Source Scene**
   - Combine screen + webcam
   - Add overlays and effects

### Automation Scripts
```java
// Switch OBS scenes programmatically
obsService.setCurrentScene("Full Screen Recording");
obsService.startRecording(config);

// Add filters and effects
obsService.addSourceFilter("Display Capture", "Blur", blurSettings);
```

### Quality Presets
```java
// Set encoding presets
obsService.setEncodingPreset("High Quality");
obsService.setOutputResolution(1920, 1080);
obsService.setBitrate(8000); // 8 Mbps
```

## 📊 Benefits Over FFmpeg

| Feature | FFmpeg | OBS Studio |
|---------|--------|------------|
| Setup Complexity | High | Medium |
| Audio Reliability | Issues | Excellent |
| Hardware Encoding | Limited | Full Support |
| Real-time Preview | No | Yes |
| Scene Management | No | Yes |
| Plugin Support | Limited | Extensive |
| Error Handling | Basic | Advanced |

## 🔧 Troubleshooting

### OBS Not Starting
```cmd
# Check if OBS is running
tasklist | findstr obs

# Start OBS manually
"C:\Program Files\obs-studio\bin\64bit\obs64.exe" --minimize-to-tray
```

### WebSocket Connection Failed
1. Check OBS WebSocket settings
2. Verify port 4455 is not blocked
3. Check firewall settings
4. Restart OBS Studio

### Recording Not Starting
1. Check OBS recording settings
2. Verify output directory exists
3. Check disk space
4. Review OBS log files

## 🎉 Next Steps

1. **Install OBS Studio** (if not already installed)
2. **Enable WebSocket Server** in OBS
3. **Run Maven** to download dependencies:
   ```cmd
   cd "d:\chubby"
   mvn clean compile
   ```
4. **Test OBS Integration**:
   ```cmd
   mvn javafx:run
   ```

The `OBSRecordingService` is now ready to replace the problematic FFmpeg implementation with a much more reliable OBS Studio backend! 🎬
