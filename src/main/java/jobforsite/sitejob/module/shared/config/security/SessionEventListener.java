package jobforsite.sitejob.module.shared.config.security;

import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.session.SessionDestroyedEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Слушатель событий уничтожения сессий Spring Security.
 * Устанавливает is_online = false при выходе пользователя или истечении сессии.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SessionEventListener implements ApplicationListener<SessionDestroyedEvent> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void onApplicationEvent(SessionDestroyedEvent event) {
        if (event.getSecurityContexts().isEmpty()) {
            return;
        }

        event.getSecurityContexts().forEach(context -> {
            if (context.getAuthentication() != null &&
                    context.getAuthentication().getPrincipal() instanceof UserDetails) {

                UserDetails userDetails = (UserDetails) context.getAuthentication().getPrincipal();
                String username = userDetails.getUsername();

                log.debug("Session destroyed for user: {}, setting is_online = false", username);

                // Обновляем статус онлайн в БД
                userRepository.findByUsername(username).ifPresent(user -> {
                    user.setOnline(false);
                    // Не обновляем last_seen при выходе, только сбрасываем онлайн
                    userRepository.save(user);
                    log.info("User {} is now offline", username);
                });
            }
        });
    }
}