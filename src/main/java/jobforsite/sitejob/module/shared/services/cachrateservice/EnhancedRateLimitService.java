package jobforsite.sitejob.module.shared.services.cachrateservice;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для расширенного ограничения частоты запросов (rate limiting) с автоматической блокировкой IP.
 * <p>
 * Использует Caffeine кэши для хранения счётчиков запросов по IP-адресам:
 * <ul>
 *   <li>Лимит на комментарии (30 в минуту)</li>
 *   <li>Лимит на личные комментарии (50 в минуту)</li>
 *   <li>Лимиты на действия с отзывами: просмотр (200/мин), добавление (20/мин), поиск (50/мин)</li>
 *   <li>Блокировка IP при 10 неудачных попытках (блок на 15 минут)</li>
 * </ul>
 * </p>
 */
@Service
public class EnhancedRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedRateLimitService.class);

    // Кэш для rate limiting комментариев
    private final Cache<String, AtomicInteger> commentRequestCounts;
    private final Cache<String, AtomicInteger> commentPersonalRequestCounts;

    // Кэш для блокировки IP
    private final Cache<String, Boolean> blockedIps;

    // Кэш для неудачных попыток (приводят к блокировке)
    private final Cache<String, AtomicInteger> failedAttempts;

    // Кэши для rate limiting отзывов
    private final Cache<String, AtomicInteger> reviewViewCounts;
    private final Cache<String, AtomicInteger> reviewAddCounts;
    private final Cache<String, AtomicInteger> reviewSearchCounts;

    public EnhancedRateLimitService() {
        // 30 комментариев в минуту на IP
        this.commentRequestCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        // 50 личных комментариев в минуту на IP
        this.commentPersonalRequestCounts = Caffeine.newBuilder()
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

        // Лимиты для отзывов
        this.reviewViewCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.reviewAddCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.reviewSearchCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    /**
     * Проверяет, может ли IP отправить ещё один обычный комментарий.
     * Лимит: 30 в минуту.
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeComment(String ip) {
        return tryConsume(ip, commentRequestCounts, 30);
    }

    /**
     * Проверяет, может ли IP отправить ещё один личный комментарий.
     * Лимит: 50 в минуту.
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumePersonalComment(String ip) {
        return tryConsume(ip, commentPersonalRequestCounts, 50);
    }

    /**
     * Проверяет, может ли IP выполнить ещё один просмотр отзывов.
     * Лимит: 200 в минуту.
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewView(String ip) {
        return tryConsume(ip, reviewViewCounts, 200);
    }

    /**
     * Проверяет, может ли IP добавить ещё один отзыв.
     * Лимит: 20 в минуту.
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewAdd(String ip) {
        return tryConsume(ip, reviewAddCounts, 20);
    }

    /**
     * Проверяет, может ли IP выполнить ещё один поиск отзывов.
     * Лимит: 50 в минуту.
     *
     * @param ip IP-адрес
     * @return true, если лимит не превышен и IP не заблокирован
     */
    public boolean tryConsumeReviewSearch(String ip) {
        return tryConsume(ip, reviewSearchCounts, 50);
    }

    /**
     * Базовая логика проверки лимита.
     *
     * @param ip    IP-адрес
     * @param cache кэш счётчиков для данного типа запросов
     * @param limit максимальное количество за период
     * @return true, если запрос разрешён
     */
    private boolean tryConsume(String ip, Cache<String, AtomicInteger> cache, int limit) {
        if (isIpBlocked(ip)) {
            log.warn("Rate limit: blocked IP rejected - {}", ip);
            return false;
        }

        AtomicInteger counter = cache.get(ip, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        if (count > limit) {
            log.warn("Rate limit exceeded for IP: {}, limit: {}, current: {}", ip, limit, count);
            recordFailedAttempt(ip);
            return false;
        }

        return true;
    }

    /**
     * Фиксирует неудачную попытку для IP.
     * При достижении 10 неудачных попыток IP блокируется на 15 минут.
     *
     * @param ip IP-адрес
     */
    public void recordFailedAttempt(String ip) {
        AtomicInteger attempts = failedAttempts.get(ip, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        if (currentAttempts >= 10) {
            log.warn("Blocking IP due to {} failed attempts: {}", currentAttempts, ip);
            blockIp(ip);
            failedAttempts.invalidate(ip);
        } else {
            log.debug("Failed attempt recorded for IP: {}, total attempts: {}", ip, currentAttempts);
        }
    }

    /**
     * Блокирует указанный IP-адрес.
     *
     * @param ip IP-адрес
     */
    public void blockIp(String ip) {
        blockedIps.put(ip, true);
        log.info("IP blocked: {}", ip);
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
     * Возвращает статистику текущего состояния rate limiting (для мониторинга).
     *
     * @return строка со статистикой
     */
    public String getStats() {
        return String.format(
                "Comment requests: %d, Personal requests: %d, Blocked IPs: %d, Failed attempts: %d",
                commentRequestCounts.estimatedSize(),
                commentPersonalRequestCounts.estimatedSize(),
                blockedIps.estimatedSize(),
                failedAttempts.estimatedSize()
        );
    }
}