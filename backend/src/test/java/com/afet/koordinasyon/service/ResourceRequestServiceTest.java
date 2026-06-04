package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.domain.entity.Neighborhood;
import com.afet.koordinasyon.domain.entity.ResourceRequest;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.RequestPriority;
import com.afet.koordinasyon.domain.enums.ResourceType;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.CreateResourceRequestRequest;
import com.afet.koordinasyon.dto.response.ResourceRequestResponse;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.ResourceRequestRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceRequestServiceTest {

    @Mock private ResourceRequestRepository resourceRequestRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ResourceRequestService service;

    private UserPrincipal admin;
    private District district;
    private Neighborhood neighborhood;
    private User actor;

    @BeforeEach
    void setUp() {
        UUID adminId = UUID.randomUUID();
        admin = new UserPrincipal(adminId, "Admin", "User", "a@x.com",
                null, UserRole.ADMIN, null, null, true, List.of());
        district = District.builder().id(UUID.randomUUID()).name("Kadıköy").build();
        neighborhood = Neighborhood.builder().id(UUID.randomUUID()).name("Moda").district(district).build();
        actor = new User();
        actor.setId(adminId);
        actor.setFirstName("Admin");
        actor.setLastName("User");
    }

    private CreateResourceRequestRequest validReq() {
        CreateResourceRequestRequest req = new CreateResourceRequestRequest();
        req.setDistrictId(district.getId());
        req.setNeighborhoodId(neighborhood.getId());
        req.setResourceType(ResourceType.WATER);
        req.setQuantity(10);
        req.setUnit("koli");
        req.setPriority(RequestPriority.HIGH);
        return req;
    }

    @Test
    @DisplayName("Talep oluştururken mahalle zorunludur")
    void create_neighborhoodRequired() {
        CreateResourceRequestRequest req = validReq();
        req.setNeighborhoodId(null);

        assertThatThrownBy(() -> service.create(req, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Mahalle");
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("İlçe/mahalle uyumsuzsa hata verir")
    void create_districtNeighborhoodMismatch() {
        District other = District.builder().id(UUID.randomUUID()).name("Beşiktaş").build();
        Neighborhood mismatched = Neighborhood.builder()
                .id(UUID.randomUUID()).name("Levent").district(other).build();

        CreateResourceRequestRequest req = validReq();
        req.setNeighborhoodId(mismatched.getId());

        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(mismatched.getId())).thenReturn(Optional.of(mismatched));

        assertThatThrownBy(() -> service.create(req, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("bu ilçeye ait değil");
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Talep response'unda neighborhoodName + priority döner")
    void create_returnsNeighborhoodNameAndPriority() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(actor));
        when(resourceRequestRepository.save(any(ResourceRequest.class))).thenAnswer(i -> {
            ResourceRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ResourceRequestResponse resp = service.create(validReq(), admin);

        assertThat(resp.getNeighborhoodName()).isEqualTo("Moda");
        assertThat(resp.getDistrictName()).isEqualTo("Kadıköy");
        assertThat(resp.getPriority()).isEqualTo("HIGH");
        assertThat(resp.getUnit()).isEqualTo("koli");
    }

    @Test
    @DisplayName("Ürün adı zorunlu kategoride (FOOD) ad boşsa hata")
    void create_nameRequiredCategory_blankName_throws() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(actor));

        CreateResourceRequestRequest req = validReq();
        req.setResourceType(ResourceType.FOOD);
        req.setName(null);

        assertThatThrownBy(() -> service.create(req, admin))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ürün adı zorunludur");
        verify(resourceRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ürün adı zorunsuz kategoride (WATER) ad boşsa productName kategori adı olur")
    void create_nameNotRequiredCategory_derivesLabel() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(actor));
        when(resourceRequestRepository.save(any(ResourceRequest.class))).thenAnswer(i -> {
            ResourceRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateResourceRequestRequest req = validReq();
        req.setResourceType(ResourceType.WATER);
        req.setName(null);

        ResourceRequestResponse resp = service.create(req, admin);
        assertThat(resp.getProductName()).isEqualTo("Su");
    }

    @Test
    @DisplayName("Özel ürün adı girilirse productName onu döner")
    void create_customName_returnsIt() {
        when(districtRepository.findById(district.getId())).thenReturn(Optional.of(district));
        when(neighborhoodRepository.findById(neighborhood.getId())).thenReturn(Optional.of(neighborhood));
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(actor));
        when(resourceRequestRepository.save(any(ResourceRequest.class))).thenAnswer(i -> {
            ResourceRequest r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        CreateResourceRequestRequest req = validReq();
        req.setResourceType(ResourceType.FOOD);
        req.setName("Bebek maması");

        ResourceRequestResponse resp = service.create(req, admin);
        assertThat(resp.getProductName()).isEqualTo("Bebek maması");
    }

    @Test
    @DisplayName("Liste Specification tabanlı findAll ile çalışır (koordinatör kapsamı dahil)")
    void list_usesSpecification() {
        UUID districtId = UUID.randomUUID();
        UserPrincipal dc = new UserPrincipal(UUID.randomUUID(), "DC", "User", "dc@x.com",
                null, UserRole.DISTRICT_COORDINATOR, districtId, null, true, List.of());
        when(resourceRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listRequests(0, 20, null, null, ResourceType.WATER, "OPEN", "HIGH", "su", dc);

        verify(resourceRequestRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(resourceRequestRepository, never()).findAll(any(Pageable.class));
    }
}
