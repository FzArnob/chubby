package com.screenrecorder.service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Manages bundled OBS Studio installation for portable distribution
 * Downloads, extracts, and manages OBS Studio binaries
 */
public class OBSBundleManager {
    // Only use the latest OBS download URL
    private static final String OBS_DOWNLOAD_URL = "https://github.com/obsproject/obs-studio/releases/download/31.0.4/OBS-Studio-31.0.4-Windows.zip";
    private static final String OBS_FOLDER_NAME = "obs-studio";
    private static final String OBS_EXE_NAME = "obs64.exe";

    private final Path applicationDir;
    private final Path obsInstallDir;
    private final Path obsExecutable;
    private Process obsProcess;

    public OBSBundleManager() {
        this.applicationDir = getApplicationDirectory();
        this.obsInstallDir = applicationDir.resolve(OBS_FOLDER_NAME);
        this.obsExecutable = obsInstallDir.resolve("bin").resolve("64bit").resolve(OBS_EXE_NAME);
        addShutdownHook();
    }

    /**
     * Check if OBS is bundled and available
     */
    public boolean isOBSBundled() {
        return Files.exists(obsExecutable) && Files.isExecutable(obsExecutable);
    }

    /**
     * Download and install OBS Studio portable version (single URL)
     */
    public CompletableFuture<Boolean> downloadAndInstallOBS() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                System.out.println("Downloading OBS Studio portable...");

                // Create temp file for download
                Path tempZip = Files.createTempFile("obs-studio", ".zip");

                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMinutes(5))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

                System.out.println("Downloading from: " + OBS_DOWNLOAD_URL);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OBS_DOWNLOAD_URL))
                    .timeout(Duration.ofMinutes(10))
                    .header("User-Agent", "ChubbyRecorder/1.0")
                    .build();

                HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempZip));

                if (response.statusCode() != 200) {
                    System.err.println("Failed to download OBS: HTTP " + response.statusCode());
                    Files.deleteIfExists(tempZip);
                    return false;
                }

                System.out.println("Extracting OBS Studio...");

                // Extract zip to OBS directory
                extractZip(tempZip, obsInstallDir);

                // Clean up temp file
                Files.deleteIfExists(tempZip);
                
                // Create OBS configuration to prevent popups and enable WebSocket
                createOBSConfiguration();
                
                // Verify installation
                if (isOBSBundled()) {
                    System.out.println("OBS Studio bundled successfully!");
                    return true;
                } else {
                    System.err.println("OBS installation verification failed");
                    return false;
                }
                
            } catch (Exception e) {
                System.err.println("Failed to download/install OBS: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Start bundled OBS Studio in background with minimal UI and WebSocket enabled
     */
    public CompletableFuture<Boolean> startBundledOBS() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isOBSBundled()) {
                    System.err.println("OBS is not bundled. Please install first.");
                    return false;
                }

                killExistingOBS();

                System.out.println("Starting bundled OBS Studio...");

                // Clean up any crash recovery files before starting
                cleanupCrashRecovery();

                // Start OBS process in portable mode with WebSocket enabled
                ProcessBuilder pb = new ProcessBuilder(
                    obsExecutable.toString(),
                    "--portable",
                    "--minimize-to-tray",
                    "--disable-shutdown-check"
                );

                pb.directory(obsInstallDir.resolve("bin").resolve("64bit").toFile());

                obsProcess = pb.start();

                // Wait for OBS to start
                Thread.sleep(8000);

                if (obsProcess.isAlive()) {
                    System.out.println("Bundled OBS Studio started successfully");
                    return true;
                } else {
                    System.err.println("Failed to start bundled OBS Studio");
                    return false;
                }
            } catch (Exception e) {
                System.err.println("Error starting bundled OBS: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Stop bundled OBS Studio
     */
    public void stopBundledOBS() {
        gracefulShutdown();
    }

    public boolean isOBSRunning() {
        try {
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
            return false;
        } catch (Exception e) {
            return obsProcess != null && obsProcess.isAlive();
        }
    }

    private Path getApplicationDirectory() {
        try {
            String jarPath = OBSBundleManager.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            Path jarFile = Paths.get(jarPath);
            return jarFile.getParent();
        } catch (Exception e) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    private void extractZip(Path zipFile, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDir.resolve(entry.getName());
                if (!entryPath.normalize().startsWith(destDir.normalize())) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[8192];
                        int length;
                        while ((length = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Create OBS configuration to prevent popups and enable WebSocket
     */
    private void createOBSConfiguration() {
        try {
            // Create config directory
            Path configDir = obsInstallDir.resolve("config").resolve("obs-studio");
            Files.createDirectories(configDir);

            // Create global.ini to prevent safe mode popup and enable WebSocket
            Path globalIni = configDir.resolve("global.ini");
            String globalConfig = """
                [General]
                EnableAutoUpdates=false
                WarnBeforeStartingStream=false
                WarnBeforeStoppingStream=false
                WarnBeforeStoppingRecord=false
                RecordWhenStreaming=false
                KeepRecordingWhenStreamStops=false
                SysTrayEnabled=false
                SysTrayWhenStarted=false
                SaveProjectors=false
                ShowTransitions=true
                ShowListboxToolbars=true
                ShowStatusBar=true
                ShowSourceIcons=true
                ShowContextToolbars=true
                EnableOutputTimer=true
                ShowMissingFilesDialog=false
                DisableShutdownCheck=true
                RecordingRemuxAfterStop=false
                SystemTrayEnabled=false
                CenterSnapping=false
                SourceSnapping=false
                SnapSensitivity=10.0
                ScreenSnapping=false
                SnapDistance=10.0
                HideProjectorCursor=true
                ProjectorAlwaysOnTop=false
                SaveProjectors=false
                SysTrayMinimizeToTray=false

                [BasicWindow]
                geometry=AdnQywADAAAAAAD4AAAAJwAABgcAAAOqAAAA+AAAACcAAAYHAAADqgAAAAAAAAAABgAAAAD4AAAAJwAABgcAAAOq
                state=AAAA/wAAAAD9AAAAAgAAAAAAAAC7AAAC9fAAAAD8AgAAAAEAAAAUAFMAbwB1AHIAYwBlAHMARABvAGMAawEAAAAmAAAC9QAAAAgBAAADAAAAAQAAAAEAAAF5AAAC9fAAAAD8AgAAAAIAAAAUAFMAYwBlAG4AZQBzAEQAbwBjAGsBAAAAAAAAALsAAABZAP///wAAABQATQBpAHgAZQByAEQAbwBjAGsBAAAAvgAAATkAAABZAP///wAAAAMAAAYAAAABZ/AAAADqAQAAAAEAAAACAAAAAQAAAPoA////AAAAAAEA///+AAAAAEAFAACgAAAAAFwEAABgAAAAAA==

                [Video]
                BaseCX=1920
                BaseCY=1080
                OutputCX=1920
                OutputCY=1080

                [Audio]
                SampleRate=44100
                ChannelSetup=Stereo

                [OBSWebSocket]
                ServerEnabled=true
                ServerPort=4455
                AuthRequired=false
                ServerPassword=
                AlertsEnabled=false
                """;
            Files.writeString(globalIni, globalConfig, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create plugin_config/obs-websocket.ini
            Path pluginConfigDir = configDir.resolve("plugin_config");
            Files.createDirectories(pluginConfigDir);
            Path obsWebSocketIni = pluginConfigDir.resolve("obs-websocket.ini");
            String webSocketSettings = """
                [OBSWebSocket]
                ServerEnabled=true
                ServerPort=4455
                AuthRequired=false
                ServerPassword=
                AlertsEnabled=false
                DebugEnabled=false
                """;
            Files.writeString(obsWebSocketIni, webSocketSettings, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create minimal scene collection
            Path scenesDir = configDir.resolve("basic").resolve("scenes");
            Files.createDirectories(scenesDir);
            String basicScenes = """
                {
                    "current_scene": "Scene",
                    "current_program_scene": "Scene", 
                    "scene_order": [
                        {
                            "name": "Scene"
                        }
                    ],
                    "name": "ChubbyRecorder",
                    "sources": [
                        {
                            "balance": 0.5,
                            "deinterlace_field_order": 0,
                            "deinterlace_mode": 0,
                            "enabled": true,
                            "flags": 0,
                            "hotkeys": {},
                            "id": "monitor_capture",
                            "mixers": 255,
                            "monitoring_type": 0,
                            "muted": false,
                            "name": "Display Capture",
                            "prev_ver": 469762048,
                            "private_settings": {},
                            "push-to-mute": false,
                            "push-to-mute-delay": 1000,
                            "push-to-talk": false,
                            "push-to-talk-delay": 1000,
                            "settings": {
                                "monitor": 0,
                                "capture_cursor": true
                            },
                            "sync": 0,
                            "versioned_id": "monitor_capture",
                            "volume": 1.0
                        }
                    ],
                    "scenes": [
                        {
                            "hotkeys": {},
                            "id": 0,
                            "name": "Scene",
                            "sources": [
                                {
                                    "name": "Display Capture"
                                }
                            ]
                        }
                    ]
                }
                """;
            Files.writeString(scenesDir.resolve("ChubbyRecorder.json"), basicScenes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Create minimal profiles.ini and scene_collections.ini
            Path basicDir = configDir.resolve("basic");
            Files.createDirectories(basicDir);
            Path profilesIni = basicDir.resolve("profiles.ini");
            String profileConfig = """
                [General]
                Name1=Untitled

                [Untitled]
                Name=Untitled
                """;
            Files.writeString(profilesIni, profileConfig, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Path sceneCollectionList = basicDir.resolve("scene_collections.ini");
            String sceneCollections = """
                [General]
                Name1=ChubbyRecorder

                [ChubbyRecorder]
                Name=ChubbyRecorder
                """;
            Files.writeString(sceneCollectionList, sceneCollections, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            System.out.println("OBS configuration created successfully");
        } catch (Exception e) {
            System.err.println("Failed to create OBS configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void gracefulShutdown() {
        if (obsProcess == null || !obsProcess.isAlive()) {
            return;
        }
        try {
            System.out.println("Initiating graceful OBS shutdown...");
            obsProcess.destroy();
            boolean shutdown = obsProcess.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!shutdown) {
                System.out.println("Graceful shutdown timed out, force killing...");
                obsProcess.destroyForcibly();
                obsProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            }
            System.out.println("OBS shutdown completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Shutdown interrupted");
        } catch (Exception e) {
            System.err.println("Error during graceful shutdown: " + e.getMessage());
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application shutting down, cleaning up OBS...");
            gracefulShutdown();
        }));
    }

    /**
     * Clean up any crash recovery files that might cause popups
     */
    private void cleanupCrashRecovery() {
        try {
            Path configDir = obsInstallDir.resolve("config").resolve("obs-studio");
            // Remove crash recovery files
            Files.deleteIfExists(configDir.resolve("crashes"));
            Files.deleteIfExists(configDir.resolve("profiler_data"));
            // Clear any lock files
            try (var stream = Files.walk(configDir, 2)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".lock"))
                      .forEach(path -> {
                          try {
                              Files.deleteIfExists(path);
                          } catch (IOException e) {
                              // Ignore
                          }
                      });
            } catch (IOException e) {
                // Directory might not exist yet
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup crash recovery: " + e.getMessage());
        }
    }

    /**
     * Kill any running OBS processes to prevent conflicts
     */
    private void killExistingOBS() {
        try {
            System.out.println("Checking for existing OBS processes...");
            // Kill any existing OBS processes
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", "obs64.exe");
            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            // Also try obs32.exe just in case
            pb = new ProcessBuilder("taskkill", "/F", "/IM", "obs32.exe");
            process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            // Wait a moment for processes to fully terminate
            Thread.sleep(1000);
            System.out.println("Existing OBS processes terminated");
        } catch (Exception e) {
            // Ignore errors - the processes might not exist
            System.out.println("No existing OBS processes found");
        }
    }

    /**
     * Get the size of OBS installation
     */
    public long getOBSInstallationSize() {
        try {
            if (!Files.exists(obsInstallDir)) {
                return 0;
            }
            return Files.walk(obsInstallDir)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Remove bundled OBS installation
     */
    public boolean removeBundledOBS() {
        try {
            stopBundledOBS();
            if (Files.exists(obsInstallDir)) {
                Files.walk(obsInstallDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path);
                        }
                    });
            }
            return !Files.exists(obsInstallDir);
        } catch (IOException e) {
            System.err.println("Failed to remove OBS installation: " + e.getMessage());
            return false;
        }
    }
}
