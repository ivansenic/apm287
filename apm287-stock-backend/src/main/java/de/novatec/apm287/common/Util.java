package de.novatec.apm287.common;

import org.apache.commons.lang3.RandomUtils;

public class Util {

	/**
	 * Returns updated price with increase or decrease of maximum 1%.
	 * 
	 * @param current
	 *            current price
	 * @return updated price
	 */
	public static double getUpdatedPrice(double current) {
		// max move is 1%, can be positive or negative
		double factor = RandomUtils.nextDouble(0.0d, 0.01d);
		if (RandomUtils.nextBoolean()) {
			factor *= -1.0;
		}
		return current + (current * factor);
	}

	/**
	 * Returns price change percentage since the last change.
	 * 
	 * @param old
	 * @param current
	 * @return
	 */
	public static double getChangePercentage(double old, double current) {
		if (old > current) {
			return -1.0d * (old / current -1.0d);
		} else {
			return current / old -1.0d;
		}
	}
}
