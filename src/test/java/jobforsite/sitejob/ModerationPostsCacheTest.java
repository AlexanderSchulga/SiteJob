package jobforsite.sitejob;

import jobforsite.sitejob.module.product.dto.PostTextBlockedDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.dto.PostTextJobBlockedDTO;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.dto.ModerationSliceResponse;
import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import jobforsite.sitejob.module.shared.services.util.CategoryUtils;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.dto.VacancyBlockedDTO;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ModerationPostsCacheTest.TestModerationCacheConfig.class,
        ModerationPostsCacheService.class
})
class ModerationPostsCacheTest {

    /**
     * Минимальный контекст: @EnableCaching + moderationCacheManager.
     * Никакого JPA, никакого Spring Boot
     */
    @Configuration
    @EnableCaching
    static class TestModerationCacheConfig {
        @Bean("moderationCacheManager")
        public CacheManager moderationCacheManager() {
            return new ConcurrentMapCacheManager(
                    "moderationProductsDto",
                    "moderationServicesDto",
                    "moderationVacanciesDto",
                    "moderationResumeDto",
                    "moderationProductsCount",
                    "moderationServicesCount",
                    "moderationVacanciesCount",
                    "moderationResumeCount"
            );
        }
    }

    @MockitoBean private PostRepo postRepo;
    @MockitoBean private PostRepoJob postRepoJob;
    @MockitoBean private PostVacancyRepository postVacancyRepository;
    @MockitoBean private PostResumeRepository postResumeRepository;
    @MockitoBean private CategoryUtils categoryUtils;
    @MockitoBean private RegionsConfig regionsConfig;

    @Autowired private ModerationPostsCacheService moderationCacheService;
    @Autowired @Qualifier("moderationCacheManager") private CacheManager cacheManager;

    private User testUser;
    private PostText testProduct;
    private PostTextJob testService;
    private Vacancy testVacancy;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        clearAllCaches();

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNameuse("Тестовый Пользователь");
        testUser.setPhonePost("+1234567890");
        testUser.setRegion("moscow_oblast");
        testUser.setCity("msk_city");

        testProduct = new PostText();
        testProduct.setId(1L);
        testProduct.setHashId("product123");
        testProduct.setTitle("Тестовый товар");
        testProduct.setPrice(1000);
        testProduct.setFull_text("Описание тестового товара");
        testProduct.setCategory("electronics");
        testProduct.setModeration(true);
        testProduct.setBlocked(false);
        testProduct.setUser(testUser);

        testService = new PostTextJob();
        testService.setId(2L);
        testService.setHashId("service123");
        testService.setTitle("Тестовая услуга");
        testService.setPrice(2000);
        testService.setFull_text("Описание тестовой услуги");
        testService.setCategory("cleaning");
        testService.setModeration(true);
        testService.setBlocked(false);
        testService.setUser(testUser);

        testVacancy = new Vacancy();
        testVacancy.setId(3L);
        testVacancy.setHashId("vacancy123");
        testVacancy.setTitle("Тестовая вакансия");
        testVacancy.setCash(50000);
        testVacancy.setFull_text("Описание тестовой вакансии");
        testVacancy.setCategory("it");
        testVacancy.setModeration(true);
        testVacancy.setBlocked(false);
        testVacancy.setUser(testUser);

        pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Моки вспомогательных сервисов
        when(categoryUtils.getCategoryDisplayName(anyString(), anyString()))
                .thenReturn("Отображаемая категория");
        when(regionsConfig.getRegionDisplayName(anyString()))
                .thenReturn("Москва и Московская область");
    }

    private void clearAllCaches() {
        for (String name : new String[]{
                "moderationProductsDto", "moderationServicesDto",
                "moderationVacanciesDto", "moderationResumeDto",
                "moderationProductsCount", "moderationServicesCount",
                "moderationVacanciesCount", "moderationResumeCount"
        }) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    // =================================================================
    // DTO Slice caching
    // =================================================================

    @Test
    void testModerationProductsDtoCaching() {
        Slice<Long> ids = new SliceImpl<>(List.of(1L), pageable, true);
        when(postRepo.findModerationIds(any(Pageable.class))).thenReturn(ids);
        when(postRepo.findByIdsWithImagesAndUser(List.of(1L)))
                .thenReturn(List.of(testProduct));

        ModerationSliceResponse<PostTextBlockedDTO> result1 =
                moderationCacheService.getModerationProductsDto(0, 3, null, null);

        assertNotNull(result1);
        assertEquals(1, result1.getContent().size());
        assertEquals("Тестовый товар", result1.getContent().get(0).getTitle());

        verify(postRepo, times(1)).findModerationIds(any(Pageable.class));
        verify(postRepo, times(1)).findByIdsWithImagesAndUser(List.of(1L));

        // Второй вызов — из кэша, репозиторий не дёргается
        ModerationSliceResponse<PostTextBlockedDTO> result2 =
                moderationCacheService.getModerationProductsDto(0, 3, null, null);

        assertNotNull(result2);
        assertEquals(1, result2.getContent().size());

        verify(postRepo, times(1)).findModerationIds(any(Pageable.class));
        verify(postRepo, times(1)).findByIdsWithImagesAndUser(List.of(1L));
    }

    @Test
    void testModerationServicesDtoCaching() {
        Slice<Long> ids = new SliceImpl<>(List.of(2L), pageable, true);
        when(postRepoJob.findModerationIds(any(Pageable.class))).thenReturn(ids);
        when(postRepoJob.findByIdsWithImagesAndUser(List.of(2L)))
                .thenReturn(List.of(testService));

        ModerationSliceResponse<PostTextJobBlockedDTO> result1 =
                moderationCacheService.getModerationServicesDto(0, 3, null, null);

        assertNotNull(result1);
        assertEquals(1, result1.getContent().size());
        assertEquals("Тестовая услуга", result1.getContent().get(0).getTitle());

        verify(postRepoJob, times(1)).findModerationIds(any(Pageable.class));

        ModerationSliceResponse<PostTextJobBlockedDTO> result2 =
                moderationCacheService.getModerationServicesDto(0, 3, null, null);

        assertNotNull(result2);
        verify(postRepoJob, times(1)).findModerationIds(any(Pageable.class));
    }

    @Test
    void testModerationVacanciesDtoCaching() {
        Slice<Long> ids = new SliceImpl<>(List.of(3L), pageable, true);
        when(postVacancyRepository.findModerationIds(any(Pageable.class))).thenReturn(ids);
        when(postVacancyRepository.findByIdsWithImagesAndUser(List.of(3L)))
                .thenReturn(List.of(testVacancy));

        ModerationSliceResponse<VacancyBlockedDTO> result1 =
                moderationCacheService.getModerationVacanciesDto(0, 3, null, null);

        assertNotNull(result1);
        assertEquals(1, result1.getContent().size());
        assertEquals("Тестовая вакансия", result1.getContent().get(0).getTitle());

        verify(postVacancyRepository, times(1)).findModerationIds(any(Pageable.class));

        ModerationSliceResponse<VacancyBlockedDTO> result2 =
                moderationCacheService.getModerationVacanciesDto(0, 3, null, null);

        assertNotNull(result2);
        verify(postVacancyRepository, times(1)).findModerationIds(any(Pageable.class));
    }

    // =================================================================
    // Count caching
    // =================================================================

    @Test
    void testModerationProductsCountCaching() {
        when(postRepo.countByModerationTrue()).thenReturn(5L);

        long count1 = moderationCacheService.getModerationProductsCount();
        assertEquals(5L, count1);
        verify(postRepo, times(1)).countByModerationTrue();

        long count2 = moderationCacheService.getModerationProductsCount();
        assertEquals(5L, count2);
        verify(postRepo, times(1)).countByModerationTrue();
    }

    @Test
    void testModerationServicesCountCaching() {
        when(postRepoJob.countByModerationTrue()).thenReturn(3L);

        long count1 = moderationCacheService.getModerationServicesCount();
        assertEquals(3L, count1);
        verify(postRepoJob, times(1)).countByModerationTrue();

        long count2 = moderationCacheService.getModerationServicesCount();
        assertEquals(3L, count2);
        verify(postRepoJob, times(1)).countByModerationTrue();
    }

    @Test
    void testModerationVacanciesCountCaching() {
        when(postVacancyRepository.countByModerationTrue()).thenReturn(7L);

        long count1 = moderationCacheService.getModerationVacanciesCount();
        assertEquals(7L, count1);
        verify(postVacancyRepository, times(1)).countByModerationTrue();

        long count2 = moderationCacheService.getModerationVacanciesCount();
        assertEquals(7L, count2);
        verify(postVacancyRepository, times(1)).countByModerationTrue();
    }

    // =================================================================
    // Different cache keys
    // =================================================================

    @Test
    void testSearchWithDifferentParametersUsesDifferentCacheKeys() {
        Slice<Long> idsTitle = new SliceImpl<>(List.of(1L), pageable, false);
        Slice<Long> idsNameuse = new SliceImpl<>(List.of(1L), pageable, false);

        when(postRepo.searchModerationIdsByTitle(eq("тест"), any(Pageable.class)))
                .thenReturn(idsTitle);
        when(postRepo.searchModerationIdsByUserName(eq("Тестовый"), any(Pageable.class)))
                .thenReturn(idsNameuse);
        when(postRepo.findByIdsWithImagesAndUser(anyList()))
                .thenReturn(List.of(testProduct));

        moderationCacheService.getModerationProductsDto(0, 3, "тест", "title");
        moderationCacheService.getModerationProductsDto(0, 3, "Тестовый", "nameuse");

        verify(postRepo, times(1)).searchModerationIdsByTitle(eq("тест"), any(Pageable.class));
        verify(postRepo, times(1)).searchModerationIdsByUserName(eq("Тестовый"), any(Pageable.class));

        // Повторные — из кэша
        moderationCacheService.getModerationProductsDto(0, 3, "тест", "title");
        moderationCacheService.getModerationProductsDto(0, 3, "Тестовый", "nameuse");

        verify(postRepo, times(1)).searchModerationIdsByTitle(eq("тест"), any(Pageable.class));
        verify(postRepo, times(1)).searchModerationIdsByUserName(eq("Тестовый"), any(Pageable.class));
    }

    @Test
    void testDifferentPagesHaveDifferentCacheKeys() {
        Slice<Long> idsPage0 = new SliceImpl<>(List.of(1L), PageRequest.of(0, 3), true);
        Slice<Long> idsPage1 = new SliceImpl<>(List.of(2L), PageRequest.of(1, 3), false);

        when(postRepo.findModerationIds(any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(0);
                    return p.getPageNumber() == 0 ? idsPage0 : idsPage1;
                });

        when(postRepo.findByIdsWithImagesAndUser(List.of(1L)))
                .thenReturn(List.of(testProduct));
        when(postRepo.findByIdsWithImagesAndUser(List.of(2L)))
                .thenReturn(List.of(testProduct));

        moderationCacheService.getModerationProductsDto(0, 3, null, null);
        moderationCacheService.getModerationProductsDto(1, 3, null, null);

        verify(postRepo, times(2)).findModerationIds(any(Pageable.class));

        // Повторные — из кэша
        moderationCacheService.getModerationProductsDto(0, 3, null, null);
        moderationCacheService.getModerationProductsDto(1, 3, null, null);

        verify(postRepo, times(2)).findModerationIds(any(Pageable.class));
    }

    // =================================================================
    // Cache eviction
    // =================================================================

    @Test
    void testClearModerationProductsCache() {
        Slice<Long> ids = new SliceImpl<>(List.of(1L), pageable, false);
        when(postRepo.findModerationIds(any(Pageable.class))).thenReturn(ids);
        when(postRepo.findByIdsWithImagesAndUser(anyList())).thenReturn(List.of(testProduct));
        when(postRepo.countByModerationTrue()).thenReturn(1L);

        moderationCacheService.getModerationProductsDto(0, 3, null, null);
        moderationCacheService.getModerationProductsCount();

        verify(postRepo, times(1)).findModerationIds(any(Pageable.class));
        verify(postRepo, times(1)).countByModerationTrue();

        moderationCacheService.clearModerationProductsCache();

        moderationCacheService.getModerationProductsDto(0, 3, null, null);
        moderationCacheService.getModerationProductsCount();

        verify(postRepo, times(2)).findModerationIds(any(Pageable.class));
        verify(postRepo, times(2)).countByModerationTrue();
    }

    @Test
    void testClearAllModerationCaches() {
        Slice<Long> idsP = new SliceImpl<>(List.of(1L), pageable, false);
        Slice<Long> idsS = new SliceImpl<>(List.of(2L), pageable, false);
        Slice<Long> idsV = new SliceImpl<>(List.of(3L), pageable, false);

        when(postRepo.findModerationIds(any(Pageable.class))).thenReturn(idsP);
        when(postRepoJob.findModerationIds(any(Pageable.class))).thenReturn(idsS);
        when(postVacancyRepository.findModerationIds(any(Pageable.class))).thenReturn(idsV);

        when(postRepo.findByIdsWithImagesAndUser(anyList())).thenReturn(List.of(testProduct));
        when(postRepoJob.findByIdsWithImagesAndUser(anyList())).thenReturn(List.of(testService));
        when(postVacancyRepository.findByIdsWithImagesAndUser(anyList()))
                .thenReturn(List.of(testVacancy));

        when(postRepo.countByModerationTrue()).thenReturn(1L);
        when(postRepoJob.countByModerationTrue()).thenReturn(1L);
        when(postVacancyRepository.countByModerationTrue()).thenReturn(1L);

        moderationCacheService.getModerationProductsDto(0, 3, null, null);
        moderationCacheService.getModerationServicesDto(0, 3, null, null);
        moderationCacheService.getModerationVacanciesDto(0, 3, null, null);
        moderationCacheService.getModerationProductsCount();
        moderationCacheService.getModerationServicesCount();
        moderationCacheService.getModerationVacanciesCount();

        verify(postRepo, times(1)).findModerationIds(any(Pageable.class));
        verify(postRepoJob, times(1)).findModerationIds(any(Pageable.class));
        verify(postVacancyRepository, times(1)).findModerationIds(any(Pageable.class));
        verify(postRepo, times(1)).countByModerationTrue();
        verify(postRepoJob, times(1)).countByModerationTrue();
        verify(postVacancyRepository, times(1)).countByModerationTrue();

        moderationCacheService.clearAllModerationCaches();

        moderationCacheService.getModerationProductsDto(0, 3, null, null);
        moderationCacheService.getModerationServicesDto(0, 3, null, null);
        moderationCacheService.getModerationVacanciesDto(0, 3, null, null);
        moderationCacheService.getModerationProductsCount();
        moderationCacheService.getModerationServicesCount();
        moderationCacheService.getModerationVacanciesCount();

        verify(postRepo, times(2)).findModerationIds(any(Pageable.class));
        verify(postRepoJob, times(2)).findModerationIds(any(Pageable.class));
        verify(postVacancyRepository, times(2)).findModerationIds(any(Pageable.class));
        verify(postRepo, times(2)).countByModerationTrue();
        verify(postRepoJob, times(2)).countByModerationTrue();
        verify(postVacancyRepository, times(2)).countByModerationTrue();
    }
}