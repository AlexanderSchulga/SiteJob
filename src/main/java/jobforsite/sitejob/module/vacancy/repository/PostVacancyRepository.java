package jobforsite.sitejob.module.vacancy.repository;

import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с вакансиями (Vacancy).
 * Предоставляет методы поиска, фильтрации, пагинации,
 * а также специализированные запросы для модерации,
 * блокировок и работы с черновиками/редактором.
 */
public interface PostVacancyRepository extends JpaRepository<Vacancy, Long> {

    /**
     * Подсчитывает количество активных (не архивных, не заблокированных) вакансий пользователя.
     *
     * @param targetUser пользователь
     * @return количество активных вакансий
     */
    long countByUserAndArchivedFalseAndBlockedFalse(User targetUser);

    /**
     * Возвращает срез заблокированных вакансий с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return срез заблокированных вакансий
     */
    Slice<Vacancy> findByBlockedTrue(Pageable pageable);

    /**
     * Поиск заблокированных вакансий по заголовку (без учета регистра).
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных вакансий
     */
    @Query("SELECT v FROM Vacancy v WHERE v.blocked = true AND LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<Vacancy> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных вакансий по имени пользователя (без учета регистра).
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных вакансий
     */
    @Query("SELECT v FROM Vacancy v WHERE v.blocked = true AND LOWER(v.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<Vacancy> searchByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных вакансий по заголовку или имени пользователя.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных вакансий
     */
    @Query("SELECT v FROM Vacancy v WHERE v.blocked = true AND " +
            "(LOWER(v.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(v.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<Vacancy> searchByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Находит все вакансии пользователя.
     *
     * @param user пользователь
     * @return список вакансий
     */
    List<Vacancy> findByUser(User user);

    /**
     * Находит все вакансии по категории.
     *
     * @param category категория
     * @return список вакансий
     */
    List<Vacancy> findByCategory(String category);

    /**
     * Находит вакансию по опыту работы.
     *
     * @param experience опыт работы
     * @return список вакансий
     */
    List<Vacancy> findByExperience(String experience);

    /**
     * Находит вакансии по формату работы.
     *
     * @param workformat формат работы
     * @return список вакансий
     */
    List<Vacancy> findByWorkformat(String workformat);

    /**
     * Поиск вакансий по части заголовка (без учета регистра).
     *
     * @param title часть заголовка
     * @return список вакансий
     */
    List<Vacancy> findByTitleContainingIgnoreCase(String title);

    /**
     * Находит вакансию по её хеш-идентификатору.
     *
     * @param hashId хеш ID
     * @return Optional с вакансией или пустой
     */
    Optional<Vacancy> findByHashId(String hashId);

    /**
     * Возвращает страницу вакансий, ожидающих модерации.
     *
     * @param pageable параметры пагинации
     * @return страница вакансий на модерации
     */
    Page<Vacancy> findByModerationTrue(Pageable pageable);

    /**
     * Подсчитывает общее количество вакансий пользователя.
     *
     * @param user пользователь
     * @return количество вакансий
     */
    long countByUser(User user);

    /**
     * Возвращает страницу всех вакансий.
     *
     * @param pageable параметры пагинации
     * @return страница вакансий
     */
    Page<Vacancy> findAll(Pageable pageable);

    /**
     * Возвращает страницу вакансий пользователя.
     *
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return страница вакансий
     */
    Page<Vacancy> findByUser(User user, Pageable pageable);

    /**
     * Проверяет существование вакансии с указанным хеш-идентификатором.
     *
     * @param hashId хеш ID
     * @return true если существует
     */
    boolean existsByHashId(String hashId);

    /**
     * Подсчитывает общее количество заблокированных вакансий.
     *
     * @return количество заблокированных
     */
    long countByBlockedTrue();

    /**
     * Подсчитывает общее количество вакансий на модерации.
     *
     * @return количество на модерации
     */
    long countByModerationTrue();

    /**
     * Расширенный поиск вакансий с фильтрацией по поисковому запросу, категории, опыту, формату работы, региону и городу.
     * Возвращает только активные вакансии (не архивные, не заблокированные, не на модерации).
     *
     * @param search поисковый запрос (по заголовку)
     * @param category категория
     * @param experience опыт (может быть null)
     * @param workformat формат работы (может быть null)
     * @param region регион
     * @param city город (специальное значение 'other_none' для прочих городов)
     * @param pageable параметры пагинации
     * @return страница отфильтрованных вакансий
     */
    @Query("SELECT p FROM Vacancy p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:experience IS NULL OR p.experience = :experience) " +
            "AND (:workformat IS NULL OR :workformat = 'all' OR p.workformat = :workformat) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Page<Vacancy> searchWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Подсчитывает количество вакансий, соответствующих фильтрам (без пагинации).
     *
     * @param search поисковый запрос
     * @param category категория
     * @param experience опыт
     * @param workformat формат работы
     * @param region регион
     * @param city город
     * @return количество
     */
    @Query("SELECT COUNT(p) FROM Vacancy p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
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

    /**
     * Возвращает страницу идентификаторов активных вакансий пользователя,
     * отсортированных по дате создания (новые сверху).
     *
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return страница ID вакансий
     */
    @Query("SELECT v.id FROM Vacancy v WHERE v.user = :user AND v.archived = false AND v.blocked = false AND v.moderation = false ORDER BY v.createdAt DESC")
    Page<Long> findVacancyIdsByUserWithPagination(@Param("user") User user, Pageable pageable);

    /**
     * Загружает вакансии по списку ID с предварительной загрузкой изображений и данных пользователя.
     * Используется для оптимизации N+1 запросов.
     *
     * @param ids список ID вакансий
     * @return список вакансий с заполненными связями
     */
    @Query("SELECT DISTINCT v FROM Vacancy v " +
            "LEFT JOIN FETCH v.imagesvacancy " +
            "LEFT JOIN FETCH v.user " +
            "WHERE v.id IN :ids")
    List<Vacancy> findVacanciesByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Возвращает срез идентификаторов активных вакансий с применением всех фильтров.
     * Используется для эффективной пагинации без подсчета общего количества.
     *
     * @param search поисковый запрос
     * @param category категория
     * @param experience опыт
     * @param workformat формат работы
     * @param region регион
     * @param city город
     * @param pageable параметры пагинации
     * @return срез ID вакансий
     */
    @Query("SELECT v.id FROM Vacancy v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:search IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:experience IS NULL OR v.experience = :experience) " +
            "AND (:workformat IS NULL OR :workformat = 'all' OR v.workformat = :workformat) " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:city IS NULL OR v.user.city = :city " +
            "     OR (:city = 'other_none' AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR v.user.city = 'other_none')))")
    Slice<Long> findVacancyIdsWithFiltersSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Возвращает срез идентификаторов вакансий с фильтром "другие города"
     * (города, не входящие в список стандартных).
     *
     * @param search поисковый запрос
     * @param category категория
     * @param experience опыт
     * @param workformat формат работы
     * @param region регион
     * @param standardCities список стандартных городов
     * @param pageable параметры пагинации
     * @return срез ID вакансий из нестандартных городов
     */
    @Query("SELECT v.id FROM Vacancy v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:experience IS NULL OR v.experience = :experience) " +
            "AND (:workformat IS NULL OR v.workformat = :workformat) " +
            "AND ((:search IS NULL) OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR " +
            "v.user.city = 'other_none' OR " +
            "(v.user.city NOT IN :standardCities)) " +
            "ORDER BY v.moderatedAt DESC")
    Slice<Long> findVacancyIdsByOtherCitiesSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities,
            Pageable pageable);

    // ==================== ЗАБЛОКИРОВАННЫЕ ВАКАНСИИ ====================

    /**
     * Возвращает срез ID заблокированных вакансий (только с активными пользователями),
     * отсортированных по дате блокировки и ID.
     *
     * @param pageable параметры пагинации
     * @return срез ID заблокированных вакансий
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.blocked = true AND p.user.active = true ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> findBlockedVacancyIds(Pageable pageable);

    /**
     * Поиск ID заблокированных вакансий по заголовку.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedVacancyIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных вакансий по имени пользователя.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedVacancyIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных вакансий по заголовку или имени пользователя.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.blocked = true AND p.user.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedVacancyIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных вакансий у активных пользователей.
     *
     * @return количество
     */
    @Query("SELECT COUNT(p) FROM Vacancy p WHERE p.blocked = true AND p.user.active = true")
    long countByBlockedTrueAndUserActiveTrue();

    // ==================== ВАКАНСИИ НА МОДЕРАЦИИ ====================

    /**
     * Возвращает срез ID вакансий, ожидающих модерации, отсортированных по дате создания.
     *
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.moderation = true ORDER BY p.createdAt DESC")
    Slice<Long> findModerationIds(Pageable pageable);

    /**
     * Поиск ID вакансий на модерации по заголовку.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID вакансий на модерации по имени пользователя.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID вакансий на модерации по заголовку или имени пользователя.
     *
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.moderation = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ С FETCH ====================

    /**
     * Загружает вакансии по списку ID с предварительной загрузкой изображений и пользователя,
     * отсортированные по дате создания.
     *
     * @param ids список ID вакансий
     * @return список вакансий
     */
    @Query("SELECT DISTINCT v FROM Vacancy v " +
            "LEFT JOIN FETCH v.imagesvacancy " +
            "LEFT JOIN FETCH v.user " +
            "WHERE v.id IN :ids " +
            "ORDER BY v.createdAt DESC")
    List<Vacancy> findByIdsWithImagesAndUser(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных вакансий в нестандартных городах
     * (города не из списка стандартных).
     *
     * @param search поисковый запрос
     * @param category категория
     * @param experience опыт
     * @param workformat формат работы
     * @param region регион
     * @param standardCities список стандартных городов
     * @return количество
     */
    @Query("SELECT COUNT(v) FROM Vacancy v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:experience IS NULL OR v.experience = :experience) " +
            "AND (:workformat IS NULL OR v.workformat = :workformat) " +
            "AND ((:search IS NULL) OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR " +
            "v.user.city = 'other_none' OR " +
            "(v.user.city NOT IN :standardCities))")
    long countByOtherCities(
            @Param("search") String search,
            @Param("category") String category,
            @Param("experience") String experience,
            @Param("workformat") String workformat,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities);

    // ==================== МЕТОДЫ ДЛЯ РЕДАКТОРА ====================

    /**
     * Возвращает срез ID всех вакансий пользователя (включая архивные, заблокированные, на модерации).
     *
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT v.id FROM Vacancy v WHERE v.user = :user ORDER BY v.createdAt DESC")
    Slice<Long> findVacancyIdsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Возвращает срез ID вакансий пользователя по категории.
     *
     * @param user пользователь
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT v.id FROM Vacancy v WHERE v.user = :user AND v.category = :category ORDER BY v.createdAt DESC")
    Slice<Long> findVacancyIdsByUserAndCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);

    /**
     * Находит ID вакансии пользователя по опыту работы.
     *
     * @param user       пользователь
     * @param experience опыт
     * @param pageable   параметры пагинации
     * @return Slice с ID вакансий
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.user = :user AND p.experience = :experience ORDER BY p.createdAt DESC")
    Slice<Long> findVacancyIdsByUserAndExperience(@Param("user") User user, @Param("experience") String experience, Pageable pageable);

    /**
     * Находит ID вакансии пользователя по формату работы.
     *
     * @param user       пользователь
     * @param workformat формат работы
     * @param pageable   параметры пагинации
     * @return Slice с ID вакансий
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.user = :user AND p.workformat = :workformat ORDER BY p.createdAt DESC")
    Slice<Long> findVacancyIdsByUserAndWorkformat(@Param("user") User user, @Param("workformat") String workformat, Pageable pageable);

    /**
     * Возвращает срез ID всех вакансий в системе (для административных нужд).
     *
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT v.id FROM Vacancy v ORDER BY v.createdAt DESC")
    Slice<Long> findAllVacancyIds(Pageable pageable);

    /**
     * Возвращает срез ID вакансий по категории.
     *
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID
     */
    @Query("SELECT v.id FROM Vacancy v WHERE v.category = :category ORDER BY v.createdAt DESC")
    Slice<Long> findVacancyIdsByCategory(@Param("category") String category, Pageable pageable);

    /**
     * Находит ID вакансий по опыту работы.
     *
     * @param experience опыт
     * @param pageable   параметры пагинации
     * @return Slice с ID вакансий
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.experience = :experience ORDER BY p.createdAt DESC")
    Slice<Long> findVacancyIdsByExperience(@Param("experience") String experience, Pageable pageable);

    /**
     * Находит ID вакансий по формату работы.
     *
     * @param workformat формат работы
     * @param pageable   параметры пагинации
     * @return Slice с ID вакансий
     */
    @Query("SELECT p.id FROM Vacancy p WHERE p.workformat = :workformat ORDER BY p.createdAt DESC")
    Slice<Long> findVacancyIdsByWorkformat(@Param("workformat") String workformat, Pageable pageable);
}