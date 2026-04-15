import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Main {
	public static void main(String[] args){
		System.out.println("Logs from your program will appear here!");
		int port = 6379;
		HashMap<String, String> storage = new HashMap<>();
		HashMap<String, Long> expiry = new HashMap<>();
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
						handleRead(key, storage, expiry);
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

	private static void handleRead(SelectionKey key, HashMap<String, String> storage, HashMap<String, Long> expiry) throws IOException{
		SocketChannel clientChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

		if (bytesRead == -1){
			clientChannel.close();
			return;
		}

		buffer.flip();
		List<String> command = RespParser.parse(buffer);

		String cmd = command.get(0).toUpperCase();
		String response;
		switch(cmd){
			case "ECHO":
				String msg = command.get(1);
				response = "$"+msg.length()+"\r\n"+msg+"\r\n";
				clientChannel.write(ByteBuffer.wrap(response.getBytes()));
				break;

			case "PING":
				response = "+PONG\r\n";
				clientChannel.write(ByteBuffer.wrap(response.getBytes()));
				break;

			case "GET":
				long now = System.currentTimeMillis();
				if (expiry.getOrDefault(command.get(1), Long.MAX_VALUE) < now){
						expiry.remove(command.get(1));
						storage.remove(command.get(1));
				}
				String value = storage.getOrDefault(command.get(1), null);
				String resp;
				if (value != null) {
					resp = "$"+value.length()+"\r\n"+value+"\r\n";
				}else{
					resp = "$-1\r\n";
				}
				clientChannel.write(ByteBuffer.wrap(resp.getBytes()));
				break;

			case "SET":
				if (command.size() > 3){
					if (command.get(3).equalsIgnoreCase("PX")) {
						expiry.put(command.get(1), System.currentTimeMillis() + Long.parseLong(command.get(4)));
					}
					else{
						expiry.put(command.get(1), System.currentTimeMillis() + Long.parseLong(command.get(4))*1000);
					}
				}
				storage.put(command.get(1), command.get(2));
				clientChannel.write(ByteBuffer.wrap(("+OK\r\n").getBytes()));
				break;
			default:
				break;
		}
	}
}