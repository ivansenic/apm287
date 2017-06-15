package de.novatec.apm287.akka.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import akka.actor.ActorRef;
import akka.pattern.PatternsCS;
import akka.util.Timeout;
import de.novatec.apm287.akka.messages.Message.BalanceRequest;
import de.novatec.apm287.akka.messages.Message.BuyRequest;
import de.novatec.apm287.akka.messages.Message.OverviewRequest;
import de.novatec.apm287.akka.messages.Message.SellRequest;
import de.novatec.apm287.common.service.IStockService;

public class AkkaStockService implements IStockService {
	
	@Qualifier("stock-service-actor")
	@Autowired
	ActorRef stockServiceActor;

	@Override
	public CompletableFuture<Object> balance() {
		CompletionStage<Object> ask = PatternsCS.ask(stockServiceActor, new BalanceRequest(), Timeout.apply(1000, TimeUnit.MILLISECONDS));
		return ask.toCompletableFuture();
	}

	@Override
	public CompletableFuture<Object> stockInfo() {
		CompletionStage<Object> ask = PatternsCS.ask(stockServiceActor, new OverviewRequest(), Timeout.apply(1000, TimeUnit.MILLISECONDS));
		return ask.toCompletableFuture();
	}
	
	@Override
	public CompletableFuture<Object> buy(String stock, int size) {
		CompletionStage<Object> ask = PatternsCS.ask(stockServiceActor, new BuyRequest(stock, size), Timeout.apply(1000, TimeUnit.MILLISECONDS));
		return ask.toCompletableFuture();
	}
	
	@Override
	public CompletableFuture<Object> sell(String stock, int size) {
		CompletionStage<Object> ask = PatternsCS.ask(stockServiceActor, new SellRequest(stock, size), Timeout.apply(1000, TimeUnit.MILLISECONDS));
		return ask.toCompletableFuture();
	}

}
