package com.screenrecorder.test;

import com.screenrecorder.service.OBSBundleManager;

/**
 * Simple test to verify OBS Bundle Manager functionality
 */
public class OBSBundleTest {
    
    public static void main(String[] args) {
        System.out.println("Testing OBS Bundle Manager...");
        
        OBSBundleManager manager = new OBSBundleManager();
        
        try {
            // Test 1: Check if OBS is bundled
            System.out.println("\n=== Test 1: Check OBS Bundle Status ===");
            boolean bundled = manager.isOBSBundled();
            System.out.println("OBS Bundled: " + bundled);
            
            if (!bundled) {
                // Test 2: Download and install OBS
                System.out.println("\n=== Test 2: Download OBS Studio ===");
                boolean downloaded = manager.downloadAndInstallOBS().get();
                System.out.println("Download successful: " + downloaded);
                
                if (!downloaded) {
                    System.out.println("Failed to download OBS. Exiting test.");
                    return;
                }
            }
            
            // Test 3: Start OBS
            System.out.println("\n=== Test 3: Start OBS Studio ===");
            boolean started = manager.startBundledOBS().get();
            System.out.println("OBS started: " + started);
            
            if (started) {
                // Wait a bit
                System.out.println("Waiting 10 seconds...");
                Thread.sleep(10000);
                
                // Test 4: Check if OBS is running
                System.out.println("\n=== Test 4: Check OBS Running Status ===");
                boolean running = manager.isOBSRunning();
                System.out.println("OBS running: " + running);
                
                // Test 5: Stop OBS
                System.out.println("\n=== Test 5: Stop OBS Studio ===");
                manager.stopBundledOBS();
                System.out.println("OBS stop command sent");
                
                // Wait for shutdown
                Thread.sleep(3000);
                
                // Check if stopped
                boolean stillRunning = manager.isOBSRunning();
                System.out.println("OBS still running: " + stillRunning);
            }
            
            // Test 6: Installation size
            System.out.println("\n=== Test 6: Installation Size ===");
            long size = manager.getOBSInstallationSize();
            System.out.println("OBS installation size: " + (size / 1024 / 1024) + " MB");
            
            System.out.println("\n=== Test Complete ===");
            System.out.println("OBS Bundle Manager test completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
