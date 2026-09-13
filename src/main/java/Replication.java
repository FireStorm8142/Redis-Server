public class Replication {
    public String role;
    public String master_replid;
    public String master_repl_offset;
    public String master_host;
    public int master_port;


    public Replication(String role, String master_replid, String master_repl_offset, String master_host, int master_port){
        this.role = role;
        this.master_replid = master_replid;
        this.master_repl_offset = master_repl_offset;
        this.master_host = master_host;
        this.master_port = master_port;
    }

    public String getReplInfo(){
        return "role:" + role + "\r\n" +
                "master_replid:" + master_replid + "\r\n" +
                "master_repl_offset:" + master_repl_offset + "\r\n" +
                "master_host:" + master_host + "\r\n" +
                "master_port" + master_port + "\r\n";
    }
}
