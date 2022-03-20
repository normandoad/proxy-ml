package mercadolibre.com.ar.proxy.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mercadolibre.com.ar.proxy.model.Consulta;

public interface ConsultaRepository extends JpaRepository<Consulta, String> {

}
