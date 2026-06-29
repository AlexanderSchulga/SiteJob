package jobforsite.sitejob.module.service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.auth.service.EmailService;
import jobforsite.sitejob.module.service.model.CommentPersonalJob;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextJobCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.PostViewService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления объявлениями категории "Услуги" (Job).
 * Обрабатывает просмотр списка, создание, детальный просмотр и приватные сообщения.
 */
@RequiredArgsConstructor
@Controller
public class RefControllerJob extends BasePostController {

    private static final Logger log = LoggerFactory.getLogger(RefControllerJob.class);

    private final PostRepoJob postRepoJob;
    private final CommentRepoJobPersonal commentRepoJobPersonal;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final ImageService imageService;
    private final RatingRepository ratingRepository;
    private final RegionsConfig regionsConfig;
    private final RateLimitService rateLimitService;
    private final PostCacheService postCacheService;
    private final PostViewService postViewService;

    private final EmailService emailService;

    /**
     * Отображает главную страницу списка услуг с фильтрацией, поиском и пагинацией.
     *
     * @param category   категория услуги (опционально)
     * @param search     поисковый запрос (опционально)
     * @param sort       сортировка (newest, popular и т.д.)
     * @param priceSort  сортировка по цене
     * @param region     регион (опционально)
     * @param city       город (опционально)
     * @param page       номер страницы
     * @param size       размер страницы
     * @param authentication данные аутентификации пользователя
     * @param request    HTTP-запрос для получения IP
     * @param model      модель для передачи данных в представление
     * @return имя шаблона страницы
     */
    @GetMapping("/page_one/viewsonejob")
    public String viewsOnePage(@RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "sort", defaultValue = "newest") String sort,
                               @RequestParam(value = "priceSort", defaultValue = "none") String priceSort,
                               @RequestParam(value = "region", required = false) String region,
                               @RequestParam(value = "city", required = false) String city,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "8") int size,
                               Authentication authentication,
                               HttpServletRequest request,
                               Model model) {

        log.debug("GET /page_one/viewsonejob: category={}, search={}, region={}, city={}, page={}, size={}",
                category, search, region, city, page, size);

        String clientIp = getClientIp(request);

        // Проверка лимита запросов на просмотр
        if (!checkRateLimitForView(clientIp, rateLimitService, "service", model, "Услуги",
                postTextJobCategoryConfig.getCategories(), regionsConfig)) {
            log.warn("Rate limit exceeded for service view from IP: {}", clientIp);
            return "page_one/viewsonejob";
        }

        // Получаем города для меню
        Map<String, List<String>> citiesByGroup = getCitiesForMenu(region, regionsConfig);

        // Получаем список постов из кэша
        List<PostTextJob> postsj = postCacheService.getServicesList(
                category, search, region, city, sort, priceSort, page, size
        );

        // Получаем общее количество из кэша
        long totalItems = postCacheService.getServicesCount(category, search, region, city);

        // Настройка общих атрибутов модели для списка
        setupListModel(model, postsj, totalItems, page, size, "Услуги",
                postTextJobCategoryConfig.getCategories(), regionsConfig,
                category, region, city, sort, priceSort, authentication);

        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("postsj", postsj);

        log.debug("Returned {} services, total items: {}", postsj.size(), totalItems);
        return "page_one/viewsonejob";
    }

    /**
     * Отображает страницу создания новой услуги.
     *
     * @param model модель для передачи данных в представление
     * @return имя шаблона страницы создания
     */
    @GetMapping("/page_two/viewstwojob")
    public String viewsTwoPage(Model model) {
        log.debug("GET /page_two/viewstwojob - showing creation form");
        Iterable<PostTextJob> postsj = postRepoJob.findAll();
        model.addAttribute("title", "Создание");
        model.addAttribute("postsj", postsj);
        model.addAttribute("categories", postTextJobCategoryConfig.getCategories());
        return "page_two/viewstwojob";
    }

    /**
     * Обрабатывает создание новой услуги.
     *
     * @param title        заголовок объявления
     * @param price        цена
     * @param full_text    полный текст объявления
     * @param category     категория
     * @param location     местоположение
     * @param moderation   флаг модерации
     * @param files        массив загружаемых изображений
     * @param authentication данные аутентификации пользователя
     * @param request      HTTP-запрос для получения IP
     * @param model        модель для передачи данных в представление
     * @return перенаправление на главную или возврат к форме при ошибке
     * @throws IOException если ошибка загрузки файлов
     */
    @PostMapping("/page_two/viewstwojob")
    public String viewsTwoPost(
            @RequestParam String title,
            @RequestParam Integer price,
            @RequestParam String full_text,
            @RequestParam String category,
            @RequestParam String location,
            @RequestParam Boolean moderation,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication,
            HttpServletRequest request,
            Model model) throws IOException {

        String clientIp = getClientIp(request);
        log.debug("POST /page_two/viewstwojob - creating new service from IP: {}", clientIp);

        // Проверка лимита на создание объявлений
        if (!checkRateLimitForCreate(clientIp, rateLimitService, "service", model,
                postTextJobCategoryConfig.getCategories(), "page_two/viewstwojob")) {
            log.warn("Rate limit exceeded for service creation from IP: {}", clientIp);
            return "page_two/viewstwojob";
        }

        // Загрузка изображений
        try {
            uploadImages(files, null, null, null, null, model, "page_two/viewstwojob");
        } catch (IllegalArgumentException e) {
            log.error("Image upload validation failed: {}", e.getMessage());
            model.addAttribute("categories", postTextJobCategoryConfig.getCategories());
            return "page_two/viewstwojob";
        }

        try {

            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            PostTextJob postj = new PostTextJob(title, price, full_text, category, location, moderation);
            User user = (User) authentication.getPrincipal();
            postj.setUser(user);

            PostTextJob savedPost = postRepoJob.save(postj);


            // ========== АВТОМАТИЧЕСКАЯ ОТПРАВКА УВЕДОМЛЕНИЯ ==========
            if (savedPost.isModeration()) {
                emailService.sendModerationNotification(
                        "service",
                        savedPost.getTitle(),
                        savedPost.getUser().getNameuse(),
                        savedPost.getUser().getRegion(),
                        savedPost.getCreatedAt()
                );
            }

            imageService.saveImages(files, null, savedPost, null, null);
            postCacheService.clearServicesCache();

            log.info("New service created successfully, post ID: {}", savedPost.getId());
            return "redirect:/index";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postTextJobCategoryConfig.getCategories());
            return "page_two/viewstwojob";
        } catch (Exception e) {
            log.error("Error creating service: errorType={}", e.getClass().getSimpleName(), e);
            return handleCreationError(e, model, postTextJobCategoryConfig.getCategories(), "page_two/viewstwojob");
        }
    }

    /**
     * Отображает детальную страницу услуги с приватными комментариями.
     *
     * @param hashId         хэш-идентификатор поста
     * @param model          модель для передачи данных в представление
     * @param authentication данные аутентификации пользователя (может быть null)
     * @param request        HTTP-запрос для регистрации просмотра
     * @return имя шаблона детальной страницы
     */
    @GetMapping("/postidviewsjob/{hashId}")
    public String viewsTwoDetails(@PathVariable String hashId, Model model, Authentication authentication, HttpServletRequest request) {
        log.debug("GET /postidviewsjob/{} - viewing service details", hashId);

        PostTextJob postj = postRepoJob.findByHashId(hashId)
                .orElseThrow(() -> {
                    log.warn("Service not found with hashId: {}", hashId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
                });

        // Регистрируем просмотр
        User currentUser = authentication != null && authentication.isAuthenticated()
                ? (User) authentication.getPrincipal()
                : null;
        postViewService.registerServiceView(postj, request, currentUser);
        log.debug("View registered for service ID: {}", postj.getId());

        // Проверка доступа к посту
        if (!checkPostAccessAndWarnings(postj, authentication, model)) {
            log.warn("Access denied to service ID: {} for user", postj.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
        }

        Long postId = postj.getId();
        boolean isPrivateTabActive = false;
        boolean hasAdminAccess = hasSuperAccess(authentication);

        // Настройка приватных комментариев
        List<CommentPersonalJob> commentsPersonalJob = setupPrivateComments(
                postj, postId, authentication, hasAdminAccess,
                id -> commentRepoJobPersonal.findByPostjobIdOrderByCreatedAtDesc(id),
                (id, userId) -> commentRepoJobPersonal.findByPostIdAndUserOrRecipient(id, userId),
                (id, userId) -> commentRepoJobPersonal.markMessagesAsRead(id, userId)
        );

        // Добавление рейтинга продавца
        addSellerRatingToModel(postj, model, ratingRepository);

        model.addAttribute("postwo", Collections.singletonList(postj));
        model.addAttribute("commentsPersonalJob", commentsPersonalJob);
        model.addAttribute("isPrivateTabActive", isPrivateTabActive);
        model.addAttribute("currentUser", authentication != null ? authentication.getPrincipal() : null);
        model.addAttribute("commentRepoJobPersonal", commentRepoJobPersonal);

        log.debug("Returning service details for ID: {}, comments count: {}", postId, commentsPersonalJob.size());
        return "postidviewsjob";
    }

    /**
     * API-метод для отметки сообщений как прочитанных.
     *
     * @param postId         идентификатор поста
     * @param authentication данные аутентификации пользователя
     * @return HTTP-ответ 200 OK при успехе, 500 при ошибке
     */
    @Transactional
    @PostMapping("/api/messages/job/{postId}/markAsRead")
    @ResponseBody
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long postId, Authentication authentication) {
        log.debug("POST /api/messages/job/{}/markAsRead - marking messages as read", postId);
        try {
            User currentUser = (User) authentication.getPrincipal();
            commentRepoJobPersonal.markMessagesAsRead(postId, currentUser.getId());
            log.debug("Messages marked as read for postId: {}", postId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking messages as read for postId {}: errorType={}", postId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(500).build();
        }
    }
}