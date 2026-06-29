package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис кеширования данных пользователя.
 * <p>
 * Предназначен для оптимизации загрузки пользовательского контента (товаров, услуг,
 * вакансий, резюме) с использованием Spring Cache. Реализует паттерн "ленивой загрузки"
 * с пагинацией и оптимизированной выборкой изображений.
 * </p>
 *
 * <p><b>Кешируемые данные:</b></p>
 * <ul>
 *   <li>{@code userByHash} — пользователь по хэш-идентификатору</li>
 *   <li>{@code userProducts} — страница товаров пользователя</li>
 *   <li>{@code userServices} — страница услуг пользователя</li>
 *   <li>{@code userVacancies} — страница вакансий пользователя</li>
 *   <li>{@code userResume} — страница резюме пользователя</li>
 *   <li>{@code postCounts} — количество постов разных типов</li>
 * </ul>
 *
 * <p><b>Особенности оптимизации:</b></p>
 * <ul>
 *   <li>Сначала загружаются только ID постов с пагинацией</li>
 *   <li>Затем — полные данные с изображениями по списку ID</li>
 *   <li>Это решает проблему N+1 запроса</li>
 * </ul>
 *
 * @author YourTeam
 * @version 1.0
 */
@RequiredArgsConstructor
@Service
public class UserCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserCacheService.class);

    private final UserRepository userRepository;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    /**
     * Возвращает пользователя по хэш-идентификатору с использованием кеша.
     * <p>
     * Результат кешируется в {@code userByHash}. Если пользователь не найден,
     * возвращается {@code null}, который НЕ кешируется (unless = "#result == null").
     * </p>
     *
     * @param userHash уникальный хэш-идентификатор пользователя
     * @return объект {@link User} или {@code null}, если не найден
     */
    @Cacheable(value = "userByHash", unless = "#result == null", key = "#userHash")
    public User findUserByHashCached(String userHash) {
        log.debug("Cache miss for user by hash");
        Optional<User> userOpt = userRepository.findByHashId(userHash);
        return userOpt.orElse(null);
    }

    /**
     * Возвращает страницу товаров пользователя с пагинацией.
     * <p>
     * Ключ кеша формируется из ID пользователя, номера страницы и размера страницы.
     * Использует оптимизированную загрузку: сначала получает ID, затем посты с изображениями.
     * </p>
     *
     * @param user пользователь
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return страница товаров {@link Page<PostText>}
     */
    @Cacheable(value = "userProducts", key = "#user.id + '_' + #page + '_' + #size")
    public Page<PostText> getProductsPage(User user, int page, int size) {
        log.debug("Cache miss for products page: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return loadProductsPage(user, pageable);
    }

    /**
     * Возвращает страницу услуг пользователя с пагинацией.
     * <p>
     * Ключ кеша формируется из ID пользователя, номера страницы и размера страницы.
     * </p>
     *
     * @param user пользователь
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return страница услуг {@link Page<PostTextJob>}
     */
    @Cacheable(value = "userServices", key = "#user.id + '_' + #page + '_' + #size")
    public Page<PostTextJob> getServicesPage(User user, int page, int size) {
        log.debug("Cache miss for services page: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return loadServicesPage(user, pageable);
    }

    /**
     * Возвращает страницу вакансий пользователя с пагинацией.
     *
     * @param user пользователь
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return страница вакансий {@link Page<Vacancy>}
     */
    @Cacheable(value = "userVacancies", key = "#user.id + '_' + #page + '_' + #size")
    public Page<Vacancy> getVacanciesPage(User user, int page, int size) {
        log.debug("Cache miss for vacancies page: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return loadVacanciesPage(user, pageable);
    }

    /**
     * Возвращает страницу резюме пользователя с пагинацией.
     *
     * @param user пользователь
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return страница резюме {@link Page<Resume>}
     */
    @Cacheable(value = "userResume", key = "#user.id + '_' + #page + '_' + #size")
    public Page<Resume> getResumePage(User user, int page, int size) {
        log.debug("Cache miss for resume page: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        return loadResumePage(user, pageable);
    }

    /**
     * Возвращает общее количество активных товаров пользователя.
     *
     * @param user пользователь
     * @return количество товаров
     */
    @Cacheable(value = "postCounts", key = "'products_' + #user.id")
    public long getTotalProductsCount(User user) {
        log.debug("Cache miss for products count");
        return postRepo.countByUserAndArchivedFalseAndBlockedFalse(user);
    }

    /**
     * Возвращает общее количество активных услуг пользователя.
     *
     * @param user пользователь
     * @return количество услуг
     */
    @Cacheable(value = "postCounts", key = "'services_' + #user.id")
    public long getTotalServicesCount(User user) {
        log.debug("Cache miss for services count");
        return postRepoJob.countByUserAndArchivedFalseAndBlockedFalse(user);
    }

    /**
     * Возвращает общее количество активных вакансий пользователя.
     *
     * @param user пользователь
     * @return количество вакансий
     */
    @Cacheable(value = "postCounts", key = "'vacancies_' + #user.id")
    public long getTotalVacanciesCount(User user) {
        log.debug("Cache miss for vacancies count");
        return postVacancyRepository.countByUserAndArchivedFalseAndBlockedFalse(user);
    }

    /**
     * Возвращает общее количество активных резюме пользователя.
     *
     * @param user пользователь
     * @return количество резюме
     */
    @Cacheable(value = "postCounts", key = "'resume_' + #user.id")
    public long getTotalResumeCount(User user) {
        log.debug("Cache miss for resume count");
        return postResumeRepository.countByUserAndArchivedFalseAndBlockedFalse(user);
    }

    /**
     * Получает все данные пользователя для первой загрузки профиля.
     * <p>
     * Агрегирует товары, услуги, вакансии и их счетчики в один объект
     * для минимизации количества запросов к клиенту.
     * </p>
     *
     * @param user         пользователь
     * @param productPage  номер страницы товаров
     * @param servicePage  номер страницы услуг
     * @param vacancyPage  номер страницы вакансий
     * @param size         размер страницы
     * @return объект {@link UserPostsData} со всеми данными
     */
    public UserPostsData getUserPostsData(User user, int productPage, int servicePage, int vacancyPage, int size) {
        Page<PostText> productsPage = getProductsPage(user, productPage, size);
        Page<PostTextJob> servicesPage = getServicesPage(user, servicePage, size);
        Page<Vacancy> vacanciesPage = getVacanciesPage(user, vacancyPage, size);

        long totalProducts = getTotalProductsCount(user);
        long totalServices = getTotalServicesCount(user);
        long totalVacancies = getTotalVacanciesCount(user);

        return new UserPostsData(
                productsPage.getContent(),
                servicesPage.getContent(),
                vacanciesPage.getContent(),
                totalProducts,
                totalServices,
                totalVacancies,
                productsPage.hasNext(),
                servicesPage.hasNext(),
                vacanciesPage.hasNext()
        );
    }

    /**
     * Очищает весь кеш, связанный с пользователем.
     * <p>
     * Должен вызываться при любых изменениях пользовательских данных:
     * <ul>
     *   <li>Создание/удаление/изменение поста</li>
     *   <li>Изменение профиля пользователя</li>
     *   <li>Архивация/разблокировка постов</li>
     * </ul>
     * </p>
     *
     * @param userHash хэш-идентификатор пользователя (не используется в логике, сохранен для API)
     */
    @CacheEvict(value = {"userByHash", "userProducts", "userServices", "userVacancies", "userResume", "postCounts"},
            allEntries = true)
    public void clearUserCache(String userHash) {
        log.debug("User cache cleared");
    }

    // ==================== private load methods ====================

    /**
     * Оптимизированная загрузка товаров с изображениями.
     * <p>
     * Сначала получает только ID постов с пагинацией, затем загружает полные данные
     * с изображениями по списку ID. Это решает проблему N+1 запроса.
     * </p>
     */
    private Page<PostText> loadProductsPage(User user, Pageable pageable) {
        Page<Long> postIdsPage = postRepo.findPostIdsByUserWithPagination(user, pageable);

        if (postIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<PostText> postsWithImages = postRepo.findPostsByIdsWithImages(postIdsPage.getContent());

        // Сохраняем порядок, соответствующий порядку ID из пагинации
        Map<Long, PostText> postMap = postsWithImages.stream()
                .collect(Collectors.toMap(PostText::getId, p -> p));

        List<PostText> orderedPosts = postIdsPage.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedPosts, pageable, postIdsPage.getTotalElements());
    }

    /**
     * Оптимизированная загрузка услуг с изображениями.
     *
     * @see #loadProductsPage(User, Pageable)
     */
    private Page<PostTextJob> loadServicesPage(User user, Pageable pageable) {
        Page<Long> postIdsPage = postRepoJob.findPostIdsByUserWithPagination(user, pageable);

        if (postIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<PostTextJob> postsWithImages = postRepoJob.findPostsByIdsWithImages(postIdsPage.getContent());

        Map<Long, PostTextJob> postMap = postsWithImages.stream()
                .collect(Collectors.toMap(PostTextJob::getId, p -> p));

        List<PostTextJob> orderedPosts = postIdsPage.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedPosts, pageable, postIdsPage.getTotalElements());
    }

    /**
     * Оптимизированная загрузка вакансий с изображениями.
     *
     * @see #loadProductsPage(User, Pageable)
     */
    private Page<Vacancy> loadVacanciesPage(User user, Pageable pageable) {
        Page<Long> postIdsPage = postVacancyRepository.findVacancyIdsByUserWithPagination(user, pageable);

        if (postIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Vacancy> vacanciesWithImages = postVacancyRepository.findVacanciesByIdsWithImages(postIdsPage.getContent());

        Map<Long, Vacancy> vacancyMap = vacanciesWithImages.stream()
                .collect(Collectors.toMap(Vacancy::getId, v -> v));

        List<Vacancy> orderedVacancies = postIdsPage.getContent().stream()
                .map(vacancyMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedVacancies, pageable, postIdsPage.getTotalElements());
    }

    /**
     * Оптимизированная загрузка резюме с изображениями.
     */
    private Page<Resume> loadResumePage(User user, Pageable pageable) {
        Page<Long> postIdsPage = postResumeRepository.findResIdsByUserWithPagination(user, pageable);

        if (postIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Resume> resumeWithImages = postResumeRepository.findResumeByIdsWithImages(postIdsPage.getContent());

        Map<Long, Resume> resumeMap = resumeWithImages.stream()
                .collect(Collectors.toMap(Resume::getId, v -> v));

        List<Resume> orderedResume = postIdsPage.getContent().stream()
                .map(resumeMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedResume, pageable, postIdsPage.getTotalElements());
    }

    // ==================== inner class ====================

    /**
     * DTO-класс, агрегирующий все данные пользователя для первой загрузки профиля.
     * <p>
     * Содержит списки товаров, услуг, вакансий, их общее количество и флаги наличия
     * следующих страниц. Используется для передачи в контроллер/шаблон одним объектом.
     * </p>
     */
    @RequiredArgsConstructor
    public static class UserPostsData {
        private final List<PostText> posts;
        private final List<PostTextJob> postsJob;
        private final List<Vacancy> vacancies;

        private final long totalProducts;
        private final long totalServices;
        private final long totalVacancies;

        private final boolean hasMoreProducts;
        private final boolean hasMoreServices;
        private final boolean hasMoreVacancies;

        public List<PostText> getPosts() { return posts; }
        public List<PostTextJob> getPostsJob() { return postsJob; }
        public List<Vacancy> getVacancies() { return vacancies; }

        public long getTotalProducts() { return totalProducts; }
        public long getTotalServices() { return totalServices; }
        public long getTotalVacancies() { return totalVacancies; }

        public boolean isHasMoreProducts() { return hasMoreProducts; }
        public boolean isHasMoreServices() { return hasMoreServices; }
        public boolean isHasMoreVacancies() { return hasMoreVacancies; }
    }
}