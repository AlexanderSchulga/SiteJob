package jobforsite.sitejob;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        BlockedPostsCacheTest.TestCacheConfig.class,
        BlockedPostsCacheService.class
})
class BlockedPostsCacheTest {

    /**
     * Минимальная конфигурация: только EnableCaching и CacheManager.
     * Никакого JPA, никакого Spring Boot, никаких двух репозиториев на одну сущность.
     */
    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "blockedProductsSlice",
                    "blockedServicesSlice",
                    "blockedVacanciesSlice",
                    "blockedResumeSlice",
                    "blockedProductsCount",
                    "blockedServicesCount",
                    "blockedVacanciesCount",
                    "blockedResumeCount"
            );
        }
    }

    @MockitoBean private PostRepo postRepo;
    @MockitoBean private PostRepoJob postRepoJob;
    @MockitoBean private PostVacancyRepository postVacancyRepository;
    @MockitoBean private PostResumeRepository postResumeRepository;

    @Autowired private BlockedPostsCacheService blockedPostsCacheService;
    @Autowired private CacheManager cacheManager;

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

        testProduct = new PostText();
        testProduct.setId(1L);
        testProduct.setHashId("product123");
        testProduct.setTitle("Тестовый товар");
        testProduct.setPrice(1000);
        testProduct.setFull_text("Описание тестового товара");
        testProduct.setBlocked(true);
        testProduct.setBlockedAt(LocalDateTime.now());
        testProduct.setUser(testUser);

        testService = new PostTextJob();
        testService.setId(2L);
        testService.setHashId("service123");
        testService.setTitle("Тестовая услуга");
        testService.setPrice(2000);
        testService.setFull_text("Описание тестовой услуги");
        testService.setBlocked(true);
        testService.setBlockedAt(LocalDateTime.now());
        testService.setUser(testUser);

        testVacancy = new Vacancy();
        testVacancy.setId(3L);
        testVacancy.setHashId("vacancy123");
        testVacancy.setTitle("Тестовая вакансия");
        testVacancy.setCash(50000);
        testVacancy.setFull_text("Описание тестовой вакансии");
        testVacancy.setBlocked(true);
        testVacancy.setBlockedAt(LocalDateTime.now());
        testVacancy.setUser(testUser);

        pageable = PageRequest.of(0, 6);
    }

    private void clearAllCaches() {
        for (String name : new String[]{
                "blockedProductsSlice", "blockedServicesSlice", "blockedVacanciesSlice", "blockedResumeSlice",
                "blockedProductsCount", "blockedServicesCount", "blockedVacanciesCount", "blockedResumeCount"
        }) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    // ========== ТОВАРЫ ==========

    @Test
    void testBlockedProductsSliceCaching() {
        Slice<Long> ids = new SliceImpl<>(Collections.singletonList(1L), pageable, true);
        when(postRepo.findBlockedProductIds(any(Pageable.class))).thenReturn(ids);
        when(postRepo.findPostsByIdsWithImages(anyList())).thenReturn(Collections.singletonList(testProduct));

        Slice<PostText> result1 = blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        assertNotNull(result1);
        assertEquals(1, result1.getContent().size());
        assertEquals("Тестовый товар", result1.getContent().get(0).getTitle());

        verify(postRepo, times(1)).findBlockedProductIds(any(Pageable.class));
        verify(postRepo, times(1)).findPostsByIdsWithImages(anyList());

        // Второй вызов — из кэша, репозиторий больше не дёргается
        Slice<PostText> result2 = blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        assertNotNull(result2);
        assertEquals(1, result2.getContent().size());

        verify(postRepo, times(1)).findBlockedProductIds(any(Pageable.class));
        verify(postRepo, times(1)).findPostsByIdsWithImages(anyList());
    }

    @Test
    void testBlockedProductsCountCaching() {
        when(postRepo.countByBlockedTrueAndUserActiveTrue()).thenReturn(5L);

        long count1 = blockedPostsCacheService.getBlockedProductsCount();
        assertEquals(5L, count1);
        verify(postRepo, times(1)).countByBlockedTrueAndUserActiveTrue();

        long count2 = blockedPostsCacheService.getBlockedProductsCount();
        assertEquals(5L, count2);
        verify(postRepo, times(1)).countByBlockedTrueAndUserActiveTrue();
    }

    // ========== УСЛУГИ ==========

    @Test
    void testBlockedServicesSliceCaching() {
        Slice<Long> ids = new SliceImpl<>(Collections.singletonList(2L), pageable, true);
        when(postRepoJob.findBlockedServiceIds(any(Pageable.class))).thenReturn(ids);
        when(postRepoJob.findPostsByIdsWithImages(anyList())).thenReturn(Collections.singletonList(testService));

        Slice<PostTextJob> result1 = blockedPostsCacheService.getBlockedServicesSlice(0, 6, null, null);
        assertNotNull(result1);
        assertEquals(1, result1.getContent().size());
        assertEquals("Тестовая услуга", result1.getContent().get(0).getTitle());

        verify(postRepoJob, times(1)).findBlockedServiceIds(any(Pageable.class));

        Slice<PostTextJob> result2 = blockedPostsCacheService.getBlockedServicesSlice(0, 6, null, null);
        assertNotNull(result2);
        verify(postRepoJob, times(1)).findBlockedServiceIds(any(Pageable.class));
    }

    // ========== ВАКАНСИИ ==========

    @Test
    void testBlockedVacanciesSliceCaching() {
        Slice<Long> ids = new SliceImpl<>(Collections.singletonList(3L), pageable, true);
        when(postVacancyRepository.findBlockedVacancyIds(any(Pageable.class))).thenReturn(ids);
        when(postVacancyRepository.findVacanciesByIdsWithImages(anyList()))
                .thenReturn(Collections.singletonList(testVacancy));

        Slice<Vacancy> result1 = blockedPostsCacheService.getBlockedVacanciesSlice(0, 6, null, null);
        assertNotNull(result1);
        assertEquals(1, result1.getContent().size());
        assertEquals("Тестовая вакансия", result1.getContent().get(0).getTitle());

        verify(postVacancyRepository, times(1)).findBlockedVacancyIds(any(Pageable.class));

        Slice<Vacancy> result2 = blockedPostsCacheService.getBlockedVacanciesSlice(0, 6, null, null);
        assertNotNull(result2);
        verify(postVacancyRepository, times(1)).findBlockedVacancyIds(any(Pageable.class));
    }

    // ========== РАЗНЫЕ КЛЮЧИ ==========

    @Test
    void testSearchWithDifferentParametersUsesDifferentCacheKeys() {
        Slice<Long> idsTitle = new SliceImpl<>(Collections.singletonList(1L), pageable, false);
        Slice<Long> idsNameuse = new SliceImpl<>(Collections.singletonList(1L), pageable, false);

        when(postRepo.searchBlockedProductIdsByTitle(eq("тест"), any(Pageable.class))).thenReturn(idsTitle);
        when(postRepo.searchBlockedProductIdsByUserName(eq("Тестовый"), any(Pageable.class))).thenReturn(idsNameuse);
        when(postRepo.findPostsByIdsWithImages(anyList())).thenReturn(Collections.singletonList(testProduct));

        blockedPostsCacheService.getBlockedProductsSlice(0, 6, "тест", "title");
        blockedPostsCacheService.getBlockedProductsSlice(0, 6, "Тестовый", "nameuse");

        verify(postRepo, times(1)).searchBlockedProductIdsByTitle(eq("тест"), any(Pageable.class));
        verify(postRepo, times(1)).searchBlockedProductIdsByUserName(eq("Тестовый"), any(Pageable.class));

        // Повторные — из кэша
        blockedPostsCacheService.getBlockedProductsSlice(0, 6, "тест", "title");
        blockedPostsCacheService.getBlockedProductsSlice(0, 6, "Тестовый", "nameuse");

        verify(postRepo, times(1)).searchBlockedProductIdsByTitle(eq("тест"), any(Pageable.class));
        verify(postRepo, times(1)).searchBlockedProductIdsByUserName(eq("Тестовый"), any(Pageable.class));
    }

    // ========== ОЧИСТКА КЭША ==========

    @Test
    void testClearBlockedProductsCache() {
        Slice<Long> ids = new SliceImpl<>(Collections.singletonList(1L), pageable, true);
        when(postRepo.findBlockedProductIds(any(Pageable.class))).thenReturn(ids);
        when(postRepo.findPostsByIdsWithImages(anyList())).thenReturn(Collections.singletonList(testProduct));
        when(postRepo.countByBlockedTrueAndUserActiveTrue()).thenReturn(1L);

        blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedProductsCount();

        verify(postRepo, times(1)).findBlockedProductIds(any(Pageable.class));
        verify(postRepo, times(1)).countByBlockedTrueAndUserActiveTrue();

        blockedPostsCacheService.clearBlockedProductsCache();

        blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedProductsCount();

        verify(postRepo, times(2)).findBlockedProductIds(any(Pageable.class));
        verify(postRepo, times(2)).countByBlockedTrueAndUserActiveTrue();
    }

    @Test
    void testClearAllBlockedCaches() {
        Slice<Long> idsProduct = new SliceImpl<>(Collections.singletonList(1L), pageable, false);
        Slice<Long> idsService = new SliceImpl<>(Collections.singletonList(2L), pageable, false);
        Slice<Long> idsVacancy = new SliceImpl<>(Collections.singletonList(3L), pageable, false);

        when(postRepo.findBlockedProductIds(any(Pageable.class))).thenReturn(idsProduct);
        when(postRepoJob.findBlockedServiceIds(any(Pageable.class))).thenReturn(idsService);
        when(postVacancyRepository.findBlockedVacancyIds(any(Pageable.class))).thenReturn(idsVacancy);

        when(postRepo.findPostsByIdsWithImages(anyList())).thenReturn(Collections.singletonList(testProduct));
        when(postRepoJob.findPostsByIdsWithImages(anyList())).thenReturn(Collections.singletonList(testService));
        when(postVacancyRepository.findVacanciesByIdsWithImages(anyList()))
                .thenReturn(Collections.singletonList(testVacancy));

        when(postRepo.countByBlockedTrueAndUserActiveTrue()).thenReturn(1L);
        when(postRepoJob.countByBlockedTrueAndUserActiveTrue()).thenReturn(1L);
        when(postVacancyRepository.countByBlockedTrueAndUserActiveTrue()).thenReturn(1L);

        blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedServicesSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedVacanciesSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedProductsCount();
        blockedPostsCacheService.getBlockedServicesCount();
        blockedPostsCacheService.getBlockedVacanciesCount();

        blockedPostsCacheService.clearAllBlockedCaches();

        blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedServicesSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedVacanciesSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedProductsCount();
        blockedPostsCacheService.getBlockedServicesCount();
        blockedPostsCacheService.getBlockedVacanciesCount();

        verify(postRepo, times(2)).findBlockedProductIds(any(Pageable.class));
        verify(postRepoJob, times(2)).findBlockedServiceIds(any(Pageable.class));
        verify(postVacancyRepository, times(2)).findBlockedVacancyIds(any(Pageable.class));
        verify(postRepo, times(2)).countByBlockedTrueAndUserActiveTrue();
        verify(postRepoJob, times(2)).countByBlockedTrueAndUserActiveTrue();
        verify(postVacancyRepository, times(2)).countByBlockedTrueAndUserActiveTrue();
    }

    // ========== РАЗНЫЕ СТРАНИЦЫ ==========

    @Test
    void testDifferentPagesHaveDifferentCacheKeys() {
        when(postRepo.findBlockedProductIds(any(Pageable.class)))
                .thenAnswer(invocation -> {
                    Pageable p = invocation.getArgument(0);
                    if (p.getPageNumber() == 0) {
                        return new SliceImpl<>(Collections.singletonList(1L),
                                PageRequest.of(0, 6), true);
                    }
                    return new SliceImpl<>(Collections.singletonList(2L),
                            PageRequest.of(1, 6), false);
                });

        PostText page1 = new PostText();
        page1.setId(1L);
        page1.setTitle("Товар со страницы 1");
        PostText page2 = new PostText();
        page2.setId(2L);
        page2.setTitle("Товар со страницы 2");

        when(postRepo.findPostsByIdsWithImages(argThat(list -> list.contains(1L))))
                .thenReturn(Collections.singletonList(page1));
        when(postRepo.findPostsByIdsWithImages(argThat(list -> list.contains(2L))))
                .thenReturn(Collections.singletonList(page2));

        Slice<PostText> r0 = blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        Slice<PostText> r1 = blockedPostsCacheService.getBlockedProductsSlice(1, 6, null, null);

        assertEquals("Товар со страницы 1", r0.getContent().get(0).getTitle());
        assertEquals("Товар со страницы 2", r1.getContent().get(0).getTitle());

        verify(postRepo, times(2)).findBlockedProductIds(any(Pageable.class));

        // Повторные — из кэша
        blockedPostsCacheService.getBlockedProductsSlice(0, 6, null, null);
        blockedPostsCacheService.getBlockedProductsSlice(1, 6, null, null);

        verify(postRepo, times(2)).findBlockedProductIds(any(Pageable.class));
    }
}