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
 * Фильтр категории "auto" (Автозапчасти и автотовары).
 * Обрабатывает фильтрацию по типу авто, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("auto")
@RequiredArgsConstructor
public class AutoFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "auto";
    }
    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO autoFilter = buildAutoFilter(filter);
        return extendsPostCacheService.getAutoList(autoFilter, page, size);
    }
    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO autoFilter = buildAutoFilter(filter);
        return extendsPostCacheService.getAutoCount(autoFilter);
    }
    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("autoType", filter.getAutoType());
        model.addAttribute("conditionAuto", filter.getConditionAuto());
    }
    private ProductFilterDTO buildAutoFilter(ProductFilterDTO filter) {
        ProductFilterDTO autoFilter = new ProductFilterDTO();
        autoFilter.setAutoType(filter.getAutoType());
        autoFilter.setConditionAuto(filter.getConditionAuto());
        autoFilter.setRegion(filter.getRegion());
        autoFilter.setCity(filter.getCity());
        autoFilter.setSearch(filter.getSearch());
        autoFilter.setCategory(filter.getCategory());
        autoFilter.setSort(filter.getSort());
        autoFilter.setPriceSort(filter.getPriceSort());

        return autoFilter;
    }


}
