package jobforsite.sitejob.module.shared.services;

import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для работы с городами и регионами.
 *
 * <p>Предоставляет методы для:
 * <ul>
 *   <li>Получения списка городов по региону с группировкой</li>
 *   <li>Проверки, является ли город стандартным для региона</li>
 *   <li>Подготовки значения города для фильтрации в запросах</li>
 * </ul>
 *
 * <p>Города делятся на две категории:
 * <ul>
 *   <li>Стандартные города — загружаются из конфигурации {@link RegionsConfig}</li>
 *   <li>Другие населенные пункты — объединенная группа для ручного ввода</li>
 * </ul>
 *
 * @author Team
 * @version 1.0
 */
@RequiredArgsConstructor
@Service
public class CityService {

    private static final Logger log = LoggerFactory.getLogger(CityService.class);

    private final RegionsConfig regionsConfig; // Конфигурация регионов и их городов

    /**
     * Получить все города для конкретного региона с объединенной группой.
     *
     * <p>Возвращает Map, где ключ — название группы городов, значение — список городов.
     * Всегда присутствует группа "Другие населенные пункты" с одним значением "other_none".
     *
     * @param regionKey ключ региона (например, "moscow", "spb" или "all")
     * @return Map с группированными городами, или пустая Map если регион не выбран или равен "all"
     */
    public Map<String, List<String>> getCitiesByRegion(String regionKey) {
        Map<String, List<String>> citiesMap = new LinkedHashMap<>();

        // Если регион не выбран или "all", возвращаем пустой список
        if (regionKey == null || "all".equals(regionKey)) {
            log.debug("Region not selected or 'all', returning empty cities map");
            return citiesMap;
        }

        log.debug("Getting cities for region: {}", regionKey);

        // Стандартные города из конфига
        List<String> standardCities = regionsConfig.getCitiesByRegion(regionKey);
        if (standardCities != null && !standardCities.isEmpty()) {
            citiesMap.put("Стандартные города", new ArrayList<>(standardCities));
            log.debug("Added {} standard cities for region {}", standardCities.size(), regionKey);
        } else {
            log.debug("No standard cities found for region {}", regionKey);
        }

        // "Другие населенные пункты (включая ручной ввод)"
        List<String> otherGroup = new ArrayList<>();
        otherGroup.add("other_none"); // Объединенное значение

        // Добавляем как одну строку меню
        citiesMap.put("Другие населенные пункты", otherGroup);
        log.debug("Added 'Other locations' group for region {}", regionKey);

        return citiesMap;
    }

    /**
     * Проверить, является ли город стандартным для региона.
     *
     * @param regionKey ключ региона
     * @param city название города для проверки
     * @return {@code true} если город является стандартным для указанного региона,
     *         {@code false} если регион или город не указаны, регион равен "other",
     *         или города нет в списке стандартных
     */
    public boolean isStandardCity(String regionKey, String city) {
        if (regionKey == null || city == null || "other".equals(regionKey)) {
            log.debug("isStandardCity: invalid parameters - regionKey={}, city={}, regionIsOther={}",
                    regionKey, city, "other".equals(regionKey));
            return false;
        }

        List<String> standardCities = regionsConfig.getCitiesByRegion(regionKey);
        boolean isStandard = standardCities != null && standardCities.contains(city);

        log.debug("City '{}' for region '{}' is standard: {}", city, regionKey, isStandard);
        return isStandard;
    }

    /**
     * Получить значение города для фильтрации.
     *
     * <p>Логика преобразования:
     * <ul>
     *   <li>Если city = null или "all" → возвращает null (означает "все города")</li>
     *   <li>Если city = "none" → возвращает null (для поиска пустых/не указанных городов в БД)</li>
     *   <li>Иначе возвращает city как есть</li>
     * </ul>
     *
     * @param cityParam параметр города из запроса
     * @return значение для фильтрации в SQL/JPQL, или null если нужно искать все или пустые значения
     */
    public String getCityFilterValue(String cityParam) {
        String result;

        if (cityParam == null || "all".equals(cityParam)) {
            result = null;
            log.debug("City filter: 'all' or null, returning null (no city restriction)");
        } else if ("none".equals(cityParam)) {
            // Для поиска пустых/не указанных городов
            result = null; // искать city IS NULL OR city = '' OR city = 'other'
            log.debug("City filter: 'none', returning null (for NULL/empty/other search)");
        } else {
            result = cityParam;
            log.debug("City filter: returning specific city '{}'", cityParam);
        }

        return result;
    }
}