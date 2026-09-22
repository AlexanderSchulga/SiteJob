package jobforsite.sitejob;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SupportRateLimitCacheTest {

    private Cache<String, RateLimitInfo> testCache;

    private static final int RATE_LIMIT = 10;
    private static final int RATE_LIMIT_WINDOW = 60;

    @BeforeEach
    public void setUp() {
        // Создаем тестовый кэш с такими же параметрами как в контроллере
        testCache = Caffeine.newBuilder()
                .expireAfterWrite(RATE_LIMIT_WINDOW, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
    }

    @Test
    public void testCacheExpiration() throws InterruptedException {
        System.out.println("\n=== Testing Cache Expiration ===");

        String testKey = "rate_limit:user1";
        long now = System.currentTimeMillis() / 1000;

        // Добавляем запись в кэш с 5 запросами
        testCache.put(testKey, new RateLimitInfo(5, now));
        assertThat(testCache.getIfPresent(testKey)).isNotNull();

        System.out.println("Cache entry created with count=5");
        System.out.println("Waiting 61 seconds for expiration...");

        Thread.sleep(61000); // Ждем 61 секунду

        // Проверяем, что запись удалилась
        assertThat(testCache.getIfPresent(testKey)).isNull();
        System.out.println("Cache entry expired successfully after window period");
    }

    @Test
    public void testRateLimitIncrement() {
        System.out.println("\n=== Testing Rate Limit Increment ===");

        String testKey = "rate_limit:user2";
        long now = System.currentTimeMillis() / 1000;

        AtomicInteger allowedCount = new AtomicInteger(0);
        AtomicInteger limitedCount = new AtomicInteger(0);

        // Имитируем 15 последовательных запросов
        for (int i = 1; i <= 15; i++) {
            RateLimitInfo info = testCache.getIfPresent(testKey);

            if (info == null) {
                // Первый запрос или после истечения окна
                testCache.put(testKey, new RateLimitInfo(1, now));
                allowedCount.incrementAndGet();
                System.out.printf("Request %d: ALLOWED (first in window)%n", i);
            } else if ((now - info.windowStart) < RATE_LIMIT_WINDOW) {
                // В пределах текущего окна
                if (info.count >= RATE_LIMIT) {
                    limitedCount.incrementAndGet();
                    System.out.printf("Request %d: RATE LIMITED (count=%d)%n", i, info.count);
                } else {
                    info.count++;
                    testCache.put(testKey, info);
                    allowedCount.incrementAndGet();
                    System.out.printf("Request %d: ALLOWED (count=%d)%n", i, info.count);
                }
            } else {
                // Новое окно
                testCache.put(testKey, new RateLimitInfo(1, now));
                allowedCount.incrementAndGet();
                System.out.printf("Request %d: ALLOWED (new window)%n", i);
            }
        }

        RateLimitInfo finalInfo = testCache.getIfPresent(testKey);

        System.out.println("\n=== Results ===");
        System.out.println("Total allowed: " + allowedCount.get());
        System.out.println("Total limited: " + limitedCount.get());
        System.out.println("Final count in cache: " + (finalInfo != null ? finalInfo.count : 0));

        // Проверяем, что счетчик не превышает лимит
        assertThat(finalInfo.count).isLessThanOrEqualTo(RATE_LIMIT);
        assertThat(limitedCount.get()).isGreaterThan(0);
    }

    @Test
    public void testCacheWithDifferentUsers() {
        System.out.println("\n=== Testing Cache with Different Users ===");

        String user1Key = "rate_limit:user1";
        String user2Key = "rate_limit:user2";
        long now = System.currentTimeMillis() / 1000;

        // User1 делает 8 запросов
        testCache.put(user1Key, new RateLimitInfo(8, now));

        // User2 делает 12 запросов (превышает лимит)
        testCache.put(user2Key, new RateLimitInfo(12, now));

        RateLimitInfo user1Info = testCache.getIfPresent(user1Key);
        RateLimitInfo user2Info = testCache.getIfPresent(user2Key);

        System.out.println("User1 request count: " + user1Info.count);
        System.out.println("User2 request count: " + user2Info.count);

        // Проверяем, что счетчики для разных пользователей независимы
        assertThat(user1Info.count).isEqualTo(8);
        assertThat(user2Info.count).isEqualTo(12);

        // Проверяем, что пользователи не влияют друг на друга
        assertThat(user1Info.count).isNotEqualTo(user2Info.count);
    }

    @Test
    public void testCacheMaxSize() {
        System.out.println("\n=== Testing Cache Max Size ===");

        long startTime = System.currentTimeMillis();

        // Заполняем кэш до максимума (10000)
        for (int i = 0; i < 10000; i++) {
            testCache.put("user" + i, new RateLimitInfo(1, System.currentTimeMillis() / 1000));
        }

        System.out.println("Filled cache with 10000 entries in " +
                (System.currentTimeMillis() - startTime) + " ms");

        // Проверяем, что размер соответствует
        assertThat(testCache.estimatedSize()).isEqualTo(10000);

        // Добавляем еще одну запись (должна вытеснить самую старую)
        testCache.put("overflow_user", new RateLimitInfo(1, System.currentTimeMillis() / 1000));

        // Размер должен остаться ~10000 (старая запись вытеснена)
        assertThat(testCache.estimatedSize()).isEqualTo(10000);
        System.out.println("Cache max size maintained: " + testCache.estimatedSize());
    }

    @Test
    public void testRateLimitWindowReset() throws InterruptedException {
        System.out.println("\n=== Testing Rate Limit Window Reset ===");

        String testKey = "rate_limit:user3";
        long now = System.currentTimeMillis() / 1000;

        // Добавляем запись с 10 запросами (достигнут лимит)
        testCache.put(testKey, new RateLimitInfo(10, now));
        System.out.println("Added user with 10 requests (limit reached)");

        // Проверяем, что следующие запросы блокируются
        RateLimitInfo info = testCache.getIfPresent(testKey);
        boolean isBlocked = info != null &&
                (System.currentTimeMillis() / 1000 - info.windowStart) < RATE_LIMIT_WINDOW &&
                info.count >= RATE_LIMIT;

        assertThat(isBlocked).isTrue();
        System.out.println("Request blocked as expected");

        // Ждем 61 секунду для сброса окна
        System.out.println("Waiting 61 seconds for window reset...");
        Thread.sleep(61000);

        // Проверяем, что запись удалилась (окно сброшено)
        assertThat(testCache.getIfPresent(testKey)).isNull();
        System.out.println("Window reset successfully - cache entry expired");
    }

    // Внутренний класс для тестирования
    private static class RateLimitInfo {
        int count;
        long windowStart;

        RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}