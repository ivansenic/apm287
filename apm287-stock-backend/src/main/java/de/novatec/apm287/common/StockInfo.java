package de.novatec.apm287.common;

import java.util.Collection;
import java.util.Collections;

public class StockInfo {
	public String code;
	public final double startingPrice;
	public double price;
	public double change;
	public int holding;
	
	public StockInfo(String code, double price) {
		this.code = code;
		this.startingPrice = price;
		this.price = price;
	}
	
	public static class StockInfoWarpper {
		public final Collection<StockInfo> stockPrices;

		public StockInfoWarpper(Collection<StockInfo> stockPrices) {
			this.stockPrices = Collections.unmodifiableCollection(stockPrices);
		}
	}
	
}
