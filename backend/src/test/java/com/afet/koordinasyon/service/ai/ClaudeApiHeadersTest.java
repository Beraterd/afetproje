package com.afet.koordinasyon.service.ai;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.HttpRequest;
import static org.junit.jupiter.api.Assertions.*;

class ClaudeApiHeadersTest {
    @Test
    void includesWorkspaceForIdentityLinkedKey() {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                .headers(ClaudeApiHeaders.create("test-key", " wrkspc_test ")).GET().build();
        assertEquals("wrkspc_test", request.headers().firstValue("anthropic-workspace-id").orElseThrow());
        assertEquals("test-key", request.headers().firstValue("x-api-key").orElseThrow());
        assertEquals("2023-06-01", request.headers().firstValue("anthropic-version").orElseThrow());
    }

    @Test
    void preservesSingleWorkspaceKeyCompatibility() {
        for (String workspace : new String[]{null, "", "  "}) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .headers(ClaudeApiHeaders.create("test-key", workspace)).GET().build();
            assertTrue(request.headers().firstValue("anthropic-workspace-id").isEmpty());
            assertEquals("test-key", request.headers().firstValue("x-api-key").orElseThrow());
        }
    }
}
