package mercadolibre.com.ar.proxy.controller.serviceslocators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import mercadolibre.com.ar.proxy.controller.services.DataBaseService;
import mercadolibre.com.ar.proxy.controller.services.StatisticsService;

@Component
public class ServiceLocator {

	private static ServiceLocator instance;
	
	@Autowired
	StatisticsService estatisticsService;
	@Autowired
	DataBaseService dataBaseService;
	
	protected ServiceLocator() {
		ServiceLocator.instance = this;
	}
	
	public static StatisticsService getEstatisticsService() {
		return ServiceLocator.instance.estatisticsService;
	}
	
	public static DataBaseService getDataBaseService() {
		return ServiceLocator.instance.dataBaseService;
	}

}
