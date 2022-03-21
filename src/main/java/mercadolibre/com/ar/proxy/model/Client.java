package mercadolibre.com.ar.proxy.model;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "client")
public class Client implements Serializable {

	@Id
	@Column(name = "id", columnDefinition = "UUID")
	private UUID id=UUID.randomUUID();
	@NotBlank
	private String ip;
	@NotNull
	private UUID idProxy;

	@OneToMany(mappedBy = "idClient", fetch = FetchType.EAGER,cascade = CascadeType.ALL)
	private Set<Query> querys;
}
