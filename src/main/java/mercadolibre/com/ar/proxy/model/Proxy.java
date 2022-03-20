package mercadolibre.com.ar.proxy.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="PROXY")
public class Proxy implements Serializable{
	
	@Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "VARCHAR(255)")
	private String id;
	@NotNull
    @Min(1L)
	private Integer puerto;
	@NotNull
    private Date fechaEncendido;
    private Date fechaApagado;
    private String excepcion;
    @OneToMany(mappedBy = "idProxy", fetch = FetchType.EAGER,cascade = CascadeType.ALL)
	private Set<Cliente> cliente;

}
