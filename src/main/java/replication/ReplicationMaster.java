package replication;

import RESP.RespEncoder;
import server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class ReplicationMaster {

    private final Server server;
    List<Replica> replicas = new ArrayList<>();

    public ReplicationMaster(Server server) {
        this.server = server;
    }

    public void processRequest(SelectionKey key, List<String> command) throws IOException {
        Replica replica = (Replica) key.attachment();
        switch(replica.getState()) {
            case WAITING_FOR_REPLCONF:
                if ("replconf".equalsIgnoreCase(command.getFirst())) {
                    String ok = "+OK\r\n";
                    replica.getChannel().write(ByteBuffer.wrap(ok.getBytes()));
                    replica.setState(Replica.stateMaster.WAITING_FOR_CAPA);
                }
                break;

            case WAITING_FOR_CAPA:
                if ("replconf".equalsIgnoreCase(command.getFirst())) {
                    String ok = "+OK\r\n";
                    replica.getChannel().write(ByteBuffer.wrap(ok.getBytes()));
                    replica.setState(Replica.stateMaster.WAITING_FOR_PSYNC);
                }
                break;

            case WAITING_FOR_PSYNC:
                if ("psync".equalsIgnoreCase(command.getFirst())) {
                    String response = "+FULLRESYNC "+ server.master_replid + " 0\r\n";
                    replica.getChannel().write(ByteBuffer.wrap(response.getBytes()));
                    response = "$19\r\n";
                    replica.getChannel().write(ByteBuffer.wrap(response.getBytes()));
                    byte[] rdb = {
                            0x52, 0x45, 0x44, 0x49, 0x53,
                            0x30, 0x30, 0x31, 0x31,
                            (byte) 0xFF,
                            0, 0, 0, 0, 0, 0, 0, 0, 0
                    };
                    replica.getChannel().write(ByteBuffer.wrap(rdb));
                    replica.setState(Replica.stateMaster.CONNECTED);
                    replicas.add(replica);
                    System.out.println("Slave connected");
                }
                break;

            default: break;
        }
    }

    public void propagateCommand(List<String> command) throws IOException{
        String response = RespEncoder.encode(command);
        for (Replica replica : replicas) {
            SocketChannel slave = replica.getChannel();
            slave.write(ByteBuffer.wrap(response.getBytes()));
        }
    }
}
