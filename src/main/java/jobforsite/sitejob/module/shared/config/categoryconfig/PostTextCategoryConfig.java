package jobforsite.sitejob.module.shared.config.categoryconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Конфигурация категорий для товаров/объявлений.
 * <p>
 * Определяет доступные категории товаров при создании/фильтрации объявлений.
 * </p>
 *
 * <p>Доступные категории:</p>
 * <ul>
 *     <li>all → "Все товары"</li>
 *     <li>motor → "Автотранспорт"</li>
 *     <li>auto → "Автозапчасти и автотовары"</li>
 *     <li>realty → "Недвижимость"</li>
 *     <li>construction → "Строительные материалы"</li>
 *     <li>farming → "Хозяйство и дача"</li>
 *     <li>interior → "Дом и интерьер"</li>
 *     <li>tools → "Инструменты"</li>
 *     <li>appliances → "Бытовая техника"</li>
 *     <li>electronics → "Электроника и гаджеты"</li>
 *     <li>clothing → "Одежда и обувь"</li>
 *     <li>children → "Детские товары"</li>
 *     <li>sports → "Товары для спорта"</li>
 *     <li>food → "Продукты питания"</li>
 *     <li>health → "Здоровье и красота"</li>
 *     <li>animals → "Флора и фауна"</li>
 *     <li>books → "Книги и канцтовары"</li>
 *     <li>fleamarket → "Разное"</li>
 * </ul>
 *
 * @author JobForsite Team
 * @version 2.0
 * @since 2.0
 */
@Component
public class PostTextCategoryConfig extends BaseCategoryConfig {

    private static final Logger log = LoggerFactory.getLogger(PostTextCategoryConfig.class);

    // =====СТАТИЧЕСКИЕ КОНФИГИ ДЛЯ СПЕЦИФИЧНЫХ ПОЛЕЙ =====
    private static final Map<String, String> PROPERTY_TYPES = new LinkedHashMap<>();
    private static final Map<String, String> MATERIAL_UP = new LinkedHashMap<>();
    private static final Map<String, String> REPAIR_UP = new LinkedHashMap<>();
    private static final Map<String, String> MATERIAL_HOUSE = new LinkedHashMap<>();
    private static final Map<String, String> REPAIR_HOUSE = new LinkedHashMap<>();
    private static final Map<String, String> VEHICLE_TYPES = new LinkedHashMap<>();
    private static final Map<String, String> VEHICLE_ORIGINS = new LinkedHashMap<>();
    private static final Map<String, String> CONSTRUCTIONS_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> INTERIOR_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> AUTO_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_AUTO_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> TOOLS_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_TOOLS_TYPE = new LinkedHashMap<>();

    // Новые мапы для остальных категорий
    private static final Map<String, String> APPLIANCES_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_APPLIANCES_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> ELECTRONICS_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_ELECTRONICS_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> CLOTHING_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> GENDER_CLOTHING = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_CLOTHING_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> GENDER_CHILDREN = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_CHILDREN_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> SPORTS_KIND = new LinkedHashMap<>();
    private static final Map<String, String> SPORTS_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> GENDER_SPORTS = new LinkedHashMap<>();
    private static final Map<String, String> CONDITION_SPORTS_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> FOOD_KIND = new LinkedHashMap<>();
    private static final Map<String, String> HEALTH_KIND = new LinkedHashMap<>();
    private static final Map<String, String> HEALTH_TYPE = new LinkedHashMap<>();
    private static final Map<String, String> GENDER_HEALTH = new LinkedHashMap<>();
    private static final Map<String, String> ANIMALS_KIND = new LinkedHashMap<>();

    private static final Map<String, String> INTERIOR_CONDITION = new LinkedHashMap<>();

    // СТАТИЧЕСКИЙ БЛОК — выполняется при загрузке класса (ДО конструктора!)
    static {
        // ===== REALTY (Недвижимость) =====
        PROPERTY_TYPES.put("apartment", "Квартира");
        PROPERTY_TYPES.put("house", "Дом");
        PROPERTY_TYPES.put("land", "Земельный участок");
        PROPERTY_TYPES.put("garage", "Гараж");
        PROPERTY_TYPES.put("commercial", "Коммерческая");

        MATERIAL_UP.put("brick", "Кирпич");
        MATERIAL_UP.put("panel", "Панель");
        MATERIAL_UP.put("monolith", "Монолит");
        MATERIAL_UP.put("block", "Блок");

        REPAIR_UP.put("forFinishing", "Под чистовую");
        REPAIR_UP.put("withoutRepair", "Без ремонта");
        REPAIR_UP.put("cosmetic", "Косметический");
        REPAIR_UP.put("good", "Хороший");
        REPAIR_UP.put("euro", "Евро");

        MATERIAL_HOUSE.put("brick", "Кирпич");
        MATERIAL_HOUSE.put("tree", "Дерево");
        MATERIAL_HOUSE.put("monolith", "Газосиликат");
        MATERIAL_HOUSE.put("block", "Блок");

        REPAIR_HOUSE.put("forFinishing", "Под чистовую");
        REPAIR_HOUSE.put("withoutRepair", "Без ремонта");
        REPAIR_HOUSE.put("cosmetic", "Косметический");
        REPAIR_HOUSE.put("good", "Хороший");
        REPAIR_HOUSE.put("made", "Сделанный");

        // ===== MOTOR (Автотранспорт) =====
        VEHICLE_TYPES.put("cargo", "Грузовые");
        VEHICLE_TYPES.put("construction", "Строительная техника");
        VEHICLE_TYPES.put("agricultural", "Сельхоз назначения");
        VEHICLE_TYPES.put("passenger", "Легковые");
        VEHICLE_TYPES.put("bus", "Автобусы");
        VEHICLE_TYPES.put("motocycle", "Мото и квадро");

        VEHICLE_ORIGINS.put("europe", "Европа");
        VEHICLE_ORIGINS.put("america", "Америка");
        VEHICLE_ORIGINS.put("asia", "Азия");
        VEHICLE_ORIGINS.put("cis", "СНГ");

        // ===== CONSTRUCTIONS (Строительные материалы) =====
        CONSTRUCTIONS_TYPE.put("roof", "Крыша");
        CONSTRUCTIONS_TYPE.put("walls", "Стены и перекрытия");
        CONSTRUCTIONS_TYPE.put("floor", "Пол");
        CONSTRUCTIONS_TYPE.put("yard", "Двор");
        CONSTRUCTIONS_TYPE.put("road", "Дорожные");
        CONSTRUCTIONS_TYPE.put("miscellaneousCC", "Разное");

        // ===== INTERIOR (Дом и интерьер) =====
        INTERIOR_TYPE.put("furniture", "Мебель");
        INTERIOR_TYPE.put("electric", "Электрический");
        INTERIOR_TYPE.put("plumbing", "Сантехника");
        INTERIOR_TYPE.put("miscellaneousI", "Разное");

        INTERIOR_CONDITION.put("0", "Новый");
        INTERIOR_CONDITION.put("1", "Б.У");

        // ===== AUTO (Автозапчасти и автотовары) =====
        AUTO_TYPE.put("cargoA", "Для грузовых");
        AUTO_TYPE.put("constructionA", "Для строительной тех.");
        AUTO_TYPE.put("agriculturalA", "Для сельхоз назнач.");
        AUTO_TYPE.put("passengerA", "Для легковых");
        AUTO_TYPE.put("busA", "Для автобусов");
        AUTO_TYPE.put("motocycleA", "Для мото и квадро");
        AUTO_TYPE.put("miscellaneousA", "Разное");

        CONDITION_AUTO_TYPE.put("0", "Новый");
        CONDITION_AUTO_TYPE.put("1", "Б/У");

        // ===== TOOLS (Инструменты) =====
        TOOLS_TYPE.put("electricT", "Электро");
        TOOLS_TYPE.put("plumbingT", "Сантехнические");
        TOOLS_TYPE.put("measuring", "Измерительные");
        TOOLS_TYPE.put("roadT", "Дорожные");
        TOOLS_TYPE.put("miscellaneousT", "Разное");

        CONDITION_TOOLS_TYPE.put("0", "Новый");
        CONDITION_TOOLS_TYPE.put("1", "Б/У");

        // ===== APPLIANCES (Бытовая техника) =====
        APPLIANCES_TYPE.put("washingMachine", "Стиральная машина");
        APPLIANCES_TYPE.put("fridge", "Холодильник");
        APPLIANCES_TYPE.put("microwave", "Микроволнока");
        APPLIANCES_TYPE.put("dishwasher", "Посудомойка");
        APPLIANCES_TYPE.put("vacuumCleaner", "Пылесос");
        APPLIANCES_TYPE.put("stove", "Плита");
        APPLIANCES_TYPE.put("miscellaneousA", "Разное");

        CONDITION_APPLIANCES_TYPE.put("0", "Новый");
        CONDITION_APPLIANCES_TYPE.put("1", "Б/У");

        // ===== ELECTRONICS (Электроника и гаджеты) =====
        ELECTRONICS_TYPE.put("PhoneAndAccessories", "Телефон и аксессуары");
        ELECTRONICS_TYPE.put("tv", "Телевизор");
        ELECTRONICS_TYPE.put("monitor", "Монитор");
        ELECTRONICS_TYPE.put("laptop", "Ноутбук");
        ELECTRONICS_TYPE.put("computer", "Компьютер");
        ELECTRONICS_TYPE.put("componentsE", "Комплектующие ПК");
        ELECTRONICS_TYPE.put("miscellaneousE", "Разное");

        CONDITION_ELECTRONICS_TYPE.put("0", "Новый");
        CONDITION_ELECTRONICS_TYPE.put("1", "Б/У");

        // ===== CLOTHING (Одежда и обувь) =====
        CLOTHING_TYPE.put("adult", "Взрослое");
        CLOTHING_TYPE.put("childrens", "Детское");

        GENDER_CLOTHING.put("men", "Мужское");
        GENDER_CLOTHING.put("woman", "Женское");

        CONDITION_CLOTHING_TYPE.put("0", "Новый");
        CONDITION_CLOTHING_TYPE.put("1", "Б/У");

        // ===== CHILDREN (Детские товары) =====
        GENDER_CHILDREN.put("menC", "Мужское");
        GENDER_CHILDREN.put("womanC", "Женское");

        CONDITION_CHILDREN_TYPE.put("0", "Новый");
        CONDITION_CHILDREN_TYPE.put("1", "Б/У");

        // ===== SPORTS (Товары для спорта) =====
        SPORTS_KIND.put("weightlifting", "Тяжелая атл.");
        SPORTS_KIND.put("lightAthletics", "Легкая атл.");
        SPORTS_KIND.put("football", "Футбол");
        SPORTS_KIND.put("basketball", "Баскетбол");
        SPORTS_KIND.put("hockey", "Хоккей");
        SPORTS_KIND.put("tennis", "Тенис");
        SPORTS_KIND.put("tabletop", "Настольные");
        SPORTS_KIND.put("miscellaneousS", "Разное");

        SPORTS_TYPE.put("adultS", "Взрослое");
        SPORTS_TYPE.put("childrensS", "Детское");

        GENDER_SPORTS.put("menS", "Мужское");
        GENDER_SPORTS.put("womanS", "Женское");

        CONDITION_SPORTS_TYPE.put("0", "Новый");
        CONDITION_SPORTS_TYPE.put("1", "Б/У");

        // ===== FOOD (Продукты питания) =====
        FOOD_KIND.put("OwnProduction", "Собственного производства");
        FOOD_KIND.put("MassConsumption", "Массового потребления");

        // ===== HEALTH (Здоровье и красота) =====
        HEALTH_KIND.put("personalHygiene", "Личная гигиена");
        HEALTH_KIND.put("careProducts", "Средства ухода");
        HEALTH_KIND.put("cosmetics", "Косметика");
        HEALTH_KIND.put("activeAdditives", "Активные добавки");
        HEALTH_KIND.put("medicines", "Лекарственные средства");
        HEALTH_KIND.put("medicalSupplies", "Медициские пренадлежности");
        HEALTH_KIND.put("miscellaneousH", "Разное");

        HEALTH_TYPE.put("adultH", "Взрослое");
        HEALTH_TYPE.put("childrensH", "Детское");

        GENDER_HEALTH.put("menH", "Мужское");
        GENDER_HEALTH.put("womanH", "Женское");

        // ===== ANIMALS (Флора и фауна) =====
        ANIMALS_KIND.put("livestock", "Домашняя скотина");
        ANIMALS_KIND.put("pets", "Комнатные животные");
        ANIMALS_KIND.put("stern", "Корма");
        ANIMALS_KIND.put("plants", "Саженцы и растения");
        ANIMALS_KIND.put("miscellaneousAn", "Разное");
    }

    public PostTextCategoryConfig() {
        super();
    }

    /**
     * Инициализирует карту категорий для товаров.
     */
    @Override
    protected void initCategories() {
        log.debug("Инициализация конфигурации категорий для товаров");

        categories.put("all", "Все товары");
        categories.put("auto", "Автозапчасти и автотовары");
        categories.put("motor", "Автотранспорт");
        categories.put("appliances", "Бытовая техника");
        categories.put("childrenPr", "Детские товары");
        categories.put("interior", "Дом и интерьер");
        categories.put("health", "Здоровье и красота");
        categories.put("books", "Книги и канцтовары");
        categories.put("constructions", "Строительные материалы");
        categories.put("clothing", "Одежда и обувь");
        categories.put("food", "Продукты питания");
        categories.put("sports", "Товары для спорта");
        categories.put("farming", "Хозяйство и дача");
        categories.put("electronics", "Электроника и гаджеты");
        categories.put("tools", "Инструменты");
        categories.put("realty", "Недвижимость");
        categories.put("animals", "Флора и фауна");
        categories.put("fleamarket", "Разное");

        log.debug("Загружено {} категорий для товаров", categories.size());
    }

    // ===== REALTY (Недвижимость) =====
    public String getPropertyTypeDisplayName(String key) {
        return PROPERTY_TYPES.getOrDefault(key, key);
    }
    public Map<String, String> getPropertyType() {
        return new LinkedHashMap<>(PROPERTY_TYPES);
    }

    public String getMaterialUpDisplayName(String key) {
        return MATERIAL_UP.getOrDefault(key, key);
    }
    public Map<String, String> getMaterialUp() {
        return new LinkedHashMap<>(MATERIAL_UP);
    }

    public String getRepairUpDisplayName(String key) {
        return REPAIR_UP.getOrDefault(key, key);
    }
    public Map<String, String> getRepairUp() {
        return new LinkedHashMap<>(REPAIR_UP);
    }

    public String getMaterialHouseDisplayName(String key) {
        return MATERIAL_HOUSE.getOrDefault(key, key);
    }
    public Map<String, String> getMaterialHouse() {
        return new LinkedHashMap<>(MATERIAL_HOUSE);
    }

    public String getRepairHouseDisplayName(String key) {
        return REPAIR_HOUSE.getOrDefault(key, key);
    }
    public Map<String, String> getRepairHouse() {
        return new LinkedHashMap<>(REPAIR_HOUSE);
    }

    // ===== MOTOR (Автотранспорт) =====
    public String getVehicleTypeDisplayName(String key) {
        return VEHICLE_TYPES.getOrDefault(key, key);
    }
    public Map<String, String> getVehicleType() {
        return new LinkedHashMap<>(VEHICLE_TYPES);
    }

    public String getVehicleOriginDisplayName(String key) {
        return VEHICLE_ORIGINS.getOrDefault(key, key);
    }
    public Map<String, String> getVehicleOrigin() {
        return new LinkedHashMap<>(VEHICLE_ORIGINS);
    }

    // ===== CONSTRUCTIONS (Строительные материалы) =====
    public String getConstructionsDisplayName(String key) {
        return CONSTRUCTIONS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConstructionsType() {
        return new LinkedHashMap<>(CONSTRUCTIONS_TYPE);
    }

    // ===== INTERIOR (Дом и интерьер) =====
    public String getInteriorDisplayName(String key) {
        return INTERIOR_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getInteriorType() {
        return new LinkedHashMap<>(INTERIOR_TYPE);
    }
    public String getConditionInteriorDisplayName(String key) {
        return INTERIOR_CONDITION.getOrDefault(key, key);
    }

    // ===== AUTO (Автозапчасти) =====
    public String getAutoDisplayName(String key) {
        return AUTO_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getAutoType() {
        return new LinkedHashMap<>(AUTO_TYPE);
    }

    public String getConditionAutoDisplayName(String key) {
        return CONDITION_AUTO_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionAutoType() {
        return new LinkedHashMap<>(CONDITION_AUTO_TYPE);
    }

    // ===== TOOLS (Инструменты) =====
    public String getToolsDisplayName(String key) {
        return TOOLS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getToolsType() {
        return new LinkedHashMap<>(TOOLS_TYPE);
    }

    public String getConditionToolsDisplayName(String key) {
        return CONDITION_TOOLS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionToolsType() {
        return new LinkedHashMap<>(CONDITION_TOOLS_TYPE);
    }

    // ===== APPLIANCES (Бытовая техника) =====
    public String getAppliancesDisplayName(String key) {
        return APPLIANCES_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getAppliancesType() {
        return new LinkedHashMap<>(APPLIANCES_TYPE);
    }

    public String getConditionAppliancesDisplayName(String key) {
        return CONDITION_APPLIANCES_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionAppliancesType() {
        return new LinkedHashMap<>(CONDITION_APPLIANCES_TYPE);
    }

    // ===== ELECTRONICS (Электроника) =====
    public String getElectronicsDisplayName(String key) {
        return ELECTRONICS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getElectronicsType() {
        return new LinkedHashMap<>(ELECTRONICS_TYPE);
    }

    public String getConditionElectronicsDisplayName(String key) {
        return CONDITION_ELECTRONICS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionElectronicsType() {
        return new LinkedHashMap<>(CONDITION_ELECTRONICS_TYPE);
    }

    // ===== CLOTHING (Одежда) =====
    public String getClothingDisplayName(String key) {
        return CLOTHING_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getClothingType() {
        return new LinkedHashMap<>(CLOTHING_TYPE);
    }

    public String getGenderClothingDisplayName(String key) {
        return GENDER_CLOTHING.getOrDefault(key, key);
    }
    public Map<String, String> getGenderClothing() {
        return new LinkedHashMap<>(GENDER_CLOTHING);
    }

    public String getConditionClothingDisplayName(String key) {
        return CONDITION_CLOTHING_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionClothingType() {
        return new LinkedHashMap<>(CONDITION_CLOTHING_TYPE);
    }

    // ===== CHILDREN (Детские товары) =====
    public String getGenderChildrenDisplayName(String key) {
        return GENDER_CHILDREN.getOrDefault(key, key);
    }
    public Map<String, String> getGenderChildren() {
        return new LinkedHashMap<>(GENDER_CHILDREN);
    }

    public String getConditionChildrenDisplayName(String key) {
        return CONDITION_CHILDREN_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionChildrenType() {
        return new LinkedHashMap<>(CONDITION_CHILDREN_TYPE);
    }

    // ===== SPORTS (Спорт) =====
    public String getSportsKindDisplayName(String key) {
        return SPORTS_KIND.getOrDefault(key, key);
    }
    public Map<String, String> getSportsKind() {
        return new LinkedHashMap<>(SPORTS_KIND);
    }

    public String getSportsTypeDisplayName(String key) {
        return SPORTS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getSportsType() {
        return new LinkedHashMap<>(SPORTS_TYPE);
    }

    public String getGenderSportsDisplayName(String key) {
        return GENDER_SPORTS.getOrDefault(key, key);
    }
    public Map<String, String> getGenderSports() {
        return new LinkedHashMap<>(GENDER_SPORTS);
    }

    public String getConditionSportsDisplayName(String key) {
        return CONDITION_SPORTS_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getConditionSportsType() {
        return new LinkedHashMap<>(CONDITION_SPORTS_TYPE);
    }

    // ===== FOOD (Продукты) =====
    public String getFoodKindDisplayName(String key) {
        return FOOD_KIND.getOrDefault(key, key);
    }
    public Map<String, String> getFoodKind() {
        return new LinkedHashMap<>(FOOD_KIND);
    }

    // ===== HEALTH (Здоровье) =====
    public String getHealthKindDisplayName(String key) {
        return HEALTH_KIND.getOrDefault(key, key);
    }
    public Map<String, String> getHealthKind() {
        return new LinkedHashMap<>(HEALTH_KIND);
    }

    public String getHealthTypeDisplayName(String key) {
        return HEALTH_TYPE.getOrDefault(key, key);
    }
    public Map<String, String> getHealthType() {
        return new LinkedHashMap<>(HEALTH_TYPE);
    }

    public String getGenderHealthDisplayName(String key) {
        return GENDER_HEALTH.getOrDefault(key, key);
    }
    public Map<String, String> getGenderHealth() {
        return new LinkedHashMap<>(GENDER_HEALTH);
    }

    // ===== ANIMALS (Флора и фауна) =====
    public String getAnimalsKindDisplayName(String key) {
        return ANIMALS_KIND.getOrDefault(key, key);
    }
    public Map<String, String> getAnimalsKind() {
        return new LinkedHashMap<>(ANIMALS_KIND);
    }
}