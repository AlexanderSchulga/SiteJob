package jobforsite.sitejob.module.product.service.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Фабрика для получения сервисов загрузки постов с пагинацией.
 * <p>
 * Предоставляет возможность динамического выбора соответствующего сервиса
 * {@link ProductLoadMoreService} на основе категории продукта.
 * Использует паттерн Фабрика для централизованного управления
 * созданием и получением сервисов загрузки.
 * </p>
 * <p>
 * Работает на основе Spring-контейнера, где все реализации
 * {@link ProductLoadMoreService} автоматически инжектятся в Map
 * с ключами по умолчанию.
 * </p>
 * @see ProductLoadMoreService
 */
@Component
@RequiredArgsConstructor
public class LoadMoreFilterFactory {

    /**
     * Карта всех доступных сервисов загрузки постов.
     * <p>
     * Автоматически заполняется Spring-контейнером всеми бинами,
     * реализующими интерфейс {@link ProductLoadMoreService}.
     * Ключом является имя бина по умолчанию.
     * </p>
     */
    private final Map<String, ProductLoadMoreService> loadMoreServices;

    /**
     * Возвращает соответствующий сервис загрузки для указанной категории.
     * <p>
     * Алгоритм выбора:
     * <ol>
     *   <li>Если категория равна null или "all" - используется сервис по умолчанию</li>
     *   <li>Иначе выполняется поиск сервиса, который поддерживает указанную категорию
     *       (через метод {@link ProductLoadMoreService#supports(String)})</li>
     *   <li>Если подходящий сервис не найден - возвращается сервис по умолчанию</li>
     * </ol>
     * </p>
     *
     * @param category название категории продукта (может быть null или "all")
     * @return экземпляр {@link ProductLoadMoreService} для работы с постами указанной категории,
     *         либо сервис по умолчанию, если подходящий не найден
     * @throws IllegalStateException если сервис по умолчанию ("defaultLoadMore") отсутствует
     */
    public ProductLoadMoreService getService(String category) {
        //String key = (category != null && !"all".equals(category)) ? category : "defaultLoadMore";

        return loadMoreServices.entrySet().stream()
                .filter(entry -> entry.getValue().supports(category))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(loadMoreServices.get("defaultLoadMore"));
    }
}