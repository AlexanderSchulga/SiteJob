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
 * Фильтр категории "tools" (Инструменты).
 * Обрабатывает фильтрацию по типу инструментов, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("tools")
@RequiredArgsConstructor
public class ToolsFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "tools";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO toolsFilter = buildToolsFilter(filter);
        return extendsPostCacheService.getToolsList(toolsFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO toolsFilter = buildToolsFilter(filter);
        return extendsPostCacheService.getToolsCount(toolsFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("toolsType", filter.getToolsType());
        model.addAttribute("conditionTools", filter.getConditionTools());
    }

    private ProductFilterDTO buildToolsFilter(ProductFilterDTO filter) {
        ProductFilterDTO toolsFilter = new ProductFilterDTO();
        toolsFilter.setToolsType(filter.getToolsType());
        toolsFilter.setConditionTools(filter.getConditionTools());
        toolsFilter.setRegion(filter.getRegion());
        toolsFilter.setCity(filter.getCity());
        toolsFilter.setSearch(filter.getSearch());
        toolsFilter.setCategory(filter.getCategory());
        toolsFilter.setSort(filter.getSort());
        toolsFilter.setPriceSort(filter.getPriceSort());

        return toolsFilter;
    }
}