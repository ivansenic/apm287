package de.novatec.apm287.akka.messages;

import java.util.Collection;

import de.novatec.apm287.common.BalanceInfo;
import de.novatec.apm287.common.StockInfo;

/**
 * Messages used only between akka actors.
 * 
 * @author Ivan Seni
 *
 */
public class Message {

	public static class OverviewRequest {
	}

	public static class OverviewReply {
		public Collection<StockInfo> stockPrices;

		public OverviewReply(Collection<StockInfo> stockPrices) {
			this.stockPrices = stockPrices;
		}
	}

	public static class BalanceRequest {
	}

	public static class BalanceReply {
		public BalanceInfo balanceInfo;

		public BalanceReply(BalanceInfo balanceInfo) {
			this.balanceInfo = balanceInfo;
		}
	}

	public interface TradeRequest {
		boolean isBuy();

		boolean isSell();

		String code();

		int size();
	}

	public static class BuyRequest implements TradeRequest {
		public String code;
		public int size;

		public BuyRequest(String code, int size) {
			this.code = code;
			this.size = size;
		}

		@Override
		public boolean isBuy() {
			return true;
		}

		@Override
		public boolean isSell() {
			return false;
		}

		@Override
		public String code() {
			return code;
		}

		@Override
		public int size() {
			return size;
		}

	}

	public static class SellRequest implements TradeRequest {
		public String code;
		public int size;

		public SellRequest(String code, int size) {
			this.code = code;
			this.size = size;
		}

		@Override
		public boolean isBuy() {
			return false;
		}

		@Override
		public boolean isSell() {
			return true;
		}

		@Override
		public String code() {
			return code;
		}

		@Override
		public int size() {
			return size;
		}

	}

}
