package jobforsite.sitejob.module.product.service.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FilterFactory {

    private final Map<String, ProductFilter> filters;

    /**
     * Возвращает фильтр для указанной категории.
     * Если подходящий фильтр не найден - возвращает дефолтный.
     */
    public ProductFilter getFilter(String category) {
        // Нормализация: "all" или null -> default
        String key = (category != null && !"all".equals(category)) ? category : "defaultFilter";

        return filters.getOrDefault(key, filters.get("defaultFilter"));
    }
}