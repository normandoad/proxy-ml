package mercadolibre.com.ar.proxy.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name="query")
public class Query implements Serializable{
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", columnDefinition = "UUID",updatable = false, unique = true, nullable = false)
	private UUID id;
	@NotNull
	private UUID idClient;
    @NotBlank
    private String queryPath;
    @NotNull
    private Date initDate;
    @NotNull
    private Date endDate;
    @NotNull
    private Date initDateMeliRequest;
    @NotNull
    private Date endDateMeliRequest;
    private String exception;
	
}
