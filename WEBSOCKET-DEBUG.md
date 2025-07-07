# OBS WebSocket Connection Troubleshooting Guide 🔧

## Problem Analysis

The OBS process is starting successfully but the WebSocket server connection is failing. Here's what I've implemented to fix this:

## Improvements Made

### 1. **Enhanced OBS Configuration**
- Added explicit WebSocket configuration in `global.ini`
- Created dedicated `obs-websocket.ini` configuration file
- Force-enabled WebSocket server on port 4455

### 2. **Improved Startup Parameters**
```java
ProcessBuilder pb = new ProcessBuilder(
    obsExecutable.toString(),
    "--minimize-to-tray",
    "--disable-shutdown-check",
    "--collection", "ChubbyRecorder",
    "--scene", "Scene",
    "--websocket-port", "4455",      // Explicit WebSocket port
    "--websocket-password", "",      // No password
    "--verbose"                      // Enable verbose logging
);
```

### 3. **Better Connection Testing**
- **Socket-based testing**: Direct port connectivity check
- **HTTP fallback**: Tests WebSocket endpoint via HTTP
- **Debug information**: Detailed status reporting
- **Extended timeout**: Increased wait time for WebSocket startup

### 4. **Debug Capabilities**
- Real-time process status monitoring
- Port accessibility testing
- HTTP response code analysis
- Detailed error reporting

## Testing Instructions

### Quick Test
```bash
cd d:\chubby
test-websocket.bat
```

### Manual Testing Steps
1. **Compile the project**:
   ```bash
   mvn compile
   ```

2. **Run the OBS Bundle Test**:
   ```bash
   java -cp "target/classes" com.screenrecorder.test.OBSBundleTest
   ```

3. **Check the debug output** for:
   - OBS Process Running: true/false
   - WebSocket Port Open: true/false
   - HTTP Response Code: (should be 404, 400, or 426)

## Expected Debug Output (Success)
```
=== OBS Debug Status ===
OBS Process Running: true
OBS WebSocket port 4455 is accessible
HTTP Response Code: 404
========================
```

## Expected Debug Output (Failure)
```
=== OBS Debug Status ===
OBS Process Running: true
OBS WebSocket port 4455 is not accessible: Connection refused
HTTP Test Failed: Connection refused
========================
```

## Common Issues & Solutions

### Issue 1: WebSocket Not Starting
**Symptoms**: OBS process runs but WebSocket port not accessible

**Solutions**:
1. **Check OBS logs** in `obs-studio/logs/`
2. **Verify configuration** files are created correctly
3. **Try manual OBS start** to see error messages

### Issue 2: Port 4455 Blocked
**Symptoms**: Connection refused errors

**Solutions**:
1. **Windows Firewall**: Add exception for port 4455
2. **Antivirus**: Add exception for OBS executable
3. **Other software**: Check if another app uses port 4455

### Issue 3: OBS Crashes on Startup
**Symptoms**: Process starts then immediately dies

**Solutions**:
1. **Run as Administrator**: Right-click → Run as administrator
2. **Check dependencies**: Ensure Visual C++ redistributables installed
3. **Graphics drivers**: Update GPU drivers

## Configuration Files Created

### 1. `global.ini`
```ini
[OBSWebSocket]
ServerEnabled=true
ServerPort=4455
AuthRequired=false
ServerPassword=
AlertsEnabled=false
```

### 2. `obs-websocket.ini`
```ini
[OBSWebSocket]
ServerEnabled=true
ServerPort=4455
AuthRequired=false
ServerPassword=
AlertsEnabled=false
DebugEnabled=false
```

## Manual Verification

### Check if OBS WebSocket is working:
1. **Start OBS manually** from `obs-studio/bin/64bit/obs64.exe`
2. **Go to Tools → WebSocket Server Settings**
3. **Verify "Enable WebSocket server" is checked**
4. **Check port is 4455**
5. **Test connection** using a WebSocket client

### Test WebSocket manually:
```bash
# Use telnet to test port
telnet localhost 4455

# Or use PowerShell
Test-NetConnection -ComputerName localhost -Port 4455
```

## Advanced Debugging

### Enable OBS Verbose Logging
The startup now includes `--verbose` flag for detailed OBS logs.

**Log locations**:
- `obs-studio/logs/` - General OBS logs
- `obs-studio/config/obs-studio/logs/` - Configuration logs

### Check Network Connectivity
```powershell
# Check if port is listening
netstat -an | findstr 4455

# Check firewall rules
netsh advfirewall firewall show rule name="OBS Studio"
```

### Verify Process Status
```powershell
# Check OBS processes
tasklist | findstr obs

# Check port usage
netstat -ano | findstr 4455
```

## Alternative Solutions

### If WebSocket still fails:

1. **Use system OBS**: Install OBS Studio normally
2. **Different port**: Try port 4444 or 4456
3. **Manual WebSocket**: Enable WebSocket manually in OBS UI

### Fallback Configuration
If automatic configuration fails, the app will:
1. Try to detect system-installed OBS
2. Provide clear error messages
3. Suggest manual installation steps

## Next Steps

1. **Run the test**: Use `test-websocket.bat`
2. **Check debug output**: Look for connection status
3. **If still failing**: Check Windows Firewall and antivirus
4. **Report results**: Include full debug output

The enhanced connection testing should now provide much better visibility into why the WebSocket connection is failing and guide you to the solution.

---

**🎯 Goal**: Establish reliable OBS WebSocket connection for seamless recording control.
