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
 * Фильтр категории "sports" (Товары для спорта).
 * Обрабатывает фильтрацию по виду, типу, полу, состоянию, региону, городу, поиску и сортировке.
 */
@Slf4j
@Component("sports")
@RequiredArgsConstructor
public class SportsFilter implements ProductFilter {

    private final ExtendsPostCacheService extendsPostCacheService;

    @Override
    public String getCategoryName() {
        return "sports";
    }

    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        ProductFilterDTO sportsFilter = buildSportsFilter(filter);
        return extendsPostCacheService.getSportsList(sportsFilter, page, size);
    }

    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        ProductFilterDTO sportsFilter = buildSportsFilter(filter);
        return extendsPostCacheService.getSportsCount(sportsFilter);
    }

    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        model.addAttribute("sportsKind", filter.getSportsKind());
        model.addAttribute("sportsType", filter.getSportsType());
        model.addAttribute("genderSports", filter.getGenderSports());
        model.addAttribute("conditionSports", filter.getConditionSports());
    }

    private ProductFilterDTO buildSportsFilter(ProductFilterDTO filter) {
        ProductFilterDTO sportsFilter = new ProductFilterDTO();
        sportsFilter.setSportsKind(filter.getSportsKind());
        sportsFilter.setSportsType(filter.getSportsType());
        sportsFilter.setGenderSports(filter.getGenderSports());
        sportsFilter.setConditionSports(filter.getConditionSports());
        sportsFilter.setRegion(filter.getRegion());
        sportsFilter.setCity(filter.getCity());
        sportsFilter.setSearch(filter.getSearch());
        sportsFilter.setCategory(filter.getCategory());
        sportsFilter.setSort(filter.getSort());
        sportsFilter.setPriceSort(filter.getPriceSort());

        return sportsFilter;
    }
}