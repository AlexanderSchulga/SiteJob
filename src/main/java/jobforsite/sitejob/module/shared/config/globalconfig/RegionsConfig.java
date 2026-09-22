package jobforsite.sitejob.module.shared.config.globalconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Конфигурация для загрузки и работы с географическими данными (регионы и города).
 * <p>
 * Загружает данные из JSON-файлов, указанных в конфигурации, и предоставляет методы
 * для получения отображаемых названий регионов и городов с поддержкой кэширования.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *   <li>Автоматическая загрузка данных при старте приложения (@PostConstruct)</li>
 *   <li>Поддержка fallback-значений при недоступности файлов</li>
 *   <li>Кэширование названий регионов и городов для оптимизации производительности</li>
 *   <li>Потокобезопасные коллекции (ConcurrentHashMap)</li>
 * </ul>
 *
 * @see org.springframework.cache.annotation.Cacheable
 * @see jakarta.annotation.PostConstruct
 */
@Slf4j
@Component
public class RegionsConfig {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    /**
     * Путь к JSON-файлу с регионами. По умолчанию: classpath:geo/regions.json
     */
    @Value("${app.geo.regions-file:classpath:geo/regions.json}")
    private String regionsFile;

    /**
     * Путь к JSON-файлу с городами. По умолчанию: classpath:geo/cities.json
     */
    @Value("${app.geo.cities-file:classpath:geo/cities.json}")
    private String citiesFile;

    /**
     * Карта регионов (ключ -> отображаемое имя). LinkedHashMap сохраняет порядок добавления.
     */
    private final Map<String, String> regions = new LinkedHashMap<>();

    /**
     * Карта городов по регионам (ключ региона -> список городов).
     * ConcurrentHashMap обеспечивает потокобезопасность.
     */
    private final Map<String, List<String>> citiesByRegion = new ConcurrentHashMap<>();

    /**
     * Конструктор с внедрением зависимостей.
     *
     * @param resourceLoader загрузчик ресурсов для чтения JSON-файлов
     * @param objectMapper   маппер Jackson для парсинга JSON
     */
    public RegionsConfig(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    /**
     * Инициализация компонента после создания бина.
     * Загружает географические данные из JSON-файлов.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing RegionsConfig component...");
        loadGeoData();
    }

    /**
     * Основной метод загрузки географических данных.
     * При ошибках загружает fallback-данные по умолчанию.
     */
    private void loadGeoData() {
        try {
            log.debug("Loading geo data from files: regionsFile={}, citiesFile={}", regionsFile, citiesFile);
            loadRegionsFromJson();
            loadCitiesFromJson();

            int totalCities = citiesByRegion.values().stream().mapToInt(List::size).sum();
            log.info("Geo data loaded successfully: {} regions, {} cities total",
                    regions.size(), totalCities);

        } catch (Exception e) {
            log.warn("Failed to load geo data from files, falling back to default data");
            loadDefaultData();
        }
    }

    /**
     * Загружает регионы из JSON-файла.
     *
     * @throws Exception при ошибке чтения или парсинга файла
     */
    private void loadRegionsFromJson() throws Exception {
        Resource resource = resourceLoader.getResource(regionsFile);
        if (!resource.exists()) {
            log.warn("Regions file not found: {}, using default regions", regionsFile);
            loadDefaultRegions();
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            Map<String, String> loadedRegions = objectMapper.readValue(is,
                    new TypeReference<Map<String, String>>() {});

            regions.clear();
            regions.putAll(loadedRegions);

            // Гарантируем наличие специального ключа "all" для всех регионов
            if (!regions.containsKey("all")) {
                regions.put("all", "Все регионы");
                log.debug("Added default 'all' region entry");
            }

            log.debug("Loaded {} regions from JSON", regions.size());
        }
    }

    /**
     * Загружает города из JSON-файла.
     *
     * @throws Exception при ошибке чтения или парсинга файла
     */
    private void loadCitiesFromJson() throws Exception {
        Resource resource = resourceLoader.getResource(citiesFile);
        if (!resource.exists()) {
            log.warn("Cities file not found: {}, using default cities", citiesFile);
            loadDefaultCities();
            return;
        }

        try (InputStream is = resource.getInputStream()) {
            Map<String, List<String>> loadedCities = objectMapper.readValue(is,
                    new TypeReference<Map<String, List<String>>>() {});

            citiesByRegion.clear();
            citiesByRegion.putAll(loadedCities);

            // Для каждого региона из карты регионов добавляем пустой список, если его нет
            for (String regionKey : regions.keySet()) {
                citiesByRegion.putIfAbsent(regionKey, new ArrayList<>());
            }

            log.debug("Loaded cities for {} regions from JSON", citiesByRegion.size());
        }
    }

    /**
     * Загружает fallback-данные по умолчанию при ошибках.
     */
    private void loadDefaultData() {
        log.info("Loading default fallback geo data");
        loadDefaultRegions();
        loadDefaultCities();
    }

    /**
     * Загружает регионы по умолчанию.
     */
    private void loadDefaultRegions() {
        regions.clear();
        regions.put("all", "Все регионы");
        regions.put("moscow_oblast", "Москва и Московская область");
        // Дополнительные регионы закомментированы до необходимости
        // regions.put("leningrad_oblast", "Ленинградская область");
        // regions.put("belgorod", "Белгородская область");
        // regions.put("voronezh", "Воронежская область");
         regions.put("other", "Другой регион");

        log.debug("Loaded {} default regions", regions.size());
    }

    /**
     * Загружает города по умолчанию.
     */
    private void loadDefaultCities() {
        citiesByRegion.clear();
        citiesByRegion.put("moscow_oblast", Arrays.asList("Москва", "Балашиха", "Химки"));
        // Дополнительные города закомментированы до необходимости
        // citiesByRegion.put("leningrad_oblast", Arrays.asList("Санкт-Петербург", "Выборг"));
        // citiesByRegion.put("belgorod", Arrays.asList("Белгород", "Старый Оскол"));
        // citiesByRegion.put("voronezh", Arrays.asList("Воронеж"));
         citiesByRegion.put("other", new ArrayList<>());
         citiesByRegion.put("all", new ArrayList<>());

        log.debug("Loaded default cities for {} regions", citiesByRegion.size());
    }

    /**
     * Возвращает копию карты всех регионов (ключ -> отображаемое имя).
     *
     * @return новая LinkedHashMap с регионами
     */
    public Map<String, String> getRegions() {
        return new LinkedHashMap<>(regions);
    }

    /**
     * Возвращает отображаемое имя региона по его ключу.
     * Результат кэшируется для повышения производительности.
     *
     * @param regionKey ключ региона (например, "moscow_oblast")
     * @return отображаемое имя региона или "Регион не указан", если ключ не найден
     */
    @Cacheable(value = "regionDisplayNames", key = "#regionKey")
    public String getRegionDisplayName(String regionKey) {
        String displayName = regions.getOrDefault(regionKey, "Регион не указан");
        log.debug("Resolved region display name: key={}, name={}", regionKey, displayName);
        return displayName;
    }

    /**
     * Возвращает список городов для указанного региона.
     *
     * @param regionKey ключ региона
     * @return список городов (не null, может быть пустым)
     */
    public List<String> getCitiesByRegion(String regionKey) {
        List<String> cities = citiesByRegion.getOrDefault(regionKey, new ArrayList<>());
        log.debug("Retrieved cities for region {}: {} cities", regionKey, cities.size());
        return cities;
    }

    /**
     * Проверяет, есть ли города для указанного региона.
     *
     * @param regionKey ключ региона
     * @return true, если регион существует и имеет хотя бы один город
     */
    public boolean hasCitiesForRegion(String regionKey) {
        boolean hasCities = citiesByRegion.containsKey(regionKey) &&
                !citiesByRegion.get(regionKey).isEmpty();
        log.debug("Region {} has cities: {}", regionKey, hasCities);
        return hasCities;
    }

    /**
     * Возвращает отображаемое имя города с учётом региона.
     * Результат кэшируется (кроме случаев, когда результат null).
     *
     * @param regionKey ключ региона
     * @param cityValue значение города
     * @return отображаемое имя города или переданное значение, если город не найден
     */
    @Cacheable(value = "cityDisplayNames", key = "#regionKey + '_' + #cityValue",
            unless = "#result == null")
    public String getCityDisplayName(String regionKey, String cityValue) {
        // для отладки
        log.debug("getCityDisplayName - regionKey: {}, cityValue: {}", regionKey, cityValue);

        // Если regionKey = "all" или null, используем cityValue как есть
        if (regionKey == null || "all".equals(regionKey)) {
            return cityValue != null ? cityValue : "Город не указан";
        }

        if (regionKey == null || regionKey.isEmpty() || "all".equals(regionKey)) {
            return cityValue;
        }

        List<String> cities = citiesByRegion.get(regionKey);
        if (cities != null && cities.contains(cityValue)) {
            return cityValue;
        }

        return cityValue;
    }

    /**
     * Возвращает комбинированное отображаемое имя (регион + город).
     *
     * @param regionKey ключ региона
     * @param cityValue значение города
     * @return строка вида "Регион, Город" или только регион, если город не указан
     */
    public String getCityDisplayNameWithRegion(String regionKey, String cityValue) {
        String regionName = getRegionDisplayName(regionKey);
        String cityName = getCityDisplayName(regionKey, cityValue);

        if ("Населенный пункт не указан".equals(cityName)) {
            log.debug("City not specified for region {}, returning only region name", regionKey);
            return regionName;
        }

        String result = regionName + ", " + cityName;
        log.debug("Combined display name: {}", result);
        return result;
    }

    /**
     * Возвращает сгруппированные города для региона (стандартные + ручные).
     *
     * @param regionKey ключ региона
     * @return карта с ключами "standard" и "manual"
     */
    public Map<String, List<String>> getGroupedCitiesByRegion(String regionKey) {
        Map<String, List<String>> groupedCities = new LinkedHashMap<>();
        List<String> standardCities = citiesByRegion.getOrDefault(regionKey, new ArrayList<>());

        groupedCities.put("standard", standardCities);
        groupedCities.put("manual", new ArrayList<>());

        log.debug("Grouped cities for region {}: {} standard, 0 manual", regionKey, standardCities.size());
        return groupedCities;
    }

    /**
     * Проверяет, является ли город стандартным для указанного региона.
     *
     * @param regionKey ключ региона
     * @param city название города
     * @return true, если город есть в стандартном списке
     */
    public boolean isStandardCity(String regionKey, String city) {
        List<String> cities = citiesByRegion.get(regionKey);
        boolean isStandard = cities != null && cities.contains(city);
        log.debug("City '{}' is standard for region '{}': {}", city, regionKey, isStandard);
        return isStandard;
    }

    /**
     * Определяет тип города (standard/manual/none).
     *
     * @param regionKey ключ региона
     * @param city название города
     * @return "standard", "manual" или "none"
     */
    public String getCityType(String regionKey, String city) {
        if (city == null || city.isEmpty() || "other".equals(city)) {
            log.debug("City '{}' classified as 'none'", city);
            return "none";
        }
        String type = isStandardCity(regionKey, city) ? "standard" : "manual";
        log.debug("City '{}' for region '{}' classified as '{}'", city, regionKey, type);
        return type;
    }

    /**
     * Возвращает значение фильтра для городов типа "other".
     *
     * @return строка "other_none"
     */
    public String getOtherCityFilterValue() {
        return "other_none";
    }

    /**
     * Проверяет, относится ли город к типу "other".
     *
     * @param city название города
     * @return true, если город null, пустой или равен "none"/"other"/"other_none"
     */
    public boolean isOtherCityType(String city) {
        boolean isOther = city == null || city.isEmpty() || "none".equals(city) ||
                "other".equals(city) || "other_none".equals(city);
        if (isOther) {
            log.debug("City '{}' identified as 'other' type", city);
        }
        return isOther;
    }

    /**
     * Возвращает отображаемое имя для городов типа "other".
     *
     * @return строка "Другие населенные пункты"
     */
    public String getOtherCityDisplayName() {
        return "Другие населенные пункты";
    }
}