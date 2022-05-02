package com.routing;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

public class Node implements Serializable{
    private static final String COMMAND_REFRESH = "refresh";
    private static final String COMMAND_SEND = "send";

    private static final String ERR_CLASS_NOT_FOUND = "Class not found Exception!";
    private static final String ERR_IO = "IO Exception!";
    private static final String ERR_CANNOT_REACH = "Cannot reach this destination!";
    private static final String ERR_UNKNOWN_HOST = "Unknown host exception!";
    private static final String ERR_SOCKET = "Socket exception!";

    private static final String MSG_HOP = ", hopping to router: ";
    private static final String MSG_SENDING_MSG = "Sending message: ";

    private int id;
    private int port;

    private InetAddress ip;
    private Socket socket;

    Map<Integer, Integer> routingTable = new HashMap<>();
    Set<Node> neighbors = new HashSet<>();
    Map<Integer, Integer> optimalHops = new HashMap<>();

    public ObjectInputStream in_client;
    public ObjectOutputStream out_client;

    public ObjectInputStream in_server;
    public ObjectOutputStream out_server;

    public Node(int id, String ip, int port) {
        try {
            this.id = id;
            this.ip = InetAddress.getByName(ip);
            this.port = port;

            //Adding the router to nodes list, creating the socket and streams
            Main.nodes.add(this);

            socket = new Socket(ip, port);

            out_client = new ObjectOutputStream(socket.getOutputStream());
            in_client = new ObjectInputStream(socket.getInputStream());
        }
        catch (UnknownHostException e) {
            System.out.println(ERR_UNKNOWN_HOST);
        }
        catch (SocketException e) {
            System.out.println(ERR_SOCKET);
        }
        catch (IOException e) {
            System.out.println(ERR_IO);
        }
    }

    public Node getNodeById(int id) {
        for (Node node : neighbors) {
            if (node.getNodeId() == id)
                return node;
        }

        return null;
    }

    public void receivePacket(Message msg){
        if(msg.command.equals(COMMAND_SEND)){
            String message = msg.getMsg();

            if(getNodeId() == msg.getNodeId())
                System.out.println("Destination reached (id: " + getNodeId() + ", ip: " + getIpAddress() + "), message received - " + message);
            else{
                //If this is not the destination - calculate the next hop and send to it

                Integer hopTo = getOptimalHops().get(msg.getNodeId());
                Node next;

                try {
                    next = getNodeById(hopTo);
                }
                catch (NullPointerException e){
                    System.out.println(ERR_CANNOT_REACH);
                    return;
                }

                int cost = getRoutingTable().get(hopTo);
                msg.setCost(cost);

                System.out.println(MSG_SENDING_MSG + message + " || from: " + getNodeId() + MSG_HOP + hopTo + ", cost: " + cost);
                //Commutator.forwardPacket(next, msg);

                Main.sendMessage(next, msg);
            }
        }
        else if(msg.command.equals(COMMAND_REFRESH)) {
            //temporary routing table (current router)
            Map<Integer, Integer> toPut = new HashMap<>(routingTable);

            //checking each entry in our routing table
            for (Map.Entry<Integer, Integer> thisEntry : routingTable.entrySet()) {
                //router id in table
                String thisKeyId = String.valueOf(thisEntry.getKey());
                //router id, which we got from msg
                String msgKeyId = String.valueOf(msg.getNodeId());

                //if the ids are equal
                if (thisKeyId.equals(msgKeyId)) {
                    //checking each entry in our received routing table
                    for (Map.Entry<Integer, Integer> msgEntry : msg.getRoutingTable().entrySet()) {
                        //getting the ids of each router in received table
                        String msgNeighborKeyId = String.valueOf(msgEntry.getKey());
                        Integer neighbour = Integer.parseInt(msgNeighborKeyId);

                        //if our routing table does not contain the received id
                        if (!toPut.containsKey(neighbour)) {
                            toPut.put(neighbour, thisEntry.getValue() + msgEntry.getValue());
                            optimalHops.put(neighbour, Integer.parseInt(thisKeyId));
                        }
                        else {
                            if (toPut.get(neighbour) > thisEntry.getValue() + msgEntry.getValue()) {
                                toPut.replace(neighbour, thisEntry.getValue() + msgEntry.getValue());
                                optimalHops.replace(neighbour, Integer.parseInt(thisKeyId));
                            }
                        }
                    }
                }
            }

            routingTable.clear();
            routingTable.putAll(toPut);
        }
    }

    public void startThread(){
        /*Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){*/
                    try{
                        Message msg = (Message) in_client.readObject();
                        receivePacket(msg);
                    }
                    catch(IOException e){
                        System.out.println(ERR_IO);
                       // break;
                    }
                    catch(ClassNotFoundException e){
                        System.out.println(ERR_CLASS_NOT_FOUND);
                       // break;
                    }
                //}
           /* }
        });
        t1.start();*/
    }

    public Socket getSocket(){
        return socket;
    }

    public void setSocket(Socket s){
        this.socket = s;
    }

    public int getNodeId(){
        return id;
    }

    public int getPort(){
        return port;
    }

    public InetAddress getIpAddress(){
        return ip;
    }

    public Map<Integer, Integer> getRoutingTable(){
        return routingTable;
    }

    public Set<Node> getNeighbors(){
        return neighbors;
    }

    public Map<Integer, Integer> getOptimalHops(){
        return optimalHops;
    }

    public void putToOptimalHops(Integer from, Integer to){
        optimalHops.put(from, to);
    }

    public void putToRoutingTable(Integer r, Integer cost){
        routingTable.put(r, cost);
    }

    public void removeFromRoutingTable(Integer r){
        routingTable.remove(r);
    }

    public void putToNeighbors(Node r){
        neighbors.add(r);
    }

    public boolean isNeighbor(Node r) {
        return neighbors.contains(r);
    }
}
