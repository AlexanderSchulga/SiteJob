package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.shared.dto.SliceResponseDTO;
import jobforsite.sitejob.module.shared.model.Reviews;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.repository.ReviewsRepo;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedReviewsRateLimitService;
import jobforsite.sitejob.module.users.service.UserService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Контроллер для управления отзывами пользователей.
 * Обеспечивает CRUD операции над отзывами, поиск пользователей, пагинацию и rate limiting.
 */
@Slf4j // Добавлен логгер
@RequiredArgsConstructor
@Controller
@RequestMapping("/page_one/reviews")
public class ReviewsController extends BaseController {

    private final ReviewsRepo reviewsRepo;
    private final UserRepository userRepository;
    private final EnhancedReviewsRateLimitService rateLimitService;
    private final UserService userService;

    /**
     * Страница поиска пользователей для отзывов.
     * Поддерживает кеширование результатов поиска и rate limiting по IP.
     *
     * @param search поисковый запрос (опционально)
     * @param authentication данные аутентификации
     * @param request HTTP запрос для получения IP
     * @param model модель для передачи данных в представление
     * @return имя шаблона страницы поиска отзывов
     */
    @GetMapping
    public String reviewsSearch(@RequestParam(required = false) String search,
                                Authentication authentication,
                                HttpServletRequest request,
                                Model model) {

        String clientIp = getClientIp(request);
        log.debug("Поиск отзывов с IP: {}, поисковый запрос: {}", clientIp, search != null ? search : "пустой");

        // Rate limiting для поиска
        if (!rateLimitService.tryConsumeReviewSearch(clientIp)) {
            log.warn("Превышен лимит запросов на поиск отзывов для IP: {}", clientIp);
            model.addAttribute("error", "Слишком много запросов. Пожалуйста, подождите.");
            model.addAttribute("users", List.of());
            model.addAttribute("reviewsOfDeletedUsers", List.of());
            return "page_one/reviews_search";
        }

        boolean isSuperOrAdmin = hasSuperAccess(authentication);

        if (search != null && !search.trim().isEmpty()) {
            // Кешируем результаты поиска для одинаковых запросов
            List<User> activeUsers = cacheableUserSearch(search.trim());
            List<Reviews> reviewsOfDeletedUsers = cacheableDeletedReviewsSearch(search.trim(), activeUsers);

            log.info("Поиск завершен: найдено активных пользователей - {}, отзывов об удаленных - {}",
                    activeUsers.size(), reviewsOfDeletedUsers.size());

            model.addAttribute("users", activeUsers);
            model.addAttribute("reviewsOfDeletedUsers", reviewsOfDeletedUsers);
            model.addAttribute("searchTerm", search.trim());
        } else {
            model.addAttribute("users", List.of());
            model.addAttribute("reviewsOfDeletedUsers", List.of());
        }

        return "page_one/reviews_search";
    }

    /**
     * Кешируемый поиск активных пользователей по поисковому термину.
     * Результаты кешируются в reviewCacheManager.
     *
     * @param searchTerm поисковый термин
     * @return список активных пользователей
     */
    @Cacheable(value = "reviewsSearchResults", key = "#searchTerm", unless = "#result.isEmpty()",
            cacheManager = "reviewCacheManager")
    public List<User> cacheableUserSearch(String searchTerm) {
        log.debug("Выполнение поиска активных пользователей с термином: {}", searchTerm);
        return userRepository.findBySearchTermExcludingSuper(searchTerm);
    }

    /**
     * Кешируемый поиск отзывов об удаленных пользователях.
     * Фильтрует отзывы, исключая тех, кто есть в списке активных.
     *
     * @param searchTerm поисковый термин
     * @param activeUsers список активных пользователей
     * @return список отзывов об удаленных пользователях (уникальные по reviewedUserId)
     */
    @Cacheable(value = "reviewsSearchResults", key = "'deleted:' + #searchTerm", unless = "#result.isEmpty()",
            cacheManager = "reviewCacheManager")
    public List<Reviews> cacheableDeletedReviewsSearch(String searchTerm, List<User> activeUsers) {
        log.debug("Поиск отзывов об удаленных пользователях по термину: {}", searchTerm);
        List<Reviews> allReviewsOfDeleted = reviewsRepo.findByReviewedUserSearchTerm(searchTerm);

        return allReviewsOfDeleted.stream()
                .filter(review -> activeUsers.stream()
                        .noneMatch(user -> user.getId().equals(review.getReviewedUserId())))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Reviews::getReviewedUserId,
                                review -> review,
                                (existing, replacement) -> existing
                        ),
                        map -> new ArrayList<>(map.values())
                ));
    }

    /**
     * Страница отзывов о конкретном пользователе.
     * Поддерживает rate limiting как на уровень страницы, так и на конкретного пользователя.
     *
     * @param hashId идентификатор пользователя (hash или числовой ID для удаленных)
     * @param authentication данные аутентификации
     * @param request HTTP запрос
     * @param model
     * @return имя шаблона страницы отзывов
     */
    @GetMapping("/{hashId}")
    public String userReviews(@PathVariable String hashId,
                              Authentication authentication,
                              HttpServletRequest request,
                              Model model) {

        String clientIp = getClientIp(request);
        log.debug("Запрос страницы отзывов для hashId: {} с IP: {}", hashId, clientIp);

        // Rate limiting для просмотра страницы
        if (!rateLimitService.tryConsumeReviewPageLoad(clientIp)) {
            log.warn("Превышен лимит загрузки страниц отзывов для IP: {}", clientIp);
            model.addAttribute("error", "Слишком много запросов. Пожалуйста, подождите.");
            return "page_one/reviews_search";
        }

        Optional<User> userOpt = userRepository.findByHashId(hashId);
        boolean userExists = userOpt.isPresent();
        User user = userOpt.orElse(null);

        // Обработка случая, когда пользователь не найден или удален
        if (!userExists || user == null) {
            try {
                List<Reviews> reviewsAboutDeletedUser = reviewsRepo.findByReviewedUserIdOrderByCreatedAtDesc(
                        Long.parseLong(hashId)
                );

                if (!reviewsAboutDeletedUser.isEmpty()) {
                    log.info("Показаны отзывы об удаленном пользователе с ID: {}", hashId);
                    return handleDeletedUserReviews(reviewsAboutDeletedUser, authentication, hashId, model);
                }
            } catch (NumberFormatException e) {
                // Невалидный hashId
                log.warn("Невалидный hashId при поиске пользователя: {}", hashId);
            }

            log.info("Пользователь не найден: {}", hashId);
            model.addAttribute("error", "Пользователь не найден");
            return "page_one/reviews_search";
        }

        User currentUser = getCurrentUser(authentication);

        // Rate limiting для конкретного пользователя
        if (!rateLimitService.tryConsumeUserReviews(clientIp, user.getId())) {
            log.warn("Превышен лимит запросов к пользователю с IP: {}", clientIp);
            model.addAttribute("error", "Слишком много запросов к этому пользователю.");
            return "page_one/reviews_search";
        }

        // Получаем отзывы с кешированием
        List<Reviews> reviews = getCachedUserReviews(user.getId());
        log.info("Загружено {} отзывов для пользователя", reviews.size());

        model.addAttribute("userRating", userService.getUserRating(user.getId()));
        model.addAttribute("reviewedUser", user);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("reviecoms", reviews);
        model.addAttribute("userExists", true);
        model.addAttribute("reviewedUserId", user.getId());
        model.addAttribute("hashId", hashId);

        return "page_one/reviews";
    }

    /**
     * Получение кешированного списка отзывов для пользователя.
     * Кеш используется для снижения нагрузки на БД.
     *
     * @param userId ID пользователя
     * @return список корневых отзывов (без ответов)
     */
    @Cacheable(value = "reviewsList", key = "#userId", unless = "#result.isEmpty()",
            cacheManager = "reviewCacheManager")
    public List<Reviews> getCachedUserReviews(Long userId) {
        log.debug("Загрузка отзывов из БД для userId: {}", userId);
        return reviewsRepo.findByReviewedUserIdAndParentReviewIsNullOrderByCreatedAtDesc(userId);
    }

    /**
     * Обработка показа отзывов об удаленном пользователе.
     * Используется, когда пользователь был удален, но отзывы на него остались.
     *
     * @param reviewsAboutDeletedUser список отзывов об удаленном пользователе
     * @param authentication данные аутентификации
     * @param hashId идентификатор
     * @param model
     * @return имя шаблона страницы отзывов
     */
    private String handleDeletedUserReviews(List<Reviews> reviewsAboutDeletedUser,
                                            Authentication authentication,
                                            String hashId,
                                            Model model) {
        Reviews firstReview = reviewsAboutDeletedUser.get(0);
        Long reviewedUserId = firstReview.getReviewedUserId();
        log.info("Отображение страницы с отзывами об удаленном пользователе с ID: {}", reviewedUserId);

        model.addAttribute("reviewedUser", null);
        model.addAttribute("currentUser", getCurrentUser(authentication));
        model.addAttribute("reviecoms", reviewsRepo.findByReviewedUserIdAndParentReviewIsNullOrderByCreatedAtDesc(reviewedUserId));
        model.addAttribute("userExists", false);
        model.addAttribute("reviewedUserId", reviewedUserId);
        model.addAttribute("hashId", hashId);

        if (reviewedUserId != null) {
            model.addAttribute("userRating", userService.getUserRating(reviewedUserId));
        }

        return "page_one/reviews";
    }

    /**
     * Добавление нового отзыва или ответа на существующий отзыв.
     * Поддерживает rate limiting и очищает связанные кеши.
     *
     * @param hashId идентификатор пользователя, которому оставляют отзыв
     * @param text текст отзыва
     * @param parentCommentId ID родительского отзыва (если это ответ)
     * @param authentication данные аутентификации
     * @param request HTTP запрос
     * @param redirectAttributes атрибуты для редиректа
     * @return перенаправление на страницу отзывов
     */
    @PostMapping("/{hashId}/reviecom")
    @Caching(evict = {
            @CacheEvict(value = "reviewsList", key = "#hashId", cacheManager = "reviewCacheManager"),
            @CacheEvict(value = "userReviewsRating", allEntries = true, cacheManager = "reviewCacheManager"),
            @CacheEvict(value = "reviewsStats", allEntries = true, cacheManager = "reviewCacheManager")
    })
    public String addReview(@PathVariable String hashId,
                            @RequestParam String text,
                            @RequestParam(required = false) Long parentCommentId,
                            Authentication authentication,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {

        String clientIp = getClientIp(request);
        log.debug("Попытка добавления отзыва для hashId: {} с IP: {}", hashId, clientIp);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Неавторизованная попытка добавления отзыва с IP: {}", clientIp);
            redirectAttributes.addFlashAttribute("error", "Авторизируйтесь!");
            return "redirect:/page_one/reviews/{hashId}";
        }

        // Rate limiting для добавления отзыва
        if (!rateLimitService.tryConsumeReviewAdd(clientIp)) {
            log.warn("Превышен лимит добавления отзывов для IP: {}", clientIp);
            redirectAttributes.addFlashAttribute("error", "Слишком много отзывов. Подождите немного.");
            return "redirect:/page_one/reviews/{hashId}";
        }

        String username = authentication.getName();
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        Optional<User> reviewedUserOpt = userRepository.findByHashId(hashId);
        User reviewedUser = reviewedUserOpt.orElse(null);

        Long userId;

        if (reviewedUser != null) {
            userId = reviewedUser.getId();
        } else {
            try {
                userId = Long.parseLong(hashId);
            } catch (NumberFormatException e) {
                log.error("Не удалось определить ID пользователя для отзыва по hashId: {}", hashId);
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/page_one/reviews";
            }

            if (!reviewsRepo.existsByReviewedUserId(userId)) {
                log.error("Пользователь с ID {} не найден в отзывах", userId);
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/page_one/reviews";
            }
        }

        Reviews review = createReview(reviewedUser, userId, author, text, parentCommentId);
        reviewsRepo.save(review);

        // Сбрасываем лимиты после успешного добавления
        rateLimitService.resetLimits(clientIp);
        log.info("Успешно добавлен {} для hashId: {}",
                parentCommentId != null ? "ответ" : "отзыв", hashId);

        redirectAttributes.addFlashAttribute("success",
                parentCommentId != null ? "Ответ добавлен" : "Отзыв добавлен");

        return "redirect:/page_one/reviews/" + hashId;
    }

    /**
     * Создание объекта отзыва на основе переданных данных.
     * Заполняет информацию как об авторе, так и о получателе отзыва.
     *
     * @param reviewedUser пользователь, которому оставляют отзыв (может быть null для удаленных)
     * @param author автор отзыва
     * @param text текст отзыва
     * @param parentCommentId ID родительского отзыва (если есть)
     * @return готовый объект Reviews
     */
    private Reviews createReview(User reviewedUser, Long userId, User author,
                                 String text, Long parentCommentId) {
        Reviews review = new Reviews();

        if (reviewedUser != null) {
            review.setReviewedUserId(reviewedUser.getId());
            review.setReviewedUsername(reviewedUser.getUsername());
            review.setReviewedNameuse(reviewedUser.getNameuse());
            review.setReviewedUserEmail(reviewedUser.getEmail());
            review.setReviewedUserPhonePost(reviewedUser.getPhonePost());
        } else {
            // Пытаемся восстановить данные о пользователе из существующих отзывов
            List<Reviews> existingReviews = reviewsRepo.findByReviewedUserIdOrderByCreatedAtDesc(userId);
            if (!existingReviews.isEmpty()) {
                Reviews existingReview = existingReviews.get(0);
                review.setReviewedUserId(userId);
                review.setReviewedUsername(existingReview.getReviewedUsername());
                review.setReviewedNameuse(existingReview.getReviewedNameuse());
                review.setReviewedUserEmail(existingReview.getReviewedUserEmail());
                review.setReviewedUserPhonePost(existingReview.getReviewedUserPhonePost());
                review.setReviewedUserDeleted(true);
            }
        }

        // Заполнение данных автора
        review.setAuthor(author);
        review.setAuthorUsername(author.getUsername());
        review.setAuthorNameuse(author.getNameuse());
        review.setAuthorEmail(author.getEmail());
        review.setText(text);
        review.setCreatedAt(LocalDateTime.now());

        // Установка родительского отзыва для ответов
        if (parentCommentId != null) {
            Reviews parentReview = reviewsRepo.findById(parentCommentId)
                    .orElseThrow(() -> new IllegalArgumentException("Родительский отзыв не найден"));
            review.setParentReview(parentReview);
        }

        return review;
    }

    /**
     * Удаление отзыва вместе со всеми ответами на него.
     * Доступно только для пользователей с правами SUPER или ADMIN.
     *
     * @param hashId идентификатор пользователя, к которому относится отзыв
     * @param reviecomId ID отзыва для удаления
     * @param authentication данные аутентификации
     * @param redirectAttributes атрибуты для редиректа
     * @return перенаправление на страницу отзывов
     */
    @PostMapping("/{hashId}/reviecom/{reviecomId}/delete")
    @Caching(evict = {
            @CacheEvict(value = "reviewsList", key = "#hashId", cacheManager = "reviewCacheManager"),
            @CacheEvict(value = "userReviewsRating", allEntries = true, cacheManager = "reviewCacheManager")
    })
    public String deleteReview(@PathVariable String hashId,
                               @PathVariable Long reviecomId,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {

        // Используем общий метод для проверки SUPER и ADMIN
        if (!hasSuperAccess(authentication)) {
            log.warn("Попытка удаления отзыва без достаточных прав для hashId: {}", hashId);
            redirectAttributes.addFlashAttribute("error", "Доступ запрещен");
            return "redirect:/page_one/reviews/" + hashId;
        }

        Reviews review = reviewsRepo.findById(reviecomId)
                .orElseThrow(() -> new IllegalArgumentException("Отзыв не найден"));

        // Если у отзыва есть ответы, сначала удаляем их
        if (!review.getReplies().isEmpty()) {
            log.debug("Удаление {} ответов на отзыв {}", review.getReplies().size(), review.getId());
            reviewsRepo.deleteAll(review.getReplies());
        }

        reviewsRepo.delete(review);
        log.info("Удален отзыв с ID: {} для hashId: {}", reviecomId, hashId);

        redirectAttributes.addFlashAttribute("success", "Отзыв удален");

        return "redirect:/page_one/reviews/" + hashId;
    }

    /**
     * Пагинированная загрузка отзывов (endpoint для AJAX).
     * Поддерживает rate limiting и возвращает данные в формате JSON.
     *
     * @param hashId идентификатор пользователя
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @param authentication данные аутентификации
     * @param request HTTP запрос
     * @return ResponseEntity с обрезанными данными отзывов
     */
    @GetMapping("/{hashId}/load-more-review")
    @ResponseBody
    public ResponseEntity<SliceResponseDTO<Map<String, Object>>> loadMoreReviews(
            @PathVariable String hashId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            Authentication authentication,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        log.debug("Загрузка порции отзывов для hashId: {}, страница: {}, размер: {}, IP: {}",
                hashId, page, size, clientIp);

        // Rate limiting для пагинации
        if (!rateLimitService.tryConsumeReviewPagination(clientIp)) {
            log.warn("Превышен лимит пагинации отзывов для IP: {}", clientIp);
            SliceResponseDTO<Map<String, Object>> errorResponse = new SliceResponseDTO<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Слишком много запросов. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            Optional<User> userOpt = userRepository.findByHashId(hashId);
            boolean userExists = userOpt.isPresent();
            User user = userOpt.orElse(null);

            Long userId;

            if (userExists && user != null) {
                userId = user.getId();
                // Rate limiting для конкретного пользователя
                if (!rateLimitService.tryConsumeUserReviews(clientIp, userId)) {
                    log.warn("Превышен лимит запросов к пользователю при пагинации для IP: {}", clientIp);
                    SliceResponseDTO<Map<String, Object>> errorResponse = new SliceResponseDTO<>();
                    errorResponse.setStatus("error");
                    errorResponse.setMessage("Слишком много запросов к этому пользователю");
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
                }
            } else {
                try {
                    userId = Long.parseLong(hashId);
                    log.debug("Используется числовой ID для удаленного пользователя: {}", userId);
                } catch (NumberFormatException e) {
                    log.error("Неверный формат hashId при пагинации: {}", hashId, e);
                    SliceResponseDTO<Map<String, Object>> errorResponse = new SliceResponseDTO<>();
                    errorResponse.setStatus("error");
                    errorResponse.setMessage("Пользователь не найден");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            User currentUser = getCurrentUser(authentication);
            boolean isSuperUser = hasOnlySuperAccess(authentication);
            boolean isAdmin = hasOnlyAdminAccess(authentication);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Slice<Reviews> reviewsSlice = reviewsRepo.findByReviewedUserIdAndParentReviewIsNull(userId, pageable);

            List<Map<String, Object>> reviewsData = new ArrayList<>();
            for (Reviews review : reviewsSlice.getContent()) {
                Map<String, Object> reviewData = convertReviewToMap(review, currentUser, isSuperUser);
                reviewsData.add(reviewData);
            }

            SliceResponseDTO<Map<String, Object>> response = SliceResponseDTO.success(
                    reviewsSlice,
                    "Отзывы успешно загружены",
                    reviewsData
            );

            SliceResponseDTO<Map<String, Object>> finalResponse = new SliceResponseDTO<>();
            finalResponse.setStatus(response.getStatus());
            finalResponse.setMessage(response.getMessage());
            finalResponse.setReviews(response.getReviews());
            finalResponse.setHasNext(response.isHasNext());
            finalResponse.setCurrentPage(response.getCurrentPage());
            finalResponse.setSize(response.getSize());
            finalResponse.setFirst(response.isFirst());
            finalResponse.setLast(response.isLast());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userExists", userExists);
            metadata.put("hashId", hashId);
            metadata.put("isSuperUser", isSuperUser);
            metadata.put("isAdmin", isAdmin);

            if (userExists && user != null) {
                metadata.put("reviewedUserName", user.getNameuse());
                if (isSuperUser) {
                    metadata.put("reviewedUserId", user.getId());
                }
            }

            finalResponse.setMetadata(metadata);
            log.debug("Успешно загружено {} отзывов для hashId: {}", reviewsData.size(), hashId);
            return ResponseEntity.ok(finalResponse);

        } catch (Exception e) {
            log.error("Ошибка при загрузке отзывов для hashId: {}", hashId, e);
            SliceResponseDTO<Map<String, Object>> errorResponse = new SliceResponseDTO<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Ошибка при загрузке отзывов: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Извлекает реальный IP-адрес клиента из заголовков HTTP.
     * Учитывает прокси и балансировщики нагрузки.
     *
     * @param request HTTP запрос
     * @return IP-адрес клиента
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    /**
     * Получает текущего аутентифицированного пользователя.
     * Может вернуть null, если пользователь не аутентифицирован.
     *
     * @param authentication данные аутентификации
     * @return пользователь или null
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            return userRepository.findByUsername(username).orElse(null);
        }
        return null;
    }

    /**
     * Преобразует объект отзыва в безопасную для отправки на клиент карту.
     * Учитывает права доступа (скрывает чувствительные данные для обычных пользователей).
     *
     * @param review отзыв для преобразования
     * @param currentUser текущий аутентифицированный пользователь
     * @param isSuperUser флаг, является ли текущий пользователь SUPER
     * @return карта с данными отзыва для JSON ответа
     */
    private Map<String, Object> convertReviewToMap(Reviews review, User currentUser, boolean isSuperUser) {
        Map<String, Object> reviewData = new HashMap<>();

        // Основная информация об отзыве
        reviewData.put("id", review.getId());
        reviewData.put("text", review.getText());
        reviewData.put("createdAt", review.getCreatedAt().toString());
        reviewData.put("formattedDate", this.formatDate(review.getCreatedAt()));

        // Безопасное получение автора (заглушка для удаленных)
        User author = review.getAuthor();
        if (author == null) {
            // Создаем заглушку для удаленного автора
            author = new User();
            author.setId(-1L);
            author.setUsername("deleted");
            author.setNameuse("Удаленный пользователь");
        }

        // ID пользователя, о котором отзыв - СКРЫВАЕМ для всех, кроме SUPER
        if (isSuperUser) {
            reviewData.put("reviewedUserId", review.getReviewedUserId());
        } else {
            reviewData.put("reviewedUserId", null); // Скрываем для обычных пользователей и админа
        }

        // Информация об авторе
        Map<String, Object> authorData = new HashMap<>();

        // ТОЛЬКО для SUPER пользователя показываем полную информацию
        if (isSuperUser) {
            // Показываем ID только для SUPER
            if (review.getAuthor() != null) {
                authorData.put("id", review.getAuthor().getId());
            } else {
                authorData.put("id", null);
            }
            authorData.put("username", review.getAuthorUsername());
            authorData.put("nameuse", review.getAuthorNameuse());
            authorData.put("isDeleted", review.isAuthorDeleted());
            authorData.put("displayName", review.getDisplayAuthorNameuse());
        } else {
            // Для обычных пользователей и админа скрываем чувствительную информацию
            authorData.put("id", null); // Скрываем ID
            authorData.put("username", null); // Скрываем логин
            authorData.put("nameuse", review.getAuthorNameuse());
            authorData.put("isDeleted", review.isAuthorDeleted());

            // Отображаем только имя пользователя, не показывая логин
            String displayName = review.getAuthorNameuse();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "Анонимный пользователь";
            }
            if (review.isAuthorDeleted()) {
                displayName = "Удаленный пользователь";
            }
            authorData.put("displayName", displayName);
        }
        reviewData.put("author", authorData);

        // Можно ли отвечать на этот отзыв (нельзя отвечать самому себе)
        boolean canReply = currentUser != null &&
                review.getAuthor() != null &&
                !currentUser.getId().equals(review.getAuthor().getId());
        reviewData.put("canReply", canReply);

        // Информация о текущем пользователе
        reviewData.put("isCurrentUserAuthor",
                currentUser != null &&
                        review.getAuthor() != null &&
                        currentUser.getId().equals(review.getAuthor().getId()));

        // Ответы на этот отзыв
        List<Map<String, Object>> repliesData = new ArrayList<>();
        for (Reviews reply : review.getReplies()) {
            Map<String, Object> replyData = new HashMap<>();
            replyData.put("id", reply.getId());
            replyData.put("text", reply.getText());
            replyData.put("createdAt", reply.getCreatedAt().toString());
            replyData.put("formattedDate", formatDate(reply.getCreatedAt()));

            Map<String, Object> replyAuthorData = new HashMap<>();

            // ТОЛЬКО для SUPER пользователя
            if (isSuperUser) {
                if (reply.getAuthor() != null) {
                    replyAuthorData.put("id", reply.getAuthor().getId());
                } else {
                    replyAuthorData.put("id", null);
                }
                replyAuthorData.put("username", reply.getAuthorUsername());
                replyAuthorData.put("nameuse", reply.getAuthorNameuse());
                replyAuthorData.put("isDeleted", reply.isAuthorDeleted());
                replyAuthorData.put("displayName", reply.getDisplayAuthorNameuse());
            } else {
                // Для обычных пользователей и админа
                replyAuthorData.put("id", null);
                replyAuthorData.put("username", null);
                replyAuthorData.put("nameuse", reply.getAuthorNameuse());
                replyAuthorData.put("isDeleted", reply.isAuthorDeleted());

                String displayName = reply.getAuthorNameuse();
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = "Анонимный пользователь";
                }
                if (reply.isAuthorDeleted()) {
                    displayName = "Удаленный пользователь";
                }
                replyAuthorData.put("displayName", displayName);
            }
            replyData.put("author", replyAuthorData);

            // Можно ли отвечать на ответ
            boolean canReplyToReply = currentUser != null &&
                    reply.getAuthor() != null &&
                    !currentUser.getId().equals(reply.getAuthor().getId());
            replyData.put("canReply", canReplyToReply);

            repliesData.add(replyData);
        }
        reviewData.put("replies", repliesData);

        // Добавляем флаг, является ли текущий пользователь SUPER
        reviewData.put("isSuperUser", isSuperUser);

        return reviewData;
    }

    /**
     * Форматирует LocalDateTime в строку для отображения на клиенте.
     *
     * @param dateTime дата и время для форматирования
     * @return отформатированная строка даты и времени
     */
    private String formatDate(LocalDateTime dateTime) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            return dateTime.format(formatter);
        } catch (Exception e) {
            log.warn("Ошибка форматирования даты: {}", e.getMessage());
            return "Не указано";
        }
    }
}