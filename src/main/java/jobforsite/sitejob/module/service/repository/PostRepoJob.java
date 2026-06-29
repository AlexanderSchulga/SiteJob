package jobforsite.sitejob.module.service.repository;

import jobforsite.sitejob.module.service.model.PostTextJob;
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
 * Репозиторий для работы с текстовыми объявлениями о работе (PostTextJob).
 * Обеспечивает поиск, фильтрацию, пагинацию и выборку ID для оптимизации запросов.
 * Поддерживает фильтрацию по: поисковому запросу, категории, региону, городу.
 * Включает методы для работы с заблокированными объявлениями и объявлениями на модерации.
 */
public interface PostRepoJob extends JpaRepository<PostTextJob, Long> {

    // ==================== Базовые методы ====================

    /**
     * Подсчитывает количество активных (не архивных, не заблокированных) объявлений пользователя.
     *
     * @param targetUser пользователь
     * @return количество объявлений
     */
    long countByUserAndArchivedFalseAndBlockedFalse(User targetUser);

    /**
     * Находит все заблокированные объявления с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return срез (slice) заблокированных объявлений
     */
    Slice<PostTextJob> findByBlockedTrue(Pageable pageable);

    /**
     * Находит объявления пользователя.
     *
     * @param user пользователь
     * @return список объявлений пользователя
     */
    List<PostTextJob> findByUser(User user);

    /**
     * Находит объявления по категории.
     *
     * @param category категория
     * @return список объявлений
     */
    List<PostTextJob> findByCategory(String category);

    /**
     * Находит объявления по заголовку (без учета регистра).
     *
     * @param title часть заголовка
     * @return список объявлений
     */
    List<PostTextJob> findByTitleContainingIgnoreCase(String title);

    /**
     * Находит объявление по hashId (уникальному публичному идентификатору).
     *
     * @param hashId уникальный идентификатор
     * @return Optional с объявлением или пустой
     */
    Optional<PostTextJob> findByHashId(String hashId);

    /**
     * Находит объявления на модерации с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница объявлений на модерации
     */
    Page<PostTextJob> findByModerationTrue(Pageable pageable);

    /**
     * Подсчитывает общее количество объявлений пользователя.
     *
     * @param user пользователь
     * @return количество объявлений
     */
    long countByUser(User user);

    /**
     * Находит все объявления с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница объявлений
     */
    Page<PostTextJob> findAll(Pageable pageable);

    /**
     * Находит объявления пользователя с пагинацией.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return страница объявлений пользователя
     */
    Page<PostTextJob> findByUser(User user, Pageable pageable);

    /**
     * Подсчитывает общее количество заблокированных объявлений.
     *
     * @return количество заблокированных объявлений
     */
    long countByBlockedTrue();

    /**
     * Подсчитывает общее количество объявлений на модерации.
     *
     * @return количество объявлений на модерации
     */
    long countByModerationTrue();

    /**
     * Проверяет существование объявления по hashId.
     *
     * @param hashId уникальный идентификатор
     * @return true, если объявление существует
     */
    boolean existsByHashId(String hashId);

    // ==================== Поиск заблокированных объявлений ====================

    /**
     * Поиск заблокированных объявлений по заголовку (для админ-панели).
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p FROM PostTextJob p WHERE p.blocked = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostTextJob> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по имени пользователя (для админ-панели).
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p FROM PostTextJob p WHERE p.blocked = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostTextJob> searchByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по заголовку или имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostTextJob p WHERE p.blocked = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<PostTextJob> searchByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ==================== Публичный поиск с фильтрами ====================

    /**
     * Полнотекстовый поиск объявлений с фильтрацией по категории, региону и городу.
     * Возвращает только активные объявления (не архивные, не заблокированные, прошедшие модерацию).
     *
     * @param search   поисковый запрос (по заголовку)
     * @param category категория (может быть null)
     * @param region   регион (может быть null)
     * @param city     город (специальное значение 'other_none' для прочих городов)
     * @param pageable параметры пагинации
     * @return страница объявлений
     */
    @Query("SELECT p FROM PostTextJob p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Page<PostTextJob> searchWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Подсчитывает количество объявлений по тем же фильтрам, что и searchWithAllFilters.
     * Используется для пагинации.
     *
     * @param search   поисковый запрос
     * @param category категория
     * @param region   регион
     * @param city     город
     * @return количество объявлений
     */
    @Query("SELECT COUNT(p) FROM PostTextJob p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    long countWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city);

    /**
     * Находит только ID активных объявлений по фильтрам (оптимизированный запрос).
     * Возвращает Slice<Long> для экономии памяти.
     *
     * @param search   поисковый запрос
     * @param category категория
     * @param region   регион
     * @param city     город
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Slice<Long> findPostJobIdsWithFiltersSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Находит ID объявлений в категории "Другие города" (города не из стандартного списка).
     *
     * @param search         поисковый запрос
     * @param category       категория
     * @param region         регион
     * @param standardCities список стандартных городов (исключаются из результата)
     * @param pageable       параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND ((:search IS NULL) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR " +
            "p.user.city = 'other_none' OR " +
            "(p.user.city NOT IN :standardCities)) " +
            "ORDER BY p.moderatedAt DESC")
    Slice<Long> findPostJobIdsByOtherCitiesSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities,
            Pageable pageable);

    /**
     * Подсчитывает количество объявлений в категории "Другие города".
     *
     * @param search         поисковый запрос
     * @param category       категория
     * @param region         регион
     * @param standardCities список стандартных городов
     * @return количество объявлений
     */
    @Query("SELECT COUNT(p) FROM PostTextJob p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND ((:search IS NULL) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR " +
            "p.user.city = 'other_none' OR " +
            "(p.user.city NOT IN :standardCities))")
    long countByOtherCities(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities);

    // ==================== Методы для страницы пользователя (личный кабинет) ====================

    /**
     * Находит ID объявлений пользователя (оптимизированный запрос).
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.user = :user ORDER BY p.createdAt DESC")
    Slice<Long> findServiceIdsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Находит ID объявлений пользователя по категории.
     *
     * @param user     пользователь
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.user = :user AND p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findServiceIdsByUserAndCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);

    /**
     * Находит ID активных объявлений пользователя для постраничной загрузки.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return страница ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.user = :user AND p.archived = false AND p.blocked = false AND p.moderation = false ORDER BY p.createdAt DESC")
    Page<Long> findPostIdsByUserWithPagination(@Param("user") User user, Pageable pageable);

    // ==================== Методы для админ-панели (заблокированные) ====================

    /**
     * Находит ID активных пользователей с заблокированными объявлениями.
     *
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.blocked = true AND p.user.active = true ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> findBlockedServiceIds(Pageable pageable);

    /**
     * Поиск заблокированных объявлений по заголовку (админ-панель).
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedServiceIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по имени пользователя (админ-панель).
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedServiceIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по заголовку или имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.blocked = true AND p.user.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedServiceIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных объявлений у активных пользователей.
     *
     * @return количество
     */
    @Query("SELECT COUNT(p) FROM PostTextJob p WHERE p.blocked = true AND p.user.active = true")
    long countByBlockedTrueAndUserActiveTrue();

    // ==================== Методы для админ-панели (модерация) ====================

    /**
     * Находит ID объявлений на модерации.
     *
     * @param pageable параметры пагинации
     * @return срез ID объявлений на модерации
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.moderation = true ORDER BY p.createdAt DESC")
    Slice<Long> findModerationIds(Pageable pageable);

    /**
     * Поиск объявлений на модерации по заголовку.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск объявлений на модерации по имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск объявлений на модерации по заголовку или имени пользователя.
     *
     * @param query    поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.moderation = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ==================== Методы для загрузки связанных сущностей (Fetch-запросы) ====================

    /**
     * Загружает объявления с их изображениями и пользователями по списку ID.
     * Используется после получения ID из Slice-запросов для оптимизации.
     *
     * @param ids список ID объявлений
     * @return список объявлений с подгруженными изображениями и пользователями
     */
    @Query("SELECT DISTINCT p FROM PostTextJob p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids")
    List<PostTextJob> findPostsByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Загружает объявления с изображениями и пользователями по списку ID с сортировкой.
     *
     * @param ids список ID объявлений
     * @return список объявлений
     */
    @Query("SELECT DISTINCT p FROM PostTextJob p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids " +
            "ORDER BY p.createdAt DESC")
    List<PostTextJob> findByIdsWithImagesAndUser(@Param("ids") List<Long> ids);

    // ==================== Общие методы получения ID ====================

    /**
     * Находит ID всех объявлений с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p ORDER BY p.createdAt DESC")
    Slice<Long> findAllServiceIds(Pageable pageable);

    /**
     * Находит ID объявлений по категории с пагинацией.
     *
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostTextJob p WHERE p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findServiceIdsByCategory(@Param("category") String category, Pageable pageable);
}