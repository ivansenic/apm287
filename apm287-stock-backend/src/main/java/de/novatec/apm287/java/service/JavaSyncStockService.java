package de.novatec.apm287.java.service;

import org.springframework.beans.factory.annotation.Autowired;

import de.novatec.apm287.common.service.IStockService;
import de.novatec.apm287.java.sync.StockManager;

public class JavaSyncStockService implements IStockService {

	@Autowired
	private StockManager manager;
	
	@Override
	public Object balance() {
		return manager.balance();
	}

	@Override
	public Object stockInfo() {
		return manager.stockInfo();
	}

	@Override
	public Object buy(String code, int size) {
		return manager.buy(code, size);
	}

	@Override
	public Object sell(String code, int size) {
		return manager.sell(code, size);
	}

}
