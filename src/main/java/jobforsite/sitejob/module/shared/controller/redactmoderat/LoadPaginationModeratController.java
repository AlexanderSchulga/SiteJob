package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.dto.ResumeBlockedDTO;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.dto.ModerationSliceResponse;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.shared.services.util.CategoryUtils;
import jobforsite.sitejob.module.shared.dto.PaginationSliceResponseDTO;
import jobforsite.sitejob.module.product.dto.PostTextBlockedDTO;
import jobforsite.sitejob.module.service.dto.PostTextJobBlockedDTO;
import jobforsite.sitejob.module.shared.services.util.ExperienceUtils;
import jobforsite.sitejob.module.shared.services.util.WorkformatUtils;
import jobforsite.sitejob.module.vacancy.dto.VacancyBlockedDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для управления модерацией объявлений.
 * Поддерживает типы: product (товар), service (услуга), vacancy (вакансия), resume (резюме).
 * <p>
 * Основные функции:
 * - Отображение главной страницы модерации со статистикой
 * - Одобрение/публикация объявлений (toggle moderation)
 * - Пагинированная загрузка объявлений на модерации (AJAX)
 * - Конвертация сущностей в DTO для передачи на фронтенд
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginationModeratController {

    private final CategoryUtils categoryUtils;
    private final ExperienceUtils experienceUtils;

    private final WorkformatUtils workformatUtils;

    private final ModerationPostsCacheService moderationCacheService;
    private final RegionsConfig regionsConfig;
    private final ModerationPostsCacheService moderationPostsCacheService;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final ImageService imageService;
    private final PostCacheService postCacheService;

    // ==================== ГЛАВНАЯ СТРАНИЦА МОДЕРАЦИИ ====================

    /**
     * Отображает главную страницу модерации.
     * Доступ только для SUPER и ADMIN.
     * Показывает количество объявлений, ожидающих модерации (из кэша).
     *
     * @param model          модель для передачи данных в представление
     * @param authentication данные текущего пользователя
     * @return имя шаблона или страница ошибки
     */
    @GetMapping("/page_two/moderat")
    public String moderatMain(Model model, Authentication authentication) {
        log.info("Запрос главной страницы модерации");
        try {
            User currentUser = (User) authentication.getPrincipal();

            // Разрешаем доступ как SUPER так и ADMIN
            boolean hasModerationAccess = "ROLE_SUPER".equals(currentUser.getRole()) ||
                    "ROLE_ADMIN".equals(currentUser.getRole());

            if (!hasModerationAccess) {
                log.warn("Доступ к модерации запрещен: недостаточно прав");
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // Получаем КОЛИЧЕСТВО постов ожидающих модерации (из кэша)
            long moderPostCount = moderationPostsCacheService.getModerationProductsCount();
            long moderPostsJobCount = moderationPostsCacheService.getModerationServicesCount();
            long moderVacanciesCount = moderationPostsCacheService.getModerationVacanciesCount();
            long moderResumeCount = moderationPostsCacheService.getModerationResumeCount();

            log.debug("Статистика модерации (из кэша): товары={}, услуги={}, вакансии={}, резюме={}",
                    moderPostCount, moderPostsJobCount, moderVacanciesCount, moderResumeCount);

            // Добавляем статистику
            model.addAttribute("moderPostCount", moderPostCount);
            model.addAttribute("moderPostsJobCount", moderPostsJobCount);
            model.addAttribute("moderVacanciesCount", moderVacanciesCount);
            model.addAttribute("moderResumeCount", moderResumeCount);

            model.addAttribute("title", "Ожидающие модерации");
            model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));

            // Добавляем начальные значения для бейджей
            model.addAttribute("initialProductsBadge", moderPostCount > 0 ? moderPostCount : 0);
            model.addAttribute("initialServicesBadge", moderPostsJobCount > 0 ? moderPostsJobCount : 0);
            model.addAttribute("initialVacanciesBadge", moderVacanciesCount > 0 ? moderVacanciesCount : 0);
            model.addAttribute("initialResumeBadge", moderResumeCount > 0 ? moderResumeCount : 0);

            return "page_two/moderat";

        } catch (Exception e) {
            log.warn("Ошибка загрузки главной страницы модерации: {}", e.getMessage());
            model.addAttribute("error", "Ошибка загрузки постов для модерации");
            return "error";
        }
    }

    // ==================== ОДОБРЕНИЕ/ПУБЛИКАЦИЯ ОБЪЯВЛЕНИЙ ====================

    /** Одобрение товара по hashId */
    @PostMapping("/page_two/moderat/{hashId}/toggle-moder")
    public String toggleModerPost(@PathVariable(value = "hashId") String hashId,
                                  @RequestParam(value = "premium", required = false, defaultValue = "false") boolean premium,
                                  @RequestParam(value = "premiumPriority", required = false, defaultValue = "0") int premiumPriority,
                                  @RequestParam(value = "donate", required = false, defaultValue = "false") boolean donate,
                                  Authentication authentication,
                                  Model model) {
        log.info("Запрос на одобрение товара, hashId: {}, premium: {}, priority: {}", hashId, premium, premiumPriority);
        return toggleModer(hashId, premium, premiumPriority, donate, authentication, model, "product");
    }

    /** Одобрение услуги по hashId */
    @PostMapping("/page_two/moderat/job/{hashId}/toggle-moder")
    public String toggleModerJob(@PathVariable(value = "hashId") String hashId,
                                 @RequestParam(value = "premium", required = false, defaultValue = "false") boolean premium,
                                 @RequestParam(value = "premiumPriority", required = false, defaultValue = "0") int premiumPriority,
                                 @RequestParam(value = "donate", required = false, defaultValue = "false") boolean donate,
                                 Authentication authentication,
                                 Model model) {
        log.info("Запрос на одобрение услуги, hashId: {}, premium: {}, priority: {}", hashId, premium, premiumPriority);
        return toggleModer(hashId, premium, premiumPriority, donate, authentication, model, "service");
    }

    /** Одобрение вакансии по hashId */
    @PostMapping("/page_two/moderat/vac/{hashId}/toggle-moder")
    public String toggleModerVacancy(@PathVariable(value = "hashId") String hashId,
                                     @RequestParam(value = "premium", required = false, defaultValue = "false") boolean premium,
                                     @RequestParam(value = "premiumPriority", required = false, defaultValue = "0") int premiumPriority,
                                     @RequestParam(value = "donate", required = false, defaultValue = "false") boolean donate,
                                     Authentication authentication,
                                     Model model) {
        log.info("Запрос на одобрение вакансии, hashId: {}, premium: {}, priority: {}", hashId, premium, premiumPriority);
        return toggleModer(hashId, premium, premiumPriority, donate, authentication, model, "vacancy");
    }

    /** Одобрение резюме по hashId */

    @PostMapping("/page_two/moderat/res/{hashId}/toggle-moder")
    public String toggleModerResume(@PathVariable(value = "hashId") String hashId,
                                     Authentication authentication,
                                     Model model) {
        log.info("Запрос на одобрение резюме, hashId: {}",  hashId);
        return toggleModerRes(hashId, authentication, model);
    }

    private String toggleModerRes(String hashId,
                                  Authentication authentication, Model model) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            if (!currentUser.getRole().equals("ROLE_SUPER") && !currentUser.getRole().equals("ROLE_ADMIN")) {
                log.warn("Доступ запрещен при одобрении: недостаточно прав, hashId: {}", hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            Optional<Resume> optionalResume = postResumeRepository.findByHashId(hashId);
            if (optionalResume.isEmpty()) {
                log.warn("Резюме не найдено при одобрении, hashId: {}", hashId);
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }
            Resume res = optionalResume.get();
            imageService.deleteMarkedImagesForResume(res);
            res.setModeration(false);
            res.setArchived(false);
            res.setModeratedAt(LocalDateTime.now());
            postResumeRepository.save(res);
            log.info("Резюме одобрено, hashId: {}", hashId);


            // ОЧИЩАЕМ ВСЕ КЭШИ МОДЕРАЦИИ
            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Очищены все кэши модерации");

            return "redirect:/page_two/moderat";

        } catch (Exception e) {
            log.warn("Ошибка при одобрении типа: hashId: {}", hashId);
            model.addAttribute("error", "Ошибка при модерации");
            return "error";
        }
    }

    /**
     * Общий метод для одобрения объявления по hashId.
     * Снимает флаг модерации, удаляет помеченные на удаление изображения,
     * очищает кэши модерации и основные кэши.
     *
     * @param hashId         hashId объявления
     * @param premium        флаг платного объявления
     * @param premiumPriority приоритет платного объявления
     * @param authentication данные текущего пользователя
     * @param model          модель для передачи ошибок
     * @param type           тип объявления
     * @return редирект или страница ошибки
     */
    private String toggleModer(String hashId, boolean premium, int premiumPriority, boolean donate,
                               Authentication authentication, Model model, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            if (!currentUser.getRole().equals("ROLE_SUPER") && !currentUser.getRole().equals("ROLE_ADMIN")) {
                log.warn("Доступ запрещен при одобрении: недостаточно прав, тип: {}, hashId: {}", type, hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Товар не найден при одобрении, hashId: {}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostText post = optionalPost.get();

                imageService.deleteMarkedImagesForPost(post);

                post.setModeration(false);
                post.setArchived(false);
                post.setModeratedAt(LocalDateTime.now());
                // Устанавливаем флаги платного объявления
                post.setPremium(premium);
                post.setPremiumPriority(premiumPriority);
                post.setDonate(donate);
                postRepo.save(post);
                log.info("Товар одобрен, hashId: {}, premium: {}, priority: {}, donate: {}", hashId, premium, premiumPriority, donate);

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Услуга не найдена при одобрении, hashId: {}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostTextJob post = optionalPost.get();

                imageService.deleteMarkedImagesForPostJob(post);

                post.setModeration(false);
                post.setArchived(false);
                post.setModeratedAt(LocalDateTime.now());
                post.setPremium(premium);
                post.setPremiumPriority(premiumPriority);
                post.setDonate(donate);
                postRepoJob.save(post);
                log.info("Услуга одобрена, hashId: {}, premium: {}, priority: {}, donate: {}", hashId, premium, premiumPriority, donate);

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findByHashId(hashId);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Вакансия не найдена при одобрении, hashId: {}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Vacancy vacancy = optionalVacancy.get();

                imageService.deleteMarkedImagesForVacancy(vacancy);

                vacancy.setModeration(false);
                vacancy.setArchived(false);
                vacancy.setModeratedAt(LocalDateTime.now());
                vacancy.setPremium(premium);
                vacancy.setPremiumPriority(premiumPriority);
                vacancy.setDonate(donate);
                postVacancyRepository.save(vacancy);
                log.info("Вакансия одобрена, hashId: {}, premium: {}, priority: {}, donate: {}", hashId, premium, premiumPriority, donate);

            }

            // ОЧИЩАЕМ ВСЕ КЭШИ МОДЕРАЦИИ
            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Очищены все кэши модерации");

            // ОЧИЩАЕМ КЭШ ГЛАВНОЙ СТРАНИЦЫ (для обновления карусели)
            postCacheService.clearIndexCache();

            return "redirect:/page_two/moderat";

        } catch (Exception e) {
            log.warn("Ошибка при отклонении типа: {}, hashId: {}", type, hashId);
            model.addAttribute("error", "Ошибка при модерации");
            return "error";
        }
    }

    // Вспомогательный метод для очистки кэша
    private void clearMainCache(String type) {
        switch (type) {
            case "product":
                postCacheService.clearProductsCache();
                break;
            case "service":
                postCacheService.clearServicesCache();
                break;
            case "vacancy":
                postCacheService.clearVacanciesCache();
                break;
        }
    }

    // ==================== AJAX-ЗАГРУЗКА ОБЪЯВЛЕНИЙ ====================

    /**
     * Пагинированная загрузка объявлений на модерации (AJAX).
     * Поддерживает поиск по заголовку, тексту или имени пользователя.
     * Данные берутся из кэша.
     *
     * @param type         тип объявления (products, services, vacancies, resume)
     * @param page         номер страницы (0-based)
     * @param size         размер страницы
     * @param searchQuery  поисковый запрос (опционально)
     * @param searchType   тип поиска (all, title, text, username)
     * @param currentUser  текущий пользователь (из аннотации)
     * @return ResponseEntity с DTO пагинированного ответа
     */
    @GetMapping("/page_two/moderat/load-more")
    @ResponseBody
    public ResponseEntity<PaginationSliceResponseDTO<?>> loadMoreModerat(
            @RequestParam(value = "type", defaultValue = "products") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "searchType", defaultValue = "all") String searchType,
            @AuthenticationPrincipal User currentUser) {

        log.debug("Загрузка объявлений на модерации: type={}, page={}, size={}, searchQuery={}, searchType={}",
                type, page, size, searchQuery, searchType);

        try {
            String currentUserRole = currentUser != null ? currentUser.getRole() : null;

            boolean hasModerationAccess = currentUser != null &&
                    ("ROLE_SUPER".equals(currentUser.getRole()) ||
                            "ROLE_ADMIN".equals(currentUser.getRole()));

            if (!hasModerationAccess) {
                log.warn("Доступ запрещен при загрузке объявлений на модерации: role={}", currentUserRole);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Доступ запрещен"));
            }

            PaginationSliceResponseDTO<?> response;

            switch (type.toLowerCase()) {
                case "products":
                    // ИСПОЛЬЗУЕМ КЭШИРУЕМЫЙ МЕТОД
                    ModerationSliceResponse<PostTextBlockedDTO> productsResponse =
                            moderationCacheService.getModerationProductsDto(page, size, searchQuery, searchType);

                    response = createDtoSliceResponse(
                            productsResponse.getContent(),
                            productsResponse.isHasNext(),
                            page,
                            productsResponse.getNumberOfElements()
                    );
                    response.setTotalResults((int) moderationCacheService.getModerationProductsCount());
                    log.debug("Загружены товары на модерации: {} записей, hasNext={}",
                            productsResponse.getNumberOfElements(), productsResponse.isHasNext());
                    break;

                case "services":
                    ModerationSliceResponse<PostTextJobBlockedDTO> servicesResponse =
                            moderationCacheService.getModerationServicesDto(page, size, searchQuery, searchType);

                    response = createDtoSliceResponse(
                            servicesResponse.getContent(),
                            servicesResponse.isHasNext(),
                            page,
                            servicesResponse.getNumberOfElements()
                    );
                    response.setTotalResults((int) moderationCacheService.getModerationServicesCount());
                    log.debug("Загружены услуги на модерации: {} записей, hasNext={}",
                            servicesResponse.getNumberOfElements(), servicesResponse.isHasNext());
                    break;

                case "vacancies":
                    ModerationSliceResponse<VacancyBlockedDTO> vacanciesResponse =
                            moderationCacheService.getModerationVacanciesDto(page, size, searchQuery, searchType);

                    response = createDtoSliceResponse(
                            vacanciesResponse.getContent(),
                            vacanciesResponse.isHasNext(),
                            page,
                            vacanciesResponse.getNumberOfElements()
                    );
                    response.setTotalResults((int) moderationCacheService.getModerationVacanciesCount());
                    log.debug("Загружены вакансии на модерации: {} записей, hasNext={}",
                            vacanciesResponse.getNumberOfElements(), vacanciesResponse.isHasNext());
                    break;

                case "resume":
                    ModerationSliceResponse<ResumeBlockedDTO> resumeResponse =
                            moderationCacheService.getModerationResumeDto(page, size, searchQuery, searchType);

                    response = createDtoSliceResponse(
                            resumeResponse.getContent(),
                            resumeResponse.isHasNext(),
                            page,
                            resumeResponse.getNumberOfElements()
                    );
                    response.setTotalResults((int) moderationCacheService.getModerationResumeCount());
                    log.debug("Загружены резюме на модерации: {} записей, hasNext={}",
                            resumeResponse.getNumberOfElements(), resumeResponse.isHasNext());
                    break;

                default:
                    log.warn("Неизвестный тип для загрузки: {}", type);
                    response = createEmptyResponse(type);
            }

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                response.setSearchQuery(searchQuery);
                response.setSearchType(searchType);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("Ошибка при загрузке объявлений на модерации: type={}", type);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка при загрузке объявлений"));
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Создает DTO ответа для пагинированного списка DTO-объектов.
     */
    private <T> PaginationSliceResponseDTO<T> createDtoSliceResponse(
            List<T> content, boolean hasNext, int currentPage, int numberOfElements) {
        PaginationSliceResponseDTO<T> response = new PaginationSliceResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные модерации успешно загружены");
        response.setRedact(content);
        response.setHasMore(hasNext);
        response.setCurrentPage(currentPage);
        response.setSize(numberOfElements);
        return response;
    }

    // ==================== МЕТОДЫ КОНВЕРТАЦИИ В DTO ====================
    // методы конвертации используются для тестирования и в других местах,
    // но фактически данные берутся напрямую из кэша через ModerationSliceResponse.

    /**
     * Конвертирует список товаров в DTO для модерации.
     */
    private List<PostTextBlockedDTO> convertToPostTextModeratDTOs(List<PostText> posts,
                                                                  String currentUserRole) {
        return posts.stream()
                .map(post -> convertToPostTextModeratDTO(post, currentUserRole))
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует один товар в DTO для модерации.
     * SUPER-пользователям показывается id и username владельца, ADMIN - нет.
     */
    private PostTextBlockedDTO convertToPostTextModeratDTO(PostText post, String currentUserRole) {
        PostTextBlockedDTO dto = new PostTextBlockedDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName(categoryUtils.getCategoryDisplayName("product", post.getCategory()));
        dto.setLocation(post.getLocation());
        dto.setFirstImagePath(post.getFirstImagePath());
        dto.setFullText(post.getFull_text());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setModeratedAt(post.getModeratedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setBlocked(post.isBlocked());
        dto.setBlockedAt(post.getBlockedAt());
        dto.setModeration(post.isModeration());
        dto.setArchived(post.isArchived());

        if (post.getUser() != null) {
            PostTextBlockedDTO.UserDTO userDTO = new PostTextBlockedDTO.UserDTO();

            // Только SUPER видит id и username
            if ("ROLE_SUPER".equals(currentUserRole)) {
                userDTO.setId(post.getUser().getId());
                userDTO.setUsername(post.getUser().getUsername());
            } else {
                userDTO.setId(null);
                userDTO.setUsername(null);
            }

            userDTO.setNameuse(post.getUser().getNameuse());
            userDTO.setPhonePost(post.getUser().getPhonePost());
            userDTO.setRegion(post.getUser().getRegion());
            userDTO.setCity(post.getUser().getCity());
            userDTO.setRegionDisplayName(regionsConfig.getRegionDisplayName(post.getUser().getRegion()));
            dto.setUser(userDTO);
        }

        return dto;
    }

    /**
     * Конвертирует список услуг в DTO для модерации.
     */
    private List<PostTextJobBlockedDTO> convertToPostTextJobModeratDTOs(List<PostTextJob> posts, String currentUserRole) {
        return posts.stream()
                .map(post -> convertToPostTextJobModeratDTO(post, currentUserRole))
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует одну услугу в DTO для модерации.
     */
    private PostTextJobBlockedDTO convertToPostTextJobModeratDTO(PostTextJob post, String currentUserRole) {
        PostTextJobBlockedDTO dto = new PostTextJobBlockedDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName(categoryUtils.getCategoryDisplayName("service", post.getCategory()));
        dto.setLocation(post.getLocation());
        dto.setFirstImagePath(post.getFirstImagePath());
        dto.setFullText(post.getFull_text());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setModeratedAt(post.getModeratedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setBlocked(post.isBlocked());
        dto.setBlockedAt(post.getBlockedAt());
        dto.setModeration(post.isModeration());
        dto.setArchived(post.isArchived());

        if (post.getUser() != null) {
            PostTextJobBlockedDTO.UserDTO userDTO = new PostTextJobBlockedDTO.UserDTO();

            if ("ROLE_SUPER".equals(currentUserRole)) {
                userDTO.setId(post.getUser().getId());
                userDTO.setUsername(post.getUser().getUsername());
            } else {
                userDTO.setId(null);
                userDTO.setUsername(null);
            }

            userDTO.setNameuse(post.getUser().getNameuse());
            userDTO.setPhonePost(post.getUser().getPhonePost());
            userDTO.setRegion(post.getUser().getRegion());
            userDTO.setCity(post.getUser().getCity());
            userDTO.setRegionDisplayName(regionsConfig.getRegionDisplayName(post.getUser().getRegion()));
            dto.setUser(userDTO);
        }

        return dto;
    }

    /**
     * Конвертирует список вакансий в DTO для модерации.
     */
    private List<VacancyBlockedDTO> convertToVacancyModeratDTOs(List<Vacancy> vacancies, String currentUserRole) {
        return vacancies.stream()
                .map(vacancy -> convertToVacancyModeratDTO(vacancy, currentUserRole))
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует одну вакансию в DTO для модерации.
     */
    private VacancyBlockedDTO convertToVacancyModeratDTO(Vacancy vacancy, String currentUserRole) {
        VacancyBlockedDTO dto = new VacancyBlockedDTO();
        dto.setId(vacancy.getId());
        dto.setHashId(vacancy.getHashId());
        dto.setTitle(vacancy.getTitle());
        dto.setLocation(vacancy.getLocation());
        dto.setCash(vacancy.getCash());
        dto.setCategory(vacancy.getCategory());
        dto.setCategoryDisplayName(categoryUtils.getCategoryDisplayName("vacancy", vacancy.getCategory()));
        dto.setExperienDisplayName(experienceUtils.getExperienDisplayName("experienKey", vacancy.getExperience()));
        dto.setWorkformatDisplayName(workformatUtils.getWorkformatDisplayName("workformatKey", vacancy.getWorkformat()));
        dto.setFirstImagePath(vacancy.getFirstImagePath());
        dto.setFullText(vacancy.getFull_text());
        dto.setCreatedAt(vacancy.getCreatedAt());
        dto.setModeratedAt(vacancy.getModeratedAt());
        dto.setUpdatedAt(vacancy.getUpdatedAt());
        dto.setBlocked(vacancy.isBlocked());
        dto.setBlockedAt(vacancy.getBlockedAt());
        dto.setModeration(vacancy.isModeration());
        dto.setArchived(vacancy.isArchived());

        if (vacancy.getUser() != null) {
            VacancyBlockedDTO.UserDTO userDTO = new VacancyBlockedDTO.UserDTO();

            if ("ROLE_SUPER".equals(currentUserRole)) {
                userDTO.setId(vacancy.getUser().getId());
                userDTO.setUsername(vacancy.getUser().getUsername());
            } else {
                userDTO.setId(null);
                userDTO.setUsername(null);
            }

            userDTO.setNameuse(vacancy.getUser().getNameuse());
            userDTO.setPhonePost(vacancy.getUser().getPhonePost());
            userDTO.setRegion(vacancy.getUser().getRegion());
            userDTO.setCity(vacancy.getUser().getCity());
            userDTO.setRegionDisplayName(regionsConfig.getRegionDisplayName(vacancy.getUser().getRegion()));
            dto.setUser(userDTO);
        }

        return dto;
    }


    /**
     * Конвертирует список резюме в DTO для модерации.
     */
    private List<ResumeBlockedDTO> convertToResumeModeratDTOs(List<Resume> resume, String currentUserRole) {
        return resume.stream()
                .map(resumes -> convertToResumeModeratDTO(resumes, currentUserRole))
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует одно резюме в DTO для модерации.
     */
    private ResumeBlockedDTO convertToResumeModeratDTO(Resume resume, String currentUserRole) {
        ResumeBlockedDTO dto = new ResumeBlockedDTO();
        dto.setId(resume.getId());
        dto.setHashId(resume.getHashId());
        dto.setTitle(resume.getTitle());
        dto.setCash(resume.getCash());
        dto.setCategory(resume.getCategory());
        dto.setCategoryDisplayName(categoryUtils.getCategoryDisplayName("resume", resume.getCategory()));
        dto.setExperienDisplayName(experienceUtils.getExperienDisplayName("experienKey", resume.getExperience()));
        dto.setWorkformatDisplayName(workformatUtils.getWorkformatDisplayName("workformatKey", resume.getWorkformat()));
        dto.setFirstImagePath(resume.getFirstImagePath());
        dto.setFullText(resume.getFull_text());
        dto.setCreatedAt(resume.getCreatedAt());
        dto.setModeratedAt(resume.getModeratedAt());
        dto.setUpdatedAt(resume.getUpdatedAt());
        dto.setBlocked(resume.isBlocked());
        dto.setBlockedAt(resume.getBlockedAt());
        dto.setModeration(resume.isModeration());
        dto.setArchived(resume.isArchived());

        if (resume.getUser() != null) {
            ResumeBlockedDTO.UserDTO userDTO = new ResumeBlockedDTO.UserDTO();

            if ("ROLE_SUPER".equals(currentUserRole)) {
                userDTO.setId(resume.getUser().getId());
                userDTO.setUsername(resume.getUser().getUsername());
            } else {
                userDTO.setId(null);
                userDTO.setUsername(null);
            }

            userDTO.setNameuse(resume.getUser().getNameuse());
            userDTO.setPhonePost(resume.getUser().getPhonePost());
            userDTO.setRegion(resume.getUser().getRegion());
            userDTO.setCity(resume.getUser().getCity());
            userDTO.setRegionDisplayName(regionsConfig.getRegionDisplayName(resume.getUser().getRegion()));
            dto.setUser(userDTO);
        }

        return dto;
    }

    /**
     * Создает пагинированный ответ из Slice.
     */
    private <T> PaginationSliceResponseDTO<T> createSliceResponse(List<T> content, Slice<?> slice, int currentPage) {
        PaginationSliceResponseDTO<T> response = new PaginationSliceResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные модерации успешно загружены");
        response.setRedact(content);
        response.setHasMore(slice.hasNext());
        response.setCurrentPage(currentPage);
        response.setSize(slice.getNumberOfElements());
        return response;
    }

    /**
     * Создает пустой ответ для неизвестного типа.
     */
    private <T> PaginationSliceResponseDTO<T> createEmptyResponse(String type) {
        PaginationSliceResponseDTO<T> response = new PaginationSliceResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет данных для модерации типа: " + type);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setSize(0);
        return response;
    }

    /**
     * Создает ответ с ошибкой.
     */
    private <T> PaginationSliceResponseDTO<T> createErrorResponse(String errorMessage) {
        PaginationSliceResponseDTO<T> response = new PaginationSliceResponseDTO<>();
        response.setStatus("error");
        response.setMessage(errorMessage);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setSize(0);
        return response;
    }
}