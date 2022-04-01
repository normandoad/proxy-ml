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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mercadolibre.com.ar.proxy.controller.serviceslocators.ServiceLocator;
import mercadolibre.com.ar.proxy.model.Client;
import mercadolibre.com.ar.proxy.model.Query;

@SuppressWarnings("static-access")
public class RequestHandler implements Runnable {

	private static final Logger log = LogManager.getLogger(RequestHandler.class);

	private static final String MERCADOLIBREAPIURL = "https://api.mercadolibre.com";

	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Integer> expirationPolicyIp = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			1, TimeUnit.SECONDS);

	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Boolean> expirationPolicyQueryPathAndIp = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			2, TimeUnit.SECONDS);
	
	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Integer> expirationPolicyQueryPath = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			5, TimeUnit.SECONDS);

	private static volatile Map<String, Integer> rateLimitByIp = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Integer>(expirationPolicyIp, new HashMap<String, Integer>()));

	private static volatile Map<String, Boolean> rateLimitByQueryPathAndIp = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Boolean>(expirationPolicyQueryPathAndIp, new HashMap<String, Boolean>()));
	
	private static volatile Map<String, Integer> rateLimitByQueryPath = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Integer>(expirationPolicyQueryPath, new HashMap<String, Integer>()));

	private Socket clientSocket;
	private Client cliente;// se guarda todo con el cliente
	private Query query = new Query();

	private String ip;

	public RequestHandler(final Socket socket, final Integer counter, final UUID idProxy) {

		this.clientSocket = socket;
		try {

			this.ip = ((InetSocketAddress) this.clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress();

			this.clientSocket.setSoTimeout(7000);

			this.query.setInitDate(new Date());
			this.cliente = ServiceLocator.getDataBaseService().findClienteByIpAndIdProxy(ip, idProxy);
			if (this.cliente == null) {
				this.cliente = new Client();
				this.cliente.setIp(ip);
				this.cliente.setIdProxy(idProxy);
			}

			Thread.currentThread().setName("RequestHandler-" + counter);

		} catch (IOException e) {
			log.error(e);
		}

	}

	@Override
	public void run() {

		Boolean favicon = Boolean.FALSE;

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

				favicon = !StringUtils.containsIgnoreCase("/favicon.ico", requestSplitted[1]);

				if (favicon) {
					this.rateLimitByIp();
					this.rateLimitByQueryPathAndIp(ip.concat(requestSplitted[1]));
					this.rateLimitByQueryPath(requestSplitted[1]);
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
			if (favicon) {

				Executors.newSingleThreadExecutor().execute(() -> {
					this.query.setEndDate(new Date());
					ServiceLocator.getDataBaseService().saveClient(cliente);
					query.setIdClient(cliente.getId());
					ServiceLocator.getDataBaseService().saveQuery(query);
				});
			}
		}
	}

	private void rateLimitByIp() {
		try {

			Integer count = rateLimitByIp.get(ip);
			count = count != null ? ++count : 0;
			rateLimitByIp.put(ip, count);
			log.debug("queryCantUser " + rateLimitByIp.size() + " count " + count);

			if (count > 0) {
				log.warn("Rate limit for: " + ip);
				Thread.currentThread().sleep(1000 + (count * 100));
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}

	private void rateLimitByQueryPathAndIp(final String queryPath) {
		try {

			Boolean isHere = rateLimitByQueryPathAndIp.get(queryPath);
			rateLimitByQueryPathAndIp.put(queryPath, Boolean.TRUE);
			log.debug("rateLimitByQueryPath " + rateLimitByQueryPathAndIp.size() + " queryPath " + queryPath);

			if (isHere != null && isHere) {
				log.warn("Rate limit for: " + this.ip + " by query: " + StringUtils.remove(queryPath, this.ip));
				Thread.currentThread().sleep(1500);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}
	}
	
	private void rateLimitByQueryPath(final String queryPath) {
		try {

			Integer count = rateLimitByQueryPath.get(queryPath);
			count = count != null ? ++count : 0;
			rateLimitByQueryPath.put(queryPath, count);
			log.debug("rateLimitByQueryPath size " + rateLimitByQueryPath.size() + " count " + count);

			if (count > 7) {
				log.warn("Rate limit by path "+queryPath);
				Thread.currentThread().sleep(1000 + (count * 100));
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			log.error(e);
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
			if (StringUtils.isEmpty(response.toString()))
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
			if (StringUtils.isEmpty(response.toString()))
				response.append(httpVer + " " + HttpURLConnection.HTTP_NO_CONTENT + " no content\n"
						+ "Proxy-agent: ProxyService/1.0\n" + "\r\n");
		}
		return response.toString();
	}
}
