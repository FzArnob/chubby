package com.screenrecorder.service;

import com.screenrecorder.model.RecordingConfig;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced OBS Recording Service that works with bundled OBS Studio
 * Automatically manages bundled OBS installation and lifecycle
 */
public class OBSPortableRecordingService {
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final BooleanProperty recordingProperty;
    private final BooleanProperty pausedProperty;
    private final StringProperty statusProperty;
    private final BooleanProperty obsConnectedProperty;
    private final BooleanProperty obsBundledProperty;
    
    private final OBSBundleManager bundleManager;
    
    // OBS WebSocket connection details
    private String obsWebSocketHost = "localhost";
    private int obsWebSocketPort = 4455;
    private String obsWebSocketPassword = "";
    
    // Recording state
    private boolean isInitialized = false;
    private String currentRecordingId;
    private RecordingConfig currentConfig;
    
    private WebSocketClient wsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger messageId = new AtomicInteger(1);
    
    public OBSPortableRecordingService() {
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.bundleManager = new OBSBundleManager();
        
        // Initialize properties
        this.recordingProperty = new SimpleBooleanProperty(false);
        this.pausedProperty = new SimpleBooleanProperty(false);
        this.statusProperty = new SimpleStringProperty("Not Connected");
        this.obsConnectedProperty = new SimpleBooleanProperty(false);
        this.obsBundledProperty = new SimpleBooleanProperty(bundleManager.isOBSBundled());
        
        // Start monitoring OBS connection
//        startConnectionMonitoring();
    }
    
    /**
     * Initialize the service - download OBS if needed and start it
     */
    public CompletableFuture<Boolean> initializeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                updateStatus("Initializing OBS...");

                // Check if OBS is bundled
                if (!bundleManager.isOBSBundled()) {
                    updateStatus("Downloading OBS Studio...");
                    boolean downloaded = bundleManager.downloadAndInstallOBS().get();
                    if (!downloaded) {
                        updateStatus("Failed to download OBS Studio");
                        return false;
                    }
                    Platform.runLater(() -> obsBundledProperty.set(true));
                }

                // Start bundled OBS
                updateStatus("Starting OBS Studio...");
                boolean started = bundleManager.startBundledOBS().get();
                if (!started) {
                    updateStatus("Failed to start OBS Studio");
                    return false;
                }

                // Wait for OBS to be ready and connect
                updateStatus("Connecting to OBS...");
                boolean connected = waitForOBSConnection(30); // Wait up to 30 seconds

                if (connected) {
                    updateStatus("OBS Ready");
                    isInitialized = true;
                    return true;
                } else {
                    updateStatus("Failed to connect to OBS");
                    return false;
                }

            } catch (Exception e) {
                updateStatus("Initialization failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Start recording with the given configuration
     */
    public CompletableFuture<Boolean> startRecording(RecordingConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isInitialized) {
                    updateStatus("OBS not initialized");
                    return false;
                }
                
                if (recordingProperty.get()) {
                    updateStatus("Already recording");
                    return false;
                }
                
                this.currentConfig = config;
                this.currentRecordingId = UUID.randomUUID().toString();
                
                updateStatus("Starting recording...");
                
                // Configure OBS recording settings
                boolean configured = configureOBSRecording(config);
                if (!configured) {
                    updateStatus("Failed to configure OBS");
                    return false;
                }
                
                // Start recording via WebSocket API
                boolean started = sendOBSCommand("StartRecord");
                if (started) {
                    Platform.runLater(() -> {
                        recordingProperty.set(true);
                        pausedProperty.set(false);
                    });
                    updateStatus("Recording started");
                    return true;
                } else {
                    updateStatus("Failed to start recording");
                    return false;
                }
                
            } catch (Exception e) {
                updateStatus("Recording start failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Stop current recording
     */
    public CompletableFuture<Boolean> stopRecording() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!recordingProperty.get()) {
                    updateStatus("Not currently recording");
                    return false;
                }
                
                updateStatus("Stopping recording...");
                
                boolean stopped = sendOBSCommand("StopRecord");
                if (stopped) {
                    Platform.runLater(() -> {
                        recordingProperty.set(false);
                        pausedProperty.set(false);
                    });
                    updateStatus("Recording stopped");
                    return true;
                } else {
                    updateStatus("Failed to stop recording");
                    return false;
                }
                
            } catch (Exception e) {
                updateStatus("Recording stop failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Toggle pause/resume recording
     */
    public CompletableFuture<Boolean> togglePause() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!recordingProperty.get()) {
                    updateStatus("Not currently recording");
                    return false;
                }
                
                boolean currentlyPaused = pausedProperty.get();
                String command = currentlyPaused ? "ResumeRecord" : "PauseRecord";
                String action = currentlyPaused ? "Resuming" : "Pausing";
                
                updateStatus(action + " recording...");
                
                boolean success = sendOBSCommand(command);
                if (success) {
                    Platform.runLater(() -> pausedProperty.set(!currentlyPaused));
                    updateStatus("Recording " + (currentlyPaused ? "resumed" : "paused"));
                    return true;
                } else {
                    updateStatus("Failed to " + action.toLowerCase() + " recording");
                    return false;
                }
                
            } catch (Exception e) {
                updateStatus("Pause/Resume failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Check if bundled OBS is available
     */
    public boolean isOBSBundled() {
        return bundleManager.isOBSBundled();
    }
    
    /**
     * Get the size of OBS installation in MB
     */
    public double getOBSInstallationSizeMB() {
        return bundleManager.getOBSInstallationSize() / (1024.0 * 1024.0);
    }
    
    /**
     * Remove bundled OBS installation
     */
    public CompletableFuture<Boolean> removeBundledOBS() {
        return CompletableFuture.supplyAsync(() -> {
            shutdown();
            boolean removed = bundleManager.removeBundledOBS();
            if (removed) {
                Platform.runLater(() -> obsBundledProperty.set(false));
            }
            return removed;
        });
    }
    
    /**
     * Shutdown the service and stop bundled OBS
     */
    public void shutdown() {
        try {
            // Stop any ongoing recording
            if (recordingProperty.get()) {
                stopRecording().get(5, TimeUnit.SECONDS);
            }
            
            // Stop bundled OBS
            bundleManager.stopBundledOBS();
            
            // Shutdown executors
            scheduledExecutor.shutdown();
            executorService.shutdown();
            
            updateStatus("Service stopped");
            
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }
    
    // Property getters
    public BooleanProperty recordingProperty() { return recordingProperty; }
    public BooleanProperty pausedProperty() { return pausedProperty; }
    public StringProperty statusProperty() { return statusProperty; }
    public BooleanProperty obsConnectedProperty() { return obsConnectedProperty; }
    public BooleanProperty obsBundledProperty() { return obsBundledProperty; }
    
    // Private helper methods
    
    private void startConnectionMonitoring() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                boolean wasConnected = obsConnectedProperty.get();
                boolean isConnected = testOBSConnection();
                
                if (wasConnected != isConnected) {
                    Platform.runLater(() -> obsConnectedProperty.set(isConnected));
                    if (isConnected) {
                        updateStatus("Connected to OBS");
                    } else {
                        updateStatus("Lost connection to OBS");
                        Platform.runLater(() -> recordingProperty.set(false));
                    }
                }
            } catch (Exception e) {
                // Ignore monitoring errors
            }
        }, 2, 5, TimeUnit.SECONDS);
    }
    
    private boolean waitForOBSConnection(int timeoutSeconds) {
        System.out.println("Waiting for OBS WebSocket connection (timeout: " + timeoutSeconds + "s)...");
        for (int i = 0; i < timeoutSeconds; i++) {
            if (testOBSConnection()) {
                System.out.println("OBS WebSocket connection established after " + (i + 1) + " seconds");
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        System.out.println("Timeout: Failed to connect to OBS WebSocket after " + timeoutSeconds + " seconds");
        return false;
    }
    
    private boolean testOBSConnection() {
        // First try socket connection (faster and more reliable)
        if (testWebSocketConnection()) {
            return true;
        }
        
        // Fallback to HTTP test
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            
            // Test WebSocket endpoint - OBS WebSocket server responds to HTTP requests
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + obsWebSocketHost + ":" + obsWebSocketPort))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "ChubbyRecorder/1.0")
                .build();
            
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            // OBS WebSocket server returns various codes when running:
            // 404 - WebSocket endpoint not found (but server is running)
            // 400 - Bad request (but server is running)
            // 426 - Upgrade required (WebSocket server is running)
            return response.statusCode() == 404 || 
                   response.statusCode() == 400 || 
                   response.statusCode() == 426;
            
        } catch (java.net.ConnectException e) {
            // Connection refused - server not running
            System.out.println("OBS WebSocket server not responding: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.out.println("Error testing OBS connection: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Alternative method to test WebSocket connection using actual WebSocket protocol
     */
    private boolean testWebSocketConnection() {
        try {
            // Try to connect via WebSocket protocol
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(obsWebSocketHost, obsWebSocketPort), 3000);
            
            // If we can connect, the port is open
            socket.close();
            System.out.println("OBS WebSocket port " + obsWebSocketPort + " is accessible");
            return true;
            
        } catch (Exception e) {
            System.out.println("OBS WebSocket port " + obsWebSocketPort + " is not accessible: " + e.getMessage());
            return false;
        }
    }
    
    private boolean connectWebSocket() {
        try {
            if (wsClient != null && wsClient.isOpen()) return true;
            final Object lock = new Object();
            final boolean[] identified = {false};
            wsClient = new WebSocketClient(new URI("ws://" + obsWebSocketHost + ":" + obsWebSocketPort)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("OBS WebSocket connected");
                    // Send Identify
                    try {
                        Map<String, Object> identify = Map.of(
                            "op", 1,
                            "d", Map.of("rpcVersion", 1)
                        );
                        wsClient.send(objectMapper.writeValueAsString(identify));
                    } catch (Exception e) {
                        System.err.println("Failed to send Identify: " + e.getMessage());
                    }
                }
                @Override
                public void onMessage(String message) {
                    try {
                        Map<?,?> msg = objectMapper.readValue(message, Map.class);
                        if (msg.get("op") instanceof Number && ((Number)msg.get("op")).intValue() == 2) {
                            synchronized(lock) {
                                identified[0] = true;
                                lock.notifyAll();
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("OBS WebSocket closed: " + reason);
                }
                @Override
                public void onError(Exception ex) {
                    System.err.println("OBS WebSocket error: " + ex.getMessage());
                }
            };
            wsClient.connectBlocking();
            // Wait for Identify handshake
            synchronized(lock) {
                long start = System.currentTimeMillis();
                while (!identified[0] && (System.currentTimeMillis() - start) < 5000) {
                    lock.wait(1000);
                }
            }
            if (!identified[0]) {
                System.err.println("OBS WebSocket: Identify handshake failed");
                wsClient.close();
                return false;
            }
            return wsClient.isOpen();
        } catch (Exception e) {
            System.err.println("Failed to connect to OBS WebSocket: " + e.getMessage());
            return false;
        }
    }

    private boolean sendOBSCommand(String command) {
        try {
            if (!connectWebSocket()) return false;
            String requestType;
            switch (command) {
                case "StartRecord":
                    requestType = "StartRecord";
                    break;
                case "StopRecord":
                    requestType = "StopRecord";
                    break;
                case "PauseRecord":
                    requestType = "PauseRecord";
                    break;
                case "ResumeRecord":
                    requestType = "ResumeRecord";
                    break;
                default:
                    return false;
            }
            Map<String, Object> req = Map.of(
                "op", 6,
                "d", Map.of(
                    "requestType", requestType,
                    "requestId", "req-" + messageId.getAndIncrement()
                )
            );
            String json = objectMapper.writeValueAsString(req);
            wsClient.send(json);
            // Optionally wait for response
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send OBS command: " + e.getMessage());
            return false;
        }
    }

    private boolean configureOBSRecording(RecordingConfig config) {
        try {
            if (!connectWebSocket()) return false;
            // 1. Set output directory
            String outputDir = config.getOutputDirectory().getAbsolutePath();
            Map<String, Object> setDirReq = Map.of(
                "op", 6,
                "d", Map.of(
                    "requestType", "SetRecordDirectory",
                    "requestId", "req-" + messageId.getAndIncrement(),
                    "requestData", Map.of("recordDirectory", outputDir)
                )
            );
            wsClient.send(objectMapper.writeValueAsString(setDirReq));

            // 2. Set output format (container)
            String format = config.getOutputFormat();
            Map<String, Object> setFormatReq = Map.of(
                "op", 6,
                "d", Map.of(
                    "requestType", "SetRecordSettings",
                    "requestId", "req-" + messageId.getAndIncrement(),
                    "requestData", Map.of("rec_format", format)
                )
            );
            wsClient.send(objectMapper.writeValueAsString(setFormatReq));

            // 3. Set resolution
            int width = config.getResolution().getWidth();
            int height = config.getResolution().getHeight();
            Map<String, Object> setResReq = Map.of(
                "op", 6,
                "d", Map.of(
                    "requestType", "SetVideoSettings",
                    "requestId", "req-" + messageId.getAndIncrement(),
                    "requestData", Map.of(
                        "baseWidth", width,
                        "baseHeight", height,
                        "outputWidth", width,
                        "outputHeight", height
                    )
                )
            );
            wsClient.send(objectMapper.writeValueAsString(setResReq));

            // 4. Set video source (scene)
            if (config.getVideoSource() != null) {
                String sceneName = config.getVideoSource().getName();
                Map<String, Object> setSceneReq = Map.of(
                    "op", 6,
                    "d", Map.of(
                        "requestType", "SetCurrentProgramScene",
                        "requestId", "req-" + messageId.getAndIncrement(),
                        "requestData", Map.of("sceneName", sceneName)
                    )
                );
                wsClient.send(objectMapper.writeValueAsString(setSceneReq));
            }

            // 5. Set audio source (if needed)
            if (config.getAudioSource() != null) {
                String audioInput = config.getAudioSource().getName();
                Map<String, Object> setAudioReq = Map.of(
                    "op", 6,
                    "d", Map.of(
                        "requestType", "SetInputSettings",
                        "requestId", "req-" + messageId.getAndIncrement(),
                        "requestData", Map.of(
                            "inputName", audioInput,
                            "inputSettings", Map.of()
                        )
                    )
                );
                wsClient.send(objectMapper.writeValueAsString(setAudioReq));
            }

            // Optionally wait for responses/acks here
            return true;
        } catch (Exception e) {
            System.err.println("Failed to configure OBS: " + e.getMessage());
            return false;
        }
    }
    
    private void updateStatus(String status) {
        Platform.runLater(() -> statusProperty.set(status));
        System.out.println("OBS Status: " + status);
    }
}
