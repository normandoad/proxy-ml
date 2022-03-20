package mercadolibre.com.ar.proxy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import mercadolibre.com.ar.proxy.model.Proxy;

public interface ProxyRepository extends JpaRepository<Proxy, String> {
	
	List<Proxy> findByPuerto(Integer puerto);
	
	List<Proxy> findByfechaApagadoIsNull();

}
