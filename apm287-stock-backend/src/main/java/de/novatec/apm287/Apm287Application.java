package de.novatec.apm287;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.novatec.apm287.akka.service.AkkaStockService;
import de.novatec.apm287.common.service.IStockService;
import de.novatec.apm287.java.service.JavaConcurrentStockService;
import de.novatec.apm287.java.service.JavaSyncStockService;

@Configuration
@SpringBootApplication
public class Apm287Application {

	public static void main(String[] args) {
		SpringApplication.run(Apm287Application.class, args);
	}

	@Bean
	IStockService getStockService(@Value("${apm287.service}") String service) {
		switch (service) {
		case "akka":
			return new AkkaStockService();
		case "java-sync":
			return new JavaSyncStockService();
		case "java-concurrent":
			return new JavaConcurrentStockService();
		default:
			return null;
		}
	}

}
