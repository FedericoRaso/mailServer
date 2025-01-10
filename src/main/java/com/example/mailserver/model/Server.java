package com.example.mailserver.model;

import com.example.mailserver.controller.LogController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private LogController logController;
    ExecutorService pool;

    /**
     *
     * constructor of "Server" class
     *
     * @param port : port of the server
     * @param controller : controller of the log view
     * @throws IOException : exception thrown from ServerSocket
     */
    public Server(int port, LogController controller) throws IOException {
        this.logController = controller;
        this.serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(10);
    }

    /**
     *
     * method run when a thread is created
     *
     */
    @Override
    public void run() {
        System.out.println("Socket server window listening ...");
        try{

            while(!serverSocket.isClosed()){
                Socket incoming = serverSocket.accept();
                pool.execute(new ClientHandler(incoming, logController));
            }
        } catch (IOException e) {
            if(serverSocket.isClosed()){
                logController.addLog("Server stopped receiving");
            }else{
                System.out.println("Error in socket server: " +e.getMessage());
            }
        }finally {
            stop();
        }
    }

    /**
     *
     * method used to stop the server
     *
     */
    public void stop() {
        try{
            System.out.println("Stopping server...");
            if(serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }
            pool.shutdown();
            System.out.println("Server correctly stopped");
        }catch(IOException e){
            System.out.println("Error stopping the server");
        }
    }
}
