package jobforsite.sitejob;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jobforsite.sitejob.config.TestSecurityConfig;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.dto.MessageTicketDTO;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.securityContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class MessageTicketsCacheAndLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepo postRepo;

    @Autowired
    private PostRepoJob postRepoJob;

    @Autowired
    private PostVacancyRepository postVacancyRepository;

    @Autowired
    private CommentRepoPersonal commentRepoPersonal;

    @Autowired
    private CommentRepoJobPersonal commentRepoJobPersonal;

    @Autowired
    private CommentRepoVacPersonal commentRepoVacPersonal;

    // Тестовые кэши для сообщений
    private Cache<String, Integer> unreadCountCache;
    private Cache<String, List<Long>> postIdsWithUnreadCache;
    private Cache<String, List<MessageTicketDTO>> messageTicketsCache;
    private Cache<String, Object> postStatusCache;

    // Rate limiting для тестов
    private Cache<String, RateLimitInfo> rateLimitCache;

    // Статистика для нагрузочных тестов
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger rateLimitedCount = new AtomicInteger(0);
    private AtomicInteger errorCount = new AtomicInteger(0);
    private AtomicLong totalResponseTime = new AtomicLong(0);

    // Константы для кэша (соответствуют CacheConfig.java)
    private static final int CACHE_EXPIRATION_MINUTES = 5;
    private static final int CACHE_MAX_SIZE = 2000;
    private static final int RATE_LIMIT_LOAD = 20;
    private static final int RATE_LIMIT_COUNT = 30;
    private static final int RATE_LIMIT_MARK_ALL = 5;
    private static final int RATE_LIMIT_WINDOW = 60;

    @BeforeEach
    public void setUp() {
        // Инициализация тестовых кэшей
        unreadCountCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        postIdsWithUnreadCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        messageTicketsCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        postStatusCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(RATE_LIMIT_WINDOW, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();

        // Сброс счетчиков
        successCount.set(0);
        rateLimitedCount.set(0);
        errorCount.set(0);
        totalResponseTime.set(0);
    }

    // ==================== ТЕСТЫ КЭШИРОВАНИЯ СООБЩЕНИЙ ====================

    @Test
    public void testUnreadCountCache() {
        System.out.println("\n=== Testing Unread Messages Count Cache ===");

        Long userId = 1L;
        String cacheKey = "unread:user:" + userId;
        Integer testCount = 5;

        // Кэшируем количество непрочитанных
        unreadCountCache.put(cacheKey, testCount);

        // Проверяем наличие в кэше
        Integer cachedCount = unreadCountCache.getIfPresent(cacheKey);
        assertThat(cachedCount).isNotNull();
        assertThat(cachedCount).isEqualTo(5);

        System.out.println("Unread count cached successfully for user " + userId);
        System.out.println("Cache size: " + unreadCountCache.estimatedSize());

        // Проверяем статистику
        var stats = unreadCountCache.stats();
        System.out.println("Cache stats - Hits: " + stats.hitCount() +
                ", Misses: " + stats.missCount() +
                ", Hit rate: " + stats.hitRate());
    }

    @Test
    public void testPostIdsWithUnreadCache() {
        System.out.println("\n=== Testing Post IDs With Unread Cache ===");

        Long userId = 1L;
        String cacheKey = "postIds:user:" + userId + "-product";

        // Создаем тестовые ID постов
        List<Long> testPostIds = Arrays.asList(101L, 102L, 103L);

        // Кэшируем
        postIdsWithUnreadCache.put(cacheKey, testPostIds);

        // Проверяем
        List<Long> cachedIds = postIdsWithUnreadCache.getIfPresent(cacheKey);
        assertThat(cachedIds).isNotNull();
        assertThat(cachedIds.size()).isEqualTo(3);
        assertThat(cachedIds).containsExactly(101L, 102L, 103L);

        System.out.println("Post IDs cached successfully for user " + userId + " (product)");

        // Проверяем для разных типов
        postIdsWithUnreadCache.put("postIds:user:" + userId + "-service", Arrays.asList(201L, 202L));
        postIdsWithUnreadCache.put("postIds:user:" + userId + "-vacancy", Arrays.asList(301L));

        assertThat(postIdsWithUnreadCache.estimatedSize()).isEqualTo(3);
        System.out.println("All post type caches stored separately");
    }

    @Test
    public void testMessageTicketsCache() {
        System.out.println("\n=== Testing Message Tickets Cache ===");

        Long userId = 1L;
        String cacheKey = "tickets:user:" + userId + ":page-0-size-4";

        // Создаем тестовые тикеты
        List<MessageTicketDTO> testTickets = createTestMessageTickets(3);

        // Кэшируем
        messageTicketsCache.put(cacheKey, testTickets);

        // Проверяем
        List<MessageTicketDTO> cachedTickets = messageTicketsCache.getIfPresent(cacheKey);
        assertThat(cachedTickets).isNotNull();
        assertThat(cachedTickets.size()).isEqualTo(3);

        System.out.println("Message tickets cached successfully for user " + userId);
    }

    @Test
    public void testPostStatusCache() {
        System.out.println("\n=== Testing Post Status Cache ===");

        String hashId = "testhash123";
        String cacheKey = "postStatus:" + hashId;

        // Создаем тестовый статус
        Map<String, Object> testStatus = new HashMap<>();
        testStatus.put("available", true);
        testStatus.put("type", "product");

        // Кэшируем
        postStatusCache.put(cacheKey, testStatus);

        // Проверяем
        Map<String, Object> cachedStatus = (Map<String, Object>) postStatusCache.getIfPresent(cacheKey);
        assertThat(cachedStatus).isNotNull();
        assertThat(cachedStatus.get("available")).isEqualTo(true);

        System.out.println("Post status cached successfully for hashId: " + hashId);

        // Проверяем для разных типов
        postStatusCache.put("serviceStatus:testhash456", Map.of("available", false, "type", "service"));
        postStatusCache.put("vacancyStatus:testhash789", Map.of("available", true, "type", "vacancy"));

        assertThat(postStatusCache.estimatedSize()).isEqualTo(3);
        System.out.println("All post type statuses cached separately");
    }

    @Test
    public void testCacheExpiration() throws InterruptedException {
        System.out.println("\n=== Testing Cache Expiration for Messages ===");

        String cacheKey = "unread:user:expire-test";

        // Кэшируем с УМЕНЬШЕННЫМ временем для теста
        Cache<String, Integer> shortLivedCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        shortLivedCache.put(cacheKey, 42);
        assertThat(shortLivedCache.getIfPresent(cacheKey)).isNotNull();
        System.out.println("Cache entry created");

        Thread.sleep(1500); // Ждем 1.5 секунды

        // Проверяем что запись удалилась
        assertThat(shortLivedCache.getIfPresent(cacheKey)).isNull();
        System.out.println("Cache entry expired after 1 second");
    }

    @Test
    public void testCacheEvictionOnNewMessage() {
        System.out.println("\n=== Testing Cache Eviction on New Message ===");

        Long userId = 42L;
        List<String> cacheKeys = Arrays.asList(
                "unread:user:" + userId,
                "postIds:user:" + userId + "-product",
                "postIds:user:" + userId + "-service",
                "postIds:user:" + userId + "-vacancy",
                "tickets:user:" + userId + ":page-0-size-4",
                "tickets:user:" + userId + ":page-1-size-4"
        );

        // Заполняем кэш
        for (String key : cacheKeys) {
            if (key.startsWith("unread")) {
                unreadCountCache.put(key, 5);
            } else if (key.startsWith("postIds")) {
                postIdsWithUnreadCache.put(key, Arrays.asList(101L, 102L));
            } else if (key.startsWith("tickets")) {
                messageTicketsCache.put(key, createTestMessageTickets(2));
            }
            System.out.println("Cached key: " + key);
        }

        System.out.println("\nInitial cache sizes:");
        System.out.println("unreadCountCache: " + unreadCountCache.estimatedSize());
        System.out.println("postIdsWithUnreadCache: " + postIdsWithUnreadCache.estimatedSize());
        System.out.println("messageTicketsCache: " + messageTicketsCache.estimatedSize());

        // Имитируем добавление нового сообщения
        simulateNewMessageReceived(userId);

        System.out.println("\nAfter new message - cache sizes:");
        System.out.println("unreadCountCache: " + unreadCountCache.estimatedSize());
        System.out.println("postIdsWithUnreadCache: " + postIdsWithUnreadCache.estimatedSize());
        System.out.println("messageTicketsCache: " + messageTicketsCache.estimatedSize());

        // Проверяем что все записи для этого пользователя удалены
        for (String key : cacheKeys) {
            if (key.startsWith("unread")) {
                assertThat(unreadCountCache.getIfPresent(key)).isNull();
            } else if (key.startsWith("postIds")) {
                assertThat(postIdsWithUnreadCache.getIfPresent(key)).isNull();
            } else if (key.startsWith("tickets")) {
                assertThat(messageTicketsCache.getIfPresent(key)).isNull();
            }
        }

        System.out.println("All cache entries for user " + userId + " were successfully evicted");
    }

    @Test
    public void testCacheEvictionOnMarkAllRead() {
        System.out.println("\n=== Testing Cache Eviction on Mark All Read ===");

        Long userId = 99L;

        // Заполняем кэш
        unreadCountCache.put("unread:user:" + userId, 10);
        postIdsWithUnreadCache.put("postIds:user:" + userId + "-product", Arrays.asList(101L, 102L));
        postIdsWithUnreadCache.put("postIds:user:" + userId + "-service", Arrays.asList(201L));
        messageTicketsCache.put("tickets:user:" + userId + ":page-0-size-4", createTestMessageTickets(3));

        System.out.println("Initial cache size: " +
                (unreadCountCache.estimatedSize() + postIdsWithUnreadCache.estimatedSize() + messageTicketsCache.estimatedSize()));

        // Имитируем отметку всех как прочитанных
        simulateMarkAllAsRead(userId);

        System.out.println("After mark all read - cache size: " +
                (unreadCountCache.estimatedSize() + postIdsWithUnreadCache.estimatedSize() + messageTicketsCache.estimatedSize()));

        // Проверяем что все кэши для пользователя очищены
        assertThat(unreadCountCache.getIfPresent("unread:user:" + userId)).isNull();
        assertThat(postIdsWithUnreadCache.getIfPresent("postIds:user:" + userId + "-product")).isNull();
        assertThat(postIdsWithUnreadCache.getIfPresent("postIds:user:" + userId + "-service")).isNull();
        assertThat(messageTicketsCache.getIfPresent("tickets:user:" + userId + ":page-0-size-4")).isNull();
    }

    @Test
    public void testCacheMaxSize() throws InterruptedException {
        System.out.println("\n=== Testing Message Cache Max Size ===");

        long startTime = System.currentTimeMillis();

        // Заполняем кэш до максимума
        for (int i = 0; i < CACHE_MAX_SIZE; i++) {
            String cacheKey = "unread:user:" + i;
            unreadCountCache.put(cacheKey, i % 10);
        }

        System.out.println("Filled cache with " + CACHE_MAX_SIZE + " entries in " +
                (System.currentTimeMillis() - startTime) + " ms");

        long size = unreadCountCache.estimatedSize();
        System.out.println("Cache size: " + size);

        // Более гибкая проверка для estimatedSize()
        assertThat(size).isBetween((long) (CACHE_MAX_SIZE - 10), (long) (CACHE_MAX_SIZE + 10));

        // Добавляем еще одну запись (должна вытеснить старую)
        unreadCountCache.put("unread:user:overflow", 42);

        // Даем время на эвикцию
        Thread.sleep(100);

        size = unreadCountCache.estimatedSize();
        System.out.println("Cache size after overflow: " + size);

        // Размер должен остаться примерно тем же
        assertThat(size).isBetween((long) (CACHE_MAX_SIZE - 10), (long) (CACHE_MAX_SIZE + 10));
        System.out.println("Cache max size maintained after adding overflow entry");
    }

    @Test
    public void testCacheKeyPattern() {
        System.out.println("\n=== Testing Message Cache Key Patterns ===");

        List<String> testKeys = Arrays.asList(
                "unread:user:123",
                "postIds:user:123-product",
                "postIds:user:123-service",
                "postIds:user:123-vacancy",
                "tickets:user:123:page-0-size-4",
                "tickets:user:123:page-1-size-4",
                "postStatus:abc123",
                "serviceStatus:def456",
                "vacancyStatus:ghi789"
        );

        for (String key : testKeys) {
            if (key.startsWith("unread")) {
                unreadCountCache.put(key, 5);
            } else if (key.startsWith("postIds")) {
                postIdsWithUnreadCache.put(key, new ArrayList<>());
            } else if (key.startsWith("tickets")) {
                messageTicketsCache.put(key, new ArrayList<>());
            } else if (key.contains("Status")) {
                postStatusCache.put(key, new HashMap<>());
            }
            System.out.println("Cached key: " + key);
        }

        assertThat(unreadCountCache.estimatedSize()).isEqualTo(1);
        assertThat(postIdsWithUnreadCache.estimatedSize()).isEqualTo(3);
        assertThat(messageTicketsCache.estimatedSize()).isEqualTo(2);
        assertThat(postStatusCache.estimatedSize()).isEqualTo(3);

        System.out.println("\nCache keys are properly namespaced by type");
    }

    // ==================== НАГРУЗОЧНЫЕ ТЕСТЫ СООБЩЕНИЙ ====================

    @Test
    public void testLoadMessageTicketsUnderLoad() throws InterruptedException {
        int threadCount = 30;
        int requestsPerThread = 5;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        // Получаем реального пользователя из базы
        User testUser = getUserForTest("fef");
        if (testUser == null) {
            System.out.println("No test user found, skipping test");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        System.out.println("\n=== Starting Load Message Tickets Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("==========================================");

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
                        if (!checkRateLimit(testUser.getUsername(), "load", RATE_LIMIT_LOAD)) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        mockMvc.perform(get("/user/notifications_message/load")
                                        .param("page", String.valueOf(j % 3))
                                        .param("size", "4")
                                        .with(securityContext(context)))
                                .andExpect(status().isOk());

                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        if (j % 10 == 0) {
                            System.out.printf("Load tickets success - Response time: %d ms%n", responseTime);
                        }

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error loading tickets: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        printTestResults("Load Message Tickets Test", totalRequests, totalTime);
    }

    @Test
    public void testGetUnreadCountUnderLoad() throws InterruptedException {
        int threadCount = 40;
        int requestsPerThread = 8;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        User testUser = getUserForTest("fef");
        if (testUser == null) {
            System.out.println("No test user found, skipping test");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        System.out.println("\n=== Starting Get Unread Count Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("===========================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStartTime = System.currentTimeMillis();

                        if (!checkRateLimit(testUser.getUsername(), "count", RATE_LIMIT_COUNT)) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        MvcResult result = mockMvc.perform(get("/user/notifications_message/count")
                                        .with(securityContext(context)))
                                .andExpect(status().isOk())
                                .andReturn();

                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        String content = result.getResponse().getContentAsString();
                        if (j % 20 == 0) {
                            System.out.printf("Get count success - Response: %s, Time: %d ms%n",
                                    content, responseTime);
                        }

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error getting count: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        printTestResults("Get Unread Count Load Test", totalRequests, totalTime);
    }

    @Test
    public void testMarkAllMessagesAsReadUnderLoad() throws InterruptedException {
        int threadCount = 15;
        int requestsPerThread = 2;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        List<User> testUsers = getMultipleTestUsers(Arrays.asList("fef", "tet", "wew"));
        if (testUsers.isEmpty()) {
            System.out.println("No test users found, skipping test");
            return;
        }

        System.out.println("\n=== Starting Mark All Messages As Read Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("==================================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final User currentUser = testUsers.get(i % testUsers.size());

            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
                );
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStartTime = System.currentTimeMillis();

                        if (!checkRateLimit(currentUser.getUsername(), "mark-all", RATE_LIMIT_MARK_ALL)) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        mockMvc.perform(post("/user/notifications_message/mark-all-read")
                                        .with(securityContext(context)))
                                .andExpect(status().isOk());

                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        System.out.printf("Mark all read by %s - Response time: %d ms%n",
                                currentUser.getUsername(), responseTime);

                        // Небольшая задержка между запросами
                        Thread.sleep(100);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error marking all as read: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        printTestResults("Mark All Messages As Read Load Test", totalRequests, totalTime);
    }

    @Test
    public void testMessageRedirectsUnderLoad() throws InterruptedException {
        int threadCount = 25;
        int requestsPerThread = 4;
        int totalRequests = threadCount * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        User testUser = getUserForTest("fef");
        if (testUser == null) {
            System.out.println("No test user found, skipping test");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());

        // Тестовые hashId (замените на реальные из базы)
        String[] testHashIds = {"aa279c1ed9d34f308a3c48d2333703da", "testhash456", "testhash789"};
        String[] redirectTypes = {"post", "service", "vacancy"};

        System.out.println("\n=== Starting Message Redirects Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Requests per thread: " + requestsPerThread);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("===========================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long requestStartTime = System.currentTimeMillis();

                        String type = redirectTypes[j % redirectTypes.length];
                        String hashId = testHashIds[j % testHashIds.length];
                        Long messageId = 100L + j; // Тестовый ID сообщения

                        if (!checkRateLimit(testUser.getUsername(), "redirect", 10)) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        String url = String.format("/message/%s/%s?messageId=%d", type, hashId, messageId);

                        mockMvc.perform(get(url)
                                        .with(securityContext(context)))
                                .andExpect(status().is3xxRedirection());

                        long responseTime = System.currentTimeMillis() - requestStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        if (j % 5 == 0) {
                            System.out.printf("Redirect to %s - Response time: %d ms%n", type, responseTime);
                        }

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error in redirect: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        printTestResults("Message Redirects Load Test", totalRequests, totalTime);
    }

    @Test
    public void testMixedMessageOperationsLoad() throws InterruptedException {
        int threadCount = 30;
        int operationsPerThread = 8;
        int totalOperations = threadCount * operationsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalOperations);

        List<User> testUsers = getMultipleTestUsers(Arrays.asList("fef", "tet", "wew"));
        if (testUsers.isEmpty()) {
            System.out.println("No test users found, skipping test");
            return;
        }

        String[] testHashIds = {"aa279c1ed9d34f308a3c48d2333703da", "testhash456", "testhash789"};
        String[] operations = {"load", "count", "redirect"};

        System.out.println("\n=== Starting Mixed Message Operations Load Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Total operations: " + totalOperations);
        System.out.println("==================================================");

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

                        int rateLimit = 0;
                        switch (operation) {
                            case "load": rateLimit = RATE_LIMIT_LOAD; break;
                            case "count": rateLimit = RATE_LIMIT_COUNT; break;
                            case "redirect": rateLimit = 10; break;
                        }

                        if (!checkRateLimit(currentUser.getUsername(), operation, rateLimit)) {
                            rateLimitedCount.incrementAndGet();
                            latch.countDown();
                            continue;
                        }

                        switch (operation) {
                            case "load":
                                mockMvc.perform(get("/user/notifications_message/load")
                                                .param("page", "0")
                                                .param("size", "4")
                                                .with(securityContext(context)))
                                        .andExpect(status().isOk());
                                break;
                            case "count":
                                mockMvc.perform(get("/user/notifications_message/count")
                                                .with(securityContext(context)))
                                        .andExpect(status().isOk());
                                break;
                            case "redirect":
                                String type = j % 2 == 0 ? "post" : "service";
                                String url = String.format("/message/%s/%s", type, testHashIds[j % testHashIds.length]);
                                mockMvc.perform(get(url)
                                                .with(securityContext(context)))
                                        .andExpect(status().is3xxRedirection());
                                break;
                        }

                        long responseTime = System.currentTimeMillis() - operationStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                        if (j % 10 == 0) {
                            System.out.printf("Operation '%s' by %s - Response time: %d ms%n",
                                    operation, currentUser.getUsername(), responseTime);
                        }

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

        latch.await(90, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        printTestResults("Mixed Message Operations Load Test", totalOperations, totalTime);
    }

    @Test
    public void testConcurrentMessageOperationsWithCache() throws InterruptedException {
        int threadCount = 20;
        int operationsPerThread = 10;
        int totalOperations = threadCount * operationsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalOperations);

        User testUser = getUserForTest("fef");
        if (testUser == null) {
            System.out.println("No test user found, skipping test");
            return;
        }

        // Предварительно заполняем кэш
        unreadCountCache.put("unread:user:" + testUser.getId(), 5);
        messageTicketsCache.put("tickets:user:" + testUser.getId() + ":page-0-size-4",
                createTestMessageTickets(3));

        System.out.println("\n=== Starting Concurrent Message Operations With Cache Test ===");
        System.out.println("Threads: " + threadCount);
        System.out.println("Operations per thread: " + operationsPerThread);
        System.out.println("Total operations: " + totalOperations);
        System.out.println("Initial cache size: " + unreadCountCache.estimatedSize());
        System.out.println("============================================================");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities())
                );
                SecurityContextHolder.setContext(context);

                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        long opStartTime = System.currentTimeMillis();

                        // Чередуем read и write операции
                        if (j % 3 == 0) {
                            // Write operation - mark as read
                            mockMvc.perform(post("/user/notifications_message/mark-all-read")
                                            .with(securityContext(context)))
                                    .andExpect(status().isOk());

                            // Инвалидируем кэш
                            simulateMarkAllAsRead(testUser.getId());
                        } else {
                            // Read operation
                            mockMvc.perform(get("/user/notifications_message/count")
                                            .with(securityContext(context)))
                                    .andExpect(status().isOk());

                            // Проверяем кэш
                            unreadCountCache.getIfPresent("unread:user:" + testUser.getId());
                        }

                        long responseTime = System.currentTimeMillis() - opStartTime;
                        successCount.incrementAndGet();
                        totalResponseTime.addAndGet(responseTime);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
                SecurityContextHolder.clearContext();
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        printTestResults("Concurrent Message Operations With Cache Test", totalOperations, totalTime);

        // Проверяем статистику кэша
        var stats = unreadCountCache.stats();
        System.out.println("\nCache Statistics:");
        System.out.println("Hit count: " + stats.hitCount());
        System.out.println("Miss count: " + stats.missCount());
        System.out.println("Hit rate: " + stats.hitRate());
        System.out.println("Eviction count: " + stats.evictionCount());
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private List<MessageTicketDTO> createTestMessageTickets(int count) {
        List<MessageTicketDTO> tickets = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        for (int i = 0; i < count; i++) {
            MessageTicketDTO ticket = new MessageTicketDTO(
                    (long) (100 + i),
                    "hash" + i,
                    "Test Post " + i,
                    i % 2 == 0 ? "product" : "service",
                    "User" + i,
                    LocalDateTime.now().minusHours(i),
                    "Test message preview " + i + "...",
                    false
            );
            ticket.setMessageId((long) (1000 + i));
            tickets.add(ticket);
        }
        return tickets;
    }

    private User getUserForTest(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    private List<User> getMultipleTestUsers(List<String> usernames) {
        List<User> users = new ArrayList<>();
        for (String username : usernames) {
            userRepository.findByUsername(username).ifPresent(users::add);
        }
        return users;
    }

    private boolean checkRateLimit(String username, String action, int limit) {
        String cacheKey = "rate_limit:" + action + ":" + username;
        long now = System.currentTimeMillis() / 1000;

        RateLimitInfo info = rateLimitCache.getIfPresent(cacheKey);

        if (info == null) {
            rateLimitCache.put(cacheKey, new RateLimitInfo(1, now));
            return true;
        }

        if (now - info.windowStart > RATE_LIMIT_WINDOW) {
            rateLimitCache.put(cacheKey, new RateLimitInfo(1, now));
            return true;
        }

        if (info.count >= limit) {
            return false;
        }

        info.count++;
        rateLimitCache.put(cacheKey, info);
        return true;
    }

    private void simulateNewMessageReceived(Long userId) {
        // Удаляем все кэши связанные с пользователем
        unreadCountCache.invalidate("unread:user:" + userId);

        // Удаляем все postIds для всех типов
        postIdsWithUnreadCache.asMap().keySet().removeIf(key ->
                key != null && key.contains("user:" + userId));

        // Удаляем все тикеты для пользователя
        messageTicketsCache.asMap().keySet().removeIf(key ->
                key != null && key.contains("user:" + userId));
    }

    private void simulateMarkAllAsRead(Long userId) {
        // Очищаем все кэши для пользователя при отметке всех как прочитанных
        simulateNewMessageReceived(userId);
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

        double requestsPerSecond = totalProcessed * 1000.0 / totalTime;
        System.out.printf("Requests per second: %.2f%n", requestsPerSecond);

        // Проверки
        assertThat(errorCount.get()).isEqualTo(0);
    }

    // Внутренний класс для rate limiting
    private static class RateLimitInfo {
        int count;
        long windowStart;
        RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}