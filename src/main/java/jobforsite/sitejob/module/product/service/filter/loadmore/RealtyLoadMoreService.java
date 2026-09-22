package jobforsite.sitejob.module.product.service.filter.loadmore;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.dto.RangeFilter;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.RealtyPostRepository;
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
 * Сервис для загрузки постов категории "Недвижимость" с пагинацией.
 * <p>
 * Реализует интерфейс {@link ProductLoadMoreService} для бесконечной прокрутки
 * постов категории "Недвижимость" с фильтрацией по всем специфическим полям:
 * тип недвижимости, комнаты, этажи, лифт, год постройки, площадь и т.д.
 * </p>
 *
 * @see ProductLoadMoreService
 * @see RealtyPostRepository
 * @see FilterRangeService
 */
@Slf4j
@Component("realtyLoadMore")
@RequiredArgsConstructor
public class RealtyLoadMoreService implements ProductLoadMoreService {

    private final RealtyPostRepository realtyPostRepository;
    private final FilterRangeService filterRangeService;

    private final DefaultLoadMoreService defaultLoadMoreService;

    /**
     * Проверяет, поддерживает ли сервис указанную категорию.
     *
     * @param category название категории
     * @return true если категория "realty"
     */
    @Override
    public boolean supports(String category) {
        return "realty".equals(category);
    }

    /**
     * Возвращает название категории.
     *
     * @return "realty"
     */
    @Override
    public String getCategoryName() {
        return "realty";
    }

    /**
     * Загружает ID постов недвижимости с фильтрацией и пагинацией.
     * <p>
     * Фильтрует по: поиску, категории, региону, городу, типу недвижимости,
     * комнатам, этажам, лифту, году постройки, площади, расстоянию до города,
     * материалу и ремонту.
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

        // Определяем год постройки в зависимости от типа недвижимости
        Integer buildYear = null;
        if ("apartment".equals(filter.getPropertyType())) {
            buildYear = filter.getApartmentBuildYear();
        } else if ("house".equals(filter.getPropertyType())) {
            buildYear = filter.getHouseBuildYear();
        }

        // Конвертируем значения select в диапазоны
        RangeFilter buildYearRange = filterRangeService.getBuildYearRange(buildYear);
        RangeFilter houseAreaRange = filterRangeService.getHouseAreaRange(filter.getHouseArea());
        RangeFilter landAreaRange = filterRangeService.getLandAreaRange(filter.getLandArea(), filter.getPropertyType());
        RangeFilter distanceToCityRange = filterRangeService.getDistanceToCityRange(filter.getDistanceToCity());
        RangeFilter floorsCountRange = filterRangeService.getFloorsCountRange(filter.getFloorsCount());
        RangeFilter floorsCountHouseRange = filterRangeService.getFloorsCountHouseRange(filter.getFloorsCountHouse());
        RangeFilter floorsApartmentRange = filterRangeService.getFloorsApartmentRange(filter.getFloorsApartment());
        RangeFilter distanceToCityHouseRange = filterRangeService.getDistanceToCityHouseRange(filter.getDistanceToCityHouse());
        RangeFilter landAreaGarageRange = filterRangeService.getLandAreaGarageRange(filter.getLandAreaGarage());
        RangeFilter landAreaCommercialRange = filterRangeService.getLandAreaCommercialRange(filter.getLandAreaCommercial());

        return realtyPostRepository.findRealtyIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getPropertyType(),
                filter.getRoomsCount(),
                floorsApartmentRange.getMin(),
                floorsApartmentRange.getMax(),
                floorsCountRange.getMin(),
                floorsCountRange.getMax(),
                floorsCountHouseRange.getMin(),
                floorsCountHouseRange.getMax(),
                filter.getHasElevator(),
                buildYearRange.getMin(),
                buildYearRange.getMax(),
                houseAreaRange.getMin(),
                houseAreaRange.getMax(),
                landAreaRange.getMin(),
                landAreaRange.getMax(),
                distanceToCityRange.getMin(),
                distanceToCityRange.getMax(),
                filter.getMaterialUp(),
                filter.getRepairUp(),
                filter.getMaterialHouse(),
                filter.getRepairHouse(),
                distanceToCityHouseRange.getMin(),
                distanceToCityHouseRange.getMax(),
                landAreaGarageRange.getMin(),
                landAreaGarageRange.getMax(),
                landAreaCommercialRange.getMin(),
                landAreaCommercialRange.getMax(),
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