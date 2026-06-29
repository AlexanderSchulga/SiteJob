package jobforsite.sitejob.module.shared.config.categoryconfig;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Абстрактная конфигурация категорий и опыта работы для вакансий.
 * <p>
 * Предоставляет базовую структуру для хранения и получения отображаемых имен
 * категорий и уровней опыта. Наследники должны реализовать метод {@link #initCategories()}
 * для заполнения карт категорий и опыта.
 * </p>
 *
 * <p>Пример использования:</p>
 * <pre>
 * public class JobCategoryConfig extends BaseCategoryConfig {
 *     {@literal @}Override
 *     protected void initCategories() {
 *         categories.put("IT", "Информационные технологии");
 *         experience.put("1-3", "1-3 года");
 *     }
 * }
 * </pre>
 *
 * @author JobForsite Team
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseCategoryConfig {

    private static final Logger log = LoggerFactory.getLogger(BaseCategoryConfig.class);

    /** Карта категорий (ключ -> отображаемое имя) */
    protected final Map<String, String> categories = new LinkedHashMap<>();

    /** Карта уровней опыта (ключ -> отображаемое имя) */
    protected final Map<String, String> experience = new LinkedHashMap<>();

    /** Карта формата работы (ключ -> отображаемое имя) */
    protected final Map<String, String> workformat = new LinkedHashMap<>();

    /**
     * Абстрактный метод инициализации категорий и опыта.
     * <p>
     * Должен быть реализован в классе-наследнике. В реализации необходимо
     * заполнить {@link #categories} и {@link #experience} и {@link #workformat}.
     * </p>
     */
    protected abstract void initCategories();

    /**
     * Конструктор, автоматически вызывающий {@link #initCategories()}.
     */
    protected BaseCategoryConfig() {
        log.debug("Инициализация конфигурации категорий");
        initCategories();
        log.debug("Инициализация завершена. Загружено категорий: {}, уровней опыта: {}, формат работы {}",
                categories.size(), experience.size(), workformat.size());
    }

    /**
     * Возвращает неизменяемую копию карты категорий.
     *
     * @return новая {@link LinkedHashMap} со всеми категориями
     */
    public Map<String, String> getCategories() {
        return new LinkedHashMap<>(categories);
    }

    /**
     * Возвращает неизменяемую копию карты уровней опыта.
     *
     * @return новая {@link LinkedHashMap} со всеми уровнями опыта
     */
    public Map<String, String> getExperience() {
        return new LinkedHashMap<>(experience);
    }


    /**
     * Возвращает неизменяемую копию карты формата работы.
     *
     * @return новая {@link LinkedHashMap} со всеми форматами
     */
    public Map<String, String> getWorkformat() {
        return new LinkedHashMap<>(workformat);
    }

    /**
     * Возвращает отображаемое имя категории по её ключу.
     *
     * @param categoryKey ключ категории (например, "IT")
     * @return отображаемое имя категории, или "Неизвестная категория", если ключ не найден
     */
    public String getCategoryDisplayName(String categoryKey) {
        String displayName = categories.getOrDefault(categoryKey, "Неизвестная категория");
        if (!categories.containsKey(categoryKey)) {
            log.warn("Запрошена неизвестная категория: {}", categoryKey);
        }
        return displayName;
    }

    /**
     * Возвращает отображаемое имя уровня опыта по его ключу.
     *
     * @param experienceKey ключ опыта (например, "1-3")
     * @return отображаемое имя опыта, или "опыт не указан", если ключ не найден
     */
    public String getExperienDisplayName(String experienceKey) {
        String displayName = experience.getOrDefault(experienceKey, "опыт не указан");
        if (!experience.containsKey(experienceKey)) {
            log.warn("Запрошен неизвестный уровень опыта: {}", experienceKey);
        }
        return displayName;
    }


    /**
     * Возвращает отображаемое имя формата работы по его ключу.
     *
     * @param workformatKey ключ формата (например, "1-3")
     * @return отображаемое имя формата, или "формат не имеет значения", если ключ не найден
     */
    public String getWorkformatDisplayName(String workformatKey) {
        String displayName = workformat.getOrDefault(workformatKey, "не имеет значения");
        if (!workformat.containsKey(workformatKey)) {
            log.warn("Запрошен неизвестный формат опыта: {}", workformatKey);
        }
        return displayName;
    }
}