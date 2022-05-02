package com.routing;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;

public class Message implements Serializable {
    String command;

    //REFRESH part
    int id;
    int port;
    InetAddress ip;
    Map<Integer, Integer> routingTable;

    //SEND part
    int cost;
    int next;
    String msg;

    public Message(Node n, String command, String msg, int cost){
        this.id = n.getNodeId();
        this.port = n.getPort();
        this.ip = n.getIpAddress();
        this.routingTable = n.getRoutingTable();
        this.command = command;
        this.msg = msg;
        this.cost = cost;
    }

    public int getNodeId(){
        return id;
    }

    public String getMsg(){
        return msg;
    }

    public void setCost(int cost){
        this.cost = cost;
    }

    public Map<Integer, Integer> getRoutingTable(){
        return routingTable;
    }
}
