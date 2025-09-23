package com.example.cloud_prog_lab1.controller;

import com.example.cloud_prog_lab1.domain.Greeting;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicLong;

@RestController
public class MainController {
    private final AtomicLong counter = new AtomicLong();

    @GetMapping("/greeting")
    public Greeting greeting() {
        return new Greeting(counter.incrementAndGet(), "Hello!");
    }
}
