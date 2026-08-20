package com.example.mathsolution.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mathsolution.config.DatabricksProperties;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class DatabricksTokenServiceTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void cachesTokenUntilRefreshWindow() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "access_token": "test-access-token",
                          "token_type": "Bearer",
                          "expires_in": 3600,
                          "scope": "all-apis"
                        }
                        """));

        DatabricksProperties properties = new DatabricksProperties(
                server.url("/").toString(),
                "https://app.example.com",
                "test-client",
                "test-secret",
                Duration.ofSeconds(10),
                Duration.ofMinutes(5)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC);
        DatabricksTokenService service = new DatabricksTokenService(
                properties,
                WebClient.builder(),
                clock
        );

        assertThat(service.getAccessToken()).isEqualTo("test-access-token");
        assertThat(service.getAccessToken()).isEqualTo("test-access-token");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }
}

