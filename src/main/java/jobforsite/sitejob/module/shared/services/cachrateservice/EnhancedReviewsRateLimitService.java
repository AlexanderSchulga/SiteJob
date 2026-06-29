package jobforsite.sitejob.module.shared.services.cachrateservice;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для ограничения частоты запросов (rate limiting) к отзывам.
 * <p>
 * Содержит отдельные лимиты для различных действий с отзывами:
 * <ul>
 *   <li>Просмотр отзывов — 200/мин</li>
 *   <li>Добавление отзывов — 20/мин</li>
 *   <li>Поиск отзывов — 50/мин</li>
 *   <li>Загрузка страниц и пагинация — 100/мин</li>
 *   <li>Просмотр отзывов конкретного пользователя — 100/мин</li>
 * </ul>
 * При превышении лимита фиксируется неудачная попытка. После 15 неудачных попыток
 * IP блокируется на 15 минут.
 * </p>
 */
@Service
public class EnhancedReviewsRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedReviewsRateLimitService.class);

    // Кеш для просмотров отзывов
    private final Cache<String, AtomicInteger> reviewViewCounts;

    // Кеш для добавления отзывов
    private final Cache<String, AtomicInteger> reviewAddCounts;

    // Кеш для поиска отзывов
    private final Cache<String, AtomicInteger> reviewSearchCounts;

    // Кеш для загрузки страниц с отзывами
    private final Cache<String, AtomicInteger> reviewPageLoadCounts;

    // Блокировка IP
    private final Cache<String, Boolean> blockedIps;

    // Неудачные попытки (приводят к блокировке)
    private final Cache<String, AtomicInteger> failedAttempts;

    public EnhancedReviewsRateLimitService() {
        // 200 просмотров отзывов в минуту на IP
        this.reviewViewCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // 20 добавлений отзывов в минуту на IP
        this.reviewAddCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // 50 поисковых запросов в минуту на IP
        this.reviewSearchCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // 100 загрузок страниц в минуту на IP
        this.reviewPageLoadCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // Блокировка IP на 15 минут
        this.blockedIps = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(5000)
                .build();

        // Неудачные попытки (срок жизни 1 час)
        this.failedAttempts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10000)
                .build();
    }

    /**
     * Проверяет лимит просмотра отзывов (200/мин).
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewView(String ip) {
        return tryConsume(ip, reviewViewCounts, 200, "view");
    }

    /**
     * Проверяет лимит добавления отзывов (20/мин).
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewAdd(String ip) {
        return tryConsume(ip, reviewAddCounts, 20, "add");
    }

    /**
     * Проверяет лимит поиска отзывов (50/мин).
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewSearch(String ip) {
        return tryConsume(ip, reviewSearchCounts, 50, "search");
    }

    /**
     * Проверяет лимит загрузки страницы с отзывами (100/мин).
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewPageLoad(String ip) {
        return tryConsume(ip, reviewPageLoadCounts, 100, "page");
    }

    /**
     * Проверяет лимит пагинации отзывов (100/мин).
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewPagination(String ip) {
        return tryConsume(ip, reviewPageLoadCounts, 100, "pagination");
    }

    /**
     * Проверяет лимит просмотра отзывов конкретного пользователя (100/мин).
     *
     * @param ip     IP-адрес
     * @param userId ID пользователя (используется в ключе кэша)
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeUserReviews(String ip, Long userId) {
        String key = "user:" + userId;
        return tryConsume(ip + ":" + key, reviewViewCounts, 100, "user");
    }

    /**
     * Базовая логика проверки лимита.
     *
     * @param ip     IP-адрес
     * @param cache  кэш счётчиков
     * @param limit  максимальное количество
     * @param action тип действия (для формирования ключа и логирования)
     * @return true, если запрос разрешён
     */
    private boolean tryConsume(String ip, Cache<String, AtomicInteger> cache, int limit, String action) {
        if (isIpBlocked(ip)) {
            log.warn("Reviews rate limit: blocked IP rejected - {}", ip);
            return false;
        }

        String key = ip + ":" + action;
        AtomicInteger counter = cache.get(key, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count > limit) {
            log.warn("Reviews rate limit exceeded for IP: {}, action: {}, limit: {}, current: {}",
                    ip, action, limit, count);
            recordFailedAttempt(ip);
            return false;
        }

        return true;
    }

    /**
     * Фиксирует неудачную попытку. При 15 неудачных попытках IP блокируется.
     *
     * @param ip IP-адрес
     */
    public void recordFailedAttempt(String ip) {
        AtomicInteger attempts = failedAttempts.get(ip, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        if (currentAttempts >= 15) {
            log.warn("Blocking IP due to {} failed attempts (reviews): {}", currentAttempts, ip);
            blockIp(ip);
            failedAttempts.invalidate(ip);
        } else {
            log.debug("Failed attempt recorded for IP: {}, total: {}", ip, currentAttempts);
        }
    }

    /**
     * Блокирует IP-адрес.
     *
     * @param ip IP-адрес
     */
    public void blockIp(String ip) {
        blockedIps.put(ip, true);
        log.info("IP blocked (reviews rate limit): {}", ip);
    }

    /**
     * Проверяет, заблокирован ли IP.
     *
     * @param ip IP-адрес
     * @return true, если IP в чёрном списке
     */
    public boolean isIpBlocked(String ip) {
        return blockedIps.getIfPresent(ip) != null;
    }

    /**
     * Очищает историю неудачных попыток для IP.
     *
     * @param ip IP-адрес
     */
    public void clearFailedAttempts(String ip) {
        failedAttempts.invalidate(ip);
        log.debug("Failed attempts cleared for IP: {}", ip);
    }

    /**
     * Возвращает количество неудачных попыток для IP.
     *
     * @param ip IP-адрес
     * @return количество попыток
     */
    public int getFailedAttempts(String ip) {
        AtomicInteger attempts = failedAttempts.getIfPresent(ip);
        return attempts != null ? attempts.get() : 0;
    }

    /**
     * Сбрасывает все лимиты для указанного IP.
     *
     * @param ip IP-адрес
     */
    public void resetLimits(String ip) {
        reviewViewCounts.invalidate(ip + ":view");
        reviewAddCounts.invalidate(ip + ":add");
        reviewSearchCounts.invalidate(ip + ":search");
        reviewPageLoadCounts.invalidate(ip + ":page");
        reviewPageLoadCounts.invalidate(ip + ":pagination");
        log.debug("Rate limits reset for IP: {}", ip);
    }

    /**
     * Возвращает статистику текущего состояния rate limiting (для мониторинга).
     *
     * @return строка со статистикой
     */
    public String getStats() {
        return String.format(
                "Review views: %d, Review adds: %d, Review searches: %d, Page loads: %d, Blocked IPs: %d, Failed attempts: %d",
                reviewViewCounts.estimatedSize(),
                reviewAddCounts.estimatedSize(),
                reviewSearchCounts.estimatedSize(),
                reviewPageLoadCounts.estimatedSize(),
                blockedIps.estimatedSize(),
                failedAttempts.estimatedSize()
        );
    }
}