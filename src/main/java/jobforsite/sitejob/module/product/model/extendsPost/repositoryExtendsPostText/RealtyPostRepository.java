package jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText;

import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.MotorVehiclesPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.RealEstatePostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с постами категории "Недвижимость".
 * <p>
 * Предоставляет методы для фильтрации, пагинации и подсчета постов
 * с учетом всех специфических полей категории (тип недвижимости,
 * комнаты, этажи, лифт, год постройки, площадь и т.д.).
 * </p>
 *
 * @see RealEstatePostText
 */
public interface RealtyPostRepository extends JpaRepository<RealEstatePostText, Long> {

    Optional<RealEstatePostText> findByHashId(String hashId);


    /**
     * Находит ID постов недвижимости с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, типу недвижимости,
     * количеству комнат, этажам, наличию лифта, году постройки, площади,
     * расстоянию до города, материалу и ремонту.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param search                    поисковый запрос по заголовку
     * @param category                  категория поста
     * @param region                    регион пользователя
     * @param city                      город пользователя (или "other_none")
     * @param propertyType              тип недвижимости (apartment, house, land и т.д.)
     * @param roomsCount                количество комнат
     * @param minFloorsApartment        минимальный этаж квартиры
     * @param maxFloorsApartment        максимальный этаж квартиры
     * @param floorsCountMin            минимальное количество этажей в доме
     * @param floorsCountMax            максимальное количество этажей в доме
     * @param floorsCountHouseMin       минимальное количество этажей в доме
     * @param floorsCountHouseMax       максимальное количество этажей в доме
     * @param hasElevator               наличие лифта
     * @param minBuildYear              минимальный год постройки
     * @param maxBuildYear              максимальный год постройки
     * @param minHouseArea              минимальная площадь дома
     * @param maxHouseArea              максимальная площадь дома
     * @param minLandArea               минимальная площадь земли
     * @param maxLandArea               максимальная площадь земли
     * @param minDistanceToCity         минимальное расстояние до города
     * @param maxDistanceToCity         максимальное расстояние до города
     * @param materialUp                материал дома (для квартиры)
     * @param repairUp                  ремонт (для квартиры)
     * @param materialHouse             материал дома
     * @param repairHouse               ремонт (для дома)
     * @param minDistanceToCityHouse    минимальное расстояние до города (для дома)
     * @param maxDistanceToCityHouse    максимальное расстояние до города (для дома)
     * @param minLandAreaGarage         минимальная площадь гаража
     * @param maxLandAreaGarage         максимальная площадь гаража
     * @param minLandAreaCommercial     минимальная площадь коммерческой недвижимости
     * @param maxLandAreaCommercial     максимальная площадь коммерческой недвижимости
     * @param pageable                  параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT r.id FROM RealEstatePostText r " +
            "WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR r.category = :category) " +
            "AND (:region IS NULL OR r.user.region = :region) " +
            "AND (:city IS NULL OR r.user.city = :city " +
            "     OR (:city = 'other_none' AND (r.user.city IS NULL OR r.user.city = '' OR r.user.city = 'other' OR r.user.city = 'other_none'))) " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minFloorsApartment IS NULL OR r.floorsApartment >= :minFloorsApartment) " +
            "AND (:maxFloorsApartment IS NULL OR r.floorsApartment <= :maxFloorsApartment) " +
            "AND (:floorsCountMin IS NULL OR r.floorsCount >= :floorsCountMin) " +
            "AND (:floorsCountMax IS NULL OR r.floorsCount <= :floorsCountMax) " +
            "AND (:floorsCountHouseMin IS NULL OR r.floorsCountHouse >= :floorsCountHouseMin) " +
            "AND (:floorsCountHouseMax IS NULL OR r.floorsCountHouse <= :floorsCountHouseMax) " +
            "AND (:hasElevator IS NULL OR r.hasElevator = :hasElevator) " +
            "AND (:minBuildYear IS NULL OR r.buildYear >= :minBuildYear) " +
            "AND (:maxBuildYear IS NULL OR r.buildYear <= :maxBuildYear) " +
            "AND (:minHouseArea IS NULL OR r.houseArea >= :minHouseArea) " +
            "AND (:maxHouseArea IS NULL OR r.houseArea <= :maxHouseArea) " +
            "AND (:minLandArea IS NULL OR r.landArea >= :minLandArea) " +
            "AND (:maxLandArea IS NULL OR r.landArea <= :maxLandArea) " +
            "AND (:minDistanceToCity IS NULL OR r.distanceToCity >= :minDistanceToCity) " +
            "AND (:maxDistanceToCity IS NULL OR r.distanceToCity <= :maxDistanceToCity) " +
            "AND (:materialUp IS NULL OR r.materialUp = :materialUp) " +
            "AND (:repairUp IS NULL OR r.repairUp = :repairUp) " +
            "AND (:materialHouse IS NULL OR r.materialHouse = :materialHouse) " +
            "AND (:repairHouse IS NULL OR r.repairHouse = :repairHouse) " +
            "AND (:minDistanceToCityHouse IS NULL OR r.distanceToCityHouse >= :minDistanceToCityHouse) " +
            "AND (:maxDistanceToCityHouse IS NULL OR r.distanceToCityHouse <= :maxDistanceToCityHouse) " +
            "AND (:minLandAreaGarage IS NULL OR r.landAreaGarage >= :minLandAreaGarage) " +
            "AND (:maxLandAreaGarage IS NULL OR r.landAreaGarage <= :maxLandAreaGarage) " +
            "AND (:minLandAreaCommercial IS NULL OR r.landAreaCommercial >= :minLandAreaCommercial) " +
            "AND (:maxLandAreaCommercial IS NULL OR r.landAreaCommercial <= :maxLandAreaCommercial)")
    Slice<Long> findRealtyIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("propertyType") String propertyType,
            @Param("roomsCount") Integer roomsCount,
            @Param("minFloorsApartment") Integer minFloorsApartment,
            @Param("maxFloorsApartment") Integer maxFloorsApartment,
            @Param("floorsCountMin") Integer floorsCountMin,
            @Param("floorsCountMax") Integer floorsCountMax,
            @Param("floorsCountHouseMin") Integer floorsCountHouseMin,
            @Param("floorsCountHouseMax") Integer floorsCountHouseMax,
            @Param("hasElevator") Boolean hasElevator,
            @Param("minBuildYear") Integer minBuildYear,
            @Param("maxBuildYear") Integer maxBuildYear,
            @Param("minHouseArea") Integer minHouseArea,
            @Param("maxHouseArea") Integer maxHouseArea,
            @Param("minLandArea") Integer minLandArea,
            @Param("maxLandArea") Integer maxLandArea,
            @Param("minDistanceToCity") Integer minDistanceToCity,
            @Param("maxDistanceToCity") Integer maxDistanceToCity,
            @Param("materialUp") String materialUp,
            @Param("repairUp") String repairUp,
            @Param("materialHouse") String materialHouse,
            @Param("repairHouse") String repairHouse,
            @Param("minDistanceToCityHouse") Integer minDistanceToCityHouse,
            @Param("maxDistanceToCityHouse") Integer maxDistanceToCityHouse,
            @Param("minLandAreaGarage") Integer minLandAreaGarage,
            @Param("maxLandAreaGarage") Integer maxLandAreaGarage,
            @Param("minLandAreaCommercial") Integer minLandAreaCommercial,
            @Param("maxLandAreaCommercial") Integer maxLandAreaCommercial,
            Pageable pageable);

    /**
     * Подсчитывает количество постов недвижимости с фильтрацией.
     *
     * @param search                    поисковый запрос по заголовку
     * @param category                  категория поста
     * @param region                    регион пользователя
     * @param city                      город пользователя
     * @param propertyType              тип недвижимости
     * @param roomsCount                количество комнат
     * @param minFloorsApartment        минимальный этаж квартиры
     * @param maxFloorsApartment        максимальный этаж квартиры
     * @param floorsCountMin            минимальное количество этажей в доме
     * @param floorsCountMax            максимальное количество этажей в доме
     * @param floorsCountHouseMin       минимальное количество этажей в доме
     * @param floorsCountHouseMax       максимальное количество этажей в доме
     * @param hasElevator               наличие лифта
     * @param minBuildYear              минимальный год постройки
     * @param maxBuildYear              максимальный год постройки
     * @param minHouseArea              минимальная площадь дома
     * @param maxHouseArea              максимальная площадь дома
     * @param minLandArea               минимальная площадь земли
     * @param maxLandArea               максимальная площадь земли
     * @param minDistanceToCity         минимальное расстояние до города
     * @param maxDistanceToCity         максимальное расстояние до города
     * @param materialUp                материал дома (для квартиры)
     * @param repairUp                  ремонт (для квартиры)
     * @param materialHouse             материал дома
     * @param repairHouse               ремонт (для дома)
     * @param minDistanceToCityHouse    минимальное расстояние до города (для дома)
     * @param maxDistanceToCityHouse    максимальное расстояние до города (для дома)
     * @param minLandAreaGarage         минимальная площадь гаража
     * @param maxLandAreaGarage         максимальная площадь гаража
     * @param minLandAreaCommercial     минимальная площадь коммерческой недвижимости
     * @param maxLandAreaCommercial     максимальная площадь коммерческой недвижимости
     * @return количество постов
     */
    @Query("SELECT COUNT(r) FROM RealEstatePostText r " +
            "WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR r.category = :category) " +
            "AND (:region IS NULL OR r.user.region = :region) " +
            "AND (:city IS NULL OR r.user.city = :city " +
            "     OR (:city = 'other_none' AND (r.user.city IS NULL OR r.user.city = '' OR r.user.city = 'other' OR r.user.city = 'other_none'))) " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minFloorsApartment IS NULL OR r.floorsApartment >= :minFloorsApartment) " +
            "AND (:maxFloorsApartment IS NULL OR r.floorsApartment <= :maxFloorsApartment) " +
            "AND (:floorsCountMin IS NULL OR r.floorsCount >= :floorsCountMin) " +
            "AND (:floorsCountMax IS NULL OR r.floorsCount <= :floorsCountMax) " +
            "AND (:floorsCountHouseMin IS NULL OR r.floorsCountHouse >= :floorsCountHouseMin) " +
            "AND (:floorsCountHouseMax IS NULL OR r.floorsCountHouse <= :floorsCountHouseMax) " +
            "AND (:hasElevator IS NULL OR r.hasElevator = :hasElevator) " +
            "AND (:minBuildYear IS NULL OR r.buildYear >= :minBuildYear) " +
            "AND (:maxBuildYear IS NULL OR r.buildYear <= :maxBuildYear) " +
            "AND (:minHouseArea IS NULL OR r.houseArea >= :minHouseArea) " +
            "AND (:maxHouseArea IS NULL OR r.houseArea <= :maxHouseArea) " +
            "AND (:minLandArea IS NULL OR r.landArea >= :minLandArea) " +
            "AND (:maxLandArea IS NULL OR r.landArea <= :maxLandArea) " +
            "AND (:minDistanceToCity IS NULL OR r.distanceToCity >= :minDistanceToCity) " +
            "AND (:maxDistanceToCity IS NULL OR r.distanceToCity <= :maxDistanceToCity) " +
            "AND (:materialUp IS NULL OR r.materialUp = :materialUp) " +
            "AND (:repairUp IS NULL OR r.repairUp = :repairUp) " +
            "AND (:materialHouse IS NULL OR r.materialHouse = :materialHouse) " +
            "AND (:repairHouse IS NULL OR r.repairHouse = :repairHouse) " +
            "AND (:minDistanceToCityHouse IS NULL OR r.distanceToCityHouse >= :minDistanceToCityHouse) " +
            "AND (:maxDistanceToCityHouse IS NULL OR r.distanceToCityHouse <= :maxDistanceToCityHouse) " +
            "AND (:minLandAreaGarage IS NULL OR r.landAreaGarage >= :minLandAreaGarage) " +
            "AND (:maxLandAreaGarage IS NULL OR r.landAreaGarage <= :maxLandAreaGarage) " +
            "AND (:minLandAreaCommercial IS NULL OR r.landAreaCommercial >= :minLandAreaCommercial) " +
            "AND (:maxLandAreaCommercial IS NULL OR r.landAreaCommercial <= :maxLandAreaCommercial)")
    long countRealtyWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("propertyType") String propertyType,
            @Param("roomsCount") Integer roomsCount,
            @Param("minFloorsApartment") Integer minFloorsApartment,
            @Param("maxFloorsApartment") Integer maxFloorsApartment,
            @Param("floorsCountMin") Integer floorsCountMin,
            @Param("floorsCountMax") Integer floorsCountMax,
            @Param("floorsCountHouseMin") Integer floorsCountHouseMin,
            @Param("floorsCountHouseMax") Integer floorsCountHouseMax,
            @Param("hasElevator") Boolean hasElevator,
            @Param("minBuildYear") Integer minBuildYear,
            @Param("maxBuildYear") Integer maxBuildYear,
            @Param("minHouseArea") Integer minHouseArea,
            @Param("maxHouseArea") Integer maxHouseArea,
            @Param("minLandArea") Integer minLandArea,
            @Param("maxLandArea") Integer maxLandArea,
            @Param("minDistanceToCity") Integer minDistanceToCity,
            @Param("maxDistanceToCity") Integer maxDistanceToCity,
            @Param("materialUp") String materialUp,
            @Param("repairUp") String repairUp,
            @Param("materialHouse") String materialHouse,
            @Param("repairHouse") String repairHouse,
            @Param("minDistanceToCityHouse") Integer minDistanceToCityHouse,
            @Param("maxDistanceToCityHouse") Integer maxDistanceToCityHouse,
            @Param("minLandAreaGarage") Integer minLandAreaGarage,
            @Param("maxLandAreaGarage") Integer maxLandAreaGarage,
            @Param("minLandAreaCommercial") Integer minLandAreaCommercial,
            @Param("maxLandAreaCommercial") Integer maxLandAreaCommercial);

    /**
     * Возвращает все активные посты недвижимости с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return Slice с активными постами
     */
    @Query("SELECT r FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "ORDER BY r.moderatedAt DESC")
    Slice<RealEstatePostText> findAllActiveSlice(Pageable pageable);

    /**
     * Находит посты недвижимости с фильтрацией (возвращает сущности).
     *
     * @param propertyType тип недвижимости
     * @param roomsCount   количество комнат
     * @param minPrice     минимальная цена
     * @param maxPrice     максимальная цена
     * @param pageable     параметры пагинации
     * @return Slice с постами
     */
    @Query("SELECT r FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minPrice IS NULL OR r.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.price <= :maxPrice) " +
            "ORDER BY r.moderatedAt DESC")
    Slice<RealEstatePostText> filterRealtySlice(@Param("propertyType") String propertyType,
                                                @Param("roomsCount") Integer roomsCount,
                                                @Param("minPrice") Integer minPrice,
                                                @Param("maxPrice") Integer maxPrice,
                                                Pageable pageable);

    /**
     * Находит ID постов недвижимости с фильтрацией (оптимизированный).
     *
     * @param propertyType тип недвижимости
     * @param roomsCount   количество комнат
     * @param minPrice     минимальная цена
     * @param maxPrice     максимальная цена
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT r.id FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minPrice IS NULL OR r.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.price <= :maxPrice) " +
            "ORDER BY r.moderatedAt DESC")
    Slice<Long> filterRealtyIdsSlice(@Param("propertyType") String propertyType,
                                     @Param("roomsCount") Integer roomsCount,
                                     @Param("minPrice") Integer minPrice,
                                     @Param("maxPrice") Integer maxPrice,
                                     Pageable pageable);

    /**
     * Находит ID постов недвижимости с фильтрацией (оптимизированный).
     *
     * @param propertyType тип недвижимости
     * @param roomsCount   количество комнат
     * @param minPrice     минимальная цена
     * @param maxPrice     максимальная цена
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT r.id FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minPrice IS NULL OR r.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.price <= :maxPrice) " +
            "ORDER BY r.moderatedAt DESC")
    Slice<Long> findRealtyIdsSlice(@Param("propertyType") String propertyType,
                                   @Param("roomsCount") Integer roomsCount,
                                   @Param("minPrice") Integer minPrice,
                                   @Param("maxPrice") Integer maxPrice,
                                   Pageable pageable);

    /**
     * Детальная фильтрация для квартир.
     *
     * @param roomsCount   количество комнат
     * @param minFloors    минимальный этаж
     * @param maxFloors    максимальный этаж
     * @param hasElevator  наличие лифта
     * @param minYear      минимальный год постройки
     * @param maxYear      максимальный год постройки
     * @param pageable     параметры пагинации
     * @return Slice с ID постов
     */
    @Query("SELECT r.id FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND r.propertyType = 'apartment' " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minFloors IS NULL OR r.floorsApartment >= :minFloors) " +
            "AND (:maxFloors IS NULL OR r.floorsApartment <= :maxFloors) " +
            "AND (:hasElevator IS NULL OR r.hasElevator = :hasElevator) " +
            "AND (:minYear IS NULL OR r.buildYear >= :minYear) " +
            "AND (:maxYear IS NULL OR r.buildYear <= :maxYear) " +
            "ORDER BY r.moderatedAt DESC")
    Slice<Long> filterApartmentsIdsSlice(@Param("roomsCount") Integer roomsCount,
                                         @Param("minFloors") Integer minFloors,
                                         @Param("maxFloors") Integer maxFloors,
                                         @Param("hasElevator") Boolean hasElevator,
                                         @Param("minYear") Integer minYear,
                                         @Param("maxYear") Integer maxYear,
                                         Pageable pageable);

    /**
     * Находит посты недвижимости по ID с загрузкой изображений и пользователя.
     *
     * @param ids список ID постов
     * @return список постов с изображениями и пользователем
     */
    @Query("SELECT DISTINCT r FROM RealEstatePostText r " +
            "LEFT JOIN FETCH r.images " +
            "LEFT JOIN FETCH r.user " +
            "WHERE r.id IN :ids")
    List<RealEstatePostText> findRealtyByIdsWithImages(@Param("ids") List<Long> ids);

    /**
     * Подсчитывает количество активных постов недвижимости с фильтрацией.
     *
     * @param propertyType тип недвижимости
     * @param roomsCount   количество комнат
     * @param minPrice     минимальная цена
     * @param maxPrice     максимальная цена
     * @return количество постов
     */
    @Query("SELECT COUNT(r) FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:minPrice IS NULL OR r.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.price <= :maxPrice)")
    long countRealtyPosts(@Param("propertyType") String propertyType,
                          @Param("roomsCount") Integer roomsCount,
                          @Param("minPrice") Integer minPrice,
                          @Param("maxPrice") Integer maxPrice);
}