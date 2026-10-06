package jobforsite.sitejob.module.product.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.product.dto.PostCreateRequestDTO;
import jobforsite.sitejob.module.product.dto.PostTextDetailDTO;
import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.mapper.PostDetailMapper;
import jobforsite.sitejob.module.product.model.CommentPersonal;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.service.FilterRangeService;
import jobforsite.sitejob.module.product.service.PostTextListService;
import jobforsite.sitejob.module.product.service.PostTypeResolver;
import jobforsite.sitejob.module.product.service.ProductService;
import jobforsite.sitejob.module.product.service.filter.FilterFactory;
import jobforsite.sitejob.module.product.service.filter.ProductFilter;
import jobforsite.sitejob.module.shared.config.categoryconfig.FilterConfigService;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.PostViewService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
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

    private final CommentRepoPersonal commentRepoPersonal;
    private final PostTextCategoryConfig postTextCategoryConfig;
    private final RegionsConfig regionsConfig;
    private final RateLimitService rateLimitService;
    private final PostViewService postViewService;
    private final FilterConfigService filterConfigService;

    private final ProductService productService;

    private final FilterRangeService filterRangeService;

    private final PostTextListService postTextListService;

    private final PostDetailMapper postDetailMapper;

    private final FilterFactory filterFactory;

    private final PostTypeResolver postTypeResolver;



    /**
     * Отображает страницу со списком товаров:
     * <ul>
     *   <li>Фильтрации по категории, региону, городу</li>
     *   <li>Поиска по названию</li>
     *   <li>Сортировки (новизна, цена)</li>
     *   <li>Пагинации</li>
     *   <li>Rate limiting для защиты от частых запросов</li>
     * </ul>
     *
     *
     * ModelAttribute ProductFilterDTO +

     * @param page           номер страницы (начиная с 0)
     * @param size           количество элементов на странице
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос (для получения IP)
     * @param model          модель для передачи данных в представление
     * @return имя шаблона страницы
     */
    @GetMapping("/page_one/viewsone")
    public String viewsOnePage(@ModelAttribute ProductFilterDTO filter,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "8") int size,
                               Authentication authentication,
                               HttpServletRequest request,
                               Model model) {

        String clientIp = getClientIp(request);

        // Нормализация параметров
        String normalizedCategory = filterRangeService.normalizeParam(filter.getCategory());
        String normalizedSearch = filterRangeService.normalizeParam(filter.getSearch());
        String normalizedRegion = filterRangeService.normalizeParam(filter.getRegion());
        String normalizedCity = filterRangeService.normalizeParam(filter.getCity());
        String sort = filter.getSort() != null ? filter.getSort() : "newest";
        String priceSort = filter.getPriceSort() != null ? filter.getPriceSort() : "none";

        // Обновляем DTO
        filter.setCategory(normalizedCategory);
        filter.setSearch(normalizedSearch);
        filter.setRegion(normalizedRegion);
        filter.setCity(normalizedCity);
        filter.setSort(sort);
        filter.setPriceSort(priceSort);

        // Rate limiting
        if (!checkRateLimitForView(clientIp, rateLimitService, "product", model, "Товары",
                postTextCategoryConfig.getCategories(), regionsConfig)) {
            return "page_one/viewsone";
        }

        // ===== ВЫБОР ФИЛЬТРА ЧЕРЕЗ ФАБРИКУ =====
        ProductFilter productFilter = filterFactory.getFilter(normalizedCategory);

        // ===== ПОЛУЧЕНИЕ ДАННЫХ =====
        List<? extends PostText> posts = productFilter.getPosts(filter, page, size);
        long totalItems = productFilter.getTotalCount(filter);
        boolean hasMore = posts.size() == size;

        // ===== ДОБАВЛЕНИЕ СПЕЦИФИЧНЫХ АТРИБУТОВ =====
        productFilter.addModelAttributes(filter, model);

        // ===== ПРЕОБРАЗОВАНИЕ В DTO =====
        List<PostTextListDTO> postListDTOs = postTextListService.toListDTO(posts);

        // ===== ОБЩИЕ АТРИБУТЫ =====
        Map<String, List<String>> citiesByGroup = getCitiesForMenu(normalizedRegion, regionsConfig);

        model.addAttribute("posts", postListDTOs);
        model.addAttribute("categories", postTextCategoryConfig.getCategories());
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("currentPage", page);
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("regions", regionsConfig.getRegions());
        model.addAttribute("filterService", filterConfigService);
        model.addAttribute("selectedCategory", normalizedCategory != null ? normalizedCategory : "all");
        model.addAttribute("selectedRegion", normalizedRegion != null ? normalizedRegion : "all");
        model.addAttribute("selectedCity", normalizedCity != null ? normalizedCity : "all");
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentPriceSort", priceSort);
        model.addAttribute("searchQuery", normalizedSearch);

        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("currentUser", authentication.getPrincipal());
        }

        log.info("Загружено {} товаров, всего: {}, страница: {}, категория: {}, фильтр: {}",
                posts.size(), totalItems, page, normalizedCategory, productFilter.getCategoryName());

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
        model.addAttribute("title", "Создание товара");
        model.addAttribute("categories", postTextCategoryConfig.getCategories());
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
     * @param authentication данные аутентификации
     * @param request        HTTP-запрос (для получения IP)
     * @param model          модель для передачи данных в представление
     * @return редирект на главную или возврат к форме с ошибкой
     * @throws IOException при ошибках загрузки файлов
     */

    @PostMapping("/page_two/viewstwo")
    public String viewsTwoPost(@ModelAttribute PostCreateRequestDTO request,
                               Authentication authentication,
                               HttpServletRequest httpRequest,
                               Model model) {

        String clientIp = getClientIp(httpRequest);

        // Rate limiting
        if (!checkRateLimitForCreate(clientIp, rateLimitService, "product", model,
                postTextCategoryConfig.getCategories(), "page_two/viewstwo")) {
            return "page_two/viewstwo";
        }

        try {
            User user = (User) authentication.getPrincipal();

            // Создаём пост через сервис с DTO
            MultipartFile[] files = request.getFiles();
            PostText post = productService.createPostFromDTO(user, request, files);

            log.info("Товар успешно создан: id={}, hashId={}, user={}",
                    post.getId(), post.getHashId(), user.getNameuse());

            return "redirect:/index";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при создании товара: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postTextCategoryConfig.getCategories());
            return "page_two/viewstwo";

        } catch (Exception e) {
            log.info("Ошибка при создании товара");
            model.addAttribute("error", "Произошла ошибка при создании товара. Попробуйте позже.");
            model.addAttribute("categories", postTextCategoryConfig.getCategories());
            return "page_two/viewstwo";
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
    public String viewsTwoDetails(@PathVariable String hashId,
                                  Model model,
                                  Authentication authentication,
                                  HttpServletRequest request) {
        log.debug("Запрос детальной страницы товара, hashId: {}", hashId);

        PostText post = postTypeResolver.loadPostByHashId(hashId);
        if (post == null) {
            log.warn("Пост не найден, hashId: {}", hashId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
        }

        // Регистрация просмотра
        User currentUser = authentication != null && authentication.isAuthenticated()
                ? (User) authentication.getPrincipal()
                : null;
        postViewService.registerProductView(post, request, currentUser);

        // Проверка доступа
        if (!checkPostAccessAndWarnings(post, authentication, model)) {
            log.warn("Доступ к посту запрещён, hashId: {}", hashId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
        }

        // ===== КОНВЕРТАЦИЯ В DTO =====
        PostTextDetailDTO postDetail = postDetailMapper.toDetailDTO(post);
        model.addAttribute("post", postDetail);

        // Приватные комментарии
        Long postId = post.getId();
        boolean hasAdminAccess = hasSuperAccess(authentication);

        List<CommentPersonal> commentsPersonal = setupPrivateComments(
                post, postId, authentication, hasAdminAccess,
                id -> commentRepoPersonal.findByPostIdOrderByCreatedAtDesc(id),
                (id, userId) -> commentRepoPersonal.findByPostIdAndUserOrRecipient(id, userId),
                (id, userId) -> commentRepoPersonal.markMessagesAsRead(id, userId)
        );

        model.addAttribute("commentsPersonal", commentsPersonal);
        model.addAttribute("currentUser", authentication != null ? authentication.getPrincipal() : null);
        model.addAttribute("commentRepoPersonal", commentRepoPersonal);

        return "postidviews";
    }

//    /**
//     * Отмечает сообщения в переписке как прочитанные.
//     * Используется для уведомлений о непрочитанных сообщениях.
//     *
//     * @param postId         ID поста (не hashId!)
//     * @param authentication данные аутентификации
//     * @return ResponseEntity с результатом операции
//     */
//    @Transactional
//    @PostMapping("/api/messages/{postId}/markAsRead")
//    @ResponseBody
//    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long postId, Authentication authentication) {
//        try {
//            User currentUser = (User) authentication.getPrincipal();
//            commentRepoPersonal.markMessagesAsRead(postId, currentUser.getId());
//            log.debug("Отмечены сообщения как прочитанные, postId: {}", postId);
//            return ResponseEntity.ok().build();
//        } catch (Exception e) {
//            log.error("Ошибка при отметке сообщений как прочитанных, postId: {}, errorType: {}", postId, e.getClass().getSimpleName(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }

}