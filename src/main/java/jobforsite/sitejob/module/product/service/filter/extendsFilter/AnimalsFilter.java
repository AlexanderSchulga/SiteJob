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
 * Фильтр категории "animals" (Флора и фауна).
 * Обрабатывает фильтрацию по виду животных, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("animals")
@RequiredArgsConstructor
public class AnimalsFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "animals";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO animalsFilter = buildAnimalsFilter(filter);
        return extendsPostCacheService.getAnimalsList(animalsFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO animalsFilter = buildAnimalsFilter(filter);
        return extendsPostCacheService.getAnimalsCount(animalsFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("animalsKind", filter.getAnimalsKind());
    }

    private ProductFilterDTO buildAnimalsFilter(ProductFilterDTO filter) {
        ProductFilterDTO animalsFilter = new ProductFilterDTO();
        animalsFilter.setAnimalsKind(filter.getAnimalsKind());
        animalsFilter.setRegion(filter.getRegion());
        animalsFilter.setCity(filter.getCity());
        animalsFilter.setSearch(filter.getSearch());
        animalsFilter.setCategory(filter.getCategory());
        animalsFilter.setSort(filter.getSort());
        animalsFilter.setPriceSort(filter.getPriceSort());

        return animalsFilter;
    }
}