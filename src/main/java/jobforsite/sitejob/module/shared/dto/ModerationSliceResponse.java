package jobforsite.sitejob.module.shared.dto;

import jobforsite.sitejob.module.product.model.PostText;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для передачи постраничных (slice) результатов модерации на клиент.
 * <p>
 * Используется вместо стандартного Spring {@link org.springframework.data.domain.Slice}
 * для упрощения сериализации и контроля над передаваемыми данными.
 * </p>
 *
 * @param <T> тип элементов на странице (например, {@link PostText},
 *           {@link jobforsite.sitejob.module.service.model.PostTextJob},
 *           {@link jobforsite.sitejob.module.vacancy.model.Vacancy},
 *           {@link jobforsite.sitejob.module.resume.model.Resume})
 */
@Data                     // Генерирует геттеры, сеттеры, toString, equals, hashCode
@NoArgsConstructor        // Для десериализации (Jackson)
@AllArgsConstructor       // Для удобного создания объекта
public class ModerationSliceResponse<T> {

    /**
     * Список сущностей на текущем срезе (slice) страницы.
     * <p>
     * Может быть пустым, если на странице нет элементов.
     * </p>
     */
    private List<T> content;

    /**
     * Флаг наличия следующей страницы.
     * <p>
     * true — есть ещё данные (следующий срез существует),
     * false — текущий срез последний.
     * </p>
     */
    private boolean hasNext;

    /**
     * Количество элементов на текущем срезе.
     * <p>
     * Обычно равно размеру {@link #content}, но может быть меньше,
     * если страница последняя или запрашиваемый размер больше доступного.
     * </p>
     */
    private int numberOfElements;
}