package com.java.client.server;

import java.net.*;
import java.io.*;
import java.util.*;

public class Client{
    private static final String COMMAND_NICK = "NICK ";
    private static final String COMMAND_USER = "USER ";
    private static final String COMMAND_LOGIN = "PRIVMSG NickServ :IDENTIFY";
    private static final String COMMAND_REGISTER = "PRIVMSG NickServ :REGISTER";

    private static final String POSTFIX = " test1 test2 : test3 test4";

    private static final String MSG_DISCONNECTED = "Client disconnected.";
    private static final String MSG_USAGE = "USAGE: java Client <host> <port>";
    private static final String MSG_CONNECTED = "Connected to the chat server! (Input HELP for more information)";
    private static final String MSG_ENTER_NAME = "Enter your username: ";
    private static final String MSG_ENTER_PASS = "Enter your password: ";
    private static final String MSG_ENTER_EMAIL = "Enter your email: ";
    private static final String MSG_LOGIN_OR_REGISTER = "Login or register? (Input 1 / 2) : ";

    private static final String ERR_BAD_PORT = "Error #1: bad port number";
    private static final String ERR_SERVER_NOT_FOUND = "Error #2: Server not found.";
    private static final String ERR_IO = "Error #3: I/O Error.";
    private static final String ERR_INCORRECT_INPUT = "Error #4: Incorrect input!";
    private static final String ERR_FAILED_CLOSE = "Error #5: Failed to close the socket properly.";
    private static final String ERR_UNEXPECTED_EXCEPTION = "Error #9999: Unexpected exception: ";

    private final String host;
    private final int port;

    private Socket socket;

    private PrintWriter writer;
    private BufferedReader reader;

    private ReaderThread rt;
    private WriterThread wt;

    public Client(String host, int port){
        this.host = host;
        this.port = port;
    }

    public void start(){
        try {
            // Creating a new socket and informing the client that the connection was successful
            socket = new Socket(host, port);
            System.out.println(MSG_CONNECTED);

            // Initializing reader and writer
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            Console console = System.console();
            int loginOrRegisterNumber = 0;

            // Asking the user to login or register
            while(true) {
                String loginOrRegisterStr = console.readLine(MSG_LOGIN_OR_REGISTER);

                try {
                    loginOrRegisterNumber = Integer.parseInt(loginOrRegisterStr);
                }
                catch (NumberFormatException e) {
                    System.err.println(ERR_INCORRECT_INPUT);
                    continue;
                }

                if(loginOrRegisterNumber < 1 || loginOrRegisterNumber > 2){
                    System.err.println(ERR_INCORRECT_INPUT);
                    continue;
                }

                break;
            }

            String username = console.readLine(MSG_ENTER_NAME);
            char[] password = console.readPassword(MSG_ENTER_PASS);

            send(COMMAND_USER + username + POSTFIX);
            send(COMMAND_NICK + username);

            // Login
            if(loginOrRegisterNumber == 1)
                send(COMMAND_LOGIN + " " + username + " " + new String(password));

            // Register (email required)
            else{
                String email = console.readLine(MSG_ENTER_EMAIL);
                send(COMMAND_REGISTER + " " + new String(password) + " " + email);
            }

            // Starting the reader and writer threads for the current user
            wt = new WriterThread(socket, this);
            rt = new ReaderThread(socket, this, wt, host);

            wt.start();
            rt.start();
        }
        catch(UnknownHostException ex){
            System.err.println(ERR_SERVER_NOT_FOUND);
            cleanUp();
        }
        catch(IOException ex){
            System.err.println(ERR_IO);
            cleanUp();
        }
    }

    public void cleanUp(){
        try{
            if(reader != null)
                reader.close();

            if(writer != null)
                writer.close();

            if(socket != null)
                socket.close();
        }
        catch(IOException ex){
            System.out.println('\r' + ERR_FAILED_CLOSE);
        }

        System.out.println('\r' + MSG_DISCONNECTED);

        // Kill all threads
        System.exit(1);
    }

    public void send(String message){
        writer.println(message + "\r\n");
    }

    public static void main(String args[]){
        final int port;

        // Error if the argument count is not equal to 1
        if(args.length != 2){
            System.err.println(MSG_USAGE);
            return;
        }

        // Trying to parse the port number from the user input to numerical form
        try{
            port = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException e){
            System.err.println(ERR_BAD_PORT);
            return;
        }

        try {
            Client client = new Client(args[0], port);
            client.start();
        }
        catch(Exception e){
            System.err.println('\r' + ERR_UNEXPECTED_EXCEPTION);
        }
    }
}