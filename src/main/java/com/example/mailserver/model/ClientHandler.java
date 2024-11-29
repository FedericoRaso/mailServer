package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.print.attribute.standard.JobKOctets;
import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;

public class ClientHandler implements Runnable {
    private Socket incoming;
    private ServerController controller;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private JSONArray inbox =null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private Scanner in = null;
    private PrintWriter out = null;
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private Lock readLock = readWriteLock.readLock();
    private Lock writeLock = readWriteLock.writeLock();
    private final String path = "src/main/resources/data/User.json";


    public ClientHandler(Socket incoming, ServerController serverController) throws IOException {
        this.incoming = incoming;
        this.controller = serverController;
        inStream = incoming.getInputStream();
        outStream = incoming.getOutputStream();
        in  = new Scanner(inStream);
        out = new PrintWriter(outStream, true);
    }

    @Override
    public void run() {
        System.out.println("sono in run server handler");

        try {
            Protocol protocol = Protocol.valueOf(in.nextLine());
            System.out.println("protocollo" + protocol);
            switch (protocol) {
                case LOGIN -> {

                    String line = in.nextLine();  //legge da client il login

                    String answer = loginControls(line);    //controlla se il login esiste
                    if (inbox != null) {
                        controller.addLog("mando l'inbox di " + line + " al client");
                        out.println(inbox);     //invia l'inbox al client
                    } else {
                        out.println(answer); //se userMails non è pieno significa che lo user non esiste
                    }
                }
                case DELETE -> {
                    String idMail = in.nextLine();
                    String user = in.nextLine();
                    try {
                        deleteMail(user, idMail);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        }finally {
            try {
                incoming.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
                int i=0;

                for (Object o : jsonArray) {
                    JSONObject person = (JSONObject) o;
                    String nome = (String) person.get("email");

                    if(nome.equals(line)) {
                        if(inbox == null) {
                            inbox = new JSONArray();
                        }

                        inbox = (JSONArray) person.get("inbox");
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

    public void deleteMail(String user, String idMail){

        String userDaCercare = user;
        String idDaEliminare = idMail;

        try {

            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader("src/main/resources/data/User.json"));
            JSONArray users = (JSONArray) obj;

            for (int i = 0; i < users.size(); i++) {
                JSONObject utente = (JSONObject) users.get(i);
                String emailUtente = (String)utente.get("email");

                if (emailUtente.equals(userDaCercare)) {
                    JSONArray inbox = (JSONArray) utente.get("inbox");

                    for (int j = 0; j < inbox.size(); j++) {
                        JSONObject email = (JSONObject) inbox.get(j);
                        if (email.get("id").equals(idDaEliminare)) {
                            inbox.remove(j);
                            controller.addLog(user + " ha eliminato una mail");
                            break;
                        }
                    }
                    break;
                }
            }

            FileWriter writer = new FileWriter("src/main/resources/data/User.json");
            writer.write(users.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

}
