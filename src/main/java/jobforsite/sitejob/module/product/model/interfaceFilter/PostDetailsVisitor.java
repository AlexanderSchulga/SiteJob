package jobforsite.sitejob.module.product.model.interfaceFilter;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;

/**
 * Интерфейс посетителя для работы с деталями постов различных категорий.
 * <p>
 * Реализует паттерн Visitor для обхода иерархии классов постов.
 * Позволяет добавлять новые операции над иерархией классов,
 * не изменяя сами классы постов.
 * </p>
 * <p>
 * Поддерживаемые категории постов:
 * <ul>
 *   <li>Недвижимость ({@link RealEstatePostText})</li>
 *   <li>Автотранспорт ({@link MotorVehiclesPostText})</li>
 *   <li>Автозапчасти ({@link AutoPostText})</li>
 *   <li>Строительные материалы ({@link ConstructionsPostText})</li>
 *   <li>Дом и интерьер ({@link InteriorPostText})</li>
 *   <li>Инструменты ({@link ToolsPostText})</li>
 *   <li>Бытовая техника ({@link AppliancesPostText})</li>
 *   <li>Электроника ({@link ElectronicsPostText})</li>
 *   <li>Одежда ({@link ClothingPostText})</li>
 *   <li>Детские товары ({@link ChildrenPrPostText})</li>
 *   <li>Товары для спорта ({@link SportsPostText})</li>
 *   <li>Продукты питания ({@link FoodPostText})</li>
 *   <li>Здоровье и красота ({@link HealthPostText})</li>
 *   <li>Флора и фауна ({@link AnimalsPostText})</li>
 * </ul>
 * </p>
 *
 * @param <T> тип возвращаемого значения после посещения
 * @see PostText
 * @see RealEstatePostText
 * @see MotorVehiclesPostText
 */
public interface PostDetailsVisitor<T> {

    /**
     * Посещает пост категории "Недвижимость".
     *
     * @param post пост недвижимости
     * @return результат обработки
     */
    T visitRealty(RealEstatePostText post);

    /**
     * Посещает пост категории "Автотранспорт".
     *
     * @param post пост автотранспорта
     * @return результат обработки
     */
    T visitVehicle(MotorVehiclesPostText post);

    /**
     * Посещает пост категории "Автозапчасти".
     *
     * @param post пост автозапчастей
     * @return результат обработки
     */
    T visitAuto(AutoPostText post);

    /**
     * Посещает пост категории "Строительные материалы".
     *
     * @param post пост строительных материалов
     * @return результат обработки
     */
    T visitConstructions(ConstructionsPostText post);

    /**
     * Посещает пост категории "Дом и интерьер".
     *
     * @param post пост дома и интерьера
     * @return результат обработки
     */
    T visitInterior(InteriorPostText post);

    /**
     * Посещает пост категории "Инструменты".
     *
     * @param post пост инструментов
     * @return результат обработки
     */
    T visitTools(ToolsPostText post);

    /**
     * Посещает пост категории "Бытовая техника".
     *
     * @param post пост бытовой техники
     * @return результат обработки
     */
    T visitAppliances(AppliancesPostText post);

    /**
     * Посещает пост категории "Электроника".
     *
     * @param post пост электроники
     * @return результат обработки
     */
    T visitElectronics(ElectronicsPostText post);

    /**
     * Посещает пост категории "Одежда".
     *
     * @param post пост одежды
     * @return результат обработки
     */
    T visitClothing(ClothingPostText post);

    /**
     * Посещает пост категории "Детские товары".
     *
     * @param post пост детских товаров
     * @return результат обработки
     */
    T visitChildrenPr(ChildrenPrPostText post);

    /**
     * Посещает пост категории "Товары для спорта".
     *
     * @param post пост спортивных товаров
     * @return результат обработки
     */
    T visitSports(SportsPostText post);

    /**
     * Посещает пост категории "Продукты питания".
     *
     * @param post пост продуктов питания
     * @return результат обработки
     */
    T visitFood(FoodPostText post);

    /**
     * Посещает пост категории "Здоровье и красота".
     *
     * @param post пост здоровья и красоты
     * @return результат обработки
     */
    T visitHealth(HealthPostText post);

    /**
     * Посещает пост категории "Флора и фауна".
     *
     * @param post пост флоры и фауны
     * @return результат обработки
     */
    T visitAnimals(AnimalsPostText post);

    /**
     * Метод для обработки обычных постов (без специфических полей).
     * Используется как fallback для постов, не имеющих специфической категории.
     *
     * @param post обычный пост
     * @return результат обработки (по умолчанию null)
     */
    default T visitDefault(PostText post) {
        return null;
    }
}