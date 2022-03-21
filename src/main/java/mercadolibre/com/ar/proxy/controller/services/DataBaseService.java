package mercadolibre.com.ar.proxy.controller.services;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mercadolibre.com.ar.proxy.model.Client;
import mercadolibre.com.ar.proxy.model.Proxy;
import mercadolibre.com.ar.proxy.model.Query;
import mercadolibre.com.ar.proxy.repository.ClientRepository;
import mercadolibre.com.ar.proxy.repository.ProxyRepository;
import mercadolibre.com.ar.proxy.repository.QueryRepository;

@Component
@Qualifier("DataBaseService")
public class DataBaseService {
	
	private static final Logger log = LogManager.getLogger(DataBaseService.class);
	
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
}
