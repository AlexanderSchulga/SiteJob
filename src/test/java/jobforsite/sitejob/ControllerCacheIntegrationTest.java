package jobforsite.sitejob;

import jobforsite.sitejob.module.product.controller.RefController;
import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.service.FilterRangeService;
import jobforsite.sitejob.module.product.service.PostTextListService;
import jobforsite.sitejob.module.product.service.PostTypeResolver;
import jobforsite.sitejob.module.product.service.ProductService;
import jobforsite.sitejob.module.product.mapper.PostDetailMapper;
import jobforsite.sitejob.module.product.service.filter.FilterFactory;
import jobforsite.sitejob.module.product.service.filter.ProductFilter;
import jobforsite.sitejob.module.service.controller.RefControllerJob;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.categoryconfig.ExperienceVacancyCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.FilterConfigService;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextJobCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostVacancyCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.WorkFormatVacancyConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.PostViewService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import jobforsite.sitejob.module.vacancy.controller.RefControllerVac;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.auth.service.EmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ControllerCacheIntegrationTest {

    // ===== Общие =====
    @Mock private RateLimitService rateLimitService;
    @Mock private PostCacheService postCacheService;
    @Mock private RegionsConfig regionsConfig;
    @Mock private Authentication authentication;

    // ===== RefController =====
    @Mock private CommentRepoPersonal commentRepoPersonal;
    @Mock private PostTextCategoryConfig postTextCategoryConfig;
    @Mock private PostViewService postViewService;
    @Mock private FilterConfigService filterConfigService;
    @Mock private ProductService productService;
    @Mock private FilterRangeService filterRangeService;
    @Mock private PostTextListService postTextListService;
    @Mock private PostDetailMapper postDetailMapper;
    @Mock private FilterFactory filterFactory;
    @Mock private PostTypeResolver postTypeResolver;
    @Mock private ProductFilter productFilter;

    @InjectMocks private RefController refController;

    // ===== RefControllerJob =====
    @Mock private PostRepoJob postRepoJob;
    @Mock private CommentRepoJobPersonal commentRepoJobPersonal;
    @Mock private PostTextJobCategoryConfig postTextJobCategoryConfig;
    @Mock private ImageService imageService;
    @Mock private RatingRepository ratingRepository;
    @Mock private EmailService emailService;

    @InjectMocks private RefControllerJob refControllerJob;

    // ===== RefControllerVac =====
    @Mock private PostVacancyRepository postVacancyRepository;
    @Mock private CommentRepoVacPersonal commentRepoVacPersonal;
    @Mock private PostVacancyCategoryConfig postVacancyCategoryConfig;
    @Mock private ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;
    @Mock private WorkFormatVacancyConfig workFormatVacancyConfig;

    @InjectMocks private RefControllerVac refControllerVac;

    private MockHttpServletRequest request;
    private Model model;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        model = new BindingAwareModelMap();

        // Rate limit — пропускаем всё
        when(rateLimitService.tryConsumeProductView(anyString())).thenReturn(true);
        when(rateLimitService.tryConsumeServiceView(anyString())).thenReturn(true);
        when(rateLimitService.tryConsumeVacancyView(anyString())).thenReturn(true);
        when(rateLimitService.isIpBlocked(anyString())).thenReturn(false);

        // Нормализация — как в FilterRangeService
        when(filterRangeService.normalizeParam(any()))
                .thenAnswer(inv -> {
                    String p = inv.getArgument(0);
                    return (p != null && !p.trim().isEmpty() && !"all".equals(p)) ? p.trim() : null;
                });

        // Категории (Map<String,String>)
        when(postTextCategoryConfig.getCategories())
                .thenReturn(Map.of("electronics", "Электроника", "auto", "Авто"));
        when(postTextJobCategoryConfig.getCategories())
                .thenReturn(Map.of("cleaning", "Уборка", "repair", "Ремонт"));
        when(postVacancyCategoryConfig.getCategories())
                .thenReturn(Map.of("it", "IT", "sales", "Продажи"));
        when(experienceVacancyCategoryConfig.getExperience())
                .thenReturn(Map.of("1-3", "1-3 года", "3-6", "3-6 лет"));
        when(workFormatVacancyConfig.getWorkformat())
                .thenReturn(Map.of("remote", "Удалённо", "office", "Офис"));

        // Регионы (Map<String,String>) и города (List<String>)
        when(regionsConfig.getRegions())
                .thenReturn(Map.of("all", "Все регионы", "moscow_oblast", "Москва и область", "spb", "СПб"));
        when(regionsConfig.getCitiesByRegion(anyString()))
                .thenReturn(List.of("msk_city", "spb_city"));

        // FilterFactory → мок productFilter
        when(filterFactory.getFilter(any())).thenReturn(productFilter);
        when(productFilter.getCategoryName()).thenReturn("Электроника");
        when(productFilter.getPosts(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(productFilter.getTotalCount(any())).thenReturn(0L);

        // PostTextListService — по умолчанию пусто
        when(postTextListService.toListDTO(anyList())).thenReturn(List.of());

        // PostCacheService (Job)
        when(postCacheService.getServicesList(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(postCacheService.getServicesCount(any(), any(), any(), any()))
                .thenReturn(0L);

        // PostCacheService (Vac)
        when(postCacheService.getVacanciesList(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(postCacheService.getVacanciesCount(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(0L);
    }

    // =================================================================
    // RefController (products)
    // =================================================================

    @Test
    void products_returnsView() {
        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategory("electronics");

        String view = refController.viewsOnePage(filter, 0, 8, authentication, request, model);

        assertEquals("page_one/viewsone", view);
        verify(filterFactory, times(1)).getFilter(any());
        verify(productFilter, times(1)).getPosts(any(), eq(0), eq(8));
        verify(productFilter, times(1)).getTotalCount(any());
        verify(productFilter, times(1)).addModelAttributes(any(), any());
    }

    @Test
    void products_rateLimitBlocks() {
        when(rateLimitService.tryConsumeProductView(anyString())).thenReturn(false);

        ProductFilterDTO filter = new ProductFilterDTO();
        String view = refController.viewsOnePage(filter, 0, 8, authentication, request, model);

        assertEquals("page_one/viewsone", view);
        assertTrue(model.containsAttribute("error"));
        verify(filterFactory, never()).getFilter(any());
        verify(productFilter, never()).getPosts(any(), anyInt(), anyInt());
    }

    @Test
    void products_emptyResults() {
        when(productFilter.getPosts(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(productFilter.getTotalCount(any())).thenReturn(0L);

        ProductFilterDTO filter = new ProductFilterDTO();
        String view = refController.viewsOnePage(filter, 0, 8, authentication, request, model);

        assertEquals("page_one/viewsone", view);
        assertEquals(0L, model.getAttribute("totalItems"));
        assertFalse((Boolean) model.getAttribute("hasMore"));
    }

    @Test
    void products_hasMoreTrue() {
        List<PostText> posts = List.of(
                createTestProduct(1L, "P1"), createTestProduct(2L, "P2"),
                createTestProduct(3L, "P3"), createTestProduct(4L, "P4"),
                createTestProduct(5L, "P5"), createTestProduct(6L, "P6"),
                createTestProduct(7L, "P7"), createTestProduct(8L, "P8")
        );
        doReturn(posts).when(productFilter).getPosts(any(), anyInt(), anyInt());
        when(productFilter.getTotalCount(any())).thenReturn(100L);

        ProductFilterDTO filter = new ProductFilterDTO();
        refController.viewsOnePage(filter, 0, 8, authentication, request, model);

        assertTrue((Boolean) model.getAttribute("hasMore"));
    }

    @Test
    void products_normalizedParameters() {
        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setCategory("all");
        filter.setSearch("");
        filter.setRegion("all");
        filter.setCity("");

        refController.viewsOnePage(filter, 0, 8, authentication, request, model);

        assertNull(filter.getCategory());
        assertNull(filter.getSearch());
        assertNull(filter.getRegion());
        assertNull(filter.getCity());
        assertEquals("newest", filter.getSort());
        assertEquals("none", filter.getPriceSort());
    }

    // =================================================================
    // RefControllerJob (services)
    // =================================================================

    @Test
    void services_returnsView() {
        String view = refControllerJob.viewsOnePage(
                "cleaning", "clean", "newest", "none",
                "spb", "spb_city", 0, 9,
                authentication, request, model
        );

        assertEquals("page_one/viewsonejob", view);
        verify(postCacheService, times(1)).getServicesList(
                eq("cleaning"), eq("clean"), eq("spb"), eq("spb_city"),
                eq("newest"), eq("none"), eq(0), eq(9)
        );
        verify(postCacheService, times(1)).getServicesCount(
                eq("cleaning"), eq("clean"), eq("spb"), eq("spb_city")
        );
        assertTrue(model.containsAttribute("postsj"));
    }

    @Test
    void services_rateLimitBlocks() {
        when(rateLimitService.tryConsumeServiceView(anyString())).thenReturn(false);

        String view = refControllerJob.viewsOnePage(
                null, null, "newest", "none",
                null, null, 0, 9,
                authentication, request, model
        );

        assertEquals("page_one/viewsonejob", view);
        assertTrue(model.containsAttribute("error"));
        verify(postCacheService, never()).getServicesList(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        );
    }

    @Test
    void services_emptyResults() {
        when(postCacheService.getServicesList(
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(postCacheService.getServicesCount(any(), any(), any(), any()))
                .thenReturn(0L);

        String view = refControllerJob.viewsOnePage(
                null, null, "newest", "none",
                null, null, 0, 9,
                authentication, request, model
        );

        assertEquals("page_one/viewsonejob", view);
        assertEquals(0L, model.getAttribute("totalItems"));
        assertFalse((Boolean) model.getAttribute("hasMore"));
    }

    // =================================================================
    // RefControllerVac (vacancies)
    // =================================================================

    @Test
    void vacancies_returnsView() {
        String view = refControllerVac.viewsOnePage(
                "it", "1-3", "remote", "developer",
                "newest", "none", "msk", "msk_city", 0, 9,
                authentication, request, model
        );

        assertEquals("page_one/viewsonevac", view);
        verify(postCacheService, times(1)).getVacanciesList(
                eq("it"), eq("1-3"), eq("remote"), eq("developer"),
                eq("msk"), eq("msk_city"), eq("newest"), eq("none"), eq(0), eq(9)
        );
        verify(postCacheService, times(1)).getVacanciesCount(
                eq("it"), eq("1-3"), eq("remote"), eq("developer"), eq("msk"), eq("msk_city")
        );
        assertTrue(model.containsAttribute("vacvac"));
    }

    @Test
    void vacancies_rateLimitBlocks() {
        when(rateLimitService.tryConsumeVacancyView(anyString())).thenReturn(false);

        String view = refControllerVac.viewsOnePage(
                null, null, null, null,
                "newest", "none", null, null, 0, 9,
                authentication, request, model
        );

        assertEquals("page_one/viewsonevac", view);
        assertTrue(model.containsAttribute("error"));
        verify(postCacheService, never()).getVacanciesList(
                any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()
        );
    }

    @Test
    void vacancies_normalizesExperienceAndWorkformat() {
        refControllerVac.viewsOnePage(
                null, null, null, null,
                "newest", "none", null, null, 0, 9,
                authentication, request, model
        );

        assertEquals("all", model.getAttribute("selectedExperience"));
        assertEquals("all", model.getAttribute("selectedWorkformat"));
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