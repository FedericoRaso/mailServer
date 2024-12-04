package com.example.mailserver.model;

import com.example.mailserver.controller.ServerController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


        try {
            Protocol protocol = Protocol.valueOf(in.nextLine());
            System.out.println("protocollo " + protocol);
            switch (protocol) {
                case LOGIN -> {

                    String line = in.nextLine();

                    String answer = loginControls(line);
                    if (inbox != null) {
                        controller.addLog(line + " ha effettuato l'accesso");
                        out.println(inbox);
                    } else {
                        out.println(answer);
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
                case SEND, FORWARD, REPLYALL, REPLY ->{
                    String recevers = in.nextLine();
                    String controlReceivers = controlReceivers(recevers);
                    out.println(controlReceivers);
                    String newMail = in.nextLine();
                    sendMail(newMail);
                    controller.addLog("Email "+protocol+" "+newMail);
                }
                case REFRESH -> {
                    String userInfo= in.nextLine();
                    String oldInbox = in.nextLine();
                    String newInbox = "";

                    while(true){
                        newInbox = getInbox(userInfo);
                        if(!(oldInbox.equals(newInbox))){
                            break;
                        }
                        Thread.sleep(2000);
                    }
                    out.println(getInbox(userInfo));
                    controller.addLog(userInfo+"ha effettuato un refresh");
                }
                case LOGOUT -> {
                    String user = in.nextLine();
                    controller.addLog(user + " ha effettuato il logout");
                }
            }

        } catch (InterruptedException e) {
            System.out.println("Errore nella gestione del client: "+e.getMessage());
        } finally {
            try {
                getClose();
            } catch (IOException e) {
                System.out.println("Errore nella gestione della chiusura dell'clienthandler: "+e.getMessage());
            }
        }
    }

    public void getClose() throws IOException {
        in.close();
        out.close();
        incoming.close();
    }



    public String getInbox(String user){
        readLock.lock();
        JSONArray newInbox = null;
        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(new FileReader(path));
            JSONArray jsonArray = (JSONArray) obj;

            for (Object o : jsonArray) {
                JSONObject person = (JSONObject) o;
                String nome = (String) person.get("email");

                if(nome.equals(user)) {

                    newInbox = (JSONArray) person.get("inbox");

                    break;
                }
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        } finally {
            readLock.unlock();
        }
        return newInbox.toJSONString() ;
    }


    public String loginControls(String line) {
        readLock.lock();
        JSONParser parser = new JSONParser();
        boolean isFound=false;

        if(!isValidEmail(line)) {
            return "nome@gmail.com";
        }else{
            try {
                Object obj = parser.parse(new FileReader(path));
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
            } finally {
                readLock.unlock();
            }

            return (isFound) ? "correct":"wrong";
        }
    }

    public void sendMail (String newMail){
        writeLock.lock();
        JSONParser parser = new JSONParser();
        try{
            Object obj = parser.parse(new FileReader(path));
            JSONArray users = (JSONArray) obj;
            obj = parser.parse(newMail);
            JSONObject mailToAdd = (JSONObject) obj;
            JSONArray inbox = (JSONArray) mailToAdd.get("inbox");
            JSONArray receivers = (JSONArray) mailToAdd.get("receivers");

            for(int i=0; i<users.size(); i++){
                JSONObject person = (JSONObject) users.get(i);
                String userEmail = (String) person.get("email");
                for(Object o : receivers){
                    String toAddEmail = (String) o;
                    if(userEmail.equals(toAddEmail)){
                        JSONArray userInbox = (JSONArray) person.get("inbox");
                        userInbox.add(mailToAdd);
                    }
                }

            }

            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(users.toJSONString());
            fileWriter.close();
        }catch (ParseException | IOException e){
            e.printStackTrace();
        }finally{
            writeLock.unlock();
        }

    }

    public void deleteMail(String user, String idMail){
        writeLock.lock();
        String userDaCercare = user;
        String idDaEliminare = idMail;

        try {

            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(path));
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
            FileWriter writer = new FileWriter(path);
            writer.write(users.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            writeLock.unlock();
        }

    }

    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_REGEX);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public String controlReceivers(String receivers){
        readLock.lock();
        for (String email : receivers.split(", ")) {

            if(!isValidEmail(email)){
                return email;
            }else{
                try{
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(new FileReader(path));
                    JSONArray jsonArray = (JSONArray) obj;

                    boolean isFound=false;

                    for (Object o : jsonArray) {
                        JSONObject person = (JSONObject) o;
                        String nome = (String) person.get("email");

                        if(nome.equals(email)) {
                            isFound=true;
                            break;
                        }
                    }

                    if(!isFound){
                        return email;
                    }
                }catch(Exception e){
                    e.printStackTrace();
                } finally {
                    readLock.unlock();
                }
            }
        }

        return "OK";
    }

}
