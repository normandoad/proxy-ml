package ar.com.mercadolibre.proxy.controller.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ar.com.mercadolibre.commons.locators.ServiceLocator;
import ar.com.mercadolibre.commons.model.Proxy;
import ar.com.mercadolibre.proxy.handlers.RequestHandler;

@Component
@Qualifier("ProxyService")
public class ProxyService {

	private static final Logger log = LogManager.getLogger(ProxyService.class);
	
	private Boolean running = Boolean.FALSE;

	private ServerSocket serverSocket;
	
	private ExecutorService serverSocketThread;
	
	private final Integer maxThreadPool=50000;
	private final Long keepAliveTime=7L;
	
	private Proxy proxy=new Proxy();
	
	private Integer port;

	public String listen(Integer port) {

		String message = "service iniciated";
		try {
			if (!running) {
				
				Date date=new Date();
				
				this.port=port;
				
				proxy.setInitDate(date);
				proxy.setPort(port);
				List<Proxy>proxys=ServiceLocator.getDataBaseService().findByEndDateIsNull();
				
				proxys.forEach(proxy2->{proxy2.setEndDate(date);proxy2.setException("unknown shutdown");});
				proxys.add(proxy);
				ServiceLocator.getDataBaseService().saveAllProxy(proxys);
				
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
		this.proxy.setEndDate(new Date());
		this.proxy.setException("stopped by de user");
		ServiceLocator.getDataBaseService().saveProxy(proxy);
		return message;
	}

	private void run() {
		Thread.currentThread().setName("ProxyService-Thread");
		ThreadPoolExecutor thread = new ThreadPoolExecutor(0, maxThreadPool,keepAliveTime, TimeUnit.SECONDS,new SynchronousQueue<Runnable>());
		Integer counter=0;
		do {
			try {
				// serverSocket.accpet() Blocks until a connection is made
				if(serverSocket.isClosed())
					serverSocket = new ServerSocket(this.port);
				Socket socket = serverSocket.accept();
					thread.submit(new RequestHandler(socket,counter,proxy.getId()));
				

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
