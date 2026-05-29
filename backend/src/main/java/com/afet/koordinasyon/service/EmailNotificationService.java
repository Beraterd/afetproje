package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.*;
import com.afet.koordinasyon.domain.enums.*;
import com.afet.koordinasyon.notification.EmailNotificationProvider;
import com.afet.koordinasyon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Istanbul"));

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username:noreply@afetkoordinasyon.tr}")
    private String fromAddress;

    private final EmailNotificationProvider emailProvider;
    private final EmailNotificationLogRepository emailLogRepository;
    private final UserRepository userRepository;
    private final DamageAssessmentRepository damageAssessmentRepository;
    private final EventRepository eventRepository;
    private final ResourceRequestRepository resourceRequestRepository;
    private final DistrictRepository districtRepository;
    private final NeighborhoodRepository neighborhoodRepository;
    private final EventVolunteerRepository eventVolunteerRepository;
    private final DocumentRepository documentRepository;

    // ── Hasar Bildirimi ───────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDamageReportCreated(UUID damageAssessmentId, UUID reporterId) {
        DamageAssessment da = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
        if (da == null) return;
        User reporter = userRepository.findById(reporterId).orElse(null);
        if (reporter == null) return;

        if (reporter.isEmailDamageNotificationsEnabled()) {
            String subject = "Hasar bildiriminiz alındı";
            String body = buildHtml(subject,
                    "Hasar bildiriminiz sisteme kaydedilmiştir.",
                    buildDamageTable(da),
                    frontendBaseUrl + "/damage",
                    "Hasar Bildirimlerini Görüntüle");
            sendAndLog(reporter, subject, body, EmailNotificationType.DAMAGE_REPORT_CREATED,
                    "DamageAssessment", damageAssessmentId.toString());
        }

        // Koordinatöre bildir
        notifyDamageCoordinators(da, damageAssessmentId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDamageReportStatusChanged(UUID damageAssessmentId, VerificationStatus newStatus) {
        DamageAssessment da = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
        if (da == null) return;
        User reporter = da.getReportedBy();
        if (reporter == null) return;

        if (!reporter.isEmailDamageNotificationsEnabled()) return;

        EmailNotificationType type;
        String subject;
        String extra;

        switch (newStatus) {
            case KOORDINATOR_ONAYLADI -> {
                type = EmailNotificationType.DAMAGE_REPORT_APPROVED;
                subject = "Hasar bildiriminiz koordinatör tarafından onaylandı";
                extra = "<p>Hasar bildiriminiz yetkili koordinatör tarafından incelenmiş ve onaylanmıştır.</p>";
            }
            case SAHADA_DOGRULANDI -> {
                type = EmailNotificationType.DAMAGE_REPORT_STATUS_CHANGED;
                subject = "Hasar bildiriminiz sahada doğrulandı";
                extra = "<p>Hasar bildiriminiz saha ekibi tarafından yerinde incelenmiştir.</p>";
            }
            case ASSIGNED -> {
                type = EmailNotificationType.DAMAGE_REPORT_FIELD_TEAM_ASSIGNED;
                subject = "Hasar tespit için saha ekibi yönlendirildi";
                extra = "<p>Hasar bildiriminiz için bir saha ekibi yönlendirilmiştir. Ekip en kısa sürede bölgenize ulaşacaktır.</p>";
            }
            default -> {
                type = EmailNotificationType.DAMAGE_REPORT_STATUS_CHANGED;
                subject = "Hasar bildiriminizin durumu güncellendi";
                extra = "<p>Hasar bildiriminizin durumu: <strong>" + newStatus.getLabel() + "</strong></p>";
            }
        }

        String body = buildHtml(subject,
                extra + buildDamageTable(da),
                "",
                frontendBaseUrl + "/damage",
                "Hasar Bildirimlerini Görüntüle");
        sendAndLog(reporter, subject, body, type, "DamageAssessment", damageAssessmentId.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDamageReportFieldTeamAssigned(UUID damageAssessmentId, UUID assigneeId) {
        DamageAssessment da = damageAssessmentRepository.findById(damageAssessmentId).orElse(null);
        if (da == null) return;
        User assignee = userRepository.findById(assigneeId).orElse(null);
        if (assignee == null) return;

        if (!assignee.isEmailDamageNotificationsEnabled()) return;

        String subject = "Size hasar tespit görevi atandı";
        String body = buildHtml(subject,
                "<p>Aşağıdaki hasar bildirimi için saha tespiti göreviniz bulunmaktadır.</p>"
                        + buildDamageTable(da),
                "",
                frontendBaseUrl + "/tasks",
                "Görevlerimi Görüntüle");
        sendAndLog(assignee, subject, body, EmailNotificationType.DAMAGE_REPORT_FIELD_TEAM_ASSIGNED,
                "DamageAssessment", damageAssessmentId.toString());
    }

    // ── Görev / Gönüllü ───────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendVolunteerJoinedTask(UUID eventId, UUID userId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        if (user.isEmailTaskNotificationsEnabled()) {
            String teamLabel = event.getTeam() != null ? event.getTeam().getName().getLabel() : "-";
            String districtName = (event.getNeighborhood() != null && event.getNeighborhood().getDistrict() != null)
                    ? event.getNeighborhood().getDistrict().getName() : "-";
            String neighborhoodName = event.getNeighborhood() != null ? event.getNeighborhood().getName() : "-";
            String subject = "Ekibe katılımınız gerçekleşti";
            String intro = "<p>" + esc(districtName) + " ilçesindeki " + esc(teamLabel) + " ekibine katılım sağladınız.</p>";
            String body = buildHtml(subject,
                    intro + buildEventTable(event),
                    "",
                    frontendBaseUrl + "/tasks",
                    "Görevlerimi Görüntüle");
            sendAndLog(user, subject, body, EmailNotificationType.VOLUNTEER_JOIN_RECEIVED,
                    "Event", eventId.toString());
        }

        // Etkinliği oluşturan kişiye bildir
        User creator = event.getCreatedBy();
        if (creator != null && !creator.getId().equals(userId) && creator.isEmailTaskNotificationsEnabled()) {
            String subject = "Yeni gönüllü görevinize katıldı";
            String coordBody = buildHtml(subject,
                    "<p><strong>" + user.getFirstName() + " " + user.getLastName()
                            + "</strong> aşağıdaki göreve katılım sağladı.</p>"
                            + buildEventTable(event),
                    "",
                    frontendBaseUrl + "/events/" + eventId,
                    "Olayı Görüntüle");
            sendAndLog(creator, subject, coordBody, EmailNotificationType.VOLUNTEER_JOIN_COORDINATOR_NOTICE,
                    "Event", eventId.toString());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendTaskCompleted(UUID eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return;

        List<EventVolunteer> volunteers = eventVolunteerRepository
                .findByEventIdAndStatus(eventId, EventVolunteerStatus.COMPLETED);

        String teamLabel = event.getTeam() != null ? event.getTeam().getName().getLabel() : "-";
        String districtName = (event.getNeighborhood() != null && event.getNeighborhood().getDistrict() != null)
                ? event.getNeighborhood().getDistrict().getName() : "-";
        String completedAt = event.getUpdatedAt() != null ? DISPLAY_FMT.format(event.getUpdatedAt()) : "-";

        for (EventVolunteer ev : volunteers) {
            try {
                User user = ev.getUser();
                if (user == null || !user.isEmailTaskNotificationsEnabled()) continue;
                String subject = "Ekip görevi tamamlandı";
                String intro = "<p>" + esc(districtName) + " ilçesindeki " + esc(teamLabel)
                        + " görevi tamamlanmıştır.</p>"
                        + "<p>Katkılarınız için teşekkür ederiz.</p>";
                String tableHtml = buildEventTable(event)
                        + "<table style=\"border-collapse:collapse;width:100%;margin:4px 0;\">"
                        + tr(false, "Tamamlanma", completedAt)
                        + "</table>";
                String body = buildHtml(subject, intro + tableHtml, "",
                        frontendBaseUrl + "/tasks", "Görevlerimi Görüntüle");
                sendAndLog(user, subject, body, EmailNotificationType.TASK_COMPLETED,
                        "Event", eventId.toString());
            } catch (Exception e) {
                log.error("Görev tamamlama maili gönderilemedi (userId={}): {}", ev.getUser(), e.getMessage());
            }
        }
    }

    // ── Kaynak / Yardım Talebi ─────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendResourceRequestCreated(UUID resourceRequestId, UUID creatorId) {
        ResourceRequest rr = resourceRequestRepository.findById(resourceRequestId).orElse(null);
        if (rr == null) return;
        User creator = userRepository.findById(creatorId).orElse(null);
        if (creator == null) return;

        if (creator.isEmailAidNotificationsEnabled()) {
            String subject = "Kaynak talebiniz alındı";
            String body = buildHtml(subject,
                    "<p>Kaynak talebiniz sisteme iletilmiştir.</p>"
                            + buildResourceTable(rr),
                    "",
                    frontendBaseUrl + "/resources",
                    "Kaynak Taleplerini Görüntüle");
            sendAndLog(creator, subject, body, EmailNotificationType.AID_REQUEST_CREATED,
                    "ResourceRequest", resourceRequestId.toString());
        }

        // Koordinatöre bildir: mahalle koord > ilçe koord > admin önceliği
        String coordSubject = "Yeni kaynak talebi oluşturuldu";
        String coordContent = "<p>"
                + esc(creator.getFirstName() + " " + creator.getLastName())
                + " tarafından yeni bir kaynak talebi oluşturuldu.</p>"
                + buildResourceTable(rr);
        boolean notified = false;

        // 1. Mahalle koordinatörü
        if (rr.getNeighborhood() != null) {
            User nbCoord = rr.getNeighborhood().getCoordinator();
            if (nbCoord != null && nbCoord.isEmailAidNotificationsEnabled()) {
                String body = buildHtml(coordSubject, coordContent, "",
                        frontendBaseUrl + "/resources", "Talepleri Görüntüle");
                sendAndLog(nbCoord, coordSubject, body, EmailNotificationType.AID_REQUEST_COORDINATOR_NOTICE,
                        "ResourceRequest", resourceRequestId.toString());
                notified = true;
            }
        }

        // 2. İlçe koordinatörü (mahalle koord yoksa)
        if (!notified && rr.getDistrict() != null) {
            User distCoord = rr.getDistrict().getCoordinator();
            if (distCoord != null && distCoord.isEmailAidNotificationsEnabled()) {
                String body = buildHtml(coordSubject, coordContent, "",
                        frontendBaseUrl + "/resources", "Talepleri Görüntüle");
                sendAndLog(distCoord, coordSubject, body, EmailNotificationType.AID_REQUEST_COORDINATOR_NOTICE,
                        "ResourceRequest", resourceRequestId.toString());
                notified = true;
            }
        }

        // 3. Adminler (hiç koordinatör yoksa)
        if (!notified) {
            notifyAdmins(coordSubject, coordContent, EmailNotificationType.AID_REQUEST_COORDINATOR_NOTICE,
                    "ResourceRequest", resourceRequestId.toString());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendResourceRequestStatusChanged(UUID resourceRequestId, String newStatus) {
        ResourceRequest rr = resourceRequestRepository.findById(resourceRequestId).orElse(null);
        if (rr == null) return;
        User creator = rr.getCreatedBy();
        if (creator == null || !creator.isEmailAidNotificationsEnabled()) return;

        boolean fulfilled = "FULFILLED".equals(newStatus);
        String subject = fulfilled ? "Kaynak talebiniz tamamlandı" : "Kaynak talebi güncellendi";
        String intro = fulfilled
                ? "<p>Oluşturduğunuz kaynak talebi tamamlanmıştır.</p>"
                : "<p>Kaynak talebinizin durumu güncellendi: <strong>" + esc(newStatus) + "</strong></p>";
        EmailNotificationType type = fulfilled
                ? EmailNotificationType.AID_REQUEST_COMPLETED
                : EmailNotificationType.AID_REQUEST_STATUS_CHANGED;

        String body = buildHtml(subject, intro + buildResourceTable(rr), "",
                frontendBaseUrl + "/resources", "Kaynak Taleplerini Görüntüle");
        sendAndLog(creator, subject, body, type, "ResourceRequest", resourceRequestId.toString());
    }

    // ── Belge onay / red ──────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDocumentApproved(UUID documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;
        User owner = doc.getUser();
        if (owner == null || !owner.isEmailSystemNotificationsEnabled()) return;

        String typeLabel = doc.getDocumentType() != null ? doc.getDocumentType().name() : "-";
        String subject = "Belgeniz onaylandı";
        String content = "<p>Yüklediğiniz belge onaylanmıştır.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "Belge türü", typeLabel)
                + tr(false, "Durum", "Onaylandı")
                + "</table>";
        String body = buildHtml(subject, content, "", frontendBaseUrl + "/profile", "Profilimi Görüntüle");
        sendAndLog(owner, subject, body, EmailNotificationType.DOCUMENT_APPROVED,
                "Document", documentId.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDocumentRejected(UUID documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;
        User owner = doc.getUser();
        if (owner == null || !owner.isEmailSystemNotificationsEnabled()) return;

        String typeLabel = doc.getDocumentType() != null ? doc.getDocumentType().name() : "-";
        String subject = "Belgeniz reddedildi";
        String reasonRow = (doc.getRejectionReason() != null && !doc.getRejectionReason().isBlank())
                ? tr(false, "Sebep", doc.getRejectionReason()) : "";
        String content = "<p>Yüklediğiniz belge reddedilmiştir. Lütfen gerekli düzeltmeleri yaparak tekrar yükleyin.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "Belge türü", typeLabel)
                + tr(false, "Durum", "Reddedildi")
                + reasonRow
                + "</table>";
        String body = buildHtml(subject, content, "", frontendBaseUrl + "/profile", "Profilimi Görüntüle");
        sendAndLog(owner, subject, body, EmailNotificationType.DOCUMENT_REJECTED,
                "Document", documentId.toString());
    }

    // ── Koordinasyon merkezi konumu ────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDistrictCoordinationCenterSet(UUID districtId) {
        District district = districtRepository.findById(districtId).orElse(null);
        if (district == null) return;

        BigDecimal lat = district.getCoordinatorLatitude();
        BigDecimal lng = district.getCoordinatorLongitude();
        String mapsLink = (lat != null && lng != null)
                ? "https://www.google.com/maps?q=" + lat + "," + lng : null;

        String subject = district.getName() + " İlçe Koordinasyon Merkezi belirlendi";
        String locRow = (lat != null && lng != null)
                ? tr(false, "Konum (enlem/boylam)", lat + ", " + lng) : "";
        String mapsRow = mapsLink != null
                ? "<tr><td colspan=\"2\" style=\"padding:8px 12px;border:1px solid #e5e7eb;\">"
                + "<a href=\"" + esc(mapsLink) + "\" style=\"color:#1d4ed8;\">Google Maps'te Görüntüle</a>"
                + "</td></tr>" : "";

        String content = "<p>" + esc(district.getName())
                + " ilçesi için koordinasyon merkezi belirlenmiştir.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "İlçe", district.getName())
                + locRow
                + mapsRow
                + "</table>"
                + "<p style=\"font-size:12px;color:#888;\">Acil durumlarda resmi yönlendirmeleri takip ediniz.</p>";

        List<User> users = userRepository.findByDistrictId(districtId);
        for (User user : users) {
            try {
                if (!user.isActive() || !user.isEmailVerified()) continue;
                if (!user.isEmailSystemNotificationsEnabled()) continue;
                String body = buildHtml(subject, content, "", frontendBaseUrl, "Sisteme Git");
                sendAndLog(user, subject, body, EmailNotificationType.COORDINATION_CENTER_SET,
                        "District", districtId.toString());
            } catch (Exception e) {
                log.error("İlçe koordinasyon merkezi maili gönderilemedi (userId={}): {}", user.getId(), e.getMessage());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNeighborhoodCoordinationCenterSet(UUID neighborhoodId) {
        Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId).orElse(null);
        if (neighborhood == null) return;

        BigDecimal lat = neighborhood.getCoordinatorLatitude();
        BigDecimal lng = neighborhood.getCoordinatorLongitude();
        String mapsLink = (lat != null && lng != null)
                ? "https://www.google.com/maps?q=" + lat + "," + lng : null;

        String districtName = neighborhood.getDistrict() != null ? neighborhood.getDistrict().getName() : "-";
        String subject = neighborhood.getName() + " Mahalle Koordinasyon Merkezi belirlendi";
        String locRow = (lat != null && lng != null)
                ? tr(false, "Konum (enlem/boylam)", lat + ", " + lng) : "";
        String mapsRow = mapsLink != null
                ? "<tr><td colspan=\"2\" style=\"padding:8px 12px;border:1px solid #e5e7eb;\">"
                + "<a href=\"" + esc(mapsLink) + "\" style=\"color:#1d4ed8;\">Google Maps'te Görüntüle</a>"
                + "</td></tr>" : "";

        String content = "<p>" + esc(districtName) + " ilçesi "
                + esc(neighborhood.getName())
                + " mahallesi için koordinasyon merkezi belirlenmiştir.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "İlçe", districtName)
                + tr(false, "Mahalle", neighborhood.getName())
                + locRow
                + mapsRow
                + "</table>"
                + "<p style=\"font-size:12px;color:#888;\">Acil durumlarda resmi yönlendirmeleri takip ediniz.</p>";

        List<User> users = userRepository.findByNeighborhoodId(neighborhoodId);
        for (User user : users) {
            try {
                if (!user.isActive() || !user.isEmailVerified()) continue;
                if (!user.isEmailSystemNotificationsEnabled()) continue;
                String body = buildHtml(subject, content, "", frontendBaseUrl, "Sisteme Git");
                sendAndLog(user, subject, body, EmailNotificationType.COORDINATION_CENTER_SET,
                        "Neighborhood", neighborhoodId.toString());
            } catch (Exception e) {
                log.error("Mahalle koordinasyon merkezi maili gönderilemedi (userId={}): {}", user.getId(), e.getMessage());
            }
        }
    }

    // ── Koordinatör ataması ───────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDistrictCoordinatorAssigned(UUID districtId, UUID userId) {
        District district = districtRepository.findById(districtId).orElse(null);
        if (district == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEmailSystemNotificationsEnabled()) return;

        String subject = "İlçe koordinatörü olarak atandınız";
        String content = "<p>" + esc(district.getName())
                + " ilçesi için ilçe koordinatörü olarak atandınız.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "İlçe", district.getName())
                + tr(false, "Görev", "İlçe Koordinatörü")
                + "</table>"
                + "<p>AFET Koordinasyon Sistemi üzerinden görevlerinizi takip edebilirsiniz.</p>";
        String body = buildHtml(subject, content, "", frontendBaseUrl, "Sisteme Git");
        sendAndLog(user, subject, body, EmailNotificationType.COORDINATOR_ASSIGNED,
                "District", districtId.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNeighborhoodCoordinatorAssigned(UUID neighborhoodId, UUID userId) {
        Neighborhood neighborhood = neighborhoodRepository.findById(neighborhoodId).orElse(null);
        if (neighborhood == null) return;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEmailSystemNotificationsEnabled()) return;

        String districtName = neighborhood.getDistrict() != null ? neighborhood.getDistrict().getName() : "-";
        String subject = "Mahalle koordinatörü olarak atandınız";
        String content = "<p>" + esc(districtName) + " ilçesi "
                + esc(neighborhood.getName())
                + " mahallesi için mahalle koordinatörü olarak atandınız.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "İlçe", districtName)
                + tr(false, "Mahalle", neighborhood.getName())
                + tr(true, "Görev", "Mahalle Koordinatörü")
                + "</table>"
                + "<p>AFET Koordinasyon Sistemi üzerinden görevlerinizi takip edebilirsiniz.</p>";
        String body = buildHtml(subject, content, "", frontendBaseUrl, "Sisteme Git");
        sendAndLog(user, subject, body, EmailNotificationType.COORDINATOR_ASSIGNED,
                "Neighborhood", neighborhoodId.toString());
    }

    // ── Hesap / Güvenlik ─────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendUserRegisteredEmail(UUID userId, String email, String firstName, String lastName) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String subject = "Hoş geldiniz — AFET Koordinasyon Sistemine kaydınız tamamlandı";
        String content = "<p>Sayın <strong>" + esc(firstName + " " + lastName) + "</strong>,</p>"
                + "<p>AFET Koordinasyon Sistemi'ne kaydınız başarıyla tamamlanmıştır.</p>"
                + "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "Ad Soyad", firstName + " " + lastName)
                + tr(false, "E-posta", email)
                + "</table>"
                + "<p>Sisteme giriş yaparak görevlerinizi takip edebilir, ekiplere katılabilirsiniz.</p>";
        String body = buildHtml(subject, content, "", frontendBaseUrl + "/login", "Sisteme Giriş Yap");
        sendAndLog(user, subject, body, EmailNotificationType.USER_REGISTERED, "User", userId.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPasswordResetEmail(UUID userId, String resetLink) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String subject = "Şifre sıfırlama talebi — AFET Koordinasyon Sistemi";
        String content = "<p>Sayın <strong>" + esc(user.getFirstName() + " " + user.getLastName()) + "</strong>,</p>"
                + "<p>Hesabınız için şifre sıfırlama talebinde bulunuldu. "
                + "Aşağıdaki bağlantıya tıklayarak şifrenizi sıfırlayabilirsiniz.</p>"
                + "<p>Bu bağlantı <strong>60 dakika</strong> geçerlidir.</p>"
                + "<div style=\"margin:24px 0;\">"
                + "<a href=\"" + resetLink + "\" style=\"background:#dc2626;color:white;padding:12px 24px;"
                + "border-radius:6px;text-decoration:none;font-size:14px;\">Şifremi Sıfırla</a></div>"
                + "<p style=\"font-size:12px;color:#888;\">Bu talebi siz yapmadıysanız bu e-postayı görmezden gelebilirsiniz. "
                + "Hesabınız güvende, herhangi bir değişiklik yapılmamıştır.</p>";
        String body = buildHtml(subject, content, "", "", "");
        sendAndLog(user, subject, body, EmailNotificationType.PASSWORD_RESET, "User", userId.toString());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPasswordChangedEmail(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        String subject = "Şifreniz değiştirildi — AFET Koordinasyon Sistemi";
        String content = "<p>Sayın <strong>" + esc(user.getFirstName() + " " + user.getLastName()) + "</strong>,</p>"
                + "<p>Hesabınızın şifresi başarıyla değiştirildi.</p>"
                + "<p>Bu işlemi siz yapmadıysanız lütfen hemen destek ile iletişime geçin.</p>";
        String body = buildHtml(subject, content, "", frontendBaseUrl + "/login", "Hesabıma Git");
        sendAndLog(user, subject, body, EmailNotificationType.PASSWORD_CHANGED, "User", userId.toString());
    }

    // ── Deprem ────────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEarthquakeAlert(UUID userId, EarthquakeEvent eq) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEmailEarthquakeNotificationsEnabled()) return;

        String subject = String.format("Deprem Bilgilendirmesi: M%.1f - %s",
                eq.getMagnitude() != null ? eq.getMagnitude() : 0.0,
                eq.getLocation() != null ? eq.getLocation() : "");

        String details = "<table style=\"border-collapse:collapse;width:100%;\">"
                + tr(true, "Büyüklük", String.format("%.1f", eq.getMagnitude()))
                + tr(false, "Konum", eq.getLocation() != null ? eq.getLocation() : "-")
                + tr(true, "Tarih/Saat", eq.getEventTime() != null ? DISPLAY_FMT.format(eq.getEventTime()) : "-")
                + tr(false, "Derinlik", eq.getDepth() != null ? String.format("%.1f km", eq.getDepth()) : "-")
                + "</table>";

        String disclaimer = "<p style=\"font-size:12px;color:#888;margin-top:16px;\">"
                + "Bu bilgilendirme otomatik oluşturulmuştur. Resmi kurum açıklamalarını takip ediniz.</p>";

        String body = buildHtml(subject, details + disclaimer, "",
                frontendBaseUrl + "/earthquake", "Deprem Bilgilerini Görüntüle");
        sendAndLog(user, subject, body, EmailNotificationType.EARTHQUAKE_ALERT,
                "EarthquakeEvent", eq.getId().toString());
    }

    // ── Yardımcı metodlar ─────────────────────────────────────────────────────

    private void notifyDamageCoordinators(DamageAssessment da, UUID damageAssessmentId) {
        String subject = "Yeni hasar bildirimi: " + da.getNeighborhood().getName();
        String content = "<p>Yeni bir hasar bildirimi oluşturuldu.</p>" + buildDamageTable(da);

        // Mahalle koordinatörü
        User nbCoord = da.getNeighborhood().getCoordinator();
        if (nbCoord != null && nbCoord.isEmailDamageNotificationsEnabled()) {
            String body = buildHtml(subject, content, "",
                    frontendBaseUrl + "/damage", "Hasar Bildirimlerini Görüntüle");
            sendAndLog(nbCoord, subject, body, EmailNotificationType.DAMAGE_REPORT_COORDINATOR_NOTICE,
                    "DamageAssessment", damageAssessmentId.toString());
        }

        // İlçe koordinatörü (farklıysa)
        User distCoord = da.getDistrict().getCoordinator();
        if (distCoord != null && distCoord.isEmailDamageNotificationsEnabled()
                && (nbCoord == null || !distCoord.getId().equals(nbCoord.getId()))) {
            String body = buildHtml(subject, content, "",
                    frontendBaseUrl + "/damage", "Hasar Bildirimlerini Görüntüle");
            sendAndLog(distCoord, subject, body, EmailNotificationType.DAMAGE_REPORT_COORDINATOR_NOTICE,
                    "DamageAssessment", damageAssessmentId.toString());
        }

        // Adminler
        notifyAdmins(subject, content, EmailNotificationType.DAMAGE_REPORT_COORDINATOR_NOTICE,
                "DamageAssessment", damageAssessmentId.toString());
    }

    private void notifyAdmins(String subject, String htmlContent, EmailNotificationType type,
                               String entityType, String entityId) {
        List<User> admins = userRepository.findAdmins();
        for (User admin : admins) {
            try {
                if (!admin.isEmailSystemNotificationsEnabled()) continue;
                String body = buildHtml(subject, htmlContent, "", frontendBaseUrl, "Sisteme Git");
                sendAndLog(admin, subject, body, type, entityType, entityId);
            } catch (Exception e) {
                log.error("Admin bildirimi gönderilemedi (adminId={}): {}", admin.getId(), e.getMessage());
            }
        }
    }

    void sendAndLog(User recipient, String subject, String htmlBody,
                    EmailNotificationType type, String entityType, String entityId) {
        EmailNotificationStatus status = EmailNotificationStatus.PENDING;
        String errorMessage = null;
        OffsetDateTime sentAt = null;

        try {
            emailProvider.send(recipient.getEmail(), subject, htmlBody);
            status = EmailNotificationStatus.SENT;
            sentAt = OffsetDateTime.now();
        } catch (Exception e) {
            status = EmailNotificationStatus.FAILED;
            errorMessage = e.getMessage();
            log.error("Email gönderilemedi: type={}, to={}, hata={}", type, recipient.getEmail(), e.getMessage());
        }

        try {
            emailLogRepository.save(EmailNotificationLog.builder()
                    .recipientUserId(recipient.getId())
                    .recipientEmail(recipient.getEmail())
                    .type(type)
                    .subject(subject)
                    .status(status)
                    .errorMessage(errorMessage)
                    .relatedEntityType(entityType)
                    .relatedEntityId(entityId)
                    .sentAt(sentAt)
                    .build());
        } catch (Exception e) {
            log.error("Email log kaydedilemedi: type={}, to={}: {}", type, recipient.getEmail(), e.getMessage());
        }
    }

    void sendSkipped(User recipient, EmailNotificationType type, String entityType, String entityId) {
        try {
            emailLogRepository.save(EmailNotificationLog.builder()
                    .recipientUserId(recipient.getId())
                    .recipientEmail(recipient.getEmail())
                    .type(type)
                    .status(EmailNotificationStatus.SKIPPED)
                    .relatedEntityType(entityType)
                    .relatedEntityId(entityId)
                    .build());
        } catch (Exception e) {
            log.error("Email skip log kaydedilemedi: {}", e.getMessage());
        }
    }

    // ── HTML template ──────────────────────────────────────────────────────────

    private String buildHtml(String title, String bodyHtml, String extraContent,
                              String ctaUrl, String ctaText) {
        return "<!DOCTYPE html><html lang=\"tr\"><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family:Arial,sans-serif;color:#333;max-width:600px;margin:0 auto;padding:24px;\">"
                + "<div style=\"background:#1d4ed8;padding:16px;border-radius:8px 8px 0 0;margin-bottom:0;\">"
                + "<h1 style=\"color:white;font-size:18px;margin:0;\">AFET Koordinasyon Sistemi</h1></div>"
                + "<div style=\"border:1px solid #e5e7eb;border-top:none;padding:24px;border-radius:0 0 8px 8px;\">"
                + "<h2 style=\"color:#1e3a5f;font-size:16px;margin-top:0;\">" + esc(title) + "</h2>"
                + bodyHtml
                + (extraContent.isBlank() ? "" : extraContent)
                + (ctaUrl.isBlank() ? "" :
                "<div style=\"margin:24px 0;\">"
                        + "<a href=\"" + esc(ctaUrl) + "\" style=\"background:#1d4ed8;color:white;padding:12px 24px;"
                        + "border-radius:6px;text-decoration:none;font-size:14px;\">" + esc(ctaText) + "</a></div>")
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0;\"/>"
                + "<p style=\"font-size:12px;color:#9ca3af;\">"
                + "Bu e-posta AFET Koordinasyon Sistemi tarafından otomatik olarak gönderilmiştir. "
                + "Lütfen bu e-postayı yanıtlamayın.</p>"
                + "</div></body></html>";
    }

    private String buildDamageTable(DamageAssessment da) {
        return "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "Adres", da.getAddress() != null ? da.getAddress() : "-")
                + tr(false, "Mahalle", da.getNeighborhood() != null ? da.getNeighborhood().getName() : "-")
                + tr(true, "İlçe", da.getDistrict() != null ? da.getDistrict().getName() : "-")
                + tr(false, "Hasar Seviyesi", da.getDamageLevel() != null ? da.getDamageLevel().name() : "-")
                + tr(true, "Durum", da.getVerificationStatus() != null ? da.getVerificationStatus().getLabel() : "-")
                + tr(false, "Tarih", da.getCreatedAt() != null ? DISPLAY_FMT.format(da.getCreatedAt()) : "-")
                + "</table>";
    }

    private String buildEventTable(Event ev) {
        return "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "Görev", ev.getTitle())
                + tr(false, "Mahalle", ev.getNeighborhood() != null ? ev.getNeighborhood().getName() : "-")
                + tr(true, "Ekip", ev.getTeam() != null ? ev.getTeam().getName().getLabel() : "-")
                + tr(false, "Durum", ev.getStatus() != null ? ev.getStatus().name() : "-")
                + "</table>";
    }

    private String buildResourceTable(ResourceRequest rr) {
        return "<table style=\"border-collapse:collapse;width:100%;margin:12px 0;\">"
                + tr(true, "Tür", rr.getResourceType() != null ? rr.getResourceType().name() : "-")
                + tr(false, "Miktar", rr.getQuantity() != null ? rr.getQuantity().toString() : "-")
                + tr(true, "İlçe", rr.getDistrict() != null ? rr.getDistrict().getName() : "-")
                + tr(false, "Açıklama", rr.getDescription() != null ? rr.getDescription() : "-")
                + tr(true, "Durum", rr.getStatus() != null ? rr.getStatus().name() : "-")
                + "</table>";
    }

    private String tr(boolean shaded, String label, String value) {
        String bg = shaded ? " style=\"background:#f9fafb;\"" : "";
        return "<tr" + bg + ">"
                + "<td style=\"padding:8px 12px;border:1px solid #e5e7eb;font-weight:600;width:130px;\">"
                + esc(label) + "</td>"
                + "<td style=\"padding:8px 12px;border:1px solid #e5e7eb;\">" + esc(value) + "</td>"
                + "</tr>";
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
