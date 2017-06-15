package de.novatec.apm287.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class StockRestService {
	
	@Autowired
	IStockService stockService;

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/balance", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object balance() {
		return stockService.balance();
	}

	@CrossOrigin(origins = "*")
	@GetMapping(value = "/stocks", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object stockInfo() {
		return stockService.stockInfo();
	}
	
	@CrossOrigin(origins = "*")
	@GetMapping(value = "/buy", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object buy(
			@RequestParam(name = "c", required = true) String code, 
			@RequestParam(name = "s", required = false, defaultValue = "1") int size
		) {
		return stockService.buy(code, size);
	}
	
	@CrossOrigin(origins = "*")
	@GetMapping(value = "/sell", produces = MediaType.APPLICATION_JSON_VALUE)
	public Object sell(
			@RequestParam(name = "c", required = true) String code, 
			@RequestParam(name = "s", required = false, defaultValue = "1") int size
		) {
		return stockService.sell(code, size);
	}


}
