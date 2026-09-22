package jobforsite.sitejob.module.product.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.product.model.CommentPersonal;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
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
 * Контроллер для загрузки пагинированных личных комментариев и управления ими.
 * Обрабатывает запросы на получение, добавление и удаление персональных комментариев к постам.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginationCommentPost extends BasePostController {

    private final PostRepo postRepo;
    private final CommentRepoPersonal commentRepoPersonal;
    private final RateLimitService rateLimitService;
    private final EnhancedRateLimitService enhancedRateLimitService;

    /**
     * Загружает порцию личных комментариев к посту с пагинацией.
     * Учитывает права доступа: администратор/владелец поста видит всё, обычный пользователь — только свои комментарии и ответы на них.
     *
     * @param hashId         уникальный идентификатор поста
     * @param page           номер страницы (начиная с 0)
     * @param size           количество комментариев на странице
     * @param authentication данные аутентификации текущего пользователя
     * @return ResponseEntity с комментариями, флагом hasMore и статусом
     */
    @GetMapping("/postidviews/{hashId}/load-more-personal-comments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMorePersonalComments(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        // Неавторизованные запросы
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Попытка загрузки личных комментариев без авторизации, hashId: {}", hashId);
            return ResponseEntity.ok(Map.of(
                    "comments", new ArrayList<>(),
                    "hasMore", false,
                    "currentPage", 0
            ));
        }

        try {
            // Поиск поста
            PostText post = postRepo.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            User currentUser = (User) authentication.getPrincipal();
            boolean hasAdminAccess = hasSuperAccess(authentication);
            boolean isOwner = currentUser.getId().equals(post.getUser().getId());

            // Проверка доступности поста
            if (!hasAdminAccess && !isOwner && (post.isBlocked() || post.isArchived() || post.isModeration())) {
                log.warn("Запрос комментариев к недоступному посту, hashId: {}, причина: blocked={}, archived={}, moderation={}",
                        hashId, post.isBlocked(), post.isArchived(), post.isModeration());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("status", "error", "error", "post_unavailable", "message", "Пост недоступен для просмотра", "redirect", "/"));
            }

            Long postId = post.getId();
            boolean hasSuperAccess = hasSuperAccess(authentication);
            boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Slice<CommentPersonal> commentsSlice;

            // Логика видимости комментариев в зависимости от роли
            if (hasSuperAccess) {
                commentsSlice = commentRepoPersonal.findSliceByPostIdAndParentCommentIsNull(postId, pageable);
            } else if (isOwner) {
                commentsSlice = commentRepoPersonal.findSliceByPostIdAndParentCommentIsNull(postId, pageable);
            } else {
                commentsSlice = commentRepoPersonal.findVisibleCommentsSliceForUser(postId, currentUser.getId(), pageable);
            }

            // Конвертация комментариев в DTO
            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentPersonal comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertPersonalCommentToMap(comment, currentUser, authentication);
                if (commentData != null) {
                    commentsData.add(commentData);
                }
            }

            log.info("Загружена страница личных комментариев: hashId={}, page={}, size={}, commentsCount={}, hasMore={}",
                    hashId, page, size, commentsData.size(), commentsSlice.hasNext());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Комментарии успешно загружены",
                    "comments", commentsData,
                    "hasMore", commentsSlice.hasNext(),
                    "currentPage", page,
                    "isSuperUser", hasOnlySuperAccess
            ));

        } catch (Exception e) {
            log.warn("Ошибка при загрузке личных комментариев: hashId={}, page={}, errorType={}", hashId, page, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Ошибка при загрузке комментариев"));
        }
    }

    /**
     * Преобразует объект личного комментария в Map для передачи на клиент.
     * Проверяет права видимости комментария для текущего пользователя.
     *
     * @param comment        комментарий
     * @param currentUser    текущий пользователь
     * @param authentication данные аутентификации
     * @return Map с данными комментария или null, если пользователь не имеет прав на его просмотр
     */
    private Map<String, Object> convertPersonalCommentToMap(CommentPersonal comment, User currentUser, Authentication authentication) {
        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);
        boolean hasSuperAccess = hasSuperAccess(authentication);

        // Проверка видимости комментария
        boolean canSeeComment = hasSuperAccess ||
                currentUser.getId().equals(comment.getPost().getUser().getId()) ||
                currentUser.getId().equals(comment.getUser().getId()) ||
                (comment.getRecipient() != null && currentUser.getId().equals(comment.getRecipient().getId()));

        if (!canSeeComment) {
            log.debug("Скрыт комментарий для пользователя: commentId={}", comment.getId());
            return null;
        }

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());

        // Текст комментария (без кодирования)
        String originalText = comment.getText();
        commentData.put("text", originalText);
        commentData.put("textEncoded", false);
        if (hasSuperAccess) {
            commentData.put("originalText", originalText);
        }

        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));
        commentData.put("parentId", comment.getParentComment() != null ? comment.getParentComment().getId() : null);

        // Получатель
        if (comment.getRecipient() != null) {
            commentData.put("hasRecipient", true);
            commentData.put("recipientId", comment.getRecipient().getHashId());
            commentData.put("recipientName", comment.getRecipient().getNameuse());
        } else {
            commentData.put("hasRecipient", false);
        }

        commentData.put("postId", comment.getPost().getId());
        commentData.put("postOwnerId", comment.getPost().getUser().getHashId());

        // Автор комментария (с маскировкой для не-SUPER)
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
        commentData.put("isPostOwner", currentUser.getId().equals(comment.getPost().getUser().getId()));

        // Ответы на комментарий
        List<Map<String, Object>> repliesData = new ArrayList<>();
        List<CommentPersonal> replies = commentRepoPersonal.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());

        for (CommentPersonal reply : replies) {
            boolean canSeeReply = hasSuperAccess ||
                    currentUser.getId().equals(comment.getPost().getUser().getId()) ||
                    currentUser.getId().equals(reply.getUser().getId()) ||
                    (reply.getRecipient() != null && currentUser.getId().equals(reply.getRecipient().getId()));

            if (canSeeReply) {
                Map<String, Object> replyData = new HashMap<>();
                replyData.put("id", reply.getId());
                replyData.put("text", reply.getText());
                replyData.put("textEncoded", false);
                if (hasSuperAccess) {
                    replyData.put("originalText", reply.getText());
                }
                replyData.put("createdAt", reply.getCreatedAt().toString());
                replyData.put("formattedDate", formatDate(reply.getCreatedAt()));
                replyData.put("parentId", reply.getParentComment() != null ? reply.getParentComment().getId() : null);

                if (reply.getRecipient() != null) {
                    replyData.put("hasRecipient", true);
                    replyData.put("recipientId", reply.getRecipient().getHashId());
                    replyData.put("recipientName", reply.getRecipient().getNameuse());
                }

                Map<String, Object> replyAuthorData = new HashMap<>();
                if (reply.getUser() != null) {
                    if (hasOnlySuperAccess) {
                        replyAuthorData.put("id", reply.getUser().getId());
                        replyAuthorData.put("username", reply.getUser().getUsername());
                        replyAuthorData.put("nameuse", reply.getUser().getNameuse());
                    } else {
                        replyAuthorData.put("id", 0);
                        replyAuthorData.put("username", "Пользователь");
                        replyAuthorData.put("nameuse", reply.getUser().getNameuse());
                    }
                    replyAuthorData.put("online", reply.getUser().isOnline());
                } else {
                    replyAuthorData.put("id", null);
                    replyAuthorData.put("username", "Удаленный пользователь");
                    replyAuthorData.put("nameuse", "Удаленный пользователь");
                    replyAuthorData.put("online", false);
                }
                replyData.put("author", replyAuthorData);
                replyData.put("isSuperUser", hasOnlySuperAccess);
                repliesData.add(replyData);
            }
        }
        commentData.put("replies", repliesData);
        return commentData;
    }

    /**
     * Добавляет новый личный комментарий к посту.
     * Поддерживает ответы на существующие комментарии (parentCommentId).
     * Проверяет rate limit и валидность текста.
     *
     * @param hashId           идентификатор поста
     * @param text             текст комментария
     * @param parentCommentId  ID родительского комментария (опционально)
     * @param authentication   данные аутентификации
     * @param request          HTTP-запрос (для получения IP)
     * @return ResponseEntity с результатом операции
     */
    @PostMapping("/postidviews/{hashId}/commentPersonal")
    @ResponseBody
    public ResponseEntity<?> addCommentPersonal(
            @PathVariable String hashId,
            @RequestParam String text,
            @RequestParam(required = false) Long parentCommentId,
            Authentication authentication,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Проверка авторизации
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Попытка добавления личного комментария без авторизации, IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Требуется авторизация"));
        }

        // Rate limit и валидация текста
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "personal")) {
            log.warn("Нарушение rate limit или слишком короткий комментарий, IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "ignored", "message", "Комментарий слишком короткий или слишком частые запросы"));
        }

        try {
            PostText post = postRepo.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Проверка доступности поста
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.warn("Попытка добавления комментария в недоступный пост, hashId: {}", hashId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "post_unavailable", "message", "Пост недоступен для просмотра", "redirect", "/"));
            }

            User user = (User) authentication.getPrincipal();

            // Создание комментария
            CommentPersonal commentPersonal = new CommentPersonal();
            commentPersonal.setText(text);
            commentPersonal.setPost(post);
            commentPersonal.setUser(user);

            // Привязка к родительскому комментарию
            if (parentCommentId != null && parentCommentId > 0) {
                CommentPersonal parentComment = commentRepoPersonal.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    commentPersonal.setParentComment(parentComment);
                }
            }

            // Определение получателя комментария
            if (user.getId().equals(post.getUser().getId())) {
                if (commentPersonal.getParentComment() != null) {
                    commentPersonal.setRecipient(commentPersonal.getParentComment().getUser());
                } else {
                    commentPersonal.setRecipient(null);
                }
            } else {
                commentPersonal.setRecipient(post.getUser());
            }

            commentRepoPersonal.save(commentPersonal);
            log.info("Добавлен новый личный комментарий, hashId: {}, parentCommentId: {}", hashId, parentCommentId);

            // Ответ в зависимости от типа запроса
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "Комментарий успешно добавлен"));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/postidviews/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.warn("Ошибка при добавлении личного комментария, hashId: {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Ошибка при добавлении комментария"));
        }
    }

    /**
     * Удаление личного комментария.
     * Обычный пользователь может удалить только свой комментарий.
     * Администратор может удалить любой комментарий.
     *
     * @param hashId               идентификатор поста
     * @param commentPersonalId    ID комментария
     * @param authentication       данные аутентификации
     * @param model                модель для передачи сообщений об ошибках
     * @return редирект на страницу поста
     */
    @PostMapping("/postidviews/{hashId}/commentPersonal/{commentPersonalId}/delete")
    public String deleteCommentPersonal(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalId,
            Authentication authentication,
            Model model) {

        try {
            User user = (User) authentication.getPrincipal();

            // Проверка прав администратора
            if (hasSuperAccess(authentication)) {
                commentRepoPersonal.deleteById(commentPersonalId);
                log.info("Удалён личный комментарий (администратор), commentId: {}, hashId: {}", commentPersonalId, hashId);
                return "redirect:/postidviews/" + hashId;
            }

            CommentPersonal comment = commentRepoPersonal.findById(commentPersonalId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

            // Проверка, что пользователь — автор комментария
            if (comment.getUser().getId().equals(user.getId())) {
                commentRepoPersonal.deleteById(commentPersonalId);
                log.info("Удалён личный комментарий (автор), commentId: {}, hashId: {}", commentPersonalId, hashId);
            } else {
                log.warn("Попытка удаления чужого комментария, hashId: {}, commentId: {}", hashId, commentPersonalId);
                model.addAttribute("error", "Вы можете удалять только свои комментарии");
                return "redirect:/postidviews/" + hashId;
            }

        } catch (Exception e) {
            log.warn("Ошибка при удалении личного комментария, hashId: {}, commentId: {}, errorType: {}", hashId, commentPersonalId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления");
        }
        return "redirect:/postidviews/" + hashId;
    }

    /**
     * Удаление ветки комментариев (комментарий + все ответы).
     * Доступно только для пользователей с правами администратора (super access).
     *
     * @param hashId               идентификатор поста
     * @param commentPersonalId    ID корневого комментария ветки
     * @param authentication       данные аутентификации
     * @param model                модель для передачи сообщений об ошибках
     * @return редирект на страницу поста
     */
    @PostMapping("/postidviews/{hashId}/commentPersonal/{commentPersonalId}/deleteBranch")
    public String deleteCommentBranch(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalId,
            Authentication authentication,
            Model model) {

        if (hasSuperAccess(authentication)) {
            commentRepoPersonal.findById(commentPersonalId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));
            deleteCommentWithReplies(commentPersonalId);
            log.info("Удалена ветка личных комментариев (администратор), rootCommentId: {}, hashId: {}", commentPersonalId, hashId);
            return "redirect:/postidviews/" + hashId;
        } else {
            log.warn("Попытка удаления ветки без прав администратора, hashId: {}, commentId: {}", hashId, commentPersonalId);
            model.addAttribute("error", "Доступ запрещен");
            return "redirect:/postidviews/" + hashId;
        }
    }

    /**
     * Рекурсивное удаление комментария и всех его ответов.
     *
     * @param commentId ID корневого комментария
     */
    private void deleteCommentWithReplies(Long commentId) {
        List<CommentPersonal> replies = commentRepoPersonal.findAllRepliesByParentId(commentId);
        for (CommentPersonal reply : replies) {
            deleteCommentWithReplies(reply.getId());
        }
        commentRepoPersonal.deleteById(commentId);
    }
}