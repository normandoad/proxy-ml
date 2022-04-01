package mercadolibre.com.ar.proxy.model;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", columnDefinition = "UUID",updatable = false, unique = true, nullable = false)
	private UUID id;
	@NotBlank
	private String ip;
	@NotNull
	private UUID idProxy;

	@OneToMany(mappedBy = "idClient", fetch = FetchType.EAGER,cascade = CascadeType.ALL)
	private Set<Query> querys;
}
