package jobforsite.sitejob.module.vacancy.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
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
 * Контроллер для загрузки и управления пагинацией личных комментариев к вакансиям.
 * Обрабатывает запросы на получение, добавление и удаление комментариев.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LoadPaginationCommentPostVac extends BasePostController {

    private final PostVacancyRepository postVacancyRepository;
    private final CommentRepoVacPersonal commentRepoVacPersonal;

    private final RateLimitService rateLimitService;
    private final EnhancedRateLimitService enhancedRateLimitService;

    /**
     * Загружает следующую страницу личных комментариев к вакансии с пагинацией.
     * Поддерживает разные уровни доступа: SUPER/ADMIN видят всё, владелец поста — все комментарии,
     * обычные пользователи — только свои и ответы на них.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param page           номер страницы (по умолчанию 0)
     * @param size           размер страницы (по умолчанию 3)
     * @param authentication объект аутентификации
     * @return ResponseEntity с картой, содержащей список комментариев, флаг hasMore и текущую страницу
     */
    @GetMapping("/postidviewsvac/{hashId}/load-more-personal-comments-vacancy")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMorePersonalCommentsVac(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            Authentication authentication) {

        // Логируем начало обработки запроса пагинации (без личных данных)
        log.debug("Запрос на загрузку комментариев к вакансии с hashId: {}, страница: {}, размер: {}", hashId, page, size);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Пользователь не аутентифицирован, возвращаем пустой список комментариев");
            return ResponseEntity.ok(Map.of(
                    "comments", new ArrayList<>(),
                    "hasMore", false,
                    "currentPage", 0
            ));
        }

        try {
            // Поиск вакансии по hashId
            Vacancy post = postVacancyRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Пост не найден"));

            User currentUser = (User) authentication.getPrincipal();
            boolean hasAdminAccess = hasSuperAccess(authentication);
            boolean isOwner = currentUser.getId().equals(post.getUser().getId());

            // Проверка доступности поста для не-администратора и не-владельца
            if (!hasAdminAccess && !isOwner && (post.isBlocked() || post.isArchived() || post.isModeration())) {
                log.warn("Попытка доступа к недоступному посту. hashId: {}, заблокирован: {}, архивирован: {}, на модерации: {}",
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

            // Настройка пагинации (сортировка по убыванию даты создания)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Slice<CommentPersonalVac> commentsSlice;

            // Разграничение доступа к комментариям в зависимости от роли пользователя
            if (hasSuperAccess) {
                // SUPER и ADMIN видят ВСЕ личные комментарии
                commentsSlice = commentRepoVacPersonal.findSliceByVacancyIdAndParentCommentIsNull(postId, pageable);
                log.debug("SUPER/ADMIN запросил все комментарии к вакансии ID: {}", postId);
            } else if (currentUser.getId().equals(post.getUser().getId())) {
                // Автор поста видит ВСЕ личные комментарии к своему посту
                commentsSlice = commentRepoVacPersonal.findSliceByVacancyIdAndParentCommentIsNull(postId, pageable);
                log.debug("Владелец поста запросил все комментарии к своей вакансии ID: {}", postId);
            } else {
                // Обычные пользователи видят ТОЛЬКО свои комментарии и ответы на них
                commentsSlice = commentRepoVacPersonal.findSliceVisibleCommentsForUser(postId, currentUser.getId(), pageable);
                log.debug("Обычный пользователь запросил только свои комментарии к вакансии ID: {}", postId);
            }

            // Конвертация комментариев в DTO для ответа
            List<Map<String, Object>> commentsData = new ArrayList<>();
            for (CommentPersonalVac comment : commentsSlice.getContent()) {
                Map<String, Object> commentData = convertPersonalCommentToMapVac(comment, currentUser, authentication);
                if (commentData != null) {
                    commentsData.add(commentData);
                }
            }

            // Формирование успешного ответа
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Комментарии успешно загружены");
            response.put("comments", commentsData);
            response.put("hasMore", commentsSlice.hasNext());
            response.put("currentPage", page);
            response.put("isSuperUser", hasOnlySuperAccess);

            log.info("Успешно загружено {} комментариев, hasMore: {}", commentsData.size(), commentsSlice.hasNext());
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
     * Преобразует объект CommentPersonalVac в Map для отправки на клиент.
     * Рекурсивно собирает все ответы (replies) на любой уровень вложенности.
     * Учитывает права доступа: SUPER/ADMIN видят реальные имена, остальные — обезличенные данные.
     *
     * @param comment        комментарий для конвертации
     * @param currentUser    текущий аутентифицированный пользователь
     * @param authentication объект аутентификации
     * @return Map с данными комментария или null, если пользователь не должен его видеть
     */
    private Map<String, Object> convertPersonalCommentToMapVac(CommentPersonalVac comment, User currentUser, Authentication authentication) {

        boolean hasOnlySuperAccess = hasOnlySuperAccess(authentication);
        boolean hasSuperAccess = hasSuperAccess(authentication);

        // Определение, может ли пользователь видеть данный комментарий
        boolean canSeeComment = hasSuperAccess ||
                currentUser.getId().equals(comment.getVacancy().getUser().getId()) ||
                currentUser.getId().equals(comment.getUser().getId()) ||
                (comment.getRecipient() != null && currentUser.getId().equals(comment.getRecipient().getId()));

        if (!canSeeComment) {
            log.debug("Пользователь не имеет прав на просмотр комментария ID: {}", comment.getId());
            return null;
        }

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("id", comment.getId());

        // Отправляем текст как есть (UTF-8)
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

        commentData.put("postId", comment.getVacancy().getId());
        commentData.put("postOwnerId", comment.getVacancy().getUser().getHashId());

        // Информация об авторе (с маскировкой для не-SUPER пользователей)
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
        commentData.put("isPostOwner", currentUser.getId().equals(comment.getVacancy().getUser().getId()));

        // Рекурсивная загрузка ответов на комментарий (replies)
        List<Map<String, Object>> repliesData = new ArrayList<>();
        List<CommentPersonalVac> replies = commentRepoVacPersonal.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());

        for (CommentPersonalVac reply : replies) {
            Map<String, Object> replyData = convertPersonalCommentToMapVac(reply, currentUser, authentication);
            if (replyData != null) {
                repliesData.add(replyData);
            }
        }
        commentData.put("replies", repliesData);

        return commentData;
    }

    /**
     * Добавляет новый личный комментарий к вакансии.
     * Выполняет валидацию текста и проверку rate limit.
     * Автоматически устанавливает получателя (recipient) в зависимости от того, кто оставляет комментарий.
     *
     * @param hashId            хеш-идентификатор вакансии
     * @param text              текст комментария
     * @param parentCommentId   ID родительского комментария (для ответов)
     * @param authentication    объект аутентификации
     * @param request           HTTP-запрос для получения IP-адреса
     * @return ResponseEntity с результатом операции
     */
    @PostMapping("/postidviewsvac/{hashId}/commentPersonalVac")
    @ResponseBody
    public ResponseEntity<?> addCommentPersonalVac(
            @PathVariable String hashId,
            @RequestParam String text,
            @RequestParam(required = false) Long parentCommentId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        String clientIp = getClientIp(request);
        log.debug("Запрос на добавление комментария к вакансии {} с IP: {}", hashId, clientIp);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Попытка добавления комментария без аутентификации с IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "error",
                    "message", "Требуется авторизация"
            ));
        }

        // Валидация комментария и проверка лимитов (rate limiting)
        if (!validateComment(text, clientIp, enhancedRateLimitService, rateLimitService, "personal")) {
            log.warn("Комментарий отклонён: слишком короткий текст или превышен лимит запросов. IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "status", "ignored",
                    "message", "Комментарий слишком короткий или слишком частые запросы"
            ));
        }

        try {
            Vacancy vacancy = postVacancyRepository.findByHashId(hashId)
                    .orElseThrow(() -> new RuntimeException("Вакансия не найдена"));

            // Проверка доступности вакансии
            if (vacancy.isBlocked() || vacancy.isArchived() || vacancy.isModeration()) {
                log.warn("Попытка добавить комментарий к недоступной вакансии. hashId: {}, заблокирована: {}, архивирована: {}, на модерации: {}",
                        hashId, vacancy.isBlocked(), vacancy.isArchived(), vacancy.isModeration());
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "post_unavailable");
                errorResponse.put("message", "Пост недоступен для просмотра");
                errorResponse.put("redirect", "/");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            User user = (User) authentication.getPrincipal();

            // Создание и заполнение нового комментария
            CommentPersonalVac commentPersonalVac = new CommentPersonalVac();
            commentPersonalVac.setText(text);
            commentPersonalVac.setVacancy(vacancy);
            commentPersonalVac.setUser(user);

            if (parentCommentId != null && parentCommentId > 0) {
                CommentPersonalVac parentComment = commentRepoVacPersonal.findById(parentCommentId).orElse(null);
                if (parentComment != null) {
                    commentPersonalVac.setParentComment(parentComment);
                }
            }

            // Установка получателя комментария (recipient)
            if (commentPersonalVac.getParentComment() != null) {
                // Ответ в ветке — получатель = автор родительского комментария
                commentPersonalVac.setRecipient(commentPersonalVac.getParentComment().getUser());
            } else {
                // Корневой комментарий — получатель = владелец поста
                // (если сам владелец пишет корневой комментарий — recipient = null)
                if (user.getId().equals(vacancy.getUser().getId())) {
                    commentPersonalVac.setRecipient(null);
                } else {
                    commentPersonalVac.setRecipient(vacancy.getUser());
                }
            }

            commentRepoVacPersonal.save(commentPersonalVac);
            log.info("Комментарий успешно добавлен к вакансии с hashId: {}", hashId);

            // Ответ в зависимости от типа запроса (AJAX или обычный)
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Комментарий успешно добавлен"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/postidviewsvac/" + hashId))
                        .build();
            }

        } catch (Exception e) {
            log.error("Ошибка при добавлении комментария к вакансии {}, errorType: {}", hashId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Ошибка при добавлении комментария: " + e.getMessage()
            ));
        }
    }

    /**
     * Удаляет один комментарий.
     * SUPER/ADMIN могут удалить любой комментарий, обычный пользователь — только свой.
     *
     * @param hashId                  хеш-идентификатор вакансии
     * @param commentPersonalVacId    ID удаляемого комментария
     * @param authentication          объект аутентификации
     * @param model                   модель для передачи сообщений об ошибках
     * @return строка перенаправления (redirect)
     */
    @PostMapping("/postidviewsvac/{hashId}/commentPersonalVac/{commentPersonalVacId}/delete")
    public String deleteCommentPersonal(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalVacId,
            Authentication authentication,
            Model model
    ) {
        log.debug("Запрос на удаление комментария ID: {} у вакансии {}", commentPersonalVacId, hashId);

        try {
            User user = (User) authentication.getPrincipal();

            // SUPER/ADMIN могут удалять любые комментарии
            if (hasSuperAccess(authentication)) {
                commentRepoVacPersonal.deleteById(commentPersonalVacId);
                log.info("Комментарий ID: {} удалён SUPER/ADMIN", commentPersonalVacId);
                return "redirect:/postidviewsvac/" + hashId;
            }

            CommentPersonalVac comment = commentRepoVacPersonal.findById(commentPersonalVacId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

            // Обычный пользователь удаляет только свой комментарий
            if (comment.getUser().getId().equals(user.getId())) {
                commentRepoVacPersonal.deleteById(commentPersonalVacId);
                log.info("Комментарий ID: {} удалён автором", commentPersonalVacId);
            } else {
                log.warn("Попытка удалить чужой комментарий ID: {}", commentPersonalVacId);
                model.addAttribute("error", "Вы можете удалять только свои комментарии");
                return "redirect:/postidviewsvac/" + hashId;
            }

        } catch (Exception e) {
            log.error("Ошибка при удалении комментария ID: {} у вакансии {}, errorType: {}", commentPersonalVacId, hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/postidviewsvac/" + hashId;
    }

    /**
     * Удаляет ветку комментариев (комментарий и все ответы на него).
     * Доступно только SUPER/ADMIN.
     *
     * @param hashId                  хеш-идентификатор вакансии
     * @param commentPersonalVacId    ID корневого комментария ветки
     * @param authentication          объект аутентификации
     * @param model                   модель для передачи сообщений об ошибках
     * @return строка перенаправления (redirect)
     */
    @PostMapping("/postidviewsvac/{hashId}/commentPersonalVac/{commentPersonalVacId}/deleteBranch")
    public String deleteCommentBranch(
            @PathVariable String hashId,
            @PathVariable Long commentPersonalVacId,
            Authentication authentication,
            Model model
    ) {
        log.info("Запрос на удаление ветки комментариев с корнем ID: {} у вакансии {}", commentPersonalVacId, hashId);

        if (hasSuperAccess(authentication)) {
            commentRepoVacPersonal.findById(commentPersonalVacId)
                    .orElseThrow(() -> new RuntimeException("Комментарий не найден"));
            deleteCommentWithReplies(commentPersonalVacId);
            log.info("Ветка комментариев с корнем ID: {} удалена SUPER/ADMIN", commentPersonalVacId);
            return "redirect:/postidviewsvac/" + hashId;
        } else {
            log.warn("Попытка удалить ветку комментариев без прав SUPER/ADMIN. Корневой ID: {}", commentPersonalVacId);
            model.addAttribute("error", "Доступ запрещен");
            return "redirect:/postidviewsvac/" + hashId;
        }
    }

    /**
     * Рекурсивно удаляет комментарий и все его ответы (дочерние комментарии).
     *
     * @param commentId ID комментария, с которого начинается удаление ветки
     */
    private void deleteCommentWithReplies(Long commentId) {
        List<CommentPersonalVac> replies = commentRepoVacPersonal.findAllRepliesByParentId(commentId);
        for (CommentPersonalVac reply : replies) {
            deleteCommentWithReplies(reply.getId());
        }
        commentRepoVacPersonal.deleteById(commentId);
        log.debug("Удалён комментарий ID: {} вместе с его веткой", commentId);
    }
}