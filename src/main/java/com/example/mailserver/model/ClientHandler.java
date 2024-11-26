package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientHandler implements Runnable {
    private Socket incoming;
    private ServerController controller;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public ClientHandler(Socket incoming, ServerController serverController) {
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


                String line = in.nextLine(); //textfield login

                String answer = loginControls(line);
                out.println(answer);

            } finally {
            incoming.close();
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
    }


    public String loginControls(String line) {
        JSONParser parser = new JSONParser();
        boolean isFound=false;
        String answer;

        if(!isValidEmail(line)) {
            return "nome@gmail.com";
        }else{
            try {
                Object obj = parser.parse(new FileReader("src/main/resources/data/User.json"));

                JSONArray jsonArray = (JSONArray) obj;

                for (Object o : jsonArray) {
                    JSONObject person = (JSONObject) o;
                    String nome = (String) person.get("email");

                    if(nome.equals(line)) {
                        isFound=true;
                        break;
                    }
                }

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

            return (isFound) ? "correct":"wrong";
        }



    }

    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}
