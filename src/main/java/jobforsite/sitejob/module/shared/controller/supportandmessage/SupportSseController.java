package jobforsite.sitejob.module.shared.controller.supportandmessage;

import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jobforsite.sitejob.module.shared.services.SupportSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

/**
 * Контроллер для Server-Sent Events (SSE) в службе поддержки.
 * Обеспечивает реальное время уведомлений о новых тикетах,
 * ответах и изменениях статуса.
 */
@RequiredArgsConstructor
@RestController
public class SupportSseController extends BaseController {

    private final SupportSseService sseService;
    private final UserRepository userRepository;

    /**
     * Эндпоинт для подписки на SSE-уведомления службы поддержки.
     * Создает и возвращает SseEmitter для текущего аутентифицированного пользователя.
     * <p>
     * При вызове также обновляет статус онлайн и время последнего визита пользователя.
     *
     * @param authentication объект аутентификации Spring Security
     * @return SseEmitter для отправки событий в реальном времени
     */
    @GetMapping("/api/support/sse")
    public SseEmitter subscribe(Authentication authentication) {
        // Получаем текущего пользователя из аутентификации
        User currentUser = (User) authentication.getPrincipal();

        // Обновляем статус онлайн и время последнего визита
        currentUser.setOnline(true);
        currentUser.setLastSeen(LocalDateTime.now());
        userRepository.save(currentUser); // Сохраняем изменения

        // Создаем и возвращаем SSE-соединение для пользователя
        return sseService.createEmitter(currentUser.getId());
    }
}