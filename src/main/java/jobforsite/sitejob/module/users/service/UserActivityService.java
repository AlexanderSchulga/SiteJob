package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для отслеживания активности пользователей.
 * <p>
 * Основная задача — минимизировать нагрузку на базу данных при частых обновлениях
 * времени последней активности пользователя. Реализован паттерн "ленивой" записи
 * с использованием кэша в памяти.
 * </p>
 *
 * <p><b>Алгоритм работы:</b></p>
 * <ul>
 *   <li>При каждом запросе пользователя вызывается {@link #recordActivity(Long)}</li>
 *   <li>Метод обновляет только кэш, если с последней записи в БД прошло меньше 60 секунд</li>
 *   <li>При превышении интервала выполняется прямой UPDATE в БД без загрузки сущности</li>
 *   <li>Кэш периодически очищается от устаревших записей</li>
 * </ul>
 *
 * <p><b>Производительность:</b> метод {@link #recordActivity(Long)} выполняет O(1) операций
 * и не блокирует потоки на операциях ввода-вывода в 99% случаев.</p>
 *
 * @author YourTeam
 * @version 1.0
 * @see UserRepository#updateLastSeenAndOnline(Long, LocalDateTime)
 */
@RequiredArgsConstructor
@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    private final UserRepository userRepository;

    /**
     * Кэш для отслеживания времени последнего фактического обновления в БД.
     * <p>
     * Хранится в памяти приложения (не в БД!). Ключ — ID пользователя, значение —
     * время последнего успешного UPDATE в таблице users.
     * </p>
     */
    private final Map<Long, LocalDateTime> lastUpdateCache = new ConcurrentHashMap<>();

    /** Интервал между прямыми обновлениями БД (в секундах) — 1 минута. */
    private static final int UPDATE_INTERVAL_SECONDS = 60;

    /**
     * "Дешевый" метод — обновляет только кэш, НЕ БД.
     * <p>
     * Вызывается при КАЖДОМ HTTP-запросе пользователя. Реальная запись в БД происходит
     * не чаще 1 раза в минуту на пользователя.
     * </p>
     *
     * @param userId идентификатор пользователя (не может быть null)
     */
    public void recordActivity(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = lastUpdateCache.get(userId);

        // Обновляем БД только если прошло больше UPDATE_INTERVAL_SECONDS
        if (lastUpdate == null ||
                java.time.Duration.between(lastUpdate, now).getSeconds() >= UPDATE_INTERVAL_SECONDS) {

            lastUpdateCache.put(userId, now);
            updateLastSeenInDatabase(userId, now);
        }
    }

    /**
     * Прямой UPDATE в БД без загрузки всей сущности.
     * <p>
     * Использует кастомный метод репозитория, который выполняет
     * {@code UPDATE users SET last_seen = :timestamp WHERE id = :userId}.
     * НАМНОГО быстрее, чем findById() + save().
     * </p>
     *
     * @param userId    идентификатор пользователя
     * @param timestamp время последней активности
     */
    @Transactional
    public void updateLastSeenInDatabase(Long userId, LocalDateTime timestamp) {
        int updatedCount = userRepository.updateLastSeenAndOnline(userId, timestamp);
        if (updatedCount > 0) {
            log.debug("Updated last_seen in database");
        }
    }

    /**
     * Старый метод — оставлен для обратной совместимости.
     * <p>
     * <b>Не должен использоваться в новом коде!</b> Если этот метод вызывается,
     * проверьте и замените вызов на {@link #recordActivity(Long)}.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @deprecated Используйте {@link #recordActivity(Long)} вместо этого метода
     */
    @Deprecated
    @Transactional
    public void updateUserActivity(Long userId) {
        log.warn("DEPRECATED method updateUserActivity() called. Use recordActivity() instead.");

        // fallback — используем новый метод
        recordActivity(userId);
    }

    /**
     * Определяет, находится ли пользователь онлайн прямо сейчас.
     * <p>
     * Пользователь считается онлайн, если его последняя активность была не более 1 минуты назад.
     * </p>
     *
     * @param user объект пользователя (может быть null)
     * @return true — если пользователь онлайн, false — иначе
     */
    public boolean isUserOnline(User user) {
        if (user == null || user.getLastSeen() == null) {
            return false;
        }
        java.time.Duration duration = java.time.Duration.between(user.getLastSeen(), LocalDateTime.now());
        return duration.toMinutes() <= 1;
    }

    /**
     * Периодическое обновление статусов онлайн (заглушка для обратной совместимости).
     * <p>
     * Выполняется каждую минуту. В текущей реализации не выполняет полезной работы,
     * но оставлен для совместимости с существующим кодом.
     * </p>
     */
    @Scheduled(fixedRate = 60000)
    public void updateOnlineStatuses() {
        // Заглушка: логика перенесена в recordActivity и isUserOnline
        log.trace("Scheduled online status update executed (no-op)");
    }

    /**
     * Периодическая очистка кэша от устаревших записей.
     * <p>
     * Удаляет записи, которые не обновлялись более 10 минут. Это предотвращает
     * бесконечный рост карты lastUpdateCache для неактивных пользователей.
     * </p>
     * <p>
     * Выполняется каждые 10 минут.
     * </p>
     */
    @Scheduled(fixedRate = 600000) // 10 минут
    public void cleanUpdateCache() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        int removed = 0;

        for (Map.Entry<Long, LocalDateTime> entry : lastUpdateCache.entrySet()) {
            if (entry.getValue().isBefore(tenMinutesAgo)) {
                lastUpdateCache.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            log.debug("Cleaned {} stale entries from activity cache", removed);
        }
    }



    /**
     * Принудительно устанавливает статус пользователя "офлайн".
     * Вызывается при выходе из системы или принудительном завершении сессии.
     *
     * @param userId идентификатор пользователя
     */
    @Transactional
    public void setUserOffline(Long userId) {
        log.debug("Setting user offline: userId={}", userId);
        int updatedCount = userRepository.updateOnlineStatus(userId, false);
        if (updatedCount > 0) {
            log.debug("User {} is now offline", userId);
        }
        // Также удаляем из кэша активности
        lastUpdateCache.remove(userId);
    }

    /**
     * Принудительно устанавливает статус пользователя "офлайн" по username.
     *
     * @param username имя пользователя
     */
    @Transactional
    public void setUserOfflineByUsername(String username) {
        log.debug("Setting user offline by username: {}", username);
        userRepository.findByUsername(username).ifPresent(user -> {
            setUserOffline(user.getId());
        });
    }
}