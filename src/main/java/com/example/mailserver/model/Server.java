package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private ServerController serverController;
    public Server(int port, ServerController controller) throws IOException {
        this.serverController = controller;
        this.serverSocket = new ServerSocket(port);

    }



    @Override
    public void run() {
        System.out.println("finestra del socket server");
        try{
            int i=3;
            while(true){
                Socket incoming = serverSocket.accept();
                System.out.println("spawning"+i);
                Runnable r =new ServerHandler(incoming, serverController);
                new Thread(r).start();
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
