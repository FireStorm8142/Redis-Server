package server;

import java.nio.channels.SocketChannel;

public class WaitingClients{

    public SocketChannel client;
    public long expiry;

    public WaitingClients(SocketChannel clientChannel, double timeout){
        this.client = clientChannel;
        if (timeout == 0) expiry = Long.MAX_VALUE;
        else expiry = System.currentTimeMillis() + (long) (timeout * 1000);
    }
}
