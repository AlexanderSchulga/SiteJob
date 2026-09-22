package jobforsite.sitejob;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.service.ProductService;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;

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
        PostCacheServiceTest.TestCacheConfig.class,
        PostCacheService.class
})
class PostCacheServiceTest {

    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "productsList", "productsCount",
                    "servicesList", "servicesCount",
                    "vacanciesList", "vacanciesCount",
                    "resumeList", "resumeCount",
                    "realtyList", "realtyCount",
                    "apartmentsList", "vehicleList", "vehicleCount",
                    "productsListNoRegion", "productsCountNoRegion",
                    "servicesPage", "vacanciesPage", "resumePage",
                    "index"
            );
        }
    }

    @MockitoBean private PostRepoJob postRepoJob;
    @MockitoBean private PostVacancyRepository postVacancyRepository;
    @MockitoBean private PostResumeRepository postResumeRepository;
    @MockitoBean private RegionsConfig regionsConfig;
    @MockitoBean private ProductService productService;

    @Autowired private PostCacheService postCacheService;
    @Autowired private CacheManager cacheManager;

    private static final String TEST_CATEGORY = "electronics";
    private static final String TEST_SEARCH = "phone";
    private static final String TEST_REGION = "moscow_oblast";
    private static final String TEST_CITY = "msk_city";
    private static final String TEST_SORT = "newest";
    private static final String TEST_PRICE_SORT = "none";
    private static final int TEST_PAGE = 0;
    private static final int TEST_SIZE = 9;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        clearAllCaches();
        pageable = PageRequest.of(TEST_PAGE, TEST_SIZE, Sort.by(Sort.Direction.DESC, "moderatedAt"));

        // RegionsConfig — города для региона
        when(regionsConfig.getCitiesByRegion(anyString()))
                .thenReturn(List.of("msk_city", "spb_city"));
    }

    private void clearAllCaches() {
        for (String name : new String[]{
                "productsList", "productsCount",
                "servicesList", "servicesCount",
                "vacanciesList", "vacanciesCount",
                "resumeList", "resumeCount",
                "realtyList", "realtyCount",
                "apartmentsList", "vehicleList", "vehicleCount",
                "productsListNoRegion", "productsCountNoRegion",
                "servicesPage", "vacanciesPage", "resumePage",
                "index"
        }) {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        }
    }

    // =================================================================
    // ТОВАРЫ (через ProductService)
    // =================================================================

    @Test
    void testProductsListCache() {
        List<PostText> mockProducts = List.of(
                createTestProduct(1L, "Product 1"),
                createTestProduct(2L, "Product 2")
        );
        when(productService.findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockProducts);

        List<PostText> result1 = postCacheService.getProductsList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertEquals(2, result1.size());
        assertEquals("Product 1", result1.get(0).getTitle());

        verify(productService, times(1)).findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class));

        // Второй вызов — из кэша
        List<PostText> result2 = postCacheService.getProductsList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertEquals(2, result2.size());
        verify(productService, times(1)).findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void testProductsListCacheWithDifferentParams() {
        when(productService.findPostsWithRegionAndCity(
                any(), eq(TEST_SEARCH), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(createTestProduct(1L, "Product 1")));
        when(productService.findPostsWithRegionAndCity(
                any(), eq("different"), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(createTestProduct(2L, "Product 2")));

        postCacheService.getProductsList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);
        postCacheService.getProductsList(
                TEST_CATEGORY, "different", TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        verify(productService, times(1)).findPostsWithRegionAndCity(
                any(), eq(TEST_SEARCH), any(), any(), any(Pageable.class));
        verify(productService, times(1)).findPostsWithRegionAndCity(
                any(), eq("different"), any(), any(), any(Pageable.class));
    }

    @Test
    void testProductsCountCache() {
        when(productService.countPostsWithRegionAndCity(any(), any(), any(), any()))
                .thenReturn(42L);

        long count1 = postCacheService.getProductsCount(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY);
        assertEquals(42L, count1);

        verify(productService, times(1)).countPostsWithRegionAndCity(
                any(), any(), any(), any());

        long count2 = postCacheService.getProductsCount(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY);
        assertEquals(42L, count2);

        verify(productService, times(1)).countPostsWithRegionAndCity(
                any(), any(), any(), any());
    }

    @Test
    void testClearProductsCache() {
        when(productService.findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(createTestProduct(1L, "Test")));

        postCacheService.getProductsList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        postCacheService.clearProductsCache();

        postCacheService.getProductsList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        verify(productService, times(2)).findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class));
    }

    // =================================================================
    // УСЛУГИ
    // =================================================================

    @Test
    void testServicesListCache() {
        List<PostTextJob> mockServices = List.of(
                createTestService(1L, "Service 1"),
                createTestService(2L, "Service 2")
        );
        Slice<Long> ids = new SliceImpl<>(List.of(1L, 2L), pageable, false);

        when(postRepoJob.findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(ids);
        when(postRepoJob.findPostsByIdsWithImages(List.of(1L, 2L)))
                .thenReturn(mockServices);

        List<PostTextJob> result1 = postCacheService.getServicesList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertEquals(2, result1.size());
        assertEquals("Service 1", result1.get(0).getTitle());

        verify(postRepoJob, times(1)).findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class));
        verify(postRepoJob, times(1)).findPostsByIdsWithImages(List.of(1L, 2L));

        // Второй вызов — из кэша
        List<PostTextJob> result2 = postCacheService.getServicesList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertEquals(2, result2.size());
        verify(postRepoJob, times(1)).findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class));
        verify(postRepoJob, times(1)).findPostsByIdsWithImages(List.of(1L, 2L));
    }

    @Test
    void testServicesListEmpty() {
        Slice<Long> emptyIds = new SliceImpl<>(List.of(), pageable, false);
        when(postRepoJob.findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyIds);

        List<PostTextJob> result = postCacheService.getServicesList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertTrue(result.isEmpty());
        verify(postRepoJob, never()).findPostsByIdsWithImages(anyList());
    }

    @Test
    void testServicesCountCache() {
        when(postRepoJob.countWithAllFilters(any(), any(), any(), any()))
                .thenReturn(24L);

        long count1 = postCacheService.getServicesCount(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY);
        assertEquals(24L, count1);

        long count2 = postCacheService.getServicesCount(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY);
        assertEquals(24L, count2);

        verify(postRepoJob, times(1)).countWithAllFilters(any(), any(), any(), any());
    }

    @Test
    void testClearServicesCache() {
        Slice<Long> ids = new SliceImpl<>(List.of(1L), pageable, false);
        when(postRepoJob.findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(ids);
        when(postRepoJob.findPostsByIdsWithImages(anyList()))
                .thenReturn(List.of(createTestService(1L, "Test")));

        postCacheService.getServicesList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        postCacheService.clearServicesCache();

        postCacheService.getServicesList(
                TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        verify(postRepoJob, times(2)).findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class));
    }

    // =================================================================
    // ВАКАНСИИ
    // =================================================================

    @Test
    void testVacanciesListCache() {
        List<Vacancy> mockVacancies = List.of(
                createTestVacancy(1L, "Vacancy 1"),
                createTestVacancy(2L, "Vacancy 2")
        );
        Slice<Long> ids = new SliceImpl<>(List.of(1L, 2L), pageable, false);

        when(postVacancyRepository.findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(ids);
        when(postVacancyRepository.findVacanciesByIdsWithImages(List.of(1L, 2L)))
                .thenReturn(mockVacancies);

        List<Vacancy> result1 = postCacheService.getVacanciesList(
                TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertEquals(2, result1.size());
        assertEquals("Vacancy 1", result1.get(0).getTitle());

        verify(postVacancyRepository, times(1)).findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class));
        verify(postVacancyRepository, times(1)).findVacanciesByIdsWithImages(List.of(1L, 2L));

        // Второй вызов — из кэша
        List<Vacancy> result2 = postCacheService.getVacanciesList(
                TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        assertEquals(2, result2.size());
        verify(postVacancyRepository, times(1)).findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void testVacanciesCountCache() {
        when(postVacancyRepository.countWithAllFilters(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(15L);

        long count1 = postCacheService.getVacanciesCount(
                TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY);
        assertEquals(15L, count1);

        long count2 = postCacheService.getVacanciesCount(
                TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY);
        assertEquals(15L, count2);

        verify(postVacancyRepository, times(1)).countWithAllFilters(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void testClearVacanciesCache() {
        Slice<Long> ids = new SliceImpl<>(List.of(1L), pageable, false);
        when(postVacancyRepository.findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(ids);
        when(postVacancyRepository.findVacanciesByIdsWithImages(anyList()))
                .thenReturn(List.of(createTestVacancy(1L, "Test")));

        postCacheService.getVacanciesList(
                TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        postCacheService.clearVacanciesCache();

        postCacheService.getVacanciesList(
                TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        verify(postVacancyRepository, times(2)).findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    // =================================================================
    // ОЧИСТКА ВСЕХ КЭШЕЙ
    // =================================================================

    @Test
    void testClearAllPostCaches() {
        when(productService.findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(createTestProduct(1L, "Product")));

        Slice<Long> serviceIds = new SliceImpl<>(List.of(1L), pageable, false);
        when(postRepoJob.findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(serviceIds);
        when(postRepoJob.findPostsByIdsWithImages(anyList()))
                .thenReturn(List.of(createTestService(1L, "Service")));

        Slice<Long> vacancyIds = new SliceImpl<>(List.of(1L), pageable, false);
        when(postVacancyRepository.findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(vacancyIds);
        when(postVacancyRepository.findVacanciesByIdsWithImages(anyList()))
                .thenReturn(List.of(createTestVacancy(1L, "Vacancy")));

        // Заполняем кэши
        postCacheService.getProductsList(TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);
        postCacheService.getServicesList(TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);
        postCacheService.getVacanciesList(TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        postCacheService.clearAllPostCaches();

        // Повторные вызовы — должны снова дёрнуть источники
        postCacheService.getProductsList(TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);
        postCacheService.getServicesList(TEST_CATEGORY, TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);
        postCacheService.getVacanciesList(TEST_CATEGORY, "1-3", "remote", TEST_SEARCH, TEST_REGION, TEST_CITY,
                TEST_SORT, TEST_PRICE_SORT, TEST_PAGE, TEST_SIZE);

        verify(productService, times(2)).findPostsWithRegionAndCity(
                any(), any(), any(), any(), any(Pageable.class));
        verify(postRepoJob, times(2)).findPostJobIdsWithFiltersSlice(
                any(), any(), any(), any(), any(Pageable.class));
        verify(postVacancyRepository, times(2)).findVacancyIdsWithFiltersSlice(
                any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    // =================================================================
    // helpers
    // =================================================================

    private PostText createTestProduct(Long id, String title) {
        PostText post = new PostText();
        post.setId(id);
        post.setTitle(title);
        post.setHashId("hash_" + id);
        return post;
    }

    private PostTextJob createTestService(Long id, String title) {
        PostTextJob post = new PostTextJob();
        post.setId(id);
        post.setTitle(title);
        post.setHashId("hash_" + id);
        return post;
    }

    private Vacancy createTestVacancy(Long id, String title) {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(id);
        vacancy.setTitle(title);
        vacancy.setHashId("hash_" + id);
        return vacancy;
    }
}