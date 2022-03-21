package mercadolibre.com.ar.proxy.controller.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mercadolibre.com.ar.proxy.model.Client;
import mercadolibre.com.ar.proxy.model.Proxy;
import mercadolibre.com.ar.proxy.model.Query;
import mercadolibre.com.ar.proxy.model.statistics.ProxyStatistics;
import mercadolibre.com.ar.proxy.model.statistics.SystemStatistics;
import mercadolibre.com.ar.proxy.repository.ClientRepository;
import mercadolibre.com.ar.proxy.repository.ProxyRepository;
import mercadolibre.com.ar.proxy.repository.QueryRepository;

@Component
@Qualifier("StatisticsService")
public class StatisticsService {

	private static final Logger log = LogManager.getLogger(StatisticsService.class);

	@Autowired
	private ClientRepository clientRepository;
	@Autowired
	private QueryRepository QueryRepository;
	@Autowired
	private ProxyRepository proxyRepository;

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public List<Proxy> findByPort(final Integer port) {
		log.debug("start execute");
		return this.proxyRepository.findByPort(port);
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public List<Proxy> findByEndDateIsNull() {

		return proxyRepository.findByEndDateIsNull();
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public void saveProxy(final Proxy proxy) {
		log.debug("start execute");
		this.proxyRepository.saveAndFlush(proxy);
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public void saveAllProxy(List<Proxy> proxys) {
		log.debug("start execute");
		this.proxyRepository.saveAll(proxys);
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public Client findByIpAndQueryPath(final String ip, final String queryPath) {
		log.debug("start execute");
		return this.clientRepository.findByIpAndQueryPath(ip, queryPath);
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public Client findClienteByIpAndIdProxy(final String ip, final UUID idProxy) {
		log.debug("start execute");
		return this.clientRepository.findByIpAndIdProxy(ip, idProxy);
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public void saveClient(final Client client) {
		log.debug("start execute");
		this.clientRepository.save(client);
	}

	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public void saveQuery(final Query queryPath) {
		log.debug("start execute");
		this.QueryRepository.save(queryPath);
	}

	public ProxyStatistics findStatisticsProxyBy(final String id) {
		Optional<Proxy> oProxy = this.proxyRepository.findById(UUID.fromString(id));

		if (oProxy.isPresent()) {

			ProxyStatistics estadisticasProxy = this.getStatisticsProxy(oProxy.get());
			return estadisticasProxy;
		}

		return null;
	}

	private ProxyStatistics getStatisticsProxy(final Proxy proxy) {

		DateTime initDate = new DateTime(proxy.getInitDate());
		DateTime endDate = proxy.getEndDate() != null ? new DateTime(proxy.getEndDate()) : DateTime.now();

		ProxyStatistics estadisticasProxy = new ProxyStatistics();

		if (proxy.getClients() != null)
			estadisticasProxy.setClientAttended(proxy.getClients().size());
		if (estadisticasProxy.getClientAttended() != null) {
			Integer querysAttended = 0;
			for (Client cliente : proxy.getClients()) {
				querysAttended = querysAttended + (cliente.getQuerys() != null ? cliente.getQuerys().size() : 0);
			}

			estadisticasProxy.setQuerysAttended(querysAttended);
		}

		estadisticasProxy.setProxy(SerializationUtils.clone(proxy));
		estadisticasProxy.getProxy().setClients(null);
		estadisticasProxy.setUpTime(new Period(initDate, endDate));

		proxy.setClients(null);

		return estadisticasProxy;

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

	public SystemStatistics getSystemStatistics() {

		List<Proxy> proxyList = this.proxyRepository.findAll();

		SystemStatistics systemStatistics = new SystemStatistics();

		Integer cantProxys = proxyList.size();
		Integer cantClients = 0;
		Integer cantQuerys = 0;

		Duration durationRequest=null;
		Duration durationMeliRequest=null;
		for (Proxy proxy : proxyList) {
			cantClients = cantClients + proxy.getClients().size();
			for (Client cliente : proxy.getClients()) {
				cantQuerys = cantQuerys + cliente.getQuerys().size();
				durationRequest=this.calculateTotalRequestTime(cliente.getQuerys(),durationRequest);
				durationMeliRequest=this.calculateTotalMeliRequestTime(cliente.getQuerys(),durationMeliRequest);
			}
		}

		this.calculateProxyTotalduraction(proxyList, systemStatistics);

		systemStatistics.setQuantitysPoxy(cantProxys);
		systemStatistics.setClientAttended(cantClients);
		systemStatistics.setQuerysAttended(cantQuerys);
		systemStatistics.setTotalRequestTime(durationRequest!=null?durationRequest.toPeriod():null);
		systemStatistics.setTotalMeliRequestTime(durationMeliRequest!=null?durationMeliRequest.toPeriod():null);
		
		if(durationRequest==null&&durationMeliRequest==null)
			systemStatistics.setTotalSystemProcessTime(null);
		else
			if(durationRequest!=null&&durationMeliRequest==null)
				systemStatistics.setTotalSystemProcessTime(durationRequest.toPeriod());
			else
				if(durationRequest==null&&durationMeliRequest!=null)
					systemStatistics.setTotalSystemProcessTime(durationMeliRequest.toPeriod());
				else
					systemStatistics.setTotalSystemProcessTime(durationRequest.minus(durationMeliRequest).toPeriod());
		
		
		return systemStatistics;
	}

}
