import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Main {
	public static void main(String[] args){
		System.out.println("Logs from your program will appear here!");
		int port = 6379;
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
						handleRead(key);
					}
					
				}
			}
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
	}

	//accept client connections and register socket with selector
	private static void handleAccept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
		SocketChannel clientChannel = serverChannel.accept();
		clientChannel.configureBlocking(false);
		clientChannel.register(selector, SelectionKey.OP_READ);
	}

	//read incoming data from client
	private static void handleRead(SelectionKey key) throws IOException{
		SocketChannel clientChannel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = clientChannel.read(buffer);

		if (bytesRead == -1){
			clientChannel.close();
		}

		buffer.flip();
		List<String> command = RespParser.parse(buffer);

		if (command.get(0).equalsIgnoreCase("ECHO")){
			String msg = command.get(1);
			String response = "$"+msg.length()+"\r\n"+msg+"\r\n";
			clientChannel.write(ByteBuffer.wrap(response.getBytes()));
		}

		else if(command.get(0).equalsIgnoreCase("PING")){
			String response = "+PONG\r\n";
			clientChannel.write(ByteBuffer.wrap(response.getBytes()));
		}
	}
}