package com.screenrecorder;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.screenrecorder.model.RecordingConfig;
import com.screenrecorder.model.RecordingSource;
import com.screenrecorder.model.Resolution;
import com.screenrecorder.service.FFmpegService;
import com.screenrecorder.service.OBSRecordingService;
import com.screenrecorder.service.SystemDiscoveryService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;

/**
 * Main controller for the Screen Recorder application
 */
public class ScreenRecorderController implements Initializable {
    
    // FXML Controls
    @FXML private ComboBox<RecordingSource> videoSourceComboBox;
    @FXML private ComboBox<RecordingSource> audioSourceComboBox;
    @FXML private ComboBox<Resolution> resolutionComboBox;
    @FXML private TextField outputDirectoryField;
    @FXML private Button browseDirectoryButton;
    @FXML private CheckBox systemAudioCheckBox;
    @FXML private CheckBox microphoneCheckBox;
    @FXML private CheckBox separateAudioCheckBox;
    @FXML private Button recordButton;
    @FXML private Button pauseButton;
    @FXML private Button stopButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar timeProgressBar;
    @FXML private VBox previewContainer;
    @FXML private MediaView previewMediaView;
    
    // Services
    private final FFmpegService ffmpegService;
    private final SystemDiscoveryService discoveryService;
    private final RecordingConfig recordingConfig;
    
    // Preview
    private MediaPlayer previewPlayer;
    
    public ScreenRecorderController() {
        this.ffmpegService = new FFmpegService();
        this.discoveryService = new SystemDiscoveryService();
        this.recordingConfig = new RecordingConfig();
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupControls();
        setupBindings();
        loadInitialData();
        checkOBSAvailability();
    }
    
    /**
     * Setup initial control states and values
     */
    private void setupControls() {
        // Initialize resolution combo box
        resolutionComboBox.setItems(FXCollections.observableArrayList(
            Resolution.HD_1080P,
            Resolution.QHD_2K,
            Resolution.UHD_4K
        ));
        resolutionComboBox.setValue(Resolution.HD_1080P);
        
        // Set default output directory
        outputDirectoryField.setText(recordingConfig.getOutputDirectory().getAbsolutePath());
        
        // Set default audio options
        systemAudioCheckBox.setSelected(true);
        microphoneCheckBox.setSelected(false);
        separateAudioCheckBox.setSelected(false);
        
        // Initially disable recording controls
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        
        // Setup preview
        setupPreview();
    }
    
    /**
     * Setup property bindings
     */
    private void setupBindings() {
        recordButton.disableProperty().bind(ffmpegService.recordingProperty());
        pauseButton.disableProperty().bind(ffmpegService.recordingProperty().not());
        stopButton.disableProperty().bind(ffmpegService.recordingProperty().not());
        
        statusLabel.textProperty().bind(ffmpegService.statusProperty());
        
        // Update record button text based on pause state
        ffmpegService.pausedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                recordButton.setText("Resume");
                recordButton.setDisable(false);
            } else {
                recordButton.setText("Record");
            }
        });
        
        // Bind combo box selections to config
        videoSourceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setVideoSource(newVal);
        });
        
        audioSourceComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setAudioSource(newVal);
        });
        
        resolutionComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setResolution(newVal);
        });
        
        // Bind checkbox states to config
        systemAudioCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setRecordSystemAudio(newVal);
        });
        
        microphoneCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setRecordMicrophone(newVal);
        });
        
        separateAudioCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setSeparateAudioOutput(newVal);
        });
        
        // Bind output directory
        outputDirectoryField.textProperty().addListener((obs, oldVal, newVal) -> {
            recordingConfig.setOutputDirectory(new File(newVal));
        });
    }
    
    /**
     * Load initial data (video and audio sources)
     */
    private void loadInitialData() {
        // Load video sources
        Task<List<RecordingSource>> videoSourceTask = discoveryService.createVideoSourceDiscoveryTask();
        videoSourceTask.setOnSucceeded(e -> {
            List<RecordingSource> videoSources = videoSourceTask.getValue();
            Platform.runLater(() -> {
                videoSourceComboBox.setItems(FXCollections.observableArrayList(videoSources));
                if (!videoSources.isEmpty()) {
                    videoSourceComboBox.setValue(videoSources.get(0));
                }
            });
        });
        
        // Load audio sources
        Task<List<RecordingSource>> audioSourceTask = discoveryService.createAudioSourceDiscoveryTask();
        audioSourceTask.setOnSucceeded(e -> {
            List<RecordingSource> audioSources = audioSourceTask.getValue();
            Platform.runLater(() -> {
                audioSourceComboBox.setItems(FXCollections.observableArrayList(audioSources));
                if (!audioSources.isEmpty()) {
                    audioSourceComboBox.setValue(audioSources.get(0));
                }
            });
        });
        
        // Run tasks in background
        Thread videoThread = new Thread(videoSourceTask);
        Thread audioThread = new Thread(audioSourceTask);
        videoThread.setDaemon(true);
        audioThread.setDaemon(true);
        videoThread.start();
        audioThread.start();
    }
    
    /**
     * Check if OBS Studio is available
     */
    private void checkOBSAvailability() {
        ffmpegService.isFFmpegAvailable().thenAccept(available -> {
            Platform.runLater(() -> {
                if (!available) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("FFmpeg Not Found");
                    alert.setHeaderText("FFmpeg is required for recording");
                    alert.setContentText("Please install FFmpeg and add to path variables.");
                    alert.showAndWait();
                    
                    recordButton.setDisable(true);
                }
            });
        });
    }
    
    /**
     * Setup preview functionality
     */
    private void setupPreview() {
        // For now, just add a placeholder
        Label previewLabel = new Label("Live preview will appear here during recording");
        previewLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
        previewContainer.getChildren().add(previewLabel);
    }
    
    // Event Handlers
    
    @FXML
    private void onRecordClicked() {
        if (ffmpegService.pausedProperty().get()) {
            // Resume recording
            ffmpegService.togglePause();
        } else {
            // Start new recording
            startOBSRecording();
        }
    }
    
    @FXML
    private void onPauseClicked() {
        ffmpegService.togglePause();
    }
    
    @FXML
    private void onStopClicked() {
        ffmpegService.stopRecording();
    }
    
    @FXML
    private void onBrowseDirectoryClicked() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");
        chooser.setInitialDirectory(recordingConfig.getOutputDirectory());
        
        File selectedDirectory = chooser.showDialog(browseDirectoryButton.getScene().getWindow());
        if (selectedDirectory != null) {
            outputDirectoryField.setText(selectedDirectory.getAbsolutePath());
            recordingConfig.setOutputDirectory(selectedDirectory);
        }
    }
    
    @FXML
    private void onRefreshSourcesClicked() {
        loadInitialData();
    }
    
    /**
     * Start OBS recording with current configuration
     */
    private void startOBSRecording() {
        // First check if OBS is available
        ffmpegService.isFFmpegAvailable().thenAccept(available -> {
            if (!available) {
                Platform.runLater(() -> {
                    showError("ffmpeg is not running. Please start OBS Studio first.");
                });
                return;
            }
            
            // Validate configuration
            if (!recordingConfig.getOutputDirectory().exists()) {
                recordingConfig.getOutputDirectory().mkdirs();
            }
            
            // Start OBS recording
            ffmpegService.startRecording(recordingConfig).thenAccept(success -> {
                if (!success) {
                    Platform.runLater(() -> {
                        showError("Failed to start OBS recording. Please check OBS settings and try again.");
                    });
                }
            });
        });
    }
    
    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Recording Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shutdown services when application closes
     */
    public void shutdown() {
        if (previewPlayer != null) {
            previewPlayer.dispose();
        }
        ffmpegService.shutdown();
        discoveryService.shutdown();
    }
}
