package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServerHandler implements Runnable {
    private Socket incoming;
    private ServerController controller;
    public ServerHandler(Socket incoming, ServerController serverController) {
        this.incoming = incoming;
        this.controller = serverController;
    }
    @Override
    public void run() {
        System.out.println("sono in run server handler");

        try{
            try{

                InputStream incomingStream = incoming.getInputStream();
                OutputStream outgoingStream = incoming.getOutputStream();
                Scanner in  = new Scanner(incomingStream);
                PrintWriter out = new PrintWriter(outgoingStream, true);


                String line = in.nextLine();
                out.println(line);

                System.out.println(line);
                controller.addLog(line);

            } finally {
            incoming.close();
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
