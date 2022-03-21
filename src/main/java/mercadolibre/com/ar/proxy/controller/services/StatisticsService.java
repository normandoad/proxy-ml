package mercadolibre.com.ar.proxy.controller.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.SerializationUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import mercadolibre.com.ar.proxy.model.Client;
import mercadolibre.com.ar.proxy.model.Proxy;
import mercadolibre.com.ar.proxy.model.Query;
import mercadolibre.com.ar.proxy.model.statistics.ClientStatistic;
import mercadolibre.com.ar.proxy.model.statistics.ProxyStatistics;
import mercadolibre.com.ar.proxy.model.statistics.SystemStatistics;
import mercadolibre.com.ar.proxy.repository.ClientRepository;
import mercadolibre.com.ar.proxy.repository.ProxyRepository;

@Component
@Qualifier("StatisticsService")
public class StatisticsService {

	@Autowired
	private ProxyRepository proxyRepository;

	@Autowired
	private ClientRepository clientRepository;

	public ProxyStatistics findStatisticsProxyBy(final String id) {
		Optional<Proxy> oProxy = this.proxyRepository.findById(UUID.fromString(id));

		ProxyStatistics estatisticsProxy = new ProxyStatistics();

		if (oProxy.isPresent()) {
			Proxy proxy = oProxy.get();

			DateTime initDate = new DateTime(proxy.getInitDate());
			DateTime endDate = proxy.getEndDate() != null ? new DateTime(proxy.getEndDate()) : DateTime.now();

			if (proxy.getClients() != null)
				estatisticsProxy.setClientAttended(proxy.getClients().size());
			if (estatisticsProxy.getClientAttended() != null) {
				Integer querysAttended = 0;
				for (Client cliente : proxy.getClients()) {
					querysAttended = querysAttended + (cliente.getQuerys() != null ? cliente.getQuerys().size() : 0);
				}

				estatisticsProxy.setQuerysAttended(querysAttended);
			}

			estatisticsProxy.setProxy(SerializationUtils.clone(proxy));
			estatisticsProxy.getProxy().setClients(null);
			estatisticsProxy.setUpTime(new Period(initDate, endDate));

			proxy.setClients(null);

			return estatisticsProxy;
		}

		return null;
	}
	
	public SystemStatistics getSystemStatistics() {

		List<Proxy> proxyList = this.proxyRepository.findAll();

		SystemStatistics systemStatistics = new SystemStatistics();

		Integer cantProxys = proxyList.size();
		Integer cantClients = 0;
		Integer cantQuerys = 0;

		Duration durationRequest = null;
		Duration durationMeliRequest = null;
		for (Proxy proxy : proxyList) {
			cantClients = cantClients + proxy.getClients().size();
			for (Client client : proxy.getClients()) {
				cantQuerys = cantQuerys + client.getQuerys().size();
				durationRequest = this.calculateTotalRequestTime(client.getQuerys(), durationRequest);
				durationMeliRequest = this.calculateTotalMeliRequestTime(client.getQuerys(), durationMeliRequest);
			}
		}

		this.calculateProxyTotalduraction(proxyList, systemStatistics);

		systemStatistics.setQuantitysPoxy(cantProxys);
		systemStatistics.setClientAttended(cantClients);
		systemStatistics.setQuerysAttended(cantQuerys);
		systemStatistics.setTotalRequestTime(durationRequest != null ? durationRequest.toPeriod() : null);
		systemStatistics.setTotalMeliRequestTime(durationMeliRequest != null ? durationMeliRequest.toPeriod() : null);

		if (durationRequest == null && durationMeliRequest == null)
			systemStatistics.setTotalSystemProcessTime(null);
		else if (durationRequest != null && durationMeliRequest == null)
			systemStatistics.setTotalSystemProcessTime(durationRequest.toPeriod());
		else if (durationRequest == null && durationMeliRequest != null)
			systemStatistics.setTotalSystemProcessTime(durationMeliRequest.toPeriod());
		else
			systemStatistics.setTotalSystemProcessTime(durationRequest.minus(durationMeliRequest).toPeriod());

		return systemStatistics;
	}

	public ClientStatistic findClientStatisticsById(final String id) {
		Optional<Client> oClient = this.clientRepository.findById(UUID.fromString(id));
		
		ClientStatistic clientStatistics=new ClientStatistic();

		if (oClient.isPresent()) {
			
			Integer cantQuerys = 0;
			Duration durationRequest = null;
			Duration durationMeliRequest = null;
			
			cantQuerys = cantQuerys + oClient.get().getQuerys().size();
			durationRequest = this.calculateTotalRequestTime( oClient.get().getQuerys(), durationRequest);
			durationMeliRequest = this.calculateTotalMeliRequestTime( oClient.get().getQuerys(), durationMeliRequest);

			clientStatistics.setTotalQueryTime(durationRequest.toPeriod());
			clientStatistics.setTotalMeliRequestTime(durationMeliRequest.toPeriod());
			clientStatistics.setCantQuerys(cantQuerys);
			return clientStatistics;
		}

		return null;
	}

	private void calculateProxyTotalduraction(final List<Proxy> proxyList, SystemStatistics systemStatistics) {
		Duration duration = null;

		for (Proxy proxy : proxyList) {
			DateTime initDate = new DateTime(proxy.getInitDate());
			DateTime endDate = new DateTime(proxy.getEndDate());

			duration = this.getDuration(initDate, endDate, duration);
		}
		systemStatistics.setUpTime(duration.toPeriod());
	}

	private Duration calculateTotalMeliRequestTime(final Set<Query> queryList, Duration duration) {

		for (Query query : queryList) {
			DateTime initDate = new DateTime(query.getInitDate());
			DateTime endDate = new DateTime(query.getEndDateMeliRequest());

			duration = this.getDuration(initDate, endDate, duration);
		}

		return duration;
	}

	private Duration calculateTotalRequestTime(final Set<Query> queryList, Duration duration) {

		for (Query query : queryList) {
			DateTime initDate = new DateTime(query.getInitDate());
			DateTime endDate = query.getEndDate() != null ? new DateTime(query.getEndDate()) : DateTime.now();

			duration = this.getDuration(initDate, endDate, duration);
		}

		return duration;
	}

	private Duration getDuration(final DateTime initDate, final DateTime endDate, Duration duration) {

		if (duration == null)
			return (new Interval(initDate, endDate)).toDuration();

		Interval nextInterval = new Interval(initDate, endDate);
		duration = duration.plus(nextInterval.toDuration());

		return duration;

	}
}
