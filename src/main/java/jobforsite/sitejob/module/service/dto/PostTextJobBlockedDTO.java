package jobforsite.sitejob.module.service.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для передачи данных о заблокированном объявлении (услуге).
 * Используется для отображения информации о заблокированных постах в админ-панели.
 * <p>
 * Содержит как информацию о самом объявлении, так и вложенный DTO с данными пользователя-владельца.
 */
@Data
public class PostTextJobBlockedDTO {

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

    /** Дата и время последней модерации */
    private LocalDateTime moderatedAt;

    /** Дата и время последнего обновления объявления */
    private LocalDateTime updatedAt;

    /** Флаг блокировки объявления */
    private boolean blocked;

    /** Дата и время блокировки объявления */
    private LocalDateTime blockedAt;

    /** Флаг прохождения модерации (true = промодерировано) */
    private boolean moderation;

    /** Флаг архивации объявления */
    private boolean archived;

    /** Флаг платного объявления (поднятие в промо-карусель) */
    private boolean premium = false;

    /** Приоритет показа в промо-карусели (чем выше, тем выше) */
    private int premiumPriority = 0;


    private boolean donate = false;

    /** Информация о пользователе-владельце объявления (вложенный DTO) */
    private UserDTO user;

    /**
     * Внутренний DTO для передачи данных о пользователе-владельце заблокированного объявления.
     * Содержит только необходимые для отображения поля без чувствительной информации.
     */
    @Data
    public static class UserDTO {
        /** ID пользователя */
        private Long id;

        /** Логин пользователя */
        private String username;

        /** Отображаемое имя пользователя */
        private String nameuse;

        /** Контактный телефон (публичная информация) */
        private String phonePost;

        /** Ключ региона пользователя */
        private String region;

        /** Название города пользователя */
        private String city;

        /** Отображаемое название региона для UI */
        private String regionDisplayName;

        // Геттеры и сеттеры (добавлены для явного указания, хотя Lombok @Data их генерирует)
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNameuse() { return nameuse; }
        public void setNameuse(String nameuse) { this.nameuse = nameuse; }

        public String getPhonePost() { return phonePost; }
        public void setPhonePost(String phonePost) { this.phonePost = phonePost; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getRegionDisplayName() { return regionDisplayName; }
        public void setRegionDisplayName(String regionDisplayName) { this.regionDisplayName = regionDisplayName; }
    }

    // ===== Геттеры и сеттеры основного DTO =====
    // (явно объявлены, хотя Lombok @Data их генерирует — оставлено для совместимости)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHashId() { return hashId; }
    public void setHashId(String hashId) { this.hashId = hashId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryDisplayName() { return categoryDisplayName; }
    public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFirstImagePath() { return firstImagePath; }
    public void setFirstImagePath(String firstImagePath) { this.firstImagePath = firstImagePath; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(LocalDateTime moderatedAt) { this.moderatedAt = moderatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }

    public boolean isModeration() { return moderation; }
    public void setModeration(boolean moderation) { this.moderation = moderation; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}