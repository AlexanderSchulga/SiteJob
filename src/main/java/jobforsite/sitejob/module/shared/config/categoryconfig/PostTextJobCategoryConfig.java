package jobforsite.sitejob.module.shared.config.categoryconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Конфигурация категорий для услуг.
 * <p>
 * Определяет доступные категории услуг при создании/фильтрации объявлений об услугах.
 * </p>
 *
 * <p>Доступные категории:</p>
 * <ul>
 *     <li>all - Все услуги</li>
 *     <li>cleaning - Уборка и клининг</li>
 *     <li>repair - Ремонт и отделка</li>
 *     <li>construction - Строительные работы</li>
 *     <li>beauty - Красота и здоровье</li>
 *     <li>transport - Транспортные услуги</li>
 *     <li>education - Обучение и курсы</li>
 *     <li>event - Организация мероприятий</li>
 *     <li>it - IT и компьютерная помощь</li>
 *     <li>legal - Юридические услуги</li>
 *     <li>accounting - Бухгалтерия и налоги</li>
 *     <li>photography - Фото и видео</li>
 *     <li>moving - Грузоперевозки и переезд</li>
 *     <li>handyman - Ремонт и монтаж (разнорабочие)</li>
 *     <li>pets - Услуги для животных</li>
 *     <li>elderly - Уход за пожилыми и детьми</li>
 * </ul>
 *
 * @author JobForsite Team
 * @version 2.0
 * @since 2.0
 */
@Component
public class PostTextJobCategoryConfig extends BaseCategoryConfig {

    private static final Logger log = LoggerFactory.getLogger(PostTextJobCategoryConfig.class);

    /**
     * Инициализирует карту категорий для услуг.
     * <p>
     * Заполняет {@link #categories} предопределёнными значениями:
     * <ul>
     *     <li>all → "Все услуги"</li>
     *     <li>cleaning → "Уборка и клининг"</li>
     *     <li>repair → "Ремонт и отделка"</li>
     *     <li>construction → "Строительные работы"</li>
     *     <li>beauty → "Красота и здоровье"</li>
     *     <li>transport → "Транспортные услуги"</li>
     *     <li>moving → "Грузоперевозки и переезд"</li>
     *     <li>education → "Обучение и курсы"</li>
     *     <li>event → "Организация мероприятий"</li>
     *     <li>it → "IT и компьютерная помощь"</li>
     *     <li>legal → "Юридические услуги"</li>
     *     <li>accounting → "Бухгалтерия и налоги"</li>
     *     <li>photography → "Фото и видео"</li>
     *     <li>travel → "Путешествия и отдых"</li>
     *     <li>pets → "Услуги для животных"</li>
     *     <li>elderly → "Уход за пожилыми и детьми"</li>
     * </ul>
     * </p>
     */
    @Override
    protected void initCategories() {
        log.debug("Инициализация конфигурации категорий для услуг");

        categories.put("all", "Все услуги");
        categories.put("cleaning", "Уборка и клининг");
        categories.put("repair", "Ремонт и отделка");
        categories.put("construction", "Строительные работы");
        categories.put("beauty", "Красота и здоровье");
        categories.put("transport", "Транспортные услуги");
        categories.put("moving", "Грузоперевозки и переезд");
        categories.put("rent", "Аренда");
        categories.put("education", "Обучение и курсы");
        categories.put("event", "Организация мероприятий");
        categories.put("it", "IT и компьютерная помощь");
        categories.put("legal", "Юридические услуги");
        categories.put("accounting", "Бухгалтерия и налоги");
        categories.put("photography", "Фото и видео");
        categories.put("travel", "Путешествия и отдых");
        categories.put("pets", "Услуги для животных");
        categories.put("elderly", "Уход за пожилыми и детьми");

        log.debug("Загружено {} категорий для услуг", categories.size());
    }
}