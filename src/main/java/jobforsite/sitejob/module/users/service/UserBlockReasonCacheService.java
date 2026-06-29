package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для кеширования информации о блокировке пользователей.
 * <p>
 * Предназначен для минимизации обращений к базе данных при проверке статуса
 * блокировки пользователя. Использует Spring Cache с провайдером (например, Caffeine или Redis).
 * </p>
 *
 * <p><b>Кешируемые данные:</b></p>
 * <ul>
 *   <li>{@code userBlockReason} — причина блокировки (строка)</li>
 *   <li>{@code userBlockInfo} — полная информация о блокировке (объект {@link BlockInfo})</li>
 * </ul>
 *
 * <p><b>Инвалидация кеша:</b> при вызове {@link #clearBlockCache(Long)}</p>
 *
 * @author YourTeam
 * @version 1.0
 */
@RequiredArgsConstructor
@Service
public class UserBlockReasonCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserBlockReasonCacheService.class);

    private final UserRepository userRepository;

    /**
     * Возвращает причину блокировки пользователя из кеша или БД.
     * <p>
     * Результат кешируется в {@code userBlockReason}. Если пользователь активен
     * (не заблокирован), возвращается {@code null}, который НЕ кешируется (unless = "#result == null").
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return причина блокировки или {@code null}, если пользователь не заблокирован
     */
    @Cacheable(value = "userBlockReason", key = "#userId", unless = "#result == null")
    public String getUserBlockReason(Long userId) {
        log.debug("Cache miss for block reason");

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && !userOpt.get().isActive()) {
            return userOpt.get().getBlockReason();
        }
        return null;
    }

    /**
     * Возвращает полную информацию о блокировке пользователя.
     * <p>
     * Результат кешируется в {@code userBlockInfo}. В отличие от {@link #getUserBlockReason(Long)},
     * этот метод возвращает объект с дополнительными полями: дата блокировки,
     * срок окончания блокировки, количество блокировок.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return объект {@link BlockInfo} или {@code null}, если пользователь не заблокирован
     */
    @Cacheable(value = "userBlockInfo", key = "#userId")
    public BlockInfo getUserBlockInfo(Long userId) {
        log.debug("Cache miss for block info");

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && !userOpt.get().isActive()) {
            User user = userOpt.get();
            return new BlockInfo(
                    user.getBlockReason(),
                    user.getBlockedAt(),
                    user.getBlockUntil(),
                    user.getBlockCount()
            );
        }
        return null;
    }

    /**
     * Очищает кеш блокировки для указанного пользователя.
     * <p>
     * Должен вызываться при любом изменении статуса блокировки:
     * <ul>
     *   <li>Блокировка пользователя</li>
     *   <li>Разблокировка пользователя</li>
     *   <li>Изменение причины блокировки</li>
     *   <li>Продление срока блокировки</li>
     * </ul>
     * </p>
     *
     * @param userId идентификатор пользователя
     */
    @CacheEvict(value = {"userBlockReason", "userBlockInfo"}, key = "#userId")
    public void clearBlockCache(Long userId) {
        log.debug("Block cache cleared");
    }

    /**
     * DTO-класс, содержащий полную информацию о блокировке пользователя.
     * <p>
     * Используется для кеширования и передачи данных из сервиса в контроллеры/шаблоны.
     * Содержит методы форматирования дат для удобного отображения в UI.
     * </p>
     */
    public static class BlockInfo {
        private final String reason;
        private final java.time.LocalDateTime blockedAt;
        private final java.time.LocalDateTime blockUntil;
        private final int blockCount;

        /**
         * Конструктор информации о блокировке.
         *
         * @param reason     причина блокировки
         * @param blockedAt  дата и время блокировки
         * @param blockUntil дата и время окончания блокировки (может быть null)
         * @param blockCount количество блокировок пользователя
         */
        public BlockInfo(String reason, java.time.LocalDateTime blockedAt,
                         java.time.LocalDateTime blockUntil, int blockCount) {
            this.reason = reason;
            this.blockedAt = blockedAt;
            this.blockUntil = blockUntil;
            this.blockCount = blockCount;
        }

        public String getReason() { return reason; }
        public java.time.LocalDateTime getBlockedAt() { return blockedAt; }
        public java.time.LocalDateTime getBlockUntil() { return blockUntil; }
        public int getBlockCount() { return blockCount; }

        /**
         * Возвращает отформатированную дату блокировки.
         *
         * @return строка в формате "dd.MM.yyyy HH:mm" или "неизвестно"
         */
        public String getFormattedBlockedAt() {
            return blockedAt != null ?
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(blockedAt) :
                    "неизвестно";
        }

        /**
         * Возвращает отформатированную дату окончания блокировки.
         *
         * @return строка в формате "dd.MM.yyyy HH:mm" или "неизвестно"
         */
        public String getFormattedBlockUntil() {
            return blockUntil != null ?
                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(blockUntil) :
                    "неизвестно";
        }

        /**
         * Возвращает количество оставшихся дней блокировки.
         *
         * @return количество полных дней до окончания блокировки, 0 если дата не указана
         */
        public long getRemainingDays() {
            if (blockUntil == null) return 0;
            return java.time.Duration.between(java.time.LocalDateTime.now(), blockUntil).toDays();
        }
    }
}