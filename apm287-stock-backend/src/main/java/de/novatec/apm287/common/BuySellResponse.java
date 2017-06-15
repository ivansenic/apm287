package de.novatec.apm287.common;

import com.fasterxml.jackson.annotation.JsonInclude;

public class BuySellResponse {
	public boolean success;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public StockInfo stockInfo;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public String reason;

	public BuySellResponse(boolean success, StockInfo stockInfo) {
		this(success, stockInfo, null);
	}
	
	public BuySellResponse(boolean success, StockInfo stockInfo, String reason) {
		this.success = success;
		this.stockInfo = stockInfo;
		this.reason = reason;
	}
}