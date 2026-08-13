package com.afet.koordinasyon.storage;

import com.afet.koordinasyon.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Upload güvenliği (item 46) için paylaşılan kontroller: izin verilen MIME/extension
 * whitelist'i, magic-byte (dosya imzası) doğrulaması ve dosya adı sanitization.
 *
 * Storage path'e ASLA kullanıcının verdiği dosya adı doğrudan yazılmaz — çağıran servisler
 * yalnızca {@link #safeExtension} ile doğrulanmış bir uzantıyı, server tarafından üretilen
 * bir UUID ile birleştirerek storage key oluşturmalıdır (path traversal'ı kökten engeller).
 * Kullanıcıya gösterilecek/Content-Disposition'a yazılacak dosya adı için {@link #sanitizeDisplayFileName}
 * kullanılmalıdır (satır sonu, tırnak, path ayırıcı karakterlerinden arındırılmış).
 */
@Component
public class FileUploadValidator {

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF },
            "image/png", new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A },
            "application/pdf", new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D } // "%PDF-"
    );

    private static final Map<String, String> EXTENSION_BY_MIME = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf"
    );

    /**
     * Declared content-type'ın izin verilen listede olduğunu VE (mümkünse) dosyanın ilk
     * byte'larının o türle eşleştiğini doğrular. WebP için basit bir RIFF/WEBP kontrolü ayrı
     * yapılır (sabit-offset imza yok).
     */
    public void validateContent(byte[] content, String declaredMimeType, Set<String> allowedMimeTypes) {
        if (content == null || content.length == 0) {
            throw new BusinessRuleException("Dosya boş olamaz");
        }
        if (declaredMimeType == null || !allowedMimeTypes.contains(declaredMimeType)) {
            throw new BusinessRuleException(
                    "Desteklenmeyen dosya türü. İzin verilenler: " + String.join(", ", allowedMimeTypes));
        }

        if ("image/webp".equals(declaredMimeType)) {
            if (!isWebp(content)) {
                throw new BusinessRuleException("Dosya içeriği belirtilen WebP formatıyla eşleşmiyor.");
            }
            return;
        }

        byte[] signature = MAGIC_BYTES.get(declaredMimeType);
        if (signature != null && !startsWith(content, signature)) {
            throw new BusinessRuleException(
                    "Dosya içeriği belirtilen dosya türüyle eşleşmiyor (sahte Content-Type olabilir).");
        }
    }

    /** Storage key'e gömülecek, server-generated + whitelist'ten gelen güvenli uzantı. */
    public String safeExtension(String declaredMimeType) {
        String ext = EXTENSION_BY_MIME.get(declaredMimeType);
        if (ext == null) {
            throw new BusinessRuleException("Desteklenmeyen dosya türü.");
        }
        return ext;
    }

    /** Kullanıcıya gösterilecek/Content-Disposition'a yazılacak dosya adı — sadece görüntü amaçlı. */
    public String sanitizeDisplayFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "dosya";
        }
        // Path ayırıcılarını at, yalnızca son segmenti al (path traversal / gömülü dizin girişimi).
        String name = originalFileName.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        // Unicode normalize + kontrol karakterlerini ve header-injection riski taşıyan
        // karakterleri (", \r, \n) at.
        name = Normalizer.normalize(name, Normalizer.Form.NFC);
        name = name.replaceAll("[\\r\\n\"\\p{Cntrl}]", "");
        name = name.replaceAll("^\\.+", ""); // baştaki ".."/"." gizli dosya girişimleri
        if (name.isBlank()) {
            name = "dosya";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        return Arrays.equals(content, 0, prefix.length, prefix, 0, prefix.length);
    }

    private boolean isWebp(byte[] content) {
        if (content.length < 12) {
            return false;
        }
        boolean riff = content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F';
        boolean webp = content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P';
        return riff && webp;
    }
}
