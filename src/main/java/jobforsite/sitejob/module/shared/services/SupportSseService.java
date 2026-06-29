package jobforsite.sitejob.module.shared.services;


import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для реализации Server-Sent Events (SSE) уведомлений в реальном времени.
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Управление SSE-соединениями для каждого пользователя</li>
 *   <li>Автоматическое обновление статуса "онлайн" при подключении/отключении</li>
 *   <li>Отправка уведомлений конкретному пользователю</li>
 *   <li>Отправка уведомлений с данными для редиректа (например, при новом ответе в тикете)</li>
 * </ul>
 * </p>
 * <p>
 * SSE-соединение имеет таймаут 1 час. При завершении соединения (по таймауту, ошибке или вручную)
 * пользователь автоматически помечается как офлайн с обновлением времени последней активности.
 * </p>
 */
@RequiredArgsConstructor
@Service
public class SupportSseService {

    private static final Logger log = LoggerFactory.getLogger(SupportSseService.class);

    /**
     * Хранилище активных SSE-эмиттеров для каждого пользователя.
     * Ключ: ID пользователя, Значение: SseEmitter
     */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    /**
     * Создаёт и регистрирует новый SSE-эмиттер для пользователя.
     * <p>
     * Если для пользователя уже существовал эмиттер, он будет закрыт и заменён новым.
     * При успешном создании соединения пользователь помечается как онлайн.
     * </p>
     *
     * @param userId ID пользователя
     * @return созданный SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        // Удаляем старый эмиттер если есть
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(3600_000L); // 1 час таймаут

        emitters.put(userId, emitter);

        // Обработчики завершения
        emitter.onCompletion(() -> removeEmitter(userId));
        emitter.onTimeout(() -> removeEmitter(userId));
        emitter.onError((e) -> removeEmitter(userId));

        // Отправляем начальное событие для подтверждения соединения
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
            log.info("SSE-соединение установлено для пользователя ID: {}", userId);
        } catch (IOException e) {
            log.warn("Ошибка при отправке connect-события для пользователя ID: {}", userId);
            removeEmitter(userId);
        }

        return emitter;
    }

    /**
     * Удаляет SSE-эмиттер пользователя и обновляет его статус.
     * <p>
     * При удалении эмиттера:
     * <ul>
     *   <li>Соединение завершается (complete)</li>
     *   <li>Пользователь помечается как офлайн (online = false)</li>
     *   <li>Обновляется время последней активности (lastSeen)</li>
     * </ul>
     * </p>
     *
     * @param userId ID пользователя
     */
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE-соединение закрыто для пользователя ID: {}", userId);

            userRepository.findById(userId).ifPresent(user -> {
                user.setOnline(false);
                user.setLastSeen(LocalDateTime.now());
                userRepository.save(user);
                log.debug("Статус пользователя ID: {} обновлён: online=false, lastSeen={}", userId, LocalDateTime.now());
            });
        }
    }

    /**
     * Отправляет уведомление конкретному пользователю через SSE.
     * <p>
     * Если при отправке возникает ошибка ввода-вывода (например, клиент отключился),
     * эмиттер автоматически удаляется.
     * </p>
     *
     * @param userId    ID получателя
     * @param eventName название события (тип уведомления)
     * @param data      данные уведомления
     */
    public void sendNotification(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("Уведомление '{}' отправлено пользователю ID: {}", eventName, userId);
            } catch (IOException e) {
                log.warn("Ошибка при отправке уведомления пользователю ID: {} - соединение закрыто", userId);
                removeEmitter(userId);
            }
        } else {
            log.debug("Пользователь ID: {} не имеет активного SSE-соединения", userId);
        }
    }

    /**
     * Отправляет уведомление всем SUPER/ADMIN пользователям.
     * <p>
     * TODO: Реализовать рассылку уведомлений администраторам.
     * </p>
     *
     * @param data данные уведомления
     */
    public void broadcastToSuperAdmins(Object data) {
        // Здесь можно хранить отдельный список SUPER/ADMIN или проверять в другом месте
        log.debug("Метод broadcastToSuperAdmins вызван, но не реализован. Данные: {}", data);
    }

    /**
     * Отправляет уведомление пользователю с данными для редиректа.
     * <p>
     * Уведомление содержит:
     * <ul>
     *   <li>type - тип уведомления (new_reply)</li>
     *   <li>ticketId - ID тикета</li>
     *   <li>replyId - ID ответа</li>
     *   <li>category - категория</li>
     *   <li>redirectUrl - URL для перехода</li>
     * </ul>
     * </p>
     *
     * @param userId   ID получателя
     * @param ticketId ID тикета
     * @param replyId  ID ответа
     * @param category категория
     */
    public void sendNotificationWithRedirect(Long userId, Long ticketId, Long replyId, String category) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "new_reply");
        notification.put("ticketId", ticketId);
        notification.put("replyId", replyId);
        notification.put("category", category);
        notification.put("redirectUrl", "/message/support/" + ticketId + "?replyId=" + replyId);
        sendNotification(userId, "new_reply", notification);
    }
}