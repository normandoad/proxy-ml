package mercadolibre.com.ar.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mercadolibre.com.ar.proxy.controller.serviceslocators.ServiceLocator;
import mercadolibre.com.ar.proxy.model.Cliente;
import mercadolibre.com.ar.proxy.model.Consulta;
import mercadolibre.com.ar.proxy.model.Proxy;

public class RequestHandler implements Runnable {

	private static final Logger log = LogManager.getLogger(RequestHandler.class);

	private Socket clientSocket;

	private static final String MERCADOLIBREAPIURL = "https://api.mercadolibre.com";

	private Cliente cliente;// se guarda todo con el cliente
	private Consulta consulta = new Consulta();

	public RequestHandler(final Socket socket,final Integer counter,final Proxy proxy) {

		this.clientSocket = socket;
		try {
			this.clientSocket.setSoTimeout(2000);

			String ip = this.clientSocket.getRemoteSocketAddress().toString();

			this.consulta.setFechaInicio(new Date());
			this.cliente = ServiceLocator.getEstadisticaService().findClienteByIp(ip);
			if (this.cliente == null) {
				this.cliente = new Cliente();
				this.cliente.setIp(ip);
				this.cliente.setProxys(new HashSet<Proxy>());
				this.cliente.getProxys().add(proxy);
			}

		} catch (IOException e) {
			log.error(e);
		}

		Thread.currentThread().setName("RequestHandler-" + counter);
	}

	@Override
	public void run() {

		try {

			BufferedReader proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			BufferedWriter proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

			String requestString = proxyToClientBr.readLine();
			if (StringUtils.isNotBlank(requestString)) {
				log.debug("Reuest Received " + requestString);
				// Get the Request type

				String[] requestSplitted = StringUtils.split(requestString, " ");
				String response = this.executeHttp(requestSplitted[0], MERCADOLIBREAPIURL.concat(requestSplitted[1]),
						requestSplitted[2]);
//				String response = this.execWithCurl(requestSplitted[0],MERCADOLIBREAPIURL.concat(requestSplitted[1]),requestSplitted[2]);

				proxyToClientBw.write(response);
				proxyToClientBw.flush();
				proxyToClientBw.close();

			}

		} catch (IOException e) {
			log.error(e);
		} finally {
//			cliente.setId(UUID.randomUUID().toString());
			this.consulta.setFechaFin(new Date());
			this.consulta.setPathConsulta("bbbbbbb");
			ServiceLocator.getEstadisticaService().saveCliente(cliente);
			consulta.setIdCliente(cliente.getId());
			ServiceLocator.getEstadisticaService().saveConsulta(consulta);
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
