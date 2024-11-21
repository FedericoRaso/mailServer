package com.example.mailserver.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerController {
    @FXML
    private TextArea logTextArea;
    @FXML
    public void initialize(){
        addLog("server avviato correttamente");
    }

    @FXML
    public void addLog(String text){
        logTextArea.appendText(text + "\n");
    }

}
