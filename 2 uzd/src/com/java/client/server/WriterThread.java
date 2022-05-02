package com.java.client.server;

import java.io.*;
import java.net.*;

public class WriterThread extends Thread{
    private static final String ERR_OUTPUT = "Error #1: Error getting the output stream.";
    private static final String ERR_READING = "Error #2: Error reading from the server.";

    private static final String INDICATOR_QUIT = "/QUIT";

    private BufferedReader reader;
    private PrintWriter writer;
    private StringBuilder builder;

    private final Socket socket;
    private final Client client;

    private String currentChannel = "";

    public WriterThread(Socket socket, Client client){
        this.socket = socket;
        this.client = client;

        try{
            reader = new BufferedReader(new InputStreamReader(System.in));
            writer = new PrintWriter(socket.getOutputStream(), true);
        }
        catch(IOException ex){
            System.err.println(ERR_OUTPUT);
            client.cleanUp();
        }
    }

    public void setCurrentChannel(String channel){
        currentChannel = channel;
    }

    public StringBuilder getBuilder(){
        return builder;
    }

    public void run(){
        int chr;

        while(!socket.isClosed()){
            try {
                //TODO Initial capacity is 16. After 16 characters are inputted, the capacity increase method slows down the app and bugs occur....
                builder = new StringBuilder();

                // Reading the raw inputs in the console char by char
                do{
                    chr = RawConsoleInput.read(false);

                    // If backspace detected - delete one char and remove it from the builder
                    if(chr == 8){
                        System.out.print("\b \b");
                        int len = builder.length();

                        if(len > 0)
                            builder.deleteCharAt(len - 1);
                    }

                    // If char is valid, append it to builder and print
                    else if(chr != -2){
                        builder.append((char)chr);
                        System.out.print((char)chr);
                    }
                }
                while(chr != 13); // While not enter

                System.out.println("");

                if(currentChannel.equals("") || builder.toString().startsWith("/"))
                    writer.println(builder);
                else
                    writer.println("PRIVMSG " + currentChannel + " :" + builder);

                if(builder.toString().startsWith(INDICATOR_QUIT))
                    break;
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

            if(writer != null)
                writer.close();
        }
        catch(IOException ex){

        }

        client.cleanUp();
    }
}