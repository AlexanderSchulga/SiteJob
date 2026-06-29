package jobforsite.sitejob.module.shared.config.cach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация кэширования для модуля модерации.
 * <p>
 * Выделяет кэши модерации в отдельный менеджер для:
 * <ul>
 *     <li>Изоляции от основного кэша (разные стратегии инвалидации)</li>
 *     <li>Более коротких TTL, так как статус модерации часто меняется</li>
 *     <li>Возможности независимой очистки кэшей модерации</li>
 * </ul>
 * </p>
 * <p>
 * Особенности кэшей модерации:
 * <ul>
 *     <li><b>DTO с изображениями</b> — TTL 5 минут (300 сек), так как проверка изображений требует времени</li>
 *     <li><b>Счетчики</b> — TTL 15 секунд, чтобы модератор видел актуальное количество ожидающих</li>
 * </ul>
 * </p>
 *
 * @author JobForSite Team
 * @see BaseCacheConfig
 * @see CacheConfig
 */
@Configuration
@EnableCaching
public class CacheConfigModeration extends BaseCacheConfig {


    /**
     * Создает и настраивает менеджер кэшей для модерации.
     * <p>
     * Регистрирует кэши для DTO объявлений на модерации и их счетчиков
     * с оптимизированными TTL для рабочего процесса модератора.
     * </p>
     *
     * @return настроенный {@link CacheManager} для модуля модерации
     */
    @Bean
    public CacheManager moderationCacheManager() {

        // Базовые кэши для DTO объявлений на модерации (TTL: 5 мин)
        String[] moderationCaches = {
                "moderationProductsDto", "moderationServicesDto",
                "moderationVacanciesDto", "moderationResumeDto"
        };


        CaffeineCacheManager cacheManager = createCacheManager(moderationCaches, null);

        // ========== DTO с изображениями (5 минут) ==========
        // Увеличенный TTL, так как модератору нужно время для проверки изображений
        // Повторный запрос того же объявления в течение 5 минут будет из кэша
        registerShortLivedCache(cacheManager, "moderationProductsDto", 300, 200);
        registerShortLivedCache(cacheManager, "moderationServicesDto", 300, 200);
        registerShortLivedCache(cacheManager, "moderationVacanciesDto", 300, 200);
        registerShortLivedCache(cacheManager, "moderationResumeDto", 300, 200);

        // ========== Счетчики (15 секунд) ==========
        // Очень короткий TTL, чтобы модератор видел актуальное количество
        // ожидающих модерации объявлений при обновлении страницы
        registerShortLivedCache(cacheManager, "moderationProductsCount", 15, 10);
        registerShortLivedCache(cacheManager, "moderationServicesCount", 15, 10);
        registerShortLivedCache(cacheManager, "moderationVacanciesCount", 15, 10);
        registerShortLivedCache(cacheManager, "moderationResumeCount", 15, 10);

        return cacheManager;
    }
}