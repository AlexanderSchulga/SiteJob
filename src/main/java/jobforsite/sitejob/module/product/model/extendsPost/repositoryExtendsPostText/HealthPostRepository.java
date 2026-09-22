package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.AnimalsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ElectronicsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.HealthPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


/**
 * Репозиторий для работы с постами категории "Здоровье и красота".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории.
 * </p>
 *
 * @see HealthPostText
 */
public interface HealthPostRepository extends JpaRepository<HealthPostText, Long> {

    Optional<HealthPostText> findByHashId(String hashId);

    // ===== ОСНОВНОЙ МЕТОД С ФИЛЬТРАЦИЕЙ =====
    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу Здоровье и красота.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя (или "other_none" для других городов)
     * @param healthType  тип Здоровье и красота
     * @param healthKind  Вид Здоровье и красота
     * @param genderHealth  Пол Здоровье и красота
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT h.id FROM HealthPostText h " +
            "WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "AND (:search IS NULL OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR h.category = :category) " +
            "AND (:region IS NULL OR h.user.region = :region) " +
            "AND (:city IS NULL OR h.user.city = :city " +
            "     OR (:city = 'other_none' AND (h.user.city IS NULL OR h.user.city = '' OR h.user.city = 'other' OR h.user.city = 'other_none'))) " +
            "AND (:healthKind IS NULL OR h.healthKind = :healthKind) " +
            "AND (:healthType IS NULL OR h.healthType = :healthType) " +
            "AND (:genderHealth IS NULL OR h.genderHealth = :genderHealth)")
    Slice<Long> findHealthPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("healthKind") String healthKind,
            @Param("healthType") String healthType,
            @Param("genderHealth") String genderHealth,

            Pageable pageable);

    // ===== ПОДСЧЕТ КОЛИЧЕСТВА =====
    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search       поисковый запрос по заголовку
     * @param category     категория поста
     * @param region       регион пользователя
     * @param city         город пользователя
     * @param healthType  тип Здоровье и красота
     * @param healthKind  Вид Здоровье и красота
     * @param genderHealth  Пол Здоровье и красота
     * @return количество постов
     */
    @Query("SELECT COUNT(h) FROM HealthPostText h " +
            "WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "AND (:search IS NULL OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR h.category = :category) " +
            "AND (:region IS NULL OR h.user.region = :region) " +
            "AND (:city IS NULL OR h.user.city = :city " +
            "     OR (:city = 'other_none' AND (h.user.city IS NULL OR h.user.city = '' OR h.user.city = 'other' OR h.user.city = 'other_none'))) " +
            "AND (:healthKind IS NULL OR h.healthKind = :healthKind) " +
            "AND (:healthType IS NULL OR h.healthType = :healthType) " +
            "AND (:genderHealth IS NULL OR h.genderHealth = :genderHealth)")
    long countHealthPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("healthKind") String healthKind,
            @Param("healthType") String healthType,
            @Param("genderHealth") String genderHealth);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT h FROM HealthPostText h WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "ORDER BY h.moderatedAt DESC")
    Slice<HealthPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по типу животных.
     *
     * @param healthType  тип Здоровье и красота
     * @param healthKind  Вид Здоровье и красота
     * @param genderHealth  Пол Электроника и гаджеты
     * @param pageable    параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT h.id FROM HealthPostText h WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "AND (:healthKind IS NULL OR h.healthKind = :healthKind) " +
            "AND (:healthType IS NULL OR h.healthType = :healthType) " +
            "AND (:genderHealth IS NULL OR h.genderHealth = :genderHealth) " +
            "ORDER BY h.moderatedAt DESC")
    Slice<Long> filterHealthPostTextIdsSlice(@Param("healthKind") String healthKind,
                                             @Param("healthType") String healthType,
                                             @Param("genderHealth") String genderHealth,
                                             Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT h FROM HealthPostText h " +
            "LEFT JOIN FETCH h.images " +
            "LEFT JOIN FETCH h.user " +
            "WHERE h.id IN :ids")
    List<HealthPostText> findHealthPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по типу.
     *
     * @param healthKind  Вид Здоровье и красота
     * @param healthType  тип Здоровье и красота
     * @param genderHealth  Пол Электроника и гаджеты
     * @return количество постов
     */
    @Query("SELECT COUNT(h) FROM HealthPostText h WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "AND (:healthKind IS NULL OR h.healthKind = :healthKind) " +
            "AND (:healthType IS NULL OR h.healthType = :healthType) " +
            "AND (:genderHealth IS NULL OR h.genderHealth = :genderHealth)")
    long countHealthPostText(@Param("healthKind") String healthKind,
                             @Param("healthType") String healthType,
                             @Param("genderHealth") String genderHealth);

}
