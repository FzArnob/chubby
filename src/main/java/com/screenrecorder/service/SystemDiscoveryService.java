package com.screenrecorder.service;

import com.screenrecorder.model.RecordingSource;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for discovering available recording sources (windows, audio devices)
 */
public class SystemDiscoveryService {
    private final ExecutorService executorService;
    
    public SystemDiscoveryService() {
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Get available video sources (screen and windows)
     */
    public CompletableFuture<List<RecordingSource>> getVideoSources() {
        return CompletableFuture.supplyAsync(() -> {
            List<RecordingSource> sources = new ArrayList<>();
            
            // Add full screen option
            sources.add(new RecordingSource("Full Screen", "desktop", RecordingSource.SourceType.FULL_SCREEN));
            
            // Add available windows
            sources.addAll(getAvailableWindows());
            
            return sources;
        }, executorService);
    }
    
    /**
     * Get available audio sources
     */
    public CompletableFuture<List<RecordingSource>> getAudioSources() {
        return CompletableFuture.supplyAsync(() -> {
            List<RecordingSource> sources = new ArrayList<>();
            
            // Add default audio devices
            sources.add(new RecordingSource("System Audio (Stereo Mix)", "Stereo Mix", RecordingSource.SourceType.AUDIO_DEVICE));
            sources.add(new RecordingSource("Microphone", "Microphone", RecordingSource.SourceType.AUDIO_DEVICE));
            
            // Try to discover more audio devices using FFmpeg
            sources.addAll(getAvailableAudioDevices());
            
            return sources;
        }, executorService);
    }
    
    /**
     * Get available windows on Windows using PowerShell
     */
    private List<RecordingSource> getAvailableWindows() {
        List<RecordingSource> windows = new ArrayList<>();
        
        try {
            // Use PowerShell to get window titles
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe", 
                "-Command", 
                "Get-Process | Where-Object {$_.MainWindowTitle -ne ''} | Select-Object MainWindowTitle"
            );
            
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean skipHeader = true;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Skip header lines
                    if (skipHeader) {
                        if (line.equals("MainWindowTitle") || line.startsWith("---")) {
                            continue;
                        }
                        skipHeader = false;
                    }
                    
                    if (!line.isEmpty() && !line.equals("MainWindowTitle")) {
                        windows.add(new RecordingSource(line, line, RecordingSource.SourceType.WINDOW));
                    }
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            // Fallback: add some common window patterns
            windows.add(new RecordingSource("Chrome Browser", "Chrome", RecordingSource.SourceType.WINDOW));
            windows.add(new RecordingSource("Firefox Browser", "Firefox", RecordingSource.SourceType.WINDOW));
            windows.add(new RecordingSource("Visual Studio Code", "Visual Studio Code", RecordingSource.SourceType.WINDOW));
        }
        
        return windows;
    }
    
    /**
     * Get available audio devices using FFmpeg
     */
    private List<RecordingSource> getAvailableAudioDevices() {
        List<RecordingSource> devices = new ArrayList<>();
        
        try {
            // Use FFmpeg to list DirectShow devices
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-list_devices", "true", "-f", "dshow", "-i", "dummy");
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                boolean inAudioSection = false;
                
                while ((line = reader.readLine()) != null) {
                    if (line.contains("DirectShow audio devices")) {
                        inAudioSection = true;
                        continue;
                    }
                    
                    if (inAudioSection && line.contains("DirectShow video devices")) {
                        break;
                    }
                    
                    if (inAudioSection && line.contains("\"")) {
                        // Extract device name from quotes
                        int start = line.indexOf("\"") + 1;
                        int end = line.lastIndexOf("\"");
                        if (start < end) {
                            String deviceName = line.substring(start, end);
                            devices.add(new RecordingSource(deviceName, deviceName, RecordingSource.SourceType.AUDIO_DEVICE));
                        }
                    }
                }
            }
            
            process.waitFor();
            
        } catch (Exception e) {
            // Ignore errors, use default devices only
        }
        
        return devices;
    }
    
    /**
     * Create a background task for discovering video sources
     */
    public Task<List<RecordingSource>> createVideoSourceDiscoveryTask() {
        return new Task<List<RecordingSource>>() {
            @Override
            protected List<RecordingSource> call() throws Exception {
                updateTitle("Discovering video sources...");
                return getVideoSources().get();
            }
        };
    }
    
    /**
     * Create a background task for discovering audio sources
     */
    public Task<List<RecordingSource>> createAudioSourceDiscoveryTask() {
        return new Task<List<RecordingSource>>() {
            @Override
            protected List<RecordingSource> call() throws Exception {
                updateTitle("Discovering audio sources...");
                return getAudioSources().get();
            }
        };
    }
    
    /**
     * Shutdown the service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
