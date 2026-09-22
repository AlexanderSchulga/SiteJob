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
 * Фильтр категории "electronics" (Электроника и гаджеты).
 * Обрабатывает фильтрацию по типу электроники, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("electronics")
@RequiredArgsConstructor
public class ElectronicsFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "electronics";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO electronicsFilter = buildElectronicsFilter(filter);
        return extendsPostCacheService.getElectronicsList(electronicsFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO electronicsFilter = buildElectronicsFilter(filter);
        return extendsPostCacheService.getElectronicsCount(electronicsFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("electronicsType", filter.getElectronicsType());
        model.addAttribute("conditionElectronics", filter.getConditionElectronics());
    }

    private ProductFilterDTO buildElectronicsFilter(ProductFilterDTO filter) {
        ProductFilterDTO electronicsFilter = new ProductFilterDTO();
        electronicsFilter.setElectronicsType(filter.getElectronicsType());
        electronicsFilter.setConditionElectronics(filter.getConditionElectronics());
        electronicsFilter.setRegion(filter.getRegion());
        electronicsFilter.setCity(filter.getCity());
        electronicsFilter.setSearch(filter.getSearch());
        electronicsFilter.setCategory(filter.getCategory());
        electronicsFilter.setSort(filter.getSort());
        electronicsFilter.setPriceSort(filter.getPriceSort());

        return electronicsFilter;
    }
}