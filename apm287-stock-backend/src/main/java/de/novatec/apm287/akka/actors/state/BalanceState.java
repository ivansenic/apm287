package de.novatec.apm287.akka.actors.state;

import java.io.Serializable;

public class BalanceState implements Serializable {

	private static final long serialVersionUID = -6075569155329603673L;
	
	private double total;

	public BalanceState(double initial) {
		this.total = initial;
	}

	public BalanceState copy() {
		return new BalanceState(total);
	}

	public void update(BalanceUpdateEvent event) {
		total += event.getUpdate();
	}

	public double getTotal() {
		return total;
	}
}