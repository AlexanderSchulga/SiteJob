package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AppliancesPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AutoPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Автозапчасти и автотовары".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see AutoPostText
 */
public interface AutoPostRepository extends JpaRepository<AutoPostText, Long> {

    Optional<AutoPostText> findByHashId(String hashId);

    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Автозапчасти и автотовары.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param autoType  тип Автозапчасти и автотовары
     * @param conditionAuto  Состояние Автозапчасти и автотовары
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    @Query("SELECT a.id FROM AutoPostText a " +
            "WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:region IS NULL OR a.user.region = :region) " +
            "AND (:city IS NULL OR a.user.city = :city " +
            "     OR (:city = 'other_none' AND (a.user.city IS NULL OR a.user.city = '' OR a.user.city = 'other' OR a.user.city = 'other_none'))) " +
            "AND (:autoType IS NULL OR a.autoType = :autoType) " +
            "AND (:conditionAuto IS NULL OR a.conditionAuto = :conditionAuto)")
    Slice<Long> findAutoPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("autoType") String autoType,
            @Param("conditionAuto") String conditionAuto,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param autoType  тип Автозапчасти и автотовары
     * @param conditionAuto  Состояние Автозапчасти и автотовары
     * @return количество постов
     */
    @Query("SELECT COUNT(a) FROM AutoPostText a " +
            "WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:region IS NULL OR a.user.region = :region) " +
            "AND (:city IS NULL OR a.user.city = :city " +
            "     OR (:city = 'other_none' AND (a.user.city IS NULL OR a.user.city = '' OR a.user.city = 'other' OR a.user.city = 'other_none'))) " +
            "AND (:autoType IS NULL OR a.autoType = :autoType) " +
            "AND (:conditionAuto IS NULL OR a.conditionAuto = :conditionAuto)")
    long countAutoPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("autoType") String autoType,
            @Param("conditionAuto") String conditionAuto);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT a FROM AutoPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "ORDER BY a.moderatedAt DESC")
    Slice<AutoPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param autoType  тип Автозапчасти и автотовары
     * @param conditionAuto  Автозапчасти и автотовары
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT a.id FROM AutoPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:autoType IS NULL OR a.autoType = :autoType) " +
            "AND (:conditionAuto IS NULL OR a.conditionAuto = :conditionAuto) " +
            "ORDER BY a.moderatedAt DESC")
    Slice<Long> filterAutoPostTextIdsSlice(@Param("autoType") String autoType,
                                      @Param("conditionAuto") String conditionAuto,
                                      Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT a FROM AutoPostText a " +
            "LEFT JOIN FETCH a.images " +
            "LEFT JOIN FETCH a.user " +
            "WHERE a.id IN :ids")
    List<AutoPostText> findAutoPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу животных.
     *
     * @param autoType  тип Автозапчасти и автотовары
     * @param conditionAuto  Автозапчасти и автотовары
     * @return количество постов
     */
    @Query("SELECT COUNT(a) FROM AutoPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:autoType IS NULL OR a.autoType = :autoType) " +
            "AND (:conditionAuto IS NULL OR a.conditionAuto = :conditionAuto)")
    long countAutoPostText(@Param("autoType") String autoType,
                           @Param("conditionAuto") String conditionAuto);

}
