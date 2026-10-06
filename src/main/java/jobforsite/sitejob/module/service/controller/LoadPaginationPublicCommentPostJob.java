package jobforsite.sitejob.module.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.service.model.CommentJob;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepositoryJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
 * Контроллер для пагинации и управления публичными комментариями к постам (Job).
 * Обрабатывает публичные комментарии, видимые всем пользователям.
 * Поддерживает кэширование для оптимизации загрузки.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginationPublicCommentPostJob extends BasePostController {

    private final RateLimitService rateLimitService;
    private final EnhancedRateLimitService enhancedRateLimitService;
    private final CommentRepositoryJob commentRepositoryJob;
    private final PostRepoJob postRepoJob;

    /**
     * Добавляет новый публичный комментарий к посту.
     * После добавления инвалидирует кэши комментариев и счетчиков.
     *
     * @param hashId           хэш-идентификатор поста
     * @param text             текст комментария
     * @param parentCommentId  ID родительского комментария (опционально, для ответов)
     * @param authentication   данные аутентификации
     * @param request          HTTP-запрос для получения IP и типа запроса
     * @return ResponseEntity с результатом операции и данными добавленного комментария
     */
    @PostMapping("/postidviewsjob/{hashId}/comment")
    @ResponseBody
    @Caching(evict = {
            @CacheEvict(value = "publicComments", key = "#hashId + '-page-*'", allEntries = true),
            @CacheEvict(value = "commentCounts", key = "#hashId")
    })
    public ResponseEntity<?> addComment(
            @PathVariable String hashId,
            @RequestParam String text,
            @RequestParam(required = false) Long parentCommentId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        String clientIp = getClientIp(request);
        log.debug("Запрос на добавление публичного комментария к услугам {}, IP: {}", hashId, clientIp);

        // Валидация комментария (длина и частота запросов)
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "public")) {
            log.warn("Публичный комментарий отклонен валидацией: IP={}, длина текста={}", clientIp, text != null ? text.length() : 0);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", "error",
                    "message", "Комментарий слишком короткий или слишком частые запросы"
            ));
        }

        try {
            // Проверка аутентификации
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Попытка добавления публичного комментария без аутентификации, IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "status", "error",
                        "message", "Требуется авторизация"
                ));
            }

            // Поиск поста
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

            // Создание нового комментария
            CommentJob comment = new CommentJob();
            comment.setText(text);
            comment.setPostjob(postj);
            comment.setUser(user);

            // Установка родительского комментария (если это ответ)
            if (parentCommentId != null && parentCommentId > 0) {
                CommentJob parentComment = commentRepositoryJob.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    comment.setParentComment(parentComment);
                    log.debug("Комментарий добавлен как ответ на commentId={}", parentCommentId);
                }
            }

            commentRepositoryJob.save(comment);
            enhancedRateLimitService.clearFailedAttempts(clientIp);
            log.info("Добавлен новый публичный комментарий: postHashId={}, parentCommentId={}", hashId, parentCommentId);

            // грузим все комментарии услуги
            List<CommentJob> allComments = commentRepositoryJob
                    .findAllByPostjobIdOrderByCreatedAtAsc(postj.getId());

            // Формирование данных для ответа
            Map<String, Object> commentData = convertCommentToMapWithRepliesJob(comment, allComments, authentication);
            if (comment.getParentComment() != null) {
                commentData.put("parentId", comment.getParentComment().getId());
            }

            // Обработка AJAX-запросов
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Комментарий успешно добавлен",
                        "comment", commentData
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/postidviewsjob/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении публичного комментария: hashId={}, parentCommentId={}, errorType={}", hashId, parentCommentId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Ошибка при добавлении комментария: " + e.getMessage()
            ));
        }
    }

    /**
     * Удаляет всю ветку публичных комментариев (комментарий и все ответы).
     * Доступно только SUPER/админу.
     *
     * @param hashId         хэш-идентификатор поста
     * @param commentId      ID корневого комментария ветки
     * @param authentication данные аутентификации
     * @param model          модель для передачи ошибки
     * @return строка редиректа на страницу поста
     */
    @PostMapping("/postidviewsjob/{hashId}/comment/{commentId}/deleteBranch")
    @CacheEvict(value = "publicComments", key = "#hashId + '-page-*'", allEntries = true)
    public String deleteCommentBranchPublicJob(
            @PathVariable String hashId,
            @PathVariable Long commentId,
            Authentication authentication,
            Model model
    ) {
        try {
            if (hasSuperAccess(authentication)) {
                deleteCommentWithRepliesPublicJob(commentId);
                log.info("SUPER/админ удалил ветку публичных комментариев: rootCommentId={}, postHashId={}", commentId, hashId);
                return "redirect:/postidviewsjob/" + hashId;
            } else {
                log.warn("Попытка удаления ветки публичных комментариев без прав SUPER/админа: commentId={}, postHashId={}", commentId, hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/postidviewsjob/" + hashId;
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении ветки публичных комментариев: commentId={}, postHashId={}, errorType={}", commentId, hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
            return "redirect:/postidviewsjob/" + hashId;
        }
    }

    /**
     * Рекурсивно удаляет публичный комментарий и все его дочерние ответы.
     *
     * @param commentId ID комментария, с которого начинается удаление
     */
    private void deleteCommentWithRepliesPublicJob(Long commentId) {
        List<CommentJob> replies = commentRepositoryJob.findAllRepliesByParentId(commentId);
        for (CommentJob reply : replies) {
            deleteCommentWithRepliesPublicJob(reply.getId());
        }
        commentRepositoryJob.deleteById(commentId);
        log.debug("Удален публичный комментарий и его ответы: commentId={}", commentId);
    }

    /**
     * Загружает публичные комментарии к посту с пагинацией (устаревший метод).
     * Не использует кэширование.
     *
     * @param hashId         хэш-идентификатор поста
     * @param page           номер страницы
     * @param size           размер страницы
     * @param authentication данные аутентификации (для прав доступа)
     * @return ResponseEntity с картой комментариев
     * @deprecated Используйте {@link #loadMorePublicCommentsJob} вместо этого метода
     */
    @Deprecated
    @GetMapping("/postidviewsjob/{hashId}/load-more-comments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreCommentsjob(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        log.debug("Вызван устаревший метод loadMoreCommentsjob для hashId={}", hashId);

        try {
            PostTextJob post = postRepoJob.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Редирект, если пост недоступен
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.debug("Пост недоступен в устаревшем методе: hashId={}", hashId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }

            Long postId = post.getId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // slice-по-корням
            //Slice<CommentJob> commentsSlice = commentRepositoryJob.findSliceByPostjobId(postId, pageable);


            // 1. Грузим только корневые комментарии (для пагинации)
            Slice<CommentJob> rootSlice = commentRepositoryJob
                    .findSliceByPostjobIdAndParentCommentIsNull(postId, pageable);

            // 2. Собираем ID корней этой страницы
            List<Long> rootIds = rootSlice.getContent().stream()
                    .map(CommentJob::getId)
                    .toList();

            // 3. Грузим ВСЕ ответы для этих корней рекурсивно
            // Проще всего — загрузить все комментарии услуги и отфильтровать на бэке
            List<CommentJob> allComments = commentRepositoryJob
                    .findAllByPostjobIdOrderByCreatedAtAsc(postId);

            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentJob root : rootSlice.getContent()) {
                Map<String, Object> commentData = convertCommentToMapWithRepliesJob(
                        root, allComments, authentication);
                commentsData.add(commentData);
            }


//            List<Map<String, Object>> commentsData = new ArrayList<>();
//            for (CommentJob comment : commentsSlice.getContent()) {
//                Map<String, Object> commentData = convertCommentToMapJob(comment, authentication);
//                commentsData.add(commentData);
//            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", rootSlice.hasNext());
            response.put("currentPage", page);

            log.debug("Устаревший метод загрузил {} комментариев для hashId={}", commentsData.size(), hashId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка в устаревшем методе loadMoreCommentsjob: hashId={}, errorType={}", hashId, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Преобразует публичный комментарий в Map для ответа (без вложенных ответов).
     * Используется в устаревшем методе.
     *
     * @param comment        объект комментария
     * @param authentication данные аутентификации (для проверки прав SUPER)
     * @return карта с данными комментария
     */
    private Map<String, Object> convertCommentToMapJob(CommentJob comment, Authentication authentication) {
        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("text", comment.getText());
        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));

        // Данные автора (скрыты для не-SUPER пользователей)
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
        } else {
            authorData.put("id", null);
            authorData.put("username", "Удаленный пользователь");
            authorData.put("nameuse", "Удаленный пользователь");
        }
        commentData.put("author", authorData);
        commentData.put("isSuperUser", hasOnlySuperAccess);

        return commentData;
    }

    /**
     * Удаляет один публичный комментарий.
     * Доступно только SUPER/админу.
     *
     * @param hashId         хэш-идентификатор поста
     * @param commentId      ID удаляемого комментария
     * @param authentication данные аутентификации
     * @param model          модель для ошибки
     * @return строка редиректа
     */
    @PostMapping("/postidviewsjob/{hashId}/comment/{commentId}/delete")
    @Caching(evict = {
            @CacheEvict(value = "publicComments", key = "#hashId + '-page-*'", allEntries = true),
            @CacheEvict(value = "commentCounts", key = "#hashId")
    })
    public String deleteComment(
            @PathVariable String hashId,
            @PathVariable Long commentId,
            Authentication authentication,
            Model model
    ) {
        try {
            if (hasSuperAccess(authentication)) {
                commentRepositoryJob.deleteById(commentId);
                log.info("SUPER/админ удалил публичный комментарий: commentId={}, postHashId={}", commentId, hashId);
                return "redirect:/postidviewsjob/" + hashId;
            } else {
                log.warn("Попытка удаления публичного комментария без прав SUPER/админа: commentId={}, postHashId={}", commentId, hashId);
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении публичного комментария: commentId={}, postHashId={}, errorType={}", commentId, hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/postidviewsjob/" + hashId;
    }

    /**
     * Загружает публичные комментарии к посту с пагинацией (кэшируемая версия).
     * Поддерживает кэширование для оптимизации производительности.
     * Включает rate limiting по IP.
     *
     * @param hashId         хэш-идентификатор поста
     * @param page           номер страницы (по умолчанию 0)
     * @param size           размер страницы (по умолчанию 3)
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос для получения IP
     * @return ResponseEntity с комментариями и мета-информацией
     */
    @GetMapping("/postidviewsjob/{hashId}/load-more-public-comments")
    @ResponseBody
    @Cacheable(value = "publicComments", key = "#hashId + '-page-' + #page + '-size-' + #size")
    public ResponseEntity<Map<String, Object>> loadMorePublicCommentsJob(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        log.debug("Запрос кэшируемых публичных комментариев к услугам {}, страница: {}, IP: {}", hashId, page, clientIp);

        // Rate limiting
        if (!enhancedRateLimitService.tryConsumeComment(clientIp)) {
            log.warn("Rate limit превышен для публичных комментариев: IP={}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            PostTextJob post = postRepoJob.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Проверка прав доступа к посту
            boolean hasAdminAccess = hasSuperAccess(authentication);
            boolean isOwner = false;

            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = (User) authentication.getPrincipal();
                isOwner = post.getUser() != null && post.getUser().getId().equals(currentUser.getId());
            }

            boolean isAvailable = hasAdminAccess || isOwner || (!post.isBlocked() && !post.isArchived() && !post.isModeration());

            if (!isAvailable) {
                log.warn("Попытка загрузки комментариев недоступного поста: hashId={}, заблокирован={}, архивирован={}, модерация={}",
                        hashId, post.isBlocked(), post.isArchived(), post.isModeration());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            Long postId = post.getId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            // Загружаем только корневые комментарии (без parentComment)
            Slice<CommentJob> commentsSlice = commentRepositoryJob.findSliceByPostjobIdAndParentCommentIsNull(postId, pageable);

            // Загружаем ВСЕ комментарии этой услуги для построения дерева
            List<CommentJob> allComments = commentRepositoryJob
                    .findAllByPostjobIdOrderByCreatedAtAsc(postId);

            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentJob comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertCommentToMapWithRepliesJob(comment, allComments, authentication);
                commentsData.add(commentData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", commentsSlice.hasNext());
            response.put("currentPage", page);
            response.put("isSuperUser", hasOnlySuperAccess(authentication));

            log.info("Загружены публичные комментарии: postHashId={}, страница={}, размер={}, получено={}",
                    hashId, page, size, commentsData.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке публичных комментариев: hashId={}, страница={}, errorType={}", hashId, page, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Возвращает количество комментариев к посту (кэшируемое).
     *
     * @param hashId хэш-идентификатор поста
     * @return ResponseEntity с количеством комментариев
     */
    @GetMapping("/postidviewsjob/{hashId}/comment-count")
    @ResponseBody
    @Cacheable(value = "commentCounts", key = "#hashId")
    public ResponseEntity<Map<String, Object>> getCommentCount(@PathVariable String hashId) {
        try {
            PostTextJob post = postRepoJob.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Редирект, если пост недоступен
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.debug("Запрос количества комментариев для недоступного поста: hashId={}", hashId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }

            long count = commentRepositoryJob.countByPostjobId(post.getId());
            log.debug("Количество публичных комментариев для hashId={}: {}", hashId, count);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", count
            ));
        } catch (Exception e) {
            log.error("Ошибка при подсчете публичных комментариев: hashId={}, errorType={}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Преобразует публичный комментарий в Map с вложенными ответами (рекурсивно).
     *
     * @param comment        объект комментария
     * @param authentication данные аутентификации (для проверки прав SUPER)
     * @return карта с данными комментария и его ответами
     */
    private Map<String, Object> convertCommentToMapWithRepliesJob(
            CommentJob comment,
            List<CommentJob> allComments,
            Authentication authentication) {

        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("text", comment.getText());
        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));

        // Данные автора
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

        if (comment.getParentComment() != null) {
            commentData.put("parentId", comment.getParentComment().getId());
            if (comment.getParentComment().getUser() != null) {
                commentData.put("recipientName", comment.getParentComment().getUser().getNameuse());
                commentData.put("recipientId", comment.getParentComment().getUser().getId());
            } else {
                commentData.put("recipientName", "Удаленный пользователь");
                commentData.put("recipientId", null);
            }
        } else {
            commentData.put("parentId", null);
            commentData.put("recipientName", null);
            commentData.put("recipientId", null);
        }

        commentData.put("author", authorData);
        commentData.put("isSuperUser", hasOnlySuperAccess);

        // рекурсивно собираем детей из in-memory списка
        List<Map<String, Object>> repliesData = new ArrayList<>();
        for (CommentJob candidate : allComments) {
            if (candidate.getParentComment() != null
                    && candidate.getParentComment().getId().equals(comment.getId())) {
                repliesData.add(convertCommentToMapWithRepliesJob(
                        candidate, allComments, authentication));
            }
        }

//        // Загрузка и добавление ответов
//        List<Map<String, Object>> repliesData = new ArrayList<>();
//        List<CommentJob> replies = commentRepositoryJob.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
//
//        for (CommentJob reply : replies) {
//            Map<String, Object> replyData = convertReplyToMapJob(reply, hasOnlySuperAccess);
//            replyData.put("parentId", comment.getId());
//            repliesData.add(replyData);
//        }

        commentData.put("replies", repliesData);
        return commentData;
    }

    /**
     * Преобразует ответ (reply) на публичный комментарий в Map.
     *
     * @param reply                объект ответа
     * @param hasOnlySuperAccess   флаг, имеет ли пользователь права SUPER
     * @return карта с данными ответа
     */
    private Map<String, Object> convertReplyToMapJob(CommentJob reply, boolean hasOnlySuperAccess) {
        Map<String, Object> replyData = new HashMap<>();
        replyData.put("id", reply.getId());
        replyData.put("text", reply.getText());
        replyData.put("createdAt", reply.getCreatedAt().toString());
        replyData.put("formattedDate", formatDate(reply.getCreatedAt()));


        // Информация о родительском комментарии (кому адресован ответ)
        if (reply.getParentComment() != null) {
            replyData.put("parentId", reply.getParentComment().getId());
            if (reply.getParentComment().getUser() != null) {
                replyData.put("recipientName", reply.getParentComment().getUser().getNameuse());
                replyData.put("recipientId", reply.getParentComment().getUser().getId());
            } else {
                replyData.put("recipientName", "Удаленный пользователь");
                replyData.put("recipientId", null);
            }
        } else {
            //replyData.put("parentId", null);
            replyData.put("recipientName", null);
            replyData.put("recipientId", null);
        }

        // Данные автора ответа
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

        return replyData;
    }
}