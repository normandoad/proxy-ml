package mercadolibre.com.ar.proxy.model.statistics;

import java.io.Serializable;

import org.joda.time.Period;

import lombok.Data;
import mercadolibre.com.ar.proxy.model.Proxy;

@SuppressWarnings("serial")
@Data
public class ProxyStatistics implements Serializable{
	
	
	private Integer clientAttended;
	private Integer querysAttended;
	private Proxy proxy;
	private Period upTime;
}
