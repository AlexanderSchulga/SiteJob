package jobforsite.sitejob.module.product.service;

import jobforsite.sitejob.module.product.dto.PostCreationData;
import jobforsite.sitejob.module.product.model.*;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Фабрика для создания постов различных категорий продуктов.
 * <p>
 * Реализует паттерн Фабрика для централизованного создания объектов
 * {@link PostText} различных типов в зависимости от категории продукта.
 * </p>
 * <p>
 * Основные задачи:
 * <ul>
 *   <li>Создание конкретных типов постов (недвижимость, авто, одежда и т.д.)</li>
 *   <li>Заполнение общих полей для всех постов</li>
 *   <li>Установка специфичных полей для каждой категории</li>
 *   <li>Автоматическая установка даты создания и статуса модерации</li>
 * </ul>
 * </p>
 * @see PostText
 * @see PostCreationData
 * @see User
 */
@Component
public class PostFactory {

    /**
     * Создает пост указанной категории.
     * <p>
     * На основе переданной категории выбирает соответствующий метод создания
     * и возвращает экземпляр конкретного типа {@link PostText}.
     * Поддерживаются все категории продуктов, определенные в системе.
     * </p>
     *
     * @param category категория поста (например, "realty", "auto", "clothing")
     * @param user     пользователь, создающий пост
     * @param data     данные для заполнения полей поста
     * @return созданный пост соответствующего типа
     * @throws IllegalArgumentException если категория не поддерживается
     * @see #createDefaultPost(User, PostCreationData)
     */
    public PostText createPost(String category, User user, PostCreationData data) {
        return switch (category) {
            case "realty" -> createRealtyPost(user, data);
            case "motor" -> createVehiclePost(user, data);
            case "constructions" -> createConstructionsPost(user, data);
            case "interior" -> createInteriorPost(user, data);
            case "tools" -> createToolsPost(user, data);
            case "appliances" -> createAppliancesPost(user, data);
            case "electronics" -> createElectronicsPost(user, data);
            case "auto" -> createAutoPost(user, data);
            case "clothing" -> createClothingPost(user, data);
            case "childrenPr" -> createChildrenPrPost(user, data);
            case "sports" -> createSportsPost(user, data);
            case "food" -> createFoodPost(user, data);
            case "health" -> createHealthPost(user, data);
            case "animals" -> createAnimalsPost(user, data);
            default -> createDefaultPost(user, data);
        };
    }

    /**
     * Создает дефолтный пост (базовый тип).
     * <p>
     * Используется для категорий, у которых нет специализированной реализации.
     * Заполняет только общие поля, без специфичных для категории.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return базовый объект {@link PostText}
     */
    private PostText createDefaultPost(User user, PostCreationData data) {
        PostText post = new PostText();
        fillCommonFields(post, user, data);
        return post;
    }

    /**
     * Создает пост категории "Недвижимость".
     * <p>
     * Заполняет общие поля и специфичные для недвижимости:
     * тип недвижимости, количество комнат, этажность, наличие лифта,
     * год постройки, площади, материалы и ремонт.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link RealEstatePostText}
     */
    private RealEstatePostText createRealtyPost(User user, PostCreationData data) {
        RealEstatePostText post = new RealEstatePostText();
        fillCommonFields(post, user, data);

        // Realty-specific
        post.setPropertyType(data.getPropertyType());
        post.setRoomsCount(data.getRoomsCount());
        post.setFloorsCount(data.getFloorsCount());
        post.setFloorsCountHouse(data.getFloorsCountHouse());
        post.setFloorsApartment(data.getFloorsApartment());
        post.setHasElevator(data.getHasElevator());
        post.setBuildYear(data.getBuildYear());
        post.setHouseArea(data.getHouseArea());
        post.setLandArea(data.getLandArea());
        post.setDistanceToCity(data.getDistanceToCity());
        post.setMaterialUp(data.getMaterialUp());
        post.setRepairUp(data.getRepairUp());
        post.setMaterialHouse(data.getMaterialHouse());
        post.setRepairHouse(data.getRepairHouse());
        post.setDistanceToCityHouse(data.getDistanceToCityHouse());
        post.setLandAreaGarage(data.getLandAreaGarage());
        post.setLandAreaCommercial(data.getLandAreaCommercial());

        return post;
    }

    /**
     * Создает пост категории "Автотранспорт".
     * <p>
     * Заполняет общие поля и специфичные для транспорта:
     * тип транспорта, происхождение, год выпуска, пробег, количество владельцев.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link MotorVehiclesPostText}
     */
    private MotorVehiclesPostText createVehiclePost(User user, PostCreationData data) {
        MotorVehiclesPostText post = new MotorVehiclesPostText();
        fillCommonFields(post, user, data);

        // Vehicle-specific
        post.setVehicleType(data.getVehicleType());
        post.setVehicleOrigin(data.getVehicleOrigin());
        post.setVehicleYear(data.getVehicleYear());
        post.setMileage(data.getMileage());
        post.setNumberOfOwners(data.getNumberOfOwners());

        return post;
    }

    /**
     * Создает пост категории "Строительные материалы".
     * <p>
     * Заполняет общие поля и специфичные для строительных материалов:
     * тип строительного материала.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link ConstructionsPostText}
     */
    private ConstructionsPostText createConstructionsPost(User user, PostCreationData data) {
        ConstructionsPostText post = new ConstructionsPostText();
        fillCommonFields(post, user, data);

        // Constructions-specific
        post.setConstructionsType(data.getConstructionsType());

        return post;
    }

    /**
     * Создает пост категории "Дом и интерьер".
     * <p>
     * Заполняет общие поля и специфичные для интерьера:
     * тип интерьера и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link InteriorPostText}
     */
    private InteriorPostText createInteriorPost(User user, PostCreationData data) {
        InteriorPostText post = new InteriorPostText();
        fillCommonFields(post, user, data);

        // Interior-specific
        post.setInteriorType(data.getInteriorType());
        post.setConditionInterior(data.getConditionInterior());

        return post;
    }

    /**
     * Создает пост категории "Инструменты".
     * <p>
     * Заполняет общие поля и специфичные для инструментов:
     * тип инструмента и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link ToolsPostText}
     */
    private ToolsPostText createToolsPost(User user, PostCreationData data) {
        ToolsPostText post = new ToolsPostText();
        fillCommonFields(post, user, data);

        // Tools-specific
        post.setToolsType(data.getToolsType());
        post.setConditionTools(data.getConditionTools());

        return post;
    }

    /**
     * Создает пост категории "Бытовая техника".
     * <p>
     * Заполняет общие поля и специфичные для бытовой техники:
     * тип техники и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link AppliancesPostText}
     */
    private AppliancesPostText createAppliancesPost(User user, PostCreationData data) {
        AppliancesPostText post = new AppliancesPostText();
        fillCommonFields(post, user, data);

        // Appliances-specific
        post.setAppliancesType(data.getAppliancesType());
        post.setConditionAppliances(data.getConditionAppliances());

        return post;
    }

    /**
     * Создает пост категории "Электроника и гаджеты".
     * <p>
     * Заполняет общие поля и специфичные для электроники:
     * тип электроники и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link ElectronicsPostText}
     */
    private ElectronicsPostText createElectronicsPost(User user, PostCreationData data) {
        ElectronicsPostText post = new ElectronicsPostText();
        fillCommonFields(post, user, data);

        // Electronics-specific
        post.setElectronicsType(data.getElectronicsType());
        post.setConditionElectronics(data.getConditionElectronics());

        return post;
    }

    /**
     * Создает пост категории "Автозапчасти и автотовары".
     * <p>
     * Заполняет общие поля и специфичные для автозапчастей:
     * тип автозапчасти и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link AutoPostText}
     */
    private AutoPostText createAutoPost(User user, PostCreationData data) {
        AutoPostText post = new AutoPostText();
        fillCommonFields(post, user, data);

        // Auto-specific
        post.setAutoType(data.getAutoType());
        post.setConditionAuto(data.getConditionAuto());

        return post;
    }

    /**
     * Создает пост категории "Одежда".
     * <p>
     * Заполняет общие поля и специфичные для одежды:
     * тип одежды, пол и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link ClothingPostText}
     */
    private ClothingPostText createClothingPost(User user, PostCreationData data) {
        ClothingPostText post = new ClothingPostText();
        fillCommonFields(post, user, data);

        // Clothing-specific
        post.setClothingType(data.getClothingType());
        post.setGenderClothing(data.getGenderClothing());
        post.setConditionClothing(data.getConditionClothing());

        return post;
    }

    /**
     * Создает пост категории "Детские товары".
     * <p>
     * Заполняет общие поля и специфичные для детских товаров:
     * пол (для кого) и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link ChildrenPrPostText}
     */
    private ChildrenPrPostText createChildrenPrPost(User user, PostCreationData data) {
        ChildrenPrPostText post = new ChildrenPrPostText();
        fillCommonFields(post, user, data);

        // ChildrenPr-specific
        post.setGenderChildrenPr(data.getGenderChildrenPr());
        post.setConditionChildrenPr(data.getConditionChildrenPr());

        return post;
    }

    /**
     * Создает пост категории "Спорт и отдых".
     * <p>
     * Заполняет общие поля и специфичные для спорта:
     * вид спорта, тип, пол и состояние.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link SportsPostText}
     */
    private SportsPostText createSportsPost(User user, PostCreationData data) {
        SportsPostText post = new SportsPostText();
        fillCommonFields(post, user, data);

        // Sports-specific
        post.setSportsKind(data.getSportsKind());
        post.setSportsType(data.getSportsType());
        post.setGenderSports(data.getGenderSports());
        post.setConditionSports(data.getConditionSports());

        return post;
    }

    /**
     * Создает пост категории "Продукты питания".
     * <p>
     * Заполняет общие поля и специфичные для продуктов:
     * вид продукта питания.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link FoodPostText}
     */
    private FoodPostText createFoodPost(User user, PostCreationData data) {
        FoodPostText post = new FoodPostText();
        fillCommonFields(post, user, data);

        // Food-specific
        post.setFoodKind(data.getFoodKind());

        return post;
    }

    /**
     * Создает пост категории "Здоровье и медицина".
     * <p>
     * Заполняет общие поля и специфичные для здоровья:
     * вид, тип и пол.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link HealthPostText}
     */
    private HealthPostText createHealthPost(User user, PostCreationData data) {
        HealthPostText post = new HealthPostText();
        fillCommonFields(post, user, data);

        // Health-specific
        post.setHealthKind(data.getHealthKind());
        post.setHealthType(data.getHealthType());
        post.setGenderHealth(data.getGenderHealth());

        return post;
    }

    /**
     * Создает пост категории "Животные и растения".
     * <p>
     * Заполняет общие поля и специфичные для животных/растений:
     * вид животного или растения.
     * </p>
     *
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     * @return объект {@link AnimalsPostText}
     */
    private AnimalsPostText createAnimalsPost(User user, PostCreationData data) {
        AnimalsPostText post = new AnimalsPostText();
        fillCommonFields(post, user, data);

        // Animals-specific
        post.setAnimalsKind(data.getAnimalsKind());

        return post;
    }

    /**
     * Заполняет общие для всех постов поля.
     * <p>
     * Устанавливает:
     * <ul>
     *   <li>Заголовок, цену, полный текст</li>
     *   <li>Категорию и местоположение</li>
     *   <li>Владельца поста (пользователя)</li>
     *   <li>Статус модерации (true - проходит модерацию)</li>
     *   <li>Дату создания (текущее время)</li>
     * </ul>
     * </p>
     *
     * @param post объект поста для заполнения
     * @param user пользователь, создающий пост
     * @param data данные для заполнения полей поста
     */
    private void fillCommonFields(PostText post, User user, PostCreationData data) {
        post.setTitle(data.getTitle());
        post.setPrice(data.getPrice());
        post.setFull_text(data.getFullText());
        post.setCategory(data.getCategory());
        post.setLocation(data.getLocation());
        post.setUser(user);
        post.setModeration(true);
        post.setCreatedAt(LocalDateTime.now());
    }
}