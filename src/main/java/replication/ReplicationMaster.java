package replication;

import server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

public class ReplicationMaster {

    stateMaster state;

    public ReplicationMaster() { state = stateMaster.WAITING_FOR_REPLCONF; }

    enum stateMaster {
        WAITING_FOR_REPLCONF,
        WAITING_FOR_CAPA,
        WAITING_FOR_PSYNC,
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
                break;

            case WAITING_FOR_CAPA:
                if ("replconf".equalsIgnoreCase(command.getFirst())) {
                    String ok = "+OK\r\n";
                    slaveChannel.write(ByteBuffer.wrap(ok.getBytes()));
                    state = stateMaster.WAITING_FOR_PSYNC;
                }
                break;

            case WAITING_FOR_PSYNC:
                if ("psync".equalsIgnoreCase(command.getFirst())) {
                    String response = "+FULLRESYNC "+ server.master_replid + " 0\r\n";
                    slaveChannel.write(ByteBuffer.wrap(response.getBytes()));
                    response = "$19\r\n";
                    slaveChannel.write(ByteBuffer.wrap(response.getBytes()));
                    byte[] rdb = {
                            0x52, 0x45, 0x44, 0x49, 0x53,
                            0x30, 0x30, 0x31, 0x31,
                            (byte) 0xFF,
                            0, 0, 0, 0, 0, 0, 0, 0, 0
                    };
                    slaveChannel.write(ByteBuffer.wrap(rdb));
                    state = stateMaster.CONNECTED;
                    System.out.println("Slave connected");
                }
                break;

            default: break;
        }
    }
}
