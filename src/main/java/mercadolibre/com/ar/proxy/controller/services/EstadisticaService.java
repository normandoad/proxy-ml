package mercadolibre.com.ar.proxy.controller.services;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import mercadolibre.com.ar.proxy.model.Cliente;
import mercadolibre.com.ar.proxy.model.Consulta;
import mercadolibre.com.ar.proxy.model.Proxy;
import mercadolibre.com.ar.proxy.repository.ClienteRepository;
import mercadolibre.com.ar.proxy.repository.ConsultaRepository;
import mercadolibre.com.ar.proxy.repository.ProxyRepository;

@Component
@Qualifier("EstadisticaService")
public class EstadisticaService {
	
	private static final Logger log = LogManager.getLogger(EstadisticaService.class);
	
	@Autowired
    private ClienteRepository clienteRepository; 
	@Autowired
    private ConsultaRepository consultaRepository;
	@Autowired
    private ProxyRepository proxyRepository;
	
	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
    public List<Proxy> findProxyByPuerto(final Integer puerto) {
    	log.debug("start execute");
    	return this.proxyRepository.findByPuerto(puerto);
    }
	
	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public List<Proxy> findProxyByFechaApagadoIsNull(){
		
		return proxyRepository.findByfechaApagadoIsNull();
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
	public Cliente findClienteByIpAndPathConsulta(final String ip,final String pathConsulta) {
    	log.debug("start execute");
    	return this.clienteRepository.findClienteByIpAndPathConsulta(ip,pathConsulta);
    }
	
	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public Cliente findClienteByIp(final String ip) {
    	log.debug("start execute");
    	return this.clienteRepository.findClienteByIp(ip);
    }
	
	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public void saveCliente(final Cliente cliente) {
    	log.debug("start execute");
    	this.clienteRepository.save(cliente);
    }
	
	@Transactional(rollbackFor = java.lang.Exception.class, propagation = Propagation.REQUIRED)
	public void saveConsulta(final Consulta consulta) {
    	log.debug("start execute");
    	this.consultaRepository.save(consulta);
    }

}
