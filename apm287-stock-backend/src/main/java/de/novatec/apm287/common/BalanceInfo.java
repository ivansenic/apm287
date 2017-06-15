package de.novatec.apm287.common;

import java.time.Instant;

public class BalanceInfo {
	public double balance;
	public double exposure;
	public long timestamp;
	
	public BalanceInfo(double balance, double exposure) {
		this.balance = balance;
		this.exposure = exposure;
		this.timestamp =  Instant.now().toEpochMilli();
	}
	
	public static class BalanceInfoWrapper {
		public final BalanceInfo balanceInfo;

		public BalanceInfoWrapper(BalanceInfo balanceInfo) {
			this.balanceInfo = balanceInfo;
		}
	}
	
}
