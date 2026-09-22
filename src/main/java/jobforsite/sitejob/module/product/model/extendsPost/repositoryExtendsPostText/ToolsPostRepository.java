package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ToolsPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Инструменты".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории (тип, состояние).
 * </p>
 *
 * @see ToolsPostText
 */
public interface ToolsPostRepository extends JpaRepository<ToolsPostText, Long> {

    Optional<ToolsPostText> findByHashId(String hashId);

    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, типу и состоянию инструментов.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search         поисковый запрос по заголовку
     * @param category       категория поста
     * @param region         регион пользователя
     * @param city           город пользователя (или "other_none")
     * @param toolsType      тип инструментов
     * @param conditionTools состояние (новый/б/у)
     * @param pageable       параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT t.id FROM ToolsPostText t " +
            "WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:region IS NULL OR t.user.region = :region) " +
            "AND (:city IS NULL OR t.user.city = :city " +
            "     OR (:city = 'other_none' AND (t.user.city IS NULL OR t.user.city = '' OR t.user.city = 'other' OR t.user.city = 'other_none'))) " +
            "AND (:toolsType IS NULL OR t.toolsType = :toolsType) " +
            "AND (:conditionTools IS NULL OR t.conditionTools = :conditionTools)")
    Slice<Long> findToolsPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("toolsType") String toolsType,
            @Param("conditionTools") String conditionTools,
            Pageable pageable);

    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search         поисковый запрос по заголовку
     * @param category       категория поста
     * @param region         регион пользователя
     * @param city           город пользователя
     * @param toolsType      тип инструментов
     * @param conditionTools состояние (новый/б/у)
     * @return количество постов
     */
    @Query("SELECT COUNT(t) FROM ToolsPostText t " +
            "WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:region IS NULL OR t.user.region = :region) " +
            "AND (:city IS NULL OR t.user.city = :city " +
            "     OR (:city = 'other_none' AND (t.user.city IS NULL OR t.user.city = '' OR t.user.city = 'other' OR t.user.city = 'other_none'))) " +
            "AND (:toolsType IS NULL OR t.toolsType = :toolsType) " +
            "AND (:conditionTools IS NULL OR t.conditionTools = :conditionTools)")
    long countToolsPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("toolsType") String toolsType,
            @Param("conditionTools") String conditionTools);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT t FROM ToolsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "ORDER BY t.moderatedAt DESC")
    Slice<ToolsPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу и состоянию.
     *
     * @param toolsType      тип инструментов
     * @param conditionTools состояние (новый/б/у)
     * @param pageable       параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT t.id FROM ToolsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:toolsType IS NULL OR t.toolsType = :toolsType) " +
            "AND (:conditionTools IS NULL OR t.conditionTools = :conditionTools) " +
            "ORDER BY t.moderatedAt DESC")
    Slice<Long> filterToolsPostTextIdsSlice(@Param("toolsType") String toolsType,
                                            @Param("conditionTools") String conditionTools,
                                            Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT t FROM ToolsPostText t " +
            "LEFT JOIN FETCH t.images " +
            "LEFT JOIN FETCH t.user " +
            "WHERE t.id IN :ids")
    List<ToolsPostText> findToolsPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу и состоянию.
     *
     * @param toolsType      тип инструментов
     * @param conditionTools состояние (новый/б/у)
     * @return количество постов
     */
    @Query("SELECT COUNT(t) FROM ToolsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:toolsType IS NULL OR t.toolsType = :toolsType) " +
            "AND (:conditionTools IS NULL OR t.conditionTools = :conditionTools)")
    long countToolsPostText(@Param("toolsType") String toolsType,
                            @Param("conditionTools") String conditionTools);
}