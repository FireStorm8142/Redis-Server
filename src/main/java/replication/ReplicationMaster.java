package replication;

import server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

public class ReplicationMaster {

    /*List<ReplicaConnection> replicas = new ArrayList<>();*/
    stateMaster state;

    public ReplicationMaster() { state = stateMaster.WAITING_FOR_REPLCONF; }

    enum stateMaster {
        WAITING_FOR_REPLCONF,
        WAITING_FOR_CAPA,
        CONNECTED
    }

    public void processRequest(SelectionKey key, List<String> command, Server server) throws IOException {
        SocketChannel slaveChannel = (SocketChannel) key.channel();
        switch(state) {
            case WAITING_FOR_REPLCONF:
                if ("replconf".equalsIgnoreCase(command.getFirst())) {
                    String ok = "+OK\r\n";
                    slaveChannel.write(ByteBuffer.wrap(ok.getBytes()));
                    state = stateMaster.WAITING_FOR_CAPA;
                }

            case WAITING_FOR_CAPA:
                if ("replconf".equalsIgnoreCase(command.getFirst())) {
                    String ok = "+OK\r\n";
                    slaveChannel.write(ByteBuffer.wrap(ok.getBytes()));
                    state = stateMaster.CONNECTED;
                    System.out.println("Slave connected");
                }

            default: break;
        }
    }
}
