package jobforsite.sitejob.module.users.controller;

import jobforsite.sitejob.module.resume.dto.ResumeDTO;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.dto.PaginationResponseDTO;
import jobforsite.sitejob.module.product.dto.PostTextDTO;
import jobforsite.sitejob.module.service.dto.PostTextJobDTO;
import jobforsite.sitejob.module.vacancy.dto.VacancyDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для пагинированной загрузки объявлений пользователя (административный интерфейс).
 * <p>
 * Позволяет администратору с правами SUPER_ACCESS просматривать все объявления
 * любого пользователя с поддержкой пагинации и фильтрацией по типу контента:
 * товары, услуги, вакансии, резюме.
 * </p>
 */
@RequiredArgsConstructor
@Controller
public class UpdateUserPagination extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserPagination.class);

    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;
    private final PostResumeCategoryConfig postResumeCategoryConfig;
    private final ExperienceResumCategoryConfig experienceResumCategoryConfig;

    private final ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;

    private final WorkFormatResumeConfig workFormatResumeConfig;

    private final WorkFormatVacancyConfig workFormatVacancyConfig;

    private final UserRepository userRepository;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    /**
     * Загружает объявления пользователя по его хеш-идентификатору с пагинацией.
     * <p>
     * Доступ только для пользователей с правами SUPER_ACCESS.
     * Поддерживаемые типы контента:
     * <ul>
     *     <li>products - товары/объявления</li>
     *     <li>services - услуги</li>
     *     <li>vacancies - вакансии</li>
     *     <li>resume - резюме</li>
     * </ul>
     * </p>
     *
     * @param hashId         хеш-идентификатор пользователя, чьи объявления загружаются
     * @param type           тип контента (products, services, vacancies, resume)
     * @param page           номер страницы (начиная с 0)
     * @param size           количество элементов на странице (по умолчанию 8)
     * @param currentUser    текущий аутентифицированный пользователь (не используется напрямую)
     * @param authentication объект аутентификации для проверки прав
     * @return ResponseEntity с пагинированным ответом, содержащим DTO объявлений
     */
    @GetMapping("/users/{hashId}/admin/posts/load-more")
    @ResponseBody
    public ResponseEntity<PaginationResponseDTO<?>> loadMoreUserPosts(
            @PathVariable("hashId") String hashId,
            @RequestParam("type") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @AuthenticationPrincipal User currentUser,
            Authentication authentication) {

        log.debug("Admin load-more posts request: hashId={}, type={}, page={}, size={}", hashId, type, page, size);

        try {
            // Проверка прав доступа
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка загрузки объявлений пользователя без прав SUPER_ACCESS");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Доступ запрещен"));
            }

            // Загружаем пользователя по hashId
            Optional<User> userOptional = userRepository.findByHashId(hashId);
            if (userOptional.isEmpty()) {
                log.warn("Пользователь с указанным hashId не найден");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Пользователь не найден"));
            }

            User targetUser = userOptional.get();

            // Корректировка номера страницы
            if (page < 0) {
                page = 0;
            }

            // Создание запроса с сортировкой по дате создания (новые сверху)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            PaginationResponseDTO<?> response;

            // Выбор типа контента
            switch (type.toLowerCase()) {
                case "products":
                    log.debug("Загрузка товаров пользователя, страница {}", page);
                    Page<PostText> productsPage = postRepo.findByUser(targetUser, pageable);
                    List<PostTextDTO> productDTOs = convertToPostTextDTOs(productsPage.getContent());
                    response = createPaginationResponse(productDTOs, productsPage, page);
                    break;

                case "services":
                    log.debug("Загрузка услуг пользователя, страница {}", page);
                    Page<PostTextJob> servicesPage = postRepoJob.findByUser(targetUser, pageable);
                    List<PostTextJobDTO> serviceDTOs = convertToPostTextJobDTOs(servicesPage.getContent());
                    response = createPaginationResponse(serviceDTOs, servicesPage, page);
                    break;

                case "vacancies":
                    log.debug("Загрузка вакансий пользователя, страница {}", page);
                    Page<Vacancy> vacanciesPage = postVacancyRepository.findByUser(targetUser, pageable);
                    List<VacancyDTO> vacancyDTOs = convertToVacancyDTOs(vacanciesPage.getContent());
                    response = createPaginationResponse(vacancyDTOs, vacanciesPage, page);
                    break;

                case "resume":
                    log.debug("Загрузка резюме пользователя, страница {}", page);
                    Page<Resume> resumePage = postResumeRepository.findByUser(targetUser, pageable);
                    List<ResumeDTO> resumeDTOs = convertToResumeDTOs(resumePage.getContent());
                    response = createPaginationResponse(resumeDTOs, resumePage, page);
                    break;

                default:
                    log.warn("Неизвестный тип контента: {}", type);
                    response = createEmptyResponse(type);
            }

            log.debug("Успешная загрузка {} элементов, всего страниц: {}",
                    response.getRedact() != null ? response.getRedact().size() : 0,
                    response.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при загрузке объявлений пользователя: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Ошибка загрузки: " + e.getMessage()));
        }
    }

    // ==================== Вспомогательные методы для конвертации в DTO ====================

    /**
     * Конвертирует список сущностей PostText в список DTO.
     *
     * @param posts список сущностей объявлений
     * @return список DTO для клиентской стороны
     */
    private List<PostTextDTO> convertToPostTextDTOs(List<PostText> posts) {
        return posts.stream()
                .map(this::convertToPostTextDTO)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность PostText в DTO.
     *
     * @param post сущность объявления
     * @return DTO объявления
     */
    private PostTextDTO convertToPostTextDTO(PostText post) {
        PostTextDTO dto = new PostTextDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName(postTextCategoryConfig.getCategoryDisplayName(post.getCategory()));
        dto.setFirstImagePath(post.getFirstImagePath());
        dto.setFullText(post.getFull_text());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setModeratedAt(post.getModeratedAt());
        dto.setBlocked(post.isBlocked());
        dto.setArchived(post.isArchived());
        dto.setModeration(post.isModeration());
        dto.setPremium(post.isPremium());
        dto.setDonate(post.isDonate());
        return dto;
    }

    /**
     * Конвертирует список сущностей PostTextJob в список DTO.
     *
     * @param posts список сущностей услуг
     * @return список DTO услуг
     */
    private List<PostTextJobDTO> convertToPostTextJobDTOs(List<PostTextJob> posts) {
        return posts.stream()
                .map(this::convertToPostTextJobDTO)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность PostTextJob в DTO.
     *
     * @param post сущность услуги
     * @return DTO услуги
     */
    private PostTextJobDTO convertToPostTextJobDTO(PostTextJob post) {
        PostTextJobDTO dto = new PostTextJobDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName(postTextJobCategoryConfig.getCategoryDisplayName(post.getCategory()));
        dto.setFirstImagePath(post.getFirstImagePath());
        dto.setFullText(post.getFull_text());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setModeratedAt(post.getModeratedAt());
        dto.setBlocked(post.isBlocked());
        dto.setArchived(post.isArchived());
        dto.setModeration(post.isModeration());
        dto.setPremium(post.isPremium());
        dto.setDonate(post.isDonate());

        return dto;
    }

    /**
     * Конвертирует список сущностей Vacancy в список DTO.
     *
     * @param vacancies список сущностей вакансий
     * @return список DTO вакансий
     */
    private List<VacancyDTO> convertToVacancyDTOs(List<Vacancy> vacancies) {
        return vacancies.stream()
                .map(this::convertToVacancyDTO)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность Vacancy в DTO.
     *
     * @param vacancy сущность вакансии
     * @return DTO вакансии
     */
    private VacancyDTO convertToVacancyDTO(Vacancy vacancy) {
        VacancyDTO dto = new VacancyDTO();
        dto.setId(vacancy.getId());
        dto.setHashId(vacancy.getHashId());
        dto.setTitle(vacancy.getTitle());
        dto.setCash(vacancy.getCash());
        dto.setCategory(vacancy.getCategory());
        dto.setExperience(vacancy.getExperience());
        dto.setWorkformat(vacancy.getWorkformat());
        dto.setCategoryDisplayName(postVacancyCategoryConfig.getCategoryDisplayName(vacancy.getCategory()));
        dto.setExperienceDisplayName(experienceVacancyCategoryConfig.getExperienDisplayName(vacancy.getExperience()));
        dto.setWorkformatDisplayName(workFormatVacancyConfig.getWorkformatDisplayName(vacancy.getWorkformat()));
        dto.setFirstImagePath(vacancy.getFirstImagePath());
        dto.setFullText(vacancy.getFull_text());
        dto.setCreatedAt(vacancy.getCreatedAt());
        dto.setModeratedAt(vacancy.getModeratedAt());
        dto.setBlocked(vacancy.isBlocked());
        dto.setArchived(vacancy.isArchived());
        dto.setModeration(vacancy.isModeration());
        dto.setPremium(vacancy.isPremium());
        dto.setDonate(vacancy.isDonate());
        return dto;
    }

    /**
     * Конвертирует список сущностей Resume в список DTO.
     *
     * @param resume список сущностей резюме
     * @return список DTO резюме
     */
    private List<ResumeDTO> convertToResumeDTOs(List<Resume> resume) {
        return resume.stream()
                .map(this::convertToResumeDTO)
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует сущность Resume в DTO.
     *
     * @param resume сущность резюме
     * @return DTO резюме
     */
    private ResumeDTO convertToResumeDTO(Resume resume) {
        ResumeDTO dto = new ResumeDTO();
        dto.setId(resume.getId());
        dto.setHashId(resume.getHashId());
        dto.setTitle(resume.getTitle());
        dto.setCash(resume.getCash());
        dto.setCategory(resume.getCategory());
        dto.setExperience(resume.getExperience());
        dto.setWorkformat(resume.getWorkformat());
        dto.setCategoryDisplayName(postResumeCategoryConfig.getCategoryDisplayName(resume.getCategory()));
        dto.setExperienceDisplayName(experienceResumCategoryConfig.getExperienDisplayName(resume.getExperience()));
        dto.setWorkformatDisplayName(workFormatResumeConfig.getWorkformatDisplayName(resume.getWorkformat()));
        dto.setFirstImagePath(resume.getFirstImagePath());
        dto.setFullText(resume.getFull_text());
        dto.setCreatedAt(resume.getCreatedAt());
        dto.setModeratedAt(resume.getModeratedAt());
        dto.setBlocked(resume.isBlocked());
        dto.setArchived(resume.isArchived());
        dto.setModeration(resume.isModeration());
        return dto;
    }

    // ==================== Вспомогательные методы для формирования ответов ====================

    /**
     * Создает стандартный пагинированный ответ с данными.
     *
     * @param content     список DTO-объектов для текущей страницы
     * @param page        объект страницы из Spring Data
     * @param currentPage текущий номер страницы
     * @param <T>         тип DTO-объекта
     * @return пагинированный ответ API
     */
    private <T> PaginationResponseDTO<T> createPaginationResponse(List<T> content, Page<?> page, int currentPage) {
        PaginationResponseDTO<T> response = new PaginationResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Данные успешно загружены");
        response.setRedact(content);
        response.setHasMore(page.hasNext());
        response.setCurrentPage(currentPage);
        response.setTotalPages(page.getTotalPages());
        response.setTotalElements(page.getTotalElements());
        return response;
    }

    /**
     * Создает пустой ответ для указанного типа контента.
     *
     * @param type тип контента
     * @param <T>  тип DTO-объекта
     * @return пустой пагинированный ответ
     */
    private <T> PaginationResponseDTO<T> createEmptyResponse(String type) {
        PaginationResponseDTO<T> response = new PaginationResponseDTO<>();
        response.setStatus("success");
        response.setMessage("Нет данных для типа: " + type);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setTotalPages(0);
        response.setTotalElements(0);
        return response;
    }

    /**
     * Создает ответ с ошибкой.
     *
     * @param errorMessage текст сообщения об ошибке
     * @param <T>          тип DTO-объекта
     * @return пагинированный ответ с ошибкой
     */
    private <T> PaginationResponseDTO<T> createErrorResponse(String errorMessage) {
        PaginationResponseDTO<T> response = new PaginationResponseDTO<>();
        response.setStatus("error");
        response.setMessage(errorMessage);
        response.setRedact(List.of());
        response.setHasMore(false);
        response.setCurrentPage(0);
        response.setTotalPages(0);
        response.setTotalElements(0);
        return response;
    }
}