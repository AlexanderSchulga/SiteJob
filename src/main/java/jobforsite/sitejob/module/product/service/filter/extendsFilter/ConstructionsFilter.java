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
 * Фильтр категории "constructions" (Строительные материалы).
 * Обрабатывает фильтрацию по типу материалов, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("constructions")
@RequiredArgsConstructor
public class ConstructionsFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "constructions";
    }
    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO constructionsFilter = buildConstructionsFilter(filter);
        return extendsPostCacheService.getConstructionsList(constructionsFilter, page, size);
    }
    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO constructionsFilter = buildConstructionsFilter(filter);
        return extendsPostCacheService.getConstructionsCount(constructionsFilter);
    }
    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("constructionsType", filter.getConstructionsType());
    }
    private ProductFilterDTO buildConstructionsFilter(ProductFilterDTO filter) {
        ProductFilterDTO constructionsFilter = new ProductFilterDTO();
        constructionsFilter.setConstructionsType(filter.getConstructionsType());
        constructionsFilter.setRegion(filter.getRegion());
        constructionsFilter.setCity(filter.getCity());
        constructionsFilter.setSearch(filter.getSearch());
        constructionsFilter.setCategory(filter.getCategory());
        constructionsFilter.setSort(filter.getSort());
        constructionsFilter.setPriceSort(filter.getPriceSort());

        return constructionsFilter;
    }


}
