package de.novatec.apm287.common.service;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApprovalService {
	
	@Value("${apm287.appoval.minSleep}")
	private long minSleep;
	
	@Value("${apm287.appoval.maxSleep}")
	private long maxSleep;
	
	@Value("${apm287.appoval.approveAlways}")
	private boolean approveAlways;

	/**
	 * Approve buy.
	 */
	public boolean approve(String code, int size) {
		sleep();

		return approveAlways || RandomUtils.nextBoolean();
	}

	/**
	 * Sleeps based on settings
	 */
	private void sleep() {
		if (maxSleep <= 0 || minSleep > maxSleep) {
			return;
		}
		
		// sleep a bit as this takes some time
		try {
			long duration = RandomUtils.nextLong(minSleep, maxSleep);
			Thread.sleep(duration, 0);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
