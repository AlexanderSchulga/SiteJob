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
 * Фильтр категории "childrenPr" (Детские товары).
 * Обрабатывает фильтрацию по полу, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("childrenPr")
@RequiredArgsConstructor
public class ChildrenPrFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "childrenPr";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO childrenPrFilter = buildChildrenPrFilter(filter);
        return extendsPostCacheService.getChildrenPrList(childrenPrFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO childrenPrFilter = buildChildrenPrFilter(filter);
        return extendsPostCacheService.getChildrenPrCount(childrenPrFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("genderChildrenPr", filter.getGenderChildrenPr());
        model.addAttribute("conditionChildrenPr", filter.getConditionChildrenPr());
    }

    private ProductFilterDTO buildChildrenPrFilter(ProductFilterDTO filter) {
        ProductFilterDTO childrenPrFilter = new ProductFilterDTO();
        childrenPrFilter.setGenderChildrenPr(filter.getGenderChildrenPr());
        childrenPrFilter.setConditionChildrenPr(filter.getConditionChildrenPr());
        childrenPrFilter.setRegion(filter.getRegion());
        childrenPrFilter.setCity(filter.getCity());
        childrenPrFilter.setSearch(filter.getSearch());
        childrenPrFilter.setCategory(filter.getCategory());
        childrenPrFilter.setSort(filter.getSort());
        childrenPrFilter.setPriceSort(filter.getPriceSort());

        return childrenPrFilter;
    }
}