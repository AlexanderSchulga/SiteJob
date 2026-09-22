package jobforsite.sitejob.module.resume.controller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.auth.service.EmailService;
import jobforsite.sitejob.module.resume.model.CommentPersonalRes;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.config.categoryconfig.ExperienceResumCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostResumeCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.WorkFormatResumeConfig;
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
 * Контроллер для управления резюме (Resume).
 * <p>
 * Обрабатывает запросы на просмотр списка резюме с фильтрацией и пагинацией,
 * создание новых резюме, просмотр детальной страницы резюме с комментариями,
 * а также отметку сообщений как прочитанных.
 * </p>
 */
@RequiredArgsConstructor
@Controller
public class RefControllerRes extends BasePostController {

    private static final Logger log = LoggerFactory.getLogger(RefControllerRes.class);

    private final PostResumeRepository postResumeRepository;
    private final CommentRepoResPersonal commentRepoResPersonal;
    private final PostResumeCategoryConfig postResumeCategoryConfig;
    private final ExperienceResumCategoryConfig experienceResumCategoryConfig;

    private final WorkFormatResumeConfig workFormatResumeConfig;

    private final ImageService imageService;
    private final RatingRepository ratingRepository;
    private final RegionsConfig regionsConfig;
    private final RateLimitService rateLimitService;
    private final PostCacheService postCacheService;
    private final PostViewService postViewService;

    private final EmailService emailService;

    /**
     * Отображает страницу со списком резюме с фильтрацией, поиском и пагинацией.
     * <p>
     * Выполняет проверку rate limit для просмотра резюме, получает отфильтрованный
     * список резюме из кэша, добавляет в модель данные для фильтров и отображает
     * страницу "page_one/viewsoneres".
     * </p>
     *
     * @param category       категория резюме (опционально)
     * @param experience     требуемый опыт (опционально)
     * @param workformat    формат работы (опционально)
     * @param search         поисковый запрос (опционально)
     * @param sort           тип сортировки (по умолчанию "newest")
     * @param priceSort      сортировка по цене (по умолчанию "none")
     * @param region         регион (опционально)
     * @param city           город (опционально)
     * @param page           номер страницы (по умолчанию 0)
     * @param size           размер страницы (по умолчанию 8)
     * @param authentication объект аутентификации Spring Security
     * @param request        HTTP-запрос (для получения IP)
     * @param model          модель Spring MVC
     * @return имя шаблона для отображения
     */
    @GetMapping("/page_one/viewsoneres")
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

        log.debug("Запрос списка резюме: category={}, experience={}, workformat={}, search={}, region={}, city={}, page={}, size={}",
                category, experience, workformat, search, region, city, page, size);

        // Очистка кэша резюме (закомментировано, но оставлено для возможности включения)
        // postCacheService.clearResumeCache();

        String clientIp = getClientIp(request);

        // Проверка rate limit для просмотра резюме
        if (!checkRateLimitForViewResume(clientIp, rateLimitService, "resume", model, "Резюме",
                postResumeCategoryConfig.getCategories(),
                experienceResumCategoryConfig.getExperience(),
                workFormatResumeConfig.getWorkformat(),
                regionsConfig)) {
            log.debug("Rate limit превышен для просмотра резюме, IP: {}", clientIp);
            return "page_one/viewsoneres";
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

        // Получение списка резюме из кэша
        List<Resume> res = postCacheService.getResumeList(
                category, experience, workformat, search, region, city, sort, priceSort, page, size
        );

        // Получение общего количества резюме из кэша
        long totalItems = postCacheService.getResumeCount(category, experience, workformat, search, region, city);

        // Настройка модели для списка резюме
        setupResumListModel(model, res, totalItems, page, size, "Резюме",
                postResumeCategoryConfig.getCategories(),
                experienceResumCategoryConfig.getExperience(),
                workFormatResumeConfig.getWorkformat(),
                regionsConfig,
                category,
                normalizedExperience,
                normalizedWorkformat,
                region, city, sort, priceSort, authentication);

        // Добавление дополнительных атрибутов в модель
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("res", res);
        model.addAttribute("experienceResumCategoryConfig", experienceResumCategoryConfig);
        model.addAttribute("workFormatResumeConfig", workFormatResumeConfig);
        model.addAttribute("postResumeCategoryConfig", postResumeCategoryConfig);
        model.addAttribute("experienceConfig", experienceResumCategoryConfig);
        model.addAttribute("workformatConfig", workFormatResumeConfig);

        log.debug("Успешно загружено {} резюме из {} всего", res.size(), totalItems);
        return "page_one/viewsoneres";
    }

    /**
     * Отображает страницу создания нового резюме.
     * <p>
     * Добавляет в модель списки категорий и вариантов опыта для формы создания.
     * </p>
     *
     * @param model модель Spring MVC
     * @return имя шаблона страницы создания резюме
     */
    @GetMapping("/page_two/viewstwores")
    public String viewsTwoPage(Model model) {
        log.debug("Отображение страницы создания резюме");

        Iterable<Resume> res = postResumeRepository.findAll();
        model.addAttribute("title", "Создание");
        model.addAttribute("res", res);
        model.addAttribute("categories", postResumeCategoryConfig.getCategories());
        model.addAttribute("experience", experienceResumCategoryConfig.getExperience());
        model.addAttribute("workformat", workFormatResumeConfig.getWorkformat());

        return "page_two/viewstwores";
    }

    /**
     * Обрабатывает создание нового резюме.
     * <p>
     * Выполняет проверку rate limit для создания, загружает изображения,
     * сохраняет резюме в базе данных и очищает кэш.
     * </p>
     *
     * @param title          заголовок резюме
     * @param cash           желаемая зарплата
     * @param full_text      полное описание резюме
     * @param category       категория резюме
     * @param experience     опыт работы
     * @param workformat    формат работы
     * @param moderation     флаг модерации (true - требуется модерация)
     * @param files          массив загружаемых изображений
     * @param authentication объект аутентификации Spring Security
     * @param request        HTTP-запрос (для получения IP)
     * @param model          модель Spring MVC
     * @return перенаправление на главную страницу или повторное отображение формы при ошибке
     * @throws IOException при ошибках загрузки изображений
     */
    @PostMapping("/page_two/viewstwores")
    public String viewsTwoPost(
            @RequestParam String title,
            @RequestParam Integer cash,
            @RequestParam String full_text,
            @RequestParam String category,
            @RequestParam String experience,
            @RequestParam String workformat,
            @RequestParam Boolean moderation,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication,
            HttpServletRequest request,
            Model model) throws IOException {

        log.debug("Создание нового резюме с категорией: {}, опытом: {}", category, experience);

        String clientIp = getClientIp(request);

        // Проверка rate limit для создания резюме
        if (!checkRateLimitForCreateResume(clientIp, rateLimitService, "resume", model,
                postResumeCategoryConfig.getCategories(), experienceResumCategoryConfig.getExperience(),
                workFormatResumeConfig.getWorkformat(), "page_two/viewstwores")) {
            log.warn("Rate limit превышен для создания резюме, IP: {}", clientIp);
            return "page_two/viewstwores";
        }

        // Загрузка изображений
        try {
            uploadImages(files, null, null, null, null, model, "page_two/viewstwores");
        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации изображений при создании резюме: {}", e.getMessage());
            model.addAttribute("categories", postResumeCategoryConfig.getCategories());
            return "page_two/viewstwores";
        }

        try {
            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            // Создание и сохранение резюме
            Resume res = new Resume(title, cash, full_text, category, experience, workformat, moderation);
            User user = (User) authentication.getPrincipal();
            res.setUser(user);

            Resume savedPost = postResumeRepository.save(res);
            log.debug("Резюме сохранено в базе данных с hashId: {}", savedPost.getHashId());


            // ========== АВТОМАТИЧЕСКАЯ ОТПРАВКА УВЕДОМЛЕНИЯ ==========
            if (savedPost.isModeration()) {
                emailService.sendModerationNotification(
                        "resume",
                        savedPost.getTitle(),
                        savedPost.getUser().getNameuse(),
                        savedPost.getUser().getRegion(),
                        savedPost.getCreatedAt()
                );
            }

            // Сохранение изображений
            imageService.saveImages(files, null, null, null, savedPost);

            // Очистка кэша резюме (чтобы новые резюме отображались в списке)
            postCacheService.clearResumeCache();
            log.debug("Кэш резюме очищен после создания резюме");

            return "redirect:/index";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postResumeCategoryConfig.getCategories());
            return "page_two/viewstwores";
        } catch (Exception e) {
            log.error("Ошибка при создании резюме, errorType: {}", e.getClass().getSimpleName(), e);
            return handleCreationErrorResume(e, model, postResumeCategoryConfig.getCategories(),
                    experienceResumCategoryConfig.getExperience(), workFormatResumeConfig.getWorkformat(),
                    "page_two/viewstwores");
        }
    }

    /**
     * Отображает детальную страницу резюме по его hashId.
     * <p>
     * Регистрирует просмотр резюме (для статистики), проверяет доступ к посту,
     * загружает личные комментарии с учетом прав пользователя и добавляет рейтинг продавца.
     * </p>
     *
     * @param hashId         уникальный идентификатор резюме
     * @param model          модель Spring MVC
     * @param authentication объект аутентификации Spring Security (может быть null для неавторизованных)
     * @param request        HTTP-запрос (для регистрации просмотра)
     * @return имя шаблона детальной страницы резюме
     * @throws ResponseStatusException если резюме не найдено или недоступно
     */
    @GetMapping("/postidviewsres/{hashId}")
    public String viewsTwoDetails(@PathVariable String hashId, Model model, Authentication authentication, HttpServletRequest request) {
        log.debug("Запрос детальной страницы резюме с hashId: {}", hashId);

        // Поиск резюме по hashId
        Resume res = postResumeRepository.findByHashId(hashId)
                .orElseThrow(() -> {
                    log.warn("Резюме с hashId: {} не найдено", hashId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
                });

        // Регистрация просмотра (работает и для авторизованных, и для неавторизованных)
        User currentUser = authentication != null && authentication.isAuthenticated()
                ? (User) authentication.getPrincipal()
                : null;
        postViewService.registerResumeView(res, request, currentUser);
        log.debug("Зарегистрирован просмотр резюме hashId: {}", hashId);

        // Проверка доступа к посту
        if (!checkPostAccessAndWarnings(res, authentication, model)) {
            log.warn("Доступ к резюме hashId: {} запрещен для текущего пользователя", hashId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден или недоступен");
        }

        Long resId = res.getId();
        boolean isPrivateTabActive = false;
        boolean hasAdminAccess = hasSuperAccess(authentication);

        // Настройка приватных комментариев с учетом прав доступа
        List<CommentPersonalRes> commentsPersonalRes = setupPrivateComments(
                res, resId, authentication, hasAdminAccess,
                id -> commentRepoResPersonal.findByResumeIdOrderByCreatedAtDesc(id),
                (id, userId) -> commentRepoResPersonal.findByResumeIdAndUserOrRecipient(id, userId),
                (id, userId) -> commentRepoResPersonal.markMessagesAsRead(id, userId)
        );
        log.debug("Загружено {} личных комментариев для резюме hashId: {}", commentsPersonalRes.size(), hashId);

        // Добавление рейтинга продавца в модель
        addSellerRatingToModel(res, model, ratingRepository);

        // Заполнение модели данными
        model.addAttribute("postwores", Collections.singletonList(res));
        model.addAttribute("commentsPersonalRes", commentsPersonalRes);
        model.addAttribute("isPrivateTabActive", isPrivateTabActive);
        model.addAttribute("currentUser", authentication != null ? authentication.getPrincipal() : null);
        model.addAttribute("commentRepoResPersonal", commentRepoResPersonal);

        return "postidviewsres";
    }

    /**
     * Отмечает все сообщения в резюме как прочитанные для текущего пользователя.
     * <p>
     * Используется для приватных комментариев/сообщений. Обновляет статус прочтения
     * в базе данных и возвращает успешный ответ.
     * </p>
     *
     * @param resId          идентификатор резюме (поста)
     * @param authentication объект аутентификации Spring Security
     * @return ResponseEntity с HTTP-статусом 200 (OK) при успехе, 500 при ошибке
     */
    @Transactional
    @PostMapping("/api/messages/res/{resId}/markAsRead")
    @ResponseBody
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long resId, Authentication authentication) {
        log.debug("Запрос на отметку сообщений как прочитанных для резюме id: {}", resId);

        try {
            User currentUser = (User) authentication.getPrincipal();
            commentRepoResPersonal.markMessagesAsRead(resId, currentUser.getId());
            log.debug("Сообщения для резюме id: {} отмечены как прочитанные", resId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Ошибка при отметке сообщений как прочитанных для резюме id: {}, errorType: {}", resId, e.getClass().getSimpleName(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}