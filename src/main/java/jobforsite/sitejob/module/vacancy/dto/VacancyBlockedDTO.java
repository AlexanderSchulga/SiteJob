package jobforsite.sitejob.module.vacancy.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для передачи данных о заблокированной вакансии.
 * Используется для отображения информации о вакансии в состоянии блокировки,
 * включая данные автора вакансии.
 */
@Data
public class VacancyBlockedDTO {

    /** Идентификатор вакансии в базе данных */
    private Long id;

    /** Хэш-идентификатор для безопасного доступа к вакансии через URL */
    private String hashId;

    /** Заголовок вакансии */
    private String title;

    /** Местоположение (город/регион) вакансии */
    private String location;

    /** Размер оплаты или зарплата */
    private Integer cash;

    /** Технический идентификатор категории */
    private String category;

    private String experience;

    private String workformat;

    private String experienDisplayName;


    private String workformatDisplayName;

    /** Отображаемое имя категории для пользователя */
    private String categoryDisplayName;

    /** Путь к первому изображению вакансии (для превью) */
    private String firstImagePath;

    /** Полный текст описания вакансии */
    private String fullText;

    /** Дата и время создания вакансии */
    private LocalDateTime createdAt;

    /** Дата и время последней модерации */
    private LocalDateTime moderatedAt;

    /** Дата и время последнего обновления */
    private LocalDateTime updatedAt;

    /** Флаг блокировки вакансии */
    private boolean blocked;

    /** Дата и время блокировки вакансии */
    private LocalDateTime blockedAt;

    /** Флаг прохождения модерации */
    private boolean moderation;

    /** Флаг архивации вакансии */
    private boolean archived;

    /** Флаг платного объявления (поднятие в промо-карусель) */
    private boolean premium = false;

    /** Приоритет показа в промо-карусели (чем выше, тем выше) */
    private int premiumPriority = 0;

    private boolean donate = false;

    /** Информация об авторе вакансии */
    private UserDTO user;

    /**
     * DTO для передачи информации об авторе заблокированной вакансии.
     * Содержит только необходимые для отображения поля.
     */
    @Data
    public static class UserDTO {
        /** Идентификатор пользователя */
        private Long id;

        /** Логин пользователя */
        private String username;

        /** Отображаемое имя пользователя */
        private String nameuse;

        /** Контактный телефон (публикуется в вакансии) */
        private String phonePost;

        /** Регион пользователя (технический идентификатор) */
        private String region;

        /** Город пользователя */
        private String city;

        /** Отображаемое имя региона */
        private String regionDisplayName;

        // === Геттеры и сеттеры с явной реализацией ===

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

    // === Геттеры и сеттеры VacancyBlockedDTO ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHashId() { return hashId; }
    public void setHashId(String hashId) { this.hashId = hashId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getCash() { return cash; }
    public void setCash(Integer cash) { this.cash = cash; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryDisplayName() { return categoryDisplayName; }
    public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getExperienDisplayName() { return experienDisplayName; }
    public void setExperienDisplayName(String experienDisplayName) { this.experienDisplayName = experienDisplayName; }

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


    public String getWorkformat() {
        return workformat;
    }

    public void setWorkformat(String workformat) {
        this.workformat = workformat;
    }

    public String getWorkformatDisplayName() {
        return workformatDisplayName;
    }

    public void setWorkformatDisplayName(String workformatDisplayName) {
        this.workformatDisplayName = workformatDisplayName;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public int getPremiumPriority() {
        return premiumPriority;
    }

    public void setPremiumPriority(int premiumPriority) {
        this.premiumPriority = premiumPriority;
    }

    public boolean isDonate() {
        return donate;
    }

    public void setDonate(boolean donate) {
        this.donate = donate;
    }
}