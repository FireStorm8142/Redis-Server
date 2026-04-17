import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HandleCommand {

    public static String handleCommand(List<String> command, HashMap<String, String> storage, HashMap<String, Long> expiry, HashMap<String, List<String>> listStorage) {
        String cmd = command.get(0).toUpperCase();
        String response;
        StringBuilder sb;
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

            case "LRANGE":
                List<String> array = listStorage.getOrDefault(command.get(1), null);
                if (array == null) response = "*0\r\n";
                else {
                    int start=Integer.parseInt(command.get(2));
                    start = Math.max((start >= 0 ? start : array.size()+start), 0);
                    int end=Integer.parseInt(command.get(3));
                    end = Math.min((end >= 0 ? end : array.size()+end), array.size()-1);
                    if (start > end){
                        response = "*0\r\n";
                        break;
                    }
                    List<String> temp = array.subList(start, end+1);
                    sb = new StringBuilder();
                    sb.append("*").append(temp.size()).append("\r\n");
                    for (String s : temp) {
                        sb.append("$").append(s.length()).append("\r\n").append(s).append("\r\n");
                    }
                    response=sb.toString();
                }
                break;

            case "LPUSH":
                List<String> list2 = listStorage.getOrDefault(command.get(1), null);
                if (list2 == null) list2 = new ArrayList<>();
                for (int i = 2; i<command.size(); i++){
                    list2.add(0, command.get(i));
                }
                listStorage.put(command.get(1), list2);
                response = ":"+list2.size()+"\r\n";
                break;

            case "LLEN":
                List<String> list3 = listStorage.getOrDefault(command.get(1), null);
                if (list3 == null) response = ":0\r\n";
                else response = ":"+list3.size()+"\r\n";
                break;

            case "LPOP":
                List<String> list4  = listStorage.getOrDefault(command.get(1), null);
                if (list4 == null) response = "$-1\r\n";
                else{
                    if (command.size() > 2){
                        int temp = Integer.parseInt(command.get(2));
                        sb = new StringBuilder();

                        sb.append("*").append(Math.min(temp, list4.size())).append("\r\n");
                        if (temp >= list4.size()){
                            while(!list4.isEmpty()){
                                String element = list4.remove(0);
                                sb.append("$").append(element.length()).append("\r\n").append(element).append("\r\n");
                            }
                            response = sb.toString();
                            break;
                        }

                        while (temp > 0){
                            String element = list4.remove(0);
                            sb.append("$").append(element.length()).append("\r\n").append(element).append("\r\n");
                            temp--;
                        }
                        response = sb.toString();
                        break;
                    }

                    String element = list4.remove(0);
                    response="$"+element.length()+"\r\n"+element+"\r\n";
                }
                break;

            default:
                response=null;
                break;
        }
        return response;
    }
}
