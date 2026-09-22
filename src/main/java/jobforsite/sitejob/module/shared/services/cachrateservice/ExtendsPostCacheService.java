package jobforsite.sitejob.module.shared.services.cachrateservice;


import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.product.service.filter.ExtendsProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис кеширования списков и счётчиков расширенных категорий постов.
 * <p>
 * Предоставляет методы для получения списков объявлений и их количества
 * по различным категориям (авто, строительство, дом и интерьер, инструменты,
 * бытовая техника, электроника, одежда, детские товары, спорт, еда, здоровье,
 * животные и растения) с поддержкой фильтрации, сортировки и постраничной выдачи.
 * <p>
 * Результаты кешируются с помощью {@link Cacheable}, ключ формируется на основе
 * параметров фильтра, номера страницы и размера страницы.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ExtendsPostCacheService {

    /**
     * Сервис фильтрации расширенных товаров.
     */
    private final ExtendsProductService extendsProductService;

    // =====  Автозапчасти и автотовары =====

    /**
     * Возвращает список автотоваров и автозапчастей с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Авто"
     */
    @Cacheable(value = "autoList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.autoType + '_' + #filter.conditionAuto + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<AutoPostText> getAutoList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for auto list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterAutoWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество автотоваров и автозапчастей по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Авто"
     */
    @Cacheable(value = "autoCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.autoType + '_' + #filter.conditionAuto + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getAutoCount(ProductFilterDTO filter) {
        return extendsProductService.countAutoWithFilters(filter);
    }

    //-----------Строительные материалы----------//

    /**
     * Возвращает список строительных материалов с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Строительные материалы"
     */
    @Cacheable(value = "constructionsList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.constructionsType + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<ConstructionsPostText> getConstructionsList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for constructions list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterConstructionsWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество строительных материалов по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Строительные материалы"
     */
    @Cacheable(value = "constructionsCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.constructionsType + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getConstructionsCount(ProductFilterDTO filter) {
        return extendsProductService.countConstructionsWithFilters(filter);
    }

    //-----------------Дом и интерьер----------------------------//

    /**
     * Возвращает список товаров для дома и интерьера с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Дом и интерьер"
     */
    @Cacheable(value = "interiorList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.interiorType + '_' + #filter.conditionInterior + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<InteriorPostText> getInteriorList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for interior list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterInteriorWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество товаров для дома и интерьера по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Дом и интерьер"
     */
    @Cacheable(value = "interiorCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.interiorType + '_' + #filter.conditionInterior + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getInteriorCount(ProductFilterDTO filter) {
        return extendsProductService.countInteriorWithFilters(filter);
    }


    //-----------------Инструменты----------------------------//

    /**
     * Возвращает список инструментов с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Инструменты"
     */
    @Cacheable(value = "toolsList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.toolsType + '_' + #filter.conditionTools + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<ToolsPostText> getToolsList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for tools list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterToolsWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество инструментов по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Инструменты"
     */
    @Cacheable(value = "toolsCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.toolsType + '_' + #filter.conditionTools + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getToolsCount(ProductFilterDTO filter) {
        return extendsProductService.countToolsWithFilters(filter);
    }

    //-----------------Бытовая техника----------------------------//

    /**
     * Возвращает список бытовой техники с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Бытовая техника"
     */
    @Cacheable(value = "appliancesList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.appliancesType + '_' + #filter.conditionAppliances + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<AppliancesPostText> getAppliancesList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for appliances list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterAppliancesWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество бытовой техники по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Бытовая техника"
     */
    @Cacheable(value = "appliancesCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.appliancesType + '_' + #filter.conditionAppliances + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getAppliancesCount(ProductFilterDTO filter) {
        return extendsProductService.countAppliancesWithFilters(filter);
    }

    //-----------------Электроника и гаджеты----------------------------//

    /**
     * Возвращает список электроники и гаджетов с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Электроника и гаджеты"
     */
    @Cacheable(value = "electronicsList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.electronicsType + '_' + #filter.conditionElectronics + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<ElectronicsPostText> getElectronicsList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for electronics list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterElectronicsWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество электроники и гаджетов по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Электроника и гаджеты"
     */
    @Cacheable(value = "electronicsCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.electronicsType + '_' + #filter.conditionElectronics + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getElectronicsCount(ProductFilterDTO filter) {
        return extendsProductService.countElectronicsWithFilters(filter);
    }

    // ===== Одежда =====

    /**
     * Возвращает список одежды с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Одежда"
     */
    @Cacheable(value = "clothingList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.clothingType + '_' + #filter.genderClothing + '_' + #filter.conditionClothing + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<ClothingPostText> getClothingList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for clothing list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterClothingWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество одежды по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Одежда"
     */
    @Cacheable(value = "clothingCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.clothingType + '_' + #filter.genderClothing + '_' + #filter.conditionClothing + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getClothingCount(ProductFilterDTO filter) {
        return extendsProductService.countClothingWithFilters(filter);
    }

    // ===== Детские товары =====

    /**
     * Возвращает список детских товаров с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Детские товары"
     */
    @Cacheable(value = "childrenPrList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.genderChildrenPr + '_' + #filter.conditionChildrenPr + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<ChildrenPrPostText> getChildrenPrList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for children products list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterChildrenPrWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество детских товаров по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Детские товары"
     */
    @Cacheable(value = "childrenPrCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.genderChildrenPr + '_' + #filter.conditionChildrenPr + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getChildrenPrCount(ProductFilterDTO filter) {
        return extendsProductService.countChildrenPrWithFilters(filter);
    }

    // ===== Спорт и отдых =====

    /**
     * Возвращает список товаров для спорта и отдыха с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Спорт и отдых"
     */
    @Cacheable(value = "sportsList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.sportsKind + '_' + #filter.sportsType + '_' + #filter.genderSports + '_' + #filter.conditionSports + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<SportsPostText> getSportsList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for sports list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterSportsWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество товаров для спорта и отдыха по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Спорт и отдых"
     */
    @Cacheable(value = "sportsCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.sportsKind + '_' + #filter.sportsType + '_' + #filter.genderSports + '_' + #filter.conditionSports + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getSportsCount(ProductFilterDTO filter) {
        return extendsProductService.countSportsWithFilters(filter);
    }

    // ===== Продукты питания =====

    /**
     * Возвращает список продуктов питания с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Продукты питания"
     */
    @Cacheable(value = "foodList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.foodKind + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<FoodPostText> getFoodList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for food list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterFoodWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество продуктов питания по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Продукты питания"
     */
    @Cacheable(value = "foodCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.foodKind + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getFoodCount(ProductFilterDTO filter) {
        return extendsProductService.countFoodWithFilters(filter);
    }

    // ===== Здоровье и медицина =====

    /**
     * Возвращает список товаров для здоровья и медицины с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Здоровье и медицина"
     */
    @Cacheable(value = "healthList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.healthKind + '_' + #filter.healthType + '_' + #filter.genderHealth + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<HealthPostText> getHealthList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for health list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterHealthWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество товаров для здоровья и медицины по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Здоровье и медицина"
     */
    @Cacheable(value = "healthCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.healthKind + '_' + #filter.healthType + '_' + #filter.genderHealth + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getHealthCount(ProductFilterDTO filter) {
        return extendsProductService.countHealthWithFilters(filter);
    }

    // ===== Животные и растения =====

    /**
     * Возвращает список товаров для животных и растений с учётом фильтра и сортировки.
     *
     * @param filter фильтр товаров
     * @param page   номер страницы
     * @param size   размер страницы
     * @return список постов категории "Животные и растения"
     */
    @Cacheable(value = "animalsList",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.animalsKind + '_' + " +
                    "#filter.region + '_' + #filter.city + '_' + #filter.sort + '_' + #filter.priceSort + '_' + " +
                    "#page + '_' + #size")
    public List<AnimalsPostText> getAnimalsList(ProductFilterDTO filter, int page, int size) {
        log.info("CACHE MISS for animals list with region and city");
        Pageable pageable = createPageable(page, size, filter.getSort(), filter.getPriceSort(), "product");
        return extendsProductService.filterAnimalsWithRegionAndSort(filter, pageable);
    }

    /**
     * Возвращает количество товаров для животных и растений по заданному фильтру.
     *
     * @param filter фильтр товаров
     * @return количество постов категории "Животные и растения"
     */
    @Cacheable(value = "animalsCount",
            key = "#filter.search + '_' + #filter.category + '_' + " +
                    "#filter.animalsKind + '_' + " +
                    "#filter.region + '_' + #filter.city")
    public long getAnimalsCount(ProductFilterDTO filter) {
        return extendsProductService.countAnimalsWithFilters(filter);
    }

    /**
     * Создаёт объект {@link Pageable} с сортировкой по дате и/или цене.
     *
     * @param page      номер страницы
     * @param size      размер страницы
     * @param sort      сортировка по дате ("oldest" — ASC, иначе DESC)
     * @param priceSort сортировка по цене ("price_asc", "price_desc")
     * @param type      тип сущности ("product")
     * @return настроенный {@link Pageable}
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

}