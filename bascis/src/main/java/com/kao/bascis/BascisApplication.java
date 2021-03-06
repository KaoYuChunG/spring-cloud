package com.kao.bascis;


import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.CachingRouteLocator;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
public class BascisApplication {

	public static void main(String[] args) {
		SpringApplication.run(BascisApplication.class, args);
	}

	private final AtomicBoolean ws = new AtomicBoolean(false);


//	再用另一個分離的yaml的檔案來管理的案例
//	@Bean
//	@RefreshScope
//	RouteLocator gateway(RouteLocatorBuilder rlb) {
//		var id = "customers";
//		if(!this.ws.get()) {
//			this.ws.set(true);
//			return rlb.routes()
//					.route(id, rs -> rs
//							.path("/customers")
//							.uri("http://localhost:8181/customers")
//					).build();
//		} else {
//			this.ws.set(true);
//			return rlb.routes()
//					.route(id, rs -> rs
//							.path("/ws/customers")
//							.uri("http://localhost:8181/customers")
//					).build();
//		}
//	}

	//show event 的情況
	@Bean
	ApplicationListener<RefreshRoutesResultEvent> routesRefreshed() {
		return new ApplicationListener<RefreshRoutesResultEvent>() {
			@Override
			public void onApplicationEvent(RefreshRoutesResultEvent rre) {
				var crl = (CachingRouteLocator)rre.getSource();
				Flux<Route> routes = crl.getRoutes();
				routes.subscribe(System.out::println);
			}
		};
	}
	
	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(5, 10);
	}

	@Bean
	RouteLocator gateway(RouteLocatorBuilder rlb) {
		return rlb
				.routes()
				.route(rs -> rs.path("/hello")
					.filters(
						fs -> fs.requestRateLimiter(rlc -> rlc
							.setRateLimiter(redisRateLimiter())
							.setKeyResolver(new KeyResolver() {
								@Override
								public Mono<String> resolve(ServerWebExchange exchange) {
//									return Mono.just("count");
									return  exchange.getPrincipal().map(principal -> principal.getName()).switchIfEmpty(Mono.empty());
								}
							})
						)
					)
					.uri("http://localhost:8181/customers")
				)
				.route(rs -> rs.path("/default")
					.filters(
						fs -> fs.filter((exchange, chain) -> chain.filter(exchange))
					)
					.uri("https:spring.io/guides")
				)
				.route(rs -> rs
					.path("/customers")
					.filters(fs -> fs.circuitBreaker(cbc -> cbc.setFallbackUri("forward:/default")))
					.uri("http://localhost:8181/customers")

				)
				.route(rs -> rs
					.path("/error/**")
					.filters(fs -> fs.retry(5))
					.uri("http://localhost:8181/customers")
				).build();
	}

//	@Bean
//	RouteLocator gateway(RouteLocatorBuilder rlb) {
//		return rlb
//				.routes()
//				.route( routeSpec -> routeSpec
//				    	.path("/hello")
//						.and().host("*.spring.io")
//						這部分有很多可以設定的東西
//						.and()
//						.after(ZonedDateTime.now())
//						.asyncPredicate(serverWebExchange -> Mono.just(serverWebExchange.getAttribute("foo")))
//						.filters( gatewayFilterSpec ->
//								gatewayFilterSpec.setPath("/guides"))
//						.uri("https://spring.io")
//				)
//				.route( "twitter",routeSpec -> routeSpec
//						.path("/twitter/**")
//						.filters( fs ->
//								fs.rewritePath("/twitter/(?<handle>.*)",
//										"/${handle}"
//										))
//						.uri("http://twitter.com/@")
//				)
//
//				.build();

//		=====================================================
//		return rlb
//				.routes()
//				.route( routeSpec -> routeSpec
//						.path("/customers")
//						.uri("http://localhost:8181/customers"))
//				.build();
//		=====================================================
//	}
	@Bean
	RouteLocator gateway(SetPathGatewayFilterFactory ff) {
		var singleRoute = Route.async()
				.id("test-route")
				.filter(new OrderedGatewayFilter( ff.apply(config -> config.setTemplate("/customers")),1))
				.uri("http://localhost:8181/customers")
				.asyncPredicate(new AsyncPredicate<ServerWebExchange>() {
					@Override
					public Publisher<Boolean> apply(ServerWebExchange serverWebExchange) {
						var uri = serverWebExchange.getRequest().getURI();
						var path = uri.getPath();
						var match = path.contains("/customers");
						return Mono.just(match);
					}
				}).build();
		return () -> Flux.just(singleRoute);
	}

}
