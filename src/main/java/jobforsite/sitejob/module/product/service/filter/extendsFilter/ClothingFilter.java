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
 * Фильтр категории "clothing" (Одежда и обувь).
 * Обрабатывает фильтрацию по типу одежды, полу, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("clothing")
@RequiredArgsConstructor
public class ClothingFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "clothing";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO clothingFilter = buildClothingFilter(filter);
        return extendsPostCacheService.getClothingList(clothingFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO clothingFilter = buildClothingFilter(filter);
        return extendsPostCacheService.getClothingCount(clothingFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("clothingType", filter.getClothingType());
        model.addAttribute("genderClothing", filter.getGenderClothing());
        model.addAttribute("conditionClothing", filter.getConditionClothing());
    }

    private ProductFilterDTO buildClothingFilter(ProductFilterDTO filter) {
        ProductFilterDTO clothingFilter = new ProductFilterDTO();
        clothingFilter.setClothingType(filter.getClothingType());
        clothingFilter.setGenderClothing(filter.getGenderClothing());
        clothingFilter.setConditionClothing(filter.getConditionClothing());
        clothingFilter.setRegion(filter.getRegion());
        clothingFilter.setCity(filter.getCity());
        clothingFilter.setSearch(filter.getSearch());
        clothingFilter.setCategory(filter.getCategory());
        clothingFilter.setSort(filter.getSort());
        clothingFilter.setPriceSort(filter.getPriceSort());

        return clothingFilter;
    }
}