package jobforsite.sitejob.module.product.service.filter.extendsFilter;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.service.filter.ProductFilter;
import jobforsite.sitejob.module.shared.services.cachrateservice.ExtendsPostCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;


/**
 * Фильтр категории "appliances" (Бытовая техника).
 * Обрабатывает фильтрацию по типу техники, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("appliances")
@RequiredArgsConstructor
public class AppliancesFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "appliances";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO appliancesFilter = buildAppliancesFilter(filter);
        return extendsPostCacheService.getAppliancesList(appliancesFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO appliancesFilter = buildAppliancesFilter(filter);
        return extendsPostCacheService.getAppliancesCount(appliancesFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("appliancesType", filter.getAppliancesType());
        model.addAttribute("conditionAppliances", filter.getConditionAppliances());
    }

    private ProductFilterDTO buildAppliancesFilter(ProductFilterDTO filter) {
        ProductFilterDTO appliancesFilter = new ProductFilterDTO();
        appliancesFilter.setAppliancesType(filter.getAppliancesType());
        appliancesFilter.setConditionAppliances(filter.getConditionAppliances());
        appliancesFilter.setRegion(filter.getRegion());
        appliancesFilter.setCity(filter.getCity());
        appliancesFilter.setSearch(filter.getSearch());
        appliancesFilter.setCategory(filter.getCategory());
        appliancesFilter.setSort(filter.getSort());
        appliancesFilter.setPriceSort(filter.getPriceSort());

        return appliancesFilter;
    }
}