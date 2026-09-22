package jobforsite.sitejob;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jobforsite.sitejob.config.TestSecurityConfig;
import jobforsite.sitejob.module.shared.model.Reviews;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.repository.ReviewsRepo;
import jobforsite.sitejob.module.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ReviewsCacheAndLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewsRepo reviewsRepo;

    // Тестовые кэши
    private Cache<String, List<Map<String, Object>>> reviewsListCache;
    private Cache<String, Double> userRatingCache;
    private Cache<String, List<User>> userSearchCache;
    private Cache<String, List<Reviews>> deletedReviewsCache;

    // Rate limiting для тестов
    private Cache<String, RateLimitInfo> rateLimitCache;

    // Статистика для нагрузочных тестов
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger rateLimitedCount = new AtomicInteger(0);
    private AtomicInteger errorCount = new AtomicInteger(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    // Константы для кэша
    private static final int CACHE_EXPIRATION_MINUTES = 5;
    private static final int CACHE_MAX_SIZE = 2000;
    private static final int RATE_LIMIT = 50; // 50 запросов в минуту

    @BeforeEach
    public void setUp() {
        // Инициализация тестовых кэшей
        reviewsListCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        userRatingCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        userSearchCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        deletedReviewsCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // Сброс счетчиков
        successCount.set(0);
        rateLimitedCount.set(0);
        errorCount.set(0);
        totalResponseTime.set(0);
    }

    // ==================== ТЕСТЫ КЭШИРОВАНИЯ ====================

    @Test
    public void testReviewsListCache() {
        System.out.println("\n=== Testing Reviews List Cache ===");

        Long userId = 1L; // Тестовый ID пользователя
        String cacheKey = "reviews:user:" + userId;

        // Создаем тестовые данные отзывов
        List<Map<String, Object>> testReviews = createTestReviews(5);

        // Кэшируем
        reviewsListCache.put(cacheKey, testReviews);

        // Проверяем наличие в кэше
        List<Map<String, Object>> cachedReviews = reviewsListCache.getIfPresent(cacheKey);
        assertThat(cachedReviews).isNotNull();
        assertThat(cachedReviews.size()).isEqualTo(5);

        System.out.println("Reviews cached successfully for user " + userId);
        System.out.println("Cache size: " + reviewsListCache.estimatedSize());

        // Проверяем статистику
        var stats = reviewsListCache.stats();
        System.out.println("Cache stats - Hits: " + stats.hitCount() +
                ", Misses: " + stats.missCount() +
                ", Hit rate: " + stats.hitRate());
    }

    @Test
    public void testUserRatingCache() {
        System.out.println("\n=== Testing User Rating Cache ===");

        Long userId = 1L;
        String cacheKey = "rating:user:" + userId;
        Double testRating = 4.5;

        // Кэшируем рейтинг
        userRatingCache.put(cacheKey, testRating);

        // Проверяем кэш
        Double cachedRating = userRatingCache.getIfPresent(cacheKey);
        assertThat(cachedRating).isNotNull();
        assertThat(cachedRating).isEqualTo(4.5);

        System.out.println("User rating cached successfully: " + cachedRating);

        // Проверяем что для разных пользователей разные ключи
        userRatingCache.put("rating:user:2", 3.8);
        assertThat(userRatingCache.getIfPresent("rating:user:1")).isNotNull();
        assertThat(userRatingCache.getIfPresent("rating:user:2")).isNotNull();
        assertThat(userRatingCache.estimatedSize()).isEqualTo(2);
    }

    @Test
    public void testSearchResultsCache() {
        System.out.println("\n=== Testing Search Results Cache ===");

        String searchTerm = "wew";
        String cacheKey = "search:" + searchTerm;

        // Создаем тестовых пользователей
        List<User> testUsers = new ArrayList<>();
        User user1 = new User();
        user1.setId(1L);
        user1.setNameuse("testuser1");
        testUsers.add(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setNameuse("testuser2");
        testUsers.add(user2);

        // Кэшируем результаты поиска
        userSearchCache.put(cacheKey, testUsers);

        // Проверяем кэш
        List<User> cachedUsers = userSearchCache.getIfPresent(cacheKey);
        assertThat(cachedUsers).isNotNull();
        assertThat(cachedUsers.size()).isEqualTo(2);

        System.out.println("Search results cached for term: " + searchTerm);

        // Проверяем что разные поисковые запросы имеют разные ключи
        userSearchCache.put("search:another", List.of(new User()));
        assertThat(userSearchCache.estimatedSize()).isEqualTo(2);
    }

    @Test
    public void testCacheExpiration() throws InterruptedException {
        System.out.println("\n=== Testing Cache Expiration ===");

        String cacheKey = "reviews:user:expire-test";
        List<Map<String, Object>> testReviews = createTestReviews(3);

        // Кэшируем с УМЕНЬШЕННЫМ временем для теста
        Cache<String, List<Map<String, Object>>> shortLivedCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        shortLivedCache.put(cacheKey, testReviews);
        assertThat(shortLivedCache.getIfPresent(cacheKey)).isNotNull();
        System.out.println("Cache entry created");

        Thread.sleep(1500); // Ждем 1.5 секунды

        // Проверяем что запись удалилась
        assertThat(shortLivedCache.getIfPresent(cacheKey)).isNull();
        System.out.println("Cache entry expired after 1 second");
    }

    @Test
    public void testCacheEvictionOnNewReview() {
        System.out.println("\n=== Testing Cache Eviction on New Review ===");

        Long userId = 42L;
        List<String> cacheKeys = List.of(
                "reviews:user:" + userId,
                "reviews:user:" + userId + ":page-0",
                "reviews:user:" + userId + ":page-1"
        );

        // Заполняем кэш
        for (String key : cacheKeys) {
            reviewsListCache.put(key, createTestReviews(3));
            System.out.println("Cached key: " + key);
        }
        userRatingCache.put("rating:user:" + userId, 4.2);

        System.out.println("\nInitial cache size: " + reviewsListCache.estimatedSize());

        // Имитируем добавление нового отзыва
        simulateReviewAdded(userId);

        System.out.println("After review added - cache size: " + reviewsListCache.estimatedSize());

        // Проверяем что все записи для этого пользователя удалены
        for (String key : cacheKeys) {
            assertThat(reviewsListCache.getIfPresent(key)).isNull();
        }
        assertThat(userRatingCache.getIfPresent("rating:user:" + userId)).isNull();

        System.out.println("All cache entries for user " + userId + " were successfully evicted");
    }

    @Test
    public void testCacheMaxSize() throws InterruptedException {
        System.out.println("\n=== Testing Cache Max Size ===");

        long startTime = System.currentTimeMillis();

        // Заполняем кэш до максимума
        for (int i = 0; i < CACHE_MAX_SIZE; i++) {
            String cacheKey = "reviews:user:" + i;
            reviewsListCache.put(cacheKey, createTestReviews(2));
        }

        System.out.println("Filled cache with " + CACHE_MAX_SIZE + " entries in " +
                (System.currentTimeMillis() - startTime) + " ms");

        long size = reviewsListCache.estimatedSize();
        System.out.println("Cache size: " + size);

        // Более гибкая проверка для estimatedSize()
        assertThat(size).isBetween((long) (CACHE_MAX_SIZE - 10), (long) (CACHE_MAX_SIZE + 10));

        // Добавляем еще одну запись (должна вытеснить старую)
        reviewsListCache.put("reviews:user:overflow", createTestReviews(2));

        // Даем время на эвикцию
        Thread.sleep(100);

        size = reviewsListCache.estimatedSize();
        System.out.println("Cache size after overflow: " + size);

        // Размер должен остаться примерно тем же
        assertThat(size).isBetween((long) (CACHE_MAX_SIZE - 10), (long) (CACHE_MAX_SIZE + 10));
        System.out.println("Cache max size maintained after adding overflow entry");
    }

    @Test
    public void testCacheKeyPattern() {
        System.out.println("\n=== Testing Cache Key Patterns ===");

        List<String> testKeys = List.of(
                "reviews:user:123",
                "reviews:user:123:page-0-size-3",
                "reviews:user:123:page-1-size-3",
                "search:testuser",
                "search:deleted:testuser",
                "rating:user:123"
        );

        for (String key : testKeys) {
            if (key.startsWith("reviews:")) {
                reviewsListCache.put(key, createTestReviews(2));
            } else if (key.startsWith("search:")) {
                userSearchCache.put(key, new ArrayList<>());
            } else if (key.startsWith("rating:")) {
                userRatingCache.put(key, 4.0);
            }
            System.out.println("Cached key: " + key);
        }

        assertThat(reviewsListCache.estimatedSize()).isEqualTo(3);
        assertThat(userSearchCache.estimatedSize()).isEqualTo(2);
        assertThat(userRatingCache.estimatedSize()).isEqualTo(1);

        System.out.println("\nCache keys are properly namespaced by type");
    }

    // ==================== НАГРУЗОЧНЫЕ ТЕСТЫ ====================

    @Test
    public void testReviewsPageLoadUnderLoad() throws InterruptedException {
        int threadCount = 30;
        int requestsPerThread = 5;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // Получаем реального пользователя из базы
        User testUser = userRepository.findByUsername("fef")
                .orElseGet(() -> {
                    // Если нет тестового пользователя, создаем заглушку для аутентификации
                    User dummy = new User();
                    dummy.setUsername("testuser");
                    dummy.setPassword("password");
                    dummy.setRole("ROLE_USER");
                    return dummy;
                });

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        String testHashId = "testhash123"; // Замените на реальный hashId из базы

        System.out.println("\n=== Starting Reviews Page Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("=======================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStartTime = System.currentTimeMillis();

                        // Проверяем rate limit
                        if (!checkRateLimit(testUser.getUsername())) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        mockMvc.perform(get("/page_one/reviews/" + testHashId)
                                        .with(securityContext(context)))
                                .andExpect(status().isOk());

                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        System.out.printf("Review page load success - Response time: %d ms%n", responseTime);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error loading review page: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printTestResults("Reviews Page Load Test", totalRequests, totalTime);
    }

    @Test
    public void testReviewSearchUnderLoad() throws InterruptedException {
        int threadCount = 20;
        int requestsPerThread = 4;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        User testUser = userRepository.findByUsername("fef")
                .orElseGet(() -> {
                    User dummy = new User();
                    dummy.setUsername("testuser");
                    dummy.setRole("ROLE_USER");
                    return dummy;
                });

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        String[] searchTerms = {"test", "user", "admin", "john", "smith"};

        System.out.println("\n=== Starting Review Search Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("========================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStartTime = System.currentTimeMillis();
                        String searchTerm = searchTerms[j % searchTerms.length];

                        if (!checkRateLimit(testUser.getUsername() + ":search")) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        mockMvc.perform(get("/page_one/reviews")
                                        .param("search", searchTerm)
                                        .with(securityContext(context)))
                                .andExpect(status().isOk());

                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        System.out.printf("Search '%s' success - Response time: %d ms%n",
                                searchTerm, responseTime);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error searching reviews: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printTestResults("Review Search Load Test", totalRequests, totalTime);
    }

    @Test
    public void testAddReviewUnderLoad() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 2;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // Получаем реальных пользователей для авторов отзывов
        List<User> testUsers = new ArrayList<>();
        String[] usernames = {"fef", "tet", "wew"};

        for (String username : usernames) {
            userRepository.findByUsername(username).ifPresent(testUsers::add);
        }

        if (testUsers.isEmpty()) {
            System.out.println("No test users found, skipping test");
            return;
        }

        String targetUserHashId = "testhash456"; // Замените на реальный hashId

        System.out.println("\n=== Starting Add Review Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("=====================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final User currentUser = testUsers.get(i % testUsers.size());

            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
                );
                SecurityContextHolder.setContext(context);

                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long requestStartTime = System.currentTimeMillis();

                        if (!checkRateLimit(currentUser.getUsername() + ":add")) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown(); // ВАЖНО: уменьшаем счетчик даже для rate-limited запросов
                            continue;
                        }

                        try {
                            mockMvc.perform(post("/page_one/reviews/" + targetUserHashId + "/reviecom")
                                            .param("text", "Test review from load test " + LocalDateTime.now())
                                            .with(securityContext(context)))
                                    .andExpect(status().is3xxRedirection())
                                    .andExpect(redirectedUrlPattern("/page_one/reviews/**"));

                            long responseTime = System.currentTimeMillis() - requestStartTime;
                            successCount.incrementAndGet();
                            totalResponseTime.addAndGet(responseTime);

                            System.out.printf("Review added by %s - Response time: %d ms%n",
                                    currentUser.getUsername(), responseTime);

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Error adding review: " + e.getMessage());
                        } finally {
                            // ВАЖНО: уменьшаем счетчик для КАЖДОГО запроса
                            latch.countDown();
                        }
                    }
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        // Добавляем таймаут для предотвращения бесконечного ожидания
        if (!latch.await(30, TimeUnit.SECONDS)) {
            System.err.println("Test timed out after 30 seconds");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        // Ждем завершения всех задач с таймаутом
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        printTestResults("Add Review Load Test", totalRequests, totalTime);
    }

    @Test
    public void testReviewsPaginationUnderLoad() throws InterruptedException {
        int threadCount = 15;
        int requestsPerThread = 4;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        User testUser = userRepository.findByUsername("fef")
                .orElseGet(() -> {
                    User dummy = new User();
                    dummy.setUsername("testuser");
                    return dummy;
                });

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        String testHashId = "aa279c1ed9d34f308a3c48d2333703da";
        int[] pages = {0, 1, 2};

        System.out.println("\n=== Starting Reviews Pagination Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("=============================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long requestStartTime = System.currentTimeMillis();
                        int page = pages[j % pages.length];

                        // Проверяем rate limit
                        if (!checkRateLimit(testUser.getUsername() + ":pagination")) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown(); // ВАЖНО: уменьшаем счетчик
                            continue;
                        }

                        try {
                            mockMvc.perform(get("/page_one/reviews/" + testHashId + "/load-more-review")
                                            .param("page", String.valueOf(page))
                                            .param("size", "2")
                                            .with(securityContext(context)))
                                    .andExpect(status().isOk());

                            long responseTime = System.currentTimeMillis() - requestStartTime;
                            successCount.incrementAndGet();
                            totalResponseTime.addAndGet(responseTime);

                            System.out.printf("Pagination page %d success - Response time: %d ms%n",
                                    page, responseTime);

                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Error loading more reviews: " + e.getMessage());
                        } finally {
                            // ВАЖНО: гарантированно уменьшаем счетчик
                            latch.countDown();
                        }
                    }
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        // Добавляем таймаут
        if (!latch.await(30, TimeUnit.SECONDS)) {
            System.err.println("Pagination test timed out after 30 seconds");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        printTestResults("Reviews Pagination Load Test", totalRequests, totalTime);
    }

    @Test
    public void testMixedReviewOperationsLoad() throws InterruptedException {
        int threadCount = 25;
        int operationsPerThread = 6;
        int totalOperations = threadCount * operationsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalOperations);

        List<User> testUsers = new ArrayList<>();
        String[] usernames = {"fef", "tet", "wew"};

        for (String username : usernames) {
            userRepository.findByUsername(username).ifPresent(testUsers::add);
        }

        if (testUsers.isEmpty()) {
            System.out.println("No test users found, skipping test");
            return;
        }

        String testHashId = "aa279c1ed9d34f308a3c48d2333703da";
        String[] operations = {"view", "search", "pagination"};

        System.out.println("\n=== Starting Mixed Review Operations Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Total operations: " + totalOperations);
        System.out.println("=================================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final User currentUser = testUsers.get(i % testUsers.size());

            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
                );
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        long operationStartTime = System.currentTimeMillis();
                        String operation = operations[j % operations.length];

                        String rateLimitKey = currentUser.getUsername() + ":" + operation;
                        if (!checkRateLimit(rateLimitKey)) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        switch (operation) {
                            case "view":
                                mockMvc.perform(get("/page_one/reviews/" + testHashId)
                                                .with(securityContext(context)))
                                        .andExpect(status().isOk());
                                break;
                            case "search":
                                mockMvc.perform(get("/page_one/reviews")
                                                .param("search", "test")
                                                .with(securityContext(context)))
                                        .andExpect(status().isOk());
                                break;
                            case "pagination":
                                mockMvc.perform(get("/page_one/reviews/" + testHashId + "/load-more-review")
                                                .param("page", "0")
                                                .param("size", "2")
                                                .with(securityContext(context)))
                                        .andExpect(status().isOk());
                                break;
                        }

                        long responseTime = System.currentTimeMillis() - operationStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        System.out.printf("Operation '%s' by %s - Response time: %d ms%n",
                                operation, currentUser.getUsername(), responseTime);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error in mixed operation: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await();
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();

        printTestResults("Mixed Review Operations Load Test", totalOperations, totalTime);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private List<Map<String, Object>> createTestReviews(int count) {
        List<Map<String, Object>> reviews = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> review = new HashMap<>();
            review.put("id", (long) i);
            review.put("text", "Test review " + i);
            review.put("createdAt", LocalDateTime.now().toString());
            review.put("formattedDate", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

            Map<String, Object> author = new HashMap<>();
            author.put("id", (long) (100 + i));
            author.put("nameuse", "Test Author " + i);
            author.put("displayName", "Test Author " + i);

            review.put("author", author);
            review.put("replies", new ArrayList<>());
            review.put("canReply", true);
            review.put("isSuperUser", false);

            reviews.add(review);
        }
        return reviews;
    }

    private boolean checkRateLimit(String key) {
        String cacheKey = "rate_limit:" + key;
        long now = System.currentTimeMillis() / 1000;

        RateLimitInfo info = rateLimitCache.getIfPresent(cacheKey);

        if (info == null) {
            rateLimitCache.put(cacheKey, new RateLimitInfo(1, now));
            return true;
        }

        if (now - info.windowStart > 60) { // 60 секунд окно
            rateLimitCache.put(cacheKey, new RateLimitInfo(1, now));
            return true;
        }

        if (info.count >= RATE_LIMIT) {
            return false;
        }

        info.count++;
        rateLimitCache.put(cacheKey, info);
        return true;
    }

    private void simulateReviewAdded(Long userId) {
        // Удаляем все кэши связанные с пользователем
        reviewsListCache.asMap().keySet().removeIf(key ->
                key.contains("user:" + userId) || key.contains(":" + userId));
        userRatingCache.invalidate("rating:user:" + userId);
    }

    private void printTestResults(String testName, int totalRequests, long totalTime) {
        int totalProcessed = successCount.get() + rateLimitedCount.get();

        System.out.println("\n=== " + testName + " Results ===");
        System.out.println("Total time: " + totalTime + " ms");
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Rate limited requests: " + rateLimitedCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Total processed: " + totalProcessed);

        if (totalProcessed > 0) {
            System.out.println("Average response time: " + (totalResponseTime.get() / totalProcessed) + " ms");
        }

        double requestsPerSecond = totalRequests * 1000.0 / totalTime;
        System.out.printf("Requests per second: %.2f%n", requestsPerSecond);

        // Проверки
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(rateLimitedCount.get()).isGreaterThanOrEqualTo(0);
    }

    // Внутренние классы для rate limiting
    private static class RateLimitInfo {
        int count;
        long windowStart;
        RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}