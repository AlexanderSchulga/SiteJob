package jobforsite.sitejob.module.shared.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO для пагинированных ответов API с использованием Slice (без totalPages).
 * Используется для передачи списков редактируемых и избранных элементов
 * с информацией о пагинации на основе срезов, а также с поддержкой поиска.
 *
 * @param <T> тип элементов в списках redact и favorites
 */
@Data
public class PaginationSliceResponseDTO<T> {
    private String status;          // Статус ответа (например, "success", "error")
    private String message;         // Сообщение для клиента
    private List<T> redact;         // Список элементов для редактирования на текущей странице
    private List<T> favorites;      // Список избранных элементов на текущей странице
    private boolean hasMore;        // Есть ли еще элементы для загрузки (следующая страница)
    private int currentPage;        // Номер текущей страницы
    private int size;               // количество элементов в текущем слайсе

    // Поля для поиска
    private String searchQuery;     // Поисковый запрос пользователя
    private String searchType;      // Тип поиска (например, "keyword", "category", "tag")
    private long totalResults;      // Общее количество результатов поиска

    /**
     * Конструктор по умолчанию.
     * Инициализирует пустой объект для последующего заполнения.
     */
    public PaginationSliceResponseDTO() {}

    /**
     * Возвращает статус ответа.
     *
     * @return статус (например, "success", "error")
     */
    public String getStatus() {
        return status;
    }

    /**
     * Устанавливает статус ответа.
     *
     * @param status статус ответа
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Возвращает сообщение ответа.
     *
     * @return сообщение для клиента
     */
    public String getMessage() {
        return message;
    }

    /**
     * Устанавливает сообщение ответа.
     *
     * @param message сообщение для клиента
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Возвращает список элементов для редактирования.
     *
     * @return список элементов типа T
     */
    public List<T> getRedact() {
        return redact;
    }

    /**
     * Устанавливает список элементов для редактирования.
     *
     * @param redact список элементов типа T
     */
    public void setRedact(List<T> redact) {
        this.redact = redact;
    }

    /**
     * Проверяет, есть ли еще элементы для загрузки.
     *
     * @return true, если есть следующая страница, иначе false
     */
    public boolean isHasMore() {
        return hasMore;
    }

    /**
     * Устанавливает флаг наличия следующих элементов.
     *
     * @param hasMore true, если есть следующая страница, иначе false
     */
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * Возвращает номер текущей страницы.
     *
     * @return номер текущей страницы
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Устанавливает номер текущей страницы.
     *
     * @param currentPage номер текущей страницы
     */
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Возвращает размер текущего слайса.
     *
     * @return количество элементов на странице
     */
    public int getSize() {
        return size;
    }

    /**
     * Устанавливает размер текущего слайса.
     *
     * @param size количество элементов на странице
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Возвращает поисковый запрос.
     *
     * @return поисковый запрос
     */
    public String getSearchQuery() {
        return searchQuery;
    }

    /**
     * Устанавливает поисковый запрос.
     *
     * @param searchQuery поисковый запрос
     */
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    /**
     * Возвращает тип поиска.
     *
     * @return тип поиска
     */
    public String getSearchType() {
        return searchType;
    }

    /**
     * Устанавливает тип поиска.
     *
     * @param searchType тип поиска
     */
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    /**
     * Возвращает общее количество результатов поиска.
     *
     * @return общее количество результатов
     */
    public long getTotalResults() {
        return totalResults;
    }

    /**
     * Устанавливает общее количество результатов поиска.
     *
     * @param totalResults общее количество результатов
     */
    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }
}