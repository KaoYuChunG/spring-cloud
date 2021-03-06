package com.kao.bascis;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

@RestController
public class ReliabilityController {

    private final Map<String, AtomicInteger> countsOfErrors = new ConcurrentHashMap<>();

    private final Map<Long, AtomicInteger> countsPerSecond = new ConcurrentHashMap<>();

    @GetMapping("/hello")
    String hello() {

        var now = System.currentTimeMillis();
        var second = (Long)(now/1000);
        var countForTheCurrentSecond =
                this.countsPerSecond.compute(second, (aLong, atomicInteger) -> {
            if(atomicInteger == null ) atomicInteger = new AtomicInteger(0);
            atomicInteger.incrementAndGet();
            return atomicInteger;
        });
        return "hello" + countForTheCurrentSecond.get();
    }

    @GetMapping("/error/{id}")
    ResponseEntity<?> error (@PathVariable String id ) {

        var result = this.countsOfErrors.compute(id, (s, atomicInteger) ->{
            if( null == atomicInteger) atomicInteger = new AtomicInteger(0);
            atomicInteger.incrementAndGet();
            return atomicInteger;
        }).get();

        if(result < 5 ) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else {
            return ResponseEntity.ok(Map.of("message", "deu certo"));
        }

    }
}
