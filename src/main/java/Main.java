import replication.ReplicationMaster;
import replication.ReplicationSlave;
import server.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.*;

public class Main {
	private static String role = "master";
	private static String masterHost;
	private static int masterPort;
	private static ReplicationMaster replicationMaster;
	private static ReplicationSlave replicationSlave;

	public static void main(String[] args){
		int port = 6379;
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("--port")) {
				if (i+1 < args.length) {
					port = Integer.parseInt(args[i+1]);
					i++;
				}
			}
			else if (args[i].equals("--replicaof")) {
				role = "slave";
				masterHost = args[i+1];
				masterPort = Integer.parseInt(args[i+2]);
				i+=2;
			}
		}

		//Initialize server state
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[20];
		random.nextBytes(bytes);
		String uuid = HexFormat.of().formatHex(bytes);
		Server server = new Server(role, port, uuid, "0", masterHost, masterPort);
		System.out.println("Server running on port : " + port);

		try {
			Selector selector = Selector.open();
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			if (role.equals("master")) replicationMaster = new ReplicationMaster();
			else replicationSlave = new ReplicationSlave(server, selector);

			while (true){
				selector.select(100);

				//checks for expired clients every cycle and removes them
				long now = System.currentTimeMillis();
				for (Queue<WaitingClients> queue : server.getWaitingClients().values()) {
					while (queue != null && !queue.isEmpty()) {
						WaitingClients wc = queue.peek();
						if (wc.expiry == Long.MAX_VALUE || wc.expiry > now) break;
						queue.poll();
						wc.client.write(ByteBuffer.wrap("*-1\r\n".getBytes()));
					}
				}

				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterate = selectedKeys.iterator();
				while (iterate.hasNext()) {
					SelectionKey key =  iterate.next();
					iterate.remove();
					if (key.isAcceptable()){
						handleAccept(serverChannel, selector);
					} else if(key.isReadable()){
						handleRead(key, server);
					} else if (key.isConnectable()) {
						replicationSlave.finishConnectToMaster(key);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
	}

	private static void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
		SocketChannel clientChannel = serverChannel.accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(selector, SelectionKey.OP_READ);
	}

	private static void handleRead(SelectionKey key, Server server) throws IOException{
		SocketChannel clientChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int bytesRead = clientChannel.read(buffer);

		if (bytesRead == -1){
			clientChannel.close();
			return;
		}

		buffer.flip();
		List<String> command = RespParser.parse(buffer);

		if ("master".equals(key.attachment())) {
			replicationSlave.processResponse(key, command, server);
			return;
		}

		if ("slave".equals(key.attachment())) {
			replicationMaster.processRequest(key, command, server);
			return;
		}

		if ("replconf".equalsIgnoreCase(command.getFirst()) && "master".equals(role)) {
			key.attach("slave");
			replicationMaster.processRequest(key, command, server);
			return;
		}

		String response = HandleCommand.handleCommand(command, clientChannel, server);
		if (response != null){
			clientChannel.write(ByteBuffer.wrap(response.getBytes()));
		}
	}
}