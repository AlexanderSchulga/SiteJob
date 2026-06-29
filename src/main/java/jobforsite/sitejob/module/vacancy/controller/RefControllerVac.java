package jobforsite.sitejob.module.vacancy.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.auth.service.EmailService;
import jobforsite.sitejob.module.shared.config.categoryconfig.ExperienceVacancyCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostVacancyCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.WorkFormatVacancyConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.PostViewService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
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

@Slf4j
@RequiredArgsConstructor
@Controller
public class RefControllerVac extends BasePostController {

    private final PostVacancyRepository postVacancyRepository;
    private final CommentRepoVacPersonal commentRepoVacPersonal;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;

    private final ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;

    private final WorkFormatVacancyConfig workFormatVacancyConfig;
    private final ImageService imageService;
    private final RatingRepository ratingRepository;
    private final RegionsConfig regionsConfig;
    private final RateLimitService rateLimitService;
    private final PostCacheService postCacheService;
    private final PostViewService postViewService;

    private final EmailService emailService;

    /**
     * Отображает страницу со списком вакансий с фильтрацией, поиском и пагинацией.
     * Использует кэширование для списка вакансий и их количества.
     * Применяет rate limiting для ограничения частоты просмотров.
     *
     * @param category     категория вакансии (опционально)
     * @param experience     требуемый опыт (опционально)
     * @param workformat    формат работы (опционально)
     * @param search       поисковый запрос (опционально)
     * @param sort         сортировка (новые/старые)
     * @param priceSort    сортировка по цене
     * @param region       регион (опционально)
     * @param city         город (опционально)
     * @param page         номер страницы
     * @param size         размер страницы
     * @param authentication данные аутентификации пользователя
     * @param request      HTTP-запрос для получения IP клиента
     * @param model        модель для передачи данных в представление
     * @return имя шаблона для отображения списка вакансий
     */
    @GetMapping("/page_one/viewsonevac")
    public String viewsOnePage(@RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "experience", required = false) String experience,
                               @RequestParam(value = "workformat", required = false) String workformat,
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
        log.info("Запрос списка вакансий: IP={}, category={}, experience={}, workformat={}, search={}, region={}, city={}, page={}",
                clientIp, category, experience, workformat, search, region, city, page);

        // Проверка rate limit для просмотра вакансий
        if (!checkRateLimitForViewVacancy(clientIp, rateLimitService, "vacancy", model, "Вакансии",
                postVacancyCategoryConfig.getCategories(),
                experienceVacancyCategoryConfig.getExperience(),
                workFormatVacancyConfig.getWorkformat(),
                regionsConfig)) {
            log.warn("Превышен rate limit для просмотра вакансий: IP={}", clientIp);
            return "page_one/viewsonevac";
        }

        // Нормализация значения опыта
        String normalizedExperience = experience;
        if (normalizedExperience == null || normalizedExperience.isEmpty()) {
            normalizedExperience = "all";
        }

        String normalizedWorkformat = workformat;
        if (normalizedWorkformat == null || normalizedWorkformat.isEmpty()) {
            normalizedWorkformat = "all";
        }

        // Получение городов для меню
        Map<String, List<String>> citiesByGroup = getCitiesForMenu(region, regionsConfig);

        // Получение списка вакансий из кэша
        List<Vacancy> vacvac = postCacheService.getVacanciesList(
                category, experience, workformat, search, region, city, sort, priceSort, page, size
        );

        // Получение общего количества вакансий из кэша
        long totalItems = postCacheService.getVacanciesCount(category, experience, workformat, search, region, city);

        // Настройка общих атрибутов модели
        setupVacancyListModel(model, vacvac, totalItems, page, size, "Вакансии",
                postVacancyCategoryConfig.getCategories(),
                experienceVacancyCategoryConfig.getExperience(),
                workFormatVacancyConfig.getWorkformat(),
                regionsConfig,
                category,
                normalizedExperience,
                normalizedWorkformat,
                region, city, sort, priceSort, authentication);

        // Добавление специфических атрибутов
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("vacvac", vacvac);
        model.addAttribute("experienceVacancyCategoryConfig", experienceVacancyCategoryConfig);
        model.addAttribute("workFormatVacancyConfig", workFormatVacancyConfig);
        model.addAttribute("workformatConfig", workFormatVacancyConfig);
        model.addAttribute("postVacancyCategoryConfig", postVacancyCategoryConfig);
        model.addAttribute("experienceConfig", experienceVacancyCategoryConfig);

        return "page_one/viewsonevac";
    }


    /**
     * Отображает страницу создания новой вакансии.
     * Передаёт в модель все существующие вакансии и список категорий.
     *
     * @param model модель для передачи данных в представление
     * @return имя шаблона для создания вакансии
     */
    @GetMapping("/page_two/viewstwovac")
    public String viewsTwoPage(Model model) {
        Iterable<Vacancy> vacvac = postVacancyRepository.findAll();
        model.addAttribute("title", "Создание");
        model.addAttribute("vacvac", vacvac);
        model.addAttribute("categories", postVacancyCategoryConfig.getCategories());
        model.addAttribute("experience", experienceVacancyCategoryConfig.getExperience());
        model.addAttribute("workformat", workFormatVacancyConfig.getWorkformat());
        return "page_two/viewstwovac";
    }

    /**
     * Обрабатывает создание новой вакансии.
     * Выполняет rate limiting, загрузку изображений, сохранение в БД и очистку кэша.
     *
     * @param title          заголовок вакансии
     * @param cash           размер оплаты
     * @param full_text      полное описание
     * @param category       категория
     * @param experience     опыт работы
     * @param workformat    формат работы
     * @param location       местоположение
     * @param moderation     статус модерации
     * @param files          массив загружаемых файлов
     * @param authentication данные аутентификации пользователя
     * @param request        HTTP-запрос
     * @param model          модель для передачи данных
     * @return редирект на главную или возврат к форме при ошибке
     * @throws IOException при ошибках загрузки файлов
     */
    @PostMapping("/page_two/viewstwovac")
    public String viewsTwoPost(
            @RequestParam String title,
            @RequestParam Integer cash,
            @RequestParam String full_text,
            @RequestParam String category,
            @RequestParam String experience,
            @RequestParam String workformat,
            @RequestParam String location,
            @RequestParam Boolean moderation,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication,
            HttpServletRequest request,
            Model model) throws IOException {

        String clientIp = getClientIp(request);
        log.info("Создание новой вакансии: IP={}, title={}, category={}, опытом: {}", clientIp, title, category, experience);

        // Проверка rate limit для создания вакансии
        if (!checkRateLimitForCreateVacancy(clientIp, rateLimitService, "vacancy", model,
                postVacancyCategoryConfig.getCategories(), experienceVacancyCategoryConfig.getExperience(),
                workFormatVacancyConfig.getWorkformat(), "page_two/viewstwovac")) {
            log.warn("Превышен rate limit для создания вакансии: IP={}", clientIp);
            return "page_two/viewstwovac";
        }

        // Загрузка изображений
        try {
            uploadImages(files, null, null, null, null, model, "page_two/viewstwovac");
        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации изображений при создании вакансии: {}", e.getMessage());
            model.addAttribute("categories", postVacancyCategoryConfig.getCategories());
            return "page_two/viewstwovac";
        }

        try {
            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            Vacancy vacvac = new Vacancy(title, cash, full_text, category, experience, workformat, location, moderation);
            User user = (User) authentication.getPrincipal();
            vacvac.setUser(user);

            Vacancy savedPost = postVacancyRepository.save(vacvac);
            log.info("Вакансия успешно сохранена: ID={}", savedPost.getId());


            // ========== АВТОМАТИЧЕСКАЯ ОТПРАВКА УВЕДОМЛЕНИЯ ==========
            if (savedPost.isModeration()) {
                emailService.sendModerationNotification(
                        "vacancy",
                        savedPost.getTitle(),
                        savedPost.getUser().getNameuse(),
                        savedPost.getUser().getRegion(),
                        savedPost.getCreatedAt()
                );
            }


            imageService.saveImages(files, null, null, savedPost, null);
            postCacheService.clearVacanciesCache();
            log.info("Кэш вакансий очищен после создания");

            return "redirect:/index";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postVacancyCategoryConfig.getCategories());
            return "page_two/viewstwovac";
        } catch (Exception e) {
            log.error("Ошибка при сохранении вакансии, errorType: {}", e.getClass().getSimpleName(), e);
            return handleCreationErrorVacancy(e, model, postVacancyCategoryConfig.getCategories(),
                    experienceVacancyCategoryConfig.getExperience(), workFormatVacancyConfig.getWorkformat(),
                    "page_two/viewstwovac");
        }
    }

    /**
     * Отображает детальную страницу вакансии по её хэш-идентификатору.
     * Регистрирует просмотр, проверяет доступ, загружает приватные комментарии и рейтинг продавца.
     *
     * @param hashId         хэш-идентификатор вакансии
     * @param model          модель для передачи данных
     * @param authentication данные аутентификации пользователя (может быть null)
     * @param request        HTTP-запрос
     * @return имя шаблона детальной страницы вакансии
     * @throws ResponseStatusException если вакансия не найдена или недоступна
     */
    @GetMapping("/postidviewsvac/{hashId}")
    public String viewsTwoDetails(@PathVariable String hashId, Model model, Authentication authentication, HttpServletRequest request) {
        log.info("Запрос детальной страницы вакансии: hashId={}", hashId);

        Vacancy vacancy = postVacancyRepository.findByHashId(hashId)
                .orElseThrow(() -> {
                    log.warn("Вакансия не найдена: hashId={}", hashId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
                });

        // Регистрация просмотра (для авторизованных и неавторизованных пользователей)
        User currentUser = authentication != null && authentication.isAuthenticated()
                ? (User) authentication.getPrincipal()
                : null;
        postViewService.registerVacancyView(vacancy, request, currentUser);
        log.info("Зарегистрирован просмотр вакансии: ID={}", vacancy.getId());

        // Проверка доступа к посту
        if (!checkPostAccessAndWarnings(vacancy, authentication, model)) {
            log.warn("Доступ к вакансии запрещён: ID={}", vacancy.getId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
        }

        Long vacId = vacancy.getId();
        boolean isPrivateTabActive = false;
        boolean hasAdminAccess = hasSuperAccess(authentication);

        // Загрузка приватных комментариев
        List<CommentPersonalVac> commentsPersonalVac = setupPrivateComments(
                vacancy, vacId, authentication, hasAdminAccess,
                id -> commentRepoVacPersonal.findByVacancyIdOrderByCreatedAtDesc(id),
                (id, userId) -> commentRepoVacPersonal.findByVacancyIdAndUserOrRecipient(id, userId),
                (id, userId) -> commentRepoVacPersonal.markMessagesAsRead(id, userId)
        );

        // Добавление рейтинга продавца
        addSellerRatingToModel(vacancy, model, ratingRepository);

        // Наполнение модели атрибутами
        model.addAttribute("vactwo", Collections.singletonList(vacancy));
        model.addAttribute("commentsPersonalVac", commentsPersonalVac);
        model.addAttribute("isPrivateTabActive", isPrivateTabActive);
        model.addAttribute("currentUser", authentication != null ? authentication.getPrincipal() : null);
        model.addAttribute("commentRepoVacPersonal", commentRepoVacPersonal);

        return "postidviewsvac";
    }

    /**
     * API-эндпоинт для отметки сообщений как прочитанных в рамках вакансии.
     * Помечает все сообщения пользователя как прочитанные.
     *
     * @param postId         идентификатор вакансии
     * @param authentication данные аутентификации пользователя
     * @return HTTP-ответ с кодом 200 в случае успеха, иначе 500
     */
    @Transactional
    @PostMapping("/api/messages/vac/{postId}/markAsRead")
    @ResponseBody
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long postId, Authentication authentication) {
        log.info("Запрос на отметку сообщений как прочитанных: postId={}", postId);

        try {
            User currentUser = (User) authentication.getPrincipal();
            commentRepoVacPersonal.markMessagesAsRead(postId, currentUser.getId());
            log.info("Сообщения отмечены как прочитанные: postId={}", postId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при отметке сообщений как прочитанных: postId={}, errorType: {}", postId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(500).build();
        }
    }
}