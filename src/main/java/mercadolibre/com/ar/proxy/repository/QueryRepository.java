package mercadolibre.com.ar.proxy.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import mercadolibre.com.ar.proxy.model.Query;

public interface QueryRepository extends JpaRepository<Query, UUID> {

}
