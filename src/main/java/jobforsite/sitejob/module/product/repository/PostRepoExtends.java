package jobforsite.sitejob.module.product.repository;

import jobforsite.sitejob.module.product.model.PostText;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepoExtends extends JpaRepository<PostText, Long> {


    /**
     * Поиск ID объявлений недвижимости со всеми фильтрами
     */
    @Query("SELECT r.id FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR r.category = :category) " +
            "AND (:region IS NULL OR r.user.region = :region) " +
            "AND (:city IS NULL OR r.user.city = :city " +
            "     OR (:city = 'other_none' AND (r.user.city IS NULL OR r.user.city = '' OR r.user.city = 'other' OR r.user.city = 'other_none'))) " +
            "AND (:propertyType IS NULL OR r.propertyType = :propertyType) " +
            "AND (:roomsCount IS NULL OR r.roomsCount = :roomsCount) " +
            "AND (:floorsApartmentMin IS NULL OR r.floorsApartment >= :floorsApartmentMin) " +
            "AND (:floorsApartmentMax IS NULL OR r.floorsApartment <= :floorsApartmentMax) " +
            "AND (:floorsCountMin IS NULL OR r.floorsCount >= :floorsCountMin) " +
            "AND (:floorsCountMax IS NULL OR r.floorsCount <= :floorsCountMax) " +
            "AND (:floorsCountHouseMin IS NULL OR r.floorsCountHouse >= :floorsCountHouseMin) " +
            "AND (:floorsCountHouseMax IS NULL OR r.floorsCountHouse <= :floorsCountHouseMax) " +
            "AND (:hasElevator IS NULL OR r.hasElevator = :hasElevator) " +
            "AND (:buildRealityYearRangeMin IS NULL OR r.buildYear >= :buildRealityYearRangeMin) " +
            "AND (:buildRealityYearRangeMax IS NULL OR r.buildYear <= :buildRealityYearRangeMax) " +
            "AND (:buildHouseYearRangeMin IS NULL OR r.buildYear >= :buildHouseYearRangeMin) " +
            "AND (:buildHouseYearRangeMax IS NULL OR r.buildYear <= :buildHouseYearRangeMax) " +
            "AND (:houseAreaMin IS NULL OR r.houseArea >= :houseAreaMin) " +
            "AND (:houseAreaMax IS NULL OR r.houseArea <= :houseAreaMax) " +
            "AND (:landAreaMin IS NULL OR r.landArea >= :landAreaMin) " +
            "AND (:landAreaMax IS NULL OR r.landArea <= :landAreaMax) " +
            "AND (:distanceToCityMin IS NULL OR r.distanceToCity >= :distanceToCityMin) " +
            "AND (:distanceToCityMax IS NULL OR r.distanceToCity <= :distanceToCityMax) " +
            "AND (:materialUp IS NULL OR r.materialUp = :materialUp) " +
            "AND (:repairUp IS NULL OR r.repairUp = :repairUp) " +
            "AND (:materialHouse IS NULL OR r.materialHouse = :materialHouse) " +
            "AND (:repairHouse IS NULL OR r.repairHouse = :repairHouse) " +
            "AND (:minDistanceToCityHouse IS NULL OR r.distanceToCityHouse >= :minDistanceToCityHouse) " +
            "AND (:maxDistanceToCityHouse IS NULL OR r.distanceToCityHouse <= :maxDistanceToCityHouse)" +
            "AND (:minLandAreaGarage IS NULL OR r.landAreaGarage >= :minLandAreaGarage) " +
            "AND (:maxLandAreaGarage IS NULL OR r.landAreaGarage <= :maxLandAreaGarage)" +
            "AND (:minLandAreaCommercial IS NULL OR r.landAreaCommercial >= :minLandAreaCommercial) " +
            "AND (:maxLandAreaCommercial IS NULL OR r.landAreaCommercial <= :maxLandAreaCommercial)" +
            "ORDER BY r.moderatedAt DESC")
    Slice<Long> findRealtyPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("propertyType") String propertyType,
            @Param("roomsCount") Integer roomsCount,
            @Param("floorsApartmentMin") Integer floorsApartmentMin,
            @Param("floorsApartmentMax") Integer floorsApartmentMax,
            @Param("floorsCountMin") Integer floorsCountMin,
            @Param("floorsCountMax") Integer floorsCountMax,
            @Param("floorsCountHouseMin") Integer floorsCountHouseMin,
            @Param("floorsCountHouseMax") Integer floorsCountHouseMax,
            @Param("hasElevator") Boolean hasElevator,
            @Param("buildRealityYearRangeMin") Integer buildRealityYearRangeMin,
            @Param("buildRealityYearRangeMax") Integer buildRealityYearRangeMax,
            @Param("buildHouseYearRangeMin") Integer buildHouseYearRangeMin,
            @Param("buildHouseYearRangeMax") Integer buildHouseYearRangeMax,
            @Param("houseAreaMin") Integer houseAreaMin,
            @Param("houseAreaMax") Integer houseAreaMax,
            @Param("landAreaMin") Integer landAreaMin,
            @Param("landAreaMax") Integer landAreaMax,
            @Param("distanceToCityMin") Integer distanceToCityMin,
            @Param("distanceToCityMax") Integer distanceToCityMax,
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
     * Поиск ID объявлений транспорта со всеми фильтрами
     */
    @Query("SELECT v.id FROM MotorVehiclesPostText v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:search IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:city IS NULL OR v.user.city = :city " +
            "     OR (:city = 'other_none' AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR v.user.city = 'other_none'))) " +
            "AND (:vehicleType IS NULL OR v.vehicleType = :vehicleType) " +
            "AND (:vehicleOrigin IS NULL OR v.vehicleOrigin = :vehicleOrigin) " +
            "AND (:vehicleYearMin IS NULL OR v.vehicleYear >= :vehicleYearMin) " +
            "AND (:vehicleYearMax IS NULL OR v.vehicleYear <= :vehicleYearMax) " +
            "AND (:minMileage IS NULL OR v.mileage >= :minMileage) " +
            "AND (:maxMileage IS NULL OR v.mileage <= :maxMileage)" +
            "AND (:minNumberOfOwners IS NULL OR v.numberOfOwners >= :minNumberOfOwners) " +
            "AND (:maxNumberOfOwners IS NULL OR v.numberOfOwners <= :maxNumberOfOwners)" +
            "ORDER BY v.moderatedAt DESC")
    Slice<Long> findVehiclePostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("vehicleType") String vehicleType,
            @Param("vehicleOrigin") String vehicleOrigin,
            @Param("vehicleYearMin") Integer vehicleYearMin,
            @Param("vehicleYearMax") Integer vehicleYearMax,
            @Param("minMileage") Integer minMileage,
            @Param("maxMileage") Integer maxMileage,
            @Param("minNumberOfOwners") Integer minNumberOfOwners,
            @Param("maxNumberOfOwners") Integer maxNumberOfOwners,
            Pageable pageable);

    /**
     * Поиск ID объявлений автозапчастей со всеми фильтрами
     */
    @Query("SELECT a.id FROM AutoPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:region IS NULL OR a.user.region = :region) " +
            "AND (:city IS NULL OR a.user.city = :city " +
            "     OR (:city = 'other_none' AND (a.user.city IS NULL OR a.user.city = '' OR a.user.city = 'other' OR a.user.city = 'other_none'))) " +
            "AND (:autoType IS NULL OR a.autoType = :autoType) " +
            "AND (:conditionAuto IS NULL OR a.conditionAuto = :conditionAuto) " +
            "ORDER BY a.moderatedAt DESC")
    Slice<Long> findAutoPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("autoType") String autoType,
            @Param("conditionAuto") String conditionAuto,
            Pageable pageable);

    /**
     * Поиск ID объявлений строительные материалы со всеми фильтрами
     */
    @Query("SELECT c.id FROM ConstructionsPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none'))) " +
            "AND (:constructionsType IS NULL OR c.constructionsType = :constructionsType) " +
            "ORDER BY c.moderatedAt DESC")
    Slice<Long> findConstructionsPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("constructionsType") String constructionsType,
            Pageable pageable);


    /**
     * Поиск ID объявлений Дом и интерьер материалы со всеми фильтрами
     */
    @Query("SELECT i.id FROM InteriorPostText i WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "AND (:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR i.category = :category) " +
            "AND (:region IS NULL OR i.user.region = :region) " +
            "AND (:city IS NULL OR i.user.city = :city " +
            "     OR (:city = 'other_none' AND (i.user.city IS NULL OR i.user.city = '' OR i.user.city = 'other' OR i.user.city = 'other_none'))) " +
            "AND (:interiorType IS NULL OR i.interiorType = :interiorType) " +
            "AND (:conditionInterior IS NULL OR i.conditionInterior = :conditionInterior) " +
            "ORDER BY i.moderatedAt DESC")
    Slice<Long> findInteriorPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("interiorType") String interiorType,
            @Param("conditionInterior") String conditionInterior,
            Pageable pageable);

    /**
     * Поиск ID объявлений инструменты материалы со всеми фильтрами
     */
    @Query("SELECT t.id FROM ToolsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:region IS NULL OR t.user.region = :region) " +
            "AND (:city IS NULL OR t.user.city = :city " +
            "     OR (:city = 'other_none' AND (t.user.city IS NULL OR t.user.city = '' OR t.user.city = 'other' OR t.user.city = 'other_none'))) " +
            "AND (:toolsType IS NULL OR t.toolsType = :toolsType) " +
            "AND (:conditionTools IS NULL OR t.conditionTools = :conditionTools) " +
            "ORDER BY t.moderatedAt DESC")
    Slice<Long> findToolsPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("toolsType") String toolsType,
            @Param("conditionTools") String conditionTools,
            Pageable pageable);

    /**
     * Поиск ID объявлений Бытовая техника со всеми фильтрами
     */
    @Query("SELECT ap.id FROM AppliancesPostText ap WHERE ap.archived = false AND ap.blocked = false AND ap.moderation = false " +
            "AND (:search IS NULL OR LOWER(ap.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR ap.category = :category) " +
            "AND (:region IS NULL OR ap.user.region = :region) " +
            "AND (:city IS NULL OR ap.user.city = :city " +
            "     OR (:city = 'other_none' AND (ap.user.city IS NULL OR ap.user.city = '' OR ap.user.city = 'other' OR ap.user.city = 'other_none'))) " +
            "AND (:appliancesType IS NULL OR ap.appliancesType = :appliancesType) " +
            "AND (:conditionAppliances IS NULL OR ap.conditionAppliances = :conditionAppliances) " +
            "ORDER BY ap.moderatedAt DESC")
    Slice<Long> findAppliancesPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("appliancesType") String appliancesType,
            @Param("conditionAppliances") String conditionAppliances,
            Pageable pageable);


    /**
     * Поиск ID объявлений Электроника и гаджеты материалы со всеми фильтрами
     */
    @Query("SELECT e.id FROM ElectronicsPostText e WHERE e.archived = false AND e.blocked = false AND e.moderation = false " +
            "AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:region IS NULL OR e.user.region = :region) " +
            "AND (:city IS NULL OR e.user.city = :city " +
            "     OR (:city = 'other_none' AND (e.user.city IS NULL OR e.user.city = '' OR e.user.city = 'other' OR e.user.city = 'other_none'))) " +
            "AND (:electronicsType IS NULL OR e.electronicsType = :electronicsType) " +
            "AND (:conditionElectronics IS NULL OR e.conditionElectronics = :conditionElectronics) " +
            "ORDER BY e.moderatedAt DESC")
    Slice<Long> findElectronicsPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("electronicsType") String electronicsType,
            @Param("conditionElectronics") String conditionElectronics,
            Pageable pageable);


    /**
     * Поиск ID объявлений Одежда и обувь со всеми фильтрами
     */
    @Query("SELECT cl.id FROM ClothingPostText cl WHERE cl.archived = false AND cl.blocked = false AND cl.moderation = false " +
            "AND (:search IS NULL OR LOWER(cl.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR cl.category = :category) " +
            "AND (:region IS NULL OR cl.user.region = :region) " +
            "AND (:city IS NULL OR cl.user.city = :city " +
            "     OR (:city = 'other_none' AND (cl.user.city IS NULL OR cl.user.city = '' OR cl.user.city = 'other' OR cl.user.city = 'other_none'))) " +
            "AND (:clothingType IS NULL OR cl.clothingType = :clothingType) " +
            "AND (:genderClothing IS NULL OR cl.genderClothing = :genderClothing) " +
            "AND (:conditionClothing IS NULL OR cl.conditionClothing = :conditionClothing) " +
            "ORDER BY cl.moderatedAt DESC")
    Slice<Long> findClothingPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("clothingType") String clothingType,
            @Param("genderClothing") String genderClothing,
            @Param("conditionClothing") String conditionClothing,
            Pageable pageable);

    /**
     * Поиск ID объявлений Детские товары со всеми фильтрами
     */
    @Query("SELECT cl.id FROM ChildrenPrPostText cl WHERE cl.archived = false AND cl.blocked = false AND cl.moderation = false " +
            "AND (:search IS NULL OR LOWER(cl.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR cl.category = :category) " +
            "AND (:region IS NULL OR cl.user.region = :region) " +
            "AND (:city IS NULL OR cl.user.city = :city " +
            "     OR (:city = 'other_none' AND (cl.user.city IS NULL OR cl.user.city = '' OR cl.user.city = 'other' OR cl.user.city = 'other_none'))) " +
            "AND (:genderChildren IS NULL OR cl.genderChildrenPr = :genderChildren) " +
            "AND (:conditionChildren IS NULL OR cl.conditionChildrenPr = :conditionChildren) " +
            "ORDER BY cl.moderatedAt DESC")
    Slice<Long> findChildrenPrPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("genderChildrenPr") String genderChildren,
            @Param("conditionChildrenPr") String conditionChildren,
            Pageable pageable);

    /**
     * Поиск ID объявлений Товары для спорта со всеми фильтрами
     */
    @Query("SELECT sp.id FROM SportsPostText sp WHERE sp.archived = false AND sp.blocked = false AND sp.moderation = false " +
            "AND (:search IS NULL OR LOWER(sp.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR sp.category = :category) " +
            "AND (:region IS NULL OR sp.user.region = :region) " +
            "AND (:city IS NULL OR sp.user.city = :city " +
            "     OR (:city = 'other_none' AND (sp.user.city IS NULL OR sp.user.city = '' OR sp.user.city = 'other' OR sp.user.city = 'other_none'))) " +
            "AND (:sportsKind IS NULL OR sp.sportsKind = :sportsKind) " +
            "AND (:sportsType IS NULL OR sp.sportsType = :sportsType) " +
            "AND (:genderSports IS NULL OR sp.genderSports = :genderSports) " +
            "AND (:conditionSports IS NULL OR sp.conditionSports = :conditionSports) " +
            "ORDER BY sp.moderatedAt DESC")
    Slice<Long> findSportsPostIdsWithAllFilters(
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
     * Поиск ID объявлений Продукты питания со всеми фильтрами
     */
    @Query("SELECT fo.id FROM FoodPostText fo WHERE fo.archived = false AND fo.blocked = false AND fo.moderation = false " +
            "AND (:search IS NULL OR LOWER(fo.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR fo.category = :category) " +
            "AND (:region IS NULL OR fo.user.region = :region) " +
            "AND (:city IS NULL OR fo.user.city = :city " +
            "     OR (:city = 'other_none' AND (fo.user.city IS NULL OR fo.user.city = '' OR fo.user.city = 'other' OR fo.user.city = 'other_none'))) " +
            "AND (:foodKind IS NULL OR fo.foodKind = :foodKind) " +
            "ORDER BY fo.moderatedAt DESC")
    Slice<Long> findFoodPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("foodKind") String foodKind,
            Pageable pageable);

    /**
     * Поиск ID объявлений Здоровье и красота со всеми фильтрами
     */
    @Query("SELECT h.id FROM HealthPostText h WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "AND (:search IS NULL OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR h.category = :category) " +
            "AND (:region IS NULL OR h.user.region = :region) " +
            "AND (:city IS NULL OR h.user.city = :city " +
            "     OR (:city = 'other_none' AND (h.user.city IS NULL OR h.user.city = '' OR h.user.city = 'other' OR h.user.city = 'other_none'))) " +
            "AND (:healthKind IS NULL OR h.healthKind = :healthKind) " +
            "AND (:healthType IS NULL OR h.healthType = :healthType) " +
            "AND (:genderHealth IS NULL OR h.genderHealth = :genderHealth) " +
            "ORDER BY h.moderatedAt DESC")
    Slice<Long> findHealthPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("healthKind") String healthKind,
            @Param("healthType") String healthType,
            @Param("genderHealth") String genderHealth,
            Pageable pageable);

    /**
     * Поиск ID объявлений Здоровье и красота со всеми фильтрами
     */
    @Query("SELECT an.id FROM AnimalsPostText an WHERE an.archived = false AND an.blocked = false AND an.moderation = false " +
            "AND (:search IS NULL OR LOWER(an.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR an.category = :category) " +
            "AND (:region IS NULL OR an.user.region = :region) " +
            "AND (:city IS NULL OR an.user.city = :city " +
            "     OR (:city = 'other_none' AND (an.user.city IS NULL OR an.user.city = '' OR an.user.city = 'other' OR an.user.city = 'other_none'))) " +
            "AND (:animalsKind IS NULL OR an.animalsKind = :animalsKind) " +
            "ORDER BY an.moderatedAt DESC")
    Slice<Long> findAnimalsPostIdsWithAllFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            @Param("animalsKind") String animalsKind,
            Pageable pageable);

// ========== DEFAULT МЕТОДЫ-ОБЕРТКИ ==========

    /**
     * Проверяет, есть ли фильтры по недвижимости
     */
    default boolean hasRealtyFilters(String propertyType, Integer roomsCount,
                                     Integer floorsApartmentMin, Integer floorsApartmentMax,
                                     Integer floorsCountMin, Integer floorsCountMax,
                                     Integer floorsCountHouseMin, Integer floorsCountHouseMax,
                                     Boolean hasElevator, Integer buildRealityYearRangeMin, Integer buildRealityYearRangeMax,
                                     Integer buildHouseYearRangeMin, Integer buildHouseYearRangeMax,
                                     Integer houseAreaMin, Integer houseAreaMax,
                                     Integer landAreaMin, Integer landAreaMax,
                                     Integer distanceToCityMin, Integer distanceToCityMax, String materialUp, String repairUp,
                                     String materialHouse, String repairHouse, Integer distanceToCityHouseMin, Integer distanceToCityHouseMax,
                                     Integer landAreaGarageMin, Integer landAreaGarageMax, Integer landAreaCommercialMin, Integer landAreaCommercialMax) {
        return propertyType != null || roomsCount != null ||
                floorsApartmentMin != null || floorsApartmentMax != null ||
                floorsCountMin != null || floorsCountMax != null ||
                floorsCountHouseMin != null || floorsCountHouseMax != null ||
                hasElevator != null || buildRealityYearRangeMin != null || buildRealityYearRangeMax != null ||
                buildHouseYearRangeMin != null || buildHouseYearRangeMax != null ||
                houseAreaMin != null || houseAreaMax != null || landAreaMin != null || landAreaMax != null ||
                distanceToCityMin != null || distanceToCityMax != null ||
                materialUp != null || repairUp != null || materialHouse != null ||
                repairHouse != null || distanceToCityHouseMin != null || distanceToCityHouseMax != null ||
                landAreaGarageMin != null || landAreaGarageMax != null ||
                landAreaCommercialMin != null || landAreaCommercialMax != null;
    }

    /**
     * Проверяет, есть ли фильтры по транспорту
     */
    default boolean hasVehicleFilters(String vehicleType, String vehicleOrigin,
                                      Integer vehicleYearMin, Integer vehicleYearMax) {
        return vehicleType != null || vehicleOrigin != null ||
                vehicleYearMin != null || vehicleYearMax != null;
    }


    /**
     * Проверяет, есть ли фильтры по автозапчастям
     */
    static boolean hasAutoFilters(String autoType, String conditionAuto) {
        return autoType != null || conditionAuto != null;
    }

    /**
     * Проверяет, есть ли фильтры по автозапчастям
     */
    static boolean hasConstructionsFilters(String constructionsType) {
        return constructionsType != null;
    }

    /**
     * Проверяет, есть ли фильтры по дом и интерьер
     */
    static boolean hasInteriorFilters(String interiorType, String conditionInterior) {
        return interiorType != null || conditionInterior != null;
    }

    /**
     * Проверяет, есть ли фильтры по автозапчастям
     */
    static boolean hasToolsFilters(String toolsType, String conditionTools) {
        return toolsType != null || conditionTools != null;
    }

    /**
     * Проверяет, есть ли фильтры по бытовой технике
     */
    static boolean hasAppliancesFilters(String appliancesType, String conditionAppliances) {
        return appliancesType != null || conditionAppliances != null;
    }

    /**
     * Проверяет, есть ли фильтры по Электроника и гаджеты
     */
    static boolean hasElectronicsFilters(String electronicsType, String conditionElectronics) {
        return electronicsType != null || conditionElectronics != null;
    }

    /**
     * Проверяет, есть ли фильтры по Одежда и обувь
     */
    static boolean hasClothingFilters(String clothingType, String genderClothing, String conditionClothing) {
        return clothingType != null || genderClothing != null ||
                conditionClothing != null;
    }

    /**
     * Проверяет, есть ли фильтры по Детские товары
     */
    static boolean hasChildrenPrFilters(String genderChildren, String conditionChildren) {
        return genderChildren != null ||
                conditionChildren != null;
    }

    /**
     * Проверяет, есть ли фильтры по Товары для спорта
     */
    static boolean hasSportsFilters(String sportsKind, String sportsType, String genderSports, String conditionSports) {
        return sportsKind != null || sportsType != null || genderSports != null ||
                conditionSports != null;
    }

    /**
     * Проверяет, есть ли фильтры по Продукты питания
     */
    static boolean hasFoodFilters(String foodKind) {
        return foodKind != null;
    }

    /**
     * Проверяет, есть ли фильтры по Продукты питания
     */
    static boolean hasHealthFilters(String healthKind, String healthType, String genderHealth) {
        return healthKind != null || healthType != null || genderHealth != null;
    }

    /**
     * Проверяет, есть ли фильтры по Продукты питания
     */
    static boolean hasAnimalsFilters(String animalsKind) {
        return animalsKind != null;
    }


    // ===== МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ВСЕХ АКТИВНЫХ ID БЕЗ ФИЛЬТРОВ =====

    @Query("SELECT r.id FROM RealEstatePostText r WHERE r.archived = false AND r.blocked = false AND r.moderation = false " +
            "AND (:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR r.category = :category) " +
            "AND (:region IS NULL OR r.user.region = :region) " +
            "AND (:city IS NULL OR r.user.city = :city " +
            "     OR (:city = 'other_none' AND (r.user.city IS NULL OR r.user.city = '' OR r.user.city = 'other' OR r.user.city = 'other_none')))")
    Slice<Long> findAllRealtyIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT v.id FROM MotorVehiclesPostText v WHERE v.archived = false AND v.blocked = false AND v.moderation = false " +
            "AND (:search IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR v.category = :category) " +
            "AND (:region IS NULL OR v.user.region = :region) " +
            "AND (:city IS NULL OR v.user.city = :city " +
            "     OR (:city = 'other_none' AND (v.user.city IS NULL OR v.user.city = '' OR v.user.city = 'other' OR v.user.city = 'other_none')))")
    Slice<Long> findAllVehicleIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT a.id FROM AutoPostText a WHERE a.archived = false AND a.blocked = false AND a.moderation = false " +
            "AND (:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "AND (:region IS NULL OR a.user.region = :region) " +
            "AND (:city IS NULL OR a.user.city = :city " +
            "     OR (:city = 'other_none' AND (a.user.city IS NULL OR a.user.city = '' OR a.user.city = 'other' OR a.user.city = 'other_none')))")
    Slice<Long> findAllAutoIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT c.id FROM ConstructionsPostText c WHERE c.archived = false AND c.blocked = false AND c.moderation = false " +
            "AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR c.category = :category) " +
            "AND (:region IS NULL OR c.user.region = :region) " +
            "AND (:city IS NULL OR c.user.city = :city " +
            "     OR (:city = 'other_none' AND (c.user.city IS NULL OR c.user.city = '' OR c.user.city = 'other' OR c.user.city = 'other_none')))")
    Slice<Long> findAllConstructionsIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT i.id FROM InteriorPostText i WHERE i.archived = false AND i.blocked = false AND i.moderation = false " +
            "AND (:search IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR i.category = :category) " +
            "AND (:region IS NULL OR i.user.region = :region) " +
            "AND (:city IS NULL OR i.user.city = :city " +
            "     OR (:city = 'other_none' AND (i.user.city IS NULL OR i.user.city = '' OR i.user.city = 'other' OR i.user.city = 'other_none')))")
    Slice<Long> findAllInteriorIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT t.id FROM ToolsPostText t WHERE t.archived = false AND t.blocked = false AND t.moderation = false " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:region IS NULL OR t.user.region = :region) " +
            "AND (:city IS NULL OR t.user.city = :city " +
            "     OR (:city = 'other_none' AND (t.user.city IS NULL OR t.user.city = '' OR t.user.city = 'other' OR t.user.city = 'other_none')))")
    Slice<Long> findAllToolsIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT ap.id FROM AppliancesPostText ap WHERE ap.archived = false AND ap.blocked = false AND ap.moderation = false " +
            "AND (:search IS NULL OR LOWER(ap.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR ap.category = :category) " +
            "AND (:region IS NULL OR ap.user.region = :region) " +
            "AND (:city IS NULL OR ap.user.city = :city " +
            "     OR (:city = 'other_none' AND (ap.user.city IS NULL OR ap.user.city = '' OR ap.user.city = 'other' OR ap.user.city = 'other_none')))")
    Slice<Long> findAllAppliancesIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT e.id FROM ElectronicsPostText e WHERE e.archived = false AND e.blocked = false AND e.moderation = false " +
            "AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:region IS NULL OR e.user.region = :region) " +
            "AND (:city IS NULL OR e.user.city = :city " +
            "     OR (:city = 'other_none' AND (e.user.city IS NULL OR e.user.city = '' OR e.user.city = 'other' OR e.user.city = 'other_none')))")
    Slice<Long> findAllElectronicsIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT cl.id FROM ClothingPostText cl WHERE cl.archived = false AND cl.blocked = false AND cl.moderation = false " +
            "AND (:search IS NULL OR LOWER(cl.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR cl.category = :category) " +
            "AND (:region IS NULL OR cl.user.region = :region) " +
            "AND (:city IS NULL OR cl.user.city = :city " +
            "     OR (:city = 'other_none' AND (cl.user.city IS NULL OR cl.user.city = '' OR cl.user.city = 'other' OR cl.user.city = 'other_none')))")
    Slice<Long> findAllClothingIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT ch.id FROM ChildrenPrPostText ch WHERE ch.archived = false AND ch.blocked = false AND ch.moderation = false " +
            "AND (:search IS NULL OR LOWER(ch.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR ch.category = :category) " +
            "AND (:region IS NULL OR ch.user.region = :region) " +
            "AND (:city IS NULL OR ch.user.city = :city " +
            "     OR (:city = 'other_none' AND (ch.user.city IS NULL OR ch.user.city = '' OR ch.user.city = 'other' OR ch.user.city = 'other_none')))")
    Slice<Long> findAllChildrenPrIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT sp.id FROM SportsPostText sp WHERE sp.archived = false AND sp.blocked = false AND sp.moderation = false " +
            "AND (:search IS NULL OR LOWER(sp.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR sp.category = :category) " +
            "AND (:region IS NULL OR sp.user.region = :region) " +
            "AND (:city IS NULL OR sp.user.city = :city " +
            "     OR (:city = 'other_none' AND (sp.user.city IS NULL OR sp.user.city = '' OR sp.user.city = 'other' OR sp.user.city = 'other_none')))")
    Slice<Long> findAllSportsIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT f.id FROM FoodPostText f WHERE f.archived = false AND f.blocked = false AND f.moderation = false " +
            "AND (:search IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR f.category = :category) " +
            "AND (:region IS NULL OR f.user.region = :region) " +
            "AND (:city IS NULL OR f.user.city = :city " +
            "     OR (:city = 'other_none' AND (f.user.city IS NULL OR f.user.city = '' OR f.user.city = 'other' OR f.user.city = 'other_none')))")
    Slice<Long> findAllFoodIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT h.id FROM HealthPostText h WHERE h.archived = false AND h.blocked = false AND h.moderation = false " +
            "AND (:search IS NULL OR LOWER(h.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR h.category = :category) " +
            "AND (:region IS NULL OR h.user.region = :region) " +
            "AND (:city IS NULL OR h.user.city = :city " +
            "     OR (:city = 'other_none' AND (h.user.city IS NULL OR h.user.city = '' OR h.user.city = 'other' OR h.user.city = 'other_none')))")
    Slice<Long> findAllHealthIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    @Query("SELECT an.id FROM AnimalsPostText an WHERE an.archived = false AND an.blocked = false AND an.moderation = false " +
            "AND (:search IS NULL OR LOWER(an.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR an.category = :category) " +
            "AND (:region IS NULL OR an.user.region = :region) " +
            "AND (:city IS NULL OR an.user.city = :city " +
            "     OR (:city = 'other_none' AND (an.user.city IS NULL OR an.user.city = '' OR an.user.city = 'other' OR an.user.city = 'other_none')))")
    Slice<Long> findAllAnimalsIds(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);
}
