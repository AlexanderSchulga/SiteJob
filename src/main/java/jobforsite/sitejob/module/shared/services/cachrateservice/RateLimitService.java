package jobforsite.sitejob.module.shared.services.cachrateservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для ограничения частоты запросов (Rate Limiting) и блокировки IP при подозрительной активности.
 *
 * <p>Основные функции:
 * <ul>
 *   <li>Лимитирование различных действий (логин, регистрация, создание товаров и т.д.) по IP</li>
 *   <li>Блокировка IP при превышении числа неудачных попыток</li>
 *   <li>Автоматическая очистка устаревших данных по расписанию</li>
 * </ul>
 *
 * <p>Все счетчики хранятся в памяти (ConcurrentHashMap), не использует внешние БД.
 *
 * @author Team
 * @version 1.0
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    // Хранилище количества неудачных попыток (например, неверный пароль) по IP
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    // Хранилище заблокированных IP с временем окончания блокировки
    private final Map<String, Long> blockedIps = new ConcurrentHashMap<>();

    // Основное хранилище счетчиков запросов для rate limiting: ключ = "IP:действие:временное_окно"
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    private static final int MAX_FAILED_ATTEMPTS = 15; // Максимум неудачных попыток до блокировки
    private static final long BLOCK_DURATION_MS = 20 * 60 * 1000; // 20 минут блокировки

    // ======================== МЕТОДЫ ДЛЯ АВТОРИЗАЦИИ ========================

    /**
     * Лимит для обновления капчи.
     */
    public boolean tryConsumeCaptchaRefresh(String ip) {
        return tryConsume(ip, "captcha_refresh", 10, 60000); // 10 запросов в минуту
    }

    /**
     * Лимит для GET-запросов страницы логина.
     * @param ip IP-адрес клиента
     * @return true если запрос разрешен, false если лимит превышен
     */
    public boolean tryConsumeLoginGet(String ip) {
        return tryConsume(ip, "login_get", 30, 60000);
    }

    /**
     * Лимит для POST-запросов логина (отправка пароля).
     */
    public boolean tryConsumeLoginPost(String ip) {
        return tryConsume(ip, "login_post", 10, 60000);
    }

    /**
     * Лимит для GET-запросов страницы регистрации.
     */
    public boolean tryConsumeRegisterGet(String ip) {
        return tryConsume(ip, "register_get", 10, 60000);
    }

    /**
     * Лимит для POST-запросов регистрации (создание аккаунта).
     */
    public boolean tryConsumeRegisterPost(String ip) {
        return tryConsume(ip, "register_post", 3, 3600000);
    }

    /**
     * Лимит для GET-запросов страницы восстановления пароля.
     */
    public boolean tryConsumeForgotPasswordGet(String ip) {
        return tryConsume(ip, "forgot_get", 20, 60000);
    }

    /**
     * Лимит для POST-запросов восстановления пароля.
     */
    public boolean tryConsumeForgotPasswordPost(String ip) {
        return tryConsume(ip, "forgot_post", 3, 3600000);
    }

    /**
     * Лимит для сброса пароля.
     */
    public boolean tryConsumeResetPassword(String ip) {
        return tryConsume(ip, "reset", 5, 60000);
    }

    /**
     * Лимит для подтверждения регистрации.
     */
    public boolean tryConsumeVerifyRegistration(String ip) {
        return tryConsume(ip, "verify", 10, 60000);
    }

    /**
     * Лимит для GET-запросов админ-панели.
     */
    public boolean tryConsumeAdminGet(String ip) {
        return tryConsume(ip, "admin_get", 60, 60000);
    }

    /**
     * Лимит для POST-запросов админ-панели.
     */
    public boolean tryConsumeAdminPost(String ip) {
        return tryConsume(ip, "admin_post", 30, 60000);
    }

    /**
     * Лимит для отладочных эндпоинтов.
     */
    public boolean tryConsumeDebug(String ip) {
        return tryConsume(ip, "debug", 5, 60000);
    }

    // ======================== ТОВАРЫ, УСЛУГИ, ВАКАНСИИ, РЕЗЮМЕ ========================

    /**
     * Лимит загрузки списка товаров.
     */
    public boolean tryConsumeProductLoad(String ip) {
        return tryConsume(ip, "product_load", 120, 60000);
    }

    /**
     * Лимит загрузки списка услуг.
     */
    public boolean tryConsumeServiceLoad(String ip) {
        return tryConsume(ip, "service_load", 120, 60000);
    }

    /**
     * Лимит загрузки списка вакансий.
     */
    public boolean tryConsumeVacancyLoad(String ip) {
        return tryConsume(ip, "vacancy_load", 120, 60000);
    }

    /**
     * Лимит загрузки списка резюме.
     */
    public boolean tryConsumeResumeLoad(String ip) {
        return tryConsume(ip, "resume_load", 120, 60000);
    }

    /**
     * Лимит просмотра карточки товара.
     */
    public boolean tryConsumeProductView(String ip) {
        return tryConsume(ip, "product_view", 200, 60000);
    }

    /**
     * Лимит просмотра карточки услуги.
     */
    public boolean tryConsumeServiceView(String ip) {
        return tryConsume(ip, "service_view", 200, 60000);
    }

    /**
     * Лимит просмотра карточки вакансии.
     */
    public boolean tryConsumeVacancyView(String ip) {
        return tryConsume(ip, "vacancy_view", 200, 60000);
    }

    /**
     * Лимит просмотра карточки резюме.
     */
    public boolean tryConsumeResumeView(String ip) {
        return tryConsume(ip, "resume_view", 200, 60000);
    }

    /**
     * Лимит создания товара.
     */
    public boolean tryConsumeProductCreate(String ip) {
        return tryConsume(ip, "product_create", 15, 3600000);
    }

    /**
     * Лимит создания услуги.
     */
    public boolean tryConsumeServiceCreate(String ip) {
        return tryConsume(ip, "service_create", 15, 3600000);
    }

    /**
     * Лимит создания вакансии.
     */
    public boolean tryConsumeVacancyCreate(String ip) {
        return tryConsume(ip, "vacancy_create", 15, 3600000);
    }

    /**
     * Лимит создания резюме.
     */
    public boolean tryConsumeResumeCreate(String ip) {
        return tryConsume(ip, "resume_create", 15, 3600000);
    }

    /**
     * Лимит добавления комментария.
     */
    public boolean tryConsumeCommentAdd(String ip) {
        return tryConsume(ip, "comment_add", 30, 60000);
    }

    /**
     * Лимит добавления личного комментария.
     */
    public boolean tryConsumePersonalCommentAdd(String ip) {
        return tryConsume(ip, "personal_comment_add", 50, 60000);
    }

    /**
     * Лимит загрузки комментариев.
     */
    public boolean tryConsumeCommentLoad(String ip) {
        return tryConsume(ip, "comment_load", 120, 60000);
    }

    // ======================== ПОИСК И ПРОФИЛИ ========================

    /**
     * Лимит поисковых запросов.
     */
    public boolean tryConsumeSearch(String ip) {
        return tryConsume(ip, "search", 60, 60000);
    }

    /**
     * Лимит просмотра пользовательских профилей.
     */
    public boolean tryConsumeUserProfile(String ip) {
        return tryConsume(ip, "user_profile", 100, 60000);
    }

    // ======================== ОСНОВНАЯ ЛОГИКА RATE LIMITING ========================

    /**
     * Основной метод проверки лимитов для заданного действия.
     *
     * @param ip IP-адрес
     * @param action тип действия (используется в ключе)
     * @param limit максимальное количество запросов за timeWindowMs
     * @param timeWindowMs временное окно в миллисекундах
     * @return true если запрос в рамках лимита
     */
    private boolean tryConsume(String ip, String action, int limit, long timeWindowMs) {
        String key = ip + ":" + action;
        long now = System.currentTimeMillis();
        long windowKey = now / timeWindowMs; // Номер текущего временного окна
        String timeKey = key + ":" + windowKey;

        AtomicInteger counter = requestCounts.computeIfAbsent(timeKey, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();

        // Периодическая очистка старых ключей (если накопилось много)
        cleanupOldKeys(now, timeWindowMs);

        boolean allowed = count <= limit;
        if (!allowed) {
            log.warn("Rate limit exceeded: IP={}, action={}, limit={}/{}ms, actual={}",
                    ip, action, limit, timeWindowMs, count);
        }
        return allowed;
    }

    /**
     * Очищает старые записи в requestCounts, если их стало слишком много.
     * Не вызывается каждый раз при полной очистке, только при превышении порога.
     */
    private void cleanupOldKeys(long now, long timeWindowMs) {
        if (requestCounts.size() > 5000) { // Порог срабатывания очистки
            int removed = 0;
            for (Map.Entry<String, AtomicInteger> entry : requestCounts.entrySet()) {
                String[] parts = entry.getKey().split(":");
                if (parts.length >= 3) {
                    try {
                        long keyTime = Long.parseLong(parts[2]) * timeWindowMs;
                        if (now - keyTime > timeWindowMs * 2) {
                            requestCounts.remove(entry.getKey());
                            removed++;
                        }
                    } catch (NumberFormatException ex) {
                        requestCounts.remove(entry.getKey());
                        removed++;
                    }
                } else {
                    requestCounts.remove(entry.getKey());
                    removed++;
                }
            }
            if (removed > 0) {
                log.debug("Cleanup old keys removed {} entries", removed);
            }
        }
    }

    // ======================== УПРАВЛЕНИЕ НЕУДАЧНЫМИ ПОПЫТКАМИ И БЛОКИРОВКОЙ ========================

    /**
     * Регистрирует неудачную попытку (например, неверный пароль).
     * При достижении MAX_FAILED_ATTEMPTS IP блокируется.
     *
     * @param ip IP-адрес нарушителя
     */
    public void recordFailedAttempt(String ip) {
        AtomicInteger attempts = failedAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        log.info("Failed attempt recorded for IP: {}, total fails: {}/{}", ip, currentAttempts, MAX_FAILED_ATTEMPTS);

        if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
            blockedIps.put(ip, System.currentTimeMillis() + BLOCK_DURATION_MS);
            failedAttempts.remove(ip);
            log.warn("IP {} has been blocked for {} minutes due to {} failed attempts",
                    ip, BLOCK_DURATION_MS / 60000, MAX_FAILED_ATTEMPTS);
        }
    }

    /**
     * Проверяет, заблокирован ли IP в данный момент.
     * Автоматически снимает блокировку при истечении времени.
     *
     * @param ip IP-адрес
     * @return true если IP заблокирован
     */
    public boolean isIpBlocked(String ip) {
        Long blockUntil = blockedIps.get(ip);
        if (blockUntil != null) {
            if (System.currentTimeMillis() < blockUntil) {
                return true;
            } else {
                blockedIps.remove(ip);
                log.debug("Block expired for IP: {}", ip);
            }
        }
        return false;
    }

    /**
     * Сбрасывает счетчик неудачных попыток для IP (обычно после успешного входа).
     */
    public void clearFailedAttempts(String ip) {
        Integer before = failedAttempts.get(ip) != null ? failedAttempts.get(ip).get() : 0;
        failedAttempts.remove(ip);
        log.debug("Cleared failed attempts for IP: {} (was {})", ip, before);
    }

    /**
     * Возвращает текущее количество неудачных попыток для IP.
     */
    public int getFailedAttempts(String ip) {
        return failedAttempts.getOrDefault(ip, new AtomicInteger(0)).get();
    }

    /**
     * Возвращает статистику по IP (заблокирован ли, сколько неудачных попыток).
     *
     * @param ip IP-адрес
     * @return Map со статистикой
     */
    public Map<String, Object> getIpStats(String ip) {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("blocked", isIpBlocked(ip));
        stats.put("failedAttempts", getFailedAttempts(ip));
        return stats;
    }

    // ======================== ПЛАНОВАЯ ОЧИСТКА ========================

    /**
     * Плановое удаление устаревших данных.
     * Запускается каждый час.
     *
     * <p>Удаляет:
     * <ul>
     *   <li>Истекшие блокировки IP</li>
     *   <li>Старые счетчики запросов (старше 5 минут)</li>
     * </ul>
     */
    @Scheduled(fixedDelay = 3600000) // Каждый час
    public void cleanupOldData() {
        long now = System.currentTimeMillis();
        int removedBlocks = 0;
        int removedCounts = 0;

        // Очистка старых блокировок
        for (Map.Entry<String, Long> entry : blockedIps.entrySet()) {
            if (now > entry.getValue()) {
                blockedIps.remove(entry.getKey());
                removedBlocks++;
            }
        }

        // Очистка старых счетчиков запросов (окна старше 5 минут)
        for (Map.Entry<String, AtomicInteger> entry : requestCounts.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length >= 3) {
                try {
                    long keyTime = Long.parseLong(parts[2]) * 60000; // предположительно окно в минутах
                    if (now - keyTime > 60000 * 5) {
                        requestCounts.remove(entry.getKey());
                        removedCounts++;
                    }
                } catch (NumberFormatException ex) {
                    requestCounts.remove(entry.getKey());
                    removedCounts++;
                }
            } else {
                requestCounts.remove(entry.getKey());
                removedCounts++;
            }
        }

        if (removedBlocks > 0 || removedCounts > 0) {
            log.info("Scheduled cleanup: removed {} expired blocks and {} old rate counters",
                    removedBlocks, removedCounts);
        }
    }
}