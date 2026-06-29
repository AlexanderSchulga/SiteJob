package jobforsite.sitejob.module.shared.config.cach;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * Конфигурация кэширования приложения на основе Caffeine.
 * <p>
 * Определяет и регистрирует все кэши, используемые в системе, с различными стратегиями
 * времени жизни (TTL) и максимальными размерами:
 * <ul>
 *     <li><b>Стандартные кэши</b> — TTL 5 минут, макс. 10 000 записей</li>
 *     <li><b>Кэши списков и страниц</b> — TTL 30 секунд (актуальные данные)</li>
 *     <li><b>Кэши счетчиков</b> — TTL 2 минуты (компромисс между свежестью и нагрузкой)</li>
 *     <li><b>Кэши пользовательских данных</b> — TTL 30-60 минут</li>
 *     <li><b>Статические страницы</b> — TTL 24 часа (редко меняются)</li>
 *     <li><b>Кэши рейтингов и городов</b> — TTL 5-30 минут</li>
 *     <li><b>Кэши блокировок</b> — короткие TTL 2-3 минуты</li>
 * </ul>
 * </p>
 * <p>
 * Все кэши собирают статистику для мониторинга эффективности.
 * </p>
 *
 * @author JobForSite Team
 * @see BaseCacheConfig
 * @see CaffeineCacheManager
 */
@Configuration
@EnableCaching
public class CacheConfig extends BaseCacheConfig {

    /**
     * Создает и настраивает основной менеджер кэшей.
     * <p>
     * Регистрирует все кэши приложения с их индивидуальными настройками TTL и размера.
     * </p>
     *
     * @return настроенный {@link CacheManager} с полной конфигурацией кэшей
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {

        // Базовые кэши системных статусов и сущностей (TTL: 5 мин, размер: 10 000)
        String[] defaultCaches = {
                "postStatus", "serviceStatus", "vacancyStatus", "resumeStatus", "supportTicketStatus",
                "publicComments", "privateComments", "commentCounts", "register",
                "login", "unreadCounts", "postIdsWithUnread", "userPostsData"
        };

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(defaultCaches);

        // Базовая конфигурация Caffeine для кэшей по умолчанию
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(DEFAULT_MAX_SIZE)
                .recordStats());


        // ========== Кэши списков (30 сек) ==========
        // Используются для быстрого отображения списков сущностей на главных страницах
        registerCustomCacheSeconds(cacheManager, "productsList", 30, 1000);
        registerCustomCacheSeconds(cacheManager, "servicesList", 30, 1000);
        registerCustomCacheSeconds(cacheManager, "vacanciesList", 30, 1000);
        registerCustomCacheSeconds(cacheManager, "resumeList", 30, 1000);

        // ========== Кэши страниц с фильтрами (30 сек) ==========
        // Для пагинированных страниц с учетом фильтрации и сортировки
        registerCustomCacheSeconds(cacheManager, "productsPage", 30, 2000);
        registerCustomCacheSeconds(cacheManager, "servicesPage", 30, 2000);
        registerCustomCacheSeconds(cacheManager, "vacanciesPage", 30, 2000);
        registerCustomCacheSeconds(cacheManager, "resumePage", 30, 2000);

        // ========== Кэши счетчиков (2 минуты) ==========
        // Кэши для общего количества записей (редко меняются, но критичны для интерфейса)
        registerShortLivedCache(cacheManager, "productsCount", 120, 500);
        registerShortLivedCache(cacheManager, "servicesCount", 120, 500);
        registerShortLivedCache(cacheManager, "vacanciesCount", 120, 500);
        registerShortLivedCache(cacheManager, "resumeCount", 120, 500);

        // ========== Кэши пользовательских данных (30-60 минут) ==========
        // Персональные данные пользователей: объявления, резюме и т.д.
        registerCustomCache(cacheManager, "userByHash", 60, 5000);
        registerCustomCache(cacheManager, "userProducts", 30, 10000);
        registerCustomCache(cacheManager, "userServices", 30, 10000);
        registerCustomCache(cacheManager, "userVacancies", 30, 10000);
        registerCustomCache(cacheManager, "userResume", 30, 10000);
        registerCustomCache(cacheManager, "userCounts", 5, 2000);  // Счетчики пользователя обновляются чаще

        // ========== Статические страницы (24 часа) ==========
        // Кэшируются редко меняющиеся информационные страницы
        registerLongLivedCache(cacheManager, "index", 24, 100);
        registerLongLivedCache(cacheManager, "about", 24, 100);

        // ========== Кэши рейтингов продавцов (5-10 минут) ==========
        // Рейтинги и отзывы продавцов
        registerCustomCache(cacheManager, "sellerRatings", 5, 10000);
        registerCustomCache(cacheManager, "sellerRatingsCount", 5, 10000);
        registerCustomCache(cacheManager, "sellerRatingsByHash", 5, 10000);
        registerCustomCache(cacheManager, "sellerRatingsCountByHash", 5, 10000);

        // ========== Кэши геоданных (30 минут) ==========
        // Отображаемые названия городов и регионов
        registerCustomCache(cacheManager, "cityDisplayNames", 30, 10000);
        registerCustomCache(cacheManager, "regionDisplayNames", 30, 10000);

        // ========== Кэши заблокированных постов ==========
        // Слайсы (выборки) заблокированного контента — TTL 3 минуты
        registerShortLivedCache(cacheManager, "blockedProductsSlice", 180, 500);
        registerShortLivedCache(cacheManager, "blockedServicesSlice", 180, 500);
        registerShortLivedCache(cacheManager, "blockedVacanciesSlice", 180, 500);
        registerShortLivedCache(cacheManager, "blockedResumeSlice", 180, 500);

        // Счетчики заблокированных постов — TTL 2 минуты
        registerShortLivedCache(cacheManager, "blockedProductsCount", 120, 10);
        registerShortLivedCache(cacheManager, "blockedServicesCount", 120, 10);
        registerShortLivedCache(cacheManager, "blockedVacanciesCount", 120, 10);
        registerShortLivedCache(cacheManager, "blockedResumeCount", 120, 10);

        // ========== Кэши информации о блокировках пользователей ==========
        // Причина и детали блокировки пользователя
        registerCustomCache(cacheManager, "userBlockReason", 30, 2000);
        registerCustomCache(cacheManager, "userBlockInfo", 20, 2000);

        return cacheManager;
    }

    /**
     * Подсчитывает количество зарегистрированных кэшей в менеджере.
     *
     * @param cacheManager менеджер кэшей
     * @return количество кэшей
     */
    private int countRegisteredCaches(CaffeineCacheManager cacheManager) {
        // Приблизительный подсчет, так как cacheManager.getCacheNames() возвращает только активные
        return cacheManager.getCacheNames() != null ? cacheManager.getCacheNames().size() : 0;
    }

}