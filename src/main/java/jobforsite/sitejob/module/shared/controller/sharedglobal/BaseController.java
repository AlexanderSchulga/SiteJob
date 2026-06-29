package jobforsite.sitejob.module.shared.controller.sharedglobal;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.users.dto.UserRatingDTO;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.service.UserActivityService;
import jobforsite.sitejob.module.users.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Базовый абстрактный контроллер для всех контроллеров приложения.
 * <p>
 * Предоставляет общие методы и атрибуты модели для всех наследников:
 * <ul>
 *     <li>Отслеживание активности пользователя</li>
 *     <li>Получение рейтинга пользователя</li>
 *     <li>Проверка прав доступа (SUPER, ADMIN)</li>
 *     <li>Получение имени текущего пользователя</li>
 * </ul>
 *
 * @see User
 * @see UserService
 * @see UserActivityService
 */
@Slf4j
public abstract class BaseController {

    @Autowired
    private UserService userService;               // Сервис для работы с пользователями

    @Autowired
    private UserActivityService userActivityService; // Сервис для отслеживания активности

    /**
     * Автоматическое отслеживание активности пользователя при каждом запросе.
     * <p>
     * Метод выполняется перед каждым запросом благодаря аннотации {@link ModelAttribute}.
     * Использует {@link UserActivityService#recordActivity(Long)} для фиксации активности.
     *
     * @param authentication объект аутентификации Spring Security (может быть null)
     */
    @ModelAttribute("trackActivity")
    public void trackUserActivity(Authentication authentication, HttpServletRequest request) {
        // Пропускаем AJAX запросы избранного
        if (isAjaxFavoritesRequest(request)) {
            return;
        }

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            userActivityService.recordActivity(currentUser.getId());
            log.debug("Активность пользователя зафиксирована");
        }
    }

    /**
     * Проверка, является ли запрос AJAX-запросом избранного
     */
    private boolean isAjaxFavoritesRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/favorites/load-more") ||
                path.contains("/favorites/check") ||
                path.contains("/favorites/toggle") ||
                path.contains("/favorites/remove");
    }

    /**
     * Формирует атрибут модели с рейтингом текущего пользователя.
     * <p>
     * Атрибут доступен в представлениях под именем "userRating".
     *
     * @param authentication объект аутентификации Spring Security (может быть null)
     * @return DTO с рейтингом пользователя или null, если пользователь не аутентифицирован
     */
    @ModelAttribute("userRating")
    public UserRatingDTO getUserRating(Authentication authentication, HttpServletRequest request) {
        // Пропускаем AJAX запросы избранного
        if (isAjaxFavoritesRequest(request)) {
            return null;
        }

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            log.debug("Получен рейтинг пользователя для атрибута model 'userRating'");
            return userService.getUserRating(currentUser.getId());
        }
        return null;
    }

    /**
     * Формирует атрибут модели с рейтингом текущего пользователя (альтернативное имя).
     * <p>
     * Атрибут доступен в представлениях под именем "currentUserRatingDTO".
     *
     * @param authentication объект аутентификации Spring Security (может быть null)
     * @return DTO с рейтингом пользователя или null, если пользователь не аутентифицирован
     */
    @ModelAttribute("currentUserRatingDTO")
    public UserRatingDTO getCurrentUserRatingDTO(Authentication authentication, HttpServletRequest request) {
        // Пропускаем AJAX запросы избранного
        if (isAjaxFavoritesRequest(request)) {
            return null;
        }

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            log.debug("Получен рейтинг пользователя для атрибута model 'currentUserRatingDTO'");
            return userService.getUserRating(currentUser.getId());
        }
        return null;
    }

    /**
     * Проверяет, имеет ли аутентифицированный пользователь права SUPER или ADMIN.
     * <p>
     * Используется для предоставления расширенного доступа к функциям (блокировка, удаление и т.д.)
     *
     * @param authentication объект аутентификации Spring Security
     * @return true, если пользователь имеет роль ROLE_SUPER или ROLE_ADMIN, иначе false
     */
    protected boolean hasSuperAccess(Authentication authentication) {
        boolean hasAccess = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (hasAccess) {
            log.debug("Проверка прав SUPER/ADMIN: доступ разрешен");
        } else {
            log.debug("Проверка прав SUPER/ADMIN: доступ запрещен");
        }
        return hasAccess;
    }

    /**
     * Проверяет, имеет ли аутентифицированный пользователь только права SUPER.
     * <p>
     * Используется для критических функций, доступных только супер-администраторам.
     *
     * @param authentication объект аутентификации Spring Security
     * @return true, если пользователь имеет роль ROLE_SUPER, иначе false
     */
    protected boolean hasOnlySuperAccess(Authentication authentication) {
        boolean hasAccess = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER"));

        if (hasAccess) {
            log.debug("Проверка прав SUPER: доступ разрешен");
        } else {
            log.debug("Проверка прав SUPER: доступ запрещен");
        }
        return hasAccess;
    }

    /**
     * Проверяет, имеет ли аутентифицированный пользователь только права ADMIN.
     *
     * @param authentication объект аутентификации Spring Security
     * @return true, если пользователь имеет роль ROLE_ADMIN, иначе false
     */
    protected boolean hasOnlyAdminAccess(Authentication authentication) {
        boolean hasAccess = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (hasAccess) {
            log.debug("Проверка прав ADMIN: доступ разрешен");
        } else {
            log.debug("Проверка прав ADMIN: доступ запрещен");
        }
        return hasAccess;
    }

    /**
     * Возвращает имя (nameuse) текущего аутентифицированного пользователя.
     *
     * @param authentication объект аутентификации Spring Security
     * @return имя пользователя или null, если аутентификация отсутствует
     */
    protected String getCurrentUserNameuse(Authentication authentication) {
        if (authentication == null) {
            log.debug("Запрос имени пользователя: аутентификация отсутствует");
            return null;
        }
        User user = (User) authentication.getPrincipal();
        String nameuse = user.getNameuse();
        log.debug("Запрос имени пользователя: имя извлечено");
        return nameuse;
    }
}