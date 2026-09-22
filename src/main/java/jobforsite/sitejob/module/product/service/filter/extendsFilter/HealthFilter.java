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
 * Фильтр категории "health" (Здоровье и красота).
 * Обрабатывает фильтрацию по виду, типу, полу, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("health")
@RequiredArgsConstructor
public class HealthFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "health";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO healthFilter = buildHealthFilter(filter);
        return extendsPostCacheService.getHealthList(healthFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO healthFilter = buildHealthFilter(filter);
        return extendsPostCacheService.getHealthCount(healthFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("healthKind", filter.getHealthKind());
        model.addAttribute("healthType", filter.getHealthType());
        model.addAttribute("genderHealth", filter.getGenderHealth());
    }

    private ProductFilterDTO buildHealthFilter(ProductFilterDTO filter) {
        ProductFilterDTO healthFilter = new ProductFilterDTO();
        healthFilter.setHealthKind(filter.getHealthKind());
        healthFilter.setHealthType(filter.getHealthType());
        healthFilter.setGenderHealth(filter.getGenderHealth());
        healthFilter.setRegion(filter.getRegion());
        healthFilter.setCity(filter.getCity());
        healthFilter.setSearch(filter.getSearch());
        healthFilter.setCategory(filter.getCategory());
        healthFilter.setSort(filter.getSort());
        healthFilter.setPriceSort(filter.getPriceSort());

        return healthFilter;
    }
}