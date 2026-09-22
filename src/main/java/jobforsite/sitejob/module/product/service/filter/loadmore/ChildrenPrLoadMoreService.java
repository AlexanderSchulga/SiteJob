package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.ChildrenPrPostRepository;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Сервис для загрузки постов категории "Детские товары" с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для бесконечной прокрутки
 * постов категории "Детские товары" с фильтрацией по полу и состоянию.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see ChildrenPrPostRepository
 */
@Slf4j
@Component("childrenPrLoadMore")
@RequiredArgsConstructor
public class ChildrenPrLoadMoreService implements ProductLoadMoreService {

    private final ChildrenPrPostRepository childrenPrPostRepository;

    private final DefaultLoadMoreService defaultLoadMoreService;

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     *
     * @param category название категории
     * @return true если категория "childrenPr"
     */
    @Override
    public boolean supports(String category) {
        return "childrenPr".equals(category);
    }

    /**
     * Возвращает название категории.
     *
     * @return "childrenPr"
     */
    @Override
    public String getCategoryName() {
        return "childrenPr";
    }

    /**
     * Загружает ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, полу и состоянию детских товаров.
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
            return defaultLoadMoreService.loadIds(filter, pageable, true, standardCities);
        }

        return childrenPrPostRepository.findChildrenPrPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getGenderChildrenPr(),
                filter.getConditionChildrenPr(),
                pageable
        );
    }

    /**
     * Добавляет специфичные параметры в ответ.
     * <p>
     * Для категории "Детские товары" специфичные параметры не требуются,
     * так как они передаются через отдельные параметры в URL.
     * </p>
     *
     * @param response ответ для клиента
     * @param filter   фильтры запроса
     */
    @Override
    public void addResponseParams(Map<String, Object> response, ProductFilterDTO filter) {
        // Специфичных параметров в response нет
    }
}