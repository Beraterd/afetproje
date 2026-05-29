package com.afet.koordinasyon.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PurgeOperationalDataRequest {

    private boolean purgeEvents;
    private boolean purgeDamageAssessments;
    private boolean purgeResourceRequests;
    private boolean purgeUsers;

    /** Silinmeyen (korunan) admin e-posta adresleri. Boş ise uygulama varsayılanı kullanılır. */
    private List<String> protectedEmails;

    /** Onay metni — tam olarak "PURGE" olmalıdır. */
    private String confirmationText;
}
