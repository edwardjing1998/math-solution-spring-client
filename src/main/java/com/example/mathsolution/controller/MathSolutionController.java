package com.example.mathsolution.controller;

import com.example.mathsolution.client.MathSolutionClient;
import com.example.mathsolution.client.MathSolutionRequest;
import com.example.mathsolution.client.MathSolutionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/math-solutions")
public class MathSolutionController {

    private final MathSolutionClient client;

    public MathSolutionController(MathSolutionClient client) {
        this.client = client;
    }

    @PostMapping
    public Mono<ResponseEntity<MathSolutionResponse>> create(
            @Valid @RequestBody MathSolutionRequest request) {

        return Mono.fromCallable(
                        () -> client.createSolution(request)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .map(ResponseEntity::ok);
    }
}