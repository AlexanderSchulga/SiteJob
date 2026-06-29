package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextJobCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostVacancyCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.service.UserCacheService;
import jobforsite.sitejob.module.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Контроллер для отображения объявлений пользователя.
 * Обрабатывает запросы на просмотр всех постов (товары, услуги, вакансии) пользователя
 * и подгрузку дополнительных элементов через load-more.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class AllPostUserController extends BaseController {

    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final UserCacheService userCacheService;
    private final RegionsConfig regionsConfig;

    /**
     * Отображает страницу со всеми объявлениями пользователя (товары, услуги, вакансии).
     * Поддерживает пагинацию для каждого типа объявлений отдельно.
     *
     * @param userHash хеш-идентификатор пользователя
     * @param productPage номер страницы для товаров
     * @param servicePage номер страницы для услуг
     * @param vacancyPage номер страницы для вакансий
     * @param size количество элементов на странице
     * @param authentication данные аутентификации текущего пользователя
     * @param request HTTP-запрос для получения IP
     * @param model MVC для передачи данных в представление
     * @return имя шаблона или страница ошибки
     */
    @GetMapping("/user/{userHash}/posts")
    public String userAllPosts(
            @PathVariable String userHash,
            @RequestParam(value = "productPage", defaultValue = "0") int productPage,
            @RequestParam(value = "servicePage", defaultValue = "0") int servicePage,
            @RequestParam(value = "vacancyPage", defaultValue = "0") int vacancyPage,
            @RequestParam(value = "size", defaultValue = "8") int size,
            Authentication authentication,
            HttpServletRequest request,
            Model model) {

        // Rate limiting проверка
        String clientIp = getClientIp(request);
        if (rateLimitService.isIpBlocked(clientIp)) {
            log.warn("Rate limit: IP blocked - {}", clientIp);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "IP временно заблокирован");
        }

        if (!rateLimitService.tryConsumeProductView(clientIp)) {
            rateLimitService.recordFailedAttempt(clientIp);
            log.warn("Rate limit: too many product view requests from IP - {}", clientIp);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Слишком много запросов");
        }

        try {
            // Находим пользователя через сервис кэширования
            User targetUser = userCacheService.findUserByHashCached(userHash);
            if (targetUser == null) {
                log.warn("User not found by hash: {}", userHash);
                model.addAttribute("error", "Пользователь не найден");
                return "error";
            }

            log.debug("Loading posts page for user hash: {}", userHash);

            // Получаем данные через сервис кэширования
            UserCacheService.UserPostsData postsData = userCacheService.getUserPostsData(
                    targetUser, productPage, servicePage, vacancyPage, size);

            if (authentication != null && authentication.isAuthenticated()) {
                User currentUser = (User) authentication.getPrincipal();
                model.addAttribute("currentUser", currentUser);
            } else {
                model.addAttribute("currentUser", null);
            }

            // Добавляем данные в модель
            model.addAttribute("posts", postsData.getPosts());
            model.addAttribute("postsJob", postsData.getPostsJob());
            model.addAttribute("vacancies", postsData.getVacancies());
            model.addAttribute("totalProducts", postsData.getTotalProducts());
            model.addAttribute("totalServices", postsData.getTotalServices());
            model.addAttribute("totalVacancies", postsData.getTotalVacancies());
            model.addAttribute("targetUser", targetUser);
            model.addAttribute("userRating", userService.getUserRatingHash(targetUser.getHashId()));
            model.addAttribute("title", "Объявления пользователя " + targetUser.getNameuse());
            model.addAttribute("hasMoreProducts", postsData.isHasMoreProducts());
            model.addAttribute("hasMoreServices", postsData.isHasMoreServices());
            model.addAttribute("hasMoreVacancies", postsData.isHasMoreVacancies());
            model.addAttribute("currentProductPage", productPage);
            model.addAttribute("currentServicePage", servicePage);
            model.addAttribute("currentVacancyPage", vacancyPage);
            model.addAttribute("pageSize", size);
            model.addAttribute("userHash", userHash);
            model.addAttribute("productCategoryConfig", postTextCategoryConfig);
            model.addAttribute("serviceCategoryConfig", postTextJobCategoryConfig);
            model.addAttribute("vacancyCategoryConfig", postVacancyCategoryConfig);
            model.addAttribute("regionsConfig", regionsConfig);


            return "user/user_posts";

        } catch (Exception e) {
            log.error("Error loading user posts for hash: {}", userHash, e);
            model.addAttribute("error", "Ошибка загрузки объявлений: " + e.getMessage());
            return "error";
        }
    }

    /**
     * AJAX-эндпоинт для подгрузки дополнительных объявлений (load-more).
     * Поддерживает три типа: products, services, vacancies.
     *
     * @param userHash хеш-идентификатор пользователя
     * @param type тип объявлений (products, services, vacancies)
     * @param page номер запрашиваемой страницы
     * @param size количество элементов на странице
     * @param request HTTP-запрос для получения IP
     * @param model MVC для передачи данных во фрагмент
     * @return фрагмент Thymeleaf с постами или фрагмент ошибки
     */
    @GetMapping("/user/{userHash}/load-more")
    public String loadMoreUserPosts(
            @PathVariable("userHash") String userHash,
            @RequestParam("type") String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "8") int size,
            HttpServletRequest request,
            Model model) {

        // Rate limiting проверка
        String clientIp = getClientIp(request);
        if (rateLimitService.isIpBlocked(clientIp)) {
            log.warn("Load-more: IP blocked - {}", clientIp);
            return "fragments/error_fragment :: errorFragment(errorMessage='IP временно заблокирован')";
        }

        boolean allowed = false;
        switch (type) {
            case "products":
                allowed = rateLimitService.tryConsumeProductLoad(clientIp);
                break;
            case "services":
                allowed = rateLimitService.tryConsumeServiceLoad(clientIp);
                break;
            case "vacancies":
                allowed = rateLimitService.tryConsumeVacancyLoad(clientIp);
                break;
        }

        if (!allowed) {
            rateLimitService.recordFailedAttempt(clientIp);
            log.warn("Load-more: rate limit exceeded for type {} from IP - {}", type, clientIp);
            return "fragments/error_fragment :: errorFragment(errorMessage='Слишком много запросов')";
        }

        try {
            // Находим пользователя через сервис кэширования
            User targetUser = userCacheService.findUserByHashCached(userHash);
            if (targetUser == null) {
                log.warn("Load-more: user not found by hash - {}", userHash);
                return "fragments/error_fragment :: errorFragment(errorMessage='Пользователь не найден')";
            }

            log.debug("Load-more: type={}, page={}, size={}, userHash={}", type, page, size, userHash);

            switch (type) {
                case "products":
                    Page<PostText> postsPage = userCacheService.getProductsPage(targetUser, page, size);
                    model.addAttribute("posts", postsPage.getContent());
                    model.addAttribute("hasMore", postsPage.hasNext());
                    break;
                case "services":
                    Page<PostTextJob> postsJobPage = userCacheService.getServicesPage(targetUser, page, size);
                    model.addAttribute("postsJob", postsJobPage.getContent());
                    model.addAttribute("hasMore", postsJobPage.hasNext());
                    break;
                case "vacancies":
                    Page<Vacancy> vacanciesPage = userCacheService.getVacanciesPage(targetUser, page, size);
                    model.addAttribute("vacancies", vacanciesPage.getContent());
                    model.addAttribute("hasMore", vacanciesPage.hasNext());
                    break;
            }

            model.addAttribute("postTextCategoryConfig", postTextCategoryConfig);
            model.addAttribute("postTextJobCategoryConfig", postTextJobCategoryConfig);
            model.addAttribute("postVacancyCategoryConfig", postVacancyCategoryConfig);
            model.addAttribute("type", type);
            model.addAttribute("currentPage", page);
            model.addAttribute("userHash", userHash);
            model.addAttribute("targetUser", targetUser);

            return "fragments/user_posts_fragment :: postsFragment";

        } catch (Exception e) {
            log.error("Load-more error for user hash: {}, type: {}", userHash, type, e);
            return "fragments/error_fragment :: errorFragment(errorMessage='Ошибка загрузки данных')";
        }
    }

    // Получение реального IP-адреса клиента с учетом прокси
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}