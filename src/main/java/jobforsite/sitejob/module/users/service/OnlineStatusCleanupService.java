package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Сервис для очистки "зависших" онлайн-статусов.
 * Пользователи, у которых last_seen старше N минут, считаются офлайн.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OnlineStatusCleanupService {

    private final UserRepository userRepository;

    /**
     * Очистка статусов онлайн для пользователей, которые не активны более 5 минут.
     * Запускается каждые 2 минуты.
     */
    @Scheduled(fixedRate = 120000) // 2 минуты
    @Transactional
    public void cleanupStaleOnlineStatuses() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);

        int updatedCount = userRepository.resetStaleOnlineStatuses(cutoffTime);

        if (updatedCount > 0) {
            log.info("Reset online status for {} stale users (last_seen before {})",
                    updatedCount, cutoffTime);
        }
    }
}