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
import ar.com.mercadolibre.proxy.utils.ProxyUtils;

@Component
@Qualifier("ProxyService")
public class ProxyService {

	private static final Logger log = LogManager.getLogger(ProxyService.class);

	private Boolean running = Boolean.FALSE;

	private ServerSocket serverSocket;
	private ServerSocket sslServerSocket;

	private ExecutorService serverSocketThread;
	private ExecutorService serverSocketSslThread;

	private Proxy proxy = new Proxy();
	private Proxy sslProxy = new Proxy();

	private Integer port;

	public String listen(Integer port) {

		String message = ProxyPropertiesLocator.getStringProperties().getInitiated();
		try {
			if (!running) {

				Date date = new Date();

				this.port = port;

				proxy.setInitDate(date);
				sslProxy.setInitDate(date);
				proxy.setPort(port);
				sslProxy.setPort(port + 1);
				List<Proxy> proxys = ServiceLocator.getDataBaseService().findByEndDateIsNull();

				proxys.forEach(proxy2 -> {
					proxy2.setEndDate(date);
					proxy2.setException(ProxyPropertiesLocator.getStringProperties().getUnknownShutdown());
				});
				proxys.add(proxy);
				proxys.add(sslProxy);
				ServiceLocator.getDataBaseService().saveAllProxy(proxys);

				serverSocket = new ServerSocket(port);
				sslServerSocket = ProxyUtils.getSSLServerSocket(port + 1);

				log.info(ProxyPropertiesLocator.getStringProperties().getWaiting() + " " + serverSocket.getLocalPort()
						+ "..");

				running = Boolean.TRUE;
				serverSocketThread = Executors.newSingleThreadExecutor();
				serverSocketThread.execute(() -> {
					this.run();
				});

				serverSocketSslThread = Executors.newSingleThreadExecutor();
				serverSocketSslThread.execute(() -> {
					this.sslRun();
				});

			} else
				message = ProxyPropertiesLocator.getStringProperties().getAlreadyIniciated();
		} catch (Exception e) {
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
				sslServerSocket.close();
				running = Boolean.FALSE;
			} else
				message = ProxyPropertiesLocator.getStringProperties().getServiceIsAlreadyStoped();

		} catch (IOException e) {
			log.error(e);
			message = e.getMessage();
		}
		serverSocketThread.shutdownNow();
		this.proxy.setEndDate(new Date());
		this.sslProxy.setEndDate(new Date());
		this.sslProxy.setException(ProxyPropertiesLocator.getStringProperties().getStoppedByTheUser());
		this.proxy.setException(ProxyPropertiesLocator.getStringProperties().getStoppedByTheUser());
		ServiceLocator.getDataBaseService().saveProxy(proxy);
		ServiceLocator.getDataBaseService().saveProxy(sslProxy);
		return message;
	}

	private void run() {
		Thread.currentThread().setName(ProxyPropertiesLocator.getStringProperties().getProxyServiceThread());
		ThreadPoolExecutor thread = new ThreadPoolExecutor(0, ProxyPropertiesLocator.getProperties().getMaxThreadPool(),
				ProxyPropertiesLocator.getProperties().getKeepAliveTime(), TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());

		Integer counter = 0;
		do {
			try {
				// serverSocket.accpet() Blocks until a connection is made
				if (serverSocket.isClosed())
					serverSocket = new ServerSocket(this.port);
				Socket socket = serverSocket.accept();
				Socket sslSocket = sslServerSocket.accept();
				thread.submit(new RequestHandler(socket, counter, proxy.getId(), thread.getPoolSize()));
				counter++;
				thread.submit(new RequestHandler(sslSocket, counter, sslProxy.getId(), thread.getPoolSize()));

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

	private void sslRun() {
		Thread.currentThread().setName(ProxyPropertiesLocator.getStringProperties().getProxyServiceThread());
		ThreadPoolExecutor thread = new ThreadPoolExecutor(0, ProxyPropertiesLocator.getProperties().getMaxThreadPool(),
				ProxyPropertiesLocator.getProperties().getKeepAliveTime(), TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>());

		Integer counter = 0;
		do {
			try {
				// serverSocket.accpet() Blocks until a connection is made
				if (sslServerSocket.isClosed())
					sslServerSocket = ProxyUtils.getSSLServerSocket(port + 1);
				Socket sslSocket = sslServerSocket.accept();
				thread.submit(new RequestHandler(sslSocket, counter, sslProxy.getId(), thread.getPoolSize()));

			} catch (SocketException e) {
				// Socket exception is triggered by management system to shut down the proxy
				log.info(ProxyPropertiesLocator.getStringProperties().getServerClosed());
			} catch (IOException e) {
				log.error(e);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
			if (counter > ProxyPropertiesLocator.getProperties().getMaxThreadPool())
				counter = -1;
			counter++;
		} while (this.running);

		thread.shutdownNow();
	}
}
