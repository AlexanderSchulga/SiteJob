package jobforsite.sitejob.module.product.dto;

import jakarta.persistence.Column;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO для передачи данных текстового объявления.
 * Используется в API для отображения основной информации об объявлении.
 */
@Data
public class PostTextDTO {
    private Long id;
    private String hashId;
    private String title;
    private Integer price;
    private String category;
    private String categoryDisplayName;
    private String location;
    private String firstImagePath;
    private String fullText;
    private LocalDateTime createdAt;

    private LocalDateTime moderatedAt;
    private LocalDateTime updatedAt;

    /** Флаг блокировки объявления */
    private boolean blocked;

    /** Дата и время блокировки объявления */
    private LocalDateTime blockedAt;

    /** Флаг того, что объявление находится на модерации */
    private boolean moderation;

    /** Флаг архивации объявления */
    private boolean archived;

    /** Информация о пользователе, опубликовавшем объявление */
    private UserDTO user;

    /** Количество просмотров объявления */
    private int viewCount;

    /** Флаг платного объявления (поднятие в промо-карусель) */

    private boolean premium = false;

    /** Приоритет показа в промо-карусели (чем выше, тем выше) */

    private int premiumPriority = 0;


    private boolean donate = false;

    /**
     * Внутренний DTO для данных пользователя.
     * Содержит базовую информацию об авторе объявления.
     */
    @Data
    public static class UserDTO {
        private Long id;
        private String nameuse;
        private String username;
    }

    // ========== Геттеры и сеттеры ==========

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
}