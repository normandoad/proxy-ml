package mercadolibre.com.ar.proxy.controller.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import mercadolibre.com.ar.proxy.RequestHandler;
import mercadolibre.com.ar.proxy.model.Proxy;

@Component
@Qualifier("ProxyService")
public class ProxyService {

	private static final Logger log = LogManager.getLogger(ProxyService.class);
	
	@Autowired
	private EstadisticaService estadisticaService;

	private Boolean running = Boolean.FALSE;

	private ServerSocket serverSocket;
	
	private ExecutorService serverSocketThread;
	
	private final Integer maxThreadPool=100000;
	
	private Proxy proxy=new Proxy();
	

	public String listen(Integer port) {

		String message = "service iniciated";
		try {
			if (!running) {
				
				Date date=new Date();
				
				proxy.setFechaEncendido(new Date());
				proxy.setPuerto(port);
				List<Proxy>proxys=estadisticaService.findProxyByFechaApagadoIsNull();
				
				proxys.forEach(proxy2->{proxy2.setFechaApagado(date);proxy2.setExcepcion("unknown shutdown");});
				
//				proxys.add(proxy);
				estadisticaService.saveAllProxy(proxys);
				
				serverSocket = new ServerSocket(port);

				log.info("Waiting for client on port " + serverSocket.getLocalPort() + "..");

				running = Boolean.TRUE;
				serverSocketThread=Executors.newSingleThreadExecutor();
				serverSocketThread.execute(() -> {
					this.run();
				});

			} else
				message = "The service is already iniciated";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.info(e);
			message = e.getMessage();
		}
		return message;
	}

	public String stopListen() {
		String message = "The service is stoped";
		try {
			if (running) {
				serverSocket.close();
				running = Boolean.FALSE;
			} else
				message = "The service is already stoped";

		} catch (IOException e) {
			log.error(e);
			message = e.getMessage();
		}
		serverSocketThread.shutdownNow();
		return message;
	}

	private void run() {
		Thread.currentThread().setName("ProxyService-Thread");
		ExecutorService thread = Executors.newFixedThreadPool(maxThreadPool);
		Integer counter=0;
		do {
			try {
				// serverSocket.accpet() Blocks until a connection is made
				Socket socket = serverSocket.accept();
				
				thread.submit(new RequestHandler(socket,counter,proxy));

			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy
				log.info("Server closed");
			} catch (IOException e) {
				log.error(e);
			}
			if(counter>maxThreadPool)
				counter=-1;
			counter++;
		} while (this.running);

		thread.shutdownNow();
	}
}
