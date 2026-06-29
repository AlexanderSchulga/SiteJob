package jobforsite.sitejob.module.shared.dto;

import lombok.Data;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

/**
 * Универсальный DTO для пагинированных ответов API на основе Spring Data Slice.
 * Slice отличается от Page тем, что не содержит totalPages и totalElements,
 * что позволяет эффективнее работать с большими наборами данных.
 *
 * @param <T> тип элементов в списке content
 */
@Data
public class SliceResponseDTO<T> {
    private String status;              // Статус ответа (например, "success", "error")
    private String message;             // Сообщение для клиента
    private List<T> content;            // Содержимое текущего слайса (список элементов)
    private boolean hasNext;            // Есть ли следующий слайс
    private int currentPage;            // Номер текущей страницы (начинается с 0)
    private int size;                   // Размер слайса (количество элементов на странице)
    private boolean first;              // Является ли текущий слайс первым
    private boolean last;               // Является ли текущий слайс последним

    // поле для метаданных
    private Map<String, Object> metadata;   // Дополнительные метаданные (фильтры, сортировка и т.д.)

    /**
     * Конструктор по умолчанию.
     * Инициализирует пустой объект для последующего заполнения.
     */
    public SliceResponseDTO() {}

    /**
     * Конструктор с полной инициализацией основных полей.
     *
     * @param status      статус ответа
     * @param message     сообщение для клиента
     * @param content     содержимое слайса
     * @param hasNext     наличие следующего слайса
     * @param currentPage номер текущей страницы
     * @param size        размер слайса
     * @param first       является ли первым
     * @param last        является ли последним
     */
    public SliceResponseDTO(String status, String message, List<T> content,
                            boolean hasNext, int currentPage, int size,
                            boolean first, boolean last) {
        this.status = status;
        this.message = message;
        this.content = content;
        this.hasNext = hasNext;
        this.currentPage = currentPage;
        this.size = size;
        this.first = first;
        this.last = last;
    }

    /**
     * Фабричный метод для создания успешного ответа из объекта Slice Spring Data.
     *
     * @param slice   объект Slice из Spring Data
     * @param message сообщение для клиента
     * @param content преобразованное содержимое слайса
     * @param <T>     тип элементов
     * @return заполненный DTO со статусом "success"
     */
    public static <T> SliceResponseDTO<T> success(Slice<?> slice, String message, List<T> content) {
        return new SliceResponseDTO<>(
                "success",
                message,
                content,
                slice.hasNext(),
                slice.getNumber(),
                slice.getSize(),
                slice.isFirst(),
                slice.isLast()
        );
    }

    /**
     * Возвращает список содержимого слайса.
     * Алиас для getContent().
     *
     * @return список элементов типа T
     */
    public List<T> getReviews() {
        return content;
    }

    /**
     * Устанавливает список содержимого слайса.
     * Алиас для setContent().
     *
     * @param reviews список элементов типа T
     */
    public void setReviews(List<T> reviews) {
        this.content = reviews;
    }

    /**
     * Устанавливает метаданные.
     *
     * @param metadata карта с дополнительными данными
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Возвращает метаданные.
     *
     * @return карта с дополнительными данными
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}