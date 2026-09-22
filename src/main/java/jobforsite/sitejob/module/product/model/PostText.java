package jobforsite.sitejob.module.product.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.interfaceFilter.PostDetailsVisitor;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.model.ImagesFoto;
import jobforsite.sitejob.module.users.model.User;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;

/**
 * Сущность текстового объявления.
 * Содержит основную информацию об объявлении: заголовок, цену, категорию, текст, изображения,
 * а также служебные поля для модерации, блокировки и архивации.
 * Реализует интерфейс BasePostController.PostAccessInfo для контроля доступа.
 */
@Slf4j
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "post_text")
@DiscriminatorColumn(name = "post_type")
public class PostText implements BasePostController.PostAccessInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Автоматическая генерация ID при добавлении записи
    private Long id;
    private String title;

    private Integer price;
    private String category;
    private String location;

    /** Флаг блокировки объявления */
    private boolean blocked = false;

    /** Флаг того, что объявление находится на модерации */
    private boolean moderation = false;

    /** Количество просмотров объявления */
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /** Полный текст объявления */
    @Column(columnDefinition = "TEXT")
    private String full_text;

    /** Дата создания объявления (автоматически) */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Дата последнего обновления (автоматически) */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Дата блокировки объявления */
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    /** Дата прохождения модерации */
    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    /** Флаг архивации объявления */
    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    /** Дата архивации объявления */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /** Список добавлений в избранное */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    /** Уникальный хеш-идентификатор объявления для публичных ссылок */
    @Column(name = "hash_id", unique = true, nullable = false)
    private String hashId;

    /** Автор объявления */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** Список изображений объявления */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "post")
    private List<ImagesFoto> images = new ArrayList<>();

    /** Список обычных комментариев к объявлению */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    /** Список персональных комментариев (личных сообщений) к объявлению */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonal> commentsPersonal = new ArrayList<>();

    /**********************************************
     /** Флаг платного объявления (поднятие в промо-карусель) */
    @Column(name = "is_premium")
    private boolean premium = false;

    @Column(name = "premium_priority", columnDefinition = "int default 0")
    private int premiumPriority = 0;


    @Column(name = "is_donate")
    private boolean donate = false;



    // ========== Конструкторы ==========

    public PostText() {

    }

    /**
     * Конструктор для создания объявления.
     * @param title заголовок
     * @param price цена
     * @param full_text полный текст
     * @param category категория
     * @param location местоположение
     * @param moderation статус модерации
     */
    public PostText(String title, Integer price, String full_text, String category, String location,
                    Boolean moderation) {
        this.title = title;
        this.price = price;
        this.full_text = full_text;
        this.category = category;
        this.location = location;
        this.moderation = moderation;
    }

    // ========== Бизнес-методы ==========

    /**
     * Сбрасывает статусы объявления для повторной модерации.
     * Очищает блокировку и архив, устанавливает статус модерации.
     */
    public void resetForModeration() {
        this.blocked = false;
        this.blockedAt = null;
        this.moderation = true;
        this.archived = false;
        this.archivedAt = null;
    }

    /**
     * Публикует объявление после редактирования администратором.
     * Снимает модерацию, убирает из архива, обновляет дату модерации.
     */
    public void publishAfterAdminEdit() {
        this.blocked = false;
        this.blockedAt = null;
        this.moderation = false;
        this.archived = false;
        this.archivedAt = null;
        this.moderatedAt = LocalDateTime.now();
        log.debug("Объявление опубликовано после админского редактирования, hashId: {}", hashId);

    }


    // ===== АБСТРАКТНЫЙ МЕТОД - ВИЗИТОР =====
    //public abstract <T> T accept(PostDetailsVisitor<T> visitor);

    public <T> T accept(PostDetailsVisitor<T> visitor) {
        return visitor.visitDefault(this);
    }

    /**
     * Возвращает путь к первому валидному изображению объявления.
     * @return путь к первому изображению или null, если изображений нет
     */
    public String getFirstImagePath() {
        if (images != null && !images.isEmpty()) {
            // Фильтруем изображения с null путями
            List<ImagesFoto> validImages = images.stream()
                    .filter(img -> img.getImagePath() != null && !img.getImagePath().isEmpty())
                    .collect(Collectors.toList());

            if (!validImages.isEmpty()) {
                return validImages.get(0).getImagePath();
            }
        }
        return null;
    }

    /**
     * Генерирует уникальный hashId перед сохранением в базу данных.
     * Использует UUID.randomUUID().
     */
    @PrePersist
    public void generateHashId() {
        if (this.hashId == null) {
            this.hashId = UUID.randomUUID().toString();
        }
    }

    // ========== Геттеры и сеттеры ==========

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<ImagesFoto> getImages() {
        return images;
    }

    public void setImages(List<ImagesFoto> images) {
        this.images = images;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public boolean isModeration() {
        return moderation;
    }

    public void setModeration(boolean moderation) {
        this.moderation = moderation;
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

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<CommentPersonal> getCommentsPersonal() {
        return commentsPersonal;
    }

    public void setCommentsPersonal(List<CommentPersonal> commentsPersonal) {
        this.commentsPersonal = commentsPersonal;
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