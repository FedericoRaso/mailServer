module com.example.mailserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;
    requires java.desktop;


    opens com.example.mailserver to javafx.fxml;
    exports com.example.mailserver;
    opens com.example.mailserver.controller to javafx.fxml;

}