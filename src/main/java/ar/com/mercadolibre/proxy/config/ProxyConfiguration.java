package ar.com.mercadolibre.proxy.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EnableAutoConfiguration()
@ComponentScan({ "ar.com.mercadolibre.commons"})
@EntityScan("ar.com.mercadolibre.commons.model")
@EnableJpaRepositories("ar.com.mercadolibre.commons.repository")
public class ProxyConfiguration {
	
}