package com.kao.customers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SpringBootApplication
public class CustomersApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomersApplication.class, args);
	}
	
	private final String [] names = "Jean, Mario, Gustavo, Java, John, Jose".split(",");

	private final AtomicInteger counter = new AtomicInteger();

	private final Flux<Customer> customers = Flux.fromStream(
			Stream.generate(() -> {
				var id = counter.incrementAndGet();

				return new Customer(id, names [ id % names.length]);
			}))
			.delayElements(Duration.ofSeconds(3));
	@Bean
	Flux<Customer> customers() {
		return this.customers.publish().autoConnect();
	}
}
