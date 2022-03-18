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

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RequestHandler implements Runnable {

	private static final Logger log = LogManager.getLogger(RequestHandler.class);

	private Socket clientSocket;
	
	private static String MERCADOLIBREAPIURL="https://api.mercadolibre.com";

	public RequestHandler(Socket clientSocket,Integer counter) {
		this.clientSocket = clientSocket;
		try {
			this.clientSocket.setSoTimeout(2000);
			
			
		} catch (IOException e) {
			log.error(e);
		}
		
		Thread.currentThread().setName("RequestHandler-"+counter);
	}

	@Override
	public void run() {
		try {
			BufferedReader proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String requestString = proxyToClientBr.readLine();
			if(StringUtils.isNotBlank(requestString)) {
				log.info("Reuest Received " + requestString);
				// Get the Request type
				
				String[]requestSplitted=StringUtils.split(requestString, " ");
				this.executeHttp(requestSplitted[0],MERCADOLIBREAPIURL.concat(requestSplitted[1]));
//				this.execWithCurl(MERCADOLIBREAPIURL.concat(requestSplitted[1]));
				
			}

		} catch (IOException e) {
			log.error(e);
			log.error("Error reading request from client");
		}
	}

	private void executeHttp(final String method, final String httpURL) {

		try {
			BufferedWriter proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			
			HttpsURLConnection conn = (HttpsURLConnection) new URL(httpURL).openConnection();
			conn.setRequestMethod(method);
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);
			if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
					&& conn.getResponseCode() <= HttpURLConnection.HTTP_ACCEPTED) {

				proxyToClientBw.write("HTTP/1.0 " + conn.getResponseCode() + " OK\n" + "Proxy-agent: ProxyService/1.0\n" + "\r\n");
				try (BufferedReader proxyToServerBr = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), "utf-8"))) {
					String responseLine;
					while ((responseLine = proxyToServerBr.readLine()) != null) {
						proxyToClientBw.write(responseLine);
					}

				}
			}else
				proxyToClientBw.write("HTTP/1.0 " + conn.getResponseCode() + " BAD REQUEST\n" + "Proxy-agent: ProxyService/1.0\n" + "\r\n");
			proxyToClientBw.flush();
			proxyToClientBw.close();

			conn.disconnect();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			log.info(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.info(e);
		}
	}
	
	@SuppressWarnings("unused")
	private void execWithCurl(final String httpURL) {

		try {

			BufferedWriter proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

			String[] cmds = StringUtils
					.split("curl -X GET -H 'Content-Type: application/json Authorization: Bearer $ACCESS_TOKEN' '"
							+ httpURL + "'", " ");

			BufferedReader reader = new BufferedReader(new InputStreamReader(new ProcessBuilder(cmds).start().getInputStream()));
		
			String responseLine;
			while ((responseLine = reader.readLine()) != null) {
				proxyToClientBw.write(responseLine);;
			}
			proxyToClientBw.flush();
			proxyToClientBw.close();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			log.info(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.info(e);
		}
	}
}
