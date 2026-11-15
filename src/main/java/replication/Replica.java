package replication;

import java.nio.channels.SocketChannel;

public class Replica {

    private final SocketChannel channel;
    private stateMaster state;

    public Replica(SocketChannel channel) {
        this.channel = channel;
        this.state = stateMaster.WAITING_FOR_REPLCONF;
    }

    enum stateMaster {
        WAITING_FOR_REPLCONF,
        WAITING_FOR_CAPA,
        WAITING_FOR_PSYNC,
        CONNECTED
    }

    SocketChannel getChannel() { return channel; }

    stateMaster getState() { return state; }

    void setState(stateMaster state) { this.state = state; }
}
