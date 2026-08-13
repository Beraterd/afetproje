package com.afet.koordinasyon.dto.response;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        List<SearchResultItem> results
) {}
