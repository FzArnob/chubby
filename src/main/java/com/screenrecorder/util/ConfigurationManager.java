package com.screenrecorder.util;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.screenrecorder.model.RecordingConfig;

/**
 * Utility for saving and loading configuration
 */
public class ConfigurationManager {
    private static final String CONFIG_FILE = "screen-recorder-config.json";
    private final ObjectMapper objectMapper;
    private final File configFile;
    
    public ConfigurationManager() {
        this.objectMapper = new ObjectMapper();
        this.configFile = new File(System.getProperty("user.home"), CONFIG_FILE);
    }
    
    /**
     * Save configuration to file
     */
    public void saveConfiguration(RecordingConfig config) {
        try {
            objectMapper.writeValue(configFile, config);
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }
    
    /**
     * Load configuration from file
     */
    public RecordingConfig loadConfiguration() {
        if (!configFile.exists()) {
            return new RecordingConfig(); // Return default config
        }
        
        try {
            return objectMapper.readValue(configFile, RecordingConfig.class);
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            return new RecordingConfig(); // Return default config on error
        }
    }
}
