package jobforsite.sitejob.module.product.repository;

import jobforsite.sitejob.module.product.model.PostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами (товарами).
 * <p>
 * Предоставляет методы для поиска, фильтрации и подсчета постов
 * с учетом региона, города, категории и поискового запроса.
 * </p>
 *
 * @see PostText
 */
public interface PostTextRepository extends JpaRepository<PostText, Long> {

    /**
     * Находит пост по хэш-идентификатору.
     *
     * @param hashId хэш-идентификатор поста
     * @return Optional с постом или пустой Optional
     */
    Optional<PostText> findByHashId(String hashId);

    /**
     * Проверяет существование поста по хэш-идентификатору.
     *
     * @param hashId хэш-идентификатор поста
     * @return true если пост существует
     */
    boolean existsByHashId(String hashId);

    /**
     * Находит ID активных постов с фильтрацией по региону и городу.
     * <p>
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * Поддерживает специальное значение "other_none" для городов,
     * которое показывает посты из других городов региона.
     * </p>
     *
     * @param category категория поста (может быть null)
     * @param search   поисковый запрос по заголовку (может быть null)
     * @param region   регион пользователя (может быть null)
     * @param city     город пользователя (может быть null или "other_none")
     * @param pageable параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT p.id FROM PostText p " +
            "WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    Slice<Long> findActivePostIdsWithRegionAndCity(
            @Param("category") String category,
            @Param("search") String search,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * Подсчитывает количество активных постов с фильтрацией по региону и городу.
     *
     * @param category категория поста (может быть null)
     * @param search   поисковый запрос по заголовку (может быть null)
     * @param region   регион пользователя (может быть null)
     * @param city     город пользователя (может быть null или "other_none")
     * @return количество постов
     */
    @Query("SELECT COUNT(p) FROM PostText p " +
            "WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none')))")
    long countActivePostsWithRegionAndCity(
            @Param("category") String category,
            @Param("search") String search,
            @Param("region") String region,
            @Param("city") String city);

    /**
     * Находит ID активных постов без фильтрации по региону и городу.
     * <p>
     * Устаревший метод для обратной совместимости.
     * Рекомендуется использовать {@link #findActivePostIdsWithRegionAndCity}.
     * </p>
     *
     * @param category категория поста (может быть null)
     * @param search   поисковый запрос по заголовку (может быть null)
     * @param pageable параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT p.id FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY p.moderatedAt DESC")
    Slice<Long> findActivePostIdsSlice(@Param("category") String category,
                                       @Param("search") String search,
                                       Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     * <p>
     * Использует FETCH JOIN для оптимизации запросов и предотвращения N+1 проблемы.
     * </p>
     *
     * @param ids список ID постов
     * @return список постов с загруженными изображениями и пользователем
     */
    @Query("SELECT DISTINCT p FROM PostText p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids")
    List<PostText> findPostsByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по категории и поиску.
     *
     * @param category категория поста (может быть null)
     * @param search   поисковый запрос по заголовку (может быть null)
     * @return количество постов
     */
    @Query("SELECT COUNT(p) FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    long countActivePosts(@Param("category") String category, @Param("search") String search);
}