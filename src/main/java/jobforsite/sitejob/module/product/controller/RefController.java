package jobforsite.sitejob.module.product.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.auth.service.EmailService;
import jobforsite.sitejob.module.product.model.CommentPersonal;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.PostViewService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Контроллер для управления товарами/объявлениями.
 * Обрабатывает:
 * <ul>
 *   <li>Просмотр списка товаров с фильтрацией и пагинацией</li>
 *   <li>Создание нового товара с загрузкой изображений</li>
 *   <li>Просмотр детальной страницы товара с комментариями</li>
 *   <li>Отметку сообщений как прочитанных</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class RefController extends BasePostController {

    private final PostRepo postRepo;
    private final CommentRepoPersonal commentRepoPersonal;
    private final PostTextCategoryConfig postTextCategoryConfig;
    private final ImageService imageService;
    private final RatingRepository ratingRepository;
    private final RegionsConfig regionsConfig;
    private final RateLimitService rateLimitService;
    private final PostCacheService postCacheService;
    private final PostViewService postViewService;

    private final EmailService emailService;

    /**
     * Отображает страницу со списком товаров с поддержкой:
     * <ul>
     *   <li>Фильтрации по категории, региону, городу</li>
     *   <li>Поиска по названию</li>
     *   <li>Сортировки (новизна, цена)</li>
     *   <li>Пагинации</li>
     *   <li>Rate limiting для защиты от частых запросов</li>
     * </ul>
     *
     * @param category       фильтр по категории
     * @param search         поисковый запрос
     * @param sort           тип сортировки (newest, oldest и т.д.)
     * @param priceSort      сортировка по цене (asc, desc, none)
     * @param region         фильтр по региону
     * @param city           фильтр по городу
     * @param page           номер страницы (начиная с 0)
     * @param size           количество элементов на странице
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос (для получения IP)
     * @param model          модель для передачи данных в представление
     * @return имя шаблона страницы
     */
    @GetMapping("/page_one/viewsone")
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

        String clientIp = getClientIp(request);
        log.debug("Запрос списка товаров: IP={}, category={}, search={}, region={}, city={}, page={}, size={}",
                clientIp, category, search, region, city, page, size);

        // Rate limiting для просмотра страницы товаров
        if (!checkRateLimitForView(clientIp, rateLimitService, "product", model, "Товары",
                postTextCategoryConfig.getCategories(), regionsConfig)) {
            log.warn("Превышен лимит запросов для просмотра товаров, IP: {}", clientIp);
            return "page_one/viewsone";
        }

        // Получение списка городов для меню
        Map<String, List<String>> citiesByGroup = getCitiesForMenu(region, regionsConfig);

        // Получение кэшированного списка постов
        List<PostText> posts = postCacheService.getProductsList(
                category, search, region, city, sort, priceSort, page, size
        );

        // Получение кэшированного общего количества
        long totalItems = postCacheService.getProductsCount(category, search, region, city);

        log.info("Загружен список товаров: найдено {} из {}, страница {}/{}",
                posts.size(), totalItems, page + 1, (int) Math.ceil((double) totalItems / size));

        // Настройка модели для отображения списка
        setupListModel(model, posts, totalItems, page, size, "Товары",
                postTextCategoryConfig.getCategories(), regionsConfig,
                category, region, city, sort, priceSort, authentication);

        // Дополнительные атрибуты
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("posts", posts);

        return "page_one/viewsone";
    }

    /**
     * Отображает страницу создания нового товара.
     *
     * @param model модель для передачи данных в представление
     * @return имя шаблона страницы создания
     */
    @GetMapping("/page_two/viewstwo")
    public String viewsTwoPage(Model model) {
        Iterable<PostText> posts = postRepo.findAll();
        model.addAttribute("title", "Создание товара");
        model.addAttribute("posts", posts);
        model.addAttribute("categories", postTextCategoryConfig.getCategories());
        log.debug("Отображена страница создания товара");
        return "page_two/viewstwo";
    }

    /**
     * Обрабатывает создание нового товара.
     * Валидирует:
     * <ul>
     *   <li>Наличие хотя бы одного изображения</li>
     *   <li>Количество изображений (не более 10)</li>
     *   <li>Rate limit для предотвращения спама</li>
     * </ul>
     *
     * @param title          название товара
     * @param price          цена
     * @param full_text      полное описание
     * @param category       категория товара
     * @param location       местоположение
     * @param moderation     флаг модерации
     * @param files          массив изображений
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос (для получения IP)
     * @param model          модель для передачи данных в представление
     * @return редирект на главную или возврат к форме с ошибкой
     * @throws IOException при ошибках загрузки файлов
     */
    @PostMapping("/page_two/viewstwo")
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
        log.debug("Запрос на создание товара: IP={}, title={}, category={}", clientIp, title, category);

        // Проверка rate limit для создания товара
        if (!checkRateLimitForCreate(clientIp, rateLimitService, "product", model,
                postTextCategoryConfig.getCategories(), "page_two/viewstwo")) {
            log.warn("Превышен лимит запросов на создание товара, IP: {}", clientIp);
            return "page_two/viewstwo";
        }

        // Валидация наличия изображений
        if (files == null || files.length == 0 || files[0].isEmpty()) {
            log.warn("Попытка создания товара без изображений, IP: {}", clientIp);
            model.addAttribute("error", "Пожалуйста, выберите хотя бы один файл для загрузки");
            model.addAttribute("categories", postTextCategoryConfig.getCategories());
            return "page_two/viewstwo";
        }

        // Валидация количества изображений
        if (files.length > 5) {
            log.warn("Попытка загрузки более 5 изображений: count={}, IP: {}", files.length, clientIp);
            model.addAttribute("error", "Можно загрузить не более 10 изображений");
            model.addAttribute("categories", postTextCategoryConfig.getCategories());
            return "page_two/viewstwo";
        }

        try {

            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            // Создание товара
            PostText post = new PostText(title, price, full_text, category, location, moderation);
            User user = (User) authentication.getPrincipal();
            post.setUser(user);

            PostText savedPost = postRepo.save(post);

// ========== АВТОМАТИЧЕСКАЯ ОТПРАВКА УВЕДОМЛЕНИЯ ==========
            if (savedPost.isModeration()) {
                emailService.sendModerationNotification(
                        "product",
                        savedPost.getTitle(),
                        savedPost.getUser().getNameuse(),
                        savedPost.getUser().getRegion(),
                        savedPost.getCreatedAt()
                );
            }

            log.info("Создан новый товар, hashId: {}, category: {}", savedPost.getHashId(), category);

            // Сохранение изображений
            imageService.saveImages(files, savedPost, null, null, null);
            log.debug("Сохранены изображения для товара, hashId: {}, количество файлов: {}", savedPost.getHashId(), files.length);

            // Очистка кэша
            postCacheService.clearProductsCache();

            return "redirect:/index";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postTextCategoryConfig.getCategories());
            return "page_two/viewstwo";
        } catch (Exception e) {
            log.error("Ошибка при создании товара, IP: {}, errorType: {}", clientIp, e.getClass().getSimpleName(), e);
            return handleCreationError(e, model, postTextCategoryConfig.getCategories(), "page_two/viewstwo");
        }
    }

    /**
     * Отображает детальную страницу товара.
     * Включает:
     * <ul>
     *   <li>Регистрацию просмотра (для статистики)</li>
     *   <li>Проверку доступности поста</li>
     *   <li>Загрузку приватных комментариев</li>
     *   <li>Информацию о рейтинге продавца</li>
     * </ul>
     *
     * @param hashId         уникальный идентификатор поста
     * @param model          модель для передачи данных в представление
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос
     * @return имя шаблона детальной страницы
     * @throws ResponseStatusException если пост не найден или недоступен
     */
    @GetMapping("/postidviews/{hashId}")
    public String viewsTwoDetails(@PathVariable String hashId, Model model, Authentication authentication, HttpServletRequest request) {
        log.debug("Запрос детальной страницы товара, hashId: {}", hashId);

        // Поиск поста
        PostText post = postRepo.findByHashId(hashId)
                .orElseThrow(() -> {
                    log.warn("Пост не найден, hashId: {}", hashId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
                });

        // Регистрация просмотра
        User currentUser = authentication != null && authentication.isAuthenticated()
                ? (User) authentication.getPrincipal()
                : null;
        postViewService.registerProductView(post, request, currentUser);
        log.debug("Зарегистрирован просмотр товара, hashId: {}, аутентифицирован: {}", hashId, currentUser != null);

        // Проверка доступа к посту
        if (!checkPostAccessAndWarnings(post, authentication, model)) {
            log.warn("Доступ к посту запрещён, hashId: {}", hashId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
        }

        Long postId = post.getId();
        boolean isPrivateTabActive = false;
        boolean hasAdminAccess = hasSuperAccess(authentication);

        // Загрузка приватных комментариев
        List<CommentPersonal> commentsPersonal = setupPrivateComments(
                post, postId, authentication, hasAdminAccess,
                id -> commentRepoPersonal.findByPostIdOrderByCreatedAtDesc(id),
                (id, userId) -> commentRepoPersonal.findByPostIdAndUserOrRecipient(id, userId),
                (id, userId) -> commentRepoPersonal.markMessagesAsRead(id, userId)
        );
        log.debug("Загружены приватные комментарии для поста, hashId: {}, количество: {}", hashId, commentsPersonal.size());

        // Добавление рейтинга продавца
        addSellerRatingToModel(post, model, ratingRepository);

        // Настройка модели
        model.addAttribute("postwo", Collections.singletonList(post));
        model.addAttribute("commentsPersonal", commentsPersonal);
        model.addAttribute("isPrivateTabActive", isPrivateTabActive);
        model.addAttribute("currentUser", authentication != null ? authentication.getPrincipal() : null);
        model.addAttribute("commentRepoPersonal", commentRepoPersonal);

        log.info("Отображена детальная страница товара, hashId: {}", hashId);
        return "postidviews";
    }

    /**
     * Отмечает сообщения в переписке как прочитанные.
     * Используется для уведомлений о непрочитанных сообщениях.
     *
     * @param postId         ID поста (не hashId!)
     * @param authentication данные аутентификации
     * @return ResponseEntity с результатом операции
     */
    @Transactional
    @PostMapping("/api/messages/{postId}/markAsRead")
    @ResponseBody
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long postId, Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            commentRepoPersonal.markMessagesAsRead(postId, currentUser.getId());
            log.debug("Отмечены сообщения как прочитанные, postId: {}", postId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при отметке сообщений как прочитанных, postId: {}, errorType: {}", postId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}