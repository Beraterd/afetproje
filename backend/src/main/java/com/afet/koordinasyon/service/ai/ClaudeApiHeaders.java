package com.afet.koordinasyon.service.ai;

public final class ClaudeApiHeaders {
    private ClaudeApiHeaders() {}

    public static String[] create(String apiKey, String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            return new String[]{"x-api-key", apiKey, "anthropic-version", "2023-06-01"};
        }
        return new String[]{"x-api-key", apiKey, "anthropic-version", "2023-06-01",
                "anthropic-workspace-id", workspaceId.trim()};
    }
}
