package com.example.mathsolution.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToController;

import com.example.mathsolution.client.MathSolutionClient;
import com.example.mathsolution.client.MathSolutionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

class MathSolutionControllerTest {

    private WebTestClient client;
    private MathSolutionClient mathSolutionClient;

    @BeforeEach
    void setUp() {
        mathSolutionClient = org.mockito.Mockito.mock(MathSolutionClient.class);
        client = bindToController(new MathSolutionController(mathSolutionClient))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void forwardsValidRequest() {
        when(mathSolutionClient.createSolution(any())).thenReturn(new MathSolutionResponse(
                "completed",
                "uploads/gemma3_math_problem.png",
                "temporary/gemma3_math_solution_from_spring.html",
                "text/html; charset=utf-8"
        ));

        client.post()
                .uri("/api/math-solutions")
                .header("Content-Type", "application/json")
                .bodyValue("""
                        {
                          "input_blob_path": "uploads/gemma3_math_problem.png",
                          "output_blob_path": "temporary/gemma3_math_solution_from_spring.html",
                          "language": "zh-CN"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("completed")
                .jsonPath("$.output_blob_path")
                .isEqualTo("temporary/gemma3_math_solution_from_spring.html");
    }
}

