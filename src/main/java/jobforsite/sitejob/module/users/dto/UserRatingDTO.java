package jobforsite.sitejob.module.users.dto;

/**
 * DTO (Data Transfer Object) для передачи информации о рейтинге пользователя.
 * <p>
 * Содержит отформатированное значение среднего рейтинга (с одним знаком после запятой)
 * и количество оценок, на основе которых рассчитан рейтинг.
 * </p>
 * <p>
 * Особенности:
 * <ul>
 *     <li>Если средний рейтинг отсутствует или меньше/равен 0, formattedRating будет null</li>
 *     <li>Количество оценок по умолчанию равно 0, если передан null</li>
 * </ul>
 * </p>
 */
public class UserRatingDTO {

    /** Отформатированное значение среднего рейтинга (например, "4.5") или null, если рейтинг отсутствует */
    private String formattedRating;

    /** Количество оценок, на основе которых рассчитан рейтинг (по умолчанию 0) */
    private Integer ratingCount;

    /**
     * Конструктор DTO рейтинга пользователя.
     * <p>
     * Выполняет форматирование среднего рейтинга с одним знаком после запятой.
     * Если средний рейтинг равен null или меньше/равен 0, поле formattedRating остается null.
     * Количество оценок устанавливается в 0, если передан null.
     * </p>
     *
     * @param averageRating средний рейтинг (может быть null или 0, если оценок нет)
     * @param ratingCount   количество оценок (может быть null)
     */
    public UserRatingDTO(Double averageRating, Integer ratingCount) {
        // Форматируем рейтинг только если он существует и положительный
        if (averageRating != null && averageRating > 0) {
            this.formattedRating = String.format("%.1f", averageRating);
        } else {
            this.formattedRating = null;
        }
        // Устанавливаем количество оценок (0 по умолчанию)
        this.ratingCount = ratingCount != null ? ratingCount : 0;
    }

    // ==================== Геттеры ====================

    /**
     * Возвращает отформатированное значение среднего рейтинга.
     *
     * @return строка с рейтингом (например, "4.5") или null, если рейтинг отсутствует
     */
    public String getFormattedRating() {
        return formattedRating;
    }

    /**
     * Возвращает количество оценок.
     *
     * @return количество оценок (минимальное значение — 0)
     */
    public Integer getRatingCount() {
        return ratingCount;
    }
}