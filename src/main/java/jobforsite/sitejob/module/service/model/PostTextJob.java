package jobforsite.sitejob.module.service.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BasePostController;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.model.ImagesFoto;
import jobforsite.sitejob.module.users.model.User;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сущность текстового объявления о работе.
 * Хранит информацию о вакансии: заголовок, цену, категорию, местоположение,
 * текст объявления, статусы модерации, блокировки, архивности.
 * Поддерживает связи с изображениями, комментариями, избранным и пользователем.
 */
@Slf4j
@Entity
public class PostTextJob implements BasePostController.PostAccessInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer price;
    private String category;
    private String location;
    private boolean blocked = false;
    private boolean moderation = false;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(columnDefinition = "TEXT")
    private String full_text;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(unique = true, nullable = false)
    private String hashId;

    @OneToMany(mappedBy = "postJob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "postjob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentJob> comments = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "postjob")
    private List<ImagesFoto> images = new ArrayList<>();

    @OneToMany(mappedBy = "postjob", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalJob> commentsPersonalJob = new ArrayList<>();

    @Column(name = "is_premium")
    private boolean premium = false;

    @Column(name = "premium_priority", columnDefinition = "int default 0")
    private int premiumPriority = 0;

    @Column(name = "is_donate")
    private boolean donate = false;

    public PostTextJob() {

    }

    /**
     * Конструктор для создания нового объявления без изображений.
     *
     * @param title      заголовок вакансии
     * @param price      оплата
     * @param full_text  полное описание
     * @param category   категория работы
     * @param location   местоположение
     * @param moderation требуется ли модерация (обычно true для новых)
     */
    public PostTextJob(String title, Integer price, String full_text, String category, String location, Boolean moderation) {
        this.title = title;
        this.price = price;
        this.full_text = full_text;
        this.category = category;
        this.location = location;
        this.moderation = moderation;
        log.debug("Создан новый объект PostTextJob: title={}, category={}, location={}", title, category, location);
    }

    /**
     * Сбрасывает статусы объявления перед повторной модерацией.
     * Снимает блокировку, помечает на модерацию, снимает архивность.
     */
    public void resetForModeration() {
        this.blocked = false;
        this.blockedAt = null;
        this.moderation = true;
        this.archived = false;
        this.archivedAt = null;
        log.debug("Сброшены статусы модерации для объявления с hashId: {}", hashId);
    }

    public void publishAfterAdminEdit() {
        this.blocked = false;
        this.blockedAt = null;
        this.moderation = false;
        this.archived = false;
        this.archivedAt = null;
        this.moderatedAt = LocalDateTime.now();
        log.debug("Объявление опубликовано после админского редактирования, hashId: {}", hashId);
    }

    /**
     * Возвращает путь к первому валидному изображению объявления.
     *
     * @return путь к первому изображению или null, если изображений нет
     */
    public String getFirstImagePath() {
        if (images != null && !images.isEmpty()) {
            // Фильтруем изображения с null или пустыми путями
            List<ImagesFoto> validImages = images.stream()
                    .filter(img -> img.getImagePath() != null && !img.getImagePath().isEmpty())
                    .collect(Collectors.toList());

            if (!validImages.isEmpty()) {
                log.trace("Получен путь к первому изображению для объявления hashId: {}", hashId);
                return validImages.get(0).getImagePath();
            }
        }
        log.trace("Изображения отсутствуют или невалидны для объявления hashId: {}", hashId);
        return null;
    }

    /**
     * Генерирует уникальный hashId перед сохранением в БД, если он ещё не установлен.
     */
    @PrePersist
    public void generateHashId() {
        if (this.hashId == null) {
            this.hashId = UUID.randomUUID().toString();
            log.debug("Сгенерирован hashId для нового объявления: {}", hashId);
        }
    }

    // Геттеры и сеттеры

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
        if (blocked) {
            log.info("Объявление заблокировано, hashId: {}", hashId);
        } else {
            log.debug("Снята блокировка с объявления, hashId: {}", hashId);
        }
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
        if (archived) {
            log.info("Объявление архивировано, hashId: {}", hashId);
        }
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

    public List<CommentJob> getComments() {
        return comments;
    }

    public void setComments(List<CommentJob> comments) {
        this.comments = comments;
    }

    public List<CommentPersonalJob> getCommentsPersonalJob() {
        return commentsPersonalJob;
    }

    public void setCommentsPersonalJob(List<CommentPersonalJob> commentsPersonalJob) {
        this.commentsPersonalJob = commentsPersonalJob;
    }
}