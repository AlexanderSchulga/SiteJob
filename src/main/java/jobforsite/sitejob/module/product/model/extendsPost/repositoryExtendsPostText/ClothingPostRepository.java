package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ClothingPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Одежда и обувь".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see ClothingPostText
 */
public interface ClothingPostRepository extends JpaRepository<ClothingPostText, Long> {

    Optional<ClothingPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====

    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Одежда и обувь.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param genderClothing  тип Одежда и обувь
     * @param conditionClothing  Состояние Одежда и обувь
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */

    @Query("SELECT c.id FROM ClothingPostText c " +
            "WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:clothingType IS NULL OR c.clothingType = :clothingType) " +
            "AND (:genderClothing IS NULL OR c.genderClothing = :genderClothing) " +
            "AND (:conditionClothing IS NULL OR c.conditionClothing = :conditionClothing)")
    Slice<Long> findClothingPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("clothingType") String clothingType,
            @Param("genderClothing") String genderClothing,
            @Param("conditionClothing") String conditionClothing,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param genderClothing  тип Одежда и обувь
     * @param conditionClothing  Состояние Одежда и обувь
     * @return количество постов
     */
    @Query("SELECT COUNT(c) FROM ClothingPostText c " +
            "WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:clothingType IS NULL OR c.clothingType = :clothingType) " +
            "AND (:genderClothing IS NULL OR c.genderClothing = :genderClothing) " +
            "AND (:conditionClothing IS NULL OR c.conditionClothing = :conditionClothing)")
    long countClothingPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("clothingType") String clothingType,
            @Param("genderClothing") String genderClothing,
            @Param("conditionClothing") String conditionClothing);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT c FROM ClothingPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "ORDER BY c.moderatedAt DESC")
    Slice<ClothingPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param genderClothing  тип Одежда и обувь
     * @param conditionClothing  Состояние Одежда и обувь
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT c.id FROM ClothingPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:clothingType IS NULL OR c.clothingType = :clothingType) " +
            "AND (:genderClothing IS NULL OR c.genderClothing = :genderClothing) " +
            "AND (:conditionClothing IS NULL OR c.conditionClothing = :conditionClothing) " +
            "ORDER BY c.moderatedAt DESC")
    Slice<Long> filterClothingPostTextIdsSlice(@Param("clothingType") String clothingType,
                                                  @Param("genderClothing") String genderClothing,
                                               @Param("conditionClothing") String conditionClothing,
                                                  Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT c FROM ClothingPostText c " +
            "LEFT JOIN FETCH c.images " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id IN :ids")
    List<ClothingPostText> findClothingPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param genderClothing  тип Одежда и обувь
     * @param conditionClothing  Состояние Одежда и обувь
     * @return количество постов
     */
    @Query("SELECT COUNT(c) FROM ClothingPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:clothingType IS NULL OR c.clothingType = :clothingType) " +
            "AND (:genderClothing IS NULL OR c.genderClothing = :genderClothing) " +
            "AND (:conditionClothing IS NULL OR c.conditionClothing = :conditionClothing)")
    long countClothingPostText(@Param("clothingType") String clothingType,
                               @Param("genderClothing") String genderClothing,
                                  @Param("conditionClothing") String conditionClothing);


}
