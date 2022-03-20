package mercadolibre.com.ar.proxy.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import mercadolibre.com.ar.proxy.controller.services.EstadisticaService;
import mercadolibre.com.ar.proxy.model.Proxy;

@RestController
@Validated
@RequestMapping("/estadisticaController")
public class EstadisticaController {
	
	
	@Autowired
	private EstadisticaService estadisticaService;
	
	@GetMapping(value = "/findServiceProxyByPort/{port}")
	@Operation(description = "search the service proxy/s by port number")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description  = "OK") })
	@ResponseStatus(HttpStatus.OK)
	public List<Proxy>  findServiceProxyByPort(@PathVariable(required = true) Integer port) throws JsonProcessingException{
		
		return estadisticaService.findProxyByPuerto(port);
	}

}
