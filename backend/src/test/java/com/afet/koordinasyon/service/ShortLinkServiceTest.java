package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.ShortLink;
import com.afet.koordinasyon.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock private ShortLinkRepository shortLinkRepository;

    @InjectMocks private ShortLinkService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "shortLinkBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("createShortLink — benzersiz kod üretir ve /s/{code} tam adresini döner")
    void createShortLink_generatesCodeAndReturnsFullUrl() {
        when(shortLinkRepository.existsByCode(anyString())).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        UUID eqId = UUID.randomUUID();
        String target = "http://localhost:5173/emergency/assembly-areas?earthquakeId=" + eqId;

        String url = service.createShortLink(target, userId, eqId, null);

        ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
        verify(shortLinkRepository).save(captor.capture());
        ShortLink saved = captor.getValue();

        assertThat(saved.getTargetUrl()).isEqualTo(target);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEarthquakeEventId()).isEqualTo(eqId);
        assertThat(saved.getCode()).isNotBlank();
        assertThat(url).isEqualTo("http://localhost:8080/s/" + saved.getCode());
    }

    @Test
    @DisplayName("createShortLink — kod çakışırsa yeni kod denenir")
    void createShortLink_retriesOnCollision() {
        // İlk kod mevcut, ikinci kod boş
        when(shortLinkRepository.existsByCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createShortLink("http://x", null, null, null);

        verify(shortLinkRepository, times(2)).existsByCode(anyString());
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("resolveTargetUrl — geçerli kod hedef URL'i döner")
    void resolveTargetUrl_validCode_returnsTarget() {
        ShortLink link = ShortLink.builder()
                .code("abc1234")
                .targetUrl("http://localhost:5173/emergency/assembly-areas")
                .build();
        when(shortLinkRepository.findByCode("abc1234")).thenReturn(Optional.of(link));

        Optional<String> result = service.resolveTargetUrl("abc1234");

        assertThat(result).contains("http://localhost:5173/emergency/assembly-areas");
    }

    @Test
    @DisplayName("resolveTargetUrl — süresi dolmuş link boş döner")
    void resolveTargetUrl_expired_returnsEmpty() {
        ShortLink link = ShortLink.builder()
                .code("exp1234")
                .targetUrl("http://x")
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();
        when(shortLinkRepository.findByCode("exp1234")).thenReturn(Optional.of(link));

        assertThat(service.resolveTargetUrl("exp1234")).isEmpty();
    }

    @Test
    @DisplayName("resolveTargetUrl — bilinmeyen kod boş döner")
    void resolveTargetUrl_unknown_returnsEmpty() {
        when(shortLinkRepository.findByCode("nope")).thenReturn(Optional.empty());
        assertThat(service.resolveTargetUrl("nope")).isEmpty();
    }

    @Test
    @DisplayName("buildFrontendUrl — frontend ana adresiyle tam URL üretir")
    void buildFrontendUrl_buildsAbsoluteUrl() {
        String url = service.buildFrontendUrl("/emergency/assembly-areas?earthquakeId=1");
        assertThat(url).isEqualTo("http://localhost:5173/emergency/assembly-areas?earthquakeId=1");
    }
}
