package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.HealthPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.InteriorPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Дом и интерьер".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see InteriorPostText
 */
public interface InteriorPostRepository extends JpaRepository<InteriorPostText, Long> {

    Optional<InteriorPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Дом и интерьер.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param interiorType  тип Дом и интерьер
     * @param conditionInterior  состояние Дом и интерьер
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT i.id FROM InteriorPostText i " +
            "WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "AND (:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR i.category = :category) " +
            "AND (:region IS NULL OR i.user.region = :region) " +
            "AND (:city IS NULL OR i.user.city = :city " +
            "     OR (:city = 'other_none' AND (i.user.city IS NULL OR i.user.city = '' OR i.user.city = 'other' OR i.user.city = 'other_none'))) " +
            "AND (:interiorType IS NULL OR i.interiorType = :interiorType) " +
            "AND (:conditionInterior IS NULL OR i.conditionInterior = :conditionInterior)")
    Slice<Long> findInteriorPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("interiorType") String interiorType,
            @Param("conditionInterior") String conditionInterior,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param interiorType  тип Дом и интерьер
     * @param conditionInterior  состояние Дом и интерьер
     * @return количество постов
     */
    @Query("SELECT COUNT(i) FROM InteriorPostText i " +
            "WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "AND (:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR i.category = :category) " +
            "AND (:region IS NULL OR i.user.region = :region) " +
            "AND (:city IS NULL OR i.user.city = :city " +
            "     OR (:city = 'other_none' AND (i.user.city IS NULL OR i.user.city = '' OR i.user.city = 'other' OR i.user.city = 'other_none'))) " +
            "AND (:interiorType IS NULL OR i.interiorType = :interiorType) " +
            "AND (:conditionInterior IS NULL OR i.conditionInterior = :conditionInterior)")
    long countInteriorPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("interiorType") String interiorType,
            @Param("conditionInterior") String conditionInterior);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT i FROM InteriorPostText i WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "ORDER BY i.moderatedAt DESC")
    Slice<InteriorPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param interiorType  тип Дом и интерьер
     * @param conditionInterior  состояние Дом и интерьер
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT i.id FROM InteriorPostText i WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "AND (:interiorType IS NULL OR i.interiorType = :interiorType) " +
            "AND (:conditionInterior IS NULL OR i.conditionInterior = :conditionInterior) " +
            "ORDER BY i.moderatedAt DESC")
    Slice<Long> filterInteriorPostTextIdsSlice(@Param("interiorType") String interiorType,
                                               @Param("conditionInterior") String conditionInterior,
                                                    Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT i FROM InteriorPostText i " +
            "LEFT JOIN FETCH i.images " +
            "LEFT JOIN FETCH i.user " +
            "WHERE i.id IN :ids")
    List<InteriorPostText> findInteriorPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу.
     *
     * @param interiorType  тип Дом и интерьер
     * @param conditionInterior  состояние Дом и интерьер
     * @return количество постов
     */
    @Query("SELECT COUNT(i) FROM InteriorPostText i WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "AND (:interiorType IS NULL OR i.interiorType = :interiorType) " +
            "AND (:conditionInterior IS NULL OR i.conditionInterior = :conditionInterior)")
    long countInteriorPostText(@Param("interiorType") String interiorType,
                                    @Param("conditionInterior") String conditionInterior);

}
