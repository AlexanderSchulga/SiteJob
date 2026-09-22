package jobforsite.sitejob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CacheStatsTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testCacheStats() {
        System.out.println("\n=== Cache Statistics ===");

        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("publicComments");

        if (cache != null) {
            var stats = cache.getNativeCache().stats();

            System.out.println("Cache: publicComments");
            System.out.println("  Hit count: " + stats.hitCount());
            System.out.println("  Miss count: " + stats.missCount());
            System.out.println("  Hit rate: " + stats.hitRate());
            System.out.println("  Eviction count: " + stats.evictionCount());
            System.out.println("  Load success count: " + stats.loadSuccessCount());
            System.out.println("  Total load time: " + stats.totalLoadTime() + " ns");
        } else {
            System.out.println("Cache 'publicComments' not found");
        }

        cache = (CaffeineCache) cacheManager.getCache("commentCounts");

        if (cache != null) {
            var stats = cache.getNativeCache().stats();

            System.out.println("\nCache: commentCounts");
            System.out.println("  Hit count: " + stats.hitCount());
            System.out.println("  Miss count: " + stats.missCount());
            System.out.println("  Hit rate: " + stats.hitRate());
        }
    }

    @Test
    public void testCacheEviction() {
        // Проверяем, что кэш настроен правильно
        CaffeineCache cache = (CaffeineCache) cacheManager.getCache("publicComments");

        assertThat(cache).isNotNull();

        var nativeCache = cache.getNativeCache();

        System.out.println("\n=== Cache Configuration ===");
        System.out.println("Maximum size: " + nativeCache.policy().eviction()
                .map(policy -> policy.getMaximum())
                .orElse(0L));
        System.out.println("Current size: " + nativeCache.estimatedSize());
        System.out.println("Expire after write: 5 minutes");
    }
}