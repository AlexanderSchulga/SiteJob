package jobforsite.sitejob.module.product.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/** DTO для детальной информации о посте */
@Data
public class PostTextDetailDTO {

    // ===== ОБЩИЕ ПОЛЯ =====
    private Long id;
    private String hashId;
    private String title;
    private Integer price;
    private String category;
    private String categoryDisplayName;
    private String location;
    private String fullText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime moderatedAt;
    private int viewCount;
    private boolean blocked;
    private boolean moderation;
    private boolean archived;
    private boolean premium;
    private int premiumPriority;
    private boolean donate;
    private List<String> imagePaths;

    // ===== ПОЛЬЗОВАТЕЛЬ =====
    private UserDetailDTO user;

    // ===== ТИП ПОСТА =====
    private String postType;

    // ===== НЕДВИЖИМОСТЬ =====
    private String propertyType;
    private String propertyTypeDisplayName;
    private Integer roomsCount;
    private Integer floorsCount;
    private Integer floorsCountHouse;
    private Boolean hasElevator;
    private Integer buildYear;
    private Integer floorsApartment;
    private Integer houseArea;
    private Integer landArea;
    private Integer distanceToCity;

    private String materialUp;
    private String materialUpDisplayName;
    private String repairUp;
    private String repairUpDisplayName;
    private String materialHouse;
    private String materialHouseDisplayName;
    private String repairHouse;
    private String repairHouseDisplayName;
    private Integer distanceToCityHouse;
    private Integer landAreaGarage;
    private Integer landAreaCommercial;

    // ===== АВТОТРАНСПОРТ =====
    private String vehicleType;
    private String vehicleTypeDisplayName;
    private String vehicleOrigin;
    private String vehicleOriginDisplayName;
    private Integer vehicleYear;
    private Integer mileage;
    private Integer numberOfOwners;

    // ===== АВТОЗАПЧАСТИ =====
    private String autoType;
    private String autoTypeDisplayName;
    private String conditionAuto;
    private String conditionAutoDisplayName;

    // ===== СТРОИТЕЛЬНЫЕ МАТЕРИАЛЫ =====
    private String constructionsType;
    private String constructionsTypeDisplayName;

    // ===== ДОМ И ИНТЕРЬЕР =====
    private String interiorType;
    private String interiorTypeDisplayName;
    private String conditionInterior;
    private String conditionInteriorDisplayName;

    // ===== ИНСТРУМЕНТЫ =====
    private String toolsType;
    private String toolsTypeDisplayName;
    private String conditionTools;
    private String conditionToolsDisplayName;

    // ===== БЫТОВАЯ ТЕХНИКА =====
    private String appliancesType;
    private String appliancesTypeDisplayName;
    private String conditionAppliances;
    private String conditionAppliancesDisplayName;

    // ===== ЭЛЕКТРОНИКА И ГАДЖЕТЫ =====
    private String electronicsType;
    private String electronicsTypeDisplayName;
    private String conditionElectronics;
    private String conditionElectronicsDisplayName;

    // ===== ОДЕЖДА И ОБУВЬ =====
    private String clothingType;
    private String clothingTypeDisplayName;
    private String genderClothing;
    private String genderClothingDisplayName;
    private String conditionClothing;
    private String conditionClothingDisplayName;

    // ===== ДЕТСКИЕ ТОВАРЫ =====
    private String genderChildrenPr;
    private String genderChildrenPrDisplayName;
    private String conditionChildrenPr;
    private String conditionChildrenPrDisplayName;

    // ===== ТОВАРЫ ДЛЯ СПОРТА =====
    private String sportsKind;
    private String sportsKindDisplayName;
    private String sportsType;
    private String sportsTypeDisplayName;
    private String genderSports;
    private String genderSportsDisplayName;
    private String conditionSports;
    private String conditionSportsDisplayName;

    // ===== ПРОДУКТЫ ПИТАНИЯ =====
    private String foodKind;
    private String foodKindDisplayName;

    // ===== ЗДОРОВЬЕ И КРАСОТА =====
    private String healthKind;
    private String healthKindDisplayName;
    private String healthType;
    private String healthTypeDisplayName;
    private String genderHealth;
    private String genderHealthDisplayName;

    // ===== ФЛОРА И ФАУНА =====
    private String animalsKind;
    private String animalsKindDisplayName;

    @Data
    public static class UserDetailDTO {
        private Long id;
        private String hashId;
        private String nameuse;
        private String phonePost;
        private String region;
        private String regionDisplayName;
        private String city;
        private boolean online;
        private double averageRating;
        private long ratingCount;
    }
}