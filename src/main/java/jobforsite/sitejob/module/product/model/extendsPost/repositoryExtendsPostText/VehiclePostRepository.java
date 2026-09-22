package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.ConstructionsPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.MotorVehiclesPostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Автотранспорт".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории (тип транспорта, регион бренда,
 * год выпуска, пробег, количество владельцев).
 * </p>
 *
 * @see MotorVehiclesPostText
 */
public interface VehiclePostRepository extends JpaRepository<MotorVehiclesPostText, Long> {


    Optional<MotorVehiclesPostText> findByHashId(String hashId);


    /**
     * Находит ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, типу транспорта,
     * региону бренда, году выпуска, пробегу и количеству владельцев.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search              поисковый запрос по заголовку
     * @param category            категория поста
     * @param region              регион пользователя
     * @param city                город пользователя (или "other_none")
     * @param vehicleType         тип транспорта
     * @param vehicleOrigin       регион бренда
     * @param minYear             минимальный год выпуска
     * @param maxYear             максимальный год выпуска
     * @param minMileage          минимальный пробег
     * @param maxMileage          максимальный пробег
     * @param minNumberOfOwners   минимальное количество владельцев
     * @param maxNumberOfOwners   максимальное количество владельцев
     * @param pageable            параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT v.id FROM MotorVehiclesPostText v " +
            "WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:search IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:city IS NULL OR v.user.city = :city " +
            "     OR (:city = 'other_none' AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR v.user.city = 'other_none'))) " +
            "AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType) " +
            "AND (:vehicleOrigin IS NULL OR v.vehicleOrigin = :vehicleOrigin) " +
            "AND (:minYear IS NULL OR v.vehicleYear >= :minYear) " +
            "AND (:maxYear IS NULL OR v.vehicleYear <= :maxYear) " +
            "AND (:minMileage IS NULL OR v.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR v.mileage <= :maxMileage) " +
            "AND (:minNumberOfOwners IS NULL OR v.numberOfOwners >= :minNumberOfOwners) " +
            "AND (:maxNumberOfOwners IS NULL OR v.numberOfOwners <= :maxNumberOfOwners)")
    Slice<Long> findVehicleIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("vehicleType") String vehicleType,
            @Param("vehicleOrigin") String vehicleOrigin,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear,
            @Param("minMileage") Integer minMileage,
            @Param("maxMileage") Integer maxMileage,
            @Param("minNumberOfOwners") Integer minNumberOfOwners,
            @Param("maxNumberOfOwners") Integer maxNumberOfOwners,
            Pageable pageable);

    /**
     * Подсчитывает количество постов с фильтрацией.
     *
     * @param search              поисковый запрос по заголовку
     * @param category            категория поста
     * @param region              регион пользователя
     * @param city                город пользователя
     * @param vehicleType         тип транспорта
     * @param vehicleOrigin       регион бренда
     * @param minYear             минимальный год выпуска
     * @param maxYear             максимальный год выпуска
     * @param minMileage          минимальный пробег
     * @param maxMileage          максимальный пробег
     * @param minNumberOfOwners   минимальное количество владельцев
     * @param maxNumberOfOwners   максимальное количество владельцев
     * @return количество постов
     */
    @Query("SELECT COUNT(v) FROM MotorVehiclesPostText v " +
            "WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:search IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:city IS NULL OR v.user.city = :city " +
            "     OR (:city = 'other_none' AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR v.user.city = 'other_none'))) " +
            "AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType) " +
            "AND (:vehicleOrigin IS NULL OR v.vehicleOrigin = :vehicleOrigin) " +
            "AND (:minYear IS NULL OR v.vehicleYear >= :minYear) " +
            "AND (:maxYear IS NULL OR v.vehicleYear <= :maxYear) " +
            "AND (:minMileage IS NULL OR v.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR v.mileage <= :maxMileage) " +
            "AND (:minNumberOfOwners IS NULL OR v.numberOfOwners >= :minNumberOfOwners) " +
            "AND (:maxNumberOfOwners IS NULL OR v.numberOfOwners <= :maxNumberOfOwners)")
    long countVehicleWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("vehicleType") String vehicleType,
            @Param("vehicleOrigin") String vehicleOrigin,
            @Param("minYear") Integer minYear,
            @Param("maxYear") Integer maxYear,
            @Param("minMileage") Integer minMileage,
            @Param("maxMileage") Integer maxMileage,
            @Param("minNumberOfOwners") Integer minNumberOfOwners,
            @Param("maxNumberOfOwners") Integer maxNumberOfOwners);

    /**
     * Возвращает все активные посты с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT v FROM MotorVehiclesPostText v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "ORDER BY v.moderatedAt DESC")
    Slice<MotorVehiclesPostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит ID постов с фильтрацией по специфическим полям.
     *
     * @param vehicleType         тип транспорта
     * @param vehicleOrigin       регион бренда
     * @param minYear             минимальный год выпуска
     * @param maxYear             максимальный год выпуска
     * @param minMileage          минимальный пробег
     * @param maxMileage          максимальный пробег
     * @param minNumberOfOwners   минимальное количество владельцев
     * @param maxNumberOfOwners   максимальное количество владельцев
     * @param pageable            параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT v.id FROM MotorVehiclesPostText v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType) " +
            "AND (:vehicleOrigin IS NULL OR v.vehicleOrigin = :vehicleOrigin) " +
            "AND (:minYear IS NULL OR v.vehicleYear >= :minYear) " +
            "AND (:maxYear IS NULL OR v.vehicleYear <= :maxYear) " +
            "AND (:minMileage IS NULL OR v.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR v.mileage <= :maxMileage) " +
            "AND (:minNumberOfOwners IS NULL OR v.numberOfOwners >= :minNumberOfOwners) " +
            "AND (:maxNumberOfOwners IS NULL OR v.numberOfOwners <= :maxNumberOfOwners) " +
            "ORDER BY v.moderatedAt DESC")
    Slice<Long> filterVehicleIdsSlice(@Param("vehicleType") String vehicleType,
                                      @Param("vehicleOrigin") String vehicleOrigin,
                                      @Param("minYear") Integer minYear,
                                      @Param("maxYear") Integer maxYear,
                                      @Param("minMileage") Integer minMileage,
                                      @Param("maxMileage") Integer maxMileage,
                                      @Param("minNumberOfOwners") Integer minNumberOfOwners,
                                      @Param("maxNumberOfOwners") Integer maxNumberOfOwners,
                                      Pageable pageable);

    /**
     * Находит посты по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT v FROM MotorVehiclesPostText v " +
            "LEFT JOIN FETCH v.images " +
            "LEFT JOIN FETCH v.user " +
            "WHERE v.id IN :ids")
    List<MotorVehiclesPostText> findVehiclesByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов с фильтрацией по специфическим полям.
     *
     * @param vehicleType         тип транспорта
     * @param vehicleOrigin       регион бренда
     * @param minYear             минимальный год выпуска
     * @param maxYear             максимальный год выпуска
     * @param minMileage          минимальный пробег
     * @param maxMileage          максимальный пробег
     * @param minNumberOfOwners   минимальное количество владельцев
     * @param maxNumberOfOwners   максимальное количество владельцев
     * @return количество постов
     */
    @Query("SELECT COUNT(v) FROM MotorVehiclesPostText v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType) " +
            "AND (:vehicleOrigin IS NULL OR v.vehicleOrigin = :vehicleOrigin) " +
            "AND (:minYear IS NULL OR v.vehicleYear >= :minYear) " +
            "AND (:maxYear IS NULL OR v.vehicleYear <= :maxYear) " +
            "AND (:minMileage IS NULL OR v.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR v.mileage <= :maxMileage) " +
            "AND (:minNumberOfOwners IS NULL OR v.numberOfOwners >= :minNumberOfOwners) " +
            "AND (:maxNumberOfOwners IS NULL OR v.numberOfOwners <= :maxNumberOfOwners)")
    long countVehiclePosts(@Param("vehicleType") String vehicleType,
                           @Param("vehicleOrigin") String vehicleOrigin,
                           @Param("minYear") Integer minYear,
                           @Param("maxYear") Integer maxYear,
                           @Param("minMileage") Integer minMileage,
                           @Param("maxMileage") Integer maxMileage,
                           @Param("minNumberOfOwners") Integer minNumberOfOwners,
                           @Param("maxNumberOfOwners") Integer maxNumberOfOwners);
}