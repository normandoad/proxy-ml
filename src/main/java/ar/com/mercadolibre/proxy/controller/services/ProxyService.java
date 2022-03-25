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
import ar.com.mercadolibre.proxy.locators.ProxyPropertiesLocator;

@Component
@Qualifier("ProxyService")
public class ProxyService {

	private static final Logger log = LogManager.getLogger(ProxyService.class);

	private Boolean running = Boolean.FALSE;

	private ServerSocket serverSocket;

	private ExecutorService serverSocketThread;

	private Proxy proxy = new Proxy();

	private Integer port;

	public String listen(Integer port) {

		String message = ProxyPropertiesLocator.getStringProperties().getInitiated();
		try {
			if (!running) {

				Date date = new Date();

				this.port = port;

				proxy.setInitDate(date);
				proxy.setPort(port);
				List<Proxy> proxys = ServiceLocator.getDataBaseService().findByEndDateIsNull();

				proxys.forEach(proxy2 -> {
					proxy2.setEndDate(date);
					proxy2.setException(ProxyPropertiesLocator.getStringProperties().getUnknownShutdown());
				});
				proxys.add(proxy);
				ServiceLocator.getDataBaseService().saveAllProxy(proxys);

				serverSocket = new ServerSocket(port);

				log.info(ProxyPropertiesLocator.getStringProperties().getWaiting() + " " + serverSocket.getLocalPort() + "..");

				running = Boolean.TRUE;
				serverSocketThread = Executors.newSingleThreadExecutor();
				serverSocketThread.execute(() -> {
					this.run();
				});

			} else
				message = ProxyPropertiesLocator.getStringProperties().getAlreadyIniciated();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.info(e);
			message = e.getMessage();
		}
		return message;
	}

	public String stopListen() {
		String message = ProxyPropertiesLocator.getStringProperties().getServiceIsStoped();
		try {
			if (running) {
				serverSocket.close();
				running = Boolean.FALSE;
			} else
				message = ProxyPropertiesLocator.getStringProperties().getServiceIsAlreadyStoped();

		} catch (IOException e) {
			log.error(e);
			message = e.getMessage();
		}
		serverSocketThread.shutdownNow();
		this.proxy.setEndDate(new Date());
		this.proxy.setException(ProxyPropertiesLocator.getStringProperties().getStoppedByTheUser());
		ServiceLocator.getDataBaseService().saveProxy(proxy);
		return message;
	}

	private void run() {
		Thread.currentThread().setName(ProxyPropertiesLocator.getStringProperties().getProxyServiceThread());
		ThreadPoolExecutor thread = new ThreadPoolExecutor(0, ProxyPropertiesLocator.getProperties().getMaxThreadPool(),
				ProxyPropertiesLocator.getProperties().getKeepAliveTime(), TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		Integer counter = 0;
		do {
			try {
				// serverSocket.accpet() Blocks until a connection is made
				if (serverSocket.isClosed())
					serverSocket = new ServerSocket(this.port);
				Socket socket = serverSocket.accept();
				thread.submit(new RequestHandler(socket, counter, proxy.getId(), thread.getPoolSize()));

			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy
				log.info(ProxyPropertiesLocator.getStringProperties().getServerClosed());
			} catch (IOException e) {
				log.error(e);
			}
			if (counter > ProxyPropertiesLocator.getProperties().getMaxThreadPool())
				counter = -1;
			counter++;
		} while (this.running);

		thread.shutdownNow();
	}
}
