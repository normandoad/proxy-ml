package ar.com.mercadolibre.proxy.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class ProxyUtils {

	private ProxyUtils() {
	}

	public static ServerSocket getSSLServerSocket(Integer port) throws Exception {

		InetSocketAddress address = new InetSocketAddress("0.0.0.0", port);

		// Backlog is the maximum number of pending connections on the socket,
		// 0 means that an implementation-specific default is used
		int backlog = 0;
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		URL resource = classloader.getResource("keystore.jks");
		Path keyStorePath = Paths.get(resource.toURI());
		char[] keyStorePassword = "pass_for_self_signed_cert".toCharArray();

//	    // Bind the socket to the given port and address
		ServerSocket serverSocket = getSslContext(keyStorePath, keyStorePassword).getServerSocketFactory()
				.createServerSocket(address.getPort(), backlog, address.getAddress());

//	     We don't need the password anymore → Overwrite it
		Arrays.fill(keyStorePassword, '0');

		return serverSocket;

	}

	private static SSLContext getSslContext(Path keyStorePath, char[] keyStorePass) throws Exception {

		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStorePath.toFile()), keyStorePass);

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, keyStorePass);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		// Null means using default implementations for TrustManager and SecureRandom
		sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
		return sslContext;
	}

}
