package jobforsite.sitejob.module.shared.controller.sharedglobal;

import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.services.BlockedPostsNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API контроллер для работы с уведомлениями.
 * <p>
 * Базовый путь: /api/notifications
 * </p>
 * <p>
 * Доступные эндпоинты:
 * <ul>
 *   <li>GET /api/notifications/has-blocked-posts — проверка наличия заблокированных постов у текущего пользователя</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private BlockedPostsNotificationService blockedPostsNotificationService;

    /**
     * API endpoint для проверки наличия заблокированных постов.
     *
     * @param authentication объект аутентификации Spring Security
     * @return ResponseEntity с JSON-ответом, содержащим hasBlockedPosts и blockedCount
     */
    @GetMapping("/has-blocked-posts")
    public ResponseEntity<?> hasBlockedPosts(Authentication authentication) {
        log.debug("API /has-blocked-posts called");

        try {
            User currentUser = (User) authentication.getPrincipal();
            boolean hasBlocked = blockedPostsNotificationService.hasBlockedPosts(currentUser);
            int blockedCount = blockedPostsNotificationService.getBlockedPostsCount(currentUser);

            log.info("Blocked posts check completed, hasBlocked: {}, blockedCount: {}", hasBlocked, blockedCount);

            Map<String, Object> response = new HashMap<>();
            response.put("hasBlockedPosts", hasBlocked);
            response.put("blockedCount", blockedCount);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error while checking blocked posts: {}", e.getClass().getSimpleName());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("hasBlockedPosts", false);
            errorResponse.put("blockedCount", 0);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }
}