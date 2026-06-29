package jobforsite.sitejob.module.shared.dto;

import lombok.Data;

import java.util.List;

/**
 * DTO для пагинированных ответов API с избранными элементами (Slice).
 * Используется для передачи списков избранных элементов с информацией о пагинации
 * и агрегированными количествами для отображения бейджей в UI.
 *
 * @param <T> тип элементов в списке favorites
 */
@Data
public class SliceFavorResponseDTO<T> {
    private String status;          // Статус ответа (например, "success", "error")
    private String message;         // Сообщение для клиента
    private List<T> favorites;      // Список избранных элементов на текущей странице
    private boolean hasMore;        // Есть ли еще элементы для загрузки (следующая страница)
    private int currentPage;        // Номер текущей страницы
    private int pageSize;           // Размер страницы (количество элементов)

    // если нужны общие количества для бейджей
    private Long totalProducts;     // Общее количество избранных товаров
    private Long totalServices;     // Общее количество избранных услуг
    private Long totalVacancies;    // Общее количество избранных вакансий
    private Long totalResume;       // Общее количество избранных резюме

    /**
     * Конструктор по умолчанию.
     * Инициализирует пустой объект для последующего заполнения.
     */
    public SliceFavorResponseDTO() {}

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
     * Возвращает список избранных элементов.
     *
     * @return список элементов типа T
     */
    public List<T> getFavorites() {
        return favorites;
    }

    /**
     * Устанавливает список избранных элементов.
     *
     * @param favorites список элементов типа T
     */
    public void setFavorites(List<T> favorites) {
        this.favorites = favorites;
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
     * Возвращает размер страницы.
     *
     * @return количество элементов на странице
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Устанавливает размер страницы.
     *
     * @param pageSize количество элементов на странице
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Возвращает общее количество избранных товаров.
     * Используется для отображения бейджа в UI.
     *
     * @return количество товаров или null, если не применимо
     */
    public Long getTotalProducts() {
        return totalProducts;
    }

    /**
     * Устанавливает общее количество избранных товаров.
     *
     * @param totalProducts количество товаров
     */
    public void setTotalProducts(Long totalProducts) {
        this.totalProducts = totalProducts;
    }

    /**
     * Возвращает общее количество избранных услуг.
     * Используется для отображения бейджа в UI.
     *
     * @return количество услуг или null, если не применимо
     */
    public Long getTotalServices() {
        return totalServices;
    }

    /**
     * Устанавливает общее количество избранных услуг.
     *
     * @param totalServices количество услуг
     */
    public void setTotalServices(Long totalServices) {
        this.totalServices = totalServices;
    }

    /**
     * Возвращает общее количество избранных вакансий.
     * Используется для отображения бейджа в UI.
     *
     * @return количество вакансий или null, если не применимо
     */
    public Long getTotalVacancies() {
        return totalVacancies;
    }

    /**
     * Устанавливает общее количество избранных вакансий.
     *
     * @param totalVacancies количество вакансий
     */
    public void setTotalVacancies(Long totalVacancies) {
        this.totalVacancies = totalVacancies;
    }

    /**
     * Возвращает общее количество избранных резюме.
     * Используется для отображения бейджа в UI.
     *
     * @return количество резюме или null, если не применимо
     */
    public Long getTotalResume() {
        return totalResume;
    }

    /**
     * Устанавливает общее количество избранных резюме.
     *
     * @param totalResume количество резюме
     */
    public void setTotalResume(Long totalResume) {
        this.totalResume = totalResume;
    }
}