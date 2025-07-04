package com.screenrecorder.model;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuration class for recording settings
 */
public class RecordingConfig {
    private RecordingSource videoSource;
    private RecordingSource audioSource;
    private Resolution resolution;
    private File outputDirectory;
    private boolean recordSystemAudio;
    private boolean recordMicrophone;
    private boolean separateAudioOutput;
    private String outputFormat;
    
    public RecordingConfig() {
        this.outputDirectory = new File(System.getProperty("user.home"), "ScreenRecordings");
        this.recordSystemAudio = true;
        this.recordMicrophone = false;
        this.separateAudioOutput = false;
        this.outputFormat = "mp4";
        this.resolution = Resolution.HD_1080P;
    }
    
    // Getters and setters
    public RecordingSource getVideoSource() {
        return videoSource;
    }
    
    public void setVideoSource(RecordingSource videoSource) {
        this.videoSource = videoSource;
    }
    
    public RecordingSource getAudioSource() {
        return audioSource;
    }
    
    public void setAudioSource(RecordingSource audioSource) {
        this.audioSource = audioSource;
    }
    
    public Resolution getResolution() {
        return resolution;
    }
    
    public void setResolution(Resolution resolution) {
        this.resolution = resolution;
    }
    
    public File getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public boolean isRecordSystemAudio() {
        return recordSystemAudio;
    }
    
    public void setRecordSystemAudio(boolean recordSystemAudio) {
        this.recordSystemAudio = recordSystemAudio;
    }
    
    public boolean isRecordMicrophone() {
        return recordMicrophone;
    }
    
    public void setRecordMicrophone(boolean recordMicrophone) {
        this.recordMicrophone = recordMicrophone;
    }
    
    public boolean isSeparateAudioOutput() {
        return separateAudioOutput;
    }
    
    public void setSeparateAudioOutput(boolean separateAudioOutput) {
        this.separateAudioOutput = separateAudioOutput;
    }
    
    public String getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    /**
     * Generate a filename with timestamp
     */
    public String generateFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        return "ScreenRecording_" + timestamp;
    }
    
    /**
     * Get the full output file path for video
     */
    public File getVideoOutputFile() {
        return new File(outputDirectory, generateFileName() + "." + outputFormat);
    }
    
    /**
     * Get the full output file path for audio (when separate)
     */
    public File getAudioOutputFile() {
        return new File(outputDirectory, generateFileName() + "_audio.aac");
    }
}
