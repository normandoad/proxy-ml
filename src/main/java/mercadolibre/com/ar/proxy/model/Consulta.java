package mercadolibre.com.ar.proxy.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
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
@Table(name="consulta")
public class Consulta implements Serializable{
	
	@Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", columnDefinition = "VARCHAR(255)")
	private String id;
	private String idCliente;
    @NotBlank
    private String pathConsulta;
    @NotNull
    private Date fechaInicio;
    @NotNull
    private Date fechaFin;
    @NotNull
    private Date fechaInicioConsultaMeli;
    @NotNull
    private Date fechaFinConsultaMeli;
    private String excepcion;
	
}
