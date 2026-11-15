package RESP;

import java.util.List;

public class RespEncoder {

    public static String encode(List<String> command) {
        StringBuilder string = new StringBuilder();

        int length = command.size();
        string.append('*').append(length).append("\r\n");
        for (String word : command) {
            int len = word.length();
            string.append('$').append(len).append("\r\n").append(word).append("\r\n");
        }

        return string.toString();
    }
}
