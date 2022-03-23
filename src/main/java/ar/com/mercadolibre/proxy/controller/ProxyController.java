package ar.com.mercadolibre.proxy.controller;

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
import ar.com.mercadolibre.proxy.controller.services.ProxyService;

@RestController
@Validated
@RequestMapping("/proxyController")
public class ProxyController {
	
	@Autowired
	private ProxyService proxyService;
	
	@GetMapping(value = "/initProxy/{port}")
	@Operation(description = "init the proxy")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description  = "OK") })
	@ResponseStatus(HttpStatus.OK)
	public String  initProxy(@PathVariable(required = true) Integer port) throws JsonProcessingException{
		
		return proxyService.listen(port);
	}
	
	@GetMapping(value = "/stopProxy")
	@Operation(description = "stop the proxy")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description  = "OK") })
	@ResponseStatus(HttpStatus.OK)
	public String  stopProxy() throws JsonProcessingException{
		return proxyService.stopListen();
	}
}
