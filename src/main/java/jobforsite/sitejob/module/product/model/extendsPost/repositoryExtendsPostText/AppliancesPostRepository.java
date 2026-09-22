package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AppliancesPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Бытовая техника".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see AppliancesPostText
 */

public interface AppliancesPostRepository extends JpaRepository<AppliancesPostText, Long> {

    Optional<AppliancesPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Бытовая техника.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param appliancesType  тип Бытовая техника
     * @param conditionAppliances  Состояние Бытовая техника
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT a.id FROM AppliancesPostText a " +
            "WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:region IS NULL OR a.user.region = :region) " +
            "AND (:city IS NULL OR a.user.city = :city " +
            "     OR (:city = 'other_none' AND (a.user.city IS NULL OR a.user.city = '' OR a.user.city = 'other' OR a.user.city = 'other_none'))) " +
            "AND (:appliancesType IS NULL OR a.appliancesType = :appliancesType) " +
            "AND (:conditionAppliances IS NULL OR a.conditionAppliances = :conditionAppliances)")
    Slice<Long> findAppliancesPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("appliancesType") String appliancesType,
            @Param("conditionAppliances") String conditionAppliances,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param appliancesType  тип Бытовая техника
     * @param conditionAppliances  Состояние Бытовая техника
     * @return количество постов
     */
    @Query("SELECT COUNT(a) FROM AppliancesPostText a " +
            "WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:region IS NULL OR a.user.region = :region) " +
            "AND (:city IS NULL OR a.user.city = :city " +
            "     OR (:city = 'other_none' AND (a.user.city IS NULL OR a.user.city = '' OR a.user.city = 'other' OR a.user.city = 'other_none'))) " +
            "AND (:appliancesType IS NULL OR a.appliancesType = :appliancesType) " +
            "AND (:conditionAppliances IS NULL OR a.conditionAppliances = :conditionAppliances)")
    long countAppliancesPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("appliancesType") String appliancesType,
            @Param("conditionAppliances") String conditionAppliances);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */

    @Query("SELECT a FROM AppliancesPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "ORDER BY a.moderatedAt DESC")
    Slice<AppliancesPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param appliancesType  тип Бытовая техника
     * @param conditionAppliances  Состояние Бытовая техника
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT a.id FROM AppliancesPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:appliancesType IS NULL OR a.appliancesType = :appliancesType) " +
            "AND (:conditionAppliances IS NULL OR a.conditionAppliances = :conditionAppliances) " +
            "ORDER BY a.moderatedAt DESC")
    Slice<Long> filterAppliancesPostTextIdsSlice(@Param("appliancesType") String appliancesType,
                                            @Param("conditionAppliances") String conditionAppliances,
                                            Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT a FROM AppliancesPostText a " +
            "LEFT JOIN FETCH a.images " +
            "LEFT JOIN FETCH a.user " +
            "WHERE a.id IN :ids")
    List<AppliancesPostText> findAppliancesPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param appliancesType  тип Бытовая техника
     * @param conditionAppliances  Состояние Бытовая техника
     * @return количество постов
     */
    @Query("SELECT COUNT(a) FROM AppliancesPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:appliancesType IS NULL OR a.appliancesType = :appliancesType) " +
            "AND (:conditionTools IS NULL OR a.conditionAppliances = :conditionAppliances)")
    long countAppliancesPostText(@Param("appliancesType") String appliancesType,
                            @Param("conditionAppliances") String conditionAppliances);

}
