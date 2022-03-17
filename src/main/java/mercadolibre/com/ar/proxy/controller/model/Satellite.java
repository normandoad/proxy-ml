package mercadolibre.com.ar.proxy.controller.model;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Satellite {
	
	public static final String KENOBI="Kenobi";
	public static final String SKYWALKER="Skywalker";
	public static final String SATO="Sato";
	
	@NotNull(message = "Name may not be null")
	@NotBlank(message = "Name may not be blank")
	@Pattern(regexp = KENOBI+"|"+SKYWALKER+"|"+SATO, flags = Pattern.Flag.CASE_INSENSITIVE,message = "The satellite name will be: "+KENOBI+" or "+SKYWALKER+" or "+SATO)
	private String name;
	@NotNull(message = "distance may not be null")
	@Positive(message = "the distance will be a positive number")
	private Float distance;
	@NotNull(message = "message may not be null")
	@NotEmpty(message = "message may not be empty")
	private List<String> message;
	
	public Satellite(@NotNull(message = "Name may not be null") @NotBlank(message = "Name may not be blank")
					@Pattern(regexp = KENOBI+"|"+SKYWALKER+"|"+SATO, flags = Pattern.Flag.CASE_INSENSITIVE,message = "The satellite name will be: "+KENOBI+" or "+SKYWALKER+" or "+SATO) 
					String name
					,@NotNull(message = "distance may not be null")@Positive(message = "the distance will be a positive number") Float distance,
					@NotNull(message = "message may not be null") @NotEmpty(message = "message may not be empty") List<String> message) {
		super();
		this.name = name;
		this.distance = distance;
		this.message = message;
	}
}
