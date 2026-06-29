package jobforsite.sitejob.module.vacancy.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.model.ImagesFoto;
import jobforsite.sitejob.module.users.model.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сущность (Entity) вакансии — основная модель для хранения информации о вакансии.
 * Реализует интерфейс PostAccessInfo для унифицированной проверки доступа к посту.
 * Поддерживает:
 * - Модерацию и блокировку
 * - Архивацию
 * - Систему комментариев (публичных и приватных)
 * - Загрузку изображений
 * - Добавление в избранное
 */
@Entity
public class Vacancy implements BasePostController.PostAccessInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Заголовок вакансии */
    private String title;

    /** Местоположение (город/регион) */
    private String location;

    /** Размер оплаты или зарплата */
    private Integer cash;

    /** Категория вакансии (технический идентификатор) */
    private String category;

    /**
     * Опыт работы.
     */
    private String experience;


    /**
     * Формат работы.
     */
    private String workformat;

    /** Флаг блокировки вакансии (скрыта от публичного просмотра) */
    private boolean blocked = false;

    /** Флаг модерации (true - прошла модерацию и опубликована) */
    private boolean moderation = false;

    /** Количество просмотров вакансии */
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /** Полный текст описания вакансии */
    @Column(columnDefinition = "TEXT")
    private String full_text;

    /** Дата и время создания */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Дата и время блокировки */
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    /** Дата и время последней модерации */
    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    /** Флаг архивации (скрыта, но не удалена) */
    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    /** Дата и время архивации */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /** Список добавлений в избранное, связанных с этой вакансией */
    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    /** Уникальный хэш-идентификатор для безопасного доступа через URL */
    @Column(unique = true, nullable = false)
    private String hashId;

    /** Автор вакансии (пользователь, создавший её) */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** Список изображений, прикреплённых к вакансии */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "vacancy")
    private List<ImagesFoto> imagesvacancy = new ArrayList<>();

    /** Публичные комментарии к вакансии */
    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentVacancy> commentsvacancy = new ArrayList<>();

    /** Приватные (личные) комментарии к вакансии */
    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalVac> commentsPersonalVac = new ArrayList<>();

    @Column(name = "is_premium")
    private boolean premium = false;

    @Column(name = "premium_priority", columnDefinition = "int default 0")
    private int premiumPriority = 0;


    @Column(name = "is_donate")
    private boolean donate = false;

    // === Конструкторы ===

    public Vacancy() {
    }

    /**
     * Конструктор для создания новой вакансии.
     *
     * @param title      заголовок
     * @param cash       оплата
     * @param full_text  полный текст
     * @param category   категория
     * @param experience опыт
     * @param location   местоположение
     * @param moderation статус модерации
     */
    public Vacancy(String title, Integer cash, String full_text, String category, String experience, String workformat,
                   String location, Boolean moderation) {
        this.title = title;
        this.cash = cash;
        this.full_text = full_text;
        this.category = category;
        this.experience = experience;
        this.workformat = workformat;
        this.location = location;
        this.moderation = moderation;

    }

    // === Бизнес-методы ===

    /**
     * Сбрасывает статусы вакансии для повторной модерации.
     * Снимает блокировку, устанавливает статус модерации,
     * сбрасывает флаг архивации.
     */
    public void resetForModeration() {
        this.blocked = false;
        this.blockedAt = null;
        this.moderation = true;
        this.archived = false;
        this.archivedAt = null;
    }

    /**
     * Возвращает путь к первому валидному изображению вакансии.
     * Используется для отображения превью в списках.
     *
     * @return путь к первому изображению или null, если изображений нет
     */
    public String getFirstImagePath() {
        if (imagesvacancy != null && !imagesvacancy.isEmpty()) {
            List<ImagesFoto> validImages = imagesvacancy.stream()
                    .filter(img -> img.getImagePath() != null && !img.getImagePath().isEmpty())
                    .collect(Collectors.toList());

            if (!validImages.isEmpty()) {
                return validImages.get(0).getImagePath();
            }
        }
        return null;
    }

    /**
     * Генерирует уникальный хэш-идентификатор перед сохранением в БД.
     * Выполняется автоматически при первом сохранении сущности.
     */
    @PrePersist
    public void generateHashId() {
        if (this.hashId == null) {
            this.hashId = UUID.randomUUID().toString();
        }
    }

    // === Геттеры и сеттеры ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCash() {
        return cash;
    }

    public void setCash(Integer cash) {
        this.cash = cash;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isModeration() {
        return moderation;
    }

    public void setModeration(boolean moderation) {
        this.moderation = moderation;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public String getFull_text() {
        return full_text;
    }

    public void setFull_text(String full_text) {
        this.full_text = full_text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(LocalDateTime moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public List<Favorites> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<Favorites> favorites) {
        this.favorites = favorites;
    }

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ImagesFoto> getImagesvacancy() {
        return imagesvacancy;
    }

    public void setImagesvacancy(List<ImagesFoto> imagesvacancy) {
        this.imagesvacancy = imagesvacancy;
    }

    public List<ImagesFoto> getImages() {
        return imagesvacancy;
    }

    public void setImages(List<ImagesFoto> images) {
        this.imagesvacancy = images;
    }

    public List<CommentVacancy> getCommentsvacancy() {
        return commentsvacancy;
    }

    public void setCommentsvacancy(List<CommentVacancy> commentsvacancy) {
        this.commentsvacancy = commentsvacancy;
    }

    public List<CommentPersonalVac> getCommentsPersonalVac() {
        return commentsPersonalVac;
    }

    public void setCommentsPersonalVac(List<CommentPersonalVac> commentsPersonalVac) {
        this.commentsPersonalVac = commentsPersonalVac;
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

    public String getWorkformat() {
        return workformat;
    }

    public void setWorkformat(String workformat) {
        this.workformat = workformat;
    }
}