package jobforsite.sitejob.module.shared.config.categoryconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Конфигурация категорий для товаров/объявлений.
 * <p>
 * Определяет доступные категории товаров при создании/фильтрации объявлений.
 * </p>
 *
 * <p>Доступные категории:</p>
 * <ul>
 *     <li>all - Все товары</li>
 *     <li>electronics - Электроника и гаджеты</li>
 *     <li>home - Дом и интерьер</li>
 *     <li>construction - Строительные материалы</li>
 *     <li>auto - Автозапчасти и автотовары</li>
 *     <li>clothing - Одежда и обувь</li>
 *     <li>children - Детские товары</li>
 *     <li>sports - Спорттовары</li>
 *     <li>food - Продукты питания</li>
 *     <li>health - Здоровье и красота</li>
 *     <li>animals - Товары для животных</li>
 *     <li>tools - Инструменты и сад</li>
 *     <li>furniture - Мебель</li>
 *     <li>books - Книги и канцтовары</li>
 *     <li>phones - Телефоны и аксессуары</li>
 *     <li>appliances - Бытовая техника</li>
 * </ul>
 *
 * @author JobForsite Team
 * @version 2.0
 * @since 2.0
 */
@Component
public class PostTextCategoryConfig extends BaseCategoryConfig {

    private static final Logger log = LoggerFactory.getLogger(PostTextCategoryConfig.class);

    /**
     * Инициализирует карту категорий для товаров.
     * <p>
     * Заполняет {@link #categories} предопределёнными значениями:
     * <ul>
     *     <li>all → "Все товары"</li>
     *     <li>electronics → "Электроника и гаджеты"</li>
     *     <li>home → "Дом и интерьер"</li>
     *     <li>construction → "Строительные материалы"</li>
     *     <li>auto → "Автозапчасти и автотовары"</li>
     *     <li>clothing → "Одежда и обувь"</li>
     *     <li>children → "Детские товары"</li>
     *     <li>sports → "Спорттовары"</li>
     *     <li>food → "Продукты питания"</li>
     *     <li>health → "Здоровье и красота"</li>
     *     <li>animals → "Флора и фауна"</li>
     *     <li>tools → "Инструменты и сад"</li>
     *     <li>furniture → "Мебель"</li>
     *     <li>books → "Книги и канцтовары"</li>
     *     <li>phones → "Телефоны и аксессуары"</li>
     *     <li>appliances → "Бытовая техника"</li>
     *     <li>farming → "Хозяйство и дача"</li>
     * </ul>
     * </p>
     */
    @Override
    protected void initCategories() {
        log.debug("Инициализация конфигурации категорий для товаров");

        categories.put("all", "Все товары");
        categories.put("realty", "Недвижимость");
        categories.put("home", "Дом и интерьер");
        categories.put("electronics", "Электроника и гаджеты");
        categories.put("construction", "Строительные материалы");
        categories.put("motor", "Автотранспорт");
        categories.put("auto", "Автозапчасти и автотовары");
        categories.put("clothing", "Одежда и обувь");
        categories.put("children", "Детские товары");
        categories.put("sports", "Спорттовары");
        categories.put("food", "Продукты питания");
        categories.put("health", "Здоровье и красота");
        categories.put("animals", "Флора и фауна");
        categories.put("tools", "Инструменты и сад");
        categories.put("furniture", "Мебель");
        categories.put("books", "Книги и канцтовары");
        categories.put("phones", "Телефоны и аксессуары");
        categories.put("appliances", "Бытовая техника");
        categories.put("farming", "Хозяйство и дача");

        log.debug("Загружено {} категорий для товаров", categories.size());
    }
}