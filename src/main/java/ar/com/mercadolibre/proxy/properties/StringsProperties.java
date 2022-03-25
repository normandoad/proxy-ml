package ar.com.mercadolibre.proxy.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "proxy.strings")
public class StringsProperties {
	
	private String initiated;
	private String unknownShutdown;
	private String waiting;
	private String alreadyIniciated;
	private String serviceIsStoped;
	private String serviceIsAlreadyStoped;
	private String stoppedByTheUser;
	private String proxyServiceThread;
	private String serverClosed;
	private String proxyAgent;
	private String error;
	private String ok;
	private String noContent;
	private String favicon;
	private String httpVer;
	private String contentType;
	private String applicationJson;
	private String accept;
	private String authorization;

}
