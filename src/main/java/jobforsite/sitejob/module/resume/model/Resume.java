package jobforsite.sitejob.module.resume.model;

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
 * Сущность резюме.
 * Содержит информацию о вакансии/резюме пользователя, включая текст, категорию, опыт, изображения.
 * Реализует интерфейс PostAccessInfo для контроля доступа к посту.
 */
@Entity
public class Resume implements BasePostController.PostAccessInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Заголовок резюме.
     */
    private String title;

    /**
     * Размер вознаграждения/зарплаты.
     */
    private Integer cash;

    /**
     * Категория резюме.
     */
    private String category;

    /**
     * Опыт работы.
     */
    private String experience;


    /**
     * Формат работы.
     */
    private String workformat;

    /**
     * Флаг блокировки резюме.
     */
    private boolean blocked = false;

    /**
     * Флаг нахождения на модерации.
     */
    private boolean moderation = false;

    /**
     * Количество просмотров резюме.
     */
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /**
     * Полный текст резюме.
     */
    @Column(columnDefinition = "TEXT")
    private String full_text;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Дата блокировки резюме.
     */
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    /**
     * Дата прохождения модерации.
     */
    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    /**
     * Флаг архивации резюме.
     */
    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;

    /**
     * Дата архивации.
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    /**
     * Список добавлений в избранное.
     */
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    /**
     * Уникальный хеш-идентификатор резюме.
     */
    @Column(unique = true, nullable = false)
    private String hashId;

    /**
     * Автор резюме.
     */
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Список изображений резюме.
     */
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "resume")
    private List<ImagesFoto> imagesresume = new ArrayList<>();

    /**
     * Список комментариев к резюме.
     */
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalRes> commentsPersonaRes = new ArrayList<>();




    public Resume() {}

    /**
     * Конструктор для создания резюме.
     *
     * @param title      заголовок
     * @param cash       вознаграждение
     * @param full_text  полный текст
     * @param category   категория
     * @param experience опыт
     * @param moderation статус модерации
     */
    public Resume(String title, Integer cash, String full_text, String category, String experience, String workformat,
                  Boolean moderation) {
        this.title = title;
        this.cash = cash;
        this.category = category;
        this.experience = experience;
        this.workformat = workformat;
        this.moderation = moderation;
        this.full_text = full_text;

    }

    /**
     * Сбрасывает статусы резюме для повторной модерации.
     * Снимает блокировку, устанавливает статус модерации, снимает архивацию.
     */
    public void resetForModeration() {
        this.blocked = false;
        this.blockedAt = null;
        this.moderation = true;
        this.archived = false;
        this.archivedAt = null;
    }

    /**
     * Возвращает путь к первому валидному изображению резюме.
     *
     * @return путь к первому изображению или null, если изображений нет
     */
    public String getFirstImagePath() {
        if (imagesresume != null && !imagesresume.isEmpty()) {
            List<ImagesFoto> validImages = imagesresume.stream()
                    .filter(img -> img.getImagePath() != null && !img.getImagePath().isEmpty())
                    .collect(Collectors.toList());

            if (!validImages.isEmpty()) {
                return validImages.get(0).getImagePath();
            }
        }
        return null;
    }

    /**
     * Генерирует уникальный hashId перед сохранением сущности.
     */
    @PrePersist
    public void generateHashId() {
        if (this.hashId == null) {
            this.hashId = UUID.randomUUID().toString();
        }
    }

    // ===== Геттеры и сеттеры =====

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

    public List<ImagesFoto> getImages() {
        return imagesresume;
    }

    public void setImages(List<ImagesFoto> images) {
        this.imagesresume = images;
    }

    public List<ImagesFoto> getImagesresume() {
        return imagesresume;
    }

    public void setImagesresume(List<ImagesFoto> imagesresume) {
        this.imagesresume = imagesresume;
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

    public List<CommentPersonalRes> getCommentsPersonaRes() {
        return commentsPersonaRes;
    }

    public void setCommentsPersonaRes(List<CommentPersonalRes> commentsPersonaRes) {
        this.commentsPersonaRes = commentsPersonaRes;
    }

    public String getWorkformat() {
        return workformat;
    }

    public void setWorkformat(String workformat) {
        this.workformat = workformat;
    }
}