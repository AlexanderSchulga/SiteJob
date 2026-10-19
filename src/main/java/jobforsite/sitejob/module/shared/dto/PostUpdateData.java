package jobforsite.sitejob.module.shared.dto;

import lombok.Data;

import java.util.List;

@Data
public class PostUpdateData {
    private String title;
    private Integer price;
    private String fullText;
    private String category;
    private String location;

    // Realty
    private String propertyType;
    private Integer roomsCount;
    private Integer floorsApartment;
    private Integer floorsCount;
    private Integer floorsCountHouse;
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

    // Vehicle
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

    //-------------Инструменты
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


    // Admin flags
    private Boolean premium;
    private Integer premiumPriority;
    private Boolean donate;

    // Images
    private List<Long> deleteImageIds;
}