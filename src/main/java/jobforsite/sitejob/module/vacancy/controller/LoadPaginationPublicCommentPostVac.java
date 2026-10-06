package jobforsite.sitejob.module.vacancy.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.CommentVacancy;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepositoryVac;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
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
 * Контроллер для загрузки и управления публичными комментариями к вакансиям с пагинацией.
 * Обрабатывает добавление, удаление, загрузку комментариев и подсчёт их количества.
 * Поддерживает кэширование для оптимизации производительности.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginationPublicCommentPostVac extends BasePostController {

    private final PostVacancyRepository postVacancyRepository;
    private final CommentRepositoryVac commentRepositoryVac;
    private final RateLimitService rateLimitService;
    private final EnhancedRateLimitService enhancedRateLimitService;

    /**
     * Добавляет новый публичный комментарий к вакансии.
     * Выполняет валидацию текста и проверку rate limit.
     * При успешном добавлении очищает кэш комментариев.
     *
     * @param hashId            хеш-идентификатор вакансии
     * @param text              текст комментария
     * @param parentCommentId   ID родительского комментария (для ответов, опционально)
     * @param authentication    объект аутентификации
     * @param request           HTTP-запрос для получения IP
     * @return ResponseEntity с результатом операции
     */
    @PostMapping("/postidviewsvac/{hashId}/comment")
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
        log.debug("Запрос на добавление публичного комментария к вакансии {}, IP: {}", hashId, clientIp);

        // Валидация комментария и проверка лимитов
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "public")) {
            log.warn("Публичный комментарий отклонён: слишком короткий текст или превышен лимит. IP: {}", clientIp);
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

            // Поиск вакансии
            Vacancy vacancy = postVacancyRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

            // Проверка доступности вакансии
            if (vacancy.isBlocked() || vacancy.isArchived() || vacancy.isModeration()) {
                log.warn("Попытка добавить комментарий к недоступной вакансии {}, статус: блок={}, архив={}, модерация={}",
                        hashId, vacancy.isBlocked(), vacancy.isArchived(), vacancy.isModeration());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            User user = (User) authentication.getPrincipal();

            // Создание нового комментария
            CommentVacancy comment = new CommentVacancy();
            comment.setText(text);
            comment.setVacancy(vacancy);
            comment.setUser(user);

            // Установка родительского комментария (если указан)
            if (parentCommentId != null && parentCommentId > 0) {
                CommentVacancy parentComment = commentRepositoryVac.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    comment.setParentComment(parentComment);
                }
            }

            commentRepositoryVac.save(comment);
            enhancedRateLimitService.clearFailedAttempts(clientIp);
            log.info("Публичный комментарий успешно добавлен к вакансии {}", hashId);

            // грузим все комментарии вакансии
            List<CommentVacancy> allComments = commentRepositoryVac
                    .findAllByVacancyIdOrderByCreatedAtAsc(vacancy.getId());

            // Конвертация комментария для ответа
            Map<String, Object> commentData = convertCommentToMapWithRepliesVac(comment, allComments, authentication);
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
                        .location(URI.create("/postidviewsvac/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении публичного комментария к вакансии {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Ошибка при добавлении комментария: " + e.getMessage()
            ));
        }
    }

    /**
     * Удаляет ветку публичных комментариев (комментарий и все ответы на него).
     * Доступно только пользователям с правами SUPER/ADMIN.
     * После удаления очищает кэш.
     *
     * @param hashId          хеш-идентификатор вакансии
     * @param commentId       ID корневого комментария
     * @param authentication  объект аутентификации
     * @param model           модель для сообщений об ошибках
     * @return строка перенаправления
     */
    @PostMapping("/postidviewsvac/{hashId}/comment/{commentId}/deleteBranch")
    @CacheEvict(value = "publicComments", key = "#hashId + '-page-*'", allEntries = true)
    public String deleteCommentBranchPublicVac(
            @PathVariable String hashId,
            @PathVariable Long commentId,
            Authentication authentication,
            Model model
    ) {
        log.debug("Запрос на удаление ветки публичных комментариев, корневой ID: {}, вакансия: {}", commentId, hashId);

        try {
            if (hasSuperAccess(authentication)) {
                deleteCommentWithRepliesPublicVac(commentId);
                log.info("Ветка публичных комментариев с корнем {} удалена", commentId);
                return "redirect:/postidviewsvac/" + hashId;
            } else {
                log.warn("Попытка удаления ветки публичных комментариев без прав SUPER/ADMIN, корневой ID: {}", commentId);
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/postidviewsvac/" + hashId;
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении ветки публичных комментариев, корневой ID: {}, errorType: {}", commentId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
            return "redirect:/postidviewsvac/" + hashId;
        }
    }

    /**
     * Рекурсивно удаляет публичный комментарий и все его дочерние комментарии.
     *
     * @param commentId ID удаляемого комментария
     */
    private void deleteCommentWithRepliesPublicVac(Long commentId) {
        List<CommentVacancy> replies = commentRepositoryVac.findAllRepliesByParentId(commentId);
        for (CommentVacancy reply : replies) {
            deleteCommentWithRepliesPublicVac(reply.getId());
        }
        commentRepositoryVac.deleteById(commentId);
        log.debug("Удалён публичный комментарий ID: {} с веткой", commentId);
    }

    /**
     * Загружает следующую страницу публичных комментариев (корневых, без ответов).
     * Используется для бесконечной ленты комментариев.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param page           номер страницы
     * @param size           размер страницы
     * @param authentication объект аутентификации
     * @return ResponseEntity со списком комментариев
     */
    @GetMapping("/postidviewsvac/{hashId}/load-more-comments")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreCommentsVac(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        log.debug("Запрос публичных комментариев к вакансии {}, страница: {}, размер: {}", hashId, page, size);

        try {
            Vacancy post = postVacancyRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            // Проверка доступности поста
            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.warn("Попытка загрузить комментарии к недоступному посту {}", hashId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }

            Long postId = post.getId();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            // slice-по-корням
            // Slice<CommentVacancy> commentsSlice = commentRepositoryVac.findSliceByVacancyId(postId, pageable);


            // 1. Грузим только корневые комментарии (для пагинации)
            Slice<CommentVacancy> rootSlice = commentRepositoryVac
                    .findSliceByVacancyIdAndParentCommentIsNull(postId, pageable);

            // 2. Собираем ID корней этой страницы
            List<Long> rootIds = rootSlice.getContent().stream()
                    .map(CommentVacancy::getId)
                    .toList();

        // 3. Грузим ВСЕ ответы для этих корней рекурсивно
            // Проще всего — загрузить все комментарии вакансии и отфильтровать на бэке
            List<CommentVacancy> allComments = commentRepositoryVac
                    .findAllByVacancyIdOrderByCreatedAtAsc(postId);

            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentVacancy root : rootSlice.getContent()) {
                Map<String, Object> commentData = convertCommentToMapWithRepliesVac(
                        root, allComments, authentication);
                commentsData.add(commentData);
            }



//            List<Map<String, Object>> commentsData = new ArrayList<>();
//            for (CommentVacancy comment : commentsSlice.getContent()) {
//                Map<String, Object> commentData = convertCommentToMapVac(comment, authentication);
//                commentsData.add(commentData);
//            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", rootSlice.hasNext());
            response.put("currentPage", page);

            log.debug("Загружено {} публичных комментариев для вакансии {}, hasMore: {}", commentsData.size(), hashId, rootSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("Ошибка при загрузке публичных комментариев для вакансии {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Преобразует публичный комментарий в Map для отправки на клиент (базовая версия, без ответов).
     * Маскирует данные автора для обычных пользователей.
     *
     * @param comment        комментарий
     * @param authentication объект аутентификации
     * @return Map с данными комментария
     */
    private Map<String, Object> convertCommentToMapVac(CommentVacancy comment, Authentication authentication) {
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
     * Удаляет один публичный комментарий (без дочерних).
     * Доступно только пользователям с правами SUPER/ADMIN.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param commentId      ID комментария
     * @param authentication объект аутентификации
     * @param model          модель для сообщений об ошибках
     * @return строка перенаправления
     */
    @PostMapping("/postidviewsvac/{hashId}/comment/{commentId}/delete")
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
        log.debug("Запрос на удаление публичного комментария ID: {}, вакансия: {}", commentId, hashId);

        try {
            if (hasSuperAccess(authentication)) {
                commentRepositoryVac.deleteById(commentId);
                log.info("Публичный комментарий ID: {} удалён", commentId);
                return "redirect:/postidviewsvac/" + hashId;
            } else {
                log.warn("Попытка удаления публичного комментария ID: {} без прав SUPER/ADMIN", commentId);
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении публичного комментария ID: {}, errorType: {}", commentId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/postidviewsvac/" + hashId;
    }

    /**
     * Загружает следующую страницу публичных комментариев с поддержкой кэширования.
     * Возвращает только корневые комментарии с их ответами (replies).
     * Использует rate limit для защиты от чрезмерных запросов.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param page           номер страницы
     * @param size           размер страницы
     * @param authentication объект аутентификации
     * @param request        HTTP-запрос для получения IP
     * @return ResponseEntity со списком комментариев и их ответами
     */
    @GetMapping("/postidviewsvac/{hashId}/load-more-public-comments")
    @ResponseBody
    @Cacheable(value = "publicComments", key = "#hashId + '-page-' + #page + '-size-' + #size")
    public ResponseEntity<Map<String, Object>> loadMorePublicCommentsVac(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        log.debug("Запрос кэшируемых публичных комментариев к вакансии {}, страница: {}, IP: {}", hashId, page, clientIp);

        // Rate limiting
        if (!enhancedRateLimitService.tryConsumeComment(clientIp)) {
            log.warn("Превышен лимит запросов публичных комментариев для IP: {}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            Vacancy post = postVacancyRepository.findByHashId(hashId)
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
            Slice<CommentVacancy> commentsSlice = commentRepositoryVac.findSliceByVacancyIdAndParentCommentIsNull(postId, pageable);

            // Загружаем ВСЕ комментарии этой вакансии для построения дерева
            List<CommentVacancy> allComments = commentRepositoryVac
                    .findAllByVacancyIdOrderByCreatedAtAsc(postId);

            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentVacancy comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertCommentToMapWithRepliesVac(comment, allComments, authentication);
                commentsData.add(commentData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", commentsSlice.hasNext());
            response.put("currentPage", page);
            response.put("isSuperUser", hasOnlySuperAccess(authentication));

            log.debug("Загружено {} кэшируемых публичных комментариев для вакансии {}, hasMore: {}", commentsData.size(), hashId, commentsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке кэшируемых публичных комментариев для вакансии {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Ошибка при загрузке комментариев: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Возвращает общее количество публичных комментариев к вакансии.
     * Результат кэшируется для оптимизации.
     *
     * @param hashId хеш-идентификатор вакансии
     * @return ResponseEntity с количеством комментариев
     */
    @GetMapping("/postidviewsvac/{hashId}/comment-count")
    @ResponseBody
    @Cacheable(value = "commentCounts", key = "#hashId")
    public ResponseEntity<Map<String, Object>> getCommentCount(@PathVariable String hashId) {
        log.debug("Запрос количества публичных комментариев для вакансии {}", hashId);

        try {
            Vacancy post = postVacancyRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            if (post.isBlocked() || post.isArchived() || post.isModeration()) {
                log.warn("Запрос количества комментариев к недоступному посту {}", hashId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/"))
                        .build();
            }

            long count = commentRepositoryVac.countByVacancyId(post.getId());
            log.debug("Количество публичных комментариев для вакансии {}: {}", hashId, count);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", count
            ));
        } catch (Exception e) {
            log.error("Ошибка при подсчёте публичных комментариев для вакансии {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Преобразует публичный комментарий в Map с его ответами (replies).
     * Рекурсивно добавляет все дочерние комментарии.
     *
     * @param comment        корневой комментарий
     * @param authentication объект аутентификации
     * @return Map с данными комментария и его ответами
     */
    private Map<String, Object> convertCommentToMapWithRepliesVac(
            CommentVacancy comment,
            List<CommentVacancy> allComments,
            Authentication authentication) {
        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());
        commentData.put("text", comment.getText());
        commentData.put("createdAt", comment.getCreatedAt().toString());
        commentData.put("formattedDate", formatDate(comment.getCreatedAt()));

        // Информация об авторе (с маскировкой)
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
        for (CommentVacancy candidate : allComments) {
            if (candidate.getParentComment() != null
                    && candidate.getParentComment().getId().equals(comment.getId())) {
                repliesData.add(convertCommentToMapWithRepliesVac(
                        candidate, allComments, authentication));
            }
        }

//        // Загрузка ответов на комментарий
//        List<Map<String, Object>> repliesData = new ArrayList<>();
//        List<CommentVacancy> replies = commentRepositoryVac.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
//
//        for (CommentVacancy reply : replies) {
//            Map<String, Object> replyData = convertReplyToMapVac(reply, hasOnlySuperAccess);
//            //replyData.put("parentId", comment.getId());
//            repliesData.add(replyData);
//        }

        commentData.put("replies", repliesData);

        return commentData;
    }

    /**
     * Преобразует ответ (дочерний комментарий) в Map для отправки на клиент.
     *
     * @param reply                дочерний комментарий
     * @param hasOnlySuperAccess   флаг, имеет ли пользователь права SUPER
     * @return Map с данными ответа
     */
    private Map<String, Object> convertReplyToMapVac(CommentVacancy reply, boolean hasOnlySuperAccess) {
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

        // Информация об авторе ответа (с маскировкой)
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