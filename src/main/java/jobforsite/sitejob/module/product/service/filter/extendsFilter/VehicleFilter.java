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
 * Фильтр категории "motor" (Транспорт).
 * Обрабатывает фильтрацию по типу транспорта, происхождению, году выпуска,
 * пробегу, количеству владельцев, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("motor")
@RequiredArgsConstructor
public class VehicleFilter implements ProductFilter {

    private final PostCacheService postCacheService;
    private final FilterRangeService filterRangeService;

    @Override
    public String getCategoryName() {
        return "motor";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO vehicleFilter = buildVehicleFilter(filter);
        return postCacheService.getVehicleList(vehicleFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO vehicleFilter = buildVehicleFilter(filter);
        return postCacheService.getVehicleCount(vehicleFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("vehicleType", filter.getVehicleType());
        model.addAttribute("vehicleOrigin", filter.getVehicleOrigin());
        model.addAttribute("vehicleYear", filter.getVehicleYear());
        model.addAttribute("mileage", filter.getMileage());
        model.addAttribute("numberOfOwners", filter.getNumberOfOwners());
    }

    private ProductFilterDTO buildVehicleFilter(ProductFilterDTO filter) {
        ProductFilterDTO vehicleFilter = new ProductFilterDTO();
        vehicleFilter.setVehicleType(filter.getVehicleType());
        vehicleFilter.setVehicleOrigin(filter.getVehicleOrigin());
        vehicleFilter.setRegion(filter.getRegion());
        vehicleFilter.setCity(filter.getCity());
        vehicleFilter.setSearch(filter.getSearch());
        vehicleFilter.setCategory(filter.getCategory());
        vehicleFilter.setSort(filter.getSort());
        vehicleFilter.setPriceSort(filter.getPriceSort());

        RangeFilter vehicleYearRange = filterRangeService.getVehicleYearRangeForFilter(filter.getVehicleYear());
        vehicleFilter.setMinYear(vehicleYearRange.getMin());
        vehicleFilter.setMaxYear(vehicleYearRange.getMax());

        RangeFilter mileageRange = filterRangeService.getMileageRangeForFilter(filter.getMileage());
        vehicleFilter.setMinMileage(mileageRange.getMin());
        vehicleFilter.setMaxMileage(mileageRange.getMax());

        RangeFilter numberOfOwnersRange = filterRangeService.getNumberOfOwnersRangeForFilter(filter.getNumberOfOwners());
        vehicleFilter.setMinNumberOfOwners(numberOfOwnersRange.getMin());
        vehicleFilter.setMaxNumberOfOwners(numberOfOwnersRange.getMax());

        return vehicleFilter;
    }
}