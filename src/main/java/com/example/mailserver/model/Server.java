package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
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
        System.out.println("finestra del socket server in ascolto ...");
        try{

            while(!serverSocket.isClosed()){
                Socket incoming = serverSocket.accept();
                pool.execute(new ClientHandler(incoming, serverController));
            }
        } catch (IOException e) {
            if(serverSocket.isClosed()){
                serverController.addLog("Il server ha smesso di ricevere");
            }else{
                System.out.println("L'errore nel socket server: " +e.getMessage());
            }
        }finally {
            stop();
        }
    }

    public void stop() {
        try{
            System.out.println("Arresto del server...");
            if(serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }
            pool.shutdown();
            System.out.println("Server arrestato correttamente");
        }catch(IOException e){
            System.out.println("Errore durante l'arresto del server");
        }
    }
}
