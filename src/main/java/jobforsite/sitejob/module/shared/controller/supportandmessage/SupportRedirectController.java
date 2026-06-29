package jobforsite.sitejob.module.shared.controller.supportandmessage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.repository.SupportCommentRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Контроллер для обработки редиректов из уведомлений в службу поддержки.
 * Обеспечивает безопасное перенаправление к тикету с проверкой прав,
 * rate limiting и асинхронной отметкой о прочтении.
 */
@RequiredArgsConstructor
@Controller
public class SupportRedirectController extends BaseController {

    private final SupportCommentRepo supportCommentRepo;

    private static final Logger logger = LoggerFactory.getLogger(SupportRedirectController.class);

    // Конфигурация rate limiting: не более 10 запросов за 60 секунд
    private static final int RATE_LIMIT = 10;
    private static final int RATE_LIMIT_WINDOW = 60;

    private Cache<String, RateLimitInfo> rateLimitCache;

    /**
     * Инициализация кэша для rate limiting после создания бина.
     */
    @PostConstruct
    public void init() {
        rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(RATE_LIMIT_WINDOW, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
    }

    /**
     * Перенаправляет пользователя к конкретному тикету поддержки.
     * Проверяет права доступа, rate limit и статус тикета.
     *
     * @param ticketId ID тикета
     * @param replyId  ID ответа (опционально, для отметки прочитанного)
     * @param authentication объект аутентификации
     * @param redirectAttributes атрибуты для flash-сообщений
     * @return редирект на страницу поддержки или логин
     */
    @GetMapping("/message/support/{ticketId}")
    public String redirectToSupportTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Long replyId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        try {
            // Проверка rate limiting
            RateLimitCheckResult rateLimitResult = checkRateLimit(authentication);
            if (!rateLimitResult.allowed) {
                logger.warn("Rate limit exceeded. Retry after: {} seconds", rateLimitResult.retryAfter);
                addFlashMessage(redirectAttributes, "warning",
                        "Слишком много запросов",
                        "Пожалуйста, подождите " + rateLimitResult.retryAfter + " секунд");
                return "redirect:/support_service";
            }

            // Проверка аутентификации
            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            User currentUser = (User) authentication.getPrincipal();

            // Проверка статуса тикета с кэшированием
            TicketStatus status = checkTicketStatusCached(ticketId);

            if (status == null) {
                logger.info("Ticket not found");
                addFlashMessage(redirectAttributes, "error",
                        "Тикет не найден", "Тикет был удален");
                return "redirect:/support_service";
            }

            if (!status.isAvailable()) {
                logger.info("Ticket unavailable, reason: {}", status.getReason());
                addFlashMessage(redirectAttributes,
                        status.getMessageType(),
                        status.getMessageTitle(),
                        status.getMessageText());
                return "redirect:/support_service";
            }

            // Проверка прав доступа к тикету
            if (!canUserAccessTicket(status.getTicket(), currentUser, authentication)) {
                logger.warn("User attempted to access unauthorized ticket");
                addFlashMessage(redirectAttributes, "error",
                        "Доступ запрещен",
                        "У вас нет прав для просмотра этого тикета");
                return "redirect:/support_service";
            }

            // Отметка сообщения как прочитанного асинхронно
            if (replyId != null) {
                markReplyAsReadAsync(replyId, currentUser.getId());
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Redirect to support ticket {} completed in {} ms", ticketId, duration);

            // Определяем категорию для редиректа
            String category = status.getTicket().getCategory();
            return "redirect:/support_service?category=" + category;

        } catch (Exception e) {
            logger.error("Error processing redirect for ticket {}: {}", ticketId, e.getClass().getSimpleName(), e);
            addFlashMessage(redirectAttributes, "error",
                    "Ошибка", "Произошла ошибка при обработке запроса");
            return "redirect:/support_service";
        }
    }

    /**
     * Проверяет статус тикета с использованием кэша Spring Cache.
     *
     * @param ticketId ID тикета
     * @return статус тикета или null, если тикет не найден
     */
    @Cacheable(value = "supportTicketStatus", key = "#ticketId", unless = "#result == null")
    public TicketStatus checkTicketStatusCached(Long ticketId) {
        Optional<SupportComment> ticketOpt = supportCommentRepo.findById(ticketId);

        if (ticketOpt.isEmpty()) {
            return null;
        }

        SupportComment ticket = ticketOpt.get();

        // Проверяем различные статусы
        if (ticket.getUser() == null) {
            return new TicketStatus(false, "error",
                    "Ошибка",
                    "Пользователь, создавший тикет, не найден",
                    null);
        }

        if (!ticket.getUser().isActive() || ticket.getUser().isDeleted()) {
            return new TicketStatus(false, "error",
                    "Пользователь заблокирован",
                    "Пользователь, создавший тикет, заблокирован",
                    ticket);
        }

        return new TicketStatus(true, null, null, null, ticket);
    }

    /**
     * Очищает кэш статуса тикета (обычно вызывается при изменении тикета).
     *
     * @param ticketId ID тикета
     */
    @CacheEvict(value = "supportTicketStatus", key = "#ticketId")
    public void evictTicketStatusCache(Long ticketId) {
        logger.debug("Cache evicted for support ticket with id: {}", ticketId);
    }

    /**
     * Проверяет, может ли пользователь получить доступ к тикету.
     *
     * @param ticket тикет
     * @param currentUser текущий пользователь
     * @param authentication объект аутентификации
     * @return true, если доступ разрешен
     */
    private boolean canUserAccessTicket(SupportComment ticket, User currentUser, Authentication authentication) {
        boolean isSuperOrAdmin = hasSuperAccess(authentication);
        String currentUserNameuse = currentUser.getNameuse();
        String ticketOwnerNameuse = ticket.getUser().getNameuse();

        if (isSuperOrAdmin) {
            if (hasOnlySuperAccess(authentication)) {
                // SUPER видит все тикеты
                return true;
            } else if (hasOnlyAdminAccess(authentication)) {
                // ADMIN видит свободные или свои взятые тикеты
                String assignedSuperNameuse = ticket.getAssignedSuper() != null ?
                        ticket.getAssignedSuper().getNameuse() : null;
                return ticket.getAssignedSuper() == null ||
                        (assignedSuperNameuse != null && assignedSuperNameuse.equals(currentUserNameuse));
            }
        } else {
            // Обычный пользователь видит ТОЛЬКО СВОИ ТИКЕТЫ
            return ticketOwnerNameuse != null && ticketOwnerNameuse.equals(currentUserNameuse);
        }

        return false;
    }

    /**
     * Асинхронно отмечает ответ как прочитанный.
     *
     * @param replyId ID ответа
     * @param userId ID пользователя, которому принадлежит тикет
     */
    private void markReplyAsReadAsync(Long replyId, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                Optional<SupportComment> replyOpt = supportCommentRepo.findById(replyId);
                if (replyOpt.isPresent()) {
                    SupportComment reply = replyOpt.get();
                    // Проверяем, что это ответ (не корневой тикет)
                    if (reply.getParentComment() != null && !reply.getIsRead()) {
                        // Проверяем, что получатель - текущий пользователь
                        if (reply.getParentComment().getUser().getId().equals(userId)) {
                            reply.setIsRead(true);
                            supportCommentRepo.save(reply);
                            logger.debug("Support reply {} marked as read", replyId);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to mark support reply as read: {}", replyId, e);
            }
        });
    }

    /**
     * Проверяет ограничение частоты запросов (rate limiting) для пользователя.
     *
     * @param authentication объект аутентификации
     * @return результат проверки
     */
    private RateLimitCheckResult checkRateLimit(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : "anonymous";

        if (authentication == null) {
            return new RateLimitCheckResult(true, userId, 0);
        }

        String key = "rate_limit_support:" + userId;
        long now = System.currentTimeMillis() / 1000;

        RateLimitInfo info = rateLimitCache.getIfPresent(key);

        if (info == null) {
            rateLimitCache.put(key, new RateLimitInfo(1, now));
            return new RateLimitCheckResult(true, userId, 0);
        }

        // Сброс окна, если прошло достаточно времени
        if (now - info.windowStart > RATE_LIMIT_WINDOW) {
            rateLimitCache.put(key, new RateLimitInfo(1, now));
            return new RateLimitCheckResult(true, userId, 0);
        }

        // Превышение лимита
        if (info.count >= RATE_LIMIT) {
            long retryAfter = RATE_LIMIT_WINDOW - (now - info.windowStart);
            return new RateLimitCheckResult(false, userId, retryAfter > 0 ? retryAfter : 0);
        }

        // Увеличиваем счетчик
        info.count++;
        rateLimitCache.put(key, info);
        return new RateLimitCheckResult(true, userId, 0);
    }

    /**
     * Вспомогательный метод для добавления flash-сообщений при редиректе.
     */
    private void addFlashMessage(RedirectAttributes redirectAttributes,
                                 String type, String title, String text) {
        redirectAttributes.addFlashAttribute("messageType", type);
        redirectAttributes.addFlashAttribute("messageTitle", title);
        redirectAttributes.addFlashAttribute("messageText", text);
    }

    // ================ ВНУТРЕННИЕ КЛАССЫ ================

    /**
     * Внутренний класс для хранения статуса тикета.
     */
    private static class TicketStatus {
        private final boolean available;
        private final String messageType;
        private final String messageTitle;
        private final String messageText;
        private final String reason;
        private final SupportComment ticket;

        public TicketStatus(boolean available, String messageType,
                            String messageTitle, String messageText,
                            SupportComment ticket) {
            this.available = available;
            this.messageType = messageType;
            this.messageTitle = messageTitle;
            this.messageText = messageText;
            this.reason = messageTitle;
            this.ticket = ticket;
        }

        public boolean isAvailable() { return available; }
        public String getMessageType() { return messageType; }
        public String getMessageTitle() { return messageTitle; }
        public String getMessageText() { return messageText; }
        public String getReason() { return reason; }
        public SupportComment getTicket() { return ticket; }
    }

    /**
     * Вспомогательный класс для хранения информации о rate limiting.
     */
    private static class RateLimitInfo {
        int count;
        long windowStart;
        RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    /**
     * Результат проверки rate limiting.
     */
    private static class RateLimitCheckResult {
        boolean allowed;
        String userId;
        long retryAfter;
        RateLimitCheckResult(boolean allowed, String userId, long retryAfter) {
            this.allowed = allowed;
            this.userId = userId;
            this.retryAfter = retryAfter;
        }
    }
}