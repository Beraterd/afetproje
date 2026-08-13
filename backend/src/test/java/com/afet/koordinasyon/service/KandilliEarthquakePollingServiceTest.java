package com.afet.koordinasyon.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

/**
 * Item 15/16 — Kandilli scheduled sync'inin kendi hatasını yutup (AFAD scheduler'ını
 * etkilemeden) devam edebildiğini doğrular.
 */
@ExtendWith(MockitoExtension.class)
class KandilliEarthquakePollingServiceTest {

    @Mock private EarthquakeEventService earthquakeEventService;

    @Test
    void kandilliSyncThrows_pollMethodSwallowsException_doesNotPropagate() {
        KandilliEarthquakePollingService pollingService = new KandilliEarthquakePollingService(earthquakeEventService);
        ReflectionTestUtils.setField(pollingService, "pollingHours", 24);

        when(earthquakeEventService.syncFromKandilli(24))
                .thenThrow(new RuntimeException("Kandilli sunucusu erişilemez"));

        // pollKandilliData() bir @Scheduled metodu — exception fırlatırsa Spring'in
        // scheduler thread'i o TEK invocation'ı loglar ama sonraki tetiklemeler etkilenmez.
        // Burada asıl garanti: metodun kendisi exception'ı YUTUYOR, hiçbir şekilde dışa sızdırmıyor.
        assertThatCode(pollingService::pollKandilliData).doesNotThrowAnyException();

        assertThat(pollingService.getLastError()).contains("Kandilli sunucusu erişilemez");
    }

    @Test
    void kandilliSyncSucceeds_statusFieldsUpdated_errorCleared() {
        KandilliEarthquakePollingService pollingService = new KandilliEarthquakePollingService(earthquakeEventService);
        ReflectionTestUtils.setField(pollingService, "pollingHours", 24);

        var response = com.afet.koordinasyon.dto.response.EarthquakeSyncResponse.builder()
                .fetchedCount(5).savedCount(2).message("ok").build();
        when(earthquakeEventService.syncFromKandilli(24)).thenReturn(response);

        pollingService.pollKandilliData();

        assertThat(pollingService.getLastError()).isNull();
        assertThat(pollingService.getLastSyncFetchedCount()).isEqualTo(5);
        assertThat(pollingService.getLastSyncSavedCount()).isEqualTo(2);
    }
}
