package de.novatec.apm287.akka.actors;

import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import de.novatec.apm287.common.PriceUpdate;
import de.novatec.apm287.common.Util;
import scala.concurrent.duration.Duration;

public class PriceGeneratorActor extends AbstractActor {

	/**
	 * @return props for creating this actor.
	 */
	public static Props props(ActorRef stockServiceActor, String... codes) {
		return Props.create(PriceGeneratorActor.class, () -> new PriceGeneratorActor(stockServiceActor, codes));
	}

	/**
	 * Stock service to report updates to.
	 */
	private ActorRef stockServiceActor;

	/**
	 * Codes to generate prices for.
	 */
	private String[] codes;

	/**
	 * Price for each code.
	 */
	private double[] prices;

	/**
	 * @param stockServiceActor
	 * @param codes
	 *            Codes to generate prices for.
	 */
	public PriceGeneratorActor(ActorRef stockServiceActor, String[] codes) {
		this.stockServiceActor = stockServiceActor;
		this.codes = codes;
		this.prices = new double[codes.length];
	}

	/**
	 * On pre-start create starting prices.
	 */
	@Override
	public void preStart() throws Exception {
		for (int i = 0; i < prices.length; i++) {
			prices[i] = 100.0d;
			
			PriceUpdate priceUpdate = new PriceUpdate(codes[i], prices[i]);
			stockServiceActor.tell(priceUpdate, getSelf());
		}
		
		// schedule first update
		scheduleUpdate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder().matchEquals("update", message -> {
			for (int i = 0; i < codes.length; i++) {
				prices[i] = Util.getUpdatedPrice(prices[i]);
				PriceUpdate priceUpdate = new PriceUpdate(codes[i], prices[i]);
				stockServiceActor.tell(priceUpdate, getSelf());
			}
			
			// schedule next update
			scheduleUpdate();
			
		}).build();
	}

	/**
	 * Schedules sending of the update message to self in 1 seconds
	 */
	private void scheduleUpdate() {
		Scheduler scheduler = getContext().getSystem().scheduler();
		scheduler.scheduleOnce(Duration.create(1, TimeUnit.SECONDS), getSelf(), "update",
				getContext().dispatcher(), null);
	}

}
