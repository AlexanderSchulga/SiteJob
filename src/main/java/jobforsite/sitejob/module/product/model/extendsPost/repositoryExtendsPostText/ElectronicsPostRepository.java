package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ClothingPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ElectronicsPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Электроника и гаджеты".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see ElectronicsPostText
 */
public interface ElectronicsPostRepository extends JpaRepository<ElectronicsPostText, Long> {

    Optional<ElectronicsPostText> findByHashId(String hashId);

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
     * @param electronicsType  тип Электроника и гаджеты
     * @param conditionElectronics  Состояние Электроника и гаджеты
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */

    @Query("SELECT t.id FROM ElectronicsPostText t " +
            "WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:region IS NULL OR t.user.region = :region) " +
            "AND (:city IS NULL OR t.user.city = :city " +
            "     OR (:city = 'other_none' AND (t.user.city IS NULL OR t.user.city = '' OR t.user.city = 'other' OR t.user.city = 'other_none'))) " +
            "AND (:electronicsType IS NULL OR t.electronicsType = :electronicsType) " +
            "AND (:conditionElectronics IS NULL OR t.conditionElectronics = :conditionElectronics)")
    Slice<Long> findElectronicsPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("electronicsType") String electronicsType,
            @Param("conditionElectronics") String conditionElectronics,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param electronicsType  тип Электроника и гаджеты
     * @param conditionElectronics  Состояние Электроника и гаджеты
     * @return количество постов
     */
    @Query("SELECT COUNT(t) FROM ElectronicsPostText t " +
            "WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:region IS NULL OR t.user.region = :region) " +
            "AND (:city IS NULL OR t.user.city = :city " +
            "     OR (:city = 'other_none' AND (t.user.city IS NULL OR t.user.city = '' OR t.user.city = 'other' OR t.user.city = 'other_none'))) " +
            "AND (:electronicsType IS NULL OR t.electronicsType = :electronicsType) " +
            "AND (:conditionElectronics IS NULL OR t.conditionElectronics = :conditionElectronics)")
    long countElectronicsPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("electronicsType") String electronicsType,
            @Param("conditionElectronics") String conditionElectronics);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT t FROM ElectronicsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "ORDER BY t.moderatedAt DESC")
    Slice<ElectronicsPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param electronicsType  тип Электроника и гаджеты
     * @param conditionElectronics  Состояние Электроника и гаджеты
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT t.id FROM ElectronicsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:electronicsType IS NULL OR t.electronicsType = :electronicsType) " +
            "AND (:conditionElectronics IS NULL OR t.conditionElectronics = :conditionElectronics) " +
            "ORDER BY t.moderatedAt DESC")
    Slice<Long> filterElectronicsPostTextIdsSlice(@Param("electronicsType") String electronicsType,
                                                 @Param("conditionElectronics") String conditionElectronics,
                                                 Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT t FROM ElectronicsPostText t " +
            "LEFT JOIN FETCH t.images " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.id IN :ids")
    List<ElectronicsPostText> findElectronicsPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param electronicsType  тип Электроника и гаджеты
     * @param conditionElectronics  Состояние Электроника и гаджеты
     * @return количество постов
     */
    @Query("SELECT COUNT(t) FROM ElectronicsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:electronicsType IS NULL OR t.electronicsType = :electronicsType) " +
            "AND (:conditionElectronics IS NULL OR t.conditionElectronics = :conditionElectronics)")
    long countElectronicsPostText(@Param("electronicsType") String electronicsType,
                                 @Param("conditionElectronics") String conditionElectronics);

}
