package jobforsite.sitejob;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;

@SpringBootTest
public class PostPublicCommentsCacheTest {

    private Cache<String, List<Map<String, Object>>> publicCommentsCache;
    private Cache<String, Long> commentCountsCache;

    private static final int CACHE_EXPIRATION_MINUTES = 1; // Используем 1 минуту для тестов вместо 5
    private static final int CACHE_MAX_SIZE = 2000;

    @BeforeEach
    public void setUp() {
        // Создаем тестовый кэш с УМЕНЬШЕННЫМ временем жизни для тестов
        publicCommentsCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();

        commentCountsCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .recordStats()
                .build();
    }

    @Test
    public void testCacheExpiration() {
        System.out.println("\n=== Testing Public Comments Cache Expiration ===");

        String cacheKey = "abc123-page-0-size-3";
        String countKey = "abc123";

        // Создаем тестовые данные комментариев
        List<Map<String, Object>> testComments = createTestComments(3);

        // Добавляем записи в кэш
        publicCommentsCache.put(cacheKey, testComments);
        commentCountsCache.put(countKey, 5L);

        assertThat(publicCommentsCache.getIfPresent(cacheKey)).isNotNull();
        assertThat(commentCountsCache.getIfPresent(countKey)).isNotNull();

        System.out.println("Cache entries created");
        System.out.println("Public comments cache size: " + publicCommentsCache.estimatedSize());
        System.out.println("Comment counts cache size: " + commentCountsCache.estimatedSize());
        System.out.println("Waiting for expiration (up to " + (CACHE_EXPIRATION_MINUTES + 1) + " minutes)...");

        // Используем Awaitility для асинхронного ожидания
        await().atMost(CACHE_EXPIRATION_MINUTES + 1, MINUTES)
                .pollInterval(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(publicCommentsCache.getIfPresent(cacheKey)).isNull();
                    assertThat(commentCountsCache.getIfPresent(countKey)).isNull();
                });

        System.out.println("Cache entries expired successfully after " + CACHE_EXPIRATION_MINUTES + " minutes");
    }

    @Test
    public void testCacheExpirationWithManualCleanup() throws InterruptedException {
        System.out.println("\n=== Testing Cache Expiration with Manual Cleanup ===");

        String cacheKey = "abc123-page-0-size-3";
        String countKey = "abc123";

        // Создаем тестовые данные
        List<Map<String, Object>> testComments = createTestComments(3);

        // Добавляем записи в кэш
        publicCommentsCache.put(cacheKey, testComments);
        commentCountsCache.put(countKey, 5L);

        System.out.println("Cache entries created at: " + System.currentTimeMillis());
        System.out.println("Waiting 30 seconds...");

        Thread.sleep(30000); // Ждем 30 секунд

        // Проверяем, что записи все еще есть (прошло меньше минуты)
        assertThat(publicCommentsCache.getIfPresent(cacheKey)).isNotNull();
        assertThat(commentCountsCache.getIfPresent(countKey)).isNotNull();
        System.out.println("After 30 seconds: cache entries still present (as expected)");

        System.out.println("Waiting additional 40 seconds...");
        Thread.sleep(40000); // Ждем еще 40 секунд (всего 70 секунд)

        // Принудительно запускаем очистку (Caffeine иногда лениво удаляет)
        publicCommentsCache.cleanUp();
        commentCountsCache.cleanUp();

        // Проверяем, что записи удалились
        assertThat(publicCommentsCache.getIfPresent(cacheKey)).isNull();
        assertThat(commentCountsCache.getIfPresent(countKey)).isNull();

        System.out.println("Cache entries expired successfully after +-70 seconds");
    }

    @Test
    public void testCacheEvictionOnNewComment() {
        System.out.println("\n=== Testing Cache Eviction on New Comment ===");

        String hashId = "testpost123";
        List<String> cacheKeys = generateCacheKeys(hashId, 3);
        String countKey = hashId;

        // Заполняем кэш
        for (int i = 0; i < cacheKeys.size(); i++) {
            List<Map<String, Object>> pageComments = createTestComments(3);
            publicCommentsCache.put(cacheKeys.get(i), pageComments);
            System.out.println("Cached page " + i + " with key: " + cacheKeys.get(i));
        }

        commentCountsCache.put(countKey, 9L);
        System.out.println("Cached comment count: 9");

        System.out.println("\nInitial cache state:");
        System.out.println("  Public comments cache size: " + publicCommentsCache.estimatedSize());
        System.out.println("  Comment counts cache size: " + commentCountsCache.estimatedSize());

        // Проверяем наличие
        for (String key : cacheKeys) {
            assertThat(publicCommentsCache.getIfPresent(key)).isNotNull();
        }
        assertThat(commentCountsCache.getIfPresent(countKey)).isNotNull();

        // Имитируем добавление комментария
        simulateCommentAdded(hashId);

        System.out.println("\nAfter new comment added (cache evicted):");
        System.out.println("  Public comments cache size: " + publicCommentsCache.estimatedSize());
        System.out.println("  Comment counts cache size: " + commentCountsCache.estimatedSize());

        // Проверяем очистку
        for (String key : cacheKeys) {
            assertThat(publicCommentsCache.getIfPresent(key)).isNull();
        }
        assertThat(commentCountsCache.getIfPresent(countKey)).isNull();

        System.out.println("All cache entries for post " + hashId + " were successfully evicted");
    }

    @Test
    public void testCacheWithDifferentPosts() {
        System.out.println("\n=== Testing Cache with Different Posts ===");

        String post1Key = "post1-page-0-size-3";
        String post2Key = "post2-page-0-size-3";
        String countKey1 = "post1";
        String countKey2 = "post2";

        List<Map<String, Object>> post1Comments = createTestComments(3);
        List<Map<String, Object>> post2Comments = createTestComments(3);

        publicCommentsCache.put(post1Key, post1Comments);
        publicCommentsCache.put(post2Key, post2Comments);
        commentCountsCache.put(countKey1, 3L);
        commentCountsCache.put(countKey2, 3L);

        List<Map<String, Object>> cachedPost1 = publicCommentsCache.getIfPresent(post1Key);
        List<Map<String, Object>> cachedPost2 = publicCommentsCache.getIfPresent(post2Key);
        Long cachedCount1 = commentCountsCache.getIfPresent(countKey1);
        Long cachedCount2 = commentCountsCache.getIfPresent(countKey2);

        System.out.println("Post1 first comment text: " + cachedPost1.get(0).get("text"));
        System.out.println("Post2 first comment text: " + cachedPost2.get(0).get("text"));
        System.out.println("Post1 comment count: " + cachedCount1);
        System.out.println("Post2 comment count: " + cachedCount2);

        assertThat(cachedPost1).isNotNull();
        assertThat(cachedPost2).isNotNull();
        assertThat(cachedCount1).isEqualTo(3L);
        assertThat(cachedCount2).isEqualTo(3L);
        assertThat(cachedPost1).isNotSameAs(cachedPost2);
    }

    @Test
    public void testCacheMaxSize() {
        System.out.println("\n=== Testing Cache Max Size ===");

        long startTime = System.currentTimeMillis();

        // Заполняем кэш до максимума
        for (int i = 0; i < CACHE_MAX_SIZE; i++) {
            String cacheKey = "post" + i + "-page-0-size-3";
            List<Map<String, Object>> comments = createTestComments(3);
            publicCommentsCache.put(cacheKey, comments);
            commentCountsCache.put("post" + i, (long) (i % 10));
        }

        System.out.println("Filled cache with " + CACHE_MAX_SIZE + " entries in " +
                (System.currentTimeMillis() - startTime) + " ms");
        System.out.println("Public comments cache size: " + publicCommentsCache.estimatedSize());
        System.out.println("Comment counts cache size: " + commentCountsCache.estimatedSize());

        assertThat(publicCommentsCache.estimatedSize()).isEqualTo(CACHE_MAX_SIZE);
        assertThat(commentCountsCache.estimatedSize()).isEqualTo(CACHE_MAX_SIZE);

        // Добавляем еще одну запись
        publicCommentsCache.put("overflow_post-page-0-size-3", createTestComments(3));
        commentCountsCache.put("overflow_post", 5L);

        // Размер должен остаться тем же (старая запись вытеснена)
        assertThat(publicCommentsCache.estimatedSize()).isEqualTo(CACHE_MAX_SIZE);
        assertThat(commentCountsCache.estimatedSize()).isEqualTo(CACHE_MAX_SIZE);

        System.out.println("Cache max size maintained: " + publicCommentsCache.estimatedSize());
    }

    @Test
    public void testCacheKeyPattern() {
        System.out.println("\n=== Testing Cache Key Pattern ===");

        String hashId = "testHash123";

        List<String> expectedKeys = List.of(
                hashId + "-page-0-size-3",
                hashId + "-page-1-size-3",
                hashId + "-page-0-size-5",
                hashId + "-page-2-size-10"
        );

        System.out.println("Testing cache key generation pattern:");
        for (String key : expectedKeys) {
            List<Map<String, Object>> comments = createTestComments(3);
            publicCommentsCache.put(key, comments);
            System.out.println("  Created cache entry with key: " + key);

            assertThat(key).matches(hashId + "-page-\\d+-size-\\d+");
        }

        for (String key : expectedKeys) {
            assertThat(publicCommentsCache.getIfPresent(key)).isNotNull();
        }

        System.out.println("All cache keys match the expected pattern");

        var stats = publicCommentsCache.stats();
        System.out.println("\nCache statistics:");
        System.out.println("  Hit count: " + stats.hitCount());
        System.out.println("  Miss count: " + stats.missCount());
        System.out.println("  Hit rate: " + stats.hitRate());
    }

    // Вспомогательные методы
    private List<Map<String, Object>> createTestComments(int count) {
        List<Map<String, Object>> comments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", (long) i);
            comment.put("text", "Test comment " + i);
            comment.put("createdAt", "2024-01-01T10:00:0" + i);
            comment.put("formattedDate", "01.01.2024, 10:0" + i);

            Map<String, Object> author = new HashMap<>();
            author.put("id", (long) (100 + i));
            author.put("username", "User" + i);
            author.put("nameuse", "Test User " + i);

            comment.put("author", author);
            comment.put("isSuperUser", false);
            comment.put("replies", new ArrayList<>());

            comments.add(comment);
        }
        return comments;
    }

    private List<String> generateCacheKeys(String hashId, int pageCount) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            keys.add(hashId + "-page-" + i + "-size-3");
        }
        return keys;
    }

    private void simulateCommentAdded(String hashId) {
        publicCommentsCache.asMap().keySet().removeIf(key -> key.startsWith(hashId));
        commentCountsCache.invalidate(hashId);
    }
}