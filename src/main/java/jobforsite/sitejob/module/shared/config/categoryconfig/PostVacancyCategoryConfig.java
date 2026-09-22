package jobforsite.sitejob.module.shared.config.categoryconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Конфигурация категорий для вакансий.
 * <p>
 * Определяет доступные должности/профессии при создании/фильтрации вакансий.
 * </p>
 *
 * <p>Доступные категории:</p>
 * <ul>
 *     <li>all - Все вакансии</li>
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
public class PostVacancyCategoryConfig extends BaseCategoryConfig {

    private static final Logger log = LoggerFactory.getLogger(PostVacancyCategoryConfig.class);

    /**
     * Инициализирует карту категорий для вакансий.
     * <p>
     * Заполняет {@link #categories} предопределёнными значениями:
     * <ul>
     *     <li>all → "Все вакансии"</li>
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
        log.debug("Инициализация конфигурации категорий для вакансий");

        categories.put("all", "Все вакансии");
        categories.put("auto", "Автобизнес и транспорт");
        categories.put("street", "Дороги и инфраструктура");
        categories.put("cleaning", "Клининг и уборка");
        categories.put("salle", "Коммерция и продажи");
        categories.put("beauty", "Красота и здоровье");
        categories.put("logistic", "Логистика и склад");
        categories.put("medicine", "Медицина и фармацевтика");
        categories.put("edu", "Образование и наука");
        categories.put("service", "Обслуживание и сервис");
        categories.put("security", "Охрана и безопасность");
        categories.put("prom", "Промышленность");
        categories.put("agriculture", "Сельское хозяйство");
        categories.put("build", "Строительство");
        categories.put("remote", "Удалённая работа");
        categories.put("manager", "Управление");
        categories.put("it", "IT и программирование");

        log.debug("Загружено {} категорий для вакансий", categories.size());
    }
}