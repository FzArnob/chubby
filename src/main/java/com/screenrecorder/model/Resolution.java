package com.screenrecorder.model;

/**
 * Represents different resolution options
 */
public class Resolution {
    private final String name;
    private final int width;
    private final int height;
    
    public Resolution(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }
    
    public String getName() {
        return name;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public String getResolutionString() {
        return width + "x" + height;
    }
    
    @Override
    public String toString() {
        return name + " (" + getResolutionString() + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Resolution that = (Resolution) obj;
        return width == that.width && height == that.height;
    }
    
    @Override
    public int hashCode() {
        return width * 31 + height;
    }
    
    // Predefined resolutions
    public static final Resolution HD_1080P = new Resolution("1080p", 1920, 1080);
    public static final Resolution QHD_2K = new Resolution("2K", 2560, 1440);
    public static final Resolution UHD_4K = new Resolution("4K", 3840, 2160);
}
