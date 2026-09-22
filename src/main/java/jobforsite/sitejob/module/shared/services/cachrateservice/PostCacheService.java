package jobforsite.sitejob.module.shared.services.cachrateservice;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.MotorVehiclesPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.RealEstatePostText;
import jobforsite.sitejob.module.product.service.ProductService;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для кэширования публичных постов (товаров, услуг, вакансий, резюме).
 * <p>
 * Обеспечивает получение страниц (Page) и списков (List) сущностей с фильтрацией
 * по категории, региону, городу, поисковому запросу, сортировкой по дате и цене.
 * Также содержит методы для получения количества записей и очистки кэша.
 * </p>
 * <p>
 * Ключи кэша строятся на основе всех параметров фильтрации. При изменении данных
 * необходимо вызывать соответствующие методы очистки кэша.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PostCacheService {

    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final RegionsConfig regionsConfig;

    private final ProductService productService;


    // ==================== ТОВАРЫ (PRODUCTS) ====================

    /**
     * Получает список товаров с фильтрацией и пагинацией (без информации об общем количестве).
     * Результат кэшируется.
     *
     * @param category   категория товара
     * @param search     поисковый запрос
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по цене
     * @param page       номер страницы
     * @param size       размер страницы
     * @return List товаров
     */
    // ===== ТОВАРЫ С РЕГИОНОМ И ГОРОДОМ =====
    @Cacheable(value = "productsList",
            key = "#category + '_' + #search + '_' + #region + '_' + #city + '_' + #page + '_' + #size + '_' + #sort + '_' + #priceSort")
    public List<PostText> getProductsList(String category, String search,
                                          String region, String city,
                                          String sort, String priceSort,
                                          int page, int size) {
        log.info("CACHE MISS for products list with region and city: region={}, city={}", region, city);

        Pageable pageable = createPageable(page, size, sort, priceSort, "product");
        return productService.findPostsWithRegionAndCity(category, search, region, city, pageable);
    }

    // ===== НЕДВИЖИМОСТЬ С РЕГИОНОМ, ГОРОДОМ, СОРТИРОВКОЙ =====

    @Cacheable(value = "realtyList",
            key = "#filter.search + '_' + #filter.propertyType + '_' + " +
                    // Квартира
                    "#filter.roomsCount + '_' + " +
                    "#filter.floorsApartment + '_' + " +
                    "#filter.floorsCount + '_' + " +
                    "#filter.hasElevator + '_' + " +
                    "#filter.apartmentBuildYear + '_' + " +
                    "#filter.materialUp + '_' + " +
                    "#filter.repairUp + '_' + " +
                    // Дом
                    "#filter.floorsCountHouse + '_' + " +
                    "#filter.houseArea + '_' + " +
                    "#filter.landArea + '_' + " +
                    "#filter.houseBuildYear + '_' + " +
                    "#filter.materialHouse + '_' + " +
                    "#filter.repairHouse + '_' + " +
                    "#filter.distanceToCityHouse + '_' + " +
                    // Участок
                    "#filter.landArea + '_' + " +
                    "#filter.distanceToCity + '_' + " +
                    // Гараж
                    "#filter.landAreaGarage + '_' + " +
                    // Коммерческая
                    "#filter.landAreaCommercial + '_' + " +
                    // Базовые
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<RealEstatePostText> getRealtyList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for realty list: propertyType={}, floorsApartment={}, houseArea={}, landArea={}",
                filter.getPropertyType(), filter.getFloorsApartment(), filter.getHouseArea(), filter.getLandArea());
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return productService.filterRealtyWithRegionAndSort(filter, pageable);
    }

    // Детальная фильтрация квартир
    @Cacheable(value = "apartmentsList",
            key = "#roomsCount + '_' + #minFloors + '_' + #maxFloors + '_' + #hasElevator + '_' + #minYear + '_' + #maxYear + '_' + #page + '_' + #size")
    public List<RealEstatePostText> getApartmentsList(Integer roomsCount, Integer minFloors, Integer maxFloors,
                                                      Boolean hasElevator, Integer minYear, Integer maxYear,
                                                      int page, int size) {
        log.info("CACHE MISS for apartments list");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "moderatedAt"));
        return productService.filterApartmentsOptimized(roomsCount, minFloors, maxFloors,
                hasElevator, minYear, maxYear, pageable);
    }

    // ===== АВТОТРАНСПОРТ С РЕГИОНОМ, ГОРОДОМ, СОРТИРОВКОЙ =====

    @Cacheable(value = "vehicleList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.vehicleType + '_' + #filter.vehicleOrigin + '_' + " +
                    "#filter.minYear + '_' + #filter.maxYear + '_' + " +
                    "#filter.minPrice + '_' + #filter.maxPrice + '_' + " +
                    "#filter.minMileage + '_' + #filter.maxMileage + '_' + " +
                    "#filter.minNumberOfOwners + '_' + #filter.maxNumberOfOwners + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<MotorVehiclesPostText> getVehicleList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for vehicle list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return productService.filterVehiclesWithRegionAndSort(filter, pageable);
    }

    // ===== ПОДСЧЕТ ТОВАРОВ С РЕГИОНОМ И ГОРОДОМ =====
    @Cacheable(value = "productsCount",
            key = "#category + '_' + #search + '_' + #region + '_' + #city")
    public long getProductsCount(String category, String search, String region, String city) {
        return productService.countPostsWithRegionAndCity(category, search, region, city);
    }

    // ===== ТОВАРЫ БЕЗ РЕГИОНА (для обратной совместимости) =====
    @Cacheable(value = "productsListNoRegion",
            key = "#category + '_' + #search + '_' + #page + '_' + #size + '_' + #sort + '_' + #priceSort")
    public List<PostText> getProductsListNoRegion(String category, String search,
                                                  String sort, String priceSort,
                                                  int page, int size) {
        log.info("CACHE MISS for products list without region");
        Pageable pageable = createPageable(page, size, sort, priceSort, "product");
        return productService.findPostsOptimized(category, search, pageable);
    }

    // ===== ПОДСЧЕТ ТОВАРОВ БЕЗ РЕГИОНА =====
    @Cacheable(value = "productsCountNoRegion", key = "#category + '_' + #search")
    public long getProductsCountNoRegion(String category, String search) {
        return productService.countPostsWithRegionAndCity(category, search, null, null);
    }

    // ===== ПОДСЧЕТ НЕДВИЖИМОСТИ =====
    @Cacheable(value = "realtyCount",
            key = "#filter.search + '_' + " +
                    "#filter.propertyType + '_' + #filter.roomsCount + '_' + " +
                    "#filter.minFloorsApartment + '_' + #filter.maxFloorsApartment + '_' + " +
                    "#filter.floorsCountMin + '_' + #filter.floorsCountMax + '_' + " +
                    "#filter.floorsCountHouseMin + '_' + #filter.floorsCountHouseMax + '_' + " +
                    "#filter.hasElevator + '_' + " +
                    "#filter.minBuildYear + '_' + #filter.maxBuildYear + '_' + " +
                    "#filter.minHouseArea + '_' + #filter.maxHouseArea + '_' + " +
                    "#filter.minLandArea + '_' + #filter.maxLandArea + '_' + " +
                    "#filter.minDistanceToCity + '_' + #filter.maxDistanceToCity + '_' + " +
                    "#filter.materialUp + '_' + #filter.repairUp + '_' + " +
                    "#filter.materialHouse + '_' + #filter.repairHouse + '_' + " +
                    "#filter.minDistanceToCityHouse + '_' + #filter.maxDistanceToCityHouse + '_' + " +
                    "#filter.minLandAreaGarage + '_' + #filter.maxLandAreaGarage + '_' + " +
                    "#filter.minLandAreaCommercial + '_' + #filter.maxLandAreaCommercial + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getRealtyCount(ProductFilterDTO filter) {
        return productService.countRealtyWithFilters(filter);
    }


    // ===== ПОДСЧЕТ АВТОТРАНСПОРТА =====
    @Cacheable(value = "vehicleCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.vehicleType + '_' + #filter.vehicleOrigin + '_' + " +
                    "#filter.minYear + '_' + #filter.maxYear + '_' + " +
                    "#filter.minPrice + '_' + #filter.maxPrice + '_' + " +
                    "#filter.minMileage + '_' + #filter.maxMileage + '_' + " +
                    "#filter.minNumberOfOwners + '_' + #filter.maxNumberOfOwners + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getVehicleCount(ProductFilterDTO filter) {
        return productService.countVehiclesWithFilters(filter);
    }

    // ==================== УСЛУГИ (SERVICES) ====================

    /**
     * Получает страницу услуг с фильтрацией и пагинацией.
     * Результат кэшируется.
     *
     * @param category   категория услуги
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по цене
     * @param page       номер страницы
     * @param size       размер страницы
     * @return Page с услугами
     */
    @Cacheable(value = "servicesPage",
            key = "#category + '_' + #search + '_' + #region + '_' + #city + '_' + #sort + '_' + #priceSort + '_' + #page + '_' + #size",
            unless = "#result == null or #result.isEmpty()")
    public Page<PostTextJob> getServicesPage(String category, String search, String region, String city,
                                             String sort, String priceSort, int page, int size) {
        log.info("CACHE MISS for services page with filters");

        Pageable pageable = createPageable(page, size, sort, priceSort, "service");
        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedRegion = normalizeParam(region);
        String normalizedCity = normalizeParam(city);

        // Получаем общее количество
        long total;
        Slice<Long> idsSlice;

        if ("other_none".equals(city)) {
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            total = postRepoJob.countByOtherCities(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    standardCities
            );

            if (total == 0) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            idsSlice = postRepoJob.findPostJobIdsByOtherCitiesSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    standardCities,
                    pageable
            );
        } else {
            total = postRepoJob.countWithAllFilters(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    normalizedCity
            );

            if (total == 0) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            idsSlice = postRepoJob.findPostJobIdsWithFiltersSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    normalizedCity,
                    pageable
            );
        }

        if (idsSlice.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        // Загружаем услуги с изображениями по ID
        List<PostTextJob> services = postRepoJob.findPostsByIdsWithImages(idsSlice.getContent());

        // Сохраняем порядок
        Map<Long, PostTextJob> serviceMap = services.stream()
                .collect(Collectors.toMap(PostTextJob::getId, Function.identity()));

        List<PostTextJob> orderedServices = idsSlice.getContent().stream()
                .map(serviceMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedServices, pageable, total);
    }

    /**
     * Получает список услуг с фильтрацией и пагинацией.
     * Результат кэшируется.
     *
     * @param category   категория услуги
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по цене
     * @param page       номер страницы
     * @param size       размер страницы
     * @return List услуг
     */
    @Cacheable(value = "servicesList",
            key = "#category + '_' + #search + '_' + #region + '_' + #city + '_' + #sort + '_' + #priceSort + '_' + #page + '_' + #size")
    public List<PostTextJob> getServicesList(String category, String search, String region, String city,
                                             String sort, String priceSort, int page, int size) {
        log.info("CACHE MISS for services list with filters");

        Pageable pageable = createPageable(page, size, sort, priceSort, "service");

        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedRegion = normalizeParam(region);
        String normalizedCity = normalizeParam(city);

        // Получаем ID с пагинацией
        Slice<Long> idsSlice;

        // ОБРАБОТКА "ДРУГИХ НАСЕЛЕННЫХ ПУНКТОВ"
        if ("other_none".equals(city)) {
            // Получаем стандартные города для региона
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);

            idsSlice = postRepoJob.findPostJobIdsByOtherCitiesSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    standardCities,
                    pageable
            );
        } else {
            idsSlice = postRepoJob.findPostJobIdsWithFiltersSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    normalizedCity,
                    pageable
            );
        }

        if (idsSlice.isEmpty()) {
            return new ArrayList<>();
        }

        // Загружаем услуги с изображениями по ID
        List<PostTextJob> services = postRepoJob.findPostsByIdsWithImages(idsSlice.getContent());

        // Сохраняем порядок
        Map<Long, PostTextJob> serviceMap = services.stream()
                .collect(Collectors.toMap(PostTextJob::getId, Function.identity()));

        List<PostTextJob> orderedServices = idsSlice.getContent().stream()
                .map(serviceMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Found {} services", orderedServices.size());
        return orderedServices;
    }

    /**
     * Получает количество услуг, соответствующих фильтрам.
     * Результат кэшируется.
     *
     * @param category категория
     * @param search   поисковый запрос
     * @param region   регион
     * @param city     город
     * @return количество услуг
     */
    @Cacheable(value = "servicesCount",
            key = "#category + '_' + #search + '_' + #region + '_' + #city",
            unless = "#result == null")
    public long getServicesCount(String category, String search, String region, String city) {
        log.info("CACHE MISS for services count with filters");

        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedRegion = normalizeParam(region);

        if ("other_none".equals(city)) {
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            return postRepoJob.countByOtherCities(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    standardCities
            );
        } else {
            String normalizedCity = normalizeParam(city);
            return postRepoJob.countWithAllFilters(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedRegion,
                    normalizedCity
            );
        }
    }

    // ==================== ВАКАНСИИ (VACANCIES) ====================

    /**
     * Получает страницу вакансий с фильтрацией и пагинацией.
     * Результат кэшируется.
     *
     * @param category   категория вакансии
     * @param experience опыт работы (может быть null)
     * @param workformat формат работы (может быть null)
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по зарплате ("price_asc", "price_desc")
     * @param page       номер страницы
     * @param size       размер страницы
     * @return Page с вакансиями
     */

    @Cacheable(value = "vacanciesPage",
            key = "#category + '_' + #experience + '_' + #workformat + '_' + #search + '_' + #region + '_' + #city + '_' + #sort + '_' + #priceSort + '_' + #page + '_' + #size",
            unless = "#result == null or #result.isEmpty()")
    public Page<Vacancy> getVacanciesPage(String category, String experience, String workformat, String search, String region, String city,
                                          String sort, String priceSort, int page, int size) {
        log.info("CACHE MISS for vacancies page with filters");

        Pageable pageable = createPageable(page, size, sort, priceSort, "vacancy");
        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedExperience = normalizeParam(experience);
        String normalizedWorkformat = normalizeParam(workformat);
        String normalizedRegion = normalizeParam(region);
        String normalizedCity = normalizeParam(city);

        long total;
        Slice<Long> idsSlice;

        if ("other_none".equals(city)) {
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            total = postVacancyRepository.countByOtherCities(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities
            );

            if (total == 0) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            idsSlice = postVacancyRepository.findVacancyIdsByOtherCitiesSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities,
                    pageable
            );
        } else {
            total = postVacancyRepository.countWithAllFilters(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity
            );

            if (total == 0) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            idsSlice = postVacancyRepository.findVacancyIdsWithFiltersSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity,
                    pageable
            );
        }

        if (idsSlice.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<Vacancy> vacancies = postVacancyRepository.findVacanciesByIdsWithImages(idsSlice.getContent());

        Map<Long, Vacancy> vacancyMap = vacancies.stream()
                .collect(Collectors.toMap(Vacancy::getId, Function.identity()));

        List<Vacancy> orderedVacancies = idsSlice.getContent().stream()
                .map(vacancyMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedVacancies, pageable, total);
    }

    /**
     * Получает список вакансий с фильтрацией и пагинацией.
     * Результат кэшируется.
     *
     * @param category   категория вакансии
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по зарплате
     * @param page       номер страницы
     * @param size       размер страницы
     * @return List вакансий
     */

    @Cacheable(value = "vacanciesList",
            key = "#category + '_' + #experience + '_' + #workformat + '_' + #search + '_' + #region + '_' + #city + '_' + #sort + '_' + #priceSort + '_' + #page + '_' + #size")
    public List<Vacancy> getVacanciesList(String category, String experience, String workformat, String search, String region, String city,
                                          String sort, String priceSort, int page, int size) {
        log.info("CACHE MISS for vacancies list with filters");

        // НОРМАЛИЗАЦИЯ ПАРАМЕТРОВ
        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedExperience = normalizeParam(experience);
        String normalizedWorkformat = normalizeParam(workformat);
        String normalizedRegion = normalizeParam(region);
        String normalizedCity = normalizeParam(city);

        Pageable pageable = createPageable(page, size, sort, priceSort, "vacancy");
        Slice<Long> idsSlice;

        // ОБРАБОТКА "ДРУГИХ НАСЕЛЕННЫХ ПУНКТОВ"
        if ("other_none".equals(city)) {
            // Получаем стандартные города для региона
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);

            idsSlice = postVacancyRepository.findVacancyIdsByOtherCitiesSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities,
                    pageable
            );
        } else {
            idsSlice = postVacancyRepository.findVacancyIdsWithFiltersSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity,
                    pageable
            );
        }

        if (idsSlice.isEmpty()) {
            return new ArrayList<>();
        }

        List<Vacancy> vacancies = postVacancyRepository.findVacanciesByIdsWithImages(idsSlice.getContent());

        // Сохраняем порядок
        Map<Long, Vacancy> vacancyMap = vacancies.stream()
                .collect(Collectors.toMap(Vacancy::getId, Function.identity()));

        List<Vacancy> orderedVacancies = idsSlice.getContent().stream()
                .map(vacancyMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Found {} vacancies", orderedVacancies.size());
        return orderedVacancies;
    }

    /**
     * Получает количество вакансий, соответствующих фильтрам.
     * Результат кэшируется.
     *
     * @param category категория
     * @param search   поисковый запрос
     * @param region   регион
     * @param city     город
     * @return количество вакансий
     */

    @Cacheable(value = "vacanciesCount",
            key = "#category + '_' + #experience + '_' + #search + '_' + #region + '_' + #city",
            unless = "#result == null")
    public long getVacanciesCount(String category, String experience, String workformat, String search, String region, String city) {
        log.info("CACHE MISS for vacancies count with filters");

        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedExperience = normalizeParam(experience);
        String normalizedWorkformat = normalizeParam(workformat);
        String normalizedRegion = normalizeParam(region);

        if ("other_none".equals(city)) {
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            return postVacancyRepository.countByOtherCities(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities
            );
        } else {
            String normalizedCity = normalizeParam(city);
            return postVacancyRepository.countWithAllFilters(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity
            );
        }
    }

    // ==================== РЕЗЮМЕ (RESUME) ====================

    /**
     * Получает страницу резюме с фильтрацией и пагинацией.
     * Результат кэшируется.
     *
     * @param category   категория резюме
     * @param experience опыт работы (может быть null)
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по зарплате
     * @param page       номер страницы
     * @param size       размер страницы
     * @return Page с резюме
     */
    @Cacheable(value = "resumePage",
            key = "#category + '_' + #experience + '_' + #workformat + '_' + #search + '_' + #region + '_' + #city + '_' + #sort + '_' + #priceSort + '_' + #page + '_' + #size",
            unless = "#result == null or #result.isEmpty()")
    public Page<Resume> getResumePage(String category, String experience, String workformat, String search, String region, String city,
                                      String sort, String priceSort, int page, int size) {
        log.info("CACHE MISS for resume page with filters");

        Pageable pageable = createPageable(page, size, sort, priceSort, "resume");
        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedExperience = normalizeParam(experience);
        String normalizedWorkformat = normalizeParam(workformat);
        String normalizedRegion = normalizeParam(region);
        String normalizedCity = normalizeParam(city);

        long total;
        Slice<Long> idsSlice;

        if ("other_none".equals(city)) {
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            total = postResumeRepository.countByOtherCities(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities
            );

            if (total == 0) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            idsSlice = postResumeRepository.findResIdsByOtherCitiesSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities,
                    pageable
            );
        } else {
            total = postResumeRepository.countWithAllFilters(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity
            );

            if (total == 0) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            idsSlice = postResumeRepository.findResIdsWithFiltersSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity,
                    pageable
            );
        }

        if (idsSlice.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, total);
        }

        List<Resume> res = postResumeRepository.findResumeByIdsWithImages(idsSlice.getContent());

        Map<Long, Resume> resMap = res.stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));

        List<Resume> orderedResume = idsSlice.getContent().stream()
                .map(resMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageImpl<>(orderedResume, pageable, total);
    }

    /**
     * Получает список резюме с фильтрацией и пагинацией.
     * Результат кэшируется.
     *
     * @param category   категория резюме
     * @param experience опыт работы
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @param sort       сортировка по дате
     * @param priceSort  сортировка по зарплате
     * @param page       номер страницы
     * @param size       размер страницы
     * @return List резюме
     */
    @Cacheable(value = "resumeList",
            key = "#category + '_' + #experience + '_' + #workformat + '_' + #search + '_' + #region + '_' + #city + '_' + #sort + '_' + #priceSort + '_' + #page + '_' + #size")
    public List<Resume> getResumeList(String category, String experience, String workformat, String search, String region, String city,
                                      String sort, String priceSort, int page, int size) {
        log.info("CACHE MISS for resume list with filters");

        // НОРМАЛИЗАЦИЯ ПАРАМЕТРОВ
        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedExperience = normalizeParam(experience);
        String normalizedWorkformat = normalizeParam(workformat);
        String normalizedRegion = normalizeParam(region);
        String normalizedCity = normalizeParam(city);

        Pageable pageable = createPageable(page, size, sort, priceSort, "resume");
        Slice<Long> idsSlice;

        // ОБРАБОТКА "ДРУГИХ НАСЕЛЕННЫХ ПУНКТОВ"
        if ("other_none".equals(city)) {
            // Получаем стандартные города для региона
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);

            idsSlice = postResumeRepository.findResIdsByOtherCitiesSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities,
                    pageable
            );
        } else {
            idsSlice = postResumeRepository.findResIdsWithFiltersSlice(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity,
                    pageable
            );
        }

        if (idsSlice.isEmpty()) {
            return new ArrayList<>();
        }

        List<Resume> res = postResumeRepository.findResumeByIdsWithImages(idsSlice.getContent());

        // Сохраняем порядок
        Map<Long, Resume> resMap = res.stream()
                .collect(Collectors.toMap(Resume::getId, Function.identity()));

        List<Resume> orderedResume = idsSlice.getContent().stream()
                .map(resMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.debug("Found {} resume", orderedResume.size());
        return orderedResume;
    }

    /**
     * Получает количество резюме, соответствующих фильтрам.
     * Результат кэшируется.
     *
     * @param category   категория
     * @param experience опыт работы
     * @param search     поисковый запрос
     * @param region     регион
     * @param city       город
     * @return количество резюме
     */
    @Cacheable(value = "resumeCount",
            key = "#category + '_' + #experience + '_' + #workformat + '_' + #search + '_' + #region + '_' + #city",
            unless = "#result == null")
    public long getResumeCount(String category, String experience, String workformat, String search, String region, String city) {
        log.info("CACHE MISS for resume count with filters");

        String normalizedSearch = normalizeParam(search);
        String normalizedCategory = normalizeParam(category);
        String normalizedExperience = normalizeParam(experience);
        String normalizedWorkformat = normalizeParam(workformat);
        String normalizedRegion = normalizeParam(region);

        if ("other_none".equals(city)) {
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            return postResumeRepository.countByOtherCities(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    standardCities
            );
        } else {
            String normalizedCity = normalizeParam(city);
            return postResumeRepository.countWithAllFilters(
                    normalizedSearch,
                    normalizedCategory,
                    normalizedExperience,
                    normalizedWorkformat,
                    normalizedRegion,
                    normalizedCity
            );
        }
    }

    // ==================== ОЧИСТКА КЭША ====================

    /**
     * Очищает все кэши публичных постов (страницы, списки и счётчики для всех типов).
     */
    @CacheEvict(value = {"productsPage", "servicesPage", "vacanciesPage", "resumePage",
            "productsCount", "servicesCount", "vacanciesCount", "resumeCount",
            "productsList", "servicesList", "vacanciesList", "resumeList"},
            allEntries = true)
    public void clearAllPostCaches() {
        log.info("All post caches cleared");
    }

    /**
     * Очищает кэши только для товаров.
     */
    @CacheEvict(value = {"productsPage", "productsCount", "productsList"}, allEntries = true)
    public void clearProductsCache() {
        log.info("Products cache cleared");
    }

    /**
     * Очищает кэши только для услуг.
     */
    @CacheEvict(value = {"servicesPage", "servicesCount", "servicesList"}, allEntries = true)
    public void clearServicesCache() {
        log.info("Services cache cleared");
    }

    /**
     * Очищает кэши только для вакансий.
     */
    @CacheEvict(value = {"vacanciesPage", "vacanciesCount", "vacanciesList"}, allEntries = true)
    public void clearVacanciesCache() {
        log.info("Vacancies cache cleared");
    }

    /**
     * Очищает кэши только для резюме.
     */
    @CacheEvict(value = {"resumePage", "resumeCount", "resumeList"}, allEntries = true)
    public void clearResumeCache() {
        log.info("Resume cache cleared");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Нормализует параметр фильтрации.
     * Пустые строки и значение "all" преобразуются в null.
     *
     * @param param исходный параметр
     * @return нормализованный параметр или null
     */
    private String normalizeParam(String param) {
        return (param != null && !param.trim().isEmpty() && !"all".equals(param)) ? param.trim() : null;
    }

    /**
     * Создаёт объект Pageable с сортировкой по дате и/или цене.
     *
     * @param page      номер страницы
     * @param size      размер страницы
     * @param sort      сортировка по дате ("oldest" - ASC, иначе DESC)
     * @param priceSort сортировка по цене ("price_asc", "price_desc")
     * @param type      тип сущности ("product", "service", "vacancy", "resume")
     * @return настроенный Pageable
     */
    // ===== ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ СОЗДАНИЯ PAGEABLE =====
    private Pageable createPageable(int page, int size, String sort, String priceSort, String type) {
        List<Sort.Order> orders = new ArrayList<>();

        // Сортировка по цене
        if ("price_asc".equals(priceSort)) {
            orders.add(Sort.Order.asc("price"));
        } else if ("price_desc".equals(priceSort)) {
            orders.add(Sort.Order.desc("price"));
        }

        // Сортировка по дате
        if ("oldest".equals(sort)) {
            orders.add(Sort.Order.asc("moderatedAt"));
        } else {
            orders.add(Sort.Order.desc("moderatedAt"));
        }

        if (orders.isEmpty()) {
            orders.add(Sort.Order.desc("moderatedAt"));
        }

        return PageRequest.of(page, size, Sort.by(orders));
    }


    /**
     * Очищает кэш главной страницы
     */
    @CacheEvict(value = "index", allEntries = true)
    public void clearIndexCache() {
        log.debug("Очищен кэш главной страницы");
    }
}