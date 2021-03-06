package com.kao.customers.controller;

import com.kao.customers.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.awt.*;

@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final Flux<Customer> customerFlux;

    @GetMapping(
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            value = "/customers"
    )
    Flux<Customer> getAll() {
        return this.customerFlux;
    }
}
