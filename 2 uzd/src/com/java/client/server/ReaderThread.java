package com.java.client.server;

import java.io.*;
import java.net.*;

public class ReaderThread extends Thread{
    private static final String ERR_INPUT = "Error #1: Error getting the input stream.";
    private static final String ERR_READING = "Error #2: Error reading from the server.";

    private static final String CMD_INVALID_EMAIL = "is not a valid e-mail address.";
    private static final String CMD_INVALID_PASSWORD = "Invalid password for";

    private static final String INDICATOR_ERROR = "ERROR";
    private static final String INDICATOR_JOIN = "JOIN";
    private static final String INDICATOR_PING = "PING";
    private static final String INDICATOR_PONG = "PONG";
    private static final String INDICATOR_PRIVATE_MESSAGE = "PRIVMSG";
    private static final String INDICATOR_NOTICE = "NOTICE";

    private static final String INDICATOR_NUMERIC_USERNAME_ERROR = "433";
    private static final String INDICATOR_NUMERIC_LIST = "322";

    private BufferedReader reader;
    private PrintWriter writer;

    private WriterThread wr;

    private final Socket socket;
    private final Client client;

    private final String host;

    public ReaderThread(Socket socket, Client client, WriterThread wr, String host) {
        this.socket = socket;
        this.client = client;
        this.wr = wr;
        this.host = host;

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        }
        catch(IOException ex){
            System.err.println(ERR_INPUT);
            client.cleanUp();
        }
    }

    public void run() {
        String message = " ";

        while (!socket.isClosed()) {
            try {
                message = reader.readLine();
                if(message != null){
                    String[] splitMessage;
                    // Splitting the message by a colon
                    splitMessage = message.split(":");

                    // Split message by spaces
                    String[] splitSpaceMessage = message.split("\\s+");

                    // Specific formatting for libera chat
                    boolean isFirstColon = message.charAt(0) == ':';

                    if(isFirstColon) {
                        String firstPart = splitMessage[1];

                        // If it is a private message or a notice - print it in format, otherwise print the entire message
                        if (firstPart.contains("!") && (firstPart.contains(INDICATOR_PRIVATE_MESSAGE) || firstPart.contains(INDICATOR_NOTICE)))
                            message = firstPart.substring(0, firstPart.lastIndexOf("!")) + ": " + message.substring(message.indexOf(splitMessage[2]));

                            // If it is from the LIST command - apply the correct format
                        else if (splitSpaceMessage.length > 1 && splitSpaceMessage[1].equals(INDICATOR_NUMERIC_LIST))
                            message = message.substring(message.indexOf("#"));

                            // If it is a basic message - apply the formatting
                        else if (splitMessage.length > 2)
                            message = message.substring(message.indexOf(splitMessage[2]));
                    }

                    System.out.println("\r" + message);
                    System.out.print(wr.getBuilder().toString());

                    // PING / PONG
                    int num = isFirstColon ? 1 : 0;
                    if(splitMessage[num].startsWith(INDICATOR_PING)) {
                        System.out.println("\r" + INDICATOR_PONG + " :" + splitMessage[num + 1]);
                        writer.println(INDICATOR_PONG + " :" + splitMessage[num + 1]);
                        continue;
                    }

                    // Setting the channel user just joined
                    if(splitSpaceMessage.length > 2 && splitSpaceMessage[1].contains(INDICATOR_JOIN))
                        wr.setCurrentChannel(splitSpaceMessage[2]);

                    // An error occurred
                    if (splitSpaceMessage[0].equals(INDICATOR_ERROR))
                        break;

                    if(isFirstColon) {
                        // Something wrong with the login/registration
                        if (splitSpaceMessage.length > 1 && splitSpaceMessage[1].equals(INDICATOR_NUMERIC_USERNAME_ERROR))
                            break;

                        // Password is incorrect
                        if (splitMessage.length > 2 && splitMessage[2].startsWith(CMD_INVALID_PASSWORD))
                            break;

                        // Email is incorrect
                        if (splitMessage.length > 2 && splitMessage[2].endsWith(CMD_INVALID_EMAIL))
                            break;
                    }
                }
            }
            catch (IOException ex) {
                if(!socket.isClosed())
                    System.err.println(ERR_READING);

                break;
            }
        }

        try{
            if(reader != null)
                reader.close();
        }
        catch(IOException ex){

        }

        //client.cleanUp();
    }
}
