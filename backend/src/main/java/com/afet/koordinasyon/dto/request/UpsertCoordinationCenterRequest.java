package com.afet.koordinasyon.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpsertCoordinationCenterRequest {

    @NotBlank(message = "Adres zorunludur")
    private String address;

    private String streetName;

    private String buildingNo;

    @NotNull(message = "Enlem zorunludur")
    @DecimalMin(value = "36.0", message = "Enlem geçersiz (İstanbul sınırları dışında)")
    @DecimalMax(value = "42.0", message = "Enlem geçersiz (İstanbul sınırları dışında)")
    private Double latitude;

    @NotNull(message = "Boylam zorunludur")
    @DecimalMin(value = "26.0", message = "Boylam geçersiz (İstanbul sınırları dışında)")
    @DecimalMax(value = "30.0", message = "Boylam geçersiz (İstanbul sınırları dışında)")
    private Double longitude;

    @NotNull(message = "Konum onayı zorunludur")
    private Boolean locationVerified;
}
