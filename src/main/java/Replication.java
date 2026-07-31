public class Replication {
    public static String role = "master";
    public Replication(){

    }

    public static String getReplInfo(){
        return "role:"+role;
    }
}
