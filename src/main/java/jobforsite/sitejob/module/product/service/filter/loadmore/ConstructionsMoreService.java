package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.ConstructionsPostRepository;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Сервис для загрузки постов категории "Строительные материалы" с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для бесконечной прокрутки
 * постов категории "Строительные материалы" с фильтрацией по типу назначения.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see ConstructionsPostRepository
 */
@Slf4j
@Component("constructionsLoadMore")
@RequiredArgsConstructor
public class ConstructionsMoreService implements ProductLoadMoreService {

    private final ConstructionsPostRepository constructionsPostRepository;

    private final DefaultLoadMoreService defaultLoadMoreService;

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     *
     * @param category название категории
     * @return true если категория "constructions"
     */
    @Override
    public boolean supports(String category) {
        return "constructions".equals(category);
    }

    /**
     * Возвращает название категории.
     *
     * @return "constructions"
     */
    @Override
    public String getCategoryName() {
        return "constructions";
    }

    /**
     * Загружает ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу строительных материалов.
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

        return constructionsPostRepository.findConstructionsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getConstructionsType(),
                pageable
        );
    }

    /**
     * Добавляет специфичные параметры в ответ.
     * <p>
     * Для категории "Строительные материалы" специфичные параметры не требуются.
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