package jobforsite.sitejob.module.shared.config.categoryconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Конфигурация категорий для резюме.
 * <p>
 * Определяет доступные сферы деятельности при создании/фильтрации резюме.
 * </p>
 *
 * <p>Доступные категории:</p>
 * <ul>
 *     <li>all - Все резюме</li>
 *     <li>it - IT и программирование</li>
 *     <li>prom - Промышленность</li>
 *     <li>build - Стройка</li>
 *     <li>street - Дороги и инфраструктура</li>
 *     <li>salle - Коммерция и продажи</li>
 *     <li>manager - Управление</li>
 *     <li>remote - Удалённая работа</li>
 *     <li>logistic - Логистика и склад</li>
 *     <li>medicine - Медицина и фармацевтика</li>
 *     <li>edu - Образование и наука</li>
 *     <li>service - Обслуживание и сервис</li>
 *     <li>cleaning - Клининг и уборка</li>
 *     <li>security - Охрана и безопасность</li>
 *     <li>beauty - Красота и здоровье</li>
 *     <li>auto - Автобизнес и транспорт</li>
 *     <li>agriculture - Сельское хозяйство</li>
 * </ul>
 *
 * @author JobForsite Team
 * @version 2.0
 * @since 2.0
 */
@Component
public class PostResumeCategoryConfig extends BaseCategoryConfig {

    private static final Logger log = LoggerFactory.getLogger(PostResumeCategoryConfig.class);

    /**
     * Инициализирует карту категорий для резюме.
     * <p>
     * Заполняет {@link #categories} предопределёнными значениями:
     * <ul>
     *     <li>all → "Все резюме"</li>
     *     <li>it → "IT и программирование"</li>
     *     <li>prom → "Промышленность"</li>
     *     <li>build → "Строительство"</li>
     *     <li>street → "Дороги и инфраструктура"</li>
     *     <li>salle → "Коммерция и продажи"</li>
     *     <li>manager → "Управление"</li>
     *     <li>remote → "Удалённая работа"</li>
     *     <li>logistic → "Логистика и склад"</li>
     *     <li>medicine → "Медицина и фармацевтика"</li>
     *     <li>edu → "Образование и наука"</li>
     *     <li>service → "Обслуживание и сервис"</li>
     *     <li>cleaning → "Клининг и уборка"</li>
     *     <li>security → "Охрана и безопасность"</li>
     *     <li>beauty → "Красота и здоровье"</li>
     *     <li>auto → "Автобизнес и транспорт"</li>
     *     <li>agriculture → "Сельское хозяйство"</li>
     * </ul>
     * </p>
     */
    @Override
    protected void initCategories() {
        log.debug("Инициализация конфигурации категорий для резюме");

        categories.put("all", "Все резюме");
        categories.put("it", "IT и программирование");
        categories.put("prom", "Промышленность");
        categories.put("build", "Строительство");
        categories.put("street", "Дороги и инфраструктура");
        categories.put("salle", "Коммерция и продажи");
        categories.put("manager", "Управление");
        categories.put("remote", "Удалённая работа");
        categories.put("logistic", "Логистика и склад");
        categories.put("medicine", "Медицина и фармацевтика");
        categories.put("edu", "Образование и наука");
        categories.put("service", "Обслуживание и сервис");
        categories.put("cleaning", "Клининг и уборка");
        categories.put("security", "Охрана и безопасность");
        categories.put("beauty", "Красота и здоровье");
        categories.put("auto", "Автобизнес и транспорт");
        categories.put("agriculture", "Сельское хозяйство");

        log.debug("Загружено {} категорий для резюме", categories.size());
    }
}