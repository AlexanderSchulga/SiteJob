package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AutoPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ChildrenPrPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Детские товары".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see ChildrenPrPostText
 */

public interface ChildrenPrPostRepository extends JpaRepository<ChildrenPrPostText, Long> {

    Optional<ChildrenPrPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Детские товары.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param genderChildrenPr  тип Детские товары
     * @param conditionChildrenPr  Состояние Детские товары
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */

    @Query("SELECT c.id FROM ChildrenPrPostText c " +
            "WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:genderChildrenPr IS NULL OR c.genderChildrenPr = :genderChildrenPr) " +
            "AND (:conditionChildrenPr IS NULL OR c.conditionChildrenPr = :conditionChildrenPr)")
    Slice<Long> findChildrenPrPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("genderChildrenPr") String genderChildrenPr,
            @Param("conditionChildrenPr") String conditionChildrenPr,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param genderChildrenPr  тип Детские товары
     * @param conditionChildrenPr  Состояние Детские товары
     * @return количество постов
     */
    @Query("SELECT COUNT(c) FROM ChildrenPrPostText c " +
            "WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:genderChildrenPr IS NULL OR c.genderChildrenPr = :genderChildrenPr) " +
            "AND (:conditionChildrenPr IS NULL OR c.conditionChildrenPr = :conditionChildrenPr)")
    long countChildrenPrPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("genderChildrenPr") String genderChildrenPr,
            @Param("conditionChildrenPr") String conditionChildrenPr);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT c FROM ChildrenPrPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "ORDER BY c.moderatedAt DESC")
    Slice<ChildrenPrPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param genderChildrenPr  тип Детские товары
     * @param conditionChildrenPr  Состояние Детские товары
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */

    @Query("SELECT c.id FROM ChildrenPrPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:genderChildrenPr IS NULL OR c.genderChildrenPr = :genderChildrenPr) " +
            "AND (:conditionChildrenPr IS NULL OR c.conditionChildrenPr = :conditionChildrenPr) " +
            "ORDER BY c.moderatedAt DESC")
    Slice<Long> filterChildrenPrPostTextIdsSlice(@Param("genderChildrenPr") String genderChildrenPr,
                                               @Param("conditionChildrenPr") String conditionChildrenPr,
                                               Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT c FROM ChildrenPrPostText c " +
            "LEFT JOIN FETCH c.images " +
            "LEFT JOIN FETCH c.user " +
            "WHERE c.id IN :ids")
    List<ChildrenPrPostText> findChildrenPrPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param genderChildrenPr  тип Детские товары
     * @param conditionChildrenPr  Состояние Детские товары
     * @return количество постов
     */
    @Query("SELECT COUNT(c) FROM ChildrenPrPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:genderChildrenPr IS NULL OR c.genderChildrenPr = :genderChildrenPr) " +
            "AND (:conditionChildrenPr IS NULL OR c.conditionChildrenPr = :conditionChildrenPr)")
    long countChildrenPrPostText(@Param("genderChildrenPr") String genderChildrenPr,
                               @Param("conditionChildrenPr") String conditionChildrenPr);

}
