package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductFilterMapper {

    /**
     * Собирает ProductFilterDTO из параметров запроса.
     * Аналог PostCreateRequestMapper.toCreationData()
     */
    public ProductFilterDTO toFilterDTO(
            String category,
            String search,
            String sort,
            String priceSort,
            String region,
            String city,
            String propertyType,
            Integer roomsCount,
            Integer floorsApartment,
            Integer floorsCount,
            Integer apartmentBuildYear,
            Integer floorsCountHouse,
            Integer houseBuildYear,
            Boolean hasElevator,
            Integer houseArea,
            Integer landArea,
            Integer distanceToCity,
            String vehicleType,
            String vehicleOrigin,
            Integer vehicleYear,

           String materialUp,
            String repairUp,
            String materialHouse,
            String repairHouse,
            Integer distanceToCityHouse,
            Integer landAreaGarage,
            Integer landAreaCommercial,

            Integer mileage,
            Integer numberOfOwners,

            // ===== Автозапчасти =====
            String autoType,
            String conditionAuto,

            // ===== Строительные материалы =====
            String constructionsType,

            // ===== Дом и интерьер =====
            String interiorType,
            String conditionInterior,

            // ===== Инструменты =====
            String toolsType,
            String conditionTools,

            // ===== Бытовая техника =====
            String appliancesType,
            String conditionAppliances,

            // ===== Электроника и гаджеты =====
            String electronicsType,
            String conditionElectronics,

            // ===== Одежда и обувь =====
            String clothingType,
            String genderClothing,
            String conditionClothing,

            // ===== Детские товары =====
            String genderChildrenPr,
            String conditionChildrenPr,

            // ===== Товары для спорта =====
            String sportsKind,
            String sportsType,
            String genderSports,
            String conditionSports,

            // ===== Продукты питания =====
            String foodKind,

            // ===== Здоровье и красота =====
            String healthKind,
            String healthType,
            String genderHealth,

            // ===== Флора и фауна =====
            String animalsKind,


            int page,
            int size) {

        ProductFilterDTO filter = new ProductFilterDTO();

        // Базовые фильтры
        filter.setCategory(category);
        filter.setSearch(search);
        filter.setSort(sort);
        filter.setPriceSort(priceSort);
        filter.setRegion(region);
        filter.setCity(city);

        // Фильтры недвижимости
        filter.setPropertyType(propertyType);
        filter.setRoomsCount(roomsCount);
        filter.setFloorsApartment(floorsApartment);
        filter.setFloorsCount(floorsCount);
        filter.setApartmentBuildYear(apartmentBuildYear);
        filter.setFloorsCountHouse(floorsCountHouse);
        filter.setHouseBuildYear(houseBuildYear);
        filter.setHasElevator(hasElevator);
        filter.setHouseArea(houseArea);
        filter.setLandArea(landArea);
        filter.setDistanceToCity(distanceToCity);

        filter.setMaterialUp(materialUp);
        filter.setRepairUp(repairUp);
        filter.setMaterialHouse(materialHouse);
        filter.setRepairHouse(repairHouse);
        filter.setDistanceToCityHouse(distanceToCityHouse);
        filter.setLandAreaGarage(landAreaGarage);
        filter.setLandAreaCommercial(landAreaCommercial);

        // Фильтры автотранспорта
        filter.setVehicleType(vehicleType);
        filter.setVehicleOrigin(vehicleOrigin);
        filter.setVehicleYear(vehicleYear);
        filter.setMileage(mileage);
        filter.setNumberOfOwners(numberOfOwners);

        // ===== Автозапчасти =====
        filter.setAutoType(autoType);
        filter.setConditionAuto(conditionAuto);

        // ===== Строительные материалы =====
        filter.setConstructionsType(constructionsType);

        // ===== Дом и интерьер =====
        filter.setInteriorType(interiorType);
        filter.setConditionInterior(conditionInterior);

        // ===== Инструменты =====
        filter.setToolsType(toolsType);
        filter.setConditionTools(conditionTools);

        // ===== Бытовая техника =====
        filter.setAppliancesType(appliancesType);
        filter.setConditionAppliances(conditionAppliances);

        // ===== Электроника и гаджеты =====
        filter.setElectronicsType(electronicsType);
        filter.setConditionElectronics(conditionElectronics);

        // ===== Одежда и обувь =====
        filter.setClothingType(clothingType);
        filter.setGenderClothing(genderClothing);
        filter.setConditionClothing(conditionClothing);

        // ===== Детские товары =====
        filter.setGenderChildrenPr(genderChildrenPr);
        filter.setConditionChildrenPr(conditionChildrenPr);

        // ===== Товары для спорта =====
        filter.setSportsKind(sportsKind);
        filter.setSportsType(sportsType);
        filter.setGenderSports(genderSports);
        filter.setConditionSports(conditionSports);

        // ===== Продукты питания =====
        filter.setFoodKind(foodKind);

        // ===== Здоровье и красота =====
        filter.setHealthKind(healthKind);
        filter.setHealthType(healthType);
        filter.setGenderHealth(genderHealth);

        // ===== Флора и фауна =====
        filter.setAnimalsKind(animalsKind);


        // Пагинация
        filter.setPage(page);
        filter.setSize(size);

        return filter;
    }

    /**
     * Нормализует параметры фильтрации (пустые строки и "all" -> null).
     * Как в RefController.
     */
    public ProductFilterDTO normalize(ProductFilterDTO filter) {
        if (filter == null) return null;

        String normalizedCategory = normalizeParam(filter.getCategory());
        String normalizedSearch = normalizeParam(filter.getSearch());
        String normalizedRegion = normalizeParam(filter.getRegion());
        String normalizedCity = normalizeParam(filter.getCity());

        filter.setCategory(normalizedCategory);
        filter.setSearch(normalizedSearch);
        filter.setRegion(normalizedRegion);
        filter.setCity(normalizedCity);

        // Сортировка по умолчанию
        if (filter.getSort() == null) {
            filter.setSort("newest");
        }
        if (filter.getPriceSort() == null) {
            filter.setPriceSort("none");
        }

        return filter;
    }

    private String normalizeParam(String param) {
        return (param != null && !param.trim().isEmpty() && !"all".equals(param))
                ? param.trim()
                : null;
    }
}