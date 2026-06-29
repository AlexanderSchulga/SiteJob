package jobforsite.sitejob.module.product.repository;

import jobforsite.sitejob.module.product.model.PostText;
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
 * Репозиторий для работы с сущностью PostText (текстовые объявления).
 * Содержит методы для поиска, фильтрации, пагинации объявлений по различным критериям:
 * - активные объявления (не заблокированы, не на модерации, не архивированы)
 * - заблокированные объявления
 * - объявления на модерации
 * - объявления пользователя
 * - поиск по заголовку и имени пользователя
 * - фильтрация по категории, региону, городу
 */
public interface PostRepo extends JpaRepository<PostText, Long> {

    // ========== Базовые методы подсчета и выборки ==========

    /**
     * Подсчитывает количество активных (не архивных, не заблокированных) объявлений пользователя.
     * @param targetUser пользователь
     * @return количество объявлений
     */
    long countByUserAndArchivedFalseAndBlockedFalse(User targetUser);

    /**
     * Находит все заблокированные объявления с пагинацией.
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    Slice<PostText> findByBlockedTrue(Pageable pageable);

    // ========== Поиск по заблокированным объявлениям ==========

    /**
     * Поиск заблокированных объявлений по заголовку (без учета регистра, частичное совпадение).
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.blocked = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostText> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по имени пользователя (nameuse) (без учета регистра, частичное совпадение).
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.blocked = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostText> searchByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.blocked = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<PostText> searchByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ========== Поиск по объявлениям на модерации ==========

    /**
     * Поиск среди объявлений на модерации по заголовку.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез объявлений на модерации
     */
    @Query("SELECT p FROM PostText p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostText> searchModerationByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск среди объявлений на модерации по имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез объявлений на модерации
     */
    @Query("SELECT p FROM PostText p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostText> searchModerationByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск среди объявлений на модерации по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез объявлений на модерации
     */
    @Query("SELECT p FROM PostText p WHERE p.moderation = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<PostText> searchModerationByTitleOrUserName(@Param("query") String query, Pageable pageable);

    // ========== Простые методы поиска ==========

    /**
     * Находит все объявления пользователя.
     * @param user пользователь
     * @return список объявлений пользователя
     */
    List<PostText> findByUser(User user);

    /**
     * Находит объявления по категории.
     * @param category категория
     * @return список объявлений в категории
     */
    List<PostText> findByCategory(String category);

    /**
     * Находит объявления по заголовку (без учета регистра, частичное совпадение).
     * @param title заголовок для поиска
     * @return список объявлений
     */
    List<PostText> findByTitleContainingIgnoreCase(String title);

    // ========== Page-методы для пагинации ==========

    /**
     * Находит объявления на модерации с пагинацией.
     * @param pageable параметры пагинации
     * @return страница объявлений на модерации
     */
    Page<PostText> findByModerationTrue(Pageable pageable);

    /**
     * Подсчитывает количество объявлений пользователя.
     * @param user пользователь
     * @return количество объявлений
     */
    long countByUser(User user);

    /**
     * Находит все объявления с пагинацией.
     * @param pageable параметры пагинации
     * @return страница объявлений
     */
    Page<PostText> findAll(Pageable pageable);

    /**
     * Находит объявления пользователя с пагинацией.
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return страница объявлений пользователя
     */
    Page<PostText> findByUser(User user, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных объявлений.
     * @return количество заблокированных объявлений
     */
    long countByBlockedTrue();

    /**
     * Подсчитывает количество объявлений на модерации.
     * @return количество объявлений на модерации
     */
    long countByModerationTrue();

    // ========== Методы работы с hashId ==========

    /**
     * Находит объявление по hashId.
     * @param hashId уникальный хеш-идентификатор
     * @return Optional с объявлением или пустой
     */
    Optional<PostText> findByHashId(String hashId);

    /**
     * Проверяет существование объявления с указанным hashId.
     * @param hashId уникальный хеш-идентификатор
     * @return true если существует
     */
    boolean existsByHashId(String hashId);

    // ========== Сложные методы фильтрации (Page) ==========

    /**
     * Поиск активных объявлений с фильтрацией по всем параметрам.
     * Активные = не архивированы, не заблокированы, не на модерации.
     * @param search поисковый запрос (по заголовку)
     * @param category категория
     * @param region регион пользователя
     * @param city город пользователя (значение 'other_none' обрабатывается как "другие/отсутствует")
     * @param pageable параметры пагинации
     * @return страница объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Page<PostText> searchWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Подсчет количества активных объявлений с фильтрацией по всем параметрам.
     * @param search поисковый запрос (по заголовку)
     * @param category категория
     * @param region регион пользователя
     * @param city город пользователя
     * @return количество объявлений
     */
    @Query("SELECT COUNT(p) FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
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

    // ========== Slice-методы для оптимизированной пагинации ==========

    /**
     * Находит ID активных объявлений пользователя с пагинацией (Slice).
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.user = :user AND p.archived = false AND p.blocked = false AND p.moderation = false ORDER BY p.createdAt DESC")
    Page<Long> findPostIdsByUserWithPagination(@Param("user") User user, Pageable pageable);

    /**
     * Загружает объявления по списку ID с предварительной загрузкой изображений и пользователя.
     * @param ids список ID объявлений
     * @return список объявлений
     */
    @Query("SELECT DISTINCT p FROM PostText p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids")
    List<PostText> findPostsByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Находит ID активных объявлений с фильтрацией (Slice).
     * @param search поисковый запрос
     * @param category категория
     * @param region регион
     * @param city город
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Slice<Long> findPostIdsWithFiltersSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Находит ID активных объявлений в городах, не входящих в список стандартных городов.
     * @param search поисковый запрос
     * @param category категория
     * @param region регион
     * @param standardCities список стандартных городов
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND ((:search IS NULL) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR " +
            "p.user.city = 'other_none' OR " +
            "(p.user.city NOT IN :standardCities)) " +
            "ORDER BY p.moderatedAt DESC")
    Slice<Long> findPostIdsByOtherCitiesSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities,
            Pageable pageable);

    // ========== Методы для заблокированных объявлений (Slice с ID) ==========

    /**
     * Находит ID заблокированных объявлений активных пользователей.
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> findBlockedProductIds(Pageable pageable);

    /**
     * Поиск ID заблокированных объявлений по заголовку.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedProductIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных объявлений по имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedProductIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных объявлений по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedProductIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных объявлений от активных пользователей.
     * @return количество
     */
    @Query("SELECT COUNT(p) FROM PostText p WHERE p.blocked = true AND p.user.active = true")
    long countByBlockedTrueAndUserActiveTrue();

    // ========== Методы для объявлений на модерации (Slice с ID) ==========

    /**
     * Находит ID объявлений на модерации.
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true ORDER BY p.createdAt DESC")
    Slice<Long> findModerationIds(Pageable pageable);

    /**
     * Поиск ID объявлений на модерации по заголовку.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID объявлений на модерации по имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID объявлений на модерации по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Загружает объявления по списку ID с предварительной загрузкой изображений и пользователя.
     * @param ids список ID объявлений
     * @return список объявлений
     */
    @Query("SELECT DISTINCT p FROM PostText p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids " +
            "ORDER BY p.createdAt DESC")
    List<PostText> findByIdsWithImagesAndUser(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных объявлений в нестандартных городах.
     * @param search поисковый запрос
     * @param category категория
     * @param region регион
     * @param standardCities список стандартных городов
     * @return количество объявлений
     */
    @Query("SELECT COUNT(p) FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
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

    // ========== Методы для редактора (управление объявлениями) ==========

    /**
     * Находит ID всех объявлений пользователя для редактора.
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.user = :user ORDER BY p.createdAt DESC")
    Slice<Long> findProductIdsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Находит ID объявлений пользователя по категории.
     * @param user пользователь
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.user = :user AND p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findProductIdsByUserAndCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);

    /**
     * Находит ID всех объявлений (для администратора/редактора).
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p ORDER BY p.createdAt DESC")
    Slice<Long> findAllProductIds(Pageable pageable);

    /**
     * Находит ID объявлений по категории (для администратора/редактора).
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findProductIdsByCategory(@Param("category") String category, Pageable pageable);






}