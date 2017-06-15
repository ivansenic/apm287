package de.novatec.apm287.akka.actors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.CircuitBreaker;
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

public class StockServiceActor extends AbstractActor {

	/**
	 * @return props for creating this actor.
	 */
	public static Props props(Optional<Double> balance, ApprovalService approvalService) {
		return Props.create(StockServiceActor.class, () -> new StockServiceActor(balance, approvalService));
	}

	/**
	 * Default balance to start with is 10.000.
	 */
	private double balance = 10_000;

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
			this.balance = balance.get().doubleValue();
		}
		this.approvalService = approvalService;
		this.circuitBreaker = new CircuitBreaker(getContext().dispatcher(), getContext().system().scheduler(), 5,
				Duration.create(50, TimeUnit.MILLISECONDS), Duration.create(1, TimeUnit.MINUTES));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(PriceUpdate.class, this::priceUpdate)
			.match(OverviewRequest.class, r -> {
				getSender().tell(new OverviewReply(Collections.unmodifiableCollection(codeToStock.values())), getSelf());
			})
			.match(BalanceRequest.class, r -> {
				BalanceInfo balanceInfo = new BalanceInfo(balance, getExposure());
				getSender().tell(new BalanceReply(balanceInfo), getSelf());
			})
			.match(BuyRequest.class, this::buyRequest).match(SellRequest.class, this::sellRequest)
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
			// first call service with circuit breaker then handle result in async mode
			// note here we are on 50 ms timeout
			final ActorRef sender = getSender();
			
			circuitBreaker.callWithCircuitBreakerCS(() -> 
				CompletableFuture.supplyAsync(() -> {
					return approvalService.approve(request.code, request.size);
				}, getContext().dispatcher())
			).whenCompleteAsync((b, t) -> {
				if (null != t) {
					if (t instanceof TimeoutException) {
						sender.tell(new BuySellResponse(false, null, "Approval resulted in timeout."), getSelf());
					} else {
						sender.tell(new BuySellResponse(false, null, "Error during approval. " + t.getMessage()), getSelf());
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
		if (cost > balance) {
			getSender().tell(new BuySellResponse(false, null, "Insufficient balance."), getSelf());
			return;
		}

		balance -= cost;
		stockInfo.holding += request.size;

		sender.tell(new BuySellResponse(true, stockInfo), getSelf());
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
		balance += cost;
		stockInfo.holding -= request.size;

		getSender().tell(new BuySellResponse(true, stockInfo), getSelf());
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
