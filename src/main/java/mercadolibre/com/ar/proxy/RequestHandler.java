package mercadolibre.com.ar.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mercadolibre.com.ar.proxy.controller.serviceslocators.ServiceLocator;
import mercadolibre.com.ar.proxy.model.Client;
import mercadolibre.com.ar.proxy.model.Query;

public class RequestHandler implements Runnable {

	private static final Logger log = LogManager.getLogger(RequestHandler.class);

	private Socket clientSocket;

	private static final String MERCADOLIBREAPIURL = "https://api.mercadolibre.com";

	private Client cliente;// se guarda todo con el cliente
	private Query query = new Query();

	public RequestHandler(final Socket socket, final Integer counter, final UUID idProxy) {

		this.clientSocket = socket;
		try {
			this.clientSocket.setSoTimeout(2000);

			String ip = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();

			ip = StringUtils.equals("127.0.0.1", ip) ? "0:0:0:0:0:0:0:1" : ip;// ipv6

			this.query.setInitDate(new Date());
			this.cliente = ServiceLocator.getDataBaseService().findClienteByIpAndIdProxy(ip,idProxy);
			if (this.cliente == null) {
				this.cliente = new Client();
				this.cliente.setIp(ip);
				this.cliente.setIdProxy(idProxy);
			}

		} catch (IOException e) {
			log.error(e);
		}

		Thread.currentThread().setName("RequestHandler-" + counter);
	}

	@Override
	public void run() {

		Boolean queryPP = Boolean.FALSE;

		try {

			BufferedReader proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			BufferedWriter proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

			String requestString = proxyToClientBr.readLine();
			if (StringUtils.isNotBlank(requestString)) {
				log.debug("Reuest Received " + requestString);
				// Get the Request type

				String[] requestSplitted = StringUtils.split(requestString, " ");
				this.query.setQueryPath(requestSplitted[1]);
				this.query.setInitDateMeliRequest(new Date());

				queryPP = !StringUtils.containsIgnoreCase("/favicon.ico", requestSplitted[1]);

				if (queryPP) {
					String response = this.executeHttp(requestSplitted[0],
							MERCADOLIBREAPIURL.concat(requestSplitted[1]), requestSplitted[2]);
//				String response = this.execWithCurl(requestSplitted[0],MERCADOLIBREAPIURL.concat(requestSplitted[1]),requestSplitted[2]);

					this.query.setEndDateMeliRequest(new Date());
					proxyToClientBw.write(response);
					proxyToClientBw.flush();
					proxyToClientBw.close();
				}

			}

		} catch (IOException e) {
			log.error(e);
		} finally {
			if (queryPP) {

				Executors.newSingleThreadExecutor().execute(() -> {
					this.query.setEndDate(new Date());
					ServiceLocator.getDataBaseService().saveClient(cliente);
					query.setIdClient(cliente.getId());
					ServiceLocator.getDataBaseService().saveQuery(query);
				});
			}
		}
	}

	@SuppressWarnings("unused")
	private String executeHttp(final String method, final String httpURL, final String httpVer) {

		StringBuilder response = new StringBuilder();

		try {
			HttpsURLConnection conn = (HttpsURLConnection) new URL(httpURL).openConnection();
			conn.setRequestMethod(method);
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Authorization", "$ACCESS_TOKEN");
			conn.setDoOutput(true);

			if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
					&& conn.getResponseCode() <= HttpURLConnection.HTTP_ACCEPTED) {
				response.append(
						httpVer + " " + conn.getResponseCode() + " OK\n" + "Proxy-agent: ProxyService/1.0\n" + "\r\n");

				try (BufferedReader proxyToServerBr = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), "utf-8"))) {
					String responseLine;
					while ((responseLine = proxyToServerBr.readLine()) != null) {
						response.append(responseLine.trim());
					}

				}
			}

			conn.disconnect();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			response.append(httpVer + " " + HttpURLConnection.HTTP_BAD_REQUEST + " error\n"
					+ "Proxy-agent: ProxyService/1.0\n" + "\r\n");
			response.append(e.getMessage());
			log.info(e);
		} finally {
			if (StringUtils.isNoneEmpty(response.toString()))
				response.append(httpVer + " " + HttpURLConnection.HTTP_NO_CONTENT + " no content\n"
						+ "Proxy-agent: ProxyService/1.0\n" + "\r\n");
		}

		return response.toString();
	}

	@SuppressWarnings("unused")
	private String execWithCurl(final String method, final String httpURL, final String httpVer) {

		StringBuilder response = new StringBuilder();
		response.append(
				httpVer + " " + HttpsURLConnection.HTTP_OK + " OK\n" + "Proxy-agent: ProxyService/1.0\n" + "\r\n");

		try {
			String[] cmds = StringUtils.split("curl -X " + method
					+ "-H 'Accept: application/json' -H 'Content-Type: application/json' -H 'Authorization: Bearer $ACCESS_TOKEN' "
					+ httpURL, " ");

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new ProcessBuilder(cmds).start().getInputStream()));

			String responseLine;
			while ((responseLine = reader.readLine()) != null) {
				response.append(responseLine.trim());
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			response.append(httpVer + " " + HttpURLConnection.HTTP_BAD_REQUEST + " error\n"
					+ "Proxy-agent: ProxyService/1.0\n" + "\r\n");
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			response.append(httpVer + " " + HttpURLConnection.HTTP_BAD_REQUEST + " error\n"
					+ "Proxy-agent: ProxyService/1.0\n" + "\r\n");
			log.error(e);
		} finally {
			if (StringUtils.isNoneEmpty(response.toString()))
				response.append(httpVer + " " + HttpURLConnection.HTTP_NO_CONTENT + " no content\n"
						+ "Proxy-agent: ProxyService/1.0\n" + "\r\n");
		}
		return response.toString();
	}
}
