package com.example.mathsolution.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DatabricksTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        String scope
) {
}

