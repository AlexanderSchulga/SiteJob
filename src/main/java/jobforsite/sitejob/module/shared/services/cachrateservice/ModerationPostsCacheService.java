package jobforsite.sitejob.module.shared.services.cachrateservice;

import jobforsite.sitejob.module.resume.dto.ResumeBlockedDTO;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.shared.services.util.CategoryUtils;
import jobforsite.sitejob.module.shared.dto.ModerationSliceResponse;
import jobforsite.sitejob.module.product.dto.PostTextBlockedDTO;
import jobforsite.sitejob.module.service.dto.PostTextJobBlockedDTO;
import jobforsite.sitejob.module.vacancy.dto.VacancyBlockedDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для работы с кэшированием данных модерации.
 * <p>
 * Обеспечивает получение слайсов (Slice) сущностей, находящихся на модерации,
 * с пагинацией, поиском и кэшированием DTO. Содержит методы для работы с
 * товарами (PostText), услугами (PostTextJob), вакансиями (Vacancy) и резюме (Resume).
 * Также включает методы для получения количества элементов в модерации и очистки кэша.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ModerationPostsCacheService {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final CategoryUtils categoryUtils;
    private final RegionsConfig regionsConfig;

    // === СТАРЫЕ МЕТОДЫ ДЛЯ ОБРАТНОЙ СОВМЕСТИМОСТИ ===

    /**
     * Получает Slice товаров (PostText), находящихся на модерации, с пагинацией и поиском.
     * Метод сохранён для обратной совместимости.
     *
     * @param page        номер страницы (начиная с 0)
     * @param size        размер страницы
     * @param searchQuery поисковый запрос (может быть null)
     * @param searchType  тип поиска: "title", "nameuse", "all"
     * @return Slice с товарами
     */
    public Slice<PostText> getModerationProductsSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Loading moderation products slice - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postRepo.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postRepo.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postRepo.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postRepo.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<PostText> posts = postRepo.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, PostText> postMap = posts.stream()
                    .collect(Collectors.toMap(PostText::getId, Function.identity()));

            List<PostText> sortedPosts = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new SliceImpl<>(sortedPosts, pageable, idsSlice.hasNext());
        }

        return new SliceImpl<>(new ArrayList<>(), pageable, false);
    }

    /**
     * Получает Slice услуг (PostTextJob), находящихся на модерации, с пагинацией и поиском.
     * Метод сохранён для обратной совместимости.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return Slice с услугами
     */
    public Slice<PostTextJob> getModerationServicesSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Loading moderation services slice - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postRepoJob.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postRepoJob.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postRepoJob.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postRepoJob.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<PostTextJob> posts = postRepoJob.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, PostTextJob> postMap = posts.stream()
                    .collect(Collectors.toMap(PostTextJob::getId, Function.identity()));

            List<PostTextJob> sortedPosts = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new SliceImpl<>(sortedPosts, pageable, idsSlice.hasNext());
        }

        return new SliceImpl<>(new ArrayList<>(), pageable, false);
    }

    /**
     * Получает Slice вакансий (Vacancy), находящихся на модерации, с пагинацией и поиском.
     * Метод сохранён для обратной совместимости.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return Slice с вакансиями
     */
    public Slice<Vacancy> getModerationVacanciesSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Loading moderation vacancies slice - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // ШАГ 1: Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postVacancyRepository.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postVacancyRepository.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postVacancyRepository.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postVacancyRepository.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<Vacancy> posts = postVacancyRepository.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, Vacancy> postMap = posts.stream()
                    .collect(Collectors.toMap(Vacancy::getId, Function.identity()));

            List<Vacancy> sortedPosts = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new SliceImpl<>(sortedPosts, pageable, idsSlice.hasNext());
        }

        return new SliceImpl<>(new ArrayList<>(), pageable, false);
    }

    /**
     * Получает Slice резюме (Resume), находящихся на модерации, с пагинацией и поиском.
     * Метод сохранён для обратной совместимости.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return Slice с резюме
     */
    public Slice<Resume> getModerationResumeSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Loading moderation resume slice - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // ШАГ 1: Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postResumeRepository.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postResumeRepository.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postResumeRepository.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postResumeRepository.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<Resume> res = postResumeRepository.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, Resume> resMap = res.stream()
                    .collect(Collectors.toMap(Resume::getId, Function.identity()));

            List<Resume> sortedPosts = ids.stream()
                    .map(resMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new SliceImpl<>(sortedPosts, pageable, idsSlice.hasNext());
        }

        return new SliceImpl<>(new ArrayList<>(), pageable, false);
    }

    // === МЕТОДЫ С КЭШИРОВАНИЕМ DTO ===

    /**
     * Получает кэшированный ответ с DTO товаров, находящихся на модерации.
     * Результат кэшируется с использованием moderationCacheManager.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return ModerationSliceResponse с DTO товаров
     */
    @Cacheable(value = "moderationProductsDto",
            cacheManager = "moderationCacheManager",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public ModerationSliceResponse<PostTextBlockedDTO> getModerationProductsDto(int page, int size, String searchQuery, String searchType) {
        log.info("CACHE MISS for moderation products DTO - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postRepo.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postRepo.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postRepo.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postRepo.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH и конвертируем в DTO
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<PostText> posts = postRepo.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, PostText> postMap = posts.stream()
                    .collect(Collectors.toMap(PostText::getId, Function.identity()));

            List<PostTextBlockedDTO> dtos = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .map(post -> convertToPostTextModeratDTO(post, "ROLE_ADMIN"))
                    .collect(Collectors.toList());

            return new ModerationSliceResponse<>(dtos, idsSlice.hasNext(), dtos.size());
        }

        return new ModerationSliceResponse<>(new ArrayList<>(), false, 0);
    }

    /**
     * Получает кэшированный ответ с DTO услуг, находящихся на модерации.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return ModerationSliceResponse с DTO услуг
     */
    @Cacheable(value = "moderationServicesDto",
            cacheManager = "moderationCacheManager",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public ModerationSliceResponse<PostTextJobBlockedDTO> getModerationServicesDto(int page, int size, String searchQuery, String searchType) {
        log.info("CACHE MISS for moderation services DTO - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postRepoJob.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postRepoJob.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postRepoJob.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postRepoJob.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH и конвертируем в DTO
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<PostTextJob> posts = postRepoJob.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, PostTextJob> postMap = posts.stream()
                    .collect(Collectors.toMap(PostTextJob::getId, Function.identity()));

            List<PostTextJobBlockedDTO> dtos = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .map(post -> convertToPostTextJobModeratDTO(post, "ROLE_ADMIN"))
                    .collect(Collectors.toList());

            return new ModerationSliceResponse<>(dtos, idsSlice.hasNext(), dtos.size());
        }

        return new ModerationSliceResponse<>(new ArrayList<>(), false, 0);
    }

    /**
     * Получает кэшированный ответ с DTO вакансий, находящихся на модерации.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return ModerationSliceResponse с DTO вакансий
     */
    @Cacheable(value = "moderationVacanciesDto",
            cacheManager = "moderationCacheManager",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public ModerationSliceResponse<VacancyBlockedDTO> getModerationVacanciesDto(int page, int size, String searchQuery, String searchType) {
        log.info("CACHE MISS for moderation vacancies DTO - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postVacancyRepository.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postVacancyRepository.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postVacancyRepository.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postVacancyRepository.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH и конвертируем в DTO
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<Vacancy> posts = postVacancyRepository.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, Vacancy> postMap = posts.stream()
                    .collect(Collectors.toMap(Vacancy::getId, Function.identity()));

            List<VacancyBlockedDTO> dtos = ids.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull)
                    .map(vacancy -> convertToVacancyModeratDTO(vacancy, "ROLE_ADMIN"))
                    .collect(Collectors.toList());

            return new ModerationSliceResponse<>(dtos, idsSlice.hasNext(), dtos.size());
        }

        return new ModerationSliceResponse<>(new ArrayList<>(), false, 0);
    }

    /**
     * Получает кэшированный ответ с DTO резюме, находящихся на модерации.
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return ModerationSliceResponse с DTO резюме
     */
    @Cacheable(value = "moderationResumeDto",
            cacheManager = "moderationCacheManager",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public ModerationSliceResponse<ResumeBlockedDTO> getModerationResumeDto(int page, int size, String searchQuery, String searchType) {
        log.info("CACHE MISS for moderation resume DTO - page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<Long> idsSlice;

        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // Получаем только ID с пагинацией
        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idsSlice = postResumeRepository.searchModerationIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idsSlice = postResumeRepository.searchModerationIdsByUserName(normalizedQuery, pageable);
                    break;
                case "all":
                default:
                    idsSlice = postResumeRepository.searchModerationIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idsSlice = postResumeRepository.findModerationIds(pageable);
        }

        // Если есть ID - загружаем полные данные с JOIN FETCH и конвертируем в DTO
        if (idsSlice.hasContent()) {
            List<Long> ids = idsSlice.getContent();
            List<Resume> res = postResumeRepository.findByIdsWithImagesAndUser(ids);

            // Сортируем в том же порядке, что и ID (сохраняем пагинацию)
            Map<Long, Resume> resMap = res.stream()
                    .collect(Collectors.toMap(Resume::getId, Function.identity()));

            List<ResumeBlockedDTO> dtos = ids.stream()
                    .map(resMap::get)
                    .filter(Objects::nonNull)
                    .map(resume -> convertToResumeModeratDTO(resume, "ROLE_ADMIN"))
                    .collect(Collectors.toList());

            return new ModerationSliceResponse<>(dtos, idsSlice.hasNext(), dtos.size());
        }

        return new ModerationSliceResponse<>(new ArrayList<>(), false, 0);
    }

    // === МЕТОДЫ ДЛЯ COUNT ===

    /**
     * Возвращает количество товаров, находящихся на модерации.
     * Результат кэшируется.
     *
     * @return количество товаров в модерации
     */
    @Cacheable(value = "moderationProductsCount", cacheManager = "moderationCacheManager")
    public long getModerationProductsCount() {
        log.info("CACHE MISS for moderation products count - executing query");
        long count = postRepo.countByModerationTrue();
        log.debug("Found {} products in moderation", count);
        return count;
    }

    /**
     * Возвращает количество услуг, находящихся на модерации.
     * Результат кэшируется.
     *
     * @return количество услуг в модерации
     */
    @Cacheable(value = "moderationServicesCount", cacheManager = "moderationCacheManager")
    public long getModerationServicesCount() {
        log.info("CACHE MISS for moderation services count - executing query");
        long count = postRepoJob.countByModerationTrue();
        log.debug("Found {} services in moderation", count);
        return count;
    }

    /**
     * Возвращает количество вакансий, находящихся на модерации.
     * Результат кэшируется.
     *
     * @return количество вакансий в модерации
     */
    @Cacheable(value = "moderationVacanciesCount", cacheManager = "moderationCacheManager")
    public long getModerationVacanciesCount() {
        log.info("CACHE MISS for moderation vacancies count - executing query");
        long count = postVacancyRepository.countByModerationTrue();
        log.debug("Found {} vacancies in moderation", count);
        return count;
    }

    /**
     * Возвращает количество резюме, находящихся на модерации.
     * Результат кэшируется.
     *
     * @return количество резюме в модерации
     */
    @Cacheable(value = "moderationResumeCount", cacheManager = "moderationCacheManager")
    public long getModerationResumeCount() {
        log.info("CACHE MISS for moderation resume count - executing query");
        long count = postResumeRepository.countByModerationTrue();
        log.debug("Found {} resume in moderation", count);
        return count;
    }

    /**
     * Возвращает общее количество объявлений на модерации (всех типов)
     */
    public long getTotalModerationCount() {
        return getModerationProductsCount() +
                getModerationServicesCount() +
                getModerationVacanciesCount() +
                getModerationResumeCount();
    }

    // === МЕТОДЫ КОНВЕРТАЦИИ ===

    /**
     * Конвертирует сущность PostText в DTO PostTextBlockedDTO.
     *
     * @param post            сущность товара
     * @param currentUserRole роль текущего пользователя (определяет, будут ли видны ID и username)
     * @return DTO для модерации
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
            if ("ROLE_SUPER".equals(currentUserRole)) {
                userDTO.setId(post.getUser().getId());
                userDTO.setUsername(post.getUser().getUsername());
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
     * Конвертирует сущность PostTextJob в DTO PostTextJobBlockedDTO.
     *
     * @param post            сущность услуги
     * @param currentUserRole роль текущего пользователя
     * @return DTO для модерации
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
     * Конвертирует сущность Vacancy в DTO VacancyBlockedDTO.
     *
     * @param vacancy         сущность вакансии
     * @param currentUserRole роль текущего пользователя
     * @return DTO для модерации
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
     * Конвертирует сущность Resume в DTO ResumeBlockedDTO.
     *
     * @param resume          сущность резюме
     * @param currentUserRole роль текущего пользователя
     * @return DTO для модерации
     */
    private ResumeBlockedDTO convertToResumeModeratDTO(Resume resume, String currentUserRole) {
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

    // === МЕТОДЫ ОЧИСТКИ КЭША ===

    /**
     * Очищает кэш для товаров (DTO и счётчик).
     */
    @CacheEvict(value = {"moderationProductsDto", "moderationProductsCount"},
            cacheManager = "moderationCacheManager", allEntries = true)
    public void clearModerationProductsCache() {
        log.info("Moderation products DTO cache cleared");
    }

    /**
     * Очищает кэш для услуг (DTO и счётчик).
     */
    @CacheEvict(value = {"moderationServicesDto", "moderationServicesCount"},
            cacheManager = "moderationCacheManager", allEntries = true)
    public void clearModerationServicesCache() {
        log.info("Moderation services DTO cache cleared");
    }

    /**
     * Очищает кэш для вакансий (DTO и счётчик).
     */
    @CacheEvict(value = {"moderationVacanciesDto", "moderationVacanciesCount"},
            cacheManager = "moderationCacheManager", allEntries = true)
    public void clearModerationVacanciesCache() {
        log.info("Moderation vacancies DTO cache cleared");
    }

    /**
     * Очищает кэш для резюме (DTO и счётчик).
     */
    @CacheEvict(value = {"moderationResumeDto", "moderationResumeCount"},
            cacheManager = "moderationCacheManager", allEntries = true)
    public void clearModerationResumeCache() {
        log.info("Moderation resume DTO cache cleared");
    }

    /**
     * Очищает все кэши, связанные с модерацией (все DTO и счётчики).
     */
    @CacheEvict(value = {"moderationProductsDto", "moderationServicesDto", "moderationVacanciesDto", "moderationResumeDto",
            "moderationProductsCount", "moderationServicesCount", "moderationVacanciesCount", "moderationResumeCount"},
            cacheManager = "moderationCacheManager", allEntries = true)
    public void clearAllModerationCaches() {
        log.info("All moderation caches cleared");
    }
}