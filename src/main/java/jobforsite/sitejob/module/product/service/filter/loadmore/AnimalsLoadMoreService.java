package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.AnimalsPostRepository;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Сервис для загрузки постов категории "Флора и фауна" с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для бесконечной прокрутки
 * постов категории "Флора и фауна" с фильтрацией по типу животных.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see AnimalsPostRepository
 */
@Slf4j
@Component("animalsLoadMore")
@RequiredArgsConstructor
public class AnimalsLoadMoreService implements ProductLoadMoreService {

    private final AnimalsPostRepository animalsPostRepository;

    private final DefaultLoadMoreService defaultLoadMoreService;

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     *
     * @param category название категории
     * @return true если категория "animals"
     */
    @Override
    public boolean supports(String category) {
        return "animals".equals(category);
    }

    /**
     * Возвращает название категории.
     *
     * @return "animals"
     */
    @Override
    public String getCategoryName() {
        return "animals";
    }

    /**
     * Загружает ID постов с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу и типу животных.
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

        // Если showOtherCities - используем отдельный метод
        if (showOtherCities) {
            return defaultLoadMoreService.loadIds(filter, pageable, true, standardCities);
        }

        return animalsPostRepository.findAnimalsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAnimalsKind(),
                pageable
        );
    }

    /**
     * Добавляет специфичные параметры в ответ.
     * <p>
     * Для категории "Флора и фауна" специфичные параметры не требуются,
     * так как они передаются через отдельные параметры в URL.
     * </p>
     *
     * @param response ответ для клиента
     * @param filter   фильтры запроса
     */
    @Override
    public void addResponseParams(Map<String, Object> response, ProductFilterDTO filter) {
        // Специфичных параметров в response нет
        // (они передаются через отдельные параметры в URL)
    }
}