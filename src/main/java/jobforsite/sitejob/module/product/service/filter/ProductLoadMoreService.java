package jobforsite.sitejob.module.product.service.filter;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

/**
 * Сервис для пагинированной загрузки товаров конкретной категории.
 */
public interface ProductLoadMoreService {

    /**
     * Проверяет, поддерживается ли данная категория.
     */
    boolean supports(String category);

    /**
     * Загружает ID товаров с пагинацией и фильтрацией.
     */
    Slice<Long> loadIds(ProductFilterDTO filter, Pageable pageable, boolean showOtherCities, List<String> standardCities);

    /**
     * Добавляет специфичные для категории параметры в response.
     */
    void addResponseParams(Map<String, Object> response, ProductFilterDTO filter);

    /**
     * Возвращает название категории.
     */
    String getCategoryName();
}