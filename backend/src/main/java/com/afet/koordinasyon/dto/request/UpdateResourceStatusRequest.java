package com.afet.koordinasyon.dto.request;

import com.afet.koordinasyon.domain.enums.ResourceRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateResourceStatusRequest {
    @NotNull
    private ResourceRequestStatus status;
}
