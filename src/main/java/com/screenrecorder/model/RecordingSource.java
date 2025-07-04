package com.screenrecorder.model;

/**
 * Represents different recording sources
 */
public class RecordingSource {
    private final String name;
    private final String identifier;
    private final SourceType type;
    
    public enum SourceType {
        FULL_SCREEN,
        WINDOW,
        AUDIO_DEVICE
    }
    
    public RecordingSource(String name, String identifier, SourceType type) {
        this.name = name;
        this.identifier = identifier;
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public SourceType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RecordingSource that = (RecordingSource) obj;
        return identifier.equals(that.identifier) && type == that.type;
    }
    
    @Override
    public int hashCode() {
        return identifier.hashCode() + type.hashCode();
    }
}
