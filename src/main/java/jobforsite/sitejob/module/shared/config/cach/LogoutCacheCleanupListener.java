package jobforsite.sitejob.module.shared.config.cach;

import jobforsite.sitejob.module.shared.services.cachrateservice.CacheCleanupService;
import jobforsite.sitejob.module.users.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LogoutCacheCleanupListener implements ApplicationListener<LogoutSuccessEvent> {

    private final CacheCleanupService cacheCleanupService;
    private final UserActivityService userActivityService;

    @Override
    public void onApplicationEvent(LogoutSuccessEvent event) {

        try {

        // Получаем username из аутентификации
        if (event.getAuthentication() != null &&
                event.getAuthentication().getPrincipal() instanceof UserDetails) {

            UserDetails userDetails = (UserDetails) event.getAuthentication().getPrincipal();
            String username = userDetails.getUsername();

            userActivityService.setUserOfflineByUsername(username);
        }

        // Очищаем кэши, связанные с комментариями при выходе пользователя
        cacheCleanupService.clearPublicCommentsCache();
        cacheCleanupService.clearCommentCountsCache();

        } catch (Exception e) {
            log.error("Ошибка при очистке кэшей при выходе пользователя: {}",
                    e.getClass().getSimpleName());
            // Не выбрасываем исключение, чтобы не нарушить процесс логина
        }
    }
}