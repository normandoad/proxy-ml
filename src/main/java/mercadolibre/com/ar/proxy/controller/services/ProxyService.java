package mercadolibre.com.ar.proxy.controller.services;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import mercadolibre.com.ar.proxy.Proxy;



@Component
@Qualifier("ProxyService")
public class ProxyService {

	private Boolean running = Boolean.FALSE;
	
	private Proxy myProxy;
	

	public String listen(Integer port) {
	
		if(!running) {
			running = Boolean.TRUE;
			myProxy = new Proxy(port,running);
			myProxy.listen();
			
		}
		return "service iniciated";
	}

	public String stopListen() {
		if(running) {
			myProxy.closeServer();
			running = Boolean.FALSE;
		}else
			return "The service is already stoped";
		return "The service is stoped";
	}
}
