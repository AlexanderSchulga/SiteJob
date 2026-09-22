package jobforsite.sitejob.module.product.service;

import jobforsite.sitejob.module.product.dto.RangeFilter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FilterRangeService {

    /**
     * Маппинг значений select в диапазоны для buildYear (год постройки)
     * Для ленты - SELECT с индексами (0-7) маппятся в диапазоны
     */
    public RangeFilter getBuildYearRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(null, 1950));
        ranges.put(1, new RangeFilter(1951, 1961));
        ranges.put(2, new RangeFilter(1962, 1973));
        ranges.put(3, new RangeFilter(1974, 1985));
        ranges.put(4, new RangeFilter(1986, 1997));
        ranges.put(5, new RangeFilter(1998, 2009));
        ranges.put(6, new RangeFilter(2010, 2021));
        ranges.put(7, new RangeFilter(2022, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для vehicleYear (год выпуска авто)
     * Для ленты - SELECT с индексами (0-9) маппятся в диапазоны
     */
    public RangeFilter getVehicleYearRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(null, 1970));
        ranges.put(1, new RangeFilter(1971, 1981));
        ranges.put(2, new RangeFilter(1982, 1987));
        ranges.put(3, new RangeFilter(1988, 1993));
        ranges.put(4, new RangeFilter(1994, 1999));
        ranges.put(5, new RangeFilter(2000, 2005));
        ranges.put(6, new RangeFilter(2006, 2011));
        ranges.put(7, new RangeFilter(2012, 2017));
        ranges.put(8, new RangeFilter(2018, 2023));
        ranges.put(9, new RangeFilter(2024, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для houseArea (площадь дома)
     */
    public RangeFilter getHouseAreaRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 50));
        ranges.put(1, new RangeFilter(51, 100));
        ranges.put(2, new RangeFilter(101, 150));
        ranges.put(3, new RangeFilter(151, 200));
        ranges.put(4, new RangeFilter(201, null));      // Более 200

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для landArea (площадь земли)
     * Для УЧАСТКОВ - SELECT с индексами (0-5) маппятся в диапазоны
     * Для ДОМОВ - SELECT с индексами (0-5) маппятся в диапазоны
     */
    public RangeFilter getLandAreaRange(Integer value, String propertyType) {
        if (value == null) return new RangeFilter(null, null);

        // Общие диапазоны для всех типов (участок и дом)
        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 10));
        ranges.put(1, new RangeFilter(11, 20));
        ranges.put(2, new RangeFilter(21, 30));
        ranges.put(3, new RangeFilter(31, 40));
        ranges.put(4, new RangeFilter(41, 50));
        ranges.put(5, new RangeFilter(51, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для distanceToCity (расстояние до города)
     * SELECT с индексами (0-7) маппятся в диапазоны
     */
    public RangeFilter getDistanceToCityRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 5));
        ranges.put(1, new RangeFilter(6, 10));
        ranges.put(2, new RangeFilter(11, 20));
        ranges.put(3, new RangeFilter(21, 30));
        ranges.put(4, new RangeFilter(31, 40));
        ranges.put(5, new RangeFilter(41, 50));
        ranges.put(6, new RangeFilter(51, 100));
        ranges.put(7, new RangeFilter(101, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * =====Маппинг значений select в диапазоны для floorsCount (этажность) =====
     *
     * Для КВАРТИР:
     *   - значение 0: "До 5 этажей" → диапазон 1-5
     *   - значение 1: "Более 5 этажей" → диапазон 6 и выше
     *
     * Для ДОМОВ:
     *   - значение 0: "1 этаж" → точное значение 1
     *   - значение 1: "2 этажа" → точное значение 2
     *   - значение 2: "3 этажа" → точное значение 3
     *   - значение 3: "4+ этажа" → диапазон 4 и выше
     */
    public RangeFilter getFloorsCountRange(Integer value) {

        if (value == null) return new RangeFilter(null, null);

            Map<Integer, RangeFilter> ranges = new HashMap<>();
            ranges.put(0, new RangeFilter(1, 5));   // До 5 этажей
            ranges.put(1, new RangeFilter(6, null)); // Более 5 этажей
            return ranges.getOrDefault(value, new RangeFilter(null, null));

        // Для других типов - точное совпадение
       // return new RangeFilter(value, value);
    }

    public RangeFilter getFloorsCountHouseRange(Integer value) {

        if (value == null) return new RangeFilter(null, null);

        // Для домов - точные значения или диапазон для "4+"
            Map<Integer, RangeFilter> ranges = new HashMap<>();
            ranges.put(0, new RangeFilter(1, 1));   // 1 этаж
            ranges.put(1, new RangeFilter(2, 2));   // 2 этажа
            ranges.put(2, new RangeFilter(3, 3));   // 3 этажа
            ranges.put(3, new RangeFilter(4, null)); // 4+ этажа
            return ranges.getOrDefault(value, new RangeFilter(null, null));

    }

    /**
     * =====Маппинг значений select в диапазоны для floorsApartment (этаж квартиры) =====
     *
     * Для КВАРТИР:
     *   - значение 0: "от 1 до 5 этаж"
     *   - значение 1: "от 6 до 10 этаж"
     *   - значение 2: "от 11 до 20 этаж"
     *   - значение 3: "от 21 до 30 этаж"
     *   - значение 4: "от 31 и выше"

     */
    public RangeFilter getFloorsApartmentRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

            Map<Integer, RangeFilter> ranges = new HashMap<>();
            ranges.put(0, new RangeFilter(1, 5));
            ranges.put(1, new RangeFilter(6, 10));
            ranges.put(2, new RangeFilter(11, 20));
            ranges.put(3, new RangeFilter(21, 30));
            ranges.put(4, new RangeFilter(31, null));
        return ranges.getOrDefault(value, new RangeFilter(null, null));

    }

    /**
     * Маппинг значений select в диапазоны для distanceToCityHouse (расстояние до города)
     * SELECT с индексами (0-7) маппятся в диапазоны
     */
    public RangeFilter getDistanceToCityHouseRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 5));
        ranges.put(1, new RangeFilter(6, 10));
        ranges.put(2, new RangeFilter(11, 20));
        ranges.put(3, new RangeFilter(21, 30));
        ranges.put(4, new RangeFilter(31, 40));
        ranges.put(5, new RangeFilter(41, 50));
        ranges.put(6, new RangeFilter(51, 100));
        ranges.put(7, new RangeFilter(101, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для landAreaGarage (площадь гаража)
     * SELECT с индексами (0-3) маппятся в диапазоны
     */
    public RangeFilter getLandAreaGarageRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 40));
        ranges.put(1, new RangeFilter(41, 50));
        ranges.put(2, new RangeFilter(51, 60));
        ranges.put(3, new RangeFilter(61, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для LandAreaCommercial (площадь коммерческой недвижимости)
     * SELECT с индексами (0-5) маппятся в диапазоны
     */
    public RangeFilter getLandAreaCommercialRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 40));
        ranges.put(1, new RangeFilter(41, 50));
        ranges.put(2, new RangeFilter(51, 60));
        ranges.put(3, new RangeFilter(61, 70));
        ranges.put(4, new RangeFilter(71, 100));
        ranges.put(5, new RangeFilter(101, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для Mileage (пробег)
     * SELECT с индексами (0-5) маппятся в диапазоны
     */
    public RangeFilter getMileageRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 10));
        ranges.put(1, new RangeFilter(11, 40));
        ranges.put(2, new RangeFilter(41, 80));
        ranges.put(3, new RangeFilter(81, 130));
        ranges.put(4, new RangeFilter(131, 180));
        ranges.put(5, new RangeFilter(181, 250));
        ranges.put(6, new RangeFilter(251, 320));
        ranges.put(7, new RangeFilter(321, 400));
        ranges.put(8, new RangeFilter(401, null));

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    /**
     * Маппинг значений select в диапазоны для NumberOfOwners (Количество владельцев)
     * SELECT с индексами (0-5) маппятся в диапазоны
     */
    public RangeFilter getNumberOfOwnersRange(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        Map<Integer, RangeFilter> ranges = new HashMap<>();
        ranges.put(0, new RangeFilter(1, 1));    // 1 владелец
        ranges.put(1, new RangeFilter(2, 2));    // 2 владельца
        ranges.put(2, new RangeFilter(3, 3));    // 3 владельца
        ranges.put(3, new RangeFilter(4, 4));    // 4 владельца
        ranges.put(4, new RangeFilter(5, null)); // 5 и более

        return ranges.getOrDefault(value, new RangeFilter(null, null));
    }

    // =====  МЕТОД: Нормализация параметров =====
    public String normalizeParam(String param) {
        return (param != null && !param.trim().isEmpty() && !"all".equals(param))
                ? param.trim()
                : null;
    }

    // =====  МЕТОД: Конвертация select в диапазон для buildYear (для RealtyFilterDTO) =====
    public RangeFilter getBuildYearRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);

        // Используем существующий метод
        return getBuildYearRange(value);
    }

    // =====  МЕТОД: Конвертация select в диапазон для vehicleYear =====
    public RangeFilter getVehicleYearRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getVehicleYearRange(value);
    }

    // ===== НОВЫЙ МЕТОД: Конвертация select в диапазон для houseArea =====
    public RangeFilter getHouseAreaRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getHouseAreaRange(value);
    }

    // =====  МЕТОД: Конвертация select в диапазон для landArea =====
    public RangeFilter getLandAreaRangeForFilter(Integer value, String propertyType) {
        if (value == null) return new RangeFilter(null, null);
        return getLandAreaRange(value, propertyType);
    }

    // ===== МЕТОД: Конвертация select в диапазон для distanceToCity =====
    public RangeFilter getDistanceToCityRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getDistanceToCityRange(value);
    }

    // ===== МЕТОД: Конвертация select в диапазон для distanceToCityHouse =====
    public RangeFilter getDistanceToCityHouseRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getDistanceToCityHouseRange(value);
    }

    // ===== МЕТОД: Конвертация select в диапазон для landAreaGarage =====
    public RangeFilter getLandAreaGarageRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getLandAreaGarageRange(value);
    }

    // ===== МЕТОД: Конвертация select в диапазон для landAreaCommercial =====
    public RangeFilter getLandAreaCommercialRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getLandAreaCommercialRange(value);
    }

    // ===== МЕТОД: Конвертация select в диапазон для Mileage =====
    public RangeFilter getMileageRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getMileageRange(value);
    }

    // ===== МЕТОД: Конвертация select в диапазон для NumberOfOwners =====
    public RangeFilter getNumberOfOwnersRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getNumberOfOwnersRange(value);
    }

    // =====  МЕТОД: floorsCount для квартир и дома (свойство propertyType) =====
    public RangeFilter getFloorsCountRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getFloorsCountRange(value);
    }

    public RangeFilter getFloorsCountHouseRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getFloorsCountHouseRange(value);
    }

    // ===== МЕТОД: floorsApartment для квартир =====
    public RangeFilter getFloorsApartmentRangeForFilter(Integer value) {
        if (value == null) return new RangeFilter(null, null);
        return getFloorsApartmentRange(value);
    }

    // ===== МЕТОД: получение диапазона по значению с учетом типа =====
    public RangeFilter getRangeForValue(Integer value, String filterType, String propertyType) {
        if (value == null) return new RangeFilter(null, null);

        return switch (filterType) {
            case "buildYear" -> getBuildYearRange(value);
            case "vehicleYear" -> getVehicleYearRange(value);
            case "houseArea" -> getHouseAreaRange(value);
            case "landArea" -> getLandAreaRange(value, propertyType);
            case "distanceToCity" -> getDistanceToCityRange(value);
            case "floorsCount" -> getFloorsCountRange(value);
            case "floorsCountHouse" -> getFloorsCountHouseRange(value);
            case "floorsApartment" -> getFloorsApartmentRange(value);
            case "distanceToCityHouse" -> getDistanceToCityHouseRange(value);
            case "landAreaGarage" -> getLandAreaGarageRange(value);
            case "landAreaCommercial" -> getLandAreaCommercialRange(value);
            case "mileage" -> getMileageRange(value);
            case "numberOfOwners" -> getNumberOfOwnersRange(value);


            default -> new RangeFilter(null, null);
        };
    }


}