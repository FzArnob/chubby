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
            ffmpegProcess.destroy();
            try {
                ffmpegProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
                } catch (IOException e) {
                    // Fallback to destroy
                    ffmpegProcess.destroy();
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
        
        // Audio input - only if audio recording is enabled
        // For now, disable audio to ensure video recording works first
        // TODO: Fix audio recording later
        command.add("-an"); // No audio for now
        
        /*
        if (config.isRecordSystemAudio() || config.isRecordMicrophone()) {
            addAudioCaptureArgs(command, config);
        } else {
            // No audio recording - add -an flag to disable audio
            command.add("-an");
        }
        */
        
        // Video encoding settings
        command.add("-c:v");
        command.add("libx264");
        command.add("-preset");
        command.add("fast");
        command.add("-crf");
        command.add("23");
        
        // Audio encoding settings (only if recording audio)
        // Disabled for now until audio recording is fixed
        /*
        if (config.isRecordSystemAudio() || config.isRecordMicrophone()) {
            command.add("-c:a");
            command.add("aac");
            command.add("-b:a");
            command.add("128k");
        }
        */
        
        // Frame rate
        command.add("-r");
        command.add("30");
        
        // Output resolution (if specified)
        if (config.getResolution() != null) {
            command.add("-s");
            command.add(config.getResolution().getResolutionString());
        }
        
        // Duration limit (remove for continuous recording)
        // command.add("-t");
        // command.add("3600"); // 1 hour max
        
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
        // Try to add system audio first
        if (config.isRecordSystemAudio()) {
            try {
                // Windows: Use dshow for audio capture
                // Try common system audio device names
                command.add("-f");
                command.add("dshow");
                command.add("-i");
                command.add("audio=\"Stereo Mix\""); // Default system audio
            } catch (Exception e) {
                // If system audio fails, continue without it
                System.err.println("Warning: Could not add system audio: " + e.getMessage());
            }
        }
        
        // Add microphone if enabled
        if (config.isRecordMicrophone()) {
            try {
                // Add microphone input
                command.add("-f");
                command.add("dshow");
                command.add("-i");
                command.add("audio=\"Microphone\""); // Default microphone
            } catch (Exception e) {
                // If microphone fails, continue without it
                System.err.println("Warning: Could not add microphone: " + e.getMessage());
            }
        }
        
        // If both audio sources failed or are disabled, disable audio
        if (!config.isRecordSystemAudio() && !config.isRecordMicrophone()) {
            command.add("-an"); // No audio
        }
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
                        outputLine.toLowerCase().contains("invalid")) {
                        hasError[0] = true;
                        errorOutput.append(outputLine).append("\n");
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
