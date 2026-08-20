package com.example.mathsolution.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "databricks")
public record DatabricksProperties(
        @NotBlank String host,
        @NotBlank String appUrl,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotNull Duration requestTimeout,
        @NotNull Duration tokenRefreshMargin
) {
}

