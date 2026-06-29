package jobforsite.sitejob.module.resume.repository;

import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью Resume.
 * Предоставляет методы для поиска, фильтрации и пагинации резюме,
 * включая заблокированные, на модерации, а также поиск с различными фильтрами.
 */
public interface PostResumeRepository extends JpaRepository<Resume, Long> {

    // ===== Базовые подсчеты и простые запросы =====

    /**
     * Подсчитывает количество активных (не архивных и не заблокированных) резюме пользователя.
     *
     * @param targetUser пользователь
     * @return количество резюме
     */
    long countByUserAndArchivedFalseAndBlockedFalse(User targetUser);

    /**
     * Находит все заблокированные резюме с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с заблокированными резюме
     */
    Slice<Resume> findByBlockedTrue(Pageable pageable);

    // ===== Поиск среди заблокированных резюме =====

    /**
     * Поиск заблокированных резюме по заголовку (без учета регистра, частичное совпадение).
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с найденными резюме
     */
    @Query("SELECT p FROM Resume p WHERE p.blocked = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<Resume> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных резюме по имени пользователя (nameuse).
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с найденными резюме
     */
    @Query("SELECT p FROM Resume p WHERE p.blocked = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<Resume> searchByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных резюме по заголовку ИЛИ имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с найденными резюме
     */
    @Query("SELECT p FROM Resume p WHERE p.blocked = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<Resume> searchByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ===== Поиск среди резюме на модерации =====

    /**
     * Поиск среди резюме на модерации по заголовку.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с найденными резюме
     */
    @Query("SELECT p FROM Resume p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<Resume> searchModerationByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск среди резюме на модерации по имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с найденными резюме
     */
    @Query("SELECT p FROM Resume p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<Resume> searchModerationByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск среди резюме на модерации по заголовку ИЛИ имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с найденными резюме
     */
    @Query("SELECT p FROM Resume p WHERE p.moderation = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<Resume> searchModerationByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ===== Базовые запросы =====

    /**
     * Находит все резюме пользователя.
     *
     * @param user пользователь
     * @return список резюме
     */
    List<Resume> findByUser(User user);

    /**
     * Находит резюме по категории.
     *
     * @param category категория
     * @return список резюме
     */
    List<Resume> findByCategory(String category);

    /**
     * Находит резюме по опыту работы.
     *
     * @param workformat формат работы
     * @return список резюме
     */
    List<Resume> findByWorkformat(String workformat);


    /**
     * Находит резюме по опыту работы.
     *
     * @param experience опыт работы
     * @return список резюме
     */
    List<Resume> findByExperience(String experience);

    /**
     * Находит резюме по частичному совпадению заголовка (без учета регистра).
     *
     * @param title часть заголовка
     * @return список резюме
     */
    List<Resume> findByTitleContainingIgnoreCase(String title);

    /**
     * Находит резюме на модерации с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с резюме на модерации
     */
    Page<Resume> findByModerationTrue(Pageable pageable);

    /**
     * Подсчитывает количество резюме пользователя.
     *
     * @param user пользователь
     * @return количество резюме
     */
    long countByUser(User user);

    /**
     * Возвращает все резюме с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с резюме
     */
    Page<Resume> findAll(Pageable pageable);

    /**
     * Находит резюме пользователя с пагинацией.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return страница с резюме пользователя
     */
    Page<Resume> findByUser(User user, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных резюме.
     *
     * @return количество заблокированных
     */
    long countByBlockedTrue();

    /**
     * Подсчитывает количество резюме на модерации.
     *
     * @return количество на модерации
     */
    long countByModerationTrue();

    /**
     * Находит резюме по hashId.
     *
     * @param hashId хеш-идентификатор
     * @return Optional с резюме
     */
    Optional<Resume> findByHashId(String hashId);

    /**
     * Проверяет существование резюме по hashId.
     *
     * @param hashId хеш-идентификатор
     * @return true если существует
     */
    boolean existsByHashId(String hashId);

    // ===== Расширенный поиск с фильтрами =====

    /**
     * Поиск резюме с учетом всех фильтров (поиск, категория, опыт, регион, город).
     * Возвращает только активные резюме (не архивные, не заблокированные, не на модерации).
     *
     * @param search     поисковый запрос (по заголовку)
     * @param category   категория (может быть null)
     * @param experience опыт (может быть null)
     * @param workformat формат работы
     * @param region     регион (может быть null)
     * @param city       город (может быть null)
     * @param pageable   параметры пагинации
     * @return страница с результатами поиска
     */
    @Query("SELECT p FROM Resume p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:experience IS NULL OR p.experience = :experience) " +
            "AND (:workformat IS NULL OR :workformat = 'all' OR p.workformat = :workformat) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Page<Resume> searchWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Подсчет количества резюме с учетом всех фильтров.
     *
     * @param search     поисковый запрос
     * @param category   категория
     * @param experience опыт
     * @param workformat формат работы
     * @param region     регион
     * @param city       город
     * @return количество найденных резюме
     */
    @Query("SELECT COUNT(p) FROM Resume p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:experience IS NULL OR p.experience = :experience) " +
            "AND (:workformat IS NULL OR :workformat = 'all' OR p.workformat = :workformat) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    long countWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("city") String city);

    // ===== Запросы с пагинацией по ID (для оптимизации) =====

    /**
     * Находит ID активных резюме пользователя с пагинацией.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return страница с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.user = :user AND p.archived = false AND p.blocked = false AND p.moderation = false ORDER BY p.createdAt DESC")
    Page<Long> findResIdsByUserWithPagination(@Param("user") User user, Pageable pageable);

    /**
     * Загружает резюме по списку ID с изображениями и пользователем (один запрос).
     *
     * @param ids список ID резюме
     * @return список резюме с заполненными связями
     */
    @Query("SELECT DISTINCT p FROM Resume p " +
            "LEFT JOIN FETCH p.imagesresume " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids")
    List<Resume> findResumeByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Находит ID активных резюме с фильтрами в виде Slice.
     *
     * @param search     поисковый запрос
     * @param category   категория
     * @param experience опыт
     * @param workformat формат работы
     * @param region     регион
     * @param city       город
     * @param pageable   параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:experience IS NULL OR p.experience = :experience) " +
            "AND (:workformat IS NULL OR :workformat = 'all' OR p.workformat = :workformat) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Slice<Long> findResIdsWithFiltersSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Находит ID активных резюме для городов, не входящих в список стандартных (категория "Другое").
     *
     * @param search         поисковый запрос
     * @param category       категория
     * @param experience     опыт
     * @param workformat формат работы
     * @param region         регион
     * @param standardCities список стандартных городов
     * @param pageable       параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:experience IS NULL OR p.experience = :experience) " +
            "AND (:workformat IS NULL OR p.workformat = :workformat) " +
            "AND ((:search IS NULL) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR " +
            "p.user.city = 'other_none' OR " +
            "(p.user.city NOT IN :standardCities)) " +
            "ORDER BY p.moderatedAt DESC")
    Slice<Long> findResIdsByOtherCitiesSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities,
            Pageable pageable);

    // ===== Запросы для заблокированных резюме (с ID) =====

    /**
     * Находит ID заблокированных резюме активных пользователей.
     *
     * @param pageable параметры пагинации
     * @return Slice с ID заблокированных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.blocked = true AND p.user.active = true ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> findBlockedResumeIds(Pageable pageable);

    /**
     * Поиск ID заблокированных резюме по заголовку.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с ID найденных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedResumeIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных резюме по имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с ID найденных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedResumeIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных резюме по заголовку ИЛИ имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с ID найденных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.blocked = true AND p.user.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedResumeIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных резюме активных пользователей.
     *
     * @return количество
     */
    @Query("SELECT COUNT(p) FROM Resume p WHERE p.blocked = true AND p.user.active = true")
    long countByBlockedTrueAndUserActiveTrue();

    // ===== Запросы для резюме на модерации (с ID) =====

    /**
     * Находит ID резюме на модерации.
     *
     * @param pageable параметры пагинации
     * @return Slice с ID резюме на модерации
     */
    @Query("SELECT p.id FROM Resume p WHERE p.moderation = true ORDER BY p.createdAt DESC")
    Slice<Long> findModerationIds(Pageable pageable);

    /**
     * Поиск ID резюме на модерации по заголовку.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с ID найденных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID резюме на модерации по имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с ID найденных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID резюме на модерации по заголовку ИЛИ имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return Slice с ID найденных резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.moderation = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ===== Загрузка с JOIN FETCH =====

    /**
     * Загружает резюме по списку ID с изображениями и пользователем.
     * Сортировка по дате создания (новые сверху).
     *
     * @param ids список ID резюме
     * @return список резюме с заполненными связями
     */
    @Query("SELECT DISTINCT p FROM Resume p " +
            "LEFT JOIN FETCH p.imagesresume " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids " +
            "ORDER BY p.createdAt DESC")
    List<Resume> findByIdsWithImagesAndUser(@Param("ids") List<Long> ids);

    /**
     * Подсчет активных резюме для городов не из стандартного списка.
     *
     * @param search         поисковый запрос
     * @param category       категория
     * @param experience     опыт
     * @param workformat формат работы
     * @param region         регион
     * @param standardCities список стандартных городов
     * @return количество резюме
     */
    @Query("SELECT COUNT(p) FROM Resume p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:experience IS NULL OR p.experience = :experience) " +
            "AND (:workformat IS NULL OR p.workformat = :workformat) " +
            "AND ((:search IS NULL) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR " +
            "p.user.city = 'other_none' OR " +
            "(p.user.city NOT IN :standardCities))")
    long countByOtherCities(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities);

    // ===== Методы для редактора =====

    /**
     * Находит ID всех резюме пользователя (для редактора).
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.user = :user ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Находит ID резюме пользователя по категории.
     *
     * @param user     пользователь
     * @param category категория
     * @param pageable параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.user = :user AND p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByUserAndCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);

    /**
     * Находит ID резюме пользователя по опыту работы.
     *
     * @param user       пользователь
     * @param experience опыт
     * @param pageable   параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.user = :user AND p.experience = :experience ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByUserAndExperience(@Param("user") User user, @Param("experience") String experience, Pageable pageable);

    /**
     * Находит ID резюме пользователя по формату работы.
     *
     * @param user       пользователь
     * @param workformat формат работы
     * @param pageable   параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.user = :user AND p.workformat = :workformat ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByUserAndWorkformat(@Param("user") User user, @Param("workformat") String workformat, Pageable pageable);

    /**
     * Находит ID всех резюме (для редактора).
     *
     * @param pageable параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p ORDER BY p.createdAt DESC")
    Slice<Long> findAllResumeIds(Pageable pageable);

    /**
     * Находит ID резюме по категории.
     *
     * @param category категория
     * @param pageable параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByCategory(@Param("category") String category, Pageable pageable);

    /**
     * Находит ID резюме по опыту работы.
     *
     * @param experience опыт
     * @param pageable   параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.experience = :experience ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByExperience(@Param("experience") String experience, Pageable pageable);

    /**
     * Находит ID резюме по формату работы.
     *
     * @param workformat опыт
     * @param pageable   параметры пагинации
     * @return Slice с ID резюме
     */
    @Query("SELECT p.id FROM Resume p WHERE p.workformat = :workformat ORDER BY p.createdAt DESC")
    Slice<Long> findResumeIdsByWorkformat(@Param("workformat") String workformat, Pageable pageable);
}