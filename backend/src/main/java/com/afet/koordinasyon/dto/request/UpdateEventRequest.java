package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEventRequest {

    @Size(max = 300)
    private String title;

    private String description;

    @Min(1)
    private Integer requiredPeople;
}
