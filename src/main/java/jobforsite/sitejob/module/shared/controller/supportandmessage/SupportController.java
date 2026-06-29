package jobforsite.sitejob.module.shared.controller.supportandmessage;

import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.repository.SupportCommentRepo;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jobforsite.sitejob.module.shared.services.SupportSseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Контроллер для обработки запросов в службу поддержки.
 * Управляет созданием тикетов, ответами, назначением операторов и уведомлениями через SSE.
 */
@RequiredArgsConstructor
@Controller
public class SupportController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(SupportController.class);

    private final SupportCommentRepo supportCommentRepo;
    private final UserRepository userRepo;
    private final SupportSseService sseService;

    /**
     * Карта категорий обращений с человекочитаемыми заголовками.
     */
    private final Map<String, String> categoryTitles = Map.of(
            "account", "Проблемы с личным аккаунтом?",
            "ads", "Проблема с обьявлениями?",
            "users", "Проблема с другими пользователями?",
            "complaints", "Жалобы",
            "suggestions", "Предложения"
    );

    /**
     * Проверяет, может ли пользователь видеть конкретный тикет.
     *
     * @param comment тикет (комментарий)
     * @param currentUser текущий пользователь
     * @param authentication объект аутентификации
     * @return true, если доступ разрешен
     */
    private boolean canUserSeeTicket(SupportComment comment, User currentUser, Authentication authentication) {
        boolean isSuperOrAdmin = hasSuperAccess(authentication);
        String currentUserNameuse = currentUser.getNameuse();
        String commentUserNameuse = comment.getUser() != null ? comment.getUser().getNameuse() : null;

        if (isSuperOrAdmin) {
            if (hasOnlySuperAccess(authentication)) {
                // SUPER видит все тикеты
                return true;
            } else if (hasOnlyAdminAccess(authentication)) {
                // ADMIN видит свободные или свои взятые тикеты
                String assignedSuperNameuse = comment.getAssignedSuper() != null ?
                        comment.getAssignedSuper().getNameuse() : null;
                return comment.getAssignedSuper() == null ||
                        (assignedSuperNameuse != null && assignedSuperNameuse.equals(currentUserNameuse));
            }
        } else {
            // Обычный пользователь видит ТОЛЬКО СВОИ ТИКЕТЫ
            // Сравниваем по nameuse
            boolean isOwnComment = commentUserNameuse != null &&
                    commentUserNameuse.equals(currentUserNameuse);

            return isOwnComment;
        }

        return false;
    }

    /**
     * Отображает главную страницу службы поддержки.
     * Если указана категория, отмечает её как прочитанную для текущего пользователя.
     *
     * @param category категория (опционально)
     * @param authentication объект аутентификации
     * @param model модель для передачи данных в представление
     * @return имя шаблона страницы поддержки
     */
    @GetMapping("/support_service")
    public String supportService(@RequestParam(value = "category", required = false) String category,
                                 Authentication authentication,
                                 Model model) {
        User currentUser = (User) authentication.getPrincipal();
        boolean isSuperOrAdmin = hasSuperAccess(authentication);
        boolean isSuperOnly = hasOnlySuperAccess(authentication);
        boolean isAdminOnly = hasOnlyAdminAccess(authentication);

        List<String> unreadCategories;

        if (isSuperOrAdmin) {
            // Для SUPER и ADMIN: категории с непрочитанными сообщениями ИЛИ с невзятыми тикетами
            List<String> unreadMessages = supportCommentRepo.findCategoriesWithUnreadForSuper(currentUser.getId());
            List<String> unassignedCategories = supportCommentRepo.findCategoriesWithUnassignedMessages();

            Set<String> allCategories = new HashSet<>(unreadMessages);
            allCategories.addAll(unassignedCategories);
            unreadCategories = new ArrayList<>(allCategories);
        } else {
            unreadCategories = supportCommentRepo.findCategoriesWithUnreadSuperRepliesForUser(currentUser.getId());
        }

        if (category == null) {
            model.addAttribute("categories", categoryTitles);
            model.addAttribute("unreadCategories", unreadCategories);
            model.addAttribute("isSuper", isSuperOrAdmin);
            model.addAttribute("isAdmin", currentUser.getRole().equals("ROLE_ADMIN"));
            model.addAttribute("isSuperOnly", isSuperOnly);
            model.addAttribute("isAdminOnly", isAdminOnly);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("selectedCategory", null);
            return "page_one/support_service";
        }

        if (isSuperOrAdmin) {
            supportCommentRepo.markCategoryAsReadForSuper(category, currentUser.getId());
        } else {
            supportCommentRepo.markCategoryAsReadForUser(category, currentUser.getId());
        }

        model.addAttribute("categories", categoryTitles);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("unreadCategories", unreadCategories);
        model.addAttribute("isSuper", isSuperOrAdmin);
        model.addAttribute("isAdmin", currentUser.getRole().equals("ROLE_ADMIN"));
        model.addAttribute("currentUser", currentUser);

        return "page_one/support_service";
    }

    /**
     * Уведомляет всех SUPER и ADMIN о новом тикете через SSE.
     *
     * @param category категория тикета
     * @param ticket созданный тикет
     */
    private void notifySuperAdminsAboutNewTicket(String category, SupportComment ticket) {
        List<User> superAdmins = userRepo.findByRoleIn(List.of("ROLE_SUPER", "ROLE_ADMIN"));

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "new_ticket");
        notification.put("category", category);
        notification.put("ticketId", ticket.getId());
        notification.put("userName", ticket.getUser() != null ? ticket.getUser().getNameuse() : "Пользователь удален");
        notification.put("timestamp", LocalDateTime.now().toString());

        for (User admin : superAdmins) {
            sseService.sendNotification(admin.getId(), "new_ticket", notification);

            // Если тикет не взят, отправляем дополнительное уведомление о невзятом тикете
            if (ticket.getAssignedSuper() == null) {
                Map<String, Object> unassignedNotification = new HashMap<>();
                unassignedNotification.put("type", "unassigned_ticket");
                unassignedNotification.put("category", category);
                unassignedNotification.put("ticketId", ticket.getId());
                sseService.sendNotification(admin.getId(), "unassigned_ticket", unassignedNotification);
            }
        }
    }

    /**
     * Добавляет новый комментарий (тикет или ответ) в указанной категории.
     * Обрабатывает права доступа и отправляет SSE-уведомления.
     *
     * @param category категория
     * @param text текст сообщения
     * @param parentCommentId ID родительского тикета (для ответов)
     * @param replyId ID комментария, на который отвечают (опционально)
     * @param authentication объект аутентификации
     * @param redirectAttributes атрибуты для редиректа с flash-сообщениями
     * @return перенаправление на страницу поддержки
     */
    @PostMapping("/support_service/{category}/comment")
    public String addSupportComment(@PathVariable String category,
                                    @RequestParam String text,
                                    @RequestParam(required = false) Long parentCommentId,
                                    @RequestParam(required = false) Long replyId,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = (User) authentication.getPrincipal();
        String currentUserNameuse = currentUser.getNameuse();

        try {
            if (parentCommentId != null && parentCommentId > 0) {
                // Проверяем существование тикета
                Optional<SupportComment> parentCommentOpt = supportCommentRepo.findById(parentCommentId);

                if (parentCommentOpt.isEmpty()) {
                    // Тикет не существует (удален)
                    logger.warn("Попытка ответить в удаленный тикет.");

                    redirectAttributes.addFlashAttribute("messageType", "error");
                    redirectAttributes.addFlashAttribute("messageTitle", "Тикет не найден");
                    redirectAttributes.addFlashAttribute("messageText", "Этот тикет был удален. Страница будет обновлена.");
                    return "redirect:/support_service?category=" + category;
                }

                SupportComment parentComment = parentCommentOpt.get();

                // Проверяем существование пользователя
                if (parentComment.getUser() == null) {
                    // Пользователь не существует (удален)
                    logger.warn("Попытка ответить в тикет удаленного пользователя.");

                    redirectAttributes.addFlashAttribute("messageType", "error");
                    redirectAttributes.addFlashAttribute("messageTitle", "Пользователь не найден");
                    redirectAttributes.addFlashAttribute("messageText", "Пользователь, создавший тикет, был удален. Тикет будет закрыт.");

                    // удаляем тикет, так как пользователя больше нет
                    supportCommentRepo.delete(parentComment);

                    return "redirect:/support_service?category=" + category;
                }

                // Если replyId указан, значит отвечаем на конкретный ответ
                SupportComment replyComment = null;
                if (replyId != null) {
                    replyComment = supportCommentRepo.findById(replyId).orElse(null);
                }

                // Проверяем, может ли пользователь видеть этот тикет
                String parentUserNameuse = parentComment.getUser().getNameuse();
                boolean isOwnTicket = parentUserNameuse != null &&
                        parentUserNameuse.equals(currentUserNameuse);

                // SUPER всегда может отвечать (даже если тикет не его)
                if (hasOnlySuperAccess(authentication)) {
                    // SUPER может отвечать на любой тикет
                }
                // ADMIN может отвечать только если взял тикет или если это его тикет
                else if (hasOnlyAdminAccess(authentication)) {
                    String assignedSuperNameuse = parentComment.getAssignedSuper() != null ?
                            parentComment.getAssignedSuper().getNameuse() : null;

                    boolean isAssignedToCurrent = assignedSuperNameuse != null &&
                            assignedSuperNameuse.equals(currentUserNameuse);

                    if (!isAssignedToCurrent && !isOwnTicket) {
                        redirectAttributes.addFlashAttribute("messageType", "error");
                        redirectAttributes.addFlashAttribute("messageTitle", "Ошибка доступа");
                        redirectAttributes.addFlashAttribute("messageText", "ADMIN может отвечать только в своих взятых тикетах или своих тикетах");
                        return "redirect:/support_service?category=" + category;
                    }
                }
                // Обычные пользователи могут отвещать ТОЛЬКО в своих тикетах
                else {
                    if (!isOwnTicket) {
                        redirectAttributes.addFlashAttribute("messageType", "error");
                        redirectAttributes.addFlashAttribute("messageTitle", "Ошибка доступа");
                        redirectAttributes.addFlashAttribute("messageText", "Вы можете отвечать только в своих тикетах");
                        return "redirect:/support_service?category=" + category;
                    }

                    boolean isSuperOrAdminReply = replyComment != null &&
                            (replyComment.getUser().getRole().equals("ROLE_SUPER") ||
                                    replyComment.getUser().getRole().equals("ROLE_ADMIN"));

                    if (replyId != null) {
                        // Если отвечаем на ответ
                        if (!isSuperOrAdminReply) {
                            redirectAttributes.addFlashAttribute("messageType", "error");
                            redirectAttributes.addFlashAttribute("messageTitle", "Ошибка");
                            redirectAttributes.addFlashAttribute("messageText", "Вы можете отвечать только на ответы поддержки");
                            return "redirect:/support_service?category=" + category;
                        }
                    }
                }
            }

            SupportComment comment = new SupportComment();
            comment.setText(text);
            comment.setUser(currentUser);
            comment.setCategory(category);

            SupportComment parentComment = null;
            if (parentCommentId != null && parentCommentId > 0) {
                parentComment = supportCommentRepo.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    comment.setParentComment(parentComment);
                }
            }

            supportCommentRepo.save(comment);

            // ОТПРАВКА УВЕДОМЛЕНИЙ ЧЕРЕЗ SSE
            if (parentComment != null && parentComment.getUser() != null) {
                // Если это ответ, уведомляем владельца тикета
                User ticketOwner = parentComment.getUser();

                Map<String, Object> replyNotification = new HashMap<>();
                replyNotification.put("type", "new_reply");
                replyNotification.put("category", category);
                replyNotification.put("ticketId", parentComment.getId());
                replyNotification.put("replyId", comment.getId());
                replyNotification.put("fromUser", currentUser.getNameuse());
                replyNotification.put("fromRole", currentUser.getRole());

                String shortText = text.length() > 50 ? text.substring(0, 50) + "..." : text;
                replyNotification.put("text", shortText);

                // Уведомляем владельца тикета
                sseService.sendNotificationWithRedirect(
                        ticketOwner.getId(),
                        parentComment.getId(),
                        comment.getId(),
                        category
                );

                // Также уведомляем SUPER/ADMIN, которые работают с этим тикетом
                if (parentComment.getAssignedSuper() != null &&
                        !parentComment.getAssignedSuper().getId().equals(currentUser.getId())) {
                    sseService.sendNotification(parentComment.getAssignedSuper().getId(), "new_reply", replyNotification);
                }
            } else if (parentComment == null) {
                // Если это новый тикет, уведомляем SUPER/ADMIN
                notifySuperAdminsAboutNewTicket(category, comment);
            }

            return "redirect:/support_service?category=" + category;

        } catch (Exception e) {
            logger.error("Ошибка при добавлении комментария: {}", e.getClass().getSimpleName(), e);
            redirectAttributes.addFlashAttribute("messageType", "error");
            redirectAttributes.addFlashAttribute("messageTitle", "Ошибка");
            redirectAttributes.addFlashAttribute("messageText", "Произошла ошибка при отправке сообщения");
            return "redirect:/support_service?category=" + category;
        }
    }

    /**
     * Проверяет, может ли пользователь ответить на конкретный комментарий.
     *
     * @param comment комментарий
     * @param currentUser текущий пользователь
     * @param authentication объект аутентификации
     * @return true, если ответ разрешен
     */
    private boolean canReplyToComment(SupportComment comment, User currentUser, Authentication authentication) {
        String currentUserNameuse = currentUser.getNameuse();

        if (comment.getParentComment() != null) {
            // Если это ответ (не корневой комментарий)
            // Пользователь может отвечать на ответы SUPER/ADMIN в своем тикете
            boolean isSuperOrAdminReply = comment.getUser().getRole().equals("ROLE_SUPER") ||
                    comment.getUser().getRole().equals("ROLE_ADMIN");

            String parentUserNameuse = comment.getParentComment().getUser() != null ?
                    comment.getParentComment().getUser().getNameuse() : null;
            boolean isOwnTicket = parentUserNameuse != null &&
                    parentUserNameuse.equals(currentUserNameuse);

            return isSuperOrAdminReply && isOwnTicket;
        }

        boolean isSuperOrAdmin = hasSuperAccess(authentication);
        boolean isAssigned = comment.getAssignedSuper() != null;

        String commentUserNameuse = comment.getUser() != null ? comment.getUser().getNameuse() : null;
        String assignedSuperNameuse = isAssigned ? comment.getAssignedSuper().getNameuse() : null;

        boolean isAssignedToCurrent = isAssigned &&
                assignedSuperNameuse != null &&
                assignedSuperNameuse.equals(currentUserNameuse);
        boolean isOwnComment = commentUserNameuse != null &&
                commentUserNameuse.equals(currentUserNameuse);

        if (isSuperOrAdmin) {
            // Для SUPER/ADMIN: может отвечать только если взял тикет
            // SUPER также может отвечать на свои тикеты
            return isAssignedToCurrent || (hasOnlySuperAccess(authentication) && isOwnComment);
        } else {
            // Для пользователя: может отвежать в своем тикете
            return isOwnComment;
        }
    }

    /**
     * Удаляет тикет (доступно только SUPER и ADMIN с правами на этот тикет).
     *
     * @param category категория
     * @param commentId ID тикета
     * @param authentication объект аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return перенаправление на страницу поддержки
     */
    @PostMapping("/support_service/{category}/comment/{commentId}/delete")
    public String deleteSupportComment(@PathVariable String category,
                                       @PathVariable Long commentId,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        User currentUser = (User) authentication.getPrincipal();

        try {
            if (!hasSuperAccess(authentication)) {
                throw new RuntimeException("Недостаточно прав для удаления");
            }

            // Проверяем существование тикета
            Optional<SupportComment> commentOpt = supportCommentRepo.findById(commentId);

            if (commentOpt.isEmpty()) {
                // Тикет уже удален
                logger.warn("Попытка удалить уже удаленный тикет.");

                redirectAttributes.addFlashAttribute("messageType", "info");
                redirectAttributes.addFlashAttribute("messageTitle", "Тикет уже удален");
                redirectAttributes.addFlashAttribute("messageText", "Этот тикет уже был удален другим оператором.");
                return "redirect:/support_service?category=" + category;
            }

            SupportComment comment = commentOpt.get();

            // SUPER может удалять любые тикеты
            boolean isAdminOnly = hasOnlyAdminAccess(authentication);

            if (isAdminOnly) {
                // Проверяем, может ли ADMIN удалить этот тикет
                boolean isAssigned = comment.getAssignedSuper() != null;
                boolean isAssignedToCurrent = isAssigned &&
                        comment.getAssignedSuper().getId().equals(currentUser.getId());

                if (isAssigned && !isAssignedToCurrent) {
                    throw new RuntimeException("ADMIN не может удалить тикет, взятый другим оператором");
                }
            }

            supportCommentRepo.delete(comment);

            logger.info("Тикет успешно удален.");

            return "redirect:/support_service?category=" + category;

        } catch (Exception e) {
            logger.error("Ошибка при удалении тикета: {}", e.getClass().getSimpleName(), e);
            redirectAttributes.addFlashAttribute("messageType", "error");
            redirectAttributes.addFlashAttribute("messageTitle", "Ошибка");
            redirectAttributes.addFlashAttribute("messageText", "Произошла ошибка при удалении тикета");
            return "redirect:/support_service?category=" + category;
        }
    }

    /**
     * Назначает тикет текущему SUPER/ADMIN (взять в работу).
     *
     * @param category категория
     * @param commentId ID тикета
     * @param authentication объект аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return перенаправление на страницу поддержки
     */
    @PostMapping("/support_service/{category}/comment/{commentId}/assign")
    public String assignTicket(@PathVariable String category,
                               @PathVariable Long commentId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        User currentUser = (User) authentication.getPrincipal();

        try {
            if (!hasSuperAccess(authentication)) {
                throw new RuntimeException("Недостаточно прав");
            }

            // Проверяем существование тикета
            Optional<SupportComment> commentOpt = supportCommentRepo.findById(commentId);

            if (commentOpt.isEmpty()) {
                // Тикет не существует (удален)
                logger.warn("Попытка взять удаленный тикет.");

                redirectAttributes.addFlashAttribute("messageType", "error");
                redirectAttributes.addFlashAttribute("messageTitle", "Тикет не найден");
                redirectAttributes.addFlashAttribute("messageText", "Этот тикет был удален. Страница будет обновлена.");
                return "redirect:/support_service?category=" + category;
            }

            SupportComment comment = commentOpt.get();

            // Проверяем существование пользователя
            if (comment.getUser() == null) {
                // Пользователь не существует (удален)
                logger.warn("Попытка взять тикет удаленного пользователя.");

                redirectAttributes.addFlashAttribute("messageType", "error");
                redirectAttributes.addFlashAttribute("messageTitle", "Пользователь не найден");
                redirectAttributes.addFlashAttribute("messageText", "Пользователь, создавший тикет, был удален. Тикет будет закрыт.");

                // Удаляем тикет, так как пользователя больше нет
                supportCommentRepo.delete(comment);

                return "redirect:/support_service?category=" + category;
            }

            // SUPER может взять любой тикет, даже если он уже взят другим оператором
            boolean isSuperOnly = hasOnlySuperAccess(authentication);

            // ADMIN может взять только свободный тикет
            if (!isSuperOnly && comment.getAssignedSuper() != null) {
                throw new RuntimeException("Тикет уже взят другим оператором");
            }

            comment.setAssignedSuper(currentUser);
            comment.setAssignedAt(LocalDateTime.now());
            supportCommentRepo.save(comment);

            // Уведомляем владельца тикета, что его вопрос взят в работу
            if (comment.getUser() != null) {
                Map<String, Object> assignedNotification = new HashMap<>();
                assignedNotification.put("type", "ticket_assigned");
                assignedNotification.put("category", category);
                assignedNotification.put("ticketId", comment.getId());
                assignedNotification.put("assignedTo", currentUser.getNameuse());

                sseService.sendNotification(comment.getUser().getId(), "ticket_assigned", assignedNotification);
            }

            logger.info("Тикет успешно назначен.");

            return "redirect:/support_service?category=" + category;

        } catch (Exception e) {
            logger.error("Ошибка при назначении тикета: {}", e.getClass().getSimpleName(), e);
            redirectAttributes.addFlashAttribute("messageType", "error");
            redirectAttributes.addFlashAttribute("messageTitle", "Ошибка");
            redirectAttributes.addFlashAttribute("messageText", "Произошла ошибка при взятии тикета");
            return "redirect:/support_service?category=" + category;
        }
    }

    /**
     * Освобождает тикет (отменяет назначение).
     *
     * @param category категория
     * @param commentId ID тикета
     * @param authentication объект аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return перенаправление на страницу поддержки
     */
    @PostMapping("/support_service/{category}/comment/{commentId}/unassign")
    public String unassignTicket(@PathVariable String category,
                                 @PathVariable Long commentId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = (User) authentication.getPrincipal();

        try {
            if (!hasSuperAccess(authentication)) {
                throw new RuntimeException("Недостаточно прав");
            }

            // Проверяем существование тикета
            Optional<SupportComment> commentOpt = supportCommentRepo.findById(commentId);

            if (commentOpt.isEmpty()) {
                // Тикет не существует (удален)
                logger.warn("Попытка освободить удаленный тикет.");

                redirectAttributes.addFlashAttribute("messageType", "error");
                redirectAttributes.addFlashAttribute("messageTitle", "Тикет не найден");
                redirectAttributes.addFlashAttribute("messageText", "Этот тикет был удален. Страница будет обновлена.");
                return "redirect:/support_service?category=" + category;
            }

            SupportComment comment = commentOpt.get();

            // SUPER может освободить любой тикет
            boolean isSuperOnly = hasOnlySuperAccess(authentication);

            // ADMIN может освободить только свой взятый тикет
            if (!isSuperOnly && (comment.getAssignedSuper() == null ||
                    !comment.getAssignedSuper().getId().equals(currentUser.getId()))) {
                throw new RuntimeException("Вы не можете освободить чужой тикет");
            }

            comment.setAssignedSuper(null);
            comment.setAssignedAt(null);
            supportCommentRepo.save(comment);

            logger.info("Тикет успешно освобожден.");

            return "redirect:/support_service?category=" + category;

        } catch (Exception e) {
            logger.error("Ошибка при освобождении тикета: {}", e.getClass().getSimpleName(), e);
            redirectAttributes.addFlashAttribute("messageType", "error");
            redirectAttributes.addFlashAttribute("messageTitle", "Ошибка");
            redirectAttributes.addFlashAttribute("messageText", "Произошла ошибка при освобождении тикета");
            return "redirect:/support_service?category=" + category;
        }
    }

    // ================ МЕТОДЫ ДЛЯ ПРОВЕРКИ УВЕДОМЛЕНИЙ ================

    /**
     * API-эндпоинт для проверки наличия непрочитанных уведомлений у пользователя.
     *
     * @param authentication объект аутентификации
     * @return JSON с полем hasUnread
     */
    @GetMapping("/api/support/hasUnread")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> hasUnreadMessages(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        boolean hasUnread = false;

        if (hasSuperAccess(authentication)) {
            // Для SUPER/ADMIN: есть ли непрочитанные сообщения ИЛИ невзятые тикеты
            boolean hasUnreadMessages = supportCommentRepo.hasUnreadForSuper(currentUser.getId());
            boolean hasUnassignedMessages = false;

            // Проверяем все категории на наличие невзятых тикетов
            for (String category : categoryTitles.keySet()) {
                if (supportCommentRepo.hasUnassignedMessagesForSuperInCategory(category)) {
                    hasUnassignedMessages = true;
                    break;
                }
            }

            hasUnread = hasUnreadMessages || hasUnassignedMessages;
        } else {
            // Для пользователя: есть ли непрочитанные ответы от SUPER/ADMIN
            hasUnread = supportCommentRepo.hasUnreadSuperRepliesForUser(currentUser.getId());
        }

        return ResponseEntity.ok(Collections.singletonMap("hasUnread", hasUnread));
    }

    /**
     * API-эндпоинт для получения детального статуса уведомлений.
     *
     * @param authentication объект аутентификации
     * @return JSON с деталями по категориям и типам уведомлений
     */
    @GetMapping("/api/support/notificationStatus")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotificationStatus(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        boolean isSuperOrAdmin = hasSuperAccess(authentication);

        Map<String, Object> response = new HashMap<>();

        if (isSuperOrAdmin) {
            // Для SUPER и ADMIN:
            //Есть ли непрочитанные сообщения
            boolean hasUnreadMessages = supportCommentRepo.hasUnreadForSuper(currentUser.getId());

            //Есть ли невзятые тикеты
            boolean hasUnassignedTickets = false;
            List<String> categoriesWithUnassigned = supportCommentRepo.findCategoriesWithUnassignedMessages();
            if (!categoriesWithUnassigned.isEmpty()) {
                hasUnassignedTickets = true;
            }

            //Общий статус
            boolean hasAnyNotification = hasUnreadMessages || hasUnassignedTickets;

            response.put("hasUnreadMessages", hasUnreadMessages);
            response.put("hasUnassignedTickets", hasUnassignedTickets);
            response.put("categoriesWithUnassigned", categoriesWithUnassigned);
            response.put("hasAnyNotification", hasAnyNotification);
            response.put("userType", currentUser.getRole().equals("ROLE_ADMIN") ? "ADMIN" : "SUPER");
        } else {
            // Для пользователя:
            // Есть ли непрочитанные ответы от SUPER/ADMIN
            boolean hasUnreadSuperReplies = supportCommentRepo.hasUnreadSuperRepliesForUser(currentUser.getId());

            //Категории с непрочитанными ответами
            List<String> categoriesWithUnreadReplies = supportCommentRepo.findCategoriesWithUnreadSuperRepliesForUser(currentUser.getId());

            response.put("hasUnreadSuperReplies", hasUnreadSuperReplies);
            response.put("categoriesWithUnreadReplies", categoriesWithUnreadReplies);
            response.put("hasAnyNotification", hasUnreadSuperReplies);
            response.put("userType", "USER");
        }

        return ResponseEntity.ok(response);
    }
}