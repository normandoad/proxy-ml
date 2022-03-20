package mercadolibre.com.ar.proxy.model;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cliente")
public class Cliente implements Serializable {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id", columnDefinition = "VARCHAR(255)")
	private String id;
	@NotBlank
	private String ip;
	@JoinTable(name = "clientesproxys", joinColumns = @JoinColumn(name = "idcliente", nullable = false), inverseJoinColumns = @JoinColumn(name = "idproxy", nullable = false))
	@ManyToMany(cascade = CascadeType.ALL)
	private Set<Proxy> proxys;

	@OneToMany(mappedBy = "idCliente", fetch = FetchType.EAGER,cascade = CascadeType.ALL)
	private Set<Consulta> consultas;
}
