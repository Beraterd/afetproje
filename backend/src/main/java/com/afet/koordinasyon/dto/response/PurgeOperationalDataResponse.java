package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PurgeOperationalDataResponse {

    private int deletedEventVolunteers;
    private int deletedEvents;
    private int deletedDamagePhotos;
    private int deletedDamageAssessments;
    private int deletedResourceRequests;
    private int deletedUsers;

    /** Kritik olmayan uyarılar (fiziksel dosya bulunamadı vb.) */
    private List<String> warnings;
}
