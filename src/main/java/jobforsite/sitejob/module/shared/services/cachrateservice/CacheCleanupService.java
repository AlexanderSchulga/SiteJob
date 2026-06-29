package jobforsite.sitejob.module.shared.services.cachrateservice;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Сервис для ручной очистки кэшей приложения.
 * <p>
 * Использует {@link CacheManager} для получения доступа ко всем кэшам
 * и их последующей инвалидации.
 * </p>
 */
@RequiredArgsConstructor
@Service
public class CacheCleanupService {

    private static final Logger log = LoggerFactory.getLogger(CacheCleanupService.class);

    private final CacheManager cacheManager;

    /**
     * Полностью очищает ВСЕ кэши, зарегистрированные в {@link CacheManager}.
     * <p>
     * Пробегает по всем именам кэшей и вызывает {@code clear()} для каждого.
     * Внимание: может временно снизить производительность, так как все кэши
     * будут перестраиваться заново при следующих запросах.
     * </p>
     */
    public void clearAllCaches() {
        log.info("Clearing ALL caches");
        cacheManager.getCacheNames().stream()
                .forEach(cacheName -> {
                    log.debug("Clearing cache: {}", cacheName);
                    cacheManager.getCache(cacheName).clear();
                });
        log.info("All caches cleared");
    }

    /**
     * Очищает только кэш публичных комментариев (с именем "publicComments").
     */
    public void clearPublicCommentsCache() {
        log.info("Clearing publicComments cache");
        cacheManager.getCache("publicComments").clear();
        log.debug("publicComments cache cleared");
    }

    /**
     * Очищает только кэш счётчиков комментариев (с именем "commentCounts").
     */
    public void clearCommentCountsCache() {
        log.info("Clearing commentCounts cache");
        cacheManager.getCache("commentCounts").clear();
        log.debug("commentCounts cache cleared");
    }
}