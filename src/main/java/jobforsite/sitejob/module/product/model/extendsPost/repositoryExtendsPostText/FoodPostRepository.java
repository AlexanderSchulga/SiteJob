package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ElectronicsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.FoodPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Продукты питания".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see FoodPostText
 */
public interface FoodPostRepository extends JpaRepository<FoodPostText, Long> {

    Optional<FoodPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Электроника и гаджеты.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param foodKind  тип Продукты питания
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT f.id FROM FoodPostText f " +
            "WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:search IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:region IS NULL OR f.user.region = :region) " +
            "AND (:city IS NULL OR f.user.city = :city " +
            "     OR (:city = 'other_none' AND (f.user.city IS NULL OR f.user.city = '' OR f.user.city = 'other' OR f.user.city = 'other_none'))) " +
            "AND (:foodKind IS NULL OR f.foodKind = :foodKind)")
    Slice<Long> findFoodPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("foodKind") String foodKind,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param foodKind  тип Продукты питания
     * @return количество постов
     */
    @Query("SELECT COUNT(f) FROM FoodPostText f " +
            "WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:search IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:region IS NULL OR f.user.region = :region) " +
            "AND (:city IS NULL OR f.user.city = :city " +
            "     OR (:city = 'other_none' AND (f.user.city IS NULL OR f.user.city = '' OR f.user.city = 'other' OR f.user.city = 'other_none'))) " +
            "AND (:foodKind IS NULL OR f.foodKind = :foodKind)")
    long countFoodPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("foodKind") String foodKind);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT f FROM FoodPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "ORDER BY f.moderatedAt DESC")
    Slice<FoodPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param foodKind  тип Продукты питания
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT f.id FROM FoodPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:foodKind IS NULL OR f.foodKind = :foodKind) " +
            "ORDER BY f.moderatedAt DESC")
    Slice<Long> filterFoodPostTextIdsSlice(@Param("foodKind") String foodKind,
                                             Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT f FROM FoodPostText f " +
            "LEFT JOIN FETCH f.images " +
            "LEFT JOIN FETCH f.user " +
            "WHERE f.id IN :ids")
    List<FoodPostText> findFoodPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param foodKind  тип Продукты питания
     * @return количество постов
     */
    @Query("SELECT COUNT(f) FROM FoodPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:foodKind IS NULL OR f.foodKind = :foodKind)")
    long countFoodPostText(@Param("foodKind") String foodKind);

}
