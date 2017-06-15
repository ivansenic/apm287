package de.novatec.apm287.common.service;

public interface IStockService {

	Object balance();
	Object stockInfo();
	Object buy(String code, int size);
	Object sell(String code, int size);
}
