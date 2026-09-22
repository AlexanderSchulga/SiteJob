package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.mapper.ProductFilterMapper;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.product.service.FilterRangeService;
import jobforsite.sitejob.module.product.service.PostTextListService;
import jobforsite.sitejob.module.product.service.filter.LoadMoreFilterFactory;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Контроллер для пагинированной загрузки (load-more) постов всех типов.
 * <p>
 * Обеспечивает бесконечную прокрутку (infinite scroll) с фильтрацией по категориям,
 * регионам, городам, поиску, сортировке (по дате и цене). Поддерживаются типы:
 * товары (products), услуги (services), вакансии (vacancies), резюме (resume).
 * </p>
 *
 * <p>
 * Особенности:
 * <ul>
 *   <li>Slice-based пагинация (без подсчета общего количества записей)</li>
 *   <li>Rate limiting для защиты от частых запросов</li>
 *   <li>Поддержка специального значения "other_none" для города (показать записи из других городов региона)</li>
 *   <li>Гибкая сортировка (новые/старые, по возрастанию/убыванию цены)</li>
 * </ul>
 * </p>
 */
@Slf4j
@Controller
public class LoadPaginationController extends BaseController {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;

    private final PostResumeRepository postResumeRepository;
    private final RegionsConfig regionsConfig;
    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;

    private final ExperienceResumCategoryConfig experienceResumCategoryConfig;

    private final ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;

    private final WorkFormatResumeConfig workFormatResumeConfig;

    private final WorkFormatVacancyConfig workFormatVacancyConfig;

    private final PostResumeCategoryConfig postResumeCategoryConfig;
    private final RateLimitService rateLimitService;
    private final FilterRangeService filterRangeService;

    private final PostTextListService postTextListService;

    private final LoadMoreFilterFactory loadMoreFilterFactory;



    public LoadPaginationController(PostRepo postRepo, PostRepoJob postRepoJob,
                                    PostVacancyRepository postVacancyRepository, PostResumeRepository postResumeRepository,
                                    RegionsConfig regionsConfig,
                                    PostTextCategoryConfig postTextCategoryConfig, PostTextJobCategoryConfig postTextJobCategoryConfig,
                                    PostVacancyCategoryConfig postVacancyCategoryConfig,
                                    PostResumeCategoryConfig postResumeCategoryConfig,
                                    ExperienceResumCategoryConfig experienceResumCategoryConfig,
                                    ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig,
                                    WorkFormatVacancyConfig workFormatVacancyConfig,
                                    WorkFormatResumeConfig workFormatResumeConfig,
                                    RateLimitService rateLimitService,
                                    FilterRangeService filterRangeService, PostTextListService postTextListService,
                                    LoadMoreFilterFactory loadMoreFilterFactory) {
        this.postRepo = postRepo;
        this.postRepoJob = postRepoJob;
        this.postVacancyRepository = postVacancyRepository;
        this.postResumeRepository = postResumeRepository;
        this.regionsConfig = regionsConfig;
        this.postTextCategoryConfig = postTextCategoryConfig;
        this.postTextJobCategoryConfig = postTextJobCategoryConfig;
        this.postVacancyCategoryConfig = postVacancyCategoryConfig;
        this.postResumeCategoryConfig = postResumeCategoryConfig;
        this.experienceResumCategoryConfig = experienceResumCategoryConfig;
        this.experienceVacancyCategoryConfig = experienceVacancyCategoryConfig;
        this.workFormatVacancyConfig = workFormatVacancyConfig;
        this.workFormatResumeConfig = workFormatResumeConfig;
        this.rateLimitService = rateLimitService;
        this.filterRangeService = filterRangeService;
        this.postTextListService = postTextListService;
        this.loadMoreFilterFactory = loadMoreFilterFactory;
    }

    /**
     * Получает реальный IP-адрес клиента с учетом прокси/X-Forwarded-For.
     *
     * @param request HTTP запрос
     * @return IP-адрес клиента
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Пагинированная загрузка товаров (постов) с фильтрацией и сортировкой.
     *
     * @param category  категория товара (опционально)
     * @param search    поисковый запрос (опционально)
     * @param sort      сортировка по дате (newest/oldest)
     * @param priceSort сортировка по цене (none/price_asc/price_desc)
     * @param region    регион (опционально)
     * @param city      город (опционально, special value "other_none" - другие города региона)
     * @param page      номер страницы (начиная с 0)
     * @param size      размер страницы (по умолчанию 9)
     * @param request   HTTP запрос для получения IP
     * @return ResponseEntity с пагинированным ответом (список постов, hasMore, nextPage)
     */
    /**
     * Пагинированная загрузка товаров (постов) с фильтрацией и сортировкой.
     */

    @GetMapping("/page_one/viewsone/load-more")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreProducts(
            @ModelAttribute ProductFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        // Rate limiting
        if (!rateLimitService.tryConsumeProductLoad(clientIp)) {
            log.warn("Rate limit превышен для load-more продуктов, IP: {}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {

            String normalizedCategory = filterRangeService.normalizeParam(filter.getCategory());
            String normalizedSearch = filterRangeService.normalizeParam(filter.getSearch());
            String normalizedRegion = filterRangeService.normalizeParam(filter.getRegion());
            String normalizedCity = filterRangeService.normalizeParam(filter.getCity());
            String sort = filter.getSort() != null ? filter.getSort() : "newest";
            String priceSort = filter.getPriceSort() != null ? filter.getPriceSort() : "none";

            filter.setCategory(normalizedCategory);
            filter.setSearch(normalizedSearch);
            filter.setRegion(normalizedRegion);
            filter.setCity(normalizedCity);
            filter.setSort(sort);
            filter.setPriceSort(priceSort);

            log.debug("Загрузка продуктов: фильтр={}, страница={}", filter, page);

            Pageable pageable = createPageable(page, size, sort, priceSort, "product");
            boolean showOtherCities = "other_none".equals(filter.getCity());
            List<String> standardCities = showOtherCities ? regionsConfig.getCitiesByRegion(filter.getRegion()) : null;

            // ===== ВЫБОР СЕРВИСА =====
            ProductLoadMoreService loadService = loadMoreFilterFactory.getService(filter.getCategory());

            // ===== ЗАГРУЗКА ID =====
            Slice<Long> idsSlice = loadService.loadIds(filter, pageable, showOtherCities, standardCities);

            if (idsSlice == null) {
                idsSlice = loadMoreFilterFactory.getService(null).loadIds(filter, pageable, showOtherCities, standardCities);
            }

            if (idsSlice.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("posts", new ArrayList<>());
                response.put("hasMore", false);
                response.put("nextPage", page);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            }

            // ===== ПОЛУЧЕНИЕ ПОСТОВ =====
            List<PostText> posts = postRepo.findPostsByIdsWithImages(idsSlice.getContent());

            Map<Long, PostText> postMap = posts.stream()
                    .collect(Collectors.toMap(PostText::getId, Function.identity()));

            List<PostText> orderedPosts = idsSlice.getContent().stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<PostTextListDTO> postDTOs = postTextListService.toListDTO(orderedPosts);

            // ===== ФОРМИРОВАНИЕ ОТВЕТА =====
            Map<String, Object> response = new HashMap<>();
            response.put("posts", postDTOs);
            response.put("hasMore", idsSlice.hasNext());
            response.put("nextPage", idsSlice.hasNext() ? page + 1 : page);
            response.put("status", "success");

            // Возвращаем текущие параметры для синхронизации состояния на клиенте
            response.put("currentCategory", filter.getCategory());
            response.put("currentRegion", filter.getRegion());
            response.put("currentCity", filter.getCity());
            response.put("currentSort", filter.getSort());
            response.put("currentPriceSort", filter.getPriceSort());

            loadService.addResponseParams(response, filter);

            log.debug("Загружено {} продуктов, hasMore={}", postDTOs.size(), idsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке продуктов: категория={}, страница={}, ошибка={}",
                    filter.getCategory(), page, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Пагинированная загрузка услуг с фильтрацией и сортировкой.
     *
     * @param category  категория услуги
     * @param search    поисковый запрос
     * @param sort      сортировка по дате
     * @param priceSort сортировка по цене
     * @param region    регион
     * @param city      город
     * @param page      номер страницы
     * @param size      размер страницы
     * @param request   HTTP запрос
     * @return ResponseEntity с пагинированным ответом
     */
    @GetMapping("/page_one/viewsonejob/load-more-services")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreServices(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "priceSort", defaultValue = "none") String priceSort,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeServiceLoad(clientIp)) {
            log.warn("Rate limit превышен для load-more услуг, IP: {}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            log.debug("Загрузка услуг: категория={}, поиск={}, регион={}, город={}, страница={}",
                    category, search, region, city, page);

            Pageable pageable = createPageable(page, size, sort, priceSort, "services");

            String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            String normalizedCategory = (category != null && !category.isEmpty() && !"all".equals(category)) ? category : null;
            String normalizedRegion = (region != null && !region.isEmpty() && !"all".equals(region)) ? region : null;
            String normalizedCity = (city != null && !city.isEmpty() && !"all".equals(city)) ? city : null;

            boolean showOtherCities = "other_none".equals(city);

            Slice<Long> idsSlice;

            if (showOtherCities) {
                List<String> standardCities = regionsConfig.getCitiesByRegion(region);
                idsSlice = postRepoJob.findPostJobIdsByOtherCitiesSlice(
                        normalizedSearch,
                        normalizedCategory,
                        normalizedRegion,
                        standardCities,
                        pageable
                );
            } else {
                idsSlice = postRepoJob.findPostJobIdsWithFiltersSlice(
                        normalizedSearch,
                        normalizedCategory,
                        normalizedRegion,
                        normalizedCity,
                        pageable
                );
            }

            if (idsSlice.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("posts", new ArrayList<>());
                response.put("hasMore", false);
                response.put("nextPage", page);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            }

            List<PostTextJob> postsjob = postRepoJob.findPostsByIdsWithImages(idsSlice.getContent());

            Map<Long, PostTextJob> postMap = postsjob.stream()
                    .collect(Collectors.toMap(PostTextJob::getId, Function.identity()));

            List<PostTextJob> orderedPosts = idsSlice.getContent().stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Map<String, Object>> simplePostsJob = orderedPosts.stream()
                    .map(this::convertToServiceMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("posts", simplePostsJob);
            response.put("hasMore", idsSlice.hasNext());
            response.put("nextPage", idsSlice.hasNext() ? page + 1 : page);
            response.put("status", "success");

            response.put("currentCategory", category);
            response.put("currentRegion", region);
            response.put("currentCity", city);
            response.put("currentSort", sort);
            response.put("currentPriceSort", priceSort);

            log.debug("Загружено {} услуг, hasMore={}", simplePostsJob.size(), idsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке услуг: категория={}, страница={}, ошибка={}", category, page, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Пагинированная загрузка вакансий с фильтрацией и сортировкой.
     */
    @GetMapping("/page_one/viewsonevac/load-more-vacancies")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreVacancies(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "workformat", required = false) String workformat,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "priceSort", defaultValue = "none") String priceSort,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeVacancyLoad(clientIp)) {
            log.warn("Rate limit превышен для load-more вакансий, IP: {}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            log.debug("Загрузка вакансий: категория={}, опыт={}, формат={}, поиск={}, регион={}, город={}, страница={}",
                    category, experience, workformat, search, region, city, page);

            Pageable pageable = createPageable(page, size, sort, priceSort, "vacancies");

            String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            String normalizedCategory = (category != null && !category.isEmpty() && !"all".equals(category)) ? category : null;
            String normalizedExperience = (experience != null && !experience.isEmpty() && !"all".equals(experience)) ? experience : null;
            String normalizedWorkformat = (workformat != null && !workformat.isEmpty() && !"all".equals(workformat)) ? workformat : null;
            String normalizedRegion = (region != null && !region.isEmpty() && !"all".equals(region)) ? region : null;
            String normalizedCity = (city != null && !city.isEmpty() && !"all".equals(city)) ? city : null;

            boolean showOtherCities = "other_none".equals(city);

            Slice<Long> idsSlice;

            if (showOtherCities) {
                List<String> standardCities = regionsConfig.getCitiesByRegion(region);
                idsSlice = postVacancyRepository.findVacancyIdsByOtherCitiesSlice(
                        normalizedSearch,
                        normalizedCategory,
                        normalizedExperience,
                        normalizedWorkformat,
                        normalizedRegion,
                        standardCities,
                        pageable
                );
            } else {
                idsSlice = postVacancyRepository.findVacancyIdsWithFiltersSlice(
                        normalizedSearch,
                        normalizedCategory,
                        normalizedExperience,
                        normalizedWorkformat,
                        normalizedRegion,
                        normalizedCity,
                        pageable
                );
            }

            if (idsSlice.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("posts", new ArrayList<>());
                response.put("hasMore", false);
                response.put("nextPage", page);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            }

            List<Vacancy> vacancies = postVacancyRepository.findVacanciesByIdsWithImages(idsSlice.getContent());

            Map<Long, Vacancy> vacancyMap = vacancies.stream()
                    .collect(Collectors.toMap(Vacancy::getId, Function.identity()));

            List<Vacancy> orderedVacancies = idsSlice.getContent().stream()
                    .map(vacancyMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Map<String, Object>> simplePostsVac = orderedVacancies.stream()
                    .map(this::convertToVacancyMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("posts", simplePostsVac);
            response.put("hasMore", idsSlice.hasNext());
            response.put("nextPage", idsSlice.hasNext() ? page + 1 : page);
            response.put("status", "success");

            response.put("currentCategory", category);
            response.put("currentExperience", experience);
            response.put("currentWorkformat", workformat);
            response.put("currentRegion", region);
            response.put("currentCity", city);
            response.put("currentSort", sort);
            response.put("currentPriceSort", priceSort);

            log.debug("Загружено {} вакансий, hasMore={}", simplePostsVac.size(), idsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке вакансий: категория={}, страница={}, ошибка={}", category, page, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Пагинированная загрузка резюме с фильтрацией (включая опыт работы) и сортировкой.
     *
     * @param category   категория резюме
     * @param experience опыт работы (опционально)
     * @param workformat формат работы (опционально)
     * @param search     поисковый запрос
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по зарплате (cash)
     * @param region     регион
     * @param city       город
     * @param page       номер страницы
     * @param size       размер страницы
     * @param request    HTTP запрос
     * @return ResponseEntity с пагинированным ответом
     */
    @GetMapping("/page_one/viewsoneres/load-more-resume")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreResume(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "experience", required = false) String experience,
            @RequestParam(value = "workformat", required = false) String workformat,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", defaultValue = "newest") String sort,
            @RequestParam(value = "priceSort", defaultValue = "none") String priceSort,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeResumeLoad(clientIp)) {
            log.warn("Rate limit превышен для load-more резюме, IP: {}", clientIp);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Слишком частые запросы. Пожалуйста, подождите.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        try {
            log.debug("Загрузка резюме: категория={}, опыт={}, формат={}, поиск={}, регион={}, город={}, страница={}",
                    category, experience, workformat, search, region, city, page);

            Pageable pageable = createPageable(page, size, sort, priceSort, "resume");

            String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
            String normalizedCategory = (category != null && !category.isEmpty() && !"all".equals(category)) ? category : null;
            String normalizedExperience = (experience != null && !experience.isEmpty() && !"all".equals(experience)) ? experience : null;
            String normalizedWorkformat = (workformat != null && !workformat.isEmpty() && !"all".equals(workformat)) ? workformat : null;
            String normalizedRegion = (region != null && !region.isEmpty() && !"all".equals(region)) ? region : null;
            String normalizedCity = (city != null && !city.isEmpty() && !"all".equals(city)) ? city : null;

            boolean showOtherCities = "other_none".equals(city);

            Slice<Long> idsSlice;

            if (showOtherCities) {
                List<String> standardCities = regionsConfig.getCitiesByRegion(region);
                idsSlice = postResumeRepository.findResIdsByOtherCitiesSlice(
                        normalizedSearch,
                        normalizedCategory,
                        normalizedExperience,
                        normalizedWorkformat,
                        normalizedRegion,
                        standardCities,
                        pageable
                );
            } else {
                idsSlice = postResumeRepository.findResIdsWithFiltersSlice(
                        normalizedSearch,
                        normalizedCategory,
                        normalizedExperience,
                        normalizedWorkformat,
                        normalizedRegion,
                        normalizedCity,
                        pageable
                );
            }

            if (idsSlice.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("res", new ArrayList<>());
                response.put("hasMore", false);
                response.put("nextPage", page);
                response.put("status", "success");
                return ResponseEntity.ok(response);
            }

            List<Resume> resume = postResumeRepository.findResumeByIdsWithImages(idsSlice.getContent());

            Map<Long, Resume> resumeMap = resume.stream()
                    .collect(Collectors.toMap(Resume::getId, Function.identity()));

            List<Resume> orderedResume = idsSlice.getContent().stream()
                    .map(resumeMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Map<String, Object>> simplePostsRes = orderedResume.stream()
                    .map(this::convertToResumeMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("res", simplePostsRes);
            response.put("hasMore", idsSlice.hasNext());
            response.put("nextPage", idsSlice.hasNext() ? page + 1 : page);
            response.put("status", "success");

            response.put("currentCategory", category);
            response.put("currentExperience", experience);
            response.put("currentWorkformat", workformat);
            response.put("currentRegion", region);
            response.put("currentCity", city);
            response.put("currentSort", sort);
            response.put("currentPriceSort", priceSort);

            log.debug("Загружено {} резюме, hasMore={}", simplePostsRes.size(), idsSlice.hasNext());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке резюме: категория={}, страница={}, ошибка={}", category, page, e.getClass().getSimpleName(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Создает объект Pageable с динамической сортировкой.
     * <p>
     * Поддерживает сортировку:
     * <ul>
     *   <li>по цене ('price_asc' / 'price_desc') — для товаров и услуг используется поле 'price',
     *       для вакансий и резюме — 'cash'</li>
     *   <li>по дате ('newest' / 'oldest') — порядок по 'moderatedAt'</li>
     * </ul>
     * </p>
     *
     * @param page       номер страницы
     * @param size       размер страницы
     * @param sort       тип сортировки по дате (newest/oldest)
     * @param priceSort  тип сортировки по цене (none/price_asc/price_desc)
     * @param entityType тип сущности (product/services/vacancies/resume) для выбора поля цены
     * @return Pageable объект
     */
    private Pageable createPageable(int page, int size, String sort, String priceSort, String entityType) {
        List<Sort.Order> orders = new ArrayList<>();

        // Сортировка по цене (если указана)
        if (!"none".equals(priceSort)) {
            String sortField;

            if ("vacancies".equals(entityType) || "resume".equals(entityType)) {
                sortField = "cash";
            } else {
                sortField = "price";
            }

            if ("price_asc".equals(priceSort)) {
                orders.add(Sort.Order.asc(sortField));
            } else if ("price_desc".equals(priceSort)) {
                orders.add(Sort.Order.desc(sortField));
            }
        }

        // Сортировка по дате
        if ("oldest".equals(sort)) {
            orders.add(Sort.Order.asc("moderatedAt"));
        } else {
            orders.add(Sort.Order.desc("moderatedAt"));
        }

        // Fallback на случай пустого списка (хотя выше всегда добавляется сортировка по дате)
        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("moderatedAt"));
        }

        return PageRequest.of(page, size, Sort.by(orders));
    }

    /**
     * Конвертирует PostText (товар) в Map для JSON ответа.
     *
     * @param post сущность товара
     * @return Map с полями для клиента
     */
    private Map<String, Object> convertToProductMap(PostText post) {
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("id", post.getHashId());
        postMap.put("hashId", post.getHashId());
        postMap.put("title", post.getTitle());
        postMap.put("price", post.getPrice());
        postMap.put("category", post.getCategory());
        postMap.put("categoryName", postTextCategoryConfig.getCategoryDisplayName(post.getCategory()));
        postMap.put("firstImagePath", post.getFirstImagePath());
        postMap.put("moderatedAt", post.getModeratedAt());
        postMap.put("premium", post.isPremium());
        postMap.put("donate", post.isDonate());
        postMap.put("blocked", post.isBlocked());

        if (post.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("hashId", post.getUser().getHashId());
            userMap.put("nameuse", post.getUser().getNameuse());
            userMap.put("region", post.getUser().getRegion());
            userMap.put("regionName", regionsConfig.getRegionDisplayName(post.getUser().getRegion()));
            userMap.put("city", post.getUser().getCity());
            userMap.put("cityDisplay", regionsConfig.getCityDisplayName(
                    post.getUser().getRegion(),
                    post.getUser().getCity()
            ));
            userMap.put("online", post.getUser().isOnline());
            postMap.put("user", userMap);
        }
        return postMap;
    }

    /**
     * Конвертирует PostTextJob (услугу) в Map для JSON ответа.
     */
    private Map<String, Object> convertToServiceMap(PostTextJob post) {
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("id", post.getHashId());
        postMap.put("hashId", post.getHashId());
        postMap.put("title", post.getTitle());
        postMap.put("price", post.getPrice());
        postMap.put("category", post.getCategory());
        postMap.put("categoryName", postTextJobCategoryConfig.getCategoryDisplayName(post.getCategory()));
        postMap.put("firstImagePath", post.getFirstImagePath());
        postMap.put("moderatedAt", post.getModeratedAt());
        postMap.put("premium", post.isPremium());
        postMap.put("donate", post.isDonate());
        postMap.put("blocked", post.isBlocked());

        if (post.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("hashId", post.getUser().getHashId());
            userMap.put("nameuse", post.getUser().getNameuse());
            userMap.put("region", post.getUser().getRegion());
            userMap.put("regionName", regionsConfig.getRegionDisplayName(post.getUser().getRegion()));
            userMap.put("city", post.getUser().getCity());
            userMap.put("cityDisplay", regionsConfig.getCityDisplayName(
                    post.getUser().getRegion(),
                    post.getUser().getCity()
            ));
            userMap.put("online", post.getUser().isOnline());
            postMap.put("user", userMap);
        }
        return postMap;
    }

    /**
     * Конвертирует Vacancy в Map для JSON ответа.
     */
    private Map<String, Object> convertToVacancyMap(Vacancy vacancy) {
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("id", vacancy.getHashId());
        postMap.put("hashId", vacancy.getHashId());
        postMap.put("title", vacancy.getTitle());
        postMap.put("cash", vacancy.getCash());
        postMap.put("category", vacancy.getCategory());
        postMap.put("experience", vacancy.getExperience());
        postMap.put("workformat", vacancy.getWorkformat());
        postMap.put("categoryName", postVacancyCategoryConfig.getCategoryDisplayName(vacancy.getCategory()));
        postMap.put("experienceName", experienceVacancyCategoryConfig.getExperienDisplayName(vacancy.getExperience()));
        postMap.put("workformatName", workFormatVacancyConfig.getWorkformatDisplayName(vacancy.getWorkformat()));
        postMap.put("firstImagePath", vacancy.getFirstImagePath());
        postMap.put("moderatedAt", vacancy.getModeratedAt());

        postMap.put("premium", vacancy.isPremium());
        postMap.put("donate", vacancy.isDonate());
        postMap.put("blocked", vacancy.isBlocked());

        if (vacancy.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("hashId", vacancy.getUser().getHashId());
            userMap.put("nameuse", vacancy.getUser().getNameuse());
            userMap.put("region", vacancy.getUser().getRegion());
            userMap.put("regionName", regionsConfig.getRegionDisplayName(vacancy.getUser().getRegion()));
            userMap.put("city", vacancy.getUser().getCity());
            userMap.put("cityDisplay", regionsConfig.getCityDisplayName(
                    vacancy.getUser().getRegion(),
                    vacancy.getUser().getCity()
            ));
            userMap.put("online", vacancy.getUser().isOnline());
            postMap.put("user", userMap);
        }
        return postMap;
    }

    /**
     * Конвертирует Resume в Map для JSON ответа.
     */
    private Map<String, Object> convertToResumeMap(Resume resume) {
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("id", resume.getHashId());
        postMap.put("hashId", resume.getHashId());
        postMap.put("title", resume.getTitle());
        postMap.put("cash", resume.getCash());
        postMap.put("category", resume.getCategory());
        postMap.put("experience", resume.getExperience());
        postMap.put("workformat", resume.getWorkformat());
        postMap.put("categoryName", postResumeCategoryConfig.getCategoryDisplayName(resume.getCategory()));
        postMap.put("experienceName", experienceResumCategoryConfig.getExperienDisplayName(resume.getExperience()));
        postMap.put("workformatName", workFormatResumeConfig.getWorkformatDisplayName(resume.getWorkformat()));
        postMap.put("firstImagePath", resume.getFirstImagePath());
        postMap.put("moderatedAt", resume.getModeratedAt());

        if (resume.getUser() != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("nameuse", resume.getUser().getNameuse());
            userMap.put("region", resume.getUser().getRegion());
            userMap.put("regionName", regionsConfig.getRegionDisplayName(resume.getUser().getRegion()));
            userMap.put("city", resume.getUser().getCity());
            userMap.put("cityDisplay", regionsConfig.getCityDisplayName(
                    resume.getUser().getRegion(),
                    resume.getUser().getCity()
            ));
            userMap.put("online", resume.getUser().isOnline());
            postMap.put("user", userMap);
        }
        return postMap;
    }
}