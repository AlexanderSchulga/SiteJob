package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.resume.dto.ResumeDTO;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.shared.dto.PaginationSliceRedactResponseDTO;
import jobforsite.sitejob.module.product.dto.PostTextDTO;
import jobforsite.sitejob.module.service.dto.PostTextJobDTO;
import jobforsite.sitejob.module.shared.services.util.ExperienceUtils;
import jobforsite.sitejob.module.shared.services.util.WorkformatUtils;
import jobforsite.sitejob.module.vacancy.dto.VacancyDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Контроллер для пагинированной загрузки данных в режиме редактирования/модерации.
 * Обеспечивает подгрузку товаров, услуг, вакансий и резюме с использованием Slice (без подсчёта общего количества).
 *
 * <p>Особенности:
 * <ul>
 *   <li>Поддержка разных типов контента: products, services, vacancies, resume</li>
 *   <li>Разделение прав: SUPER-пользователи видят все записи, обычные пользователи — только свои</li>
 *   <li>Фильтрация по категориям</li>
 *   <li>Оптимизация: сначала загружаются только ID, затем данные собираются одним запросом с JOIN FETCH</li>
 *   <li>Сортировка по moderatedAt DESC</li>
 * </ul>
 *
 * @see PostTextDTO Товары
 * @see PostTextJobDTO Услуги
 * @see VacancyDTO Вакансии
 * @see ResumeDTO Резюме
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class LoadPaginationRedactController {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;
    private final PostResumeCategoryConfig postResumeCategoryConfig;

    private final ExperienceUtils experienceUtils;

    private final WorkformatUtils workformatUtils;

    /**
     * Основной эндпоинт для подгрузки отредактированных/модерируемых данных.
     *
     * @param type     тип загружаемых данных (products, services, vacancies, resume)
     * @param category категория для фильтрации (опционально, "all" — без фильтра)
     * @param page     номер страницы (начиная с 0)
     * @param size     размер страницы (по умолчанию 8)
     * @param currentUser текущий авторизованный пользователь
     * @return ответ с DTO, содержащим список элементов, статус и флаг hasMore
     */
    @GetMapping("/page_two/redactor/load-more-redact")
    @ResponseBody
    public ResponseEntity<PaginationSliceRedactResponseDTO<?>> loadMoreRedact(
            @RequestParam(value = "type", defaultValue = "products") String type,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @AuthenticationPrincipal User currentUser) {

        long startTime = System.currentTimeMillis();
        log.debug("Запрос подгрузки данных: type={}, page={}, size={}, category={}",
                type, page, size, category != null ? category : "все");

        try {
            if (page < 0) {
                page = 0;
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "moderatedAt"));
            boolean isSuperUser = currentUser != null && "ROLE_SUPER".equals(currentUser.getRole());
            log.debug("isSuperUser={}", isSuperUser);

            PaginationSliceRedactResponseDTO<?> response;

            switch (type.toLowerCase()) {
                case "products":
                    response = loadProducts(currentUser, isSuperUser, category, pageable, page);
                    break;
                case "services":
                    response = loadServices(currentUser, isSuperUser, category, pageable, page);
                    break;
                case "vacancies":
                    response = loadVacancies(currentUser, isSuperUser, category, pageable, page);
                    break;
                case "resume":
                    response = loadResume(currentUser, isSuperUser, category, pageable, page);
                    break;
                default:
                    log.warn("Неизвестный тип данных: {}", type);
                    response = createEmptySliceRedactResponse(type);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Загружено {} элементов типа {}, hasMore={}, время={}ms",
                    response.getRedact() != null ? response.getRedact().size() : 0,
                    type, response.isHasMore(), duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.warn("Ошибка при загрузке данных типа: {}", type);
            PaginationSliceRedactResponseDTO<?> errorResponse = createErrorSliceRedactResponse("Ошибка при загрузке данных");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ==================== PRODUCTS (Товары) ====================

    /**
     * Загружает список товаров для модерации/редактирования.
     * Использует двухэтапную загрузку: сначала ID, затем данные с JOIN FETCH.
     *
     * @param currentUser  текущий пользователь
     * @param isSuperUser  флаг SUPER-прав
     * @param category     фильтр по категории
     * @param pageable     параметры пагинации
     * @param currentPage  номер текущей страницы
     * @return ответ с DTO товаров
     */
    private PaginationSliceRedactResponseDTO<PostTextDTO> loadProducts(
            User currentUser, boolean isSuperUser, String category, Pageable pageable, int currentPage) {

        Slice<Long> idsSlice = getProductIdsSlice(currentUser, isSuperUser, category, pageable);
        log.debug("Products: получено {} ID", idsSlice.getContent().size());

        if (idsSlice.getContent().isEmpty()) {
            return createEmptyProductsResponse(currentPage);
        }

        List<PostText> posts = postRepo.findByIdsWithImagesAndUser(idsSlice.getContent());

        Map<Long, PostText> postMap = posts.stream()
                .collect(Collectors.toMap(PostText::getId, Function.identity()));

        List<PostText> sortedPosts = idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<PostTextDTO> dtos = convertToPostTextDTOs(sortedPosts);
        boolean hasMore = idsSlice.hasNext();

        return createProductsResponse(dtos, currentPage, pageable.getPageSize(), hasMore);
    }

    private Slice<Long> getProductIdsSlice(User currentUser, boolean isSuperUser, String category, Pageable pageable) {
        if (isSuperUser) {
            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postRepo.findProductIdsByCategory(category, pageable);
            } else {
                return postRepo.findAllProductIds(pageable);
            }
        } else {
            if (currentUser == null) {
                return new SliceImpl<>(new ArrayList<>(), pageable, false);
            }

            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postRepo.findProductIdsByUserAndCategory(currentUser, category, pageable);
            } else {
                return postRepo.findProductIdsByUser(currentUser, pageable);
            }
        }
    }

    private PaginationSliceRedactResponseDTO<PostTextDTO> createProductsResponse(
            List<PostTextDTO> content, int currentPage, int size, boolean hasMore) {
        PaginationSliceRedactResponseDTO<PostTextDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные успешно загружены");
        response.setRedact(content);
        response.setHasMore(hasMore);
        response.setCurrentPage(currentPage);
        response.setSize(content.size());
        return response;
    }

    private PaginationSliceRedactResponseDTO<PostTextDTO> createEmptyProductsResponse(int currentPage) {
        PaginationSliceRedactResponseDTO<PostTextDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет товаров");
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(currentPage);
        response.setSize(0);
        return response;
    }

    // ==================== SERVICES (Услуги) ====================

    /**
     * Загружает список услуг для модерации/редактирования.
     */
    private PaginationSliceRedactResponseDTO<PostTextJobDTO> loadServices(
            User currentUser, boolean isSuperUser, String category, Pageable pageable, int currentPage) {

        Slice<Long> idsSlice = getServiceIdsSlice(currentUser, isSuperUser, category, pageable);
        log.debug("Services: получено {} ID", idsSlice.getContent().size());

        if (idsSlice.getContent().isEmpty()) {
            return createEmptyServicesResponse(currentPage);
        }

        List<PostTextJob> services = postRepoJob.findByIdsWithImagesAndUser(idsSlice.getContent());

        Map<Long, PostTextJob> serviceMap = services.stream()
                .collect(Collectors.toMap(PostTextJob::getId, Function.identity()));

        List<PostTextJob> sortedServices = idsSlice.getContent().stream()
                .map(serviceMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<PostTextJobDTO> dtos = convertToPostTextJobDTOs(sortedServices);
        boolean hasMore = idsSlice.hasNext();

        return createServicesResponse(dtos, currentPage, pageable.getPageSize(), hasMore);
    }

    private Slice<Long> getServiceIdsSlice(User currentUser, boolean isSuperUser, String category, Pageable pageable) {
        if (isSuperUser) {
            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postRepoJob.findServiceIdsByCategory(category, pageable);
            } else {
                return postRepoJob.findAllServiceIds(pageable);
            }
        } else {
            if (currentUser == null) {
                return new SliceImpl<>(new ArrayList<>(), pageable, false);
            }

            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postRepoJob.findServiceIdsByUserAndCategory(currentUser, category, pageable);
            } else {
                return postRepoJob.findServiceIdsByUser(currentUser, pageable);
            }
        }
    }

    private PaginationSliceRedactResponseDTO<PostTextJobDTO> createServicesResponse(
            List<PostTextJobDTO> content, int currentPage, int size, boolean hasMore) {
        PaginationSliceRedactResponseDTO<PostTextJobDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные успешно загружены");
        response.setRedact(content);
        response.setHasMore(hasMore);
        response.setCurrentPage(currentPage);
        response.setSize(content.size());
        return response;
    }

    private PaginationSliceRedactResponseDTO<PostTextJobDTO> createEmptyServicesResponse(int currentPage) {
        PaginationSliceRedactResponseDTO<PostTextJobDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет услуг");
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(currentPage);
        response.setSize(0);
        return response;
    }

    // ==================== VACANCIES (Вакансии) ====================

    /**
     * Загружает список вакансий для модерации/редактирования.
     */
    private PaginationSliceRedactResponseDTO<VacancyDTO> loadVacancies(
            User currentUser, boolean isSuperUser, String category, Pageable pageable, int currentPage) {

        Slice<Long> idsSlice = getVacancyIdsSlice(currentUser, isSuperUser, category, pageable);
        log.debug("Vacancies: получено {} ID", idsSlice.getContent().size());

        if (idsSlice.getContent().isEmpty()) {
            return createEmptyVacanciesResponse(currentPage);
        }

        List<Vacancy> vacancies = postVacancyRepository.findByIdsWithImagesAndUser(idsSlice.getContent());

        Map<Long, Vacancy> vacancyMap = vacancies.stream()
                .collect(Collectors.toMap(Vacancy::getId, Function.identity()));

        List<Vacancy> sortedVacancies = idsSlice.getContent().stream()
                .map(vacancyMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<VacancyDTO> dtos = convertToVacancyDTOs(sortedVacancies);
        boolean hasMore = idsSlice.hasNext();

        return createVacanciesResponse(dtos, currentPage, pageable.getPageSize(), hasMore);
    }

    private Slice<Long> getVacancyIdsSlice(User currentUser, boolean isSuperUser, String category, Pageable pageable) {
        if (isSuperUser) {
            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postVacancyRepository.findVacancyIdsByCategory(category, pageable);
            } else {
                return postVacancyRepository.findAllVacancyIds(pageable);
            }
        } else {
            if (currentUser == null) {
                return new SliceImpl<>(new ArrayList<>(), pageable, false);
            }

            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postVacancyRepository.findVacancyIdsByUserAndCategory(currentUser, category, pageable);
            } else {
                return postVacancyRepository.findVacancyIdsByUser(currentUser, pageable);
            }
        }
    }

    private PaginationSliceRedactResponseDTO<VacancyDTO> createVacanciesResponse(
            List<VacancyDTO> content, int currentPage, int size, boolean hasMore) {
        PaginationSliceRedactResponseDTO<VacancyDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные успешно загружены");
        response.setRedact(content);
        response.setHasMore(hasMore);
        response.setCurrentPage(currentPage);
        response.setSize(content.size());
        return response;
    }

    private PaginationSliceRedactResponseDTO<VacancyDTO> createEmptyVacanciesResponse(int currentPage) {
        PaginationSliceRedactResponseDTO<VacancyDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет вакансий");
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(currentPage);
        response.setSize(0);
        return response;
    }

    // ==================== RESUME (Резюме) ====================

    /**
     * Загружает список резюме для модерации/редактирования.
     */
    private PaginationSliceRedactResponseDTO<ResumeDTO> loadResume(
            User currentUser, boolean isSuperUser, String category, Pageable pageable, int currentPage) {

        Slice<Long> idsSlice = getResumeIdsSlice(currentUser, isSuperUser, category, pageable);
        log.debug("Resume: получено {} ID", idsSlice.getContent().size());

        if (idsSlice.getContent().isEmpty()) {
            return createEmptyResumeResponse(currentPage);
        }

        List<Resume> resume = postResumeRepository.findByIdsWithImagesAndUser(idsSlice.getContent());

        Map<Long, Resume> resumeMap = resume.stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));

        List<Resume> sortedResume = idsSlice.getContent().stream()
                .map(resumeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ResumeDTO> dtos = convertToResumeDTOs(sortedResume);
        boolean hasMore = idsSlice.hasNext();

        return createResumeResponse(dtos, currentPage, pageable.getPageSize(), hasMore);
    }

    private Slice<Long> getResumeIdsSlice(User currentUser, boolean isSuperUser, String category, Pageable pageable) {
        if (isSuperUser) {
            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postResumeRepository.findResumeIdsByCategory(category, pageable);
            } else {
                return postResumeRepository.findAllResumeIds(pageable);
            }
        } else {
            if (currentUser == null) {
                return new SliceImpl<>(new ArrayList<>(), pageable, false);
            }

            if (category != null && !category.isEmpty() && !"all".equals(category)) {
                return postResumeRepository.findResumeIdsByUserAndCategory(currentUser, category, pageable);
            } else {
                return postResumeRepository.findResumeIdsByUser(currentUser, pageable);
            }
        }
    }

    private PaginationSliceRedactResponseDTO<ResumeDTO> createResumeResponse(
            List<ResumeDTO> content, int currentPage, int size, boolean hasMore) {
        PaginationSliceRedactResponseDTO<ResumeDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные успешно загружены");
        response.setRedact(content);
        response.setHasMore(hasMore);
        response.setCurrentPage(currentPage);
        response.setSize(content.size());
        return response;
    }

    private PaginationSliceRedactResponseDTO<ResumeDTO> createEmptyResumeResponse(int currentPage) {
        PaginationSliceRedactResponseDTO<ResumeDTO> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет резюме");
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(currentPage);
        response.setSize(0);
        return response;
    }

    // ==================== CONVERTERS (Конвертеры в DTO) ====================

    private List<PostTextDTO> convertToPostTextDTOs(List<PostText> posts) {
        log.debug("Converting {} posts to DTOs", posts.size());
        return posts.stream()
                .map(this::convertToPostTextDTO)
                .collect(Collectors.toList());
    }

    private PostTextDTO convertToPostTextDTO(PostText post) {
        PostTextDTO dto = new PostTextDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setViewCount(post.getViewCount());

        dto.setPremium(post.isPremium());
        dto.setDonate(post.isDonate());

        if (postTextCategoryConfig != null && post.getCategory() != null) {
            String displayName = postTextCategoryConfig.getCategoryDisplayName(post.getCategory());
            dto.setCategoryDisplayName(displayName);
        } else {
            dto.setCategoryDisplayName(post.getCategory());
        }

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
            PostTextDTO.UserDTO userDTO = new PostTextDTO.UserDTO();
            userDTO.setNameuse(post.getUser().getNameuse());
            userDTO.setUsername(post.getUser().getUsername());
            dto.setUser(userDTO);
        }

        return dto;
    }

    private List<PostTextJobDTO> convertToPostTextJobDTOs(List<PostTextJob> posts) {
        return posts.stream()
                .map(this::convertToPostTextJobDTO)
                .collect(Collectors.toList());
    }

    private PostTextJobDTO convertToPostTextJobDTO(PostTextJob post) {
        PostTextJobDTO dto = new PostTextJobDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setViewCount(post.getViewCount());
        dto.setPremium(post.isPremium());
        dto.setDonate(post.isDonate());


        if (postTextJobCategoryConfig != null && post.getCategory() != null) {
            String displayName = postTextJobCategoryConfig.getCategoryDisplayName(post.getCategory());
            dto.setCategoryDisplayName(displayName);
        } else {
            dto.setCategoryDisplayName(post.getCategory());
        }

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
            PostTextJobDTO.UserDTO userDTO = new PostTextJobDTO.UserDTO();
            userDTO.setNameuse(post.getUser().getNameuse());
            userDTO.setUsername(post.getUser().getUsername());
            dto.setUser(userDTO);
        }

        return dto;
    }

    private List<VacancyDTO> convertToVacancyDTOs(List<Vacancy> vacancies) {
        return vacancies.stream()
                .map(this::convertToVacancyDTO)
                .collect(Collectors.toList());
    }

    private VacancyDTO convertToVacancyDTO(Vacancy vacancy) {
        VacancyDTO dto = new VacancyDTO();
        dto.setId(vacancy.getId());
        dto.setHashId(vacancy.getHashId());
        dto.setTitle(vacancy.getTitle());
        dto.setLocation(vacancy.getLocation());
        dto.setCash(vacancy.getCash());
        dto.setCategory(vacancy.getCategory());
        String experienceDisplayName = experienceUtils.getExperienDisplayName("vacancy", vacancy.getExperience());
        String workformatDisplayName = workformatUtils.getWorkformatDisplayName("vacancy", vacancy.getWorkformat());
        dto.setExperienceDisplayName(experienceDisplayName);
        dto.setWorkformatDisplayName(workformatDisplayName);
        dto.setExperience(vacancy.getExperience());
        dto.setWorkformat(vacancy.getWorkformat());
        dto.setViewCount(vacancy.getViewCount());
        dto.setPremium(vacancy.isPremium());
        dto.setDonate(vacancy.isDonate());

        if (postVacancyCategoryConfig != null && vacancy.getCategory() != null) {
            String displayName = postVacancyCategoryConfig.getCategoryDisplayName(vacancy.getCategory());
            dto.setCategoryDisplayName(displayName);
        } else {
            dto.setCategoryDisplayName(vacancy.getCategory());
        }

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
            VacancyDTO.UserDTO userDTO = new VacancyDTO.UserDTO();
            userDTO.setNameuse(vacancy.getUser().getNameuse());
            userDTO.setUsername(vacancy.getUser().getUsername());
            dto.setUser(userDTO);
        }

        return dto;
    }

    private List<ResumeDTO> convertToResumeDTOs(List<Resume> resume) {
        return resume.stream()
                .map(this::convertToResumeDTO)
                .collect(Collectors.toList());
    }

    private ResumeDTO convertToResumeDTO(Resume resume) {
        ResumeDTO dto = new ResumeDTO();
        dto.setId(resume.getId());
        dto.setHashId(resume.getHashId());
        dto.setTitle(resume.getTitle());
        dto.setCash(resume.getCash());
        dto.setCategory(resume.getCategory());
        String experienceDisplayName = experienceUtils.getExperienDisplayName("resume", resume.getExperience());
        String workformatDisplayName = workformatUtils.getWorkformatDisplayName("resume", resume.getWorkformat());
        dto.setExperienceDisplayName(experienceDisplayName);
        dto.setWorkformatDisplayName(workformatDisplayName);
        dto.setExperience(resume.getExperience());
        dto.setWorkformat(resume.getWorkformat());
        dto.setViewCount(resume.getViewCount());

        if (postResumeCategoryConfig != null && resume.getCategory() != null) {
            String displayName = postResumeCategoryConfig.getCategoryDisplayName(resume.getCategory());
            dto.setCategoryDisplayName(displayName);
        } else {
            dto.setCategoryDisplayName(resume.getCategory());
        }

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
            ResumeDTO.UserDTO userDTO = new ResumeDTO.UserDTO();
            userDTO.setNameuse(resume.getUser().getNameuse());
            userDTO.setUsername(resume.getUser().getUsername());
            dto.setUser(userDTO);
        }

        return dto;
    }

    /**
     * Создаёт пустой ответ для неизвестного типа данных.
     *
     * @param type тип данных
     * @param <T>  тип DTO
     * @return пустой ответ с сообщением
     */
    private <T> PaginationSliceRedactResponseDTO<T> createEmptySliceRedactResponse(String type) {
        PaginationSliceRedactResponseDTO<T> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет данных для типа: " + type);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setSize(0);
        return response;
    }

    /**
     * Создаёт ответ с ошибкой.
     *
     * @param errorMessage сообщение об ошибке
     * @param <T>          тип DTO
     * @return ответ с ошибкой
     */
    private <T> PaginationSliceRedactResponseDTO<T> createErrorSliceRedactResponse(String errorMessage) {
        PaginationSliceRedactResponseDTO<T> response = new PaginationSliceRedactResponseDTO<>();
        response.setStatus("error");
        response.setMessage(errorMessage);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setSize(0);
        return response;
    }
}