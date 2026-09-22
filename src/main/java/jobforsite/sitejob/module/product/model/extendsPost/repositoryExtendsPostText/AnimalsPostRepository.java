package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ConstructionsPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Флора и фауна".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see AnimalsPostText
 */

public interface AnimalsPostRepository extends JpaRepository<AnimalsPostText, Long> {

    Optional<AnimalsPostText> findByHashId(String hashId);

    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу животных.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param animalsKind  тип животных
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    @Query("SELECT f.id FROM AnimalsPostText f " +
            "WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:search IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:region IS NULL OR f.user.region = :region) " +
            "AND (:city IS NULL OR f.user.city = :city " +
            "     OR (:city = 'other_none' AND (f.user.city IS NULL OR f.user.city = '' OR f.user.city = 'other' OR f.user.city = 'other_none'))) " +
            "AND (:animalsKind IS NULL OR f.animalsKind = :animalsKind)")
    Slice<Long> findAnimalsPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("animalsKind") String animalsKind,

            Pageable pageable);

    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param animalsKind  тип животных
     * @return количество постов
     */
    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    @Query("SELECT COUNT(f) FROM AnimalsPostText f " +
            "WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:search IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:region IS NULL OR f.user.region = :region) " +
            "AND (:city IS NULL OR f.user.city = :city " +
            "     OR (:city = 'other_none' AND (f.user.city IS NULL OR f.user.city = '' OR f.user.city = 'other' OR f.user.city = 'other_none'))) " +
            "AND (:animalsKind IS NULL OR f.animalsKind = :animalsKind)")
    long countAnimalsPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("animalsKind") String animalsKind);


    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */

    @Query("SELECT f FROM AnimalsPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "ORDER BY f.moderatedAt DESC")
    Slice<AnimalsPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param animalsKind тип животных
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT f.id FROM AnimalsPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:animalsKind IS NULL OR f.animalsKind = :animalsKind) " +
            "ORDER BY f.moderatedAt DESC")
    Slice<Long> filterAnimalsPostTextIdsSlice(@Param("animalsKind") String animalsKind,
                                           Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT f FROM AnimalsPostText f " +
            "LEFT JOIN FETCH f.images " +
            "LEFT JOIN FETCH f.user " +
            "WHERE f.id IN :ids")
    List<AnimalsPostText> findAnimalsPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param animalsKind тип животных
     * @return количество постов
     */
    @Query("SELECT COUNT(f) FROM AnimalsPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:animalsKind IS NULL OR f.animalsKind = :animalsKind)")
    long countAnimalsPostText(@Param("animalsKind") String animalsKind);

}
