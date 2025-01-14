package com.example.mailserver;

import java.io.IOException;

import com.example.mailserver.controller.LogController;
import com.example.mailserver.model.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    Server server;

    /**
     *
     * method used to start the log view
     *
     * @param stage : stage of the view
     * @throws Exception : exception thrown "load" method
     */

    @Override
    public void start(Stage stage) throws Exception {


        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("log-view.fxml"));
        LogController controller = new LogController();
        fxmlLoader.setController(controller);

        try{
             server=new Server(8189, controller);
             Thread threadServer=new Thread(server);
             threadServer.setDaemon(true);
             threadServer.start();
        }catch(IOException e){
            e.printStackTrace();
        }

        Scene scene = new Scene(fxmlLoader.load(), 500,500 );
        stage.setTitle("Log Server");
        stage.setScene(scene);
        stage.show();
    }

    /**
     *
     * main method
     *
     * @param args : args from input
     */
    public static void main(String[] args) {
        launch();
    }
}
