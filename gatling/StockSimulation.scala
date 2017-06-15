package de.novatec.apm287.gatling

import scala.concurrent.duration._
import scala.util._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class StockSimulation extends Simulation {

	val httpProtocol = http
		.baseURL("http://localhost:8080")
		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate, sdch")
		.acceptLanguageHeader("en-US,en;q=0.8,de;q=0.6,hr;q=0.4,sr;q=0.2")
		.userAgentHeader("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.90 Safari/537.36")

	val headers_0 = Map("Upgrade-Insecure-Requests" -> "1")
	val rnd = new scala.util.Random

	val scn = scenario("StockSimulation")
		.exec(http("Overview")
			.get("/stocks")
			.headers(headers_0)
			.check(
      			jsonPath("$.stockPrices[" + 0 + "].code").saveAs("code")
    		))
		//.pause(100 milliseconds)
		.exec(http("Balance")
			.get("/balance")
			.headers(headers_0))
		//.pause(100 milliseconds)
		.exec(http("Buy")
			.get("/buy?c=${code}&s=10")
			.headers(headers_0))
		//.pause(100 milliseconds)
		.exec(http("Sell")
			.get("/sell?c=${code}&s=10")
			.headers(headers_0))

	setUp(scn.inject(constantUsersPerSec(400) during(60 seconds) randomized)).protocols(httpProtocol)
}