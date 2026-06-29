package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.product.dto.PostTextBlockedDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.dto.ResumeBlockedDTO;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.dto.PostTextJobBlockedDTO;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.dto.PaginationSliceResponseDTO;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
import jobforsite.sitejob.module.shared.services.util.CategoryUtils;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.dto.VacancyBlockedDTO;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для управления заблокированными объявлениями (товары, услуги, вакансии, резюме).
 * Доступен только для пользователей с ролями SUPER и ADMIN.
 *
 * <p>Особенности:
 * <ul>
 *   <li>Просмотр списка заблокированных объявлений с пагинацией (Slice)</li>
 *   <li>Поиск среди заблокированных по заголовку или имени пользователя</li>
 *   <li>Блокировка/разблокировка объявлений (только для SUPER)</li>
 *   <li>Кэширование заблокированных записей через {@link BlockedPostsCacheService}</li>
 *   <li>API для получения статистики по заблокированным объявлениям</li>
 * </ul>
 *
 * @see PostTextBlockedDTO Заблокированные товары
 * @see PostTextJobBlockedDTO Заблокированные услуги
 * @see VacancyBlockedDTO Заблокированные вакансии
 * @see ResumeBlockedDTO Заблокированные резюме
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginatRedactBlockedController {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    private final CategoryUtils categoryUtils;
    private final BlockedPostsCacheService blockedPostsCacheService;
    private final RegionsConfig regionsConfig;
    private final FavorRepo favorRepo;

    /**
     * Эндпоинт для загрузки заблокированных объявлений с пагинацией и поиском.
     *
     * @param type       тип объявлений (products, services, vacancies, resume)
     * @param page       номер страницы (начиная с 0)
     * @param size       размер страницы (по умолчанию 8)
     * @param searchQuery поисковый запрос (опционально)
     * @param searchType тип поиска (title, nameuse, all)
     * @param currentUser текущий пользователь
     * @return ответ со списком заблокированных объявлений
     */
    @GetMapping("/page_two/redactorblocked/load-more")
    @ResponseBody
    public ResponseEntity<PaginationSliceResponseDTO<?>> loadMoreBlocked(
            @RequestParam(value = "type", defaultValue = "products") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "searchType", defaultValue = "all") String searchType,
            @AuthenticationPrincipal User currentUser) {

        long startTime = System.currentTimeMillis();
        String userRole = currentUser != null ? currentUser.getRole() : "null";
        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        log.debug("Запрос заблокированных: type={}, page={}, size={}, searchMode={}, searchType={}, role={}",
                type, page, size, isSearchMode, searchType, userRole);

        try {
            if (page < 0) {
                page = 0;
            }

            // Доступ для SUPER и ADMIN
            boolean isAdminUser = currentUser != null &&
                    ("ROLE_SUPER".equals(currentUser.getRole()) ||
                            "ROLE_ADMIN".equals(currentUser.getRole()));

            if (!isAdminUser) {
                log.warn("Отказ в доступе: пользователь с ролью {} не является ADMIN/SUPER", userRole);
                PaginationSliceResponseDTO<?> errorResponse = createErrorResponse("Доступ запрещен");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            PaginationSliceResponseDTO<?> response;
            String searchParam = isSearchMode ? searchQuery : null;

            switch (type.toLowerCase()) {
                case "products":
                    Slice<PostText> postsSlice = blockedPostsCacheService.getBlockedProductsSlice(
                            page, size, searchParam, searchType);
                    log.debug("Заблокированных товаров получено: {}", postsSlice.getNumberOfElements());
                    List<PostTextBlockedDTO> productDTOs = convertToPostTextBlockedDTOs(postsSlice.getContent(), currentUser.getRole());
                    response = createSliceResponse(productDTOs, postsSlice, page);
                    break;

                case "services":
                    Slice<PostTextJob> servicesSlice = blockedPostsCacheService.getBlockedServicesSlice(
                            page, size, searchParam, searchType);
                    log.debug("Заблокированных услуг получено: {}", servicesSlice.getNumberOfElements());
                    List<PostTextJobBlockedDTO> serviceDTOs = convertToPostTextJobBlockedDTOs(servicesSlice.getContent(), currentUser.getRole());
                    response = createSliceResponse(serviceDTOs, servicesSlice, page);
                    break;

                case "vacancies":
                    Slice<Vacancy> vacanciesSlice = blockedPostsCacheService.getBlockedVacanciesSlice(
                            page, size, searchParam, searchType);
                    log.debug("Заблокированных вакансий получено: {}", vacanciesSlice.getNumberOfElements());
                    List<VacancyBlockedDTO> vacancyDTOs = convertToVacancyBlockedDTOs(vacanciesSlice.getContent(), currentUser.getRole());
                    response = createSliceResponse(vacancyDTOs, vacanciesSlice, page);
                    break;

                case "resume":
                    Slice<Resume> resumeSlice = blockedPostsCacheService.getBlockedResumeSlice(
                            page, size, searchParam, searchType);
                    log.debug("Заблокированных резюме получено: {}", resumeSlice.getNumberOfElements());
                    List<ResumeBlockedDTO> resumeDTOs = convertToResumeBlockedDTOs(resumeSlice.getContent(), currentUser.getRole());
                    response = createSliceResponse(resumeDTOs, resumeSlice, page);
                    break;

                default:
                    log.warn("Неизвестный тип данных: {}", type);
                    response = createEmptyResponse(type);
            }

            if (isSearchMode) {
                response.setSearchQuery(searchQuery);
                response.setSearchType(searchType);
                response.setTotalResults(response.getRedact() != null ? response.getRedact().size() : 0);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Загружено {} заблокированных элементов типа {}, hasMore={}, время={}ms",
                    response.getRedact() != null ? response.getRedact().size() : 0,
                    type, response.isHasMore(), duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке заблокированных данных типа {}: {}", type, e.getClass().getSimpleName(), e);
            PaginationSliceResponseDTO<?> errorResponse = createErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ==================== TOGGLE BLOCK ENDPOINTS ====================

    /**
     * Блокировка/разблокировка товара по hashId.
     */
    @PostMapping("/page_two/redactor/{hashId}/toggle-block")
    public String toggleBlockPost(@PathVariable(value = "hashId") String hashId,
                                  Authentication authentication,
                                  Model model) {
        return toggleBlock(hashId, authentication, model, "product");
    }

    /**
     * Блокировка/разблокировка услуги по hashId.
     */
    @PostMapping("/page_two/redactor/job/{hashId}/toggle-block")
    public String toggleBlockJob(@PathVariable(value = "hashId") String hashId,
                                 Authentication authentication,
                                 Model model) {
        return toggleBlock(hashId, authentication, model, "service");
    }

    /**
     * Блокировка/разблокировка вакансии по hashId.
     */
    @PostMapping("/page_two/redactor/vac/{hashId}/toggle-block")
    public String toggleBlockVacancy(@PathVariable(value = "hashId") String hashId,
                                     Authentication authentication,
                                     Model model) {
        return toggleBlock(hashId, authentication, model, "vacancy");
    }

    /**
     * Блокировка/разблокировка резюме по hashId.
     */
    @PostMapping("/page_two/redactor/res/{hashId}/toggle-block")
    public String toggleBlockResume(@PathVariable(value = "hashId") String hashId,
                                    Authentication authentication,
                                    Model model) {
        return toggleBlock(hashId, authentication, model, "resume");
    }

    /**
     * Общий метод для блокировки/разблокировки объявления по hashId.
     * Доступен только для SUPER-пользователей. При блокировке очищаются избранные записи.
     *
     * @param hashId        хеш-идентификатор объявления
     * @param authentication объект аутентификации
     * @param model         модель для передачи ошибок
     * @param type          тип объявления (product, service, vacancy, resume)
     * @return redirect на страницу редактора или error
     */
    private String toggleBlock(String hashId, Authentication authentication, Model model, String type) {
        log.debug("Toggle block: type={}, hashId={}", type, hashId);

        try {
            User currentUser = (User) authentication.getPrincipal();
            log.debug("Пользователь: роль={}", currentUser.getRole());

            if (!currentUser.getRole().equals("ROLE_SUPER")) {
                log.warn("Отказ в блокировке: недостаточно прав для роли {}", currentUser.getRole());
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Товар с hashId {} не найден", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostText post = optionalPost.get();
                post.setBlocked(!post.isBlocked());
                if (post.isBlocked()) {
                    post.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByPostId(post.getId());
                } else {
                    post.setBlockedAt(null);
                }
                postRepo.save(post);
                blockedPostsCacheService.clearBlockedProductsCache();
                log.info("Товар id={} изменён: blocked={}", post.getId(), post.isBlocked());

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Услуга с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostTextJob post = optionalPost.get();
                post.setBlocked(!post.isBlocked());
                if (post.isBlocked()) {
                    post.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByPostJobId(post.getId());
                } else {
                    post.setBlockedAt(null);
                }
                postRepoJob.save(post);
                blockedPostsCacheService.clearBlockedServicesCache();
                log.info("Услуга id={} изменена: blocked={}", post.getId(), post.isBlocked());

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findByHashId(hashId);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Вакансия с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Vacancy vacancy = optionalVacancy.get();
                vacancy.setBlocked(!vacancy.isBlocked());
                if (vacancy.isBlocked()) {
                    vacancy.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByVacancyId(vacancy.getId());
                } else {
                    vacancy.setBlockedAt(null);
                }
                postVacancyRepository.save(vacancy);
                blockedPostsCacheService.clearBlockedVacanciesCache();
                log.info("Вакансия id={} изменена: blocked={}", vacancy.getId(), vacancy.isBlocked());

            } else if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findByHashId(hashId);
                if (optionalResume.isEmpty()) {
                    log.warn("Резюме с hashId {} не найдено", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Resume res = optionalResume.get();
                res.setBlocked(!res.isBlocked());
                if (res.isBlocked()) {
                    res.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByResumeId(res.getId());
                } else {
                    res.setBlockedAt(null);
                }
                postResumeRepository.save(res);
                blockedPostsCacheService.clearBlockedResumeCache();
                log.info("Резюме id={} изменено: blocked={}", res.getId(), res.isBlocked());
            }

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при блокировке/разблокировке записи hashId={}, type={}: {}", hashId, type, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при блокировке: " + e.getMessage());
            return "error";
        }
    }

    // ==================== MAIN PAGE ====================

    /**
     * Главная страница управления заблокированными объявлениями.
     * Отображает статистику количества заблокированных записей каждого типа.
     *
     * @param model       модель для передачи атрибутов в представление
     * @param currentUser текущий пользователь
     * @return название шаблона или error
     */
    @GetMapping("/page_two/redactorblocked")
    public String redactorBlockedMain(Model model, @AuthenticationPrincipal User currentUser) {
        String userRole = currentUser != null ? currentUser.getRole() : "null";
        log.debug("Запрос главной страницы заблокированных, role={}", userRole);

        try {
            // Проверка прав доступа
            if (!"ROLE_SUPER".equals(currentUser.getRole()) && !"ROLE_ADMIN".equals(currentUser.getRole())) {
                log.warn("Отказ в доступе к странице заблокированных: роль={}", userRole);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // Получение статистики из кэша
            long blockedProductsCount = blockedPostsCacheService.getBlockedProductsCount();
            long blockedServicesCount = blockedPostsCacheService.getBlockedServicesCount();
            long blockedVacanciesCount = blockedPostsCacheService.getBlockedVacanciesCount();
            long blockedResumeCount = blockedPostsCacheService.getBlockedResumeCount();

            log.debug("Статистика заблокированных: products={}, services={}, vacancies={}, resume={}",
                    blockedProductsCount, blockedServicesCount, blockedVacanciesCount, blockedResumeCount);

            model.addAttribute("blockedProductsCount", blockedProductsCount);
            model.addAttribute("blockedServicesCount", blockedServicesCount);
            model.addAttribute("blockedVacanciesCount", blockedVacanciesCount);
            model.addAttribute("blockedResumeCount", blockedResumeCount);
            model.addAttribute("title", "Заблокированные объявления");
            model.addAttribute("isSuper", true);
            model.addAttribute("initialProductsBadge", blockedProductsCount > 0 ? blockedProductsCount : 0);
            model.addAttribute("initialServicesBadge", blockedServicesCount > 0 ? blockedServicesCount : 0);
            model.addAttribute("initialVacanciesBadge", blockedVacanciesCount > 0 ? blockedVacanciesCount : 0);
            model.addAttribute("initialResumeBadge", blockedResumeCount > 0 ? blockedResumeCount : 0);

            return "page_two/redactorblocked";

        } catch (Exception e) {
            log.error("Ошибка загрузки страницы заблокированных: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки заблокированных объявлений: " + e.getMessage());
            return "error";
        }
    }

    // ==================== SEARCH METHODS ====================

    /**
     * Поиск среди заблокированных товаров.
     */
    private Slice<PostText> searchBlockedPosts(String query, String searchType, Pageable pageable) {
        String normalizedQuery = query != null ? query.trim() : "";
        log.debug("Поиск среди товаров: query={}, searchType={}", normalizedQuery, searchType);

        switch (searchType) {
            case "title":
                return postRepo.searchByTitle(normalizedQuery, pageable);
            case "nameuse":
                return postRepo.searchByUserName(normalizedQuery, pageable);
            case "all":
            default:
                return postRepo.searchByTitleOrUserName(normalizedQuery, pageable);
        }
    }

    /**
     * Поиск среди заблокированных услуг.
     */
    private Slice<PostTextJob> searchBlockedServices(String query, String searchType, Pageable pageable) {
        String normalizedQuery = query != null ? query.trim() : "";
        log.debug("Поиск среди услуг: query={}, searchType={}", normalizedQuery, searchType);

        switch (searchType) {
            case "title":
                return postRepoJob.searchByTitle(normalizedQuery, pageable);
            case "nameuse":
                return postRepoJob.searchByUserName(normalizedQuery, pageable);
            case "all":
            default:
                return postRepoJob.searchByTitleOrUserName(normalizedQuery, pageable);
        }
    }

    /**
     * Поиск среди заблокированных вакансий.
     */
    private Slice<Vacancy> searchBlockedVacancies(String query, String searchType, Pageable pageable) {
        String normalizedQuery = query != null ? query.trim() : "";
        log.debug("Поиск среди вакансий: query={}, searchType={}", normalizedQuery, searchType);

        switch (searchType) {
            case "title":
                return postVacancyRepository.searchByTitle(normalizedQuery, pageable);
            case "nameuse":
                return postVacancyRepository.searchByUserName(normalizedQuery, pageable);
            case "all":
            default:
                return postVacancyRepository.searchByTitleOrUserName(normalizedQuery, pageable);
        }
    }

    /**
     * Поиск среди заблокированных резюме.
     */
    private Slice<Resume> searchBlockedResume(String query, String searchType, Pageable pageable) {
        String normalizedQuery = query != null ? query.trim() : "";
        log.debug("Поиск среди резюме: query={}, searchType={}", normalizedQuery, searchType);

        switch (searchType) {
            case "title":
                return postResumeRepository.searchByTitle(normalizedQuery, pageable);
            case "nameuse":
                return postResumeRepository.searchByUserName(normalizedQuery, pageable);
            case "all":
            default:
                return postResumeRepository.searchByTitleOrUserName(normalizedQuery, pageable);
        }
    }

    // ==================== STATISTICS API ====================

    /**
     * REST API для получения статистики по количеству заблокированных объявлений каждого типа.
     *
     * @param currentUser текущий пользователь
     * @return Map с количеством заблокированных записей
     */
    @GetMapping("/page_two/redactorblocked/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getBlockedStatistics(
            @AuthenticationPrincipal User currentUser) {

        String userRole = currentUser != null ? currentUser.getRole() : "null";
        log.debug("Запрос статистики заблокированных, role={}", userRole);

        try {
            boolean isAdminUser = currentUser != null &&
                    ("ROLE_SUPER".equals(currentUser.getRole()) ||
                            "ROLE_ADMIN".equals(currentUser.getRole()));

            if (!isAdminUser) {
                log.warn("Отказ в доступе к статистике: роль={}", userRole);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Map<String, Long> statistics = new HashMap<>();
            statistics.put("products", blockedPostsCacheService.getBlockedProductsCount());
            statistics.put("services", blockedPostsCacheService.getBlockedServicesCount());
            statistics.put("vacancies", blockedPostsCacheService.getBlockedVacanciesCount());
            statistics.put("resume", blockedPostsCacheService.getBlockedResumeCount());

            log.debug("Статистика отправлена: {}", statistics);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Ошибка получения статистики: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== CONVERTERS (Конвертеры в DTO) ====================

    private List<PostTextBlockedDTO> convertToPostTextBlockedDTOs(List<PostText> posts, String currentUserRole) {
        return posts.stream()
                .map(post -> convertToPostTextBlockedDTO(post, currentUserRole))
                .collect(Collectors.toList());
    }

    private PostTextBlockedDTO convertToPostTextBlockedDTO(PostText post, String currentUserRole) {
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

    private List<PostTextJobBlockedDTO> convertToPostTextJobBlockedDTOs(List<PostTextJob> posts, String currentUserRole) {
        return posts.stream()
                .map(post -> convertToPostTextJobBlockedDTO(post, currentUserRole))
                .collect(Collectors.toList());
    }

    private PostTextJobBlockedDTO convertToPostTextJobBlockedDTO(PostTextJob post, String currentUserRole) {
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

    private List<VacancyBlockedDTO> convertToVacancyBlockedDTOs(List<Vacancy> vacancies, String currentUserRole) {
        return vacancies.stream()
                .map(vacancy -> convertToVacancyBlockedDTO(vacancy, currentUserRole))
                .collect(Collectors.toList());
    }

    private VacancyBlockedDTO convertToVacancyBlockedDTO(Vacancy vacancy, String currentUserRole) {
        VacancyBlockedDTO dto = new VacancyBlockedDTO();
        dto.setId(vacancy.getId());
        dto.setHashId(vacancy.getHashId());
        dto.setTitle(vacancy.getTitle());
        dto.setLocation(vacancy.getLocation());
        dto.setCash(vacancy.getCash());
        dto.setCategory(vacancy.getCategory());
        dto.setCategoryDisplayName(categoryUtils.getCategoryDisplayName("vacancy", vacancy.getCategory()));
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

    private List<ResumeBlockedDTO> convertToResumeBlockedDTOs(List<Resume> resume, String currentUserRole) {
        return resume.stream()
                .map(resumes -> convertToResumeBlockedDTO(resumes, currentUserRole))
                .collect(Collectors.toList());
    }

    private ResumeBlockedDTO convertToResumeBlockedDTO(Resume resume, String currentUserRole) {
        ResumeBlockedDTO dto = new ResumeBlockedDTO();
        dto.setId(resume.getId());
        dto.setHashId(resume.getHashId());
        dto.setTitle(resume.getTitle());
        dto.setCash(resume.getCash());
        dto.setCategory(resume.getCategory());
        dto.setCategoryDisplayName(categoryUtils.getCategoryDisplayName("resume", resume.getCategory()));
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

    // ==================== RESPONSE HELPERS ====================

    private <T> PaginationSliceResponseDTO<T> createSliceResponse(List<T> content, Slice<?> slice, int currentPage) {
        PaginationSliceResponseDTO<T> response = new PaginationSliceResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные успешно загружены");
        response.setRedact(content);
        response.setHasMore(slice.hasNext());
        response.setCurrentPage(currentPage);
        response.setSize(slice.getNumberOfElements());
        return response;
    }

    private <T> PaginationSliceResponseDTO<T> createEmptyResponse(String type) {
        PaginationSliceResponseDTO<T> response = new PaginationSliceResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет данных для типа: " + type);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setSize(0);
        return response;
    }

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