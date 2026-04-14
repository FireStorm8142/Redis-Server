import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class RespParser {
    
    public static List<String> parse(ByteBuffer buffer){
        List<String> result = new ArrayList<>();

        char prefix = (char) buffer.get();
        if (prefix != '*') throw new RuntimeException("Invalid RESP: * expected");

        int arg = readArg(buffer);

        for (int i=0; i<arg; i++){
            if((char)buffer.get() != '$') throw new RuntimeException("Expected Bulk String");

            int size = readArg(buffer);
            byte[] data = new byte[size];
            buffer.get(data);
            result.add(new String(data));
            
            buffer.get();
            buffer.get();
        }
        return result;
    }

    public static int readArg(ByteBuffer buffer){
        StringBuilder sb = new StringBuilder();

        while (true) { 
            char c = (char) buffer.get();
            if (c=='\r'){
                buffer.get();
                break;
            }
            sb.append(c);
        }

        return Integer.parseInt(sb.toString());
    }
}

