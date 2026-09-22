package server;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;

public class Server {
    public String role;
    public int port;
    public String master_replid;
    public String master_repl_offset;
    public String master_host;
    public int master_port;
    public HashMap<String, String> storage;
    public HashMap<String, Long> expiry;
    public HashMap<String, List<String>> listStorage;
    public HashMap<String, Queue<WaitingClients>> waitingClients;


    public Server(String role, int port, String master_replid, String master_repl_offset, String master_host, int master_port){
        this.role = role;
        this.port = port;
        this.master_replid = master_replid;
        this.master_repl_offset = master_repl_offset;
        this.master_host = master_host;
        this.master_port = master_port;
        this.storage = new HashMap<>();
        this.expiry = new HashMap<>();
        this.listStorage = new HashMap<>();
        this.waitingClients = new HashMap<>();
    }

    public String getReplInfo(){
        return "role:" + role + "\r\n" +
                "master_replid:" + master_replid + "\r\n" +
                "master_repl_offset:" + master_repl_offset + "\r\n" +
                "master_host:" + master_host + "\r\n" +
                "master_port:" + master_port + "\r\n";
    }

    public HashMap<String, String> getStorage() { return storage; }

    public HashMap<String, Long> getExpiry() {
        return expiry;
    }

    public HashMap<String, List<String>> getListStorage() { return listStorage; }

    public HashMap<String, Queue<WaitingClients>> getWaitingClients() { return waitingClients; }

}
