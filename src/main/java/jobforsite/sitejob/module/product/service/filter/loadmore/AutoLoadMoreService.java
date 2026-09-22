package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.AutoPostRepository;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Сервис для загрузки постов категории "Автозапчасти и автотовары" с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для бесконечной прокрутки
 * постов категории "Автозапчасти" с фильтрацией по типу и состоянию.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see AutoPostRepository
 */
@Slf4j
@Component("autoLoadMore")
@RequiredArgsConstructor
public class AutoLoadMoreService implements ProductLoadMoreService {

    private final AutoPostRepository autoPostRepository;

    private final DefaultLoadMoreService defaultLoadMoreService;

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     *
     * @param category название категории
     * @return true если категория "auto"
     */
    @Override
    public boolean supports(String category) {
        return "auto".equals(category);
    }

    /**
     * Возвращает название категории.
     *
     * @return "auto"
     */
    @Override
    public String getCategoryName() {
        return "auto";
    }

    /**
     * Загружает ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, типу и состоянию автозапчастей.
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

        return autoPostRepository.findAutoPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAutoType(),
                filter.getConditionAuto(),
                pageable
        );
    }

    /**
     * Добавляет специфичные параметры в ответ.
     * <p>
     * Для категории "Автозапчасти" специфичные параметры не требуются,
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