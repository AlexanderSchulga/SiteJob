package jobforsite.sitejob.module.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.service.model.CommentPersonalJob;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Контроллер для пагинации и управления персональными комментариями к постам (Job).
 * Обрабатывает запросы на загрузку, добавление, удаление комментариев и веток комментариев.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoadPaginationCommentPostJob extends BasePostController {

    private final PostRepoJob postRepoJob;
    private final CommentRepoJobPersonal commentRepoJobPersonal;

    private final RateLimitService rateLimitService;
    private final EnhancedRateLimitService enhancedRateLimitService;

    /**
     * Загружает личные комментарии к посту с пагинацией.
     * Доступ зависит от роли пользователя и статуса поста.
     *
     * @param hashId         хэш-идентификатор поста
     * @param page           номер страницы (по умолчанию 0)
     * @param size           размер страницы (по умолчанию 3)
     * @param authentication данные аутентификации пользователя
     * @return ResponseEntity с картой, содержащей комментарии, флаг hasMore и текущую страницу
     */
    @GetMapping("/postidviewsjob/{hashId}/load-more-personal-comments-job")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMorePersonalCommentsJob(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        // Неаутентифицированные пользователи получают пустой ответ
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Попытка загрузки комментариев без аутентификации, hashId: {}", hashId);
            return ResponseEntity.ok(Map.of(
                    "comments", new ArrayList<>(),
                    "hasMore", false,
                    "currentPage", 0
            ));
        }

        try {
            PostTextJob post = postRepoJob.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            User currentUser = (User) authentication.getPrincipal();
            boolean hasAdminAccess = hasSuperAccess(authentication);
            boolean isOwner = currentUser.getId().equals(post.getUser().getId());

            // Проверка доступности поста для не-админов и не-владельцев
            if (!hasAdminAccess && !isOwner && (post.isBlocked() || post.isArchived() || post.isModeration())) {
                log.warn("Доступ к посту запрещен: hashId={}, заблокирован={}, архивирован={}, модерация={}",
                        hashId, post.isBlocked(), post.isArchived(), post.isModeration());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            Long postId = post.getId();
            boolean hasSuperAccess = hasSuperAccess(authentication);
            boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Slice<CommentPersonalJob> commentsSlice;

            // Определение видимости комментариев в зависимости от роли
            if (hasSuperAccess) {
                // SUPER и Админ видят все личные комментарии
                commentsSlice = commentRepoJobPersonal.findSliceByPostjobIdAndParentCommentIsNull(postId, pageable);
                log.debug("Загрузка всех комментариев для SUPER/админа, postId={}", postId);
            } else if (currentUser.getId().equals(post.getUser().getId())) {
                // Автор поста видит ВСЕ личные комментарии к своему посту
                commentsSlice = commentRepoJobPersonal.findSliceByPostjobIdAndParentCommentIsNull(postId, pageable);
                log.debug("Загрузка всех комментариев для владельца поста, postId={}", postId);
            } else {
                // Покупатели видят ТОЛЬКО свои комментарии и ответы на них
                commentsSlice = commentRepoJobPersonal.findSliceVisibleCommentsForUser(postId, currentUser.getId(), pageable);
                log.debug("Загрузка видимых комментариев для обычного пользователя, postId={}", postId);
            }

            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentPersonalJob comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertPersonalCommentToMapJob(comment, currentUser, authentication);
                if (commentData != null) {
                    commentsData.add(commentData);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", commentsSlice.hasNext());
            response.put("currentPage", page);
            response.put("isSuperUser", hasOnlySuperAccess);

            log.info("Успешная загрузка комментариев: postId={}, страница={}, размер={}, всего комментариев={}",
                    postId, page, size, commentsData.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.info("Ошибка при загрузке комментариев для hashId={}: errorType={}", hashId, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Преобразует объект CommentPersonalJob в Map для отправки на клиент.
     * Применяются правила видимости в зависимости от прав пользователя.
     *
     * @param comment        объект комментария
     * @param currentUser    текущий аутентифицированный пользователь
     * @param authentication данные аутентификации для проверки прав
     * @return карта с данными комментария или null, если пользователь не должен его видеть
     */
    private Map<String, Object> convertPersonalCommentToMapJob(CommentPersonalJob comment, User currentUser, Authentication authentication) {

        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);
        boolean hasSuperAccess = hasSuperAccess(authentication);

        // Проверка видимости комментария для текущего пользователя
        boolean canSeeComment = hasSuperAccess ||
                currentUser.getId().equals(comment.getPostjob().getUser().getId()) ||
                currentUser.getId().equals(comment.getUser().getId()) ||
                (comment.getRecipient() != null && currentUser.getId().equals(comment.getRecipient().getId()));

        if (!canSeeComment) {
            log.debug("Комментарий скрыт для пользователя: commentId={}", comment.getId());
            return null;
        }

        // Основные поля комментария
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());

        String originalText = comment.getText();
        commentData.put("text", originalText);
        commentData.put("textEncoded", false);

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

        // Данные поста (для отладки)
        commentData.put("postId", comment.getPostjob().getId());
        commentData.put("postOwnerId", comment.getPostjob().getUser().getHashId());

        // Данные автора с сокрытием для не-SUPER пользователей
        Map<String, Object> authorData = new HashMap<>();
        if (comment.getUser() != null) {
            if (hasOnlySuperAccess) {
                authorData.put("id", comment.getUser().getId());
                authorData.put("username", comment.getUser().getUsername());
                authorData.put("nameuse", comment.getUser().getNameuse());
            } else {
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

        commentData.put("isSuperAdminUser", hasSuperAccess);
        commentData.put("isPostOwner", currentUser.getId().equals(comment.getPostjob().getUser().getId()));

        // Рекурсивная загрузка ответов на комментарий (replies)
        List<Map<String, Object>> repliesData = new ArrayList<>();
        List<CommentPersonalJob> replies = commentRepoJobPersonal.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());

        for (CommentPersonalJob reply : replies) {
            Map<String, Object> replyData = convertPersonalCommentToMapJob(reply, currentUser, authentication);
            if (replyData != null) {
                repliesData.add(replyData);
            }
        }
        commentData.put("replies", repliesData);

        return commentData;
    }

    /**
     * Добавляет новый персональный комментарий к посту.
     * Выполняет валидацию текста, проверку лимитов и определение получателя.
     *
     * @param hashId           хэш-идентификатор поста
     * @param text             текст комментария
     * @param parentCommentId  ID родительского комментария (опционально, для ответов)
     * @param authentication   данные аутентификации
     * @param request          HTTP-запрос для получения IP и типа запроса
     * @return ResponseEntity с результатом операции
     */
    @PostMapping("/postidviewsjob/{hashId}/commentPersonalJob")
    @ResponseBody
    public ResponseEntity<?> addCommentPersonalJob(
            @PathVariable String hashId,
            @RequestParam String text,
            @RequestParam(required = false) Long parentCommentId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        log.debug("Запрос на добавление комментария к услуги {} с IP: {}", hashId, clientIp);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Попытка добавления комментария без аутентификации, IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Требуется авторизация"
            ));
        }

        // Валидация комментария (длина и частота запросов)
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "personal")) {
            log.warn("Комментарий отклонен валидацией: IP={}, длина текста={}", clientIp, text != null ? text.length() : 0);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", "ignored",
                    "message", "Комментарий слишком короткий или слишком частые запросы"
            ));
        }

        try {
            PostTextJob postj = postRepoJob.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Проверка доступности поста
            if (postj.isBlocked() || postj.isArchived() || postj.isModeration()) {
                log.warn("Попытка комментирования недоступного поста: hashId={}, заблокирован={}, архивирован={}, модерация={}",
                        hashId, postj.isBlocked(), postj.isArchived(), postj.isModeration());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            User user = (User) authentication.getPrincipal();

            CommentPersonalJob commentPersonalJob = new CommentPersonalJob();
            commentPersonalJob.setText(text);
            commentPersonalJob.setPostjob(postj);
            commentPersonalJob.setUser(user);

            // Установка родительского комментария, если указан
            if (parentCommentId != null && parentCommentId > 0) {
                CommentPersonalJob parentComment = commentRepoJobPersonal.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    commentPersonalJob.setParentComment(parentComment);
                }
            }

            // Установка получателя комментария (recipient)
            if (commentPersonalJob.getParentComment() != null) {
                // Ответ в ветке — получатель = автор родительского комментария
                commentPersonalJob.setRecipient(commentPersonalJob.getParentComment().getUser());
            } else {
                // Корневой комментарий — получатель = владелец поста
                // (если сам владелец пишет корневой комментарий — recipient = null)
                if (user.getId().equals(postj.getUser().getId())) {
                    commentPersonalJob.setRecipient(null);
                } else {
                    commentPersonalJob.setRecipient(postj.getUser());
                }
            }

            commentRepoJobPersonal.save(commentPersonalJob);
            log.info("Добавлен новый комментарий: postHashId={}, parentCommentId={}, recipientSet={}",
                    hashId, parentCommentId, commentPersonalJob.getRecipient() != null);

            // Обработка AJAX-запросов
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Комментарий успешно добавлен"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/postidviewsjob/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении комментария: hashId={}, parentCommentId={}, errorType={}", hashId, parentCommentId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Ошибка при добавлении комментария: " + e.getMessage()
            ));
        }
    }

    /**
     * Удаляет один комментарий. Только автор комментария или SUPER/админ.
     *
     * @param hashId                 хэш-идентификатор поста (для редиректа)
     * @param commentPersonalJobId   ID удаляемого комментария
     * @param authentication         данные аутентификации
     * @param model                  модель для передачи ошибки
     * @return строка редиректа на страницу поста
     */
    @PostMapping("/postidviewsjob/{hashId}/commentPersonalJob/{commentPersonalJobId}/delete")
    public String deleteCommentPersonal(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalJobId,
            Authentication authentication,
            Model model
    ) {
        try {
            User user = (User) authentication.getPrincipal();

            if (hasSuperAccess(authentication)) {
                commentRepoJobPersonal.deleteById(commentPersonalJobId);
                log.info("SUPER/админ удалил комментарий: commentId={}, postHashId={}", commentPersonalJobId, hashId);
                return "redirect:/postidviewsjob/" + hashId;
            }

            CommentPersonalJob comment = commentRepoJobPersonal.findById(commentPersonalJobId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

            if (comment.getUser().getId().equals(user.getId())) {
                commentRepoJobPersonal.deleteById(commentPersonalJobId);
                log.info("Пользователь удалил свой комментарий: commentId={}, postHashId={}", commentPersonalJobId, hashId);
            } else {
                log.warn("Попытка удаления чужого комментария: commentId={}, postHashId={}", commentPersonalJobId, hashId);
                model.addAttribute("error", "Вы можете удалять только свои комментарии");
                return "redirect:/postidviewsjob/" + hashId;
            }

        } catch (Exception e) {
            log.error("Ошибка при удалении комментария: commentId={}, postHashId={}, errorType={}", commentPersonalJobId, hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/postidviewsjob/" + hashId;
    }

    /**
     * Удаляет всю ветку комментариев (комментарий и все его ответы).
     * Доступно только SUPER/админу.
     *
     * @param hashId                 хэш-идентификатор поста
     * @param commentPersonalJobId   ID корневого комментария ветки
     * @param authentication         данные аутентификации
     * @param model                  модель для ошибки
     * @return строка редиректа
     */
    @PostMapping("/postidviewsjob/{hashId}/commentPersonalJob/{commentPersonalJobId}/deleteBranch")
    public String deleteCommentBranch(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalJobId,
            Authentication authentication,
            Model model
    ) {
        if (hasSuperAccess(authentication)) {
            commentRepoJobPersonal.findById(commentPersonalJobId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));
            deleteCommentWithReplies(commentPersonalJobId);
            log.info("SUPER/админ удалил ветку комментариев: rootCommentId={}, postHashId={}", commentPersonalJobId, hashId);
            return "redirect:/postidviewsjob/" + hashId;
        } else {
            log.warn("Попытка удаления ветки без прав SUPER/админа: commentId={}, postHashId={}", commentPersonalJobId, hashId);
            model.addAttribute("error", "Доступ запрещен");
            return "redirect:/postidviewsjob/" + hashId;
        }
    }

    /**
     * Рекурсивно удаляет комментарий и все его дочерние ответы.
     *
     * @param commentId ID комментария, с которого начинается удаление
     */
    private void deleteCommentWithReplies(Long commentId) {
        List<CommentPersonalJob> replies = commentRepoJobPersonal.findAllRepliesByParentId(commentId);
        for (CommentPersonalJob reply : replies) {
            deleteCommentWithReplies(reply.getId());
        }
        commentRepoJobPersonal.deleteById(commentId);
        log.debug("Удален комментарий и его ответы: commentId={}", commentId);
    }
}