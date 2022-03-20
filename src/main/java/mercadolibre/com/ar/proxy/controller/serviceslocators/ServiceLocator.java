package mercadolibre.com.ar.proxy.controller.serviceslocators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mercadolibre.com.ar.proxy.controller.services.EstadisticaService;

@Component
public class ServiceLocator {

	private static ServiceLocator instance;
	
	@Autowired
	EstadisticaService estadisticaService;
	
	protected ServiceLocator() {
		ServiceLocator.instance = this;
	}
	
	public static EstadisticaService getEstadisticaService() {
		return ServiceLocator.instance.estadisticaService;
	}

}
