package replication;

import server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

public class ReplicationSlave {

    SocketChannel masterChannel;
    stateSlave state;

    public ReplicationSlave(Server server, Selector selector) throws IOException{
        state = stateSlave.CONNECTING;
        this.connectToMaster(server, selector);
    }

    enum stateSlave {
        CONNECTING,
        WAITING_FOR_PONG,
        WAITING_FOR_REPLCONF_OK,
        WAITING_FOR_CAPA_OK,
        WAITING_FOR_FULLRESYNC
    }

    public void processResponse(SelectionKey key, List<String> command, Server server) throws IOException {
        SocketChannel masterChannel = (SocketChannel) key.channel();
        switch (state) {
            case WAITING_FOR_PONG:
                if ("pong".equalsIgnoreCase(command.getFirst())) {
                    String replconf = "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n"+server.port+"\r\n";
                    masterChannel.write(ByteBuffer.wrap(replconf.getBytes()));
                    state = stateSlave.WAITING_FOR_REPLCONF_OK;
                }
                break;

            case WAITING_FOR_REPLCONF_OK:
                if ("ok".equalsIgnoreCase(command.getFirst())) {
                    String replconf = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n";
                    masterChannel.write(ByteBuffer.wrap(replconf.getBytes()));
                    state = stateSlave.WAITING_FOR_CAPA_OK;
                }

            case WAITING_FOR_CAPA_OK:
                if ("ok".equalsIgnoreCase(command.getFirst())) {
                    String psync = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n";
                    masterChannel.write(ByteBuffer.wrap(psync.getBytes()));
                    state = stateSlave.WAITING_FOR_FULLRESYNC;
                    System.out.println("Master connected");
                }

            default: break;
        }
    }

    public void connectToMaster(Server server, Selector selector) throws IOException {
        SocketChannel masterChannel = SocketChannel.open();
        masterChannel.configureBlocking(false);
        masterChannel.connect(new InetSocketAddress(server.master_host, server.master_port));
        masterChannel.register(selector, SelectionKey.OP_CONNECT, "master");
        this.masterChannel = masterChannel;
    }

    public void finishConnectToMaster(SelectionKey key) throws IOException{
        masterChannel.finishConnect();
        String ping = "*1\r\n$4\r\nPING\r\n";
        masterChannel.write(ByteBuffer.wrap(ping.getBytes()));
        key.interestOps(SelectionKey.OP_READ);
        state = stateSlave.WAITING_FOR_PONG;
    }

}
