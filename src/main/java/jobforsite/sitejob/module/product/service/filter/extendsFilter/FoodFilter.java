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
 * Фильтр категории "food" (Продукты питания).
 * Обрабатывает фильтрацию по виду продуктов, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("food")
@RequiredArgsConstructor
public class FoodFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "food";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO foodFilter = buildFoodFilter(filter);
        return extendsPostCacheService.getFoodList(foodFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO foodFilter = buildFoodFilter(filter);
        return extendsPostCacheService.getFoodCount(foodFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("foodKind", filter.getFoodKind());
    }

    private ProductFilterDTO buildFoodFilter(ProductFilterDTO filter) {
        ProductFilterDTO foodFilter = new ProductFilterDTO();
        foodFilter.setFoodKind(filter.getFoodKind());
        foodFilter.setRegion(filter.getRegion());
        foodFilter.setCity(filter.getCity());
        foodFilter.setSearch(filter.getSearch());
        foodFilter.setCategory(filter.getCategory());
        foodFilter.setSort(filter.getSort());
        foodFilter.setPriceSort(filter.getPriceSort());

        return foodFilter;
    }
}