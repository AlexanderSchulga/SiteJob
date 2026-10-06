package jobforsite.sitejob.module.product.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepository;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.product.model.Comment;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.CommentVacancy;
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
 * Контроллер для загрузки пагинированных публичных комментариев к постам.
 * Обеспечивает CRUD операции с публичными комментариями, включая древовидные ответы,
 * кэширование и rate limiting.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginationPublicCommentPost extends BasePostController {

    private final EnhancedRateLimitService enhancedRateLimitService;
    private final PostRepo postRepo;
    private final CommentRepository commentRepository;
    private final RateLimitService rateLimitService;

    /**
     * Добавляет новый публичный комментарий к посту.
     * Поддерживает ответы на существующие комментарии (parentCommentId).
     * При добавлении инвалидирует кэш комментариев и счётчиков.
     *
     * @param hashId           уникальный идентификатор поста
     * @param text             текст комментария
     * @param parentCommentId  ID родительского комментария (опционально)
     * @param authentication   данные аутентификации
     * @param request          HTTP-запрос (для получения IP)
     * @return ResponseEntity с результатом операции или редирект
     */
    @PostMapping("/postidviews/{hashId}/comment")
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
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Валидация комментария и проверка rate limit
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "public")) {
            log.warn("Нарушение rate limit при добавлении публичного комментария, IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "error", "message", "Комментарий слишком короткий или слишком частые запросы"));
        }

        try {
            // Проверка авторизации
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Попытка добавления публичного комментария без авторизации, IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Требуется авторизация"));
            }

            // Поиск поста
            PostText post = postRepo.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Проверка доступности поста
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.warn("Попытка добавить комментарий к недоступной товара {}, статус: блок={}, архив={}, модерация={}",
                        hashId, post.isBlocked(), post.isArchived(), post.isModeration());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            User user = (User) authentication.getPrincipal();

            // Создание комментария
            Comment comment = new Comment();
            comment.setText(text);
            comment.setPost(post);
            comment.setUser(user);

            // Привязка к родительскому комментарию (ответ)
            if (parentCommentId != null && parentCommentId > 0) {
                Comment parentComment = commentRepository.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    comment.setParentComment(parentComment);
                }
            }

            commentRepository.save(comment);
            enhancedRateLimitService.clearFailedAttempts(clientIp);
            log.info("Добавлен новый публичный комментарий, hashId: {}, parentCommentId: {}", hashId, parentCommentId);

            // грузим все комментарии вакансии
            List<Comment> allComments = commentRepository
                    .findAllByPostIdOrderByCreatedAtAsc(post.getId());

            // Конвертация для ответа
            Map<String, Object> commentData = convertCommentToMapWithReplies(comment, allComments, authentication);
            if (comment.getParentComment() != null) {
                commentData.put("parentId", comment.getParentComment().getId());
            }

            // Ответ в зависимости от типа запроса
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Комментарий успешно добавлен",
                        "comment", commentData
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/postidviews/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.warn("Ошибка при добавлении публичного комментария, hashId: {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Ошибка при добавлении комментария"));
        }
    }

    /**
     * Удаление ветки публичных комментариев (комментарий + все ответы).
     * Доступно только для пользователей с правами администратора (super access).
     * При удалении инвалидирует кэш комментариев.
     *
     * @param hashId         идентификатор поста
     * @param commentId      ID корневого комментария ветки
     * @param authentication данные аутентификации
     * @param model          модель для передачи сообщений об ошибках
     * @return редирект на страницу поста
     */
    @PostMapping("/postidviews/{hashId}/comment/{commentId}/deleteBranch")
    @CacheEvict(value = "publicComments", key = "#hashId + '-page-*'", allEntries = true)
    public String deleteCommentBranchPublic(
            @PathVariable String hashId,
            @PathVariable Long commentId,
            Authentication authentication,
            Model model) {

        try {
            if (hasSuperAccess(authentication)) {
                deleteCommentWithRepliesPublic(commentId);
                log.info("Удалена ветка публичных комментариев, rootCommentId: {}, hashId: {}", commentId, hashId);
                return "redirect:/postidviews/" + hashId;
            } else {
                log.warn("Попытка удаления ветки публичных комментариев без прав, hashId: {}, commentId: {}", hashId, commentId);
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/postidviews/" + hashId;
            }
        } catch (Exception e) {
            log.warn("Ошибка при удалении ветки публичных комментариев, hashId: {}, commentId: {}, errorType: {}", hashId, commentId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления");
            return "redirect:/postidviews/" + hashId;
        }
    }

    /**
     * Рекурсивное удаление комментария и всех его ответов.
     *
     * @param commentId ID корневого комментария
     */
    private void deleteCommentWithRepliesPublic(Long commentId) {
        List<Comment> replies = commentRepository.findAllRepliesByParentId(commentId);
        for (Comment reply : replies) {
            deleteCommentWithRepliesPublic(reply.getId());
        }
        commentRepository.deleteById(commentId);
    }

    /**
     * Загружает порцию публичных комментариев (без учёта иерархии, плоский список).
     * Используется для альтернативного режима отображения комментариев.
     *
     * @param hashId         идентификатор поста
     * @param page           номер страницы
     * @param size           размер страницы
     * @param authentication данные аутентификации
     * @return ResponseEntity со списком комментариев
     */
    @GetMapping("/postidviews/{hashId}/load-more-comments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreComments(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        try {
            PostText post = postRepo.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Перенаправление при недоступности поста
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.warn("Запрос комментариев к недоступному посту (load-more-comments), hashId: {}", hashId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }

            Long postId = post.getId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            //Slice по корням
            //Slice<Comment> commentsSlice = commentRepository.findSliceByPostId(postId, pageable);

            // 1. Грузим только корневые комментарии (для пагинации)
            Slice<Comment> rootSlice = commentRepository
                    .findSliceByPostIdAndParentCommentIsNull(postId, pageable);

            // 2. Собираем ID корней этой страницы
            List<Long> rootIds = rootSlice.getContent().stream()
                    .map(Comment::getId)
                    .toList();

            // 3. Грузим ВСЕ ответы для этих корней рекурсивно
            // Проще всего — загрузить все комментарии товаров и отфильтровать на бэке
            List<Comment> allComments = commentRepository
                    .findAllByPostIdOrderByCreatedAtAsc(postId);


            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (Comment root : rootSlice.getContent()) {
                Map<String, Object> commentData = convertCommentToMapWithReplies(
                        root, allComments, authentication);
                commentsData.add(commentData);
            }

//            // Конвертация комментариев
//            List<Map<String, Object>> commentsData = new ArrayList<>();
//            for (Comment comment : commentsSlice.getContent()) {
//                Map<String, Object> commentData = convertCommentToMap(comment, authentication);
//                commentsData.add(commentData);
//            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", rootSlice.hasNext());
            response.put("currentPage", page);

            log.debug("Загружено {} публичных комментариев для товаров {}, hasMore: {}", commentsData.size(), hashId, rootSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("Ошибка при загрузке публичных комментариев (плоский список), hashId: {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Ошибка при загрузке комментариев"));
        }
    }

    /**
     * Преобразует комментарий в Map для клиента (без вложенных ответов).
     *
     * @param comment        комментарий
     * @param authentication данные аутентификации
     * @return Map с данными комментария
     */
    private Map<String, Object> convertCommentToMap(Comment comment, Authentication authentication) {
        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("text", comment.getText());
        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));

        // Информация об авторе (с маскировкой для не-SUPER)
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
     * Удаление одного публичного комментария (без каскадного удаления ответов).
     * Доступно только для администраторов. При удалении инвалидирует кэш.
     *
     * @param hashId         идентификатор поста
     * @param commentId      ID комментария
     * @param authentication данные аутентификации
     * @param model          модель для передачи сообщений об ошибках
     * @return редирект на страницу поста
     */
    @PostMapping("/postidviews/{hashId}/comment/{commentId}/delete")
    @Caching(evict = {
            @CacheEvict(value = "publicComments", key = "#hashId + '-page-*'", allEntries = true),
            @CacheEvict(value = "commentCounts", key = "#hashId")
    })
    public String deleteComment(
            @PathVariable String hashId,
            @PathVariable Long commentId,
            Authentication authentication,
            Model model) {

        try {
            if (hasSuperAccess(authentication)) {
                commentRepository.deleteById(commentId);
                log.info("Удалён публичный комментарий, commentId: {}, hashId: {}", commentId, hashId);
                return "redirect:/postidviews/" + hashId;
            } else {
                log.warn("Попытка удаления публичного комментария без прав, hashId: {}, commentId: {}", hashId, commentId);
            }
        } catch (Exception e) {
            log.warn("Ошибка при удалении публичного комментария, hashId: {}, commentId: {}, errorType: {}", hashId, commentId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления");
        }
        return "redirect:/postidviews/" + hashId;
    }

    /**
     * Загружает порцию публичных комментариев с поддержкой иерархии (древовидная структура).
     * Результат кэшируется для уменьшения нагрузки на БД.
     *
     * @param hashId         идентификатор поста
     * @param page           номер страницы
     * @param size           размер страницы
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос (для rate limiting)
     * @return ResponseEntity с древовидной структурой комментариев
     */
    @GetMapping("/postidviews/{hashId}/load-more-public-comments")
    @ResponseBody
    @Cacheable(value = "publicComments", key = "#hashId + '-page-' + #page + '-size-' + #size")
    public ResponseEntity<Map<String, Object>> loadMorePublicComments(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Rate limiting
        if (!enhancedRateLimitService.tryConsumeComment(clientIp)) {
            log.warn("Превышен лимит запросов публичных комментариев для IP: {}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            PostText post = postRepo.findByHashId(hashId)
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
                log.warn("Попытка загрузить комментарии к недоступному посту {}, доступ запрещён", hashId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            Long postId = post.getId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Slice<Comment> commentsSlice = commentRepository.findSliceByPostIdAndParentCommentIsNull(postId, pageable);

            List<Comment> allComments = commentRepository
                    .findAllByPostIdOrderByCreatedAtAsc(postId);

            // Конвертация с вложенными ответами
            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (Comment comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertCommentToMapWithReplies(comment, allComments, authentication);
                commentsData.add(commentData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", commentsSlice.hasNext());
            response.put("currentPage", page);
            response.put("isSuperUser", hasOnlySuperAccess(authentication));

            log.debug("Загружено {} кэшируемых публичных комментариев для товара {}, hasMore: {}", commentsData.size(), hashId, commentsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("Ошибка при загрузке кэшируемых публичных комментариев для товара {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Получает общее количество публичных комментариев к посту.
     * Результат кэшируется.
     *
     * @param hashId идентификатор поста
     * @return ResponseEntity с количеством комментариев
     */
    @GetMapping("/postidviews/{hashId}/comment-count")
    @ResponseBody
    @Cacheable(value = "commentCounts", key = "#hashId")
    public ResponseEntity<Map<String, Object>> getCommentCount(@PathVariable String hashId) {
        try {
            PostText post = postRepo.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.warn("Запрос количества комментариев для недоступного поста, hashId: {}", hashId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }

            long count = commentRepository.countByPostId(post.getId());
            log.debug("Получено количество публичных комментариев, hashId: {}, count: {}", hashId, count);

            return ResponseEntity.ok(Map.of("status", "success", "count", count));

        } catch (Exception e) {
            log.warn("Ошибка при получении количества публичных комментариев, hashId: {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Ошибка при получении количества комментариев"));
        }
    }

    /**
     * Преобразует комментарий в Map с вложенными ответами (рекурсивно для первого уровня).
     *
     * @param comment        корневой комментарий
     * @param authentication данные аутентификации
     * @return Map с данными комментария и списком ответов
     */
    private Map<String, Object> convertCommentToMapWithReplies(
            Comment comment,
            List<Comment> allComments,
            Authentication authentication) {
        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("text", comment.getText());
        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));

        // Информация об авторе
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
        for (Comment candidate : allComments) {
            if (candidate.getParentComment() != null
                    && candidate.getParentComment().getId().equals(comment.getId())) {
                repliesData.add(convertCommentToMapWithReplies(
                        candidate, allComments, authentication));
            }
        }

//        // Получение и конвертация ответов на комментарий
//        List<Map<String, Object>> repliesData = new ArrayList<>();
//        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
//
//        for (Comment reply : replies) {
//            Map<String, Object> replyData = convertReplyToMap(reply, hasOnlySuperAccess);
//            replyData.put("parentId", comment.getId());
//            repliesData.add(replyData);
//        }

        commentData.put("replies", repliesData);
        return commentData;
    }

    /**
     * Преобразует ответ (вложенный комментарий) в Map.
     *
     * @param reply                комментарий-ответ
     * @param hasOnlySuperAccess   флаг наличия super-доступа
     * @return Map с данными ответа
     */
    private Map<String, Object> convertReplyToMap(Comment reply, boolean hasOnlySuperAccess) {
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

        // Информация об авторе ответа
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