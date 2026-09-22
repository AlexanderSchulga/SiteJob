package jobforsite.sitejob.module.product.service.filter;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Дефолтный фильтр для постов (товаров).
 * <p>
 * Реализует интерфейс {@link ProductFilter} для всех категорий,
 * у которых нет специализированного фильтра.
 * Используется как fallback при фильтрации постов.
 * </p>
 * <p>
 * Получает данные из кэша через {@link PostCacheService}
 * для оптимизации производительности.
 * </p>
 *
 * @see ProductFilter
 * @see PostCacheService
 */
@Slf4j
@Component("defaultFilter")
@RequiredArgsConstructor
public class DefaultFilter implements ProductFilter {

    private final PostCacheService postCacheService;

    /**
     * Возвращает название категории.
     *
     * @return "default"
     */
    @Override
    public String getCategoryName() {
        return "default";
    }

    /**
     * Получает список постов с пагинацией.
     * <p>
     * Использует кэшированный метод {@link PostCacheService#getProductsList}
     * для получения постов с учетом категории, поиска, региона, города и сортировки.
     * </p>
     *
     * @param filter фильтры для загрузки постов
     * @param page   номер страницы (начиная с 0)
     * @param size   количество элементов на странице
     * @return список постов
     */
    @Override
    public List<? extends PostText> getPosts(ProductFilterDTO filter, int page, int size) {
        return postCacheService.getProductsList(
                filter.getCategory(),
                filter.getSearch(),
                filter.getRegion(),
                filter.getCity(),
                filter.getSort() != null ? filter.getSort() : "newest",
                filter.getPriceSort() != null ? filter.getPriceSort() : "none",
                page,
                size
        );
    }

    /**
     * Подсчитывает общее количество постов с фильтрацией.
     * <p>
     * Использует кэшированный метод {@link PostCacheService#getProductsCount}
     * для подсчета постов с учетом категории, поиска, региона и города.
     * </p>
     *
     * @param filter фильтры для загрузки постов
     * @return общее количество постов
     */
    @Override
    public long getTotalCount(ProductFilterDTO filter) {
        return postCacheService.getProductsCount(
                filter.getCategory(),
                filter.getSearch(),
                filter.getRegion(),
                filter.getCity()
        );
    }

    /**
     * Добавляет специфичные атрибуты в модель.
     * <p>
     * Для дефолтной категории специфичные атрибуты не требуются.
     * </p>
     *
     * @param filter фильтры запроса
     * @param model  модель для добавления атрибутов
     */
    @Override
    public void addModelAttributes(ProductFilterDTO filter, Model model) {
        // Для общей категории специфичных атрибутов нет
    }
}