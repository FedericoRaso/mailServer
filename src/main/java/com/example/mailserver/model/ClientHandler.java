package com.example.mailserver.model;

import com.example.mailserver.controller.LogController;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.*;


public class ClientHandler implements Runnable {
    private Socket incoming;
    private LogController controller;
    private JSONArray inbox =null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private Scanner in = null;
    private PrintWriter out = null;
    private ReadWriteLock readWriteLockJSON = new ReentrantReadWriteLock();
    private Lock readLockJSON = readWriteLockJSON.readLock();
    private Lock writeLockJSON = readWriteLockJSON.writeLock();
    private ReadWriteLock readWriteLockNewMails = new ReentrantReadWriteLock();
    private Lock readLockNewMails = readWriteLockNewMails.readLock();
    private Lock writeLockNewMails = readWriteLockNewMails.writeLock();
    private final String path = "src/main/resources/data/User.json";
    private static List<String[]> newEmails = new ArrayList<>();

    /**
     *
     * constructor of the class
     *
     * @param incoming : socket of the server to connect with
     * @param logController : controller of the view
     * @throws IOException : exception thrown from "getInputStream" and "getOutStream" methods
     */
    public ClientHandler(Socket incoming, LogController logController) throws IOException {
        this.incoming = incoming;
        this.controller = logController;
        inStream = incoming.getInputStream();
        outStream = incoming.getOutputStream();
        in  = new Scanner(inStream);
        out = new PrintWriter(outStream, true);
    }

    /**
     *
     * method that runs when a thread is started
     *
     */
    @Override
    public void run() {

        try {
            Protocol protocol = Protocol.valueOf(in.nextLine());
            System.out.println("protocol " + protocol);
            switch (protocol) {
                case LOGIN -> {

                    String line = in.nextLine();

                    String answer = loginChecks(line);
                    if (inbox != null) {
                        controller.addLog(line + " logged in");
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
                    String receivers = in.nextLine();
                    String controlReceivers = receiversChecks(receivers);
                    out.println(controlReceivers);
                    String newMail = in.nextLine();
                    sendMail(newMail);
                    controller.addLog("Email "+protocol+" "+newMail);
                }
                case REFRESH -> {

                    String userInfo = in.nextLine();
                    String answer = getRefresh(userInfo);

                    if(!answer.equals("no changes")){
                        controller.addLog(userInfo + " refreshed");
                    }

                    out.println(answer);
                }
                case LOGOUT -> {
                    String user = in.nextLine();
                    controller.addLog(user + " logged out");
                }
            }

        } catch (Exception e) {
            System.out.println("Error in client handling: "+e.getMessage());
        } finally {
            try {
                closeConnection();
            } catch (IOException e) {
                System.out.println("Error closing the client handler: "+e.getMessage());
            }
        }
    }

    public String getRefresh(String user){
        readLockNewMails.lock();
        if(newEmails == null || newEmails.isEmpty()){
            readLockNewMails.unlock();
            return "no changes";
        }else {

            for (String[] newMail : newEmails) {

                if (newMail[0].equals(user)) {
                    System.out.println(newMail[1]);
                    String answer = newMail[1];
                    readLockNewMails.unlock();
                    writeLockNewMails.lock();
                    System.out.println(newEmails.size());
                    newEmails.remove(newMail);
                    writeLockNewMails.unlock();
                    return answer;
                }
            }
            readLockNewMails.unlock();
            return "no changes";
        }
    }


    /**
     *
     * method used to close the connection with the server
     *
     * @throws IOException : exception thrown from "close" method
     */
    public void closeConnection() throws IOException {
        in.close();
        out.close();
        incoming.close();
    }


    /**
     *
     * method used to get the inbox of a user
     *
     * @param user : user whose inbox is requested
     * @return : string representation
     */
    public JSONArray getInbox(String user){
        readLockJSON.lock();
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
            readLockJSON.unlock();
        }
        return newInbox ;
    }

    /**
     * 
     * method used to check if the login parameters are right
     * 
     * @param line : mail of the user
     * @return : result of the checks
     */
    public String loginChecks(String line) {

            inbox = getInbox(line);

            return (inbox==null) ? "wrong":"correct";
    }

    /**
     *
     * method used to send a mail to another user
     *
     * @param newMail : mail to be sent
     */
    public void sendMail (String newMail){
        writeLockJSON.lock();
        JSONParser parser = new JSONParser();
        try{
            Object obj = parser.parse(new FileReader(path));
            JSONArray users = (JSONArray) obj;
            obj = parser.parse(newMail);
            JSONObject mailToAdd = (JSONObject) obj;
            JSONArray receivers = (JSONArray) mailToAdd.get("receivers");

            for(int i=0; i<users.size(); i++){
                JSONObject person = (JSONObject) users.get(i);
                String userEmail = (String) person.get("email");
                for(Object o : receivers){
                    String toAddEmail = (String) o;
                    if(userEmail.equals(toAddEmail)){
                        JSONArray userInbox = (JSONArray) person.get("inbox");
                        userInbox.add(mailToAdd);
                        writeLockNewMails.lock();
                        newEmails.add(new String[]{userEmail, mailToAdd.toString()});
                        writeLockNewMails.unlock();
                    }
                }

            }

            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(users.toJSONString());
            fileWriter.close();
        }catch (ParseException | IOException e){
            e.printStackTrace();
        }finally{
            writeLockJSON.unlock();
        }

    }

    /**
     *
     * method used to delete a mail from the inbox
     *
     * @param user : user that has the mail to be deleted
     * @param idMail : id of the mail to be deleted
     */
    public void deleteMail(String user, String idMail){
        writeLockJSON.lock();
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
                            controller.addLog(user + " deleted a mail");
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
            writeLockJSON.unlock();
        }

    }



    /**
     *
     * method used to check if the receivers of a mail exist
     *
     * @param receivers : string with the receivers of the mail
     * @return : result of the checks
     */
    public String receiversChecks(String receivers){
        readLockJSON.lock();
        try {
            for (String email : receivers.split(", ")) {
                try {
                    JSONParser parser = new JSONParser();
                    Object obj = parser.parse(new FileReader(path));
                    JSONArray jsonArray = (JSONArray) obj;

                    boolean isFound = false;

                    for (Object o : jsonArray) {
                        JSONObject person = (JSONObject) o;
                        String nome = (String) person.get("email");

                        if (nome.equals(email)) {
                            isFound = true;
                            break;
                        }
                    }

                    if (!isFound) {
                        return email;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        }finally {
            readLockJSON.unlock();
        }
        return "OK";
    }

}
