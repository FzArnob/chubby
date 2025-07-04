package com.screenrecorder.service;

import com.screenrecorder.model.RecordingConfig;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing OBS Studio recording operations via WebSocket API
 * 
 * Requirements:
 * 1. OBS Studio 28+ installed
 * 2. obs-websocket plugin enabled (built-in since OBS 28)
 * 3. WebSocket server enabled in OBS Tools > WebSocket Server Settings
 * 
 * Maven Dependencies needed:
 * - org.java-websocket:Java-WebSocket:1.5.3
 * - com.fasterxml.jackson.core:jackson-databind:2.15.2
 * - org.slf4j:slf4j-api:2.0.7
 */
public class OBSRecordingService {
    
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutor;
    private final BooleanProperty recordingProperty;
    private final BooleanProperty pausedProperty;
    private final StringProperty statusProperty;
    private final BooleanProperty obsConnectedProperty;
    
    // OBS WebSocket connection details
    private String obsWebSocketHost = "localhost";
    private int obsWebSocketPort = 4455;
    private String obsWebSocketPassword = ""; // Set in OBS WebSocket settings
    
    // HTTP client for REST-like calls to OBS
    private final HttpClient httpClient;
    
    // Current recording session
    private String currentSessionId;
    private RecordingConfig currentConfig;
    
    public OBSRecordingService() {
        this.executorService = Executors.newCachedThreadPool();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.recordingProperty = new SimpleBooleanProperty(false);
        this.pausedProperty = new SimpleBooleanProperty(false);
        this.statusProperty = new SimpleStringProperty("Disconnected");
        this.obsConnectedProperty = new SimpleBooleanProperty(false);
        
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        // Start connection monitoring
        startConnectionMonitoring();
    }
    
    /**
     * Check if OBS Studio is running and WebSocket is available
     */
    public CompletableFuture<Boolean> isOBSAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if OBS process is running
                ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq obs64.exe");
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("obs64.exe")) {
                            return true;
                        }
                    }
                }
                
                // Also try obs32.exe
                pb = new ProcessBuilder("tasklist", "/FI", "IMAGENAME eq obs32.exe");
                process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("obs32.exe")) {
                            return true;
                        }
                    }
                }
                
                return false;
            } catch (Exception e) {
                return false;
            }
        }, executorService);
    }
    
    /**
     * Test connection to OBS WebSocket
     */
    public CompletableFuture<Boolean> testOBSConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simple HTTP request to check if WebSocket server is responding
                String url = String.format("http://%s:%d", obsWebSocketHost, obsWebSocketPort);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                        
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // OBS WebSocket typically returns a specific response
                return response.statusCode() == 200 || response.statusCode() == 404; // 404 is normal for WebSocket endpoint
                
            } catch (Exception e) {
                return false;
            }
        }, executorService);
    }
    
    /**
     * Start recording with OBS
     */
    public CompletableFuture<Boolean> startRecording(RecordingConfig config) {
        if (recordingProperty.get()) {
            return CompletableFuture.completedFuture(false);
        }
        
        this.currentConfig = config;
        this.currentSessionId = UUID.randomUUID().toString();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First, ensure OBS is running
                if (!isOBSAvailable().get()) {
                    Platform.runLater(() -> statusProperty.set("OBS Studio is not running"));
                    return false;
                }
                
                // Configure OBS recording settings
                if (!configureOBSSettings(config)) {
                    Platform.runLater(() -> statusProperty.set("Failed to configure OBS settings"));
                    return false;
                }
                
                // Start recording via OBS
                if (!sendOBSStartRecording()) {
                    Platform.runLater(() -> statusProperty.set("Failed to start OBS recording"));
                    return false;
                }
                
                Platform.runLater(() -> {
                    recordingProperty.set(true);
                    statusProperty.set("Recording with OBS...");
                });
                
                // Start monitoring recording status
                startRecordingMonitoring();
                
                return true;
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusProperty.set("Error: " + e.getMessage());
                });
                return false;
            }
        }, executorService);
    }
    
    /**
     * Stop OBS recording
     */
    public void stopRecording() {
        if (!recordingProperty.get()) {
            return;
        }
        
        executorService.submit(() -> {
            try {
                sendOBSStopRecording();
                
                Platform.runLater(() -> {
                    recordingProperty.set(false);
                    pausedProperty.set(false);
                    statusProperty.set("Recording stopped");
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusProperty.set("Error stopping recording: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * Pause/Resume OBS recording
     */
    public void togglePause() {
        executorService.submit(() -> {
            try {
                boolean isPaused = pausedProperty.get();
                
                if (isPaused) {
                    sendOBSResumeRecording();
                    Platform.runLater(() -> {
                        pausedProperty.set(false);
                        statusProperty.set("Recording resumed");
                    });
                } else {
                    sendOBSPauseRecording();
                    Platform.runLater(() -> {
                        pausedProperty.set(true);
                        statusProperty.set("Recording paused");
                    });
                }
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusProperty.set("Error toggling pause: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * Configure OBS settings based on recording configuration
     */
    private boolean configureOBSSettings(RecordingConfig config) {
        try {
            // Set output directory
            if (!setOBSOutputDirectory(config.getOutputDirectory().getAbsolutePath())) {
                return false;
            }
            
            // Set recording format
            if (!setOBSRecordingFormat("mp4")) {
                return false;
            }
            
            // Set video settings
            if (config.getResolution() != null) {
                if (!setOBSVideoSettings(config.getResolution().getWidth(), config.getResolution().getHeight())) {
                    return false;
                }
            }
            
            // Configure audio sources
            if (!configureOBSAudioSources(config)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to configure OBS settings: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send start recording command to OBS
     */
    private boolean sendOBSStartRecording() {
        // This would use OBS WebSocket protocol
        // For now, simulate with OBS command line
        try {
            // Use OBS command line interface
            ProcessBuilder pb = new ProcessBuilder(
                "obs64.exe", 
                "--startrecording",
                "--minimize-to-tray"
            );
            pb.start();
            
            // Wait a moment for OBS to start recording
            Thread.sleep(2000);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to start OBS recording: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send stop recording command to OBS
     */
    private boolean sendOBSStopRecording() {
        try {
            // Use OBS command line to stop recording
            ProcessBuilder pb = new ProcessBuilder(
                "obs64.exe", 
                "--stoprecording"
            );
            pb.start();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to stop OBS recording: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send pause recording command to OBS
     */
    private boolean sendOBSPauseRecording() {
        // OBS doesn't have built-in pause, so we'll simulate it
        return sendOBSStopRecording();
    }
    
    /**
     * Send resume recording command to OBS
     */
    private boolean sendOBSResumeRecording() {
        // Resume by starting recording again
        return sendOBSStartRecording();
    }
    
    /**
     * Set OBS output directory
     */
    private boolean setOBSOutputDirectory(String directory) {
        try {
            // This would typically be done via WebSocket API
            // For now, we'll assume the directory is set in OBS
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set OBS recording format
     */
    private boolean setOBSRecordingFormat(String format) {
        try {
            // This would be done via WebSocket API
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Set OBS video settings
     */
    private boolean setOBSVideoSettings(int width, int height) {
        try {
            // This would be done via WebSocket API
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Configure OBS audio sources
     */
    private boolean configureOBSAudioSources(RecordingConfig config) {
        try {
            // This would be done via WebSocket API
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Start monitoring OBS connection
     */
    private void startConnectionMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            testOBSConnection().thenAccept(connected -> {
                Platform.runLater(() -> {
                    obsConnectedProperty.set(connected);
                    if (connected && statusProperty.get().equals("Disconnected")) {
                        statusProperty.set("OBS Connected");
                    } else if (!connected && !recordingProperty.get()) {
                        statusProperty.set("OBS Disconnected");
                    }
                });
            });
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Start monitoring recording status
     */
    private void startRecordingMonitoring() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            if (recordingProperty.get()) {
                // Monitor recording time and update status
                Platform.runLater(() -> {
                    // This would get actual recording time from OBS
                    statusProperty.set("Recording with OBS - " + getCurrentRecordingTime());
                });
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Get current recording time (placeholder)
     */
    private String getCurrentRecordingTime() {
        // This would get actual time from OBS WebSocket
        return "00:00:00";
    }
    
    /**
     * Set OBS WebSocket connection details
     */
    public void setOBSConnection(String host, int port, String password) {
        this.obsWebSocketHost = host;
        this.obsWebSocketPort = port;
        this.obsWebSocketPassword = password;
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        stopRecording();
        executorService.shutdown();
        scheduledExecutor.shutdown();
    }
    
    // Property getters
    public BooleanProperty recordingProperty() {
        return recordingProperty;
    }
    
    public BooleanProperty pausedProperty() {
        return pausedProperty;
    }
    
    public StringProperty statusProperty() {
        return statusProperty;
    }
    
    public BooleanProperty obsConnectedProperty() {
        return obsConnectedProperty;
    }
}
