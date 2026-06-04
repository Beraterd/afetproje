package com.afet.koordinasyon.repository;

import com.afet.koordinasyon.domain.enums.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

/**
 * Canlı Postgres'e karşı gerçek SQL üretimini doğrulayan regresyon testi.
 * Salt-okunur; şema değiştirmez.
 * <p>
 * Regresyon: "function lower(bytea) does not exist" — null String arama parametresi
 * Hibernate 6 tarafından LOWER/CONCAT içinde bytea olarak bind ediliyordu. Aşağıdaki
 * tüm kombinasyonlar (özellikle search=null + category dolu/boş) artık hata vermemeli.
 */
@SpringBootTest
class ResourceStockRepositorySearchIT {

    @Autowired
    private ResourceStockRepository repository;

    @Autowired
    private AssemblyAreaRepository assemblyAreaRepository;

    @Test
    void search_allCombinations_doNotThrow() {
        // search yok (en kritik senaryo: GET /api/resource-stocks ve /summary bunu kullanır)
        repository.search(null, null, null, false, "%");
        // kategori filtresi + search yok
        repository.search(null, null, ResourceType.WATER, false, "%");
        // metinli arama
        repository.search(null, null, null, true, "%su%");
        // kategori + metinli arama
        repository.search(null, null, ResourceType.WATER, true, "%battaniye%");
    }

    @Test
    void assemblyAreaSearch_doesNotThrow() {
        // Aynı null-String-param defekti AssemblyArea filtrelerinde de düzeltildi
        assemblyAreaRepository.findWithFilters(null, null, null, null, null, false, "%", PageRequest.of(0, 5));
        assemblyAreaRepository.findWithFilters(null, null, null, null, null, true, "%moda%", PageRequest.of(0, 5));
    }
}
