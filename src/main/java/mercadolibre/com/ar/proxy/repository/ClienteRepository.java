package mercadolibre.com.ar.proxy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mercadolibre.com.ar.proxy.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
	
	
	@Query(value = "SELECT cl FROM Cliente cl WHERE cl.ip=:ip AND cl.id=(SELECT co.idCliente FROM Consulta co WHERE co.pathConsulta=:pathConsulta)")
	Cliente findClienteByIpAndPathConsulta(@Param("ip")final String ip,@Param("pathConsulta")final String pathConsulta);
	
	Cliente findClienteByIp(@Param("ip")final String ip);

}
