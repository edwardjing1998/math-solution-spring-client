package com.example.mathsolution.client;

import com.example.mathsolution.config.DatabricksProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DatabricksTokenService {

    private final DatabricksProperties properties;
    private final WebClient webClient;
    private final Clock clock;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    @Autowired
    public DatabricksTokenService(
            DatabricksProperties properties,
            WebClient.Builder builder
    ) {
        this(
                properties,
                builder,
                Clock.systemUTC()
        );
    }

    /*
     * Package-private constructor used by unit tests,
     * allowing tests to provide a fixed Clock.
     */
    DatabricksTokenService(
            DatabricksProperties properties,
            WebClient.Builder builder,
            Clock clock
    ) {
        this.properties = properties;

        this.webClient = builder
                .baseUrl(properties.host())
                .build();

        this.clock = clock;
    }

    public String getAccessToken() {
        if (tokenIsValid()) {
            return cachedToken;
        }

        synchronized (this) {
            if (tokenIsValid()) {
                return cachedToken;
            }

            return requestNewToken();
        }
    }

    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }

    private boolean tokenIsValid() {
        return cachedToken != null
                && clock.instant().isBefore(
                expiresAt.minus(
                        properties.tokenRefreshMargin()
                )
        );
    }

    private String requestNewToken() {
        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add(
                "grant_type",
                "client_credentials"
        );

        form.add(
                "scope",
                "all-apis"
        );

        DatabricksTokenResponse response =
                webClient.post()
                        .uri("/oidc/v1/token")
                        .headers(headers ->
                                headers.setBasicAuth(
                                        properties.clientId(),
                                        properties.clientSecret()
                                )
                        )
                        .contentType(
                                MediaType
                                        .APPLICATION_FORM_URLENCODED
                        )
                        .body(
                                BodyInserters
                                        .fromFormData(form)
                        )
                        .retrieve()
                        .bodyToMono(
                                DatabricksTokenResponse.class
                        )
                        .block(
                                properties.requestTimeout()
                        );

        Objects.requireNonNull(
                response,
                "Databricks returned an empty OAuth response"
        );

        if (response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new IllegalStateException(
                    "Databricks returned no OAuth access token"
            );
        }

        cachedToken = response.accessToken();

        expiresAt = clock.instant().plusSeconds(
                response.expiresIn()
        );

        return cachedToken;
    }
}