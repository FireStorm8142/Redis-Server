import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HandleCommand {

    public static String handleCommand(List<String> command, HashMap<String, String> storage, HashMap<String, Long> expiry, HashMap<String, List<String>> listStorage) {
        String cmd = command.get(0).toUpperCase();
        String response;
        switch(cmd) {
            case "ECHO":
                String msg = command.get(1);
                response = "$" + msg.length() + "\r\n" + msg + "\r\n";
                break;

            case "PING":
                response = "+PONG\r\n";
                break;

            case "GET":
                long now = System.currentTimeMillis();
                if (expiry.getOrDefault(command.get(1), Long.MAX_VALUE) < now) {
                    expiry.remove(command.get(1));
                    storage.remove(command.get(1));
                }
                String value = storage.getOrDefault(command.get(1), null);
                if (value != null) {
                    response = "$" + value.length() + "\r\n" + value + "\r\n";
                } else {
                    response = "$-1\r\n";
                }
                break;

            case "SET":
                if (command.size() > 3) {
                    if (command.get(3).equalsIgnoreCase("PX")) {
                        expiry.put(command.get(1), System.currentTimeMillis() + Long.parseLong(command.get(4)));
                    } else {
                        expiry.put(command.get(1), System.currentTimeMillis() + Long.parseLong(command.get(4)) * 1000);
                    }
                }
                storage.put(command.get(1), command.get(2));
                response = "+OK\r\n";
                break;

            case "RPUSH":
                List<String> list = listStorage.getOrDefault(command.get(1), null);
                if (list == null) list = new ArrayList<>();
                for (int i =2; i<command.size(); i++){
                    list.add(command.get(i));
                }
                listStorage.put(command.get(1), list);

                int size = list.size();
                response = ":"+size+"\r\n";
                break;

            default:
                response=null;
                break;
        }
        return response;
    }
}

