package ar.com.mercadolibre.proxy.properties;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "proxy.properties")
public class Properties {
	@NotNull
	private Integer maxPoolSize;
	@NotNull
	private Integer maxThreadPool;
	@NotNull
	private Integer maxThreadPoolSize;
	@NotNull
	private Integer expirationPolicyIp;
	@NotNull
	private Integer expirationPolicyQueryPathAndIp;
	@NotNull
	private Integer expirationPolicyQueryPath;
	@NotNull
	private Integer expirationPolicyFlooding;
	@NotNull
	private Long keepAliveTime;
	@NotNull
	private String mercadoLibreApiUrl;

}
