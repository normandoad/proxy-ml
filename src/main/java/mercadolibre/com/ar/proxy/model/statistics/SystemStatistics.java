package mercadolibre.com.ar.proxy.model.statistics;

import java.io.Serializable;

import org.joda.time.Period;

import lombok.Data;

@SuppressWarnings("serial")
@Data
public class SystemStatistics implements Serializable{
	
	private Integer quantitysPoxy;
	private Integer clientAttended;
	private Integer querysAttended;
	private Period upTime;
	private Period totalRequestTime;
	private Period totalMeliRequestTime;
	private Period totalSystemProcessTime;
}
