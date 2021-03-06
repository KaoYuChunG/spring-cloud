package com.kao.customers.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kao.customers.Customer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Flux;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfiguration {

    private final ObjectMapper objectMapper;

    //轉成json
    @SneakyThrows
    private String from(Customer customer) {
        return this.objectMapper.writeValueAsString(customer);
    }

    @Bean
    WebSocketHandler webSocketHandler(Flux<Customer> customerFlux) {
        return webSocketSession -> {

            //由此對資訊進行處裡並寄出
            var map = customerFlux
                    .map(customer -> from(customer))
                    .map(webSocketSession::textMessage);
            return webSocketSession.send(map);

        };
    }

    @Bean
    SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler customerWsh) {
        return new SimpleUrlHandlerMapping(Map.of("/ws/customers", customerWsh), 10);
    }
}
