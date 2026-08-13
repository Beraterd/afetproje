package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.ResourceStock;
import com.afet.koordinasyon.domain.entity.ResourceStockMovement;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.AuditActionType;
import com.afet.koordinasyon.domain.enums.ResourceStockMovementType;
import com.afet.koordinasyon.domain.enums.ResourceStockStatus;
import com.afet.koordinasyon.domain.enums.ResourceType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.CreateResourceStockRequest;
import com.afet.koordinasyon.dto.request.TransferStockRequest;
import com.afet.koordinasyon.dto.request.UpdateStockQuantityRequest;
import com.afet.koordinasyon.dto.response.ResourceStockResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceStockMovementRepository;
import com.afet.koordinasyon.repository.ResourceStockRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceStockServiceTest {

    @Mock private ResourceStockRepository stockRepository;
    @Mock private ResourceStockMovementRepository movementRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private ResourceStockService service;

    private District district;
    private Neighborhood neighborhood;
    private User actor;
    private UserPrincipal admin;

    @BeforeEach
    void setUp() {
        district = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        neighborhood = Neighborhood.builder().id(UUID.randomUUID()).name("Moda").district(district).build();
        actor = new User();
        actor.setId(UUID.randomUUID());
        actor.setFirstName("Admin");
        actor.setLastName("User");
        admin = new UserPrincipal(actor.getId(), "Admin", "User", "a@x.com",
                null, UserRole.ADMIN, null, null, true, false, List.of());
    }

    private UserPrincipal districtCoord(UUID districtId) {
        return new UserPrincipal(UUID.randomUUID(), "DC", "User", "dc@x.com",
                null, UserRole.DISTRICT_COORDINATOR, districtId, null, true, false, List.of());
    }

    // ── Hesaplama testleri ─────────────────────────────────────────────────────

    @Test
    @DisplayName("daysRemaining = quantity / dailyUsageEstimate; estimate yoksa null")
    void daysRemaining_calculation() {
        assertThat(service.computeDaysRemaining(100, 20.0)).isEqualTo(5.0);
        assertThat(service.computeDaysRemaining(100, null)).isNull();
        assertThat(service.computeDaysRemaining(100, 0.0)).isNull();
    }

    @Test
    @DisplayName("status: tükendi / kritik (eşik) / kritik (<=2g) / azalıyor (<=5g) / yeterli")
    void status_calculation() {
        assertThat(service.computeStatus(0, 10, 5.0)).isEqualTo(ResourceStockStatus.OUT_OF_STOCK);
        assertThat(service.computeStatus(5, 10, null)).isEqualTo(ResourceStockStatus.CRITICAL);   // eşik altı
        assertThat(service.computeStatus(100, 0, 60.0)).isEqualTo(ResourceStockStatus.CRITICAL);  // ~1.7 gün
        assertThat(service.computeStatus(100, 0, 25.0)).isEqualTo(ResourceStockStatus.DECREASING);// 4 gün
        assertThat(service.computeStatus(100, 0, 10.0)).isEqualTo(ResourceStockStatus.SUFFICIENT);// 10 gün
        assertThat(service.computeStatus(100, 0, null)).isEqualTo(ResourceStockStatus.SUFFICIENT);// tüketim bilinmiyor
    }

    // ── Oluşturma + ilk hareket kaydı ──────────────────────────────────────────

    @Test
    @DisplayName("Stok oluşturma: INITIAL hareket kaydı oluşur, durum hesaplanır")
    void create_recordsInitialMovement() {
        CreateResourceStockRequest req = new CreateResourceStockRequest();
        req.setName("İçme Suyu");
        req.setCategory(ResourceType.WATER);
        req.setQuantity(120);
        req.setUnit("koli");
        req.setDailyUsageEstimate(40.0);
        req.setCriticalThreshold(30);
        req.setDistrictId(district.getId());
        req.setNeighborhoodId(neighborhood.getId());

        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockRepository.save(any(ResourceStock.class))).thenAnswer(i -> {
            ResourceStock s = i.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        ResourceStockResponse resp = service.create(req, admin);

        assertThat(resp.getQuantity()).isEqualTo(120);
        assertThat(resp.getDaysRemaining()).isEqualTo(3.0); // 120/40
        assertThat(resp.getStatus()).isEqualTo(ResourceStockStatus.DECREASING.name()); // 3 gün

        ArgumentCaptor<ResourceStockMovement> mv = ArgumentCaptor.forClass(ResourceStockMovement.class);
        verify(movementRepository).save(mv.capture());
        assertThat(mv.getValue().getMovementType()).isEqualTo(ResourceStockMovementType.INITIAL);
        assertThat(mv.getValue().getNewQuantity()).isEqualTo(120);
        assertThat(mv.getValue().getPreviousQuantity()).isZero();
    }

    // ── Kategoriye göre dinamik ürün adı ────────────────────────────────────────

    private CreateResourceStockRequest stockReq(ResourceType category, String name) {
        CreateResourceStockRequest req = new CreateResourceStockRequest();
        req.setName(name);
        req.setCategory(category);
        req.setQuantity(10);
        req.setUnit("adet");
        req.setDistrictId(district.getId());
        req.setNeighborhoodId(neighborhood.getId());
        return req;
    }

    private void stubFullCreate() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockRepository.save(any(ResourceStock.class))).thenAnswer(i -> {
            ResourceStock s = i.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
    }

    @Test
    @DisplayName("Ad zorunlu kategoride (FOOD) ad boşsa hata")
    void create_nameRequiredCategory_blankName_throws() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));

        assertThatThrownBy(() -> service.create(stockReq(ResourceType.FOOD, null), admin))
                .isInstanceOf(com.afet.koordinasyon.exception.BusinessRuleException.class)
                .hasMessageContaining("ürün adı zorunludur");
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ad zorunlu olmayan kategoride (WATER) ad boşsa kategori label'ı atanır")
    void create_nameNotRequiredCategory_derivesLabel() {
        stubFullCreate();
        ResourceStockResponse resp = service.create(stockReq(ResourceType.WATER, null), admin);
        assertThat(resp.getName()).isEqualTo("Su");
    }

    @Test
    @DisplayName("OTHER kategorisinde ad ve not boşsa hata")
    void create_other_noNameNoNotes_throws() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));

        assertThatThrownBy(() -> service.create(stockReq(ResourceType.OTHER, null), admin))
                .isInstanceOf(com.afet.koordinasyon.exception.BusinessRuleException.class)
                .hasMessageContaining("Diğer");
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("OTHER kategorisinde sadece not doluysa ad label'dan türetilir")
    void create_other_withNotesOnly_ok() {
        stubFullCreate();
        CreateResourceStockRequest req = stockReq(ResourceType.OTHER, null);
        req.setNotes("Özel ekipman");
        ResourceStockResponse resp = service.create(req, admin);
        assertThat(resp.getName()).isEqualTo("Diğer");
    }

    @Test
    @DisplayName("İlçe/mahalle uyumsuzsa stok oluşturulamaz")
    void create_districtNeighborhoodMismatch_throws() {
        District other = District.builder().id(UUID.randomUUID()).name("Beşiktaş").build();
        Neighborhood mismatched = Neighborhood.builder()
                .id(UUID.randomUUID()).name("Levent").district(other).build();
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(mismatched.getId())).thenReturn(Optional.of(mismatched));

        CreateResourceStockRequest req = stockReq(ResourceType.WATER, "Su");
        req.setNeighborhoodId(mismatched.getId());

        assertThatThrownBy(() -> service.create(req, admin))
                .isInstanceOf(com.afet.koordinasyon.exception.BusinessRuleException.class)
                .hasMessageContaining("bu ilçeye ait değil");
        verify(stockRepository, never()).save(any());
    }

    // ── Miktar güncelleme + hareket kaydı ──────────────────────────────────────

    @Test
    @DisplayName("Miktar güncelleme: fark pozitifse INCREASE hareketi ve doğru miktar")
    void updateQuantity_recordsIncrease() {
        ResourceStock stock = ResourceStock.builder()
                .id(UUID.randomUUID()).name("Battaniye").category(ResourceType.BLANKET)
                .quantity(50).unit("adet").criticalThreshold(10)
                .district(district).neighborhood(neighborhood).build();
        when(stockRepository.findById(stock.getId())).thenReturn(Optional.of(stock));
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));
        when(stockRepository.save(any(ResourceStock.class))).thenAnswer(i -> i.getArgument(0));

        UpdateStockQuantityRequest req = new UpdateStockQuantityRequest();
        req.setNewQuantity(80);
        req.setReason("+30 battaniye eklendi");

        service.updateQuantity(stock.getId(), req, admin);

        assertThat(stock.getQuantity()).isEqualTo(80);
        ArgumentCaptor<ResourceStockMovement> mv = ArgumentCaptor.forClass(ResourceStockMovement.class);
        verify(movementRepository).save(mv.capture());
        assertThat(mv.getValue().getMovementType()).isEqualTo(ResourceStockMovementType.INCREASE);
        assertThat(mv.getValue().getQuantityChange()).isEqualTo(30);
        assertThat(mv.getValue().getPreviousQuantity()).isEqualTo(50);
        assertThat(mv.getValue().getNewQuantity()).isEqualTo(80);
    }

    // ── Koordinatör yetki alanı ────────────────────────────────────────────────

    @Test
    @DisplayName("Koordinatör yetki alanı dışındaki stoğu güncelleyemez (AccessDenied)")
    void coordinator_cannotManageOutsideDistrict() {
        UUID otherDistrictId = UUID.randomUUID();
        ResourceStock stock = ResourceStock.builder()
                .id(UUID.randomUUID()).name("Su").category(ResourceType.WATER)
                .quantity(10).unit("koli").district(district).neighborhood(neighborhood).build();
        when(stockRepository.findById(stock.getId())).thenReturn(Optional.of(stock));

        UpdateStockQuantityRequest req = new UpdateStockQuantityRequest();
        req.setNewQuantity(5);

        // district coordinator of a DIFFERENT district
        assertThatThrownBy(() -> service.updateQuantity(stock.getId(), req, districtCoord(otherDistrictId)))
                .isInstanceOf(AccessDeniedException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("İlçe koordinatörü liste sorgusu kendi ilçesine daraltılır")
    void coordinator_listScopedToOwnDistrict() {
        UserPrincipal dc = districtCoord(district.getId());
        when(stockRepository.search(eq(district.getId()), any(), any(), anyBoolean(), anyString()))
                .thenReturn(List.of());

        service.list(null, null, null, null, null, false, dc);

        // districtId parametresi koordinatörün kendi ilçesiyle, search olmadan (hasSearch=false) çağrıldı
        verify(stockRepository).search(eq(district.getId()), isNull(), isNull(), eq(false), anyString());
    }

    @Test
    @DisplayName("Lookup: stok yoksa hasStock=false ve uygun mesaj döner")
    void lookup_noStock() {
        when(stockRepository.findForLookup(district.getId(), neighborhood.getId(), ResourceType.WATER))
                .thenReturn(List.of());
        var resp = service.lookup(district.getId(), neighborhood.getId(), ResourceType.WATER, 50);
        assertThat(resp.isHasStock()).isFalse();
        assertThat(resp.getMessage()).contains("kayıtlı stok bulunamadı");
    }

    @Test
    @DisplayName("Lookup: talep edilen miktar stoktan fazlaysa uyarı mesajı verir")
    void lookup_requestExceedsStock() {
        ResourceStock s = ResourceStock.builder()
                .id(UUID.randomUUID()).name("Su").category(ResourceType.WATER)
                .quantity(100).unit("koli").criticalThreshold(10).dailyUsageEstimate(10.0)
                .district(district).neighborhood(neighborhood).build();
        when(stockRepository.findForLookup(district.getId(), neighborhood.getId(), ResourceType.WATER))
                .thenReturn(List.of(s));

        var resp = service.lookup(district.getId(), neighborhood.getId(), ResourceType.WATER, 250);
        assertThat(resp.isHasStock()).isTrue();
        assertThat(resp.getTotalQuantity()).isEqualTo(100);
        assertThat(resp.getDaysRemaining()).isEqualTo(10.0);
        assertThat(resp.getMessage()).contains("Talep edilen miktar mevcut stoktan fazla");
    }

    // ── Kaynak transfer önerisi + atomic transfer ───────────────────────────────
    //
    // Gerçek eşzamanlı transaction/DB-seviyesi optimistic-lock davranışı (Hibernate @Version)
    // bu ortamda PostgreSQL olmadığı için doğrudan test edilemiyor — aşağıdaki testler servis
    // katmanının mantığını (eşik koruması, aynı-kategori kuralı, scope/yetki) ve
    // OptimisticLockingFailureException'ın doğru şekilde yayılıp movement/audit kaydı
    // oluşmadan durdurulduğunu doğrular.

    private District otherDistrict;
    private ResourceStock source;
    private ResourceStock target;

    private void setUpTransferFixtures() {
        otherDistrict = District.builder().id(UUID.randomUUID()).name("Pendik").build();
        source = ResourceStock.builder()
                .id(UUID.randomUUID()).name("Su").category(ResourceType.WATER)
                .quantity(450).criticalThreshold(100).unit("koli").district(district).active(true).build();
        target = ResourceStock.builder()
                .id(UUID.randomUUID()).name("Su").category(ResourceType.WATER)
                .quantity(63).criticalThreshold(100).unit("koli").district(otherDistrict).active(true).build();
        lenient().when(stockRepository.findById(source.getId())).thenReturn(Optional.of(source));
        lenient().when(stockRepository.findById(target.getId())).thenReturn(Optional.of(target));
    }

    private TransferStockRequest transferReq(int quantity) {
        TransferStockRequest req = new TransferStockRequest();
        req.setSourceStockId(source.getId());
        req.setTargetStockId(target.getId());
        req.setQuantity(quantity);
        req.setReason("Test transferi");
        return req;
    }

    @Test
    @DisplayName("Başarılı transfer: iki stok da güncellenir, hareket ve audit kaydı oluşur")
    void transfer_success_updatesBothStocksAndLogsAudit() {
        setUpTransferFixtures();
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        when(stockRepository.save(any(ResourceStock.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.transfer(transferReq(50), admin);

        assertThat(response.getSource().getQuantity()).isEqualTo(400);
        assertThat(response.getTarget().getQuantity()).isEqualTo(113);
        verify(movementRepository, times(2)).save(any());
        verify(auditLogService).logUserAction(any(), eq(AuditActionType.RESOURCE_STOCK_TRANSFERRED),
                eq("ResourceStock"), eq(target.getId()), any(String.class), any());
    }

    @Test
    @DisplayName("Kaynak kritik eşiğin altına düşecekse transfer reddedilir")
    void transfer_wouldDropSourceBelowThreshold_rejected() {
        setUpTransferFixtures();

        assertThatThrownBy(() -> service.transfer(transferReq(400), admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("kritik eşiğinin altına");

        verify(stockRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("Farklı kategorideki stoklar arasında transfer yapılamaz")
    void transfer_differentCategories_rejected() {
        setUpTransferFixtures();
        target.setCategory(ResourceType.BLANKET);

        assertThatThrownBy(() -> service.transfer(transferReq(10), admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aynı kaynak kategorisi");

        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("İlçe koordinatörü kaynak veya hedefin kendi ilçesinde olmadığı bir transferi yapamaz")
    void transfer_districtCoordinator_notStakeholder_throwsAccessDenied() {
        setUpTransferFixtures();
        UUID unrelatedDistrictId = UUID.randomUUID();

        assertThatThrownBy(() -> service.transfer(transferReq(50), districtCoord(unrelatedDistrictId)))
                .isInstanceOf(AccessDeniedException.class);

        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("İlçe koordinatörü kaynak deposu kendi ilçesindeyse transfer yapabilir")
    void transfer_districtCoordinator_isSourceDistrict_succeeds() {
        setUpTransferFixtures();
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        when(stockRepository.save(any(ResourceStock.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.transfer(transferReq(50), districtCoord(district.getId()));

        assertThat(response.getSource().getQuantity()).isEqualTo(400);
    }

    @Test
    @DisplayName("Mahalle koordinatörü transfer başlatamaz")
    void transfer_neighborhoodCoordinator_throwsAccessDenied() {
        setUpTransferFixtures();
        UserPrincipal nc = new UserPrincipal(UUID.randomUUID(), "NC", "User", "nc@x.com",
                null, UserRole.NEIGHBORHOOD_COORDINATOR, district.getId(), neighborhood.getId(), true, false, List.of());

        assertThatThrownBy(() -> service.transfer(transferReq(50), nc))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Eşzamanlı güncelleme çakışması (optimistic lock) hatası movement/audit kaydı oluşmadan yayılır")
    void transfer_optimisticLockOnSecondSave_propagatesWithoutPartialSideEffects() {
        setUpTransferFixtures();
        when(userRepository.findById(any())).thenReturn(Optional.of(actor));
        when(stockRepository.save(source)).thenReturn(source);
        when(stockRepository.save(target)).thenThrow(
                new ObjectOptimisticLockingFailureException(ResourceStock.class, target.getId()));

        assertThatThrownBy(() -> service.transfer(transferReq(50), admin))
                .isInstanceOf(OptimisticLockingFailureException.class);

        verify(movementRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    @DisplayName("Hedef stok kritik değilse transfer önerisi boş döner")
    void suggestTransfers_targetSufficient_returnsEmpty() {
        setUpTransferFixtures();
        target.setQuantity(500);

        assertThat(service.suggestTransfers(target.getId(), admin)).isEmpty();
        verify(stockRepository, never()).findByCategoryAndActiveTrueAndIdNot(any(), any());
    }

    @Test
    @DisplayName("Hedef kritikse, güvenli fazlalığı olan aday doğru önerilen miktarla döner")
    void suggestTransfers_targetCritical_returnsDeterministicSuggestion() {
        setUpTransferFixtures();
        when(stockRepository.findByCategoryAndActiveTrueAndIdNot(ResourceType.WATER, target.getId()))
                .thenReturn(List.of(source));

        var suggestions = service.suggestTransfers(target.getId(), admin);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).getSourceStockId()).isEqualTo(source.getId());
        assertThat(suggestions.get(0).getSuggestedQuantity()).isEqualTo(37); // need = 100-63
        assertThat(suggestions.get(0).getAvailableQuantity()).isEqualTo(350); // 450-100
    }
}
