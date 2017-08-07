package de.novatec.apm287.akka.actors.state;

import java.io.Serializable;

public class BalanceUpdateEvent implements Serializable {

	private static final long serialVersionUID = 3899477342934025732L;

	/**
	 * How much balance is updated.
	 */
	private final double update;

	public BalanceUpdateEvent(double update) {
		this.update = update;
	}

	public double getUpdate() {
		return update;
	}
}