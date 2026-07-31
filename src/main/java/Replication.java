public class Replication {
    public String role;
    public String master_replid;
    public String master_repl_offset;


    public Replication(String role, String master_replid, String master_repl_offset){
        this.role = role;
        this.master_replid = master_replid;
        this.master_repl_offset = master_repl_offset;
    }

    public String getReplInfo(){
        return "role:" + role + "\r\n" +
                "master_replid:" + master_replid + "\r\n" +
                "master_repl_offset:" + master_repl_offset + "\r\n";
    }
}
