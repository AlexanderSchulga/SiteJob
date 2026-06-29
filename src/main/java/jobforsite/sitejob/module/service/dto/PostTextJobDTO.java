package jobforsite.sitejob.module.service.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для передачи данных об объявлении (услуге) на основные страницы.
 * Используется для отображения списка услуг и детальной информации.
 * <p>
 * Содержит основные поля объявления, а также вложенный DTO с минимальной информацией о пользователе.
 * Отличается от PostTextJobBlockedDTO тем, что не содержит флагов блокировки и archived.
 */
@Data
public class PostTextJobDTO {

    // ===== Основные поля объявления =====

    /** Уникальный идентификатор объявления в БД */
    private Long id;

    /** Хэш-идентификатор для безопасных ссылок (не содержит прямой ID) */
    private String hashId;

    /** Заголовок объявления */
    private String title;

    /** Цена услуги */
    private Integer price;

    /** Ключ категории (технический идентификатор) */
    private String category;

    /** Отображаемое название категории для UI */
    private String categoryDisplayName;

    /** Местоположение (текстовое поле) */
    private String location;

    /** Путь к первому изображению объявления (для превью) */
    private String firstImagePath;

    /** Полный текст объявления */
    private String fullText;

    /** Дата и время создания объявления */
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления объявления */
    private LocalDateTime updatedAt;

    /** Дата и время последней модерации */
    private LocalDateTime moderatedAt;

    /** Флаг блокировки объявления */
    private boolean blocked;

    /** Дата и время блокировки объявления */
    private LocalDateTime blockedAt;

    /** Флаг прохождения модерации (true = промодерировано) */
    private boolean moderation;

    /** Флаг архивации объявления */
    private boolean archived;

    /** Информация о пользователе-владельце объявления (вложенный DTO) */
    private UserDTO user;

    /** Количество просмотров объявления */
    private int viewCount;

    /** Флаг платного объявления (поднятие в промо-карусель) */
    private boolean premium = false;

    /** Приоритет показа в промо-карусели (чем выше, тем выше) */
    private int premiumPriority = 0;

    private boolean donate = false;

    /**
     * Внутренний DTO для передачи минимальной информации о пользователе.
     * Содержит только ID, отображаемое имя и логин.
     */
    @Data
    public static class UserDTO {
        /** ID пользователя */
        private Long id;

        /** Отображаемое имя пользователя */
        private String nameuse;

        /** Логин пользователя */
        private String username;
    }

    // ===== Геттеры и сеттеры =====
    // (явно объявлены для полей, которые не покрываются Lombok из-за кастомной логики)

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}