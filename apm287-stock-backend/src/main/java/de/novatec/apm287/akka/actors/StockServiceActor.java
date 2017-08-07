package de.novatec.apm287.akka.actors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.CircuitBreaker;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotOffer;
import de.novatec.apm287.akka.actors.state.BalanceState;
import de.novatec.apm287.akka.actors.state.BalanceUpdateEvent;
import de.novatec.apm287.akka.messages.Message.BalanceReply;
import de.novatec.apm287.akka.messages.Message.BalanceRequest;
import de.novatec.apm287.akka.messages.Message.BuyRequest;
import de.novatec.apm287.akka.messages.Message.OverviewReply;
import de.novatec.apm287.akka.messages.Message.OverviewRequest;
import de.novatec.apm287.akka.messages.Message.SellRequest;
import de.novatec.apm287.common.BalanceInfo;
import de.novatec.apm287.common.BuySellResponse;
import de.novatec.apm287.common.PriceUpdate;
import de.novatec.apm287.common.StockInfo;
import de.novatec.apm287.common.Util;
import de.novatec.apm287.common.service.ApprovalService;
import scala.concurrent.duration.Duration;

public class StockServiceActor extends AbstractPersistentActor {

	/**
	 * @return props for creating this actor.
	 */
	public static Props props(Optional<Double> balance, ApprovalService approvalService) {
		return Props.create(StockServiceActor.class, () -> new StockServiceActor(balance, approvalService));
	}

	/**
	 * Current balance state.
	 */
	private BalanceState balanceState;

	/**
	 * When buy/sell comes with more than this stock size we must approve this.
	 */
	private int approveThreshold = 5;

	/**
	 * Map where we hold current data.
	 */
	private Map<String, StockInfo> codeToStock = new HashMap<>();

	/**
	 * Circuit breaker for calling the service.
	 */
	private CircuitBreaker circuitBreaker;

	/**
	 * Approval service.
	 */
	private ApprovalService approvalService;

	/**
	 * @param balance
	 *            Optionally balance to start with.
	 */
	public StockServiceActor(Optional<Double> balance, ApprovalService approvalService) {
		if (balance.isPresent()) {
			this.balanceState = new BalanceState(balance.get().doubleValue());
		} else {
			this.balanceState = new BalanceState(10_000);
		}
		this.approvalService = approvalService;
		this.circuitBreaker = new CircuitBreaker(getContext().dispatcher(), getContext().system().scheduler(), 5,
				Duration.create(50, TimeUnit.MILLISECONDS), Duration.create(1, TimeUnit.MINUTES));
		
		if (10_000 == balanceState.getTotal()) {
			ActorSystem system = getContext().getSystem();
			system.scheduler().scheduleOnce(Duration.create(1, "minute"), getSelf(), "restart", system.dispatcher(), null);
		}
	}

	@Override
	public String persistenceId() {
		return "balance-update-events";
	}

	@Override
	public Receive createReceiveRecover() {
		return receiveBuilder()
				.match(BalanceUpdateEvent.class, balanceState::update)
				.match(SnapshotOffer.class, ss -> balanceState = (BalanceState) ss.snapshot())
				.match(RecoveryCompleted.class, r -> System.out.println("Balance after recovery " + balanceState.getTotal()))
				.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(PriceUpdate.class, this::priceUpdate).match(OverviewRequest.class, r -> {
					getSender().tell(new OverviewReply(Collections.unmodifiableCollection(codeToStock.values())), getSelf());
				})
				.match(BalanceRequest.class, r -> {
					BalanceInfo balanceInfo = new BalanceInfo(balanceState.getTotal(), getExposure());
					getSender().tell(new BalanceReply(balanceInfo), getSelf());
				})
				.match(BuyRequest.class, this::buyRequest)
				.match(SellRequest.class, this::sellRequest)
				.matchEquals("restart", m -> {
					System.out.println("Balance before failing " + balanceState.getTotal());
					throw new Exception("I had to dieeee");
				})
				.build();
	}

	/**
	 * @return Current exposure in stocks.
	 */
	private double getExposure() {
		return codeToStock.values().stream().mapToDouble(s -> s.price * s.holding).sum();
	}

	/**
	 * Executes buy request.
	 */
	private void buyRequest(BuyRequest request) {
		StockInfo stockInfo = codeToStock.get(request.code);
		if (null == stockInfo) {
			getSender().tell(new BuySellResponse(false, null, "Wrong stock code."), getSelf());
			return;
		}
		if (request.size <= 0) {
			getSender().tell(new BuySellResponse(false, null, "Wrong buy size."), getSelf());
			return;
		}

		if (request.size > approveThreshold) {
			// first call service with circuit breaker then handle result in
			// async mode
			// note here we are on 50 ms timeout
			final ActorRef sender = getSender();

			circuitBreaker.callWithCircuitBreakerCS(() -> CompletableFuture.supplyAsync(() -> {
				return approvalService.approve(request.code, request.size);
			}, getContext().dispatcher())).whenCompleteAsync((b, t) -> {
				if (null != t) {
					if (t instanceof TimeoutException) {
						sender.tell(new BuySellResponse(false, null, "Approval resulted in timeout."), getSelf());
					} else {
						sender.tell(new BuySellResponse(false, null, "Error during approval. " + t.getMessage()),
								getSelf());
					}
				} else {
					if (b) {
						executeBuy(request, sender);
					} else {
						sender.tell(new BuySellResponse(false, null, "Transaction not approved."), getSelf());
					}
				}
			}, getContext().dispatcher());
		} else {
			executeBuy(request, getSender());
		}
	}

	/**
	 * When approved does the buy.s
	 */
	private void executeBuy(BuyRequest request, ActorRef sender) {
		StockInfo stockInfo = codeToStock.get(request.code);
		double cost = stockInfo.price * request.size;
		if (cost > balanceState.getTotal()) {
			getSender().tell(new BuySellResponse(false, null, "Insufficient balance."), getSelf());
			return;
		}

		BalanceUpdateEvent balanceUpdateEvent = new BalanceUpdateEvent(-cost);
		persist(balanceUpdateEvent, e -> {
			balanceState.update(e);
			stockInfo.holding += request.size;
			sender.tell(new BuySellResponse(true, stockInfo), getSelf());
		});
	}

	/**
	 * Executes buy request.
	 */
	private void sellRequest(SellRequest request) {
		StockInfo stockInfo = codeToStock.get(request.code);
		if (null == stockInfo) {
			getSender().tell(new BuySellResponse(false, null, "Wrong stock code."), getSelf());
			return;
		}
		if (request.size <= 0) {
			getSender().tell(new BuySellResponse(false, null, "Wrong sell size."), getSelf());
			return;
		}

		if (request.size > stockInfo.holding) {
			getSender().tell(new BuySellResponse(false, null, "Insufficient stock holding."), getSelf());
			return;
		}

		double cost = stockInfo.price * request.size;
		BalanceUpdateEvent balanceUpdateEvent = new BalanceUpdateEvent(cost);
		persist(balanceUpdateEvent, (BalanceUpdateEvent e) -> {
			balanceState.update(balanceUpdateEvent);
			stockInfo.holding -= request.size;
			getSender().tell(new BuySellResponse(true, stockInfo), getSelf());
			
			// optionally save snapshot
			// saveSnapshot(balanceState);
		});
	}
	
	/**
	 * Updates price of a single stock.
	 */
	private void priceUpdate(PriceUpdate update) {
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

}
