package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private ServerController serverController;
    ExecutorService pool;


    public Server(int port, ServerController controller) throws IOException {
        this.serverController = controller;
        this.serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(10);
    }


    @Override
    public void run() {
        System.out.println("finestra del socket server");
        try{

            while(true){
                Socket incoming = serverSocket.accept();
                pool.execute(new ClientHandler(incoming, serverController));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
