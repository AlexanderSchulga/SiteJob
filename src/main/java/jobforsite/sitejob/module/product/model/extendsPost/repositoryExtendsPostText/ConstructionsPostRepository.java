package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ConstructionsPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Строительные материалы".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see ConstructionsPostText
 */

public interface ConstructionsPostRepository extends JpaRepository<ConstructionsPostText, Long> {

    Optional<ConstructionsPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Строительные материалы.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param constructionsType  тип Строительные материалы
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT c.id FROM ConstructionsPostText c " +
            "WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:constructionsType IS NULL OR c.constructionsType = :constructionsType)")
    Slice<Long> findConstructionsPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("constructionsType") String constructionsType,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param constructionsType  тип Строительные материалы
     * @return количество постов
     */
    @Query("SELECT COUNT(c) FROM ConstructionsPostText c " +
            "WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:constructionsType IS NULL OR c.constructionsType = :constructionsType)")
    long countConstructionsPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("constructionsType") String constructionsType);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT c FROM ConstructionsPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "ORDER BY c.moderatedAt DESC")
    Slice<ConstructionsPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param constructionsType  тип Строительные материалы
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT c.id FROM ConstructionsPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:constructionsType IS NULL OR c.constructionsType = :constructionsType) " +
            "ORDER BY c.moderatedAt DESC")
    Slice<Long> filterConstructionsPostTextIdsSlice(@Param("constructionsType") String constructionsType,
                                           Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT c FROM ConstructionsPostText c " +
            "LEFT JOIN FETCH c.images " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id IN :ids")
    List<ConstructionsPostText> findConstructionsPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param constructionsType  тип Строительные материалы
     * @return количество постов
     */
    @Query("SELECT COUNT(c) FROM ConstructionsPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:constructionsType IS NULL OR c.constructionsType = :constructionsType)")
    long countConstructionsPostText(@Param("constructionsType") String constructionsType);

}
