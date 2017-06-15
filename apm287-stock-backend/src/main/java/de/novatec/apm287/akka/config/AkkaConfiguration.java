package de.novatec.apm287.akka.config;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.scaladsl.Source;
import de.novatec.apm287.akka.actors.PriceGeneratorActor;
import de.novatec.apm287.akka.actors.StockServiceActor;
import de.novatec.apm287.akka.stream.PriceUpdateGraphStage;
import de.novatec.apm287.common.PriceUpdate;
import de.novatec.apm287.common.service.ApprovalService;
import scala.concurrent.duration.Duration;

/**
 * Bean configuration for the akka service.
 * 
 * @author ise
 *
 */
@Configuration
public class AkkaConfiguration {

	private ActorSystem system;

	@SuppressWarnings("unused")
	@Autowired
	@Bean(name = "stock-service-actor")
	public ActorRef getStockServiceActor(ApprovalService approvalService) {
		// init system
		system = ActorSystem.create("akka-stock-exchange");
		

		// generate some codes
		String[] codes = new String[5];
		for (int i = 0; i < codes.length; i++) {
			codes[i] = RandomStringUtils.randomAlphabetic(3).toUpperCase();
		}

		final ActorRef stockServiceActor = system.actorOf(StockServiceActor.props(Optional.empty(), approvalService), "stockServiceActor");
		final ActorRef priceGeneratorActor = system.actorOf(PriceGeneratorActor.props(stockServiceActor, codes), "priceGeneratorActor");

		// streaming approach
		Materializer mat = ActorMaterializer.create(system);
		PriceUpdateGraphStage priceUpdateGraphStage = new PriceUpdateGraphStage();
		final Source<PriceUpdate, NotUsed> source = Source.fromGraph(priceUpdateGraphStage);
		Flow<PriceUpdate, PriceUpdate, NotUsed> flow = Flow.of(PriceUpdate.class).throttle(1,
				Duration.create(100, TimeUnit.MILLISECONDS), 1, ThrottleMode.shaping());
		Sink<PriceUpdate, NotUsed> sink = Sink.actorRef(stockServiceActor, "completed");
		// source.via(flow).runWith(sink, mat);

		return stockServiceActor;
	}

	@PreDestroy
	public void terminate() {
		system.terminate();
	}
}
