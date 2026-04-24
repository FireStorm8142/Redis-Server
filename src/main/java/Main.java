import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Main {
	public static void main(String[] args){
		System.out.println("Logs from your program will appear here!");
		int port = 6379;
		HashMap<String, String> storage = new HashMap<>();
		HashMap<String, Long> expiry = new HashMap<>();
		HashMap<String, List<String>> listStorage = new HashMap<>();
		HashMap<String, Queue<SocketChannel>> waitingClients = new HashMap<>();
		try {
			Selector selector = Selector.open();
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(port));
			serverChannel.configureBlocking(false);
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			while (true){
				selector.select();
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterate = selectedKeys.iterator();
				while (iterate.hasNext()) {
					SelectionKey key =  iterate.next();
					iterate.remove();
					if (key.isAcceptable()){
						handleAccept(serverChannel, selector);
					} else if(key.isReadable()){
						handleRead(key, storage, expiry, listStorage, waitingClients);
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

	private static void handleRead(SelectionKey key, HashMap<String, String> storage, HashMap<String, Long> expiry, HashMap<String, List<String>> listStorage, HashMap<String, Queue<SocketChannel>> waitingClients) throws IOException{
		SocketChannel clientChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int bytesRead = clientChannel.read(buffer);

		if (bytesRead == -1){
			clientChannel.close();
			return;
		}

		buffer.flip();
		List<String> command = RespParser.parse(buffer);

		String response = HandleCommand.handleCommand(command, storage, expiry, listStorage, waitingClients, clientChannel);
		if (response != null){
			clientChannel.write(ByteBuffer.wrap(response.getBytes()));
		}
	}
}