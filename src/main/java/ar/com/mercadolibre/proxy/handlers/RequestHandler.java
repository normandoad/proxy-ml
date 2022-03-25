package ar.com.mercadolibre.proxy.handlers;

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

import ar.com.mercadolibre.commons.locators.ServiceLocator;
import ar.com.mercadolibre.commons.model.Client;
import ar.com.mercadolibre.commons.model.Query;
import ar.com.mercadolibre.proxy.locators.ProxyPropertiesLocator;

@SuppressWarnings("static-access")
public class RequestHandler implements Runnable {

	private static final Logger log = LogManager.getLogger(RequestHandler.class);

	private static final String MERCADOLIBREAPIURL = ProxyPropertiesLocator.getProperties().getMercadoLibreApiUrl();

	private static final Integer MAXTHREADPOOLSIZE = ProxyPropertiesLocator.getProperties().getMaxThreadPoolSize();

	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Integer> expirationPolicyIp = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			ProxyPropertiesLocator.getProperties().getExpirationPolicyIp(), TimeUnit.SECONDS);

	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Boolean> expirationPolicyQueryPathAndIp = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			ProxyPropertiesLocator.getProperties().getExpirationPolicyQueryPathAndIp(), TimeUnit.SECONDS);

	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Integer> expirationPolicyQueryPath = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			ProxyPropertiesLocator.getProperties().getExpirationPolicyQueryPath(), TimeUnit.SECONDS);

	private static final PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, Integer> expirationPolicyFlooding = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(
			ProxyPropertiesLocator.getProperties().getExpirationPolicyFlooding(), TimeUnit.MINUTES);

	private static volatile Map<String, Integer> rateLimitByIp = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Integer>(expirationPolicyIp, new HashMap<String, Integer>()));

	private static volatile Map<String, Boolean> rateLimitByQueryPathAndIp = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Boolean>(expirationPolicyQueryPathAndIp, new HashMap<String, Boolean>()));

	private static volatile Map<String, Integer> rateLimitByQueryPath = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Integer>(expirationPolicyQueryPath, new HashMap<String, Integer>()));

	private static volatile Map<String, Integer> flooding = Collections.synchronizedMap(
			new PassiveExpiringMap<String, Integer>(expirationPolicyFlooding, new HashMap<String, Integer>()));

	private Socket clientSocket;
	private Client cliente;// se guarda todo con el cliente
	private Query query = new Query();

	private String ip;
	private Integer poolSize;

	public RequestHandler(final Socket socket, final Integer counter, final UUID idProxy, final Integer poolSize) {

		this.clientSocket = socket;
		try {

			this.ip = ((InetSocketAddress) this.clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress();
			this.poolSize = poolSize;

			this.clientSocket.setSoTimeout(12000);

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

		if (this.flooding()) {
			Boolean favicon = Boolean.FALSE;
			try {
				BufferedReader proxyToClientBr = new BufferedReader(
						new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter proxyToClientBw = new BufferedWriter(
						new OutputStreamWriter(clientSocket.getOutputStream()));

				String requestString = proxyToClientBr.readLine();
				if (StringUtils.isNotBlank(requestString)) {
					log.debug("Reuest Received " + requestString);
					// Get the Request type

					String[] requestSplitted = StringUtils.split(requestString, " ");
					this.query.setQueryPath(requestSplitted[1]);
					this.query.setInitDateMeliRequest(new Date());

					favicon = !StringUtils.containsIgnoreCase(
							"/" + ProxyPropertiesLocator.getStringProperties().getFavicon(), requestSplitted[1]);

					if (favicon) {
						this.rateLimit(requestSplitted[1]);
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
	}

	private Boolean flooding() {
		try {

			Integer count = this.flooding.get(ip);

			if (count == null)
				count = this.rateLimitByIp.get(ip);

			if (count != null && count >= 10) {
				BufferedWriter proxyToClientBw = new BufferedWriter(
						new OutputStreamWriter(clientSocket.getOutputStream()));
				proxyToClientBw.write(ProxyPropertiesLocator.getStringProperties().getHttpVer() + " "
						+ HttpURLConnection.HTTP_NOT_ACCEPTABLE
						+ ProxyPropertiesLocator.getStringProperties().getError() + "\n"
						+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n" + " flooding!!! ");
				proxyToClientBw.flush();
				proxyToClientBw.close();

				this.flooding.put(this.ip, count);
				log.info("flooding by " + this.ip + " ip");
				log.debug("flooding " + this.flooding.size() + " count " + count);
				return Boolean.FALSE;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error(e);
		}

		return Boolean.TRUE;
	}

	private void rateLimit(final String httpURL) {
		this.rateLimitByIp();
		this.rateLimitByQueryPathAndIp(ip.concat(httpURL));
		this.rateLimitByQueryPath(httpURL);
	}

	private void rateLimitByIp() {
		if (this.poolSize > MAXTHREADPOOLSIZE) {
			try {

				Integer count = this.rateLimitByIp.get(ip);
				count = count != null ? ++count : 0;
				this.rateLimitByIp.put(this.ip, count);
				log.debug("queryCantUser " + this.rateLimitByIp.size() + " count " + count);

				if (count > 0) {
					log.warn("Rate limit for: " + this.ip);
					Thread.currentThread().sleep(1000 + (count * 100));
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				log.error(e);
			}
		}
	}

	private void rateLimitByQueryPathAndIp(final String queryPath) {
		try {

			Boolean isHere = this.rateLimitByQueryPathAndIp.get(queryPath);
			this.rateLimitByQueryPathAndIp.put(queryPath, Boolean.TRUE);
			log.debug("rateLimitByQueryPath " + this.rateLimitByQueryPathAndIp.size() + " queryPath " + queryPath);

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
				log.warn("Rate limit by path " + queryPath);
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
			conn.setRequestProperty(ProxyPropertiesLocator.getStringProperties().getContentType(),
					ProxyPropertiesLocator.getStringProperties().getApplicationJson() + "; utf-8");
			conn.setRequestProperty(ProxyPropertiesLocator.getStringProperties().getAccept(),
					ProxyPropertiesLocator.getStringProperties().getApplicationJson());
			conn.setRequestProperty(ProxyPropertiesLocator.getStringProperties().getAuthorization(), "$ACCESS_TOKEN");
			conn.setDoOutput(true);

			if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
					&& conn.getResponseCode() <= HttpURLConnection.HTTP_ACCEPTED) {
				response.append(
						httpVer + " " + conn.getResponseCode() + ProxyPropertiesLocator.getStringProperties().getOk()
								+ "\n" + ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");

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
			response.append(httpVer + " " + HttpURLConnection.HTTP_BAD_REQUEST
					+ ProxyPropertiesLocator.getStringProperties().getError() + "\n"
					+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");
			response.append(e.getMessage());
			log.info(e);
		} finally {
			if (StringUtils.isNoneEmpty(response.toString()))
				response.append(httpVer + " " + HttpURLConnection.HTTP_NO_CONTENT
						+ ProxyPropertiesLocator.getStringProperties().getNoContent() + "\n"
						+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");
		}

		return response.toString();
	}

	@SuppressWarnings("unused")
	private String execWithCurl(final String method, final String httpURL, final String httpVer) {

		StringBuilder response = new StringBuilder();
		response.append(
				httpVer + " " + HttpsURLConnection.HTTP_OK + ProxyPropertiesLocator.getStringProperties().getOk() + "\n"
						+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");

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
			response.append(httpVer + " " + HttpURLConnection.HTTP_BAD_REQUEST
					+ ProxyPropertiesLocator.getStringProperties().getError() + "\n"
					+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");
			log.error(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			response.append(httpVer + " " + HttpURLConnection.HTTP_BAD_REQUEST
					+ ProxyPropertiesLocator.getStringProperties().getError() + "\n"
					+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");
			log.error(e);
		} finally {
			if (StringUtils.isNoneEmpty(response.toString()))
				response.append(httpVer + " " + HttpURLConnection.HTTP_NO_CONTENT
						+ ProxyPropertiesLocator.getStringProperties().getNoContent() + "\n"
						+ ProxyPropertiesLocator.getStringProperties().getProxyAgent() + "\n\r\n");
		}
		return response.toString();
	}
}
