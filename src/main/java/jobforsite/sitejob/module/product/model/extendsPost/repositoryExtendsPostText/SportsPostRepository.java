package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.SportsPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Товары для спорта".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории (вид спорта, тип, пол, состояние).
 * </p>
 *
 * @see SportsPostText
 */
public interface SportsPostRepository extends JpaRepository<SportsPostText, Long> {

    Optional<SportsPostText> findByHashId(String hashId);

    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, виду спорта,
     * типу, полу и состоянию.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search          поисковый запрос по заголовку
     * @param category        категория поста
     * @param region          регион пользователя
     * @param city            город пользователя (или "other_none")
     * @param sportsKind      вид спорта
     * @param sportsType      тип (взрослое/детское)
     * @param genderSports    пол
     * @param conditionSports состояние (новый/б/у)
     * @param pageable        параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT s.id FROM SportsPostText s " +
            "WHERE s.archived = false AND s.blocked = false AND s.moderation = false " +
            "AND (:search IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR s.category = :category) " +
            "AND (:region IS NULL OR s.user.region = :region) " +
            "AND (:city IS NULL OR s.user.city = :city " +
            "     OR (:city = 'other_none' AND (s.user.city IS NULL OR s.user.city = '' OR s.user.city = 'other' OR s.user.city = 'other_none'))) " +
            "AND (:sportsKind IS NULL OR s.sportsKind = :sportsKind) " +
            "AND (:sportsType IS NULL OR s.sportsType = :sportsType) " +
            "AND (:genderSports IS NULL OR s.genderSports = :genderSports) " +
            "AND (:conditionSports IS NULL OR s.conditionSports = :conditionSports)")
    Slice<Long> findSportsPostTextIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("sportsKind") String sportsKind,
            @Param("sportsType") String sportsType,
            @Param("genderSports") String genderSports,
            @Param("conditionSports") String conditionSports,
            Pageable pageable);

    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search          поисковый запрос по заголовку
     * @param category        категория поста
     * @param region          регион пользователя
     * @param city            город пользователя
     * @param sportsKind      вид спорта
     * @param sportsType      тип (взрослое/детское)
     * @param genderSports    пол
     * @param conditionSports состояние (новый/б/у)
     * @return количество постов
     */
    @Query("SELECT COUNT(s) FROM SportsPostText s " +
            "WHERE s.archived = false AND s.blocked = false AND s.moderation = false " +
            "AND (:search IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR s.category = :category) " +
            "AND (:region IS NULL OR s.user.region = :region) " +
            "AND (:city IS NULL OR s.user.city = :city " +
            "     OR (:city = 'other_none' AND (s.user.city IS NULL OR s.user.city = '' OR s.user.city = 'other' OR s.user.city = 'other_none'))) " +
            "AND (:sportsKind IS NULL OR s.sportsKind = :sportsKind) " +
            "AND (:sportsType IS NULL OR s.sportsType = :sportsType) " +
            "AND (:genderSports IS NULL OR s.genderSports = :genderSports) " +
            "AND (:conditionSports IS NULL OR s.conditionSports = :conditionSports)")
    long countSportsPostTextWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("sportsKind") String sportsKind,
            @Param("sportsType") String sportsType,
            @Param("genderSports") String genderSports,
            @Param("conditionSports") String conditionSports);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT s FROM SportsPostText s WHERE s.archived = false AND s.blocked = false AND s.moderation = false " +
            "ORDER BY s.moderatedAt DESC")
    Slice<SportsPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по специфическим полям.
     *
     * @param sportsKind      вид спорта
     * @param sportsType      тип (взрослое/детское)
     * @param genderSports    пол
     * @param conditionSports состояние (новый/б/у)
     * @param pageable        параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT s.id FROM SportsPostText s WHERE s.archived = false AND s.blocked = false AND s.moderation = false " +
            "AND (:sportsKind IS NULL OR s.sportsKind = :sportsKind) " +
            "AND (:sportsType IS NULL OR s.sportsType = :sportsType) " +
            "AND (:genderSports IS NULL OR s.genderSports = :genderSports) " +
            "AND (:conditionSports IS NULL OR s.conditionSports = :conditionSports) " +
            "ORDER BY s.moderatedAt DESC")
    Slice<Long> filterSportsPostTextIdsSlice(@Param("sportsKind") String sportsKind,
                                             @Param("sportsType") String sportsType,
                                             @Param("genderSports") String genderSports,
                                             @Param("conditionSports") String conditionSports,
                                             Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT s FROM SportsPostText s " +
            "LEFT JOIN FETCH s.images " +
            "LEFT JOIN FETCH s.user " +
            "WHERE s.id IN :ids")
    List<SportsPostText> findSportsPostTextByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по специфическим полям.
     *
     * @param sportsKind      вид спорта
     * @param sportsType      тип (взрослое/детское)
     * @param genderSports    пол
     * @param conditionSports состояние (новый/б/у)
     * @return количество постов
     */
    @Query("SELECT COUNT(s) FROM SportsPostText s WHERE s.archived = false AND s.blocked = false AND s.moderation = false " +
            "AND (:sportsKind IS NULL OR s.sportsKind = :sportsKind) " +
            "AND (:sportsType IS NULL OR s.sportsType = :sportsType) " +
            "AND (:genderSports IS NULL OR s.genderSports = :genderSports) " +
            "AND (:conditionSports IS NULL OR s.conditionSports = :conditionSports)")
    long countSportsPostText(@Param("sportsKind") String sportsKind,
                             @Param("sportsType") String sportsType,
                             @Param("genderSports") String genderSports,
                             @Param("conditionSports") String conditionSports);
}