package jobforsite.sitejob.module.shared.config.categoryconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Конфигурация уровней опыта для вакансий.
 * <p>
 * Определяет доступные варианты опыта работы при создании/фильтрации вакансии.
 * </p>
 *
 * <p>Доступные уровни опыта:</p>
 * <ul>
 *     <li>all - Не имеет значение</li>
 *     <li>no - Без опыта</li>
 *     <li>one - 0,5 - 1 год</li>
 *     <li>two - 1 - 3 года</li>
 *     <li>three - 3 - 5 лет</li>
 *     <li>four - Более 5 лет</li>
 * </ul>
 *
 * @author JobForsite Team
 * @version 1.0
 * @since 1.0
 */

@Component

public class ExperienceVacancyCategoryConfig extends BaseCategoryConfig{


    private static final Logger log = LoggerFactory.getLogger(ExperienceVacancyCategoryConfig.class);

    /**
     * Инициализирует карту уровней опыта для вакансии.
     * <p>
     * Заполняет {@link #experience} предопределенными значениями:
     * <ul>
     *     <li>all → "Не имеет значение"</li>
     *     <li>no → "Без опыта"</li>
     *     <li>one → "0,5 - 1"</li>
     *     <li>two → "1 - 3"</li>
     *     <li>three → "3 - 5"</li>
     *     <li>four → "Более 5 лет"</li>
     * </ul>
     * </p>
     */
    @Override
    protected void initCategories() {
        log.debug("Инициализация конфигурации опыта для вакансии");

        experience.put("all", "Не имеет значение");
        experience.put("no", "Без опыта");
        experience.put("one", "0,5 - 1");
        experience.put("two", "1 - 3");
        experience.put("three", "3 - 5");
        experience.put("four", "Более 5 лет");

        log.debug("Загружено {} уровней опыта для вакансии", experience.size());
    }

}
