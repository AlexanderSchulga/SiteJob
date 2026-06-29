package jobforsite.sitejob.module.shared.dto;

import lombok.Data;
import java.util.List;

/**
 * Универсальный DTO для пагинированных ответов API.
 * Используется для передачи списков редактируемых и избранных элементов
 * вместе с метаинформацией о страницах.
 *
 * @param <T> тип элементов в списках redact и favorites
 */
@Data
public class PaginationResponseDTO<T> {
    private String status;          // Статус ответа (например, "success", "error")
    private String message;         // Сообщение для клиента
    private List<T> redact;         // Список элементов для редактирования
    private List<T> favorites;      // Список избранных элементов
    private boolean hasMore;        // Есть ли еще страницы
    private int currentPage;        // Текущая страница
    private int totalPages;         // Общее количество страниц
    private long totalElements;     // Общее количество элементов во всем наборе

    /**
     * Конструктор по умолчанию.
     * Инициализирует пустой объект для последующего заполнения.
     */
    public PaginationResponseDTO() {}

    /**
     * Конструктор с полной инициализацией всех полей.
     *
     * @param status       статус ответа
     * @param message      сопроводительное сообщение
     * @param redact       список элементов для редактирования
     * @param favorites    список избранных элементов
     * @param hasMore      флаг наличия следующих страниц
     * @param currentPage  номер текущей страницы
     * @param totalPages   общее количество страниц
     * @param totalElements общее количество элементов
     */
    public PaginationResponseDTO(String status, String message, List<T> redact, List<T> favorites, boolean hasMore,
                                 int currentPage, int totalPages, long totalElements) {
        this.status = status;
        this.message = message;
        this.redact = redact;
        this.favorites = favorites;
        this.hasMore = hasMore;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }
}