package jobforsite.sitejob.module.product.service.filter.extendsFilter;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.dto.RangeFilter;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.service.FilterRangeService;
import jobforsite.sitejob.module.product.service.filter.ProductFilter;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Фильтр категории "realty" (Недвижимость).
 * Обрабатывает фильтрацию по типу недвижимости (квартира, дом, участок, гараж,
 * коммерческая), количеству комнат, этажам, году постройки, площади, материалам,
 * ремонту, удалённости от города, а также по региону, городу, поиску, цене и сортировке.
 */
@Slf4j
@Component("realty")
@RequiredArgsConstructor
public class RealtyFilter implements ProductFilter {

    private final PostCacheService postCacheService;
    private final FilterRangeService filterRangeService;

    @Override
    public String getCategoryName() {
        return "realty";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO realtyFilter = buildRealtyFilter(filter);
        return postCacheService.getRealtyList(realtyFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO realtyFilter = buildRealtyFilter(filter);
        return postCacheService.getRealtyCount(realtyFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("propertyType", filter.getPropertyType());
        model.addAttribute("roomsCount", filter.getRoomsCount());
        model.addAttribute("floorsApartment", filter.getFloorsApartment());
        model.addAttribute("floorsCount", filter.getFloorsCount());
        model.addAttribute("floorsCountHouse", filter.getFloorsCountHouse());
        model.addAttribute("hasElevator", filter.getHasElevator());
        model.addAttribute("apartmentBuildYear", filter.getApartmentBuildYear());
        model.addAttribute("houseBuildYear", filter.getHouseBuildYear());
        model.addAttribute("houseArea", filter.getHouseArea());
        model.addAttribute("landArea", filter.getLandArea());
        model.addAttribute("distanceToCity", filter.getDistanceToCity());
        model.addAttribute("materialUp", filter.getMaterialUp());
        model.addAttribute("repairUp", filter.getRepairUp());
        model.addAttribute("materialHouse", filter.getMaterialHouse());
        model.addAttribute("repairHouse", filter.getRepairHouse());
        model.addAttribute("distanceToCityHouse", filter.getDistanceToCityHouse());
        model.addAttribute("landAreaGarage", filter.getLandAreaGarage());
        model.addAttribute("landAreaCommercial", filter.getLandAreaCommercial());
    }

    private ProductFilterDTO buildRealtyFilter(ProductFilterDTO filter) {
        ProductFilterDTO realtyFilter = new ProductFilterDTO();

        // Базовые поля
        realtyFilter.setRegion(filter.getRegion());
        realtyFilter.setCity(filter.getCity());
        realtyFilter.setSearch(filter.getSearch());
        realtyFilter.setCategory(filter.getCategory());
        realtyFilter.setSort(filter.getSort());
        realtyFilter.setPriceSort(filter.getPriceSort());
        realtyFilter.setMinPrice(filter.getMinPrice());
        realtyFilter.setMaxPrice(filter.getMaxPrice());

        // ===== ОБЩИЕ ПОЛЯ =====
        realtyFilter.setPropertyType(filter.getPropertyType());
        realtyFilter.setRoomsCount(filter.getRoomsCount());
        realtyFilter.setHasElevator(filter.getHasElevator());

        // ===== ДЛЯ КВАРТИРЫ =====
        realtyFilter.setFloorsApartment(filter.getFloorsApartment());
        realtyFilter.setFloorsCount(filter.getFloorsCount());
        realtyFilter.setApartmentBuildYear(filter.getApartmentBuildYear());
        realtyFilter.setMaterialUp(filter.getMaterialUp());
        realtyFilter.setRepairUp(filter.getRepairUp());

        // ===== ДЛЯ ДОМА =====
        realtyFilter.setFloorsCountHouse(filter.getFloorsCountHouse());
        realtyFilter.setHouseArea(filter.getHouseArea());
        realtyFilter.setLandArea(filter.getLandArea());
        realtyFilter.setHouseBuildYear(filter.getHouseBuildYear());
        realtyFilter.setDistanceToCityHouse(filter.getDistanceToCityHouse());
        realtyFilter.setMaterialHouse(filter.getMaterialHouse());
        realtyFilter.setRepairHouse(filter.getRepairHouse());

        // ===== ДЛЯ УЧАСТКА =====
        realtyFilter.setLandArea(filter.getLandArea());
        realtyFilter.setDistanceToCity(filter.getDistanceToCity());

        // ===== ДЛЯ ГАРАЖА =====
        realtyFilter.setLandAreaGarage(filter.getLandAreaGarage());

        // ===== ДЛЯ КОММЕРЧЕСКОЙ =====
        realtyFilter.setLandAreaCommercial(filter.getLandAreaCommercial());

        // Год постройки
        Integer buildYear = null;
        if ("apartment".equals(filter.getPropertyType())) {
            buildYear = filter.getApartmentBuildYear();
        } else if ("house".equals(filter.getPropertyType())) {
            buildYear = filter.getHouseBuildYear();
        }

        applyRanges(realtyFilter, buildYear);

        return realtyFilter;
    }

    private void applyRanges(ProductFilterDTO realtyFilter, Integer buildYear) {
        RangeFilter floorsApartmentRange = filterRangeService.getFloorsApartmentRangeForFilter(realtyFilter.getFloorsApartment());
        realtyFilter.setMinFloorsApartment(floorsApartmentRange.getMin());
        realtyFilter.setMaxFloorsApartment(floorsApartmentRange.getMax());

        RangeFilter floorsCountRange = filterRangeService.getFloorsCountRangeForFilter(realtyFilter.getFloorsCount());
        realtyFilter.setFloorsCountMin(floorsCountRange.getMin());
        realtyFilter.setFloorsCountMax(floorsCountRange.getMax());

        RangeFilter floorsCountHouseRange = filterRangeService.getFloorsCountHouseRangeForFilter(realtyFilter.getFloorsCountHouse());
        realtyFilter.setFloorsCountHouseMin(floorsCountHouseRange.getMin());
        realtyFilter.setFloorsCountHouseMax(floorsCountHouseRange.getMax());

        RangeFilter buildYearRange = filterRangeService.getBuildYearRangeForFilter(buildYear);
        realtyFilter.setMinBuildYear(buildYearRange.getMin());
        realtyFilter.setMaxBuildYear(buildYearRange.getMax());

        RangeFilter houseAreaRange = filterRangeService.getHouseAreaRangeForFilter(realtyFilter.getHouseArea());
        realtyFilter.setMinHouseArea(houseAreaRange.getMin());
        realtyFilter.setMaxHouseArea(houseAreaRange.getMax());

        RangeFilter landAreaRange = filterRangeService.getLandAreaRangeForFilter(realtyFilter.getLandArea(), realtyFilter.getPropertyType());
        realtyFilter.setMinLandArea(landAreaRange.getMin());
        realtyFilter.setMaxLandArea(landAreaRange.getMax());

        RangeFilter distanceToCityRange = filterRangeService.getDistanceToCityRangeForFilter(realtyFilter.getDistanceToCity());
        realtyFilter.setMinDistanceToCity(distanceToCityRange.getMin());
        realtyFilter.setMaxDistanceToCity(distanceToCityRange.getMax());

        RangeFilter distanceToCityHouseRange = filterRangeService.getDistanceToCityHouseRangeForFilter(realtyFilter.getDistanceToCityHouse());
        realtyFilter.setMinDistanceToCityHouse(distanceToCityHouseRange.getMin());
        realtyFilter.setMaxDistanceToCityHouse(distanceToCityHouseRange.getMax());


        RangeFilter landAreaGarageRange = filterRangeService.getLandAreaGarageRangeForFilter(realtyFilter.getLandAreaGarage());
        realtyFilter.setMinLandAreaGarage(landAreaGarageRange.getMin());
        realtyFilter.setMaxLandAreaGarage(landAreaGarageRange.getMax());

        RangeFilter landAreaCommercialRange = filterRangeService.getLandAreaCommercialRangeForFilter(realtyFilter.getLandAreaCommercial());
        realtyFilter.setMinLandAreaCommercial(landAreaCommercialRange.getMin());
        realtyFilter.setMaxLandAreaCommercial(landAreaCommercialRange.getMax());


    }
}