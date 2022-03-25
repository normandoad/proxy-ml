package ar.com.mercadolibre.proxy.locators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.com.mercadolibre.proxy.properties.StringsProperties;
import ar.com.mercadolibre.proxy.properties.Properties;

@Component
public class ProxyPropertiesLocator {
	
	private static ProxyPropertiesLocator instance;
	
	@Autowired
	private Properties properties;

	@Autowired
	private StringsProperties stringProperties;

	public ProxyPropertiesLocator() {
		super();
		// TODO Auto-generated constructor stub
		
		ProxyPropertiesLocator.instance=this;
	}
	
	public static Properties getProperties() {
		return ProxyPropertiesLocator.instance.properties;
	}
	
	public static  StringsProperties getStringProperties() {
		return ProxyPropertiesLocator.instance.stringProperties;
	}

}
