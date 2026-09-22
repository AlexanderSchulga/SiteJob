package jobforsite.sitejob.module.product.dto;

import lombok.Data;

/**DTO для фильтров категорий товаров*/
@Data
public class ProductFilterDTO {

    // ===== Базовые фильтры =====
    private String category;
    private String search;
    private String region;
    private String city;
    private String sort;
    private String priceSort;

    private Integer minPrice;
    private Integer maxPrice;

    private Integer price;

    // ===== Пагинация =====
    private int page;
    private int size;

    // ===== Фильтры для недвижимости =====

    private Integer minFloorsApartment;
    private Integer maxFloorsApartment;
    private Integer floorsCountMin;
    private Integer floorsCountMax;
    private Integer floorsCountHouseMin;
    private Integer floorsCountHouseMax;

    private Integer minBuildYear;
    private Integer maxBuildYear;
    private Integer minHouseArea;
    private Integer maxHouseArea;
    private Integer minLandArea;
    private Integer maxLandArea;
    private Integer minDistanceToCity;
    private Integer maxDistanceToCity;

    private Integer minDistanceToCityHouse;
    private Integer maxDistanceToCityHouse;
    private Integer minLandAreaGarage;
    private Integer maxLandAreaGarage;
    private Integer minLandAreaCommercial;
    private Integer maxLandAreaCommercial;
    private String propertyType;      // apartment, house, land

    // Для квартир
    private Integer roomsCount;       // 0-студия, 1,2,3,4-более
    private Integer floorsApartment;  // этаж квартиры
    private Integer apartmentBuildYear; // год постройки для квартиры
    private String materialUp;
    private String repairUp;

    // Для квартир и домов
    private Integer floorsCount;
    private Boolean hasElevator;      // лифт (да/нет)

    // Для домов
    private Integer floorsCountHouse;
    private Integer houseBuildYear;   // год постройки для дома
    private Integer houseArea;        // площадь дома (число)
    private Integer landArea;         // площадь земли (число)
    private String materialHouse;
    private String repairHouse;
    private Integer distanceToCityHouse;

    // Для участков
    private Integer distanceToCity;   // расстояние до города (0-7)

    private Integer landAreaGarage;

    private Integer landAreaCommercial;

    // ===== Фильтры для автотранспорта =====
    private String vehicleType;       // cargo, construction, passenger, bus
    private String vehicleOrigin;     // europe, america, asia, cis
    private Integer vehicleYear;      // год выпуска (число)
    private Integer mileage;
    private Integer numberOfOwners;

    private Integer minYear;
    private Integer maxYear;

    private Integer minMileage;

    private Integer maxMileage;

    private Integer minNumberOfOwners;

    private Integer maxNumberOfOwners;

    //--автозапчасти----//

    private String autoType;
    private String conditionAuto;

    //----Строительные материалы-//
    private String constructionsType;

    //--------Дом и интерьер----------//
    private String interiorType;
    private String conditionInterior;

    //------------Инструменты--------------------//
    private String toolsType;
    private String conditionTools;

    //------Бытовая техника-----//
    private String appliancesType;
    private String conditionAppliances;

    //---------Электроника и гаджеты-------------//
    private String electronicsType;
    private String conditionElectronics;

    //----------Одежда и обувь--------------//
    private String clothingType;
    private String genderClothing;
    private String conditionClothing;

    //---------------Детские товары-----------------------//
    private String genderChildrenPr;
    private String conditionChildrenPr;

    //----------------Товары для спорта-------------------------------//
    private String sportsKind;
    private String sportsType;
    private String genderSports;
    private String conditionSports;

    //------------Продукты питания-----------------------------//
    private String foodKind;

    //--------------Здоровье и красота--------------------------//
    private String healthKind;
    private String healthType;
    private String genderHealth;

    //-----Флора и фауна-------//
    private String animalsKind;

}