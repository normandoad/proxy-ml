package mercadolibre.com.ar.proxy.controller.model;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Position {
	
	@NotBlank
	private Float x;
	@NotBlank
	private Float y;
	@NotBlank
	private String message; 

}
