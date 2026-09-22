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
 * Фильтр категории "interior" (Дом и интерьер).
 * Обрабатывает фильтрацию по типу интерьера, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("interior")
@RequiredArgsConstructor
public class InteriorFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "interior";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO interiorFilter = buildInteriorFilter(filter);
        return extendsPostCacheService.getInteriorList(interiorFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO interiorFilter = buildInteriorFilter(filter);
        return extendsPostCacheService.getInteriorCount(interiorFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("interiorType", filter.getInteriorType());
        model.addAttribute("conditionInterior", filter.getConditionInterior());
    }

    private ProductFilterDTO buildInteriorFilter(ProductFilterDTO filter) {
        ProductFilterDTO interiorFilter = new ProductFilterDTO();
        interiorFilter.setInteriorType(filter.getInteriorType());
        interiorFilter.setConditionInterior(filter.getConditionInterior());
        interiorFilter.setRegion(filter.getRegion());
        interiorFilter.setCity(filter.getCity());
        interiorFilter.setSearch(filter.getSearch());
        interiorFilter.setCategory(filter.getCategory());
        interiorFilter.setSort(filter.getSort());
        interiorFilter.setPriceSort(filter.getPriceSort());

        return interiorFilter;
    }
}