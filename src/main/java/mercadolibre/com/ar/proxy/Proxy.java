package mercadolibre.com.ar.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Proxy implements Runnable {

	private static final Logger log = LogManager.getLogger(Proxy.class);

	private ServerSocket serverSocket;

	private volatile Boolean running;

	private List<Thread> servicingThreads = new ArrayList<Thread>();

	public Proxy(final Integer port, Boolean running) {

		new Thread(this).start(); // Starts overriden run() method at bottom
		try {
			// Create the Server Socket for the Proxy
			serverSocket = new ServerSocket(port);

			// Set the timeout
			// serverSocket.setSoTimeout(100000); // debug
			log.info("Waiting for client on port " + serverSocket.getLocalPort() + "..");
			this.running = running;
		}

		// Catch exceptions associated with opening socket
		catch (SocketException se) {
			log.error("Socket Exception when connecting to client");
			log.error(se);
		} catch (SocketTimeoutException ste) {
			log.info("Timeout occured while connecting to client");
		} catch (IOException io) {
			log.error("IO exception when connecting to client");
		}
	}

	public void listen() {

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
			do {
				try {
					ExecutorService removeDeathThreads = Executors.newSingleThreadExecutor();
					// serverSocket.accpet() Blocks until a connection is made
					Socket socket = serverSocket.accept();

					// Create new Thread and pass it Runnable RequestHandler
					Thread thread = new Thread(new RequestHandler(socket));
					// Key a reference to each thread so they can be joined later if necessary
					servicingThreads.add(thread);
					// thread.start();
					thread.start();
					removeDeathThreads.execute(() -> {this.removeDeathThreads();});
				} catch (SocketException e) {
					// Socket exception is triggered by management system to shut down the proxy
					log.info("Server closed");
				} catch (IOException e) {
					log.error(e);
				}
			}while (this.running);
			
			if (servicingThreads.size() > 0)
				log.info("can't remove " + servicingThreads.size() + " threads from servicingThreads");
			executorService.shutdown();
		});
	}

	public void closeServer() {
		log.info("Closing Server..");

		this.running = Boolean.FALSE;

		try {
			// Close all servicing threads
			for (Thread thread : servicingThreads) {
				if (thread.isAlive()) {
					log.info("Waiting on " + thread.getId() + " to close..");
					thread.join();
					log.info(" closed");
				}
			}
			this.removeDeathThreads();

		} catch (InterruptedException e) {
			log.error(e);
		}

		// Close Server Socket
		try {
			log.info("Terminating Connection");
			serverSocket.close();
		} catch (Exception e) {
			log.error("Exception closing proxy's server socket");
			log.error(e);
		}

	}

	private void removeDeathThreads() {
		log.info("Removing death threads");
		ArrayList<Thread> also = new ArrayList<Thread>();
		// Close all servicing threads
		for (Thread thread : servicingThreads) {
			if (!thread.isAlive())
				also.add(thread);
		}
		servicingThreads.removeAll(also);
	}

	@Override
	public void run() {}

}
