package de.novatec.apm287.akka.stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import akka.stream.Attributes;
import akka.stream.Outlet;
import akka.stream.SourceShape;
import akka.stream.stage.AbstractOutHandler;
import akka.stream.stage.GraphStage;
import akka.stream.stage.GraphStageLogic;
import de.novatec.apm287.common.PriceUpdate;
import de.novatec.apm287.common.Util;

public class PriceUpdateGraphStage extends GraphStage<SourceShape<PriceUpdate>> {
	
	public final Outlet<PriceUpdate> out = Outlet.create("PriceSource.out");
	
	private final SourceShape<PriceUpdate> shape = SourceShape.of(out);

	@Override
	public SourceShape<PriceUpdate> shape() {
		return shape;
	}

	@Override
	public GraphStageLogic createLogic(Attributes arg0) throws Exception {
		return new GraphStageLogic(shape()) {
			
			/**
			 * Codes to generate prices for.
			 */
			private String[] codes;

			/**
			 * Price for each code.
			 */
			private double[] prices;
			
			{
				codes = new String[5];
				prices = new double[codes.length];
				for (int i = 0; i < codes.length; i++) {
					codes[i] = RandomStringUtils.randomAlphabetic(3).toUpperCase();
					prices[i] = 100.0d;
				}
				
				
				setHandler(out, new AbstractOutHandler() {
					
					@Override
					public void onPull() throws Exception {
						int i = RandomUtils.nextInt(0, codes.length);
						prices[i] = Util.getUpdatedPrice(prices[i]);
						PriceUpdate priceUpdate = new PriceUpdate(codes[i], prices[i]);
						push(out, priceUpdate);
					}
				});
			}
		};
	}

}
