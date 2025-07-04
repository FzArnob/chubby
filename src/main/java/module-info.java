module screenrecorder {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    exports com.screenrecorder;
    exports com.screenrecorder.model;
    exports com.screenrecorder.service;
    
    // Opens packages to JavaFX FXML for reflection access
    opens com.screenrecorder to javafx.fxml;
    opens com.screenrecorder.model to javafx.fxml;
}
