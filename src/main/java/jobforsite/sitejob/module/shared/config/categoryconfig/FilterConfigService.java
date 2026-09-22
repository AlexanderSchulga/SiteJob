package jobforsite.sitejob.module.shared.config.categoryconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Сервис для загрузки и предоставления конфигурации фильтров для категорий продуктов.
 * <p>
 * Загружает настройки фильтров из JSON-файла (geo/filters.json) и предоставляет
 * методы для доступа к опциям фильтрации для различных категорий и подкатегорий.
 * </p>
 * <p>
 * Структура данных:
 * <pre>
 * category -> subcategory -> filterKey -> {label, type, options}
 * </pre>
 * </p>
 * <p>
 * Пример использования в Thymeleaf:
 * <pre>
 * ${filterConfigService.getOptions('realty', 'apartment', 'roomsCount')}
 * </pre>
 * </p>
 *
 */

@Component
public class FilterConfigService {

    private static final Logger log = LoggerFactory.getLogger(FilterConfigService.class);

    /**
     * Основная структура хранения фильтров.
     * <p>
     * Уровни вложенности:
     * <ul>
     *   <li>1-й уровень: категория (например, "realty", "motor", "auto")</li>
     *   <li>2-й уровень: подкатегория (например, "apartment", "house", "land")</li>
     *   <li>3-й уровень: ключ фильтра (например, "roomsCount", "vehicleType")</li>
     *   <li>Значение: Map с полями label, type, options</li>
     * </ul>
     * </p>
     */

    // Структура: category -> subcategory -> filterKey -> {label, type, options}
    private final Map<String, Map<String, Map<String, Object>>> filters = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Инициализация сервиса после создания бина.
     * <p>
     * Загружает конфигурацию фильтров из файла geo/filters.json.
     * В случае ошибки загружает дефолтные фильтры (заглушка).
     * </p>
     */

    @PostConstruct
    public void init() {
        try {
            log.info("Loading filters from JSON configuration...");

            ClassPathResource resource = new ClassPathResource("geo/filters.json");
            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, Object> config = objectMapper.readValue(
                        inputStream,
                        new TypeReference<Map<String, Object>>() {}
                );

                @SuppressWarnings("unchecked")
                Map<String, Map<String, Map<String, Object>>> filtersData =
                        (Map<String, Map<String, Map<String, Object>>>) config.get("filters");

                filters.putAll(filtersData);

                log.info("Loaded filters: realty categories: {}, motor categories: {}, auto categories: {}, constructions categories: {}, " +
                                "interior categories: {}, tools categories: {}, appliances categories: {}, electronics categories: {}, " +
                                "clothing categories: {}, childrenPr categories: {}, sports categories: {}, food categories: {}, " +
                                "health categories: {}, animals categories: {}",
                        filters.getOrDefault("realty", Map.of()).size(),
                        filters.getOrDefault("motor", Map.of()).size(),

                        filters.getOrDefault("auto", Map.of()).size(),
                        filters.getOrDefault("constructions", Map.of()).size(),
                        filters.getOrDefault("interior", Map.of()).size(),
                        filters.getOrDefault("tools", Map.of()).size(),
                        filters.getOrDefault("appliances", Map.of()).size(),
                        filters.getOrDefault("electronics", Map.of()).size(),
                        filters.getOrDefault("clothing", Map.of()).size(),
                        filters.getOrDefault("childrenPr", Map.of()).size(),
                        filters.getOrDefault("sports", Map.of()).size(),
                        filters.getOrDefault("food", Map.of()).size(),
                        filters.getOrDefault("health", Map.of()).size(),
                        filters.getOrDefault("animals", Map.of()).size()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to load filters from JSON, using default filters");
            loadDefaultFilters();
        }
    }

    private void loadDefaultFilters() {
        log.warn("Loading default filters as fallback");
        // Можно добавить дефолтные фильтры в коде
    }

    /**
     * Получить опции для конкретного фильтра.
     * @param category - основная категория (realty, motor)
     * @param subcategory - подкатегория (apartment, house, land, motor)
     * @param filterKey - ключ фильтра (roomsCount, vehicleType и т.д.)
     * @return Map с опциями фильтра
     */
    public Map<String, String> getFilterOptions(String category, String subcategory, String filterKey) {
        // Получаем карту подкатегорий для данной категории
        Map<String, Map<String, Object>> subcategories = filters.get(category);
        if (subcategories == null) {
            log.warn("Category not found: {}", category);
            return new LinkedHashMap<>();
        }

        // Получаем карту фильтров для данной подкатегории
        Map<String, Object> subcategoryFilters = subcategories.get(subcategory);
        if (subcategoryFilters == null) {
            log.warn("Subcategory not found: {} in category: {}", subcategory, category);
            return new LinkedHashMap<>();
        }

        // Получаем данные конкретного фильтра
        Object filter = subcategoryFilters.get(filterKey);
        if (filter == null) {
            log.warn("Filter key not found: {} in subcategory: {}", filterKey, subcategory);
            return new LinkedHashMap<>();
        }

        // Извлекаем options из фильтра
        if (filter instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> filterMap = (Map<String, Object>) filter;
            Object options = filterMap.get("options");
            if (options instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> result = (Map<String, String>) options;
                return result;
            }
        }

        return new LinkedHashMap<>();
    }


    /**
     * Удобный метод для вызова из шаблонов Thymeleaf
     * Пример: ${filterConfigService.getOptions('realty', 'apartment', 'roomsCount')}
     */
    public Map<String, String> getOptions(String category, String subcategory, String filterKey) {
        return getFilterOptions(category, subcategory, filterKey);
    }

}