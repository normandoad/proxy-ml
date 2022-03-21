package mercadolibre.com.ar.proxy.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import mercadolibre.com.ar.proxy.model.Proxy;

public interface ProxyRepository extends JpaRepository<Proxy, UUID> {
	
	List<Proxy> findByPort(Integer port);
	
	List<Proxy> findByEndDateIsNull();

}
