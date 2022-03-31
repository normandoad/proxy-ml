package ar.com.mercadolibre.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class Prueba {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		InetSocketAddress address = new InetSocketAddress("0.0.0.0", 8443);

		startSingleThreaded(address);
	}
	
	public static void startSingleThreaded(InetSocketAddress address) {

	    System.out.println("Start single-threaded server at " + address);

	    try (ServerSocket serverSocket = getServerSocket(address)) {

	        // This infinite loop is not CPU-intensive since method "accept" blocks
	        // until a client has made a connection to the socket
	        while (true) {
	            try (Socket socket = serverSocket.accept();
	                 // Use the socket to read the client's request
	            		BufferedReader reader = new BufferedReader(new InputStreamReader(
	                         socket.getInputStream(), StandardCharsets.UTF_8.name()));
	                 // Writing to the output stream and then closing it sends
	                 // data to the client
	            		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
	                         socket.getOutputStream(), StandardCharsets.UTF_8.name()))
	            ) {
	                getHeaderLines(reader).forEach(System.out::println);

	                writer.write(getResponse(StandardCharsets.UTF_8));
	                writer.flush();

	            } catch (IOException e) {
	                System.err.println("Exception while handling connection");
	                e.printStackTrace();
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Could not create socket at " + address);
	        e.printStackTrace();
	    }
	}

	private static ServerSocket getServerSocket(InetSocketAddress address)
	        throws Exception {

	    // Backlog is the maximum number of pending connections on the socket,
	    // 0 means that an implementation-specific default is used
	    int backlog = 0;
	    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
	    URL resource = classloader.getResource("keystore.jks");
	    Path keyStorePath = Paths.get(resource.toURI());
	    char[] keyStorePassword = "pass_for_self_signed_cert".toCharArray();

//	    // Bind the socket to the given port and address
	    ServerSocket serverSocket = getSslContext(keyStorePath, keyStorePassword)
	            .getServerSocketFactory()
	            .createServerSocket(address.getPort(), backlog, address.getAddress());

//	     We don't need the password anymore → Overwrite it
	    Arrays.fill(keyStorePassword, '0');

	    return serverSocket;
	  
	}

	private static SSLContext getSslContext(Path keyStorePath, char[] keyStorePass)
	        throws Exception {

		KeyStore  keyStore = KeyStore.getInstance("JKS");
	    keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);

	    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
	    keyManagerFactory.init(keyStore, keyStorePass);

	    SSLContext  sslContext = SSLContext.getInstance("TLS");
	    // Null means using default implementations for TrustManager and SecureRandom
	    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
	    return sslContext;
	}

	private static String getResponse(Charset encoding) {
	    String body = "The server says hi 👋\r\n";
	    Integer contentLength = body.getBytes(encoding).length;

	    return "HTTP/1.1 200 OK\r\n" +
	            String.format("Content-Length: %d\r\n", contentLength) +
	            String.format("Content-Type: text/plain; charset=%s\r\n",
	                    encoding.displayName()) +
	            // An empty line marks the end of the response's header
	            "\r\n" +
	            body;
	}

	private static List<String> getHeaderLines(BufferedReader reader)
	        throws IOException {
	    List<String> lines = new ArrayList<String>();
	    String line = reader.readLine();
	    // An empty line marks the end of the request's header
	    while (!line.isEmpty()) {
	        lines.add(line);
	        line = reader.readLine();
	    }
	    return lines;
	}
}
