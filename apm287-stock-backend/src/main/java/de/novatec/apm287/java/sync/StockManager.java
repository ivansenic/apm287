package de.novatec.apm287.java.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.novatec.apm287.common.BalanceInfo;
import de.novatec.apm287.common.BalanceInfo.BalanceInfoWrapper;
import de.novatec.apm287.common.BuySellResponse;
import de.novatec.apm287.common.PriceUpdate;
import de.novatec.apm287.common.StockInfo;
import de.novatec.apm287.common.StockInfo.StockInfoWarpper;
import de.novatec.apm287.common.Util;
import de.novatec.apm287.common.service.ApprovalService;

@Component
public class StockManager {
	
	/**
	 * When buy comes with more than this stock size we must approve this.
	 */
	private static final int APPROVE_THRESHOLD = 5;

	
	/**
	 * Approval service.
	 */
	@Autowired
	private ApprovalService approvalService;
	
	/**
	 * Default balance to start with is 10.000.
	 */
	private double balance = 10_000;

	/**
	 * Map where we hold current data.
	 */
	private Map<String, StockInfo> codeToStock = new HashMap<>();
	
	/**
	 * Executor for updater.
	 */
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	
	/**
	 * Just schedule on the start.
	 */
	public StockManager() {
		PriceUpdater updater = new PriceUpdater();
		executorService.scheduleAtFixedRate(updater, 1, 1, TimeUnit.SECONDS);
	}

	/**
	 * @return current balance info
	 */
	public synchronized BalanceInfoWrapper balance() {
		BalanceInfo balanceInfo = new BalanceInfo(balance, getExposure());
		return new BalanceInfoWrapper(balanceInfo); 
	}

	/**
	 * @return stock info wrapper
	 */
	public synchronized StockInfoWarpper stockInfo() {
		return new StockInfoWarpper(codeToStock.values());
	}

	public synchronized BuySellResponse buy(String code, int size) {
		StockInfo stockInfo = codeToStock.get(code);
		if (null == stockInfo) {
			return new BuySellResponse(false, null, "Wrong stock code.");
		}
		if (size <= 0) {
			return new BuySellResponse(false, null, "Wrong buy size.");
		}

		if (size > APPROVE_THRESHOLD) {
			boolean approved = approvalService.approve(code, size);
			if (!approved) {
				return new BuySellResponse(false, null, "Transaction not approved.");
			} else {
				return executeBuy(stockInfo, size);
			}
		} else {
			return executeBuy(stockInfo, size);
		}
	}

	public synchronized BuySellResponse sell(String code, int size) {
		StockInfo stockInfo = codeToStock.get(code);
		if (null == stockInfo) {
			return new BuySellResponse(false, null, "Wrong stock code.");
		}
		if (size <= 0) {
			return new BuySellResponse(false, null, "Wrong sell size.");
		}

		if (size > stockInfo.holding) {
			return new BuySellResponse(false, null, "Insufficient stock holding.");
		}

		double cost = stockInfo.price * size;
		balance += cost;
		stockInfo.holding -= size;

		return new BuySellResponse(true, stockInfo);
	}
	
	/**
	 * Updates price of a single stock.
	 */
	protected synchronized void priceUpdate(PriceUpdate update) {
		String code = update.code;
		StockInfo stockPrice = codeToStock.get(code);
		if (null != stockPrice) {
			stockPrice.change = Util.getChangePercentage(stockPrice.startingPrice, update.price);
			stockPrice.price = update.price;
		} else {
			stockPrice = new StockInfo(code, update.price);
			codeToStock.put(code, stockPrice);
		}
	}
	
	/**
	 * When approved does the buy.
	 */
	private BuySellResponse executeBuy(StockInfo stockInfo, int size) {
		double cost = stockInfo.price * size;
		if (cost > balance) {
			return new BuySellResponse(false, null, "Insufficient balance.");
		}

		balance -= cost;
		stockInfo.holding += size;

		return new BuySellResponse(true, stockInfo);
	}
	
	/**
	 * @return Current exposure in stocks.
	 */
	private double getExposure() {
		return codeToStock.values().stream().mapToDouble(s -> s.price * s.holding).sum();
	}
	
	private class PriceUpdater implements Runnable {
		
		/**
		 * Codes to generate prices for.
		 */
		private String[] codes;

		/**
		 * Price for each code.
		 */
		private double[] prices;
		
		public PriceUpdater() {
			codes = new String[5];
			prices = new double[codes.length];
			for (int i = 0; i < codes.length; i++) {
				codes[i] = RandomStringUtils.randomAlphabetic(3).toUpperCase();
				prices[i] = 100.0d;
			}
			
			sendUpdates();
		}
		
		@Override
		public void run() {
			for (int i = 0; i < codes.length; i++) {
				prices[i] = Util.getUpdatedPrice(prices[i]);
			}
			
			sendUpdates();
		}
		
		private void sendUpdates() {
			for (int i = 0; i < prices.length; i++) {
				PriceUpdate priceUpdate = new PriceUpdate(codes[i], prices[i]);
				priceUpdate(priceUpdate);
			}
		}
		
	}
	
}
