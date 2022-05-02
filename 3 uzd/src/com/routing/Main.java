package com.routing;

import java.io.*;
import java.net.*;
import java.util.*;

public class Main {
    public static final int PORT = 5432;
    public static final int BUFFER = 4096;

    private static final String COMMAND_MAKE = "make";
    private static final String COMMAND_ADD = "add";
    private static final String COMMAND_ADD_REL = "addrel";
    private static final String COMMAND_UPDATE = "update";
    private static final String COMMAND_REFRESH = "refresh";
    private static final String COMMAND_DISPLAY = "display";
    private static final String COMMAND_REMOVE = "remove";
    private static final String COMMAND_REMOVE_REL = "removerel";
    private static final String COMMAND_SEND = "send";
    private static final String COMMAND_QUIT = "quit";

    private static final String MSG_TOPOLOGY_DONE = "Done reading topology!";
    private static final String MSG_DISABLED_CONNECTION = "Disabled connection with node ";
    private static final String MSG_ROUTING_TABLE = "Destination \t Optimal hop \t Cost";
    private static final String MSG_HOP = ", hopping to router: ";
    private static final String MSG_SENDING_MSG = "Sending message: ";
    private static final String MSG_CANNOT_REACH = "Cannot reach the destination!";
    private static final String MSG_RELATION_REMOVED = "Relation removed!";
    private static final String MSG_ROUTER_ADDED = "Router added!";
    private static final String MSG_RELATION_NOT_FOUND = "Relation not found!";
    private static final String MSG_RELATION_EXISTS = "Relation already exists!";
    private static final String MSG_RELATION_ADDED = "Relation added!";

    private static final String ERR_SERVER = "Server error!";
    private static final String ERR_INCORRECT_COMMAND = "Incorrect command!";
    private static final String ERR_INCORRECT_PARAMETERS = "Incorrect parameters!";
    private static final String ERR_MAKE_NOT_DONE = "'make' command not done yet!";
    private static final String ERR_NODE_NOT_FOUND = "Node not found!";
    private static final String ERR_FILE_NOT_FOUND = "File not found!";
    private static final String ERR_NOT_NEIGHBORS = "You can only update cost to neighbor nodes!";
    private static final String ERR_IO = "IO Exception!";
    private static final String ERR_CANNOT_REACH = "Cannot reach this destination!";

    public static List<Node> nodes = new ArrayList<>();
    public static Hashtable<Node, Socket> clients = new Hashtable<>();

    private static ServerSocket socket;

    public static void printMenu(){
        System.out.println("");
        System.out.println("*********Custom Routing Protocol**********");
        System.out.println("1. make <topology-file>");
        System.out.println("2. add <id> <ip>");
        System.out.println("3. addrel <id1> <id2> <cost>");
        System.out.println("4. update <id1> <id2> <cost>");
        System.out.println("5. refresh");
        System.out.println("6. display <id>");
        System.out.println("7. remove <id>");
        System.out.println("9. removerel <id1> <id2>");
        System.out.println("9. send <id> <id> <msg>");
        System.out.println("10. quit");
        System.out.println("");
    }

    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);

        socket = new ServerSocket(PORT);

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket s;
                while(true) {
                    try {
                        s = socket.accept();

                        Node n = nodes.get(nodes.size() - 1);

                        n.out_server = new ObjectOutputStream(s.getOutputStream());
                        n.in_server = new ObjectInputStream(s.getInputStream());

                        clients.put(n, s);
                    }
                    catch (IOException e) {
                        System.err.println(ERR_SERVER);
                        System.exit(1);
                    }
                }
            }
        });
        t1.start();

        while (true) {
            printMenu();

            String line = in.nextLine();
            String[] arguments = line.split(" ");
            String command = arguments[0];

            switch (command) {
                case COMMAND_MAKE:
                    if (arguments.length != 2) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }

                    if ((arguments[1].equals(""))) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else {
                        String filename = arguments[1];
                        readTopology(filename);
                    }
                    break;

                case COMMAND_ADD:
                    if (arguments.length != 3) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else{
                        int id;
                        String ip;

                        try {
                            id = Integer.parseInt(arguments[1]);
                            ip = arguments[2];
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        addRouter(id, ip);
                    }
                    break;

                case COMMAND_ADD_REL:
                    if (arguments.length != 4) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else{
                        int id1;
                        int id2;
                        int cost;

                        try {
                            id1 = Integer.parseInt(arguments[1]);
                            id2 = Integer.parseInt(arguments[2]);
                            cost = Integer.parseInt(arguments[3]);
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        addRelation(id1, id2, cost);
                        refresh();
                    }
                    break;

                case COMMAND_UPDATE:
                    if (arguments.length != 4) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else {
                        int id1;
                        int id2;
                        int cost;

                        try {
                            id1 = Integer.parseInt(arguments[1]);
                            id2 = Integer.parseInt(arguments[2]);
                            cost = Integer.parseInt(arguments[3]);
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        update(id1, id2, cost);
                    }

                    break;

                case COMMAND_REFRESH:
                    refresh();
                    break;

                case COMMAND_DISPLAY:
                    if (arguments.length != 2) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else {
                        int id;

                        try {
                            id = Integer.parseInt(arguments[1]);
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        Node toDisplay = getNodeById(id);

                        if (toDisplay == null) {
                            System.out.println(ERR_NODE_NOT_FOUND);
                            break;
                        }

                        display(id);
                    }
                    break;

                case COMMAND_REMOVE:
                    if (arguments.length != 2) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else{
                        int id;

                        try {
                            id = Integer.parseInt(arguments[1]);
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        Node toDisable = getNodeById(id);

                        if (toDisable == null) {
                            System.out.println(ERR_NODE_NOT_FOUND);
                            break;
                        }

                        remove(toDisable);
                    }
                    break;

                case COMMAND_REMOVE_REL:
                    if (arguments.length != 3) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else{
                        int id1;
                        int id2;

                        try {
                            id1 = Integer.parseInt(arguments[1]);
                            id2 = Integer.parseInt(arguments[2]);
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        removeRelation(id1, id2);
                        refresh();
                    }
                    break;

                case COMMAND_SEND:
                    if (arguments.length != 4) {
                        System.out.println(ERR_INCORRECT_COMMAND);
                        break;
                    }
                    else {
                        String from = arguments[1];
                        String to = arguments[2];
                        String msg = arguments[3];
                        Node n, t;

                        try {
                            n = getNodeById(Integer.parseInt(from));
                            t = getNodeById(Integer.parseInt(to));

                            if (n == null || t == null) {
                                System.out.println(ERR_NODE_NOT_FOUND);
                                break;
                            }
                        }
                        catch (Exception e){
                            System.out.println(ERR_INCORRECT_PARAMETERS);
                            break;
                        }

                        if(!n.getOptimalHops().containsKey(Integer.parseInt(to))){
                            System.out.println(MSG_CANNOT_REACH);
                            break;
                        }

                        Integer hopTo = n.getOptimalHops().get(t.getNodeId());
                        Node next;

                        next = getNodeById(hopTo);

                        if(next == null){
                            System.out.println(ERR_CANNOT_REACH);
                            System.exit(1);
                        }

                        int cost = n.getRoutingTable().get(hopTo);
                        System.out.println(MSG_SENDING_MSG + msg + " || from: " + from + MSG_HOP + hopTo + ", cost: " + cost);
                        sendMessage(next, new Message(t, COMMAND_SEND, msg, cost));
                    }
                    break;

                case COMMAND_QUIT:
                    System.exit(1);

                default:
                    System.out.println(ERR_INCORRECT_COMMAND);
            }
        }
    }

    public static void readTopology(String filename) {
        //Reading the file and adding the routers and relations
        File file = new File("./com/routing/" + filename);

        try {
            nodes.clear();

            Scanner scanner = new Scanner(file);

            int nodeNumber = scanner.nextInt();
            int neighborNumber = scanner.nextInt();
            scanner.nextLine();

            for (int i = 0; i < nodeNumber; ++i) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");
                addRouter(Integer.parseInt(parts[0]), parts[1]);
            }

            for (int i = 0; i < neighborNumber; ++i) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");

                int fromID = Integer.parseInt(parts[0]);
                int toID = Integer.parseInt(parts[1]);
                int cost = Integer.parseInt(parts[2]);

                addRelation(fromID, toID, cost);
            }

            refresh();

            System.out.println(MSG_TOPOLOGY_DONE);
            scanner.close();
        }
        catch (IOException e) {
            System.out.println(ERR_IO);
        }
    }

    public static void addRouter(int id, String ip){
        if(getNodeById(id) != null){
            System.out.println("Router with id:" + id + " is already present.");
            return;
        }

        Node node = new Node(id, ip, PORT);
        node.putToRoutingTable(node.getNodeId(), 0);
        node.putToOptimalHops(node.getNodeId(), node.getNodeId());
        System.out.println(MSG_ROUTER_ADDED);
    }

    public static void addRelation(int fromID, int toID, int cost){
        Node from = getNodeById(fromID);
        Node to = getNodeById(toID);

        if(from == null || to == null){
            System.out.println(ERR_NODE_NOT_FOUND);
            return;
        }

        if(from.isNeighbor(to)){
            System.out.println(MSG_RELATION_EXISTS);
            return;
        }

        from.putToRoutingTable(toID, cost);
        from.putToOptimalHops(toID, toID);
        from.putToNeighbors(to);

        to.putToRoutingTable(fromID, cost);
        to.putToOptimalHops(fromID, fromID);
        to.putToNeighbors(from);

        System.out.println(MSG_RELATION_ADDED);
    }

    public static void removeRelation(int fromID, int toID){
        Node from = getNodeById(fromID);
        Node to = getNodeById(toID);

        if(from == null || to == null){
            System.out.println(ERR_NODE_NOT_FOUND);
            return;
        }

        if(!from.isNeighbor(to)){
            System.out.println(MSG_RELATION_NOT_FOUND);
            return;
        }

        from.removeFromRoutingTable(toID);
        from.getOptimalHops().remove(toID);
        from.getNeighbors().remove(to);

        to.removeFromRoutingTable(fromID);
        to.getOptimalHops().remove(fromID);
        to.getNeighbors().remove(from);

        System.out.println(MSG_RELATION_REMOVED);
    }

    public static Node getNodeById(int id) {
        for (Node node : nodes) {
            if (node.getNodeId() == id)
                return node;
        }

        return null;
    }

    public static void update(int id1, int id2, int cost){
        Node from = getNodeById(id1);
        Node to = getNodeById(id2);

        if(from == null || to == null) {
            System.out.println(ERR_NODE_NOT_FOUND);
            return;
        }

        if (from.isNeighbor(to)) {
            from.getRoutingTable().replace(id2, cost);
            to.getRoutingTable().replace(id1, cost);

            resetRoutingTables(COMMAND_UPDATE, null);
            refresh();
        }
        else
            System.out.println(ERR_NOT_NEIGHBORS);
    }

    private static void resetRoutingTables(String command, Node toDisable){
        //Leaves only the router and its neighbors
        for (Node r : nodes) {
            if(command.equals(COMMAND_REMOVE)){
                r.removeFromRoutingTable(toDisable.getNodeId());
                r.getNeighbors().remove(toDisable);
            }

            Map<Integer, Integer> toPut = new HashMap<>(r.getRoutingTable());

            for(Map.Entry<Integer, Integer> entry : r.getRoutingTable().entrySet()){
                if(r.isNeighbor(getNodeById(entry.getKey())) || r.getNodeId() == entry.getKey()) {
                    r.getOptimalHops().replace(entry.getKey(), entry.getKey());
                    continue;
                }

                toPut.remove(entry.getKey());
                r.getOptimalHops().remove(entry.getKey());
            }

            r.getRoutingTable().clear();
            r.getRoutingTable().putAll(toPut);
        }
    }

    public static void refresh() {
        for(Node r : nodes) {
            //sending message to each neighbor
            Message message = new Message(r, COMMAND_REFRESH, "", 0);

            for (Node neighbor : r.getNeighbors())
                sendMessage(neighbor, message);
        }

        for(Node r : nodes) {
            //sending message from each neighbor
            for (Node neighbor : r.getNeighbors()) {
                Message message = new Message(neighbor, COMMAND_REFRESH, "", 0);
                sendMessage(r, message);
            }
        }
    }

    public static void sendMessage(Node to, Message message){
        //Commutator.forwardPacket(to, message);

        try {
            to.out_server.reset();
            to.out_server.writeObject(message);
            to.startThread();
        }
        catch(IOException e){
            System.out.println(ERR_IO);
        }
    }

    public static void remove(Node toDisable) {
        nodes.remove(toDisable);
        clients.remove(toDisable);

        try {
            toDisable.getSocket().close();
        }
        catch (IOException e){
            System.out.println(ERR_IO);
        }

        resetRoutingTables(COMMAND_REMOVE, toDisable);
        refresh();

        System.out.println(MSG_DISABLED_CONNECTION + toDisable.getNodeId() + "(" + toDisable.getIpAddress() + ")");
    }

    public static void display(int id) {
        //Displays the router's id, its destinations, optimal hops and costs.

        Node toDisplay = getNodeById(id);

        if(toDisplay == null) {
            System.out.println(ERR_NODE_NOT_FOUND);
            return;
        }

        System.out.println("\n");
        System.out.println("Router's information - ID: " + id + ", ip: " + toDisplay.getIpAddress());
        System.out.println(MSG_ROUTING_TABLE);
        nodes.sort(new NodeComparator());

        for (Integer r : toDisplay.getRoutingTable().keySet()) {
            int cost = toDisplay.getRoutingTable().get(r);
            String costStr = "" + cost;
            System.out.println("" + r + " \t\t " + toDisplay.getOptimalHops().getOrDefault(r, -1) + " \t\t " +  costStr);
        }
    }
}
