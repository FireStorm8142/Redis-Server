import java.nio.channels.SocketChannel;

public class WaitingClients{

    SocketChannel client;
    long expiry;

    public WaitingClients(SocketChannel clientChannel, double timeout){
        this.client = clientChannel;
        if (timeout == 0) expiry = Long.MAX_VALUE;
        else expiry = System.currentTimeMillis() + (long) (timeout * 1000);
    }
}
