package jobforsite.sitejob.module.product.dto;

import lombok.Data;

@Data
public class PostCreationData {
    private String title;
    private Integer price;
    private String fullText;
    private String category;
    private String location;
    private boolean moderation = true;

    // ===== Поля для недвижимости =====
    private String propertyType;
    private Integer roomsCount;
    private Integer floorsCount;

    private Integer floorsCountHouse;
    private Integer floorsApartment;
    private Boolean hasElevator;
    private Integer buildYear;
    private Integer houseArea;
    private Integer landArea;
    private Integer distanceToCity;

    private String materialUp;
    private String repairUp;
    private String materialHouse;
    private String repairHouse;
    private Integer distanceToCityHouse;
    private Integer landAreaGarage;
    private Integer landAreaCommercial;

    // ===== Поля для автотранспорта =====
    private String vehicleType;
    private String vehicleOrigin;
    private Integer vehicleYear;
    private Integer mileage;
    private Integer numberOfOwners;

    // ===== Поля для автозапчастей =====
    private String autoType;
    private String conditionAuto;

    //-----Строительные материалы--------//
    private String constructionsType;

    //-----Дом и интерьер
    private String interiorType;
    private String conditionInterior;

    //---------Инструменты
    private String toolsType;
    private String conditionTools;

    //-------Бытовая техника--
    private String appliancesType;
    private String conditionAppliances;

    //-----Электроника и гаджеты-----
    private String electronicsType;
    private String conditionElectronics;

    //--------Одежда и обувь
    private String clothingType;
    private String genderClothing;
    private String conditionClothing;

    //----------Детские товары
    private String genderChildrenPr;
    private String conditionChildrenPr;

    //-------Товары для спорта
    private String sportsKind;
    private String sportsType;
    private String genderSports;
    private String conditionSports;

    //---------Продукты питания------
    private String foodKind;

    //------Здоровье и красота---------
    private String healthKind;
    private String healthType;
    private String genderHealth;

    //--Флора и фауна--
    private String animalsKind;
}