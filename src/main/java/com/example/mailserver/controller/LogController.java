package com.example.mailserver.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class LogController {
    @FXML
    private TextArea logTextArea;

    /**
     *
     * method used to initialize the view
     *
     */
    @FXML
    public void initialize(){
        addLog("server started and listening ...");
    }

    /**
     *
     * method used to print a log on the view
     *
     * @param text : text to print
     */
    @FXML
    public void addLog(String text){
        logTextArea.appendText(text + "\n");
    }

}
