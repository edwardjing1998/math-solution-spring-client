package com.example.mathsolution.client;

import com.example.mathsolution.config.DatabricksProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class MathSolutionClient {

    private final DatabricksTokenService tokenService;
    private final DatabricksProperties properties;
    private final WebClient webClient;

    public MathSolutionClient(
            DatabricksTokenService tokenService,
            DatabricksProperties properties,
            WebClient.Builder builder) {

        this.tokenService = tokenService;
        this.properties = properties;
        this.webClient = builder
                .baseUrl(properties.appUrl())
                .build();
    }

    public MathSolutionResponse createSolution(
            MathSolutionRequest request) {

        return call(request, true)
                .block(properties.requestTimeout());
    }

    private Mono<MathSolutionResponse> call(
            MathSolutionRequest request,
            boolean retryUnauthorized) {

        /*
         * DatabricksTokenService currently performs a blocking
         * WebClient operation. Execute it on boundedElastic so that
         * it never blocks a reactor-http-nio thread.
         */
        return Mono.fromCallable(tokenService::getAccessToken)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(token ->
                        executeRequest(
                                request,
                                token,
                                retryUnauthorized
                        )
                );
    }

    private Mono<MathSolutionResponse> executeRequest(
            MathSolutionRequest request,
            String token,
            boolean retryUnauthorized) {

        return webClient.post()
                .uri("/api/v1/solutions")
                .headers(headers ->
                        headers.setBearerAuth(token))
                .bodyValue(request)
                .exchangeToMono(response -> {
                    HttpStatusCode status =
                            response.statusCode();

                    if (status.is2xxSuccessful()) {
                        return response.bodyToMono(
                                MathSolutionResponse.class
                        );
                    }

                    if (status.value() == 401
                            && retryUnauthorized) {

                        tokenService.invalidate();

                        return response.releaseBody()
                                .then(call(request, false));
                    }

                    return response.bodyToMono(String.class)
                            .defaultIfEmpty(
                                    "<empty response body>"
                            )
                            .flatMap(responseBody ->
                                    Mono.error(
                                            new DatabricksAppException(
                                                    status.value(),
                                                    responseBody
                                            )
                                    )
                            );
                });
    }
}