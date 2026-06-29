package jobforsite.sitejob.module.shared.config.cach;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурация кэширования для модуля отзывов и рейтингов.
 * <p>
 * Выделяет кэши отзывов в отдельный менеджер для:
 * <ul>
 *     <li>Оптимизации частых запросов к отзывам продавцов</li>
 *     <li>Изоляции от основного кэша (отзывы могут иметь другие паттерны доступа)</li>
 *     <li>Возможности настройки специфических параметров для рейтинговых вычислений</li>
 * </ul>
 * </p>
 * <p>
 * Особенности кэшей отзывов:
 * <ul>
 *     <li><b>reviewsList</b> — список отзывов продавца (TTL 5 минут)</li>
 *     <li><b>reviewsStats</b> — статистика отзывов (TTL 5 минут)</li>
 *     <li><b>userReviewsRating</b> — рейтинг пользователя (TTL 5 минут)</li>
 *     <li><b>reviewsSearchResults</b> — результаты поиска по отзывам (TTL 5 минут)</li>
 * </ul>
 * </p>
 * <p>
 * TTL в 5 минут выбран как компромисс между производительностью и актуальностью,
 * так как отзывы добавляются относительно часто, но не критичны к секундной задержке.
 * </p>
 *
 * @author JobForSite Team
 * @see BaseCacheConfig
 * @see CacheConfig
 */
@Configuration
@EnableCaching
public class CacheConfigReviews extends BaseCacheConfig {

    /**
     * Создает и настраивает менеджер кэшей для отзывов и рейтингов.
     * <p>
     * Использует кастомную конфигурацию Caffeine:
     * <ul>
     *     <li>TTL: 5 минут</li>
     *     <li>Максимальный размер: 2000 записей</li>
     *     <li>Сбор статистики: включен</li>
     * </ul>
     * </p>
     *
     * @return настроенный {@link CacheManager} для модуля отзывов
     */
    @Bean
    public CacheManager reviewCacheManager() {


        // Определение кэшей для работы с отзывами
        String[] reviewCaches = {
                "reviewsList",           // Список отзывов продавца
                "reviewsStats",          // Статистика отзывов (средний рейтинг, количество)
                "userReviewsRating",     // Рейтинг конкретного пользователя
                "reviewsSearchResults"   // Результаты поиска по отзывам
        };


        // Создание менеджера кэшей с кастомной конфигурацией Caffeine
        // TTL и максимальный размер оптимизированы под типичную нагрузку раздела отзывов
        CaffeineCacheManager cacheManager = createCacheManager(
                reviewCaches,
                cm -> cm.setCaffeine(Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)   // Время жизни записи
                        .maximumSize(2000)                       // Максимум 2000 отзывов/рейтингов
                        .recordStats())                          // Сбор статистики для мониторинга
        );

        return cacheManager;
    }
}