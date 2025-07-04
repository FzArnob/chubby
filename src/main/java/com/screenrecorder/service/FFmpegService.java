package com.screenrecorder.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.screenrecorder.model.RecordingConfig;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Service for managing FFmpeg recording operations
 */
public class FFmpegService {
    private Process ffmpegProcess;
    private final ExecutorService executorService;
    private final BooleanProperty recordingProperty;
    private final BooleanProperty pausedProperty;
    private final StringProperty statusProperty;
    private RecordingConfig currentConfig;
    
    public FFmpegService() {
        this.executorService = Executors.newCachedThreadPool();
        this.recordingProperty = new SimpleBooleanProperty(false);
        this.pausedProperty = new SimpleBooleanProperty(false);
        this.statusProperty = new SimpleStringProperty("Idle");
    }
    
    /**
     * Check if FFmpeg is available on the system
     */
    public CompletableFuture<Boolean> isFFmpegAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
                Process process = pb.start();
                int exitCode = process.waitFor();
                return exitCode == 0;
            } catch (Exception e) {
                return false;
            }
        }, executorService);
    }
    
    /**
     * Start recording with the given configuration
     */
    public CompletableFuture<Boolean> startRecording(RecordingConfig config) {
        if (recordingProperty.get()) {
            return CompletableFuture.completedFuture(false);
        }
        
        this.currentConfig = config;
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Ensure output directory exists
                if (!config.getOutputDirectory().exists()) {
                    config.getOutputDirectory().mkdirs();
                }
                
                List<String> command = buildFFmpegCommand(config);
                
                // Debug: Print the FFmpeg command
                System.out.println("FFmpeg command: " + String.join(" ", command));
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                
                // Set working directory
                pb.directory(config.getOutputDirectory());
                
                ffmpegProcess = pb.start();
                
                Platform.runLater(() -> {
                    recordingProperty.set(true);
                    statusProperty.set("Recording started...");
                });
                
                // Monitor FFmpeg output in background
                monitorFFmpegOutput();
                
                return true;
            } catch (Exception e) {
                e.printStackTrace(); // Debug: Print full stack trace
                Platform.runLater(() -> {
                    statusProperty.set("Error: " + e.getMessage());
                });
                return false;
            }
        }, executorService);
    }
    
    /**
     * Stop the current recording
     */
    public void stopRecording() {
        if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
            try {
                // Send 'q' command to FFmpeg for graceful shutdown
                ffmpegProcess.getOutputStream().write("q\n".getBytes());
                ffmpegProcess.getOutputStream().flush();
                
                // Close the output stream to signal FFmpeg we're done sending commands
                ffmpegProcess.getOutputStream().close();
                
                // Wait indefinitely for FFmpeg to finish and properly close the video file
                // This ensures the video file is always properly finalized, no matter how long it takes
                System.out.println("Waiting for FFmpeg to finish writing video file...");
                ffmpegProcess.waitFor();
                System.out.println("FFmpeg shutdown gracefully");
                
            } catch (IOException e) {
                // If sending 'q' fails, try graceful destroy first
                System.out.println("Could not send quit command to FFmpeg, trying graceful destroy");
                ffmpegProcess.destroy();
                try {
                    // Wait indefinitely for graceful shutdown
                    ffmpegProcess.waitFor();
                    System.out.println("FFmpeg shutdown gracefully after destroy signal");
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    ffmpegProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ffmpegProcess.destroyForcibly();
            }
        }
        
        Platform.runLater(() -> {
            recordingProperty.set(false);
            pausedProperty.set(false);
            statusProperty.set("Idle");
        });
    }
    
    /**
     * Pause/Resume recording (simulate by stopping and restarting)
     */
    public void togglePause() {
        boolean isPaused = pausedProperty.get();
        pausedProperty.set(!isPaused);
        
        if (!isPaused) {
            // Pausing - send 'q' to FFmpeg to stop gracefully
            if (ffmpegProcess != null && ffmpegProcess.isAlive()) {
                try {
                    ffmpegProcess.getOutputStream().write("q\n".getBytes());
                    ffmpegProcess.getOutputStream().flush();
                    ffmpegProcess.getOutputStream().close();
                    
                    // Wait indefinitely for graceful shutdown
                    ffmpegProcess.waitFor();
                } catch (IOException | InterruptedException e) {
                    // Fallback to destroy
                    ffmpegProcess.destroyForcibly();
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            Platform.runLater(() -> statusProperty.set("Paused"));
        } else {
            // Resuming - restart recording
            if (currentConfig != null) {
                startRecording(currentConfig);
            }
        }
    }
    
    /**
     * Build FFmpeg command based on configuration
     */
    private List<String> buildFFmpegCommand(RecordingConfig config) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y"); // Overwrite output files
        
        // Video input - always add video source first
        if (config.getVideoSource() != null) {
            switch (config.getVideoSource().getType()) {
                case FULL_SCREEN -> addScreenCaptureArgs(command);
                case WINDOW -> addWindowCaptureArgs(command, config);
                case AUDIO_DEVICE -> {
                    // Audio device not used for video input, use desktop as fallback
                    addScreenCaptureArgs(command);
                }
            }
        } else {
            // Fallback to desktop capture if no video source specified
            addScreenCaptureArgs(command);
        }
        
        // Audio input - handle audio properly for Windows Media Player compatibility
        if (config.isRecordSystemAudio() || config.isRecordMicrophone()) {
            // Try real audio first, but we'll add fallback logic in case it fails
            addAudioCaptureArgs(command, config);
        } else {
            // Even if not recording audio, add a silent audio track for better compatibility
            addSilentAudio(command);
        }
        
        // Video encoding settings
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("fast");
        command.add("-crf");
        command.add("23");
        command.add("-pix_fmt");
        command.add("yuv420p"); // Ensure compatibility with Windows Media Player
        
        // Audio encoding settings - always include for Windows Media Player compatibility
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-ar");
        command.add("48000"); // Sample rate
        command.add("-ac");
        command.add("2"); // Stereo channels
        
        // Frame rate
        command.add("-r");
        command.add("30");
        
        // Output resolution (if specified)
        if (config.getResolution() != null) {
            command.add("-s");
            command.add(config.getResolution().getResolutionString());
        }
        
        // MP4 container settings for Windows Media Player compatibility
        command.add("-movflags");
        command.add("+faststart"); // Move metadata to beginning of file for better streaming
        command.add("-strict");
        command.add("experimental");
        
        // Output file
        command.add(config.getVideoOutputFile().getAbsolutePath());
        
        return command;
    }
    
    /**
     * Add screen capture arguments for Windows
     */
    private void addScreenCaptureArgs(List<String> command) {
        // Windows: Use gdigrab for screen capture
        command.add("-f");
        command.add("gdigrab");
        command.add("-framerate");
        command.add("30");
        command.add("-i");
        command.add("desktop");
    }
    
    /**
     * Add window capture arguments
     */
    private void addWindowCaptureArgs(List<String> command, RecordingConfig config) {
        // Windows: Use gdigrab with window title
        command.add("-f");
        command.add("gdigrab");
        command.add("-framerate");
        command.add("30");
        command.add("-i");
        command.add("title=" + config.getVideoSource().getIdentifier());
    }
    /**
     * Add audio capture arguments for Windows
     */
    private void addAudioCaptureArgs(List<String> command, RecordingConfig config) {
        // For Windows, we need to handle audio inputs differently
        boolean hasSystemAudio = config.isRecordSystemAudio();
        boolean hasMicrophone = config.isRecordMicrophone();
        
        // Get the actual audio source names from config if available
        String systemAudioDevice = "Stereo Mix";
        String microphoneDevice = "Microphone";
        
        if (config.getAudioSource() != null) {
            String deviceName = config.getAudioSource().getIdentifier();
            if (config.isRecordSystemAudio() && deviceName.toLowerCase().contains("stereo") || 
                deviceName.toLowerCase().contains("system") || deviceName.toLowerCase().contains("speaker")) {
                systemAudioDevice = deviceName;
            } else if (config.isRecordMicrophone() && (deviceName.toLowerCase().contains("mic") || 
                      deviceName.toLowerCase().contains("input"))) {
                microphoneDevice = deviceName;
            }
        }
        
        if (hasSystemAudio && hasMicrophone) {
            // Record both system audio and microphone
            command.add("-f");
            command.add("dshow");
            command.add("-i");
            command.add("audio=" + systemAudioDevice);
            command.add("-f");
            command.add("dshow");
            command.add("-i");
            command.add("audio=" + microphoneDevice);
            command.add("-filter_complex");
            command.add("[1:a][2:a]amix=inputs=2[a]");
            command.add("-map");
            command.add("0:v");
            command.add("-map");
            command.add("[a]");
        } else if (hasSystemAudio) {
            // Record only system audio using dshow
            command.add("-f");
            command.add("dshow");
            command.add("-i");
            command.add("audio=" + systemAudioDevice);
        } else if (hasMicrophone) {
            // Record only microphone using dshow
            command.add("-f");
            command.add("dshow");
            command.add("-i");
            command.add("audio=" + microphoneDevice);
        }
    }
    
    /**
     * Add silent audio track for compatibility
     */
    private void addSilentAudio(List<String> command) {
        command.add("-f");
        command.add("lavfi");
        command.add("-i");
        command.add("anullsrc=channel_layout=stereo:sample_rate=48000");
    }
    
    /**
     * Monitor FFmpeg output for errors and progress
     */
    private void monitorFFmpegOutput() {
        executorService.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ffmpegProcess.getInputStream()))) {
                
                String line;
                final boolean[] hasError = {false};
                StringBuilder errorOutput = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    final String outputLine = line;
                    
                    // Debug: Print all FFmpeg output
                    System.out.println("FFmpeg: " + outputLine);
                    
                    // Check for errors
                    if (outputLine.toLowerCase().contains("error") || 
                        outputLine.toLowerCase().contains("failed") ||
                        outputLine.toLowerCase().contains("invalid") ||
                        outputLine.toLowerCase().contains("could not open") ||
                        outputLine.toLowerCase().contains("no such file")) {
                        hasError[0] = true;
                        errorOutput.append(outputLine).append("\n");
                        
                        // If it's an audio-related error, we might want to restart with silent audio
                        if (outputLine.toLowerCase().contains("audio") && 
                            (outputLine.toLowerCase().contains("error opening") ||
                             outputLine.toLowerCase().contains("could not open") ||
                             outputLine.toLowerCase().contains("unknown input format") ||
                             outputLine.toLowerCase().contains("invalid argument"))) {
                            System.err.println("Audio device error detected: " + outputLine);
                            System.err.println("Consider restarting recording with silent audio only");
                        }
                    }
                    
                    Platform.runLater(() -> {
                        // Parse FFmpeg output for progress, errors, etc.
                        if (outputLine.contains("time=")) {
                            // Extract time information for progress
                            String timeInfo = extractTimeInfo(outputLine);
                            if (!timeInfo.isEmpty()) {
                                statusProperty.set("Recording - " + timeInfo);
                            }
                        } else if (outputLine.contains("frame=")) {
                            // Show frame information
                            statusProperty.set("Recording - " + outputLine.trim());
                        } else if (hasError[0]) {
                            statusProperty.set("Error: " + outputLine);
                        }
                    });
                }
                
                // Process ended
                final boolean finalHasError = hasError[0];
                final String finalErrorOutput = errorOutput.toString();
                
                Platform.runLater(() -> {
                    recordingProperty.set(false);
                    pausedProperty.set(false);
                    if (finalHasError && !finalErrorOutput.trim().isEmpty()) {
                        statusProperty.set("Recording failed: " + finalErrorOutput.trim());
                    } else {
                        statusProperty.set("Recording completed");
                    }
                });
                
            } catch (IOException e) {
                e.printStackTrace(); // Debug: Print full stack trace
                Platform.runLater(() -> {
                    recordingProperty.set(false);
                    statusProperty.set("Error reading FFmpeg output: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * Extract time information from FFmpeg output
     */
    private String extractTimeInfo(String line) {
        int timeIndex = line.indexOf("time=");
        if (timeIndex != -1) {
            int endIndex = line.indexOf(" ", timeIndex + 5);
            if (endIndex == -1) endIndex = line.length();
            return line.substring(timeIndex + 5, endIndex);
        }
        return "";
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        stopRecording();
        executorService.shutdown();
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
}
