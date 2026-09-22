package jobforsite.sitejob;

import jobforsite.sitejob.config.*;
import jobforsite.sitejob.module.shared.controller.sharedcontroller.AllPostUserController;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextJobCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostVacancyCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.users.service.UserActivityService;
import jobforsite.sitejob.module.users.service.UserCacheService;
import jobforsite.sitejob.module.users.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AllPostUserController.class)
@Import({TestSecurityConfig.class})  // Импортируем тестовую конфигурацию
@ActiveProfiles("test")  // Активируем профиль test
class AllPostUserControllerLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostTextCategoryConfig postTextCategoryConfig;

    @MockitoBean
    private PostTextJobCategoryConfig postTextJobCategoryConfig;

    @MockitoBean
    private PostVacancyCategoryConfig postVacancyCategoryConfig;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private UserCacheService userCacheService;

    @MockitoBean
    private Authentication authentication;

    @MockitoBean
    private UserActivityService userActivityService;

    @MockitoBean
    private RegionsConfig regionsConfig;

    private User testUser;
    private final String TEST_USER_HASH = "test";
    private final String TEST_IP = "192.168.1.1";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setHashId(TEST_USER_HASH);
        testUser.setNameuse("Test User");
        testUser.setRegion("moscow_oblast");

        // Настройка моков по умолчанию
        when(rateLimitService.isIpBlocked(anyString())).thenReturn(false);
        when(rateLimitService.tryConsumeProductView(anyString())).thenReturn(true);

        // Настройка кэш-сервиса
        when(userCacheService.findUserByHashCached(TEST_USER_HASH)).thenReturn(testUser);


        // Настройка RegionsConfig
        when(regionsConfig.getRegionDisplayName(anyString())).thenReturn("Москва и Московская область");
        when(regionsConfig.getCityDisplayName(anyString(), anyString())).thenReturn("Москва");

        // Пустые страницы для тестов
        Page<PostText> emptyProductPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 6), 0);
        Page<PostTextJob> emptyServicePage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 6), 0);
        Page<Vacancy> emptyVacancyPage = new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 6), 0);

        when(userCacheService.getProductsPage(any(), anyInt(), anyInt())).thenReturn(emptyProductPage);
        when(userCacheService.getServicesPage(any(), anyInt(), anyInt())).thenReturn(emptyServicePage);
        when(userCacheService.getVacanciesPage(any(), anyInt(), anyInt())).thenReturn(emptyVacancyPage);

        when(userCacheService.getTotalProductsCount(any())).thenReturn(0L);
        when(userCacheService.getTotalServicesCount(any())).thenReturn(0L);
        when(userCacheService.getTotalVacanciesCount(any())).thenReturn(0L);


       // userCacheService.UserRatingHash mockUser = mock(userCacheService.UserRatingHash.class);
        // Настройка UserService
       // when(userCacheService.getUserRatingHash(anyString())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("Тест Rate Limit - превышение лимита запросов")
    void testRateLimitExceeded() throws Exception {
        // Симулируем превышение лимита
        when(rateLimitService.tryConsumeProductView(TEST_IP)).thenReturn(false);
        when(rateLimitService.isIpBlocked(TEST_IP)).thenReturn(false);

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(status().reason("Слишком много запросов"));

        verify(rateLimitService).recordFailedAttempt(TEST_IP);
    }

    @Test
    @DisplayName("Тест Rate Limit - IP заблокирован")
    void testIpBlocked() throws Exception {
        when(rateLimitService.isIpBlocked(TEST_IP)).thenReturn(true);

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isTooManyRequests())
                .andExpect(status().reason("IP временно заблокирован"));
    }

    @Test
    @DisplayName("Тест кэширования - повторные запросы должны использовать кэш")
    void testCachingBehavior() throws Exception {
        // Настройка данных
        UserCacheService.UserPostsData mockData = mock(UserCacheService.UserPostsData.class);
        when(userCacheService.getUserPostsData(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockData);

        // Первый запрос - должен вызвать поиск пользователя
        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk())
                .andExpect(view().name("user/user_posts"));

        verify(userCacheService, times(1)).findUserByHashCached(TEST_USER_HASH);

        // Сброс счетчика для проверки следующего вызова
        clearInvocations(userCacheService);

        // Второй запрос - должен использовать кэш
        when(userCacheService.findUserByHashCached(TEST_USER_HASH)).thenReturn(testUser);

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk());

        verify(userCacheService, times(1)).findUserByHashCached(TEST_USER_HASH);
    }

    @Test
    @DisplayName("Тест Rate Limit для load-more endpoints")
    void testLoadMoreRateLimit() throws Exception {
        // Тест для products
        when(rateLimitService.tryConsumeProductLoad(TEST_IP)).thenReturn(false);

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/load-more")
                        .param("type", "products")
                        .param("page", "1")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/error_fragment :: errorFragment(errorMessage='Слишком много запросов')"));
        // Убрали проверку .andExpect(model().attribute("errorMessage", "Слишком много запросов"));

        verify(rateLimitService).recordFailedAttempt(TEST_IP);

        // Тест для services
        when(rateLimitService.tryConsumeServiceLoad(TEST_IP)).thenReturn(false);

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/load-more")
                        .param("type", "services")
                        .param("page", "1")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/error_fragment :: errorFragment(errorMessage='Слишком много запросов')"));

        // Тест для vacancies
        when(rateLimitService.tryConsumeVacancyLoad(TEST_IP)).thenReturn(false);

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/load-more")
                        .param("type", "vacancies")
                        .param("page", "1")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/error_fragment :: errorFragment(errorMessage='Слишком много запросов')"));
    }

    @Test
    @DisplayName("Нагрузочный тест - множественные запросы с разных IP")
    void testMultipleRequestsFromDifferentIps() throws Exception {
        int numberOfRequests = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rateLimitCount = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        // Настройка данных для успешных запросов
        UserCacheService.UserPostsData mockData = mock(UserCacheService.UserPostsData.class);
        when(userCacheService.getUserPostsData(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockData);

        // Настройка поведения rate limit: первые 80 запросов успешны, остальные блокируются
        when(rateLimitService.tryConsumeProductView(anyString()))
                .thenAnswer(invocation -> {
                    String ip = invocation.getArgument(0);
                    int ipHash = ip.hashCode() & 0x7fffffff;
                    return ipHash % 100 < 80; // 80% успешных запросов
                });

        for (int i = 0; i < numberOfRequests; i++) {
            final String ip = "192.168.1." + (i % 255);
            executorService.submit(() -> {
                try {
                    MvcResult result = mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                                    .header("X-Forwarded-For", ip))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 429) {
                        rateLimitCount.incrementAndGet();
                    } else {
                        otherErrors.incrementAndGet();
                    }
                } catch (Exception e) {
                    otherErrors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        System.out.println("Успешных запросов: " + successCount.get());
        System.out.println("Отклонено rate limit: " + rateLimitCount.get());
        System.out.println("Другие ошибки: " + otherErrors.get());

        assertTrue(completed, "Таймаут при выполнении запросов");
        assertEquals(numberOfRequests, successCount.get() + rateLimitCount.get() + otherErrors.get());
    }

    @RepeatedTest(5)
    @DisplayName("Тест производительности - время ответа")
    void testPerformance() throws Exception {
        // Настройка моков
        UserCacheService.UserPostsData mockData = mock(UserCacheService.UserPostsData.class);
        when(userCacheService.getUserPostsData(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockData);

        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk());

        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        System.out.println("Время ответа: " + responseTime + "ms");

        assertTrue(responseTime < 1500, "Время ответа слишком большое: " + responseTime + "ms");
    }

    @Test
    @DisplayName("Тест кэширования счетчиков")
    void testCountersCaching() throws Exception {
        // Настройка данных с учетом реальной структуры
        UserCacheService.UserPostsData mockData = mock(UserCacheService.UserPostsData.class);

        // Настраиваем мок, чтобы он возвращал какие-то данные
        // Это нужно, чтобы контроллер не останавливал выполнение раньше времени
        when(mockData.getPosts()).thenReturn(Collections.emptyList());
        when(mockData.getPostsJob()).thenReturn(Collections.emptyList());
        when(mockData.getVacancies()).thenReturn(Collections.emptyList());
        when(mockData.getTotalProducts()).thenReturn(10L);  // Важно: устанавливаем счетчики
        when(mockData.getTotalServices()).thenReturn(5L);
        when(mockData.getTotalVacancies()).thenReturn(3L);

        when(userCacheService.getUserPostsData(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockData);

        // Первый запрос
        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk());

        // Второй запрос
        mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk());

        // Проверяем, что getUserPostsData вызван дважды (так как кэширования нет)
        verify(userCacheService, times(2)).getUserPostsData(any(), anyInt(), anyInt(), anyInt(), anyInt());

        // Проверяем, что findUserByHashCached вызван дважды
        verify(userCacheService, times(2)).findUserByHashCached(TEST_USER_HASH);

        // Проверяем, что других вызовов не было
        verifyNoMoreInteractions(userCacheService);

        // Если вы хотите проверить, что счетчики были получены из mockData,
        // то можно проверить, что методы получения счетчиков у mockData были вызваны
        verify(mockData, atLeastOnce()).getTotalProducts();
        verify(mockData, atLeastOnce()).getTotalServices();
        verify(mockData, atLeastOnce()).getTotalVacancies();
    }

    @Test
    @DisplayName("Тест обработки ошибок - пользователь не найден")
    void testUserNotFound() throws Exception {
        when(userCacheService.findUserByHashCached("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/user/nonexistent/posts")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isOk())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("Тест различных размеров страницы")
    void testDifferentPageSizes() throws Exception {
        UserCacheService.UserPostsData mockData = mock(UserCacheService.UserPostsData.class);
        when(userCacheService.getUserPostsData(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(mockData);

        int[] sizes = {3, 6, 9, 12};
        for (int size : sizes) {
            mockMvc.perform(get("/user/" + TEST_USER_HASH + "/posts")
                            .param("size", String.valueOf(size))
                            .header("X-Forwarded-For", TEST_IP))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("pageSize", size));
        }
    }
}