package jobforsite.sitejob.module.shared.config.categoryconfig;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Конфигурация формата работы для резюме.
 * <p>
 * Определяет доступные варианты формата работы при создании/фильтрации резюме.
 * </p>
 *
 * <p>Доступные форматы работы:</p>
 * <ul>
 *     <li>all - Все</li>
 *     <li>one - Удаленно</li>
 *     <li>two - На месте работодателя</li>
 *     <li>three - Гибрид</li>
 *    <li>four - Не имеет значение</li>

 * </ul>
 *
 * @author JobForsite Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class WorkFormatResumeConfig extends BaseCategoryConfig {


    private static final Logger log = LoggerFactory.getLogger(WorkFormatResumeConfig.class);

    /**
     * Инициализирует карту формата работы для резюме.
     * <p>
     * Заполняет {@link #workformat} предопределенными значениями:
     * <ul>
     *     <li>all - Все</li>
     *     <li>one - Удаленно</li>
     *     <li>two - На месте работодателя</li>
     *     <li>three - Гибрид</li>
     *    <li>four - Не имеет значение</li>

     * </ul>
     * </p>
     */
    @Override
    protected void initCategories() {
        log.debug("Инициализация конфигурации формата работы для резюме");

        workformat.put("all", "Все");
        workformat.put("one", "Удаленно");
        workformat.put("two", "На месте работодателя");
        workformat.put("three", "Гибрид");
        workformat.put("four", "Не имеет значение");

        log.debug("Загружено {} форматы работы для резюме", workformat.size());
    }


}
