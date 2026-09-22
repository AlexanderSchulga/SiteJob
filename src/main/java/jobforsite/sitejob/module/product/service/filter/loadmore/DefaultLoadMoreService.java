package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.dto.RangeFilter;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.product.repository.PostRepoExtends;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Дефолтный сервис для загрузки постов с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для всех категорий,
 * у которых нет специализированного сервиса.
 * Используется как fallback при загрузке постов.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see PostRepo
 * @see PostRepoExtends
 */
@Slf4j
@Component("defaultLoadMore")
public class DefaultLoadMoreService implements ProductLoadMoreService {

    private final PostRepo postRepo;
    private final PostRepoExtends postRepoExtends;

    public DefaultLoadMoreService(PostRepo postRepo, PostRepoExtends postRepoExtends) {
        this.postRepo = postRepo;
        this.postRepoExtends = postRepoExtends;
    }

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     * <p>
     * Всегда возвращает true, так как это дефолтный сервис для всех категорий.
     * </p>
     *
     * @param category название категории
     * @return всегда true
     */
    @Override
    public boolean supports(String category) {
        return true;
    }

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
     * Загружает ID постов с фильтрацией и пагинацией.
     * <p>
     * Использует универсальный метод репозитория с передачей всех фильтров как null.
     * Возвращает только активные посты (не архивные, не заблокированные, прошедшие модерацию).
     * </p>
     *
     * @param filter            фильтры для загрузки постов
     * @param pageable          параметры пагинации
     * @param showOtherCities   флаг показа постов из других городов региона
     * @param standardCities    список стандартных городов региона
     * @return Slice с ID постов
     */
    @Override
    public Slice<Long> loadIds(ProductFilterDTO filter, Pageable pageable,
                               boolean showOtherCities, List<String> standardCities) {

        if (showOtherCities) {
            return postRepo.findPostIdsByOtherCitiesSlice(
                    filter.getSearch(),
                    filter.getCategory(),
                    filter.getRegion(),
                    standardCities,
                    pageable
            );
        }

        RangeFilter emptyRange = new RangeFilter(null, null);

        return postRepo.findPostIdsWithFiltersSlice(
                postRepoExtends,
                filter.getSearch(), filter.getCategory(), filter.getRegion(), filter.getCity(),
                null, null,
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                null,
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                null, null,
                emptyRange.getMin(), emptyRange.getMax(),
                null, null, null, null,
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                emptyRange.getMin(), emptyRange.getMax(),
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                pageable
        );
    }

    /**
     * Добавляет специфичные параметры в ответ.
     * <p>
     * Для дефолтной категории специфичные параметры не требуются.
     * </p>
     *
     * @param response ответ для клиента
     * @param filter   фильтры запроса
     */
    @Override
    public void addResponseParams(Map<String, Object> response, ProductFilterDTO filter) {
        // Для общей категории специфичных параметров нет
    }
}