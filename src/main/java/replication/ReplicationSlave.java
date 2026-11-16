package replication;

import RESP.RespParser;
import server.Server;
import Handler.HandleCommand;

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
    int rdbBytesLeft;
    int rdbBytesRead;
    byte[] rdb;

    public ReplicationSlave(Server server, Selector selector) throws IOException{
        state = stateSlave.CONNECTING;
        this.connectToMaster(server, selector);
    }

    enum stateSlave {
        CONNECTING,
        WAITING_FOR_PONG,
        WAITING_FOR_REPLCONF_OK,
        WAITING_FOR_CAPA_OK,
        WAITING_FOR_FULLRESYNC,
        WAITING_FOR_RDB_HEADER,
        WAITING_FOR_RDB,
        REPLICATING
    }

    public void processResponse(SelectionKey key, Server server, ByteBuffer buffer) throws IOException {
        SocketChannel masterChannel = (SocketChannel) key.channel();
        List<String> command;
        switch (state) {
            case WAITING_FOR_PONG:
                command = RespParser.parse(buffer);
                if ("pong".equalsIgnoreCase(command.getFirst())) {
                    String replconf = "*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n"+server.port+"\r\n";
                    masterChannel.write(ByteBuffer.wrap(replconf.getBytes()));
                    state = stateSlave.WAITING_FOR_REPLCONF_OK;
                }
                break;

            case WAITING_FOR_REPLCONF_OK:
                command = RespParser.parse(buffer);
                if ("ok".equalsIgnoreCase(command.getFirst())) {
                    String replconf = "*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n";
                    masterChannel.write(ByteBuffer.wrap(replconf.getBytes()));
                    state = stateSlave.WAITING_FOR_CAPA_OK;
                }
                break;

            case WAITING_FOR_CAPA_OK:
                command = RespParser.parse(buffer);
                if ("ok".equalsIgnoreCase(command.getFirst())) {
                    String psync = "*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n";
                    masterChannel.write(ByteBuffer.wrap(psync.getBytes()));
                    state = stateSlave.WAITING_FOR_FULLRESYNC;
                }
                break;

            case WAITING_FOR_FULLRESYNC:
                command = RespParser.parse(buffer);
                state = stateSlave.WAITING_FOR_RDB_HEADER;
                break;

            case WAITING_FOR_RDB_HEADER:
                rdbBytesLeft = RespParser.parseRDBHeader(buffer);
                rdb = new byte[rdbBytesLeft];
                rdbBytesRead = 0;
                state = stateSlave.WAITING_FOR_RDB;
                break;

            case WAITING_FOR_RDB:
                int bytes = Math.min(buffer.remaining(), rdbBytesLeft);
                buffer.get(rdb, rdbBytesRead, bytes);
                rdbBytesRead += bytes;
                rdbBytesLeft -= bytes;
                if (rdbBytesLeft == 0) {
                    state = stateSlave.REPLICATING;
                    buffer.position(buffer.limit());
                    System.out.println("Connected to Master");
                }
                break;

            case REPLICATING:
                command = RespParser.parse(buffer);
                HandleCommand.handleCommand(command, masterChannel, server, null);
                break;

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
