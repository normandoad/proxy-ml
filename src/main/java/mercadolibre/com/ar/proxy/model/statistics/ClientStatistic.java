package mercadolibre.com.ar.proxy.model.statistics;

import java.io.Serializable;

import org.joda.time.Period;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class ClientStatistic implements Serializable{
	
	private Integer cantQuerys;
	private Period totalQueryTime;
	private Period totalMeliRequestTime;

}
