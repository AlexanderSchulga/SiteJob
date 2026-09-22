package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.dto.RangeFilter;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.VehiclePostRepository;
import jobforsite.sitejob.module.product.service.FilterRangeService;
import jobforsite.sitejob.module.product.service.filter.ProductLoadMoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Сервис для загрузки постов категории "Автотранспорт" с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для бесконечной прокрутки
 * постов категории "Автотранспорт" с фильтрацией по типу, региону бренда,
 * году выпуска, пробегу и количеству владельцев.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see VehiclePostRepository
 * @see FilterRangeService
 */
@Slf4j
@Component("vehicleLoadMore")
@RequiredArgsConstructor
public class VehicleLoadMoreService implements ProductLoadMoreService {

    private final VehiclePostRepository vehiclePostRepository;
    private final FilterRangeService filterRangeService;

    private final DefaultLoadMoreService defaultLoadMoreService;

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     *
     * @param category название категории
     * @return true если категория "motor"
     */
    @Override
    public boolean supports(String category) {
        return "motor".equals(category);
    }

    /**
     * Возвращает название категории.
     *
     * @return "motor"
     */
    @Override
    public String getCategoryName() {
        return "motor";
    }

    /**
     * Загружает ID постов автотранспорта с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, типу транспорта,
     * региону бренда, году выпуска, пробегу и количеству владельцев.
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

        RangeFilter vehicleYearRange = filterRangeService.getVehicleYearRange(filter.getVehicleYear());
        RangeFilter mileageRange = filterRangeService.getMileageRange(filter.getMileage());
        RangeFilter numberOfOwnersRange = filterRangeService.getNumberOfOwnersRange(filter.getNumberOfOwners());

        return vehiclePostRepository.findVehicleIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getVehicleType(),
                filter.getVehicleOrigin(),
                vehicleYearRange.getMin(),
                vehicleYearRange.getMax(),
                mileageRange.getMin(),
                mileageRange.getMax(),
                numberOfOwnersRange.getMin(),
                numberOfOwnersRange.getMax(),
                pageable
        );
    }

    /**
     * Добавляет специфичные параметры в ответ.
     *
     * @param response ответ для клиента
     * @param filter   фильтры запроса
     */
    @Override
    public void addResponseParams(Map<String, Object> response, ProductFilterDTO filter) {
        // Специфичных параметров в response нет
    }
}