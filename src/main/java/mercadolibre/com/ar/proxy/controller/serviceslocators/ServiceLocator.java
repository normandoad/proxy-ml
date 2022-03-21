package mercadolibre.com.ar.proxy.controller.serviceslocators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mercadolibre.com.ar.proxy.controller.services.StatisticsService;

@Component
public class ServiceLocator {

	private static ServiceLocator instance;
	
	@Autowired
	StatisticsService estadisticsService;
	
	protected ServiceLocator() {
		ServiceLocator.instance = this;
	}
	
	public static StatisticsService getEstadisticsService() {
		return ServiceLocator.instance.estadisticsService;
	}

}
