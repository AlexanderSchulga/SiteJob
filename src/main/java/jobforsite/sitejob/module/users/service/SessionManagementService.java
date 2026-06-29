package jobforsite.sitejob.module.users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис управления сессиями пользователей.
 * <p>
 * Предоставляет функциональность для принудительного завершения сессий
 * и проверки наличия активных сессий.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *   <li>Работает через {@link SessionRegistry} из Spring Security</li>
 *   <li>Поддерживает только принципалы типа {@link UserDetails}</li>
 *   <li>Не логирует идентификаторы пользователей для безопасности</li>
 * </ul>
 *
 * @see SessionRegistry
 * @see UserDetails
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SessionManagementService {

    private final SessionRegistry sessionRegistry;

    private final UserActivityService userActivityService;


    /**
     * Завершает все активные сессии пользователя.
     * <p>
     * Метод находит все сессии, связанные с указанным именем пользователя,
     * и принудительно завершает их через {@link SessionInformation#expireNow()}.
     * </p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>
     * int expired = sessionManagementService.expireUserSessions("john.doe");
     * // expired == количество завершённых сессий (0, если сессий не было)
     * </pre>
     *
     * @param username имя пользователя, сессии которого нужно завершить
     * @return количество завершённых сессий (0, если пользователь не найден или нет активных сессий)
     */
    public int expireUserSessions(String username) {
        log.debug("Attempting to expire all sessions for user");

        List<Object> principals = sessionRegistry.getAllPrincipals();
        int expiredCount = 0;

        for (Object principal : principals) {
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                if (userDetails.getUsername().equals(username)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);

                    for (SessionInformation session : sessions) {
                        session.expireNow();
                        expiredCount++;
                    }

                    if (!sessions.isEmpty()) {
                        log.info("Expired {} session(s) for user", expiredCount);
                        // Устанавливаем статус офлайн в БД
                        userActivityService.setUserOfflineByUsername(username);
                    }
                }
            }
        }

        if (expiredCount == 0) {
            log.debug("No sessions expired - user not found or already inactive");
        }

        return expiredCount;
    }

    /**
     * Проверяет, имеет ли пользователь активную сессию.
     * <p>
     * Возвращает true, если найдена хотя бы одна активная сессия для указанного пользователя.
     * </p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>
     * if (sessionManagementService.hasActiveSession("john.doe")) {
     *     // пользователь уже авторизован
     * }
     * </pre>
     *
     * @param username имя пользователя для проверки
     * @return true, если есть хотя бы одна активная сессия, false в противном случае
     */
    public boolean hasActiveSession(String username) {
        log.debug("Checking active session existence for user");

        List<Object> principals = sessionRegistry.getAllPrincipals();

        for (Object principal : principals) {
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                if (userDetails.getUsername().equals(username)) {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    boolean hasSession = !sessions.isEmpty();

                    if (hasSession) {
                        log.debug("Active session found for user");
                    } else {
                        log.debug("No active sessions for user");
                    }
                    return hasSession;
                }
            }
        }

        log.debug("User not found in session registry");
        return false;
    }
}