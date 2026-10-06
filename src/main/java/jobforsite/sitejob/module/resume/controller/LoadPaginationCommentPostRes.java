package jobforsite.sitejob.module.resume.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.resume.model.CommentPersonalRes;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления пагинацией и операциями с персональными комментариями к резюме.
 * <p>
 * Обрабатывает запросы на загрузку комментариев с пагинацией, добавление новых комментариев,
 * удаление отдельных комментариев и целых веток обсуждений.
 * </p>
 */
@RequiredArgsConstructor
@Controller
public class LoadPaginationCommentPostRes extends BasePostController {

    private static final Logger log = LoggerFactory.getLogger(LoadPaginationCommentPostRes.class);

    private final PostResumeRepository postResumeRepository;
    private final CommentRepoResPersonal commentRepoResPersonal;
    private final RateLimitService rateLimitService;
    private final EnhancedRateLimitService enhancedRateLimitService;

    /**
     * Загружает следующую страницу личных комментариев к резюме с пагинацией.
     * <p>
     * В зависимости от прав пользователя (администратор, владелец поста, обычный пользователь)
     * возвращает соответствующий набор комментариев. Для неавторизованных запросов возвращает пустой результат.
     * </p>
     *
     * @param hashId         уникальный идентификатор резюме (поста)
     * @param page           номер запрашиваемой страницы (по умолчанию 0)
     * @param size           количество комментариев на странице (по умолчанию 3)
     * @param authentication объект аутентификации Spring Security
     * @return ResponseEntity с картой, содержащей статус, список комментариев, флаг hasMore и текущую страницу
     */
    @GetMapping("/postidviewsres/{hashId}/load-more-personal-comments-resume")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMorePersonalCommentsRes(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        // Логируем начало загрузки комментариев
        log.debug("Запрос на загрузку комментариев для резюме с hashId: {}, страница: {}, размер: {}", hashId, page, size);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Неавторизованный запрос на загрузку комментариев, возвращаем пустой результат");
            return ResponseEntity.ok(Map.of(
                    "comments", new ArrayList<>(),
                    "hasMore", false,
                    "currentPage", 0
            ));
        }

        try {
            // Поиск резюме по hashId
            Resume res = postResumeRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Получаем текущего пользователя
            User currentUser = (User) authentication.getPrincipal();
            // Проверяем, имеет ли пользователь права администратора
            boolean hasAdminAccess = hasSuperAccess(authentication);
            boolean isOwner = currentUser.getId().equals(res.getUser().getId());

            // Проверка доступности поста для текущего пользователя
            // Для админов пропускаем проверку доступности поста
            if (!hasAdminAccess && !isOwner && (res.isBlocked() || res.isArchived() || res.isModeration())) {
                log.warn("Попытка доступа к недоступному посту hashId: {} пользователем без прав", hashId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            Long resId = res.getId();

            // Повторная проверка аутентификации (дублирование для безопасности)
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Аутентификация потеряна при загрузке комментариев для hashId: {}", hashId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "Требуется авторизация");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Проверка ролей
            boolean hasSuperAccess = hasSuperAccess(authentication);
            boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

            // Создание пагинации (сортировка по дате создания DESC)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            //Получение среза корневых комментариев в зависимости от прав
            Slice<CommentPersonalRes> commentsSlice;

            if (hasSuperAccess) {
                // SUPER и Админ видят все личные комментарии
                commentsSlice = commentRepoResPersonal.findSliceByResumeIdAndParentCommentIsNull(resId, pageable);
                log.debug("Пользователь с SUPER-доступом загружает все комментарии для резюме id: {}", resId);
            } else if (currentUser.getId().equals(res.getUser().getId())) {
                // Автор поста видит ВСЕ личные комментарии к своему посту
                commentsSlice = commentRepoResPersonal.findSliceByResumeIdAndParentCommentIsNull(resId, pageable);
                log.debug("Владелец поста загружает все комментарии для своего резюме id: {}", resId);
            } else {
                // Обычные пользователи видят ТОЛЬКО свои комментарии и ответы на них
                commentsSlice = commentRepoResPersonal.findVisibleCommentsSliceForUser(resId, currentUser.getId(), pageable);
                log.debug("Обычный пользователь загружает только свои комментарии для резюме id: {}", resId);
            }

            // Преобразование комментариев в DTO для ответа
            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentPersonalRes comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertPersonalCommentResToMap(comment, currentUser, authentication);
                // Добавляем только если commentData не null (пользователь имеет право видеть комментарий)
                if (commentData != null) {
                    commentsData.add(commentData);
                }
            }

            //Формирование успешного ответа
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", commentsSlice.hasNext());
            response.put("currentPage", page);
            response.put("isSuperUser", hasOnlySuperAccess);

            log.debug("Успешно загружено {} комментариев для резюме id: {}, hasMore: {}", commentsData.size(), resId, commentsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.info("Ошибка при загрузке комментариев для hashId: {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Преобразует объект {@link CommentPersonalRes} в карту для передачи на фронтенд.
     * <p>
     * Метод учитывает права доступа: владелец поста и администраторы видят полную информацию,
     * обычные пользователи — только ограниченный набор данных (имена скрыты).
     * </p>
     *
     * @param comment        объект комментария
     * @param currentUser    текущий авторизованный пользователь
     * @param authentication объект аутентификации Spring Security
     * @return карта с данными комментария или null, если пользователь не должен видеть этот комментарий
     */
    private Map<String, Object> convertPersonalCommentResToMap(CommentPersonalRes comment, User currentUser, Authentication authentication) {
        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);
        boolean hasSuperAccess = hasSuperAccess(authentication);

        // Проверяем, может ли текущий пользователь видеть этот комментарий
        // Владелец поста видит ВСЕ комментарии к своему посту
        boolean canSeeComment = hasSuperAccess || // SUPER и ADMIN
                currentUser.getId().equals(comment.getResume().getUser().getId()) || // Владелец поста видит все
                currentUser.getId().equals(comment.getUser().getId()) || // Автор комментария
                (comment.getRecipient() != null && currentUser.getId().equals(comment.getRecipient().getId())); // Получатель

        if (!canSeeComment) {
            log.debug("Пользователь не имеет прав на просмотр комментария id: {}", comment.getId());
            return null;
        }

        Map<String, Object> commentData = new HashMap<>();
        // Основная информация о комментарии
        commentData.put("id", comment.getId());

        // Текст комментария не кодируется в base64 для всех пользователей
        String originalText = comment.getText();
        commentData.put("text", originalText);
        commentData.put("textEncoded", false); // Флаг, что текст НЕ закодирован

        // Для SUPER-пользователей добавляем также оригинальный текст (дублирование для удобства)
        if (hasSuperAccess) {
            commentData.put("originalText", originalText);
        }

        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));
        commentData.put("parentId", comment.getParentComment() != null ? comment.getParentComment().getId() : null);

        // Информация о получателе комментария
        if (comment.getRecipient() != null) {
            commentData.put("hasRecipient", true);
            commentData.put("recipientId", comment.getRecipient().getHashId());
            commentData.put("recipientName", comment.getRecipient().getNameuse());
        } else if (comment.getParentComment() != null && comment.getParentComment().getUser() != null) {
            // Fallback: адресат — автор родительского комментария
            commentData.put("hasRecipient", true);
            commentData.put("recipientId", comment.getParentComment().getUser().getHashId());
            commentData.put("recipientName", comment.getParentComment().getUser().getNameuse());
        } else {
            commentData.put("hasRecipient", false);
            commentData.put("recipientName", null);
        }

        // Добавляем информацию о посте для отладки
        commentData.put("resId", comment.getResume().getId());
        commentData.put("resOwnerId", comment.getResume().getUser().getHashId());

        // Информация об авторе
        Map<String, Object> authorData = new HashMap<>();
        if (comment.getUser() != null) {
            if (hasOnlySuperAccess) {
                // Только SUPER-пользователь видит реальные данные
                authorData.put("id", comment.getUser().getId());
                authorData.put("username", comment.getUser().getUsername());
                authorData.put("nameuse", comment.getUser().getNameuse());
            } else {
                // Для остальных - скрытая информация
                authorData.put("id", 0);
                authorData.put("username", "Пользователь");
                authorData.put("nameuse", comment.getUser().getNameuse());
            }
            authorData.put("online", comment.getUser().isOnline());
        } else {
            authorData.put("id", null);
            authorData.put("username", "Удаленный пользователь");
            authorData.put("nameuse", "Удаленный пользователь");
            authorData.put("online", false);
        }
        commentData.put("author", authorData);

        // Добавляем флаги для фронтенда
        commentData.put("isSuperAdminUser", hasSuperAccess);
        commentData.put("isResOwner", currentUser.getId().equals(comment.getResume().getUser().getId()));

        // Рекурсивная загрузка ответов на комментарий (replies)
        List<Map<String, Object>> repliesData = new ArrayList<>();
        List<CommentPersonalRes> replies = commentRepoResPersonal.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());

        for (CommentPersonalRes reply : replies) {
            Map<String, Object> replyData = convertPersonalCommentResToMap(reply, currentUser, authentication);
            if (replyData != null) {
                repliesData.add(replyData);
            }
        }
        commentData.put("replies", repliesData);

        return commentData;
    }

    /**
     * Добавляет новый персональный комментарий к резюме.
     * <p>
     * Выполняет валидацию комментария (длина, частота запросов), проверяет доступность поста,
     * устанавливает получателя (автор поста или автор родительского комментария) и сохраняет комментарий.
     * Поддерживает как AJAX-запросы (JSON-ответ), так и обычные (редирект).
     * </p>
     *
     * @param hashId           уникальный идентификатор резюме
     * @param text             текст комментария
     * @param parentCommentId  ID родительского комментария (опционально, для ответов)
     * @param authentication   объект аутентификации Spring Security
     * @param request          HTTP-запрос (для определения типа запроса и IP-адреса)
     * @return ResponseEntity с результатом операции (успех/ошибка)
     */
    @PostMapping("/postidviewsres/{hashId}/commentPersonalRes")
    @ResponseBody
    public ResponseEntity<?> addCommentPersonalRes(
            @PathVariable String hashId,
            @RequestParam String text,
            @RequestParam(required = false) Long parentCommentId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        log.debug("Попытка добавления комментария к резюме hashId: {}, parentCommentId: {}", hashId, parentCommentId);

        // Проверка аутентификации
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Неавторизованная попытка добавления комментария к резюме hashId: {}", hashId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Требуется авторизация"
            ));
        }

        // Валидация комментария с использованием сервисов лимитирования (для персональных комментариев)
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "personal")) {
            log.warn("Комментарий отклонен валидацией: слишком короткий текст или превышен лимит запросов для IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", "ignored",
                    "message", "Комментарий слишком короткий или слишком частые запросы"
            ));
        }

        try {
            // Поиск резюме
            Resume res = postResumeRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Проверка доступности поста
            if (res.isBlocked() || res.isArchived() || res.isModeration()) {
                log.warn("Попытка добавления комментария к недоступному посту hashId: {}", hashId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            User user = (User) authentication.getPrincipal();

            // Создание и заполнение нового комментария
            CommentPersonalRes commentPersonalRes = new CommentPersonalRes();
            commentPersonalRes.setText(text);
            commentPersonalRes.setResume(res);
            commentPersonalRes.setUser(user);

            // Установка родительского комментария (если указан)
            if (parentCommentId != null && parentCommentId > 0) {
                CommentPersonalRes parentComment = commentRepoResPersonal.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    commentPersonalRes.setParentComment(parentComment);
                }
            }

            // Установка получателя комментария (recipient)
            if (commentPersonalRes.getParentComment() != null) {
                // Ответ в ветке — получатель = автор родительского комментария
                commentPersonalRes.setRecipient(commentPersonalRes.getParentComment().getUser());
            } else {
                // Корневой комментарий — получатель = владелец поста
                // (если сам владелец пишет корневой комментарий — recipient = null)
                if (user.getId().equals(res.getUser().getId())) {
                    commentPersonalRes.setRecipient(null);
                } else {
                    commentPersonalRes.setRecipient(res.getUser());
                }
            }

            // Сохранение комментария
            commentRepoResPersonal.save(commentPersonalRes);
            log.debug("Комментарий успешно добавлен к резюме hashId: {}", hashId);

            //Формирование ответа в зависимости от типа запроса
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Комментарий успешно добавлен"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/postidviewsres/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении комментария к резюме hashId: {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Ошибка при добавлении комментария: " + e.getMessage()
            ));
        }
    }

    /**
     * Удаляет конкретный комментарий (только для автора комментария или администратора).
     * <p>
     * Если пользователь имеет права SUPER-доступа, комментарий удаляется без дополнительных проверок.
     * Иначе удаление разрешено только автору комментария.
     * </p>
     *
     * @param hashId                  уникальный идентификатор резюме
     * @param commentPersonalResId    ID удаляемого комментария
     * @param authentication          объект аутентификации Spring Security
     * @param model                   модель Spring MVC для передачи сообщений об ошибках
     * @return строка с redirect-адресом на страницу резюме
     */
    @PostMapping("/postidviewsres/{hashId}/commentPersonalRes/{commentPersonalResId}/delete")
    public String deleteCommentPersonalRes(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalResId,
            Authentication authentication,
            Model model
    ) {
        log.debug("Запрос на удаление комментария id: {} для резюме hashId: {}", commentPersonalResId, hashId);

        try {
            User user = (User) authentication.getPrincipal();

            // SUPER-доступ: удаляем без проверок
            if (hasSuperAccess(authentication)) {
                commentRepoResPersonal.deleteById(commentPersonalResId);
                log.debug("Комментарий id: {} удален пользователем с SUPER-доступом", commentPersonalResId);
                return "redirect:/postidviewsres/" + hashId;
            }

            // Обычный пользователь: проверяем, что он автор комментария
            CommentPersonalRes comment = commentRepoResPersonal.findById(commentPersonalResId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

            if (comment.getUser().getId().equals(user.getId())) {
                commentRepoResPersonal.deleteById(commentPersonalResId);
                log.debug("Комментарий id: {} удален его автором", commentPersonalResId);
            } else {
                log.warn("Пользователь без прав попытался удалить комментарий id: {}", commentPersonalResId);
                model.addAttribute("error", "Вы можете удалять только свои комментарии");
                return "redirect:/postidviewsres/" + hashId;
            }

        } catch (Exception e) {
            log.error("Ошибка при удалении комментария id: {} для резюме hashId: {}, errorType: {}", commentPersonalResId, hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/postidviewsres/" + hashId;
    }

    /**
     * Удаляет ветку комментариев (комментарий вместе со всеми ответами).
     * <p>
     * Доступно только для пользователей с правами SUPER-доступа.
     * </p>
     *
     * @param hashId                  уникальный идентификатор резюме
     * @param commentPersonalResId    ID корневого комментария удаляемой ветки
     * @param authentication          объект аутентификации Spring Security
     * @param model                   модель Spring MVC для передачи сообщений об ошибках
     * @return строка с redirect-адресом на страницу резюме
     */
    @PostMapping("/postidviewsres/{hashId}/commentPersonalRes/{commentPersonalResId}/deleteBranch")
    public String deleteCommentBranch(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalResId,
            Authentication authentication,
            Model model
    ) {
        log.debug("Запрос на удаление ветки комментариев с корнем id: {} для резюме hashId: {}", commentPersonalResId, hashId);

        if (hasSuperAccess(authentication)) {
            commentRepoResPersonal.findById(commentPersonalResId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));
            deleteCommentWithReplies(commentPersonalResId);
            log.debug("Ветка комментариев с корнем id: {} успешно удалена", commentPersonalResId);
            return "redirect:/postidviewsres/" + hashId;
        } else {
            log.warn("Пользователь без SUPER-доступа попытался удалить ветку комментариев id: {}", commentPersonalResId);
            model.addAttribute("error", "Доступ запрещен");
            return "redirect:/postidviewsres/" + hashId;
        }
    }

    /**
     * Рекурсивно удаляет комментарий и все его ответы (всю ветку).
     *
     * @param commentId ID корневого комментария удаляемой ветки
     */
    private void deleteCommentWithReplies(Long commentId) {
        List<CommentPersonalRes> replies = commentRepoResPersonal.findAllRepliesByParentId(commentId);
        for (CommentPersonalRes reply : replies) {
            deleteCommentWithReplies(reply.getId());
        }
        commentRepoResPersonal.deleteById(commentId);
    }
}