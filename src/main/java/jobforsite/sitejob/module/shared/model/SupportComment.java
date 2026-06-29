package jobforsite.sitejob.module.shared.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность для хранения комментариев в системе поддержки.
 * <p>
 * Поддерживает иерархическую структуру (комментарий + ответы на него),
 * категории обращения, статус прочтения, а также возможность назначения
 * комментария супервайзеру для обработки.
 * </p>
 *
 * @see User
 */
@Entity
@Table(name = "support_comments")
public class SupportComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Текст комментария. Обязательное поле.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /**
     * Дата и время создания комментария. Устанавливается автоматически.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Пользователь, оставивший комментарий. Обязательное поле.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Родительский комментарий (если текущий комментарий является ответом).
     * Загрузка ленивая для оптимизации производительности.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private SupportComment parentComment;

    /**
     * Список ответов на данный комментарий.
     * При удалении родительского комментария все ответы удаляются каскадно.
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupportComment> replies = new ArrayList<>();

    /**
     * Флаг, указывающий, был ли комментарий прочитан.
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * Категория обращения в службу поддержки.
     * Возможные значения: "account", "ads", "users", "complaints", "suggestions"
     */
    @Column(name = "category", nullable = false)
    private String category;

    /**
     * Супервайзер, назначенный для обработки комментария.
     * Загрузка ленивая.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_super_id")
    private User assignedSuper;

    /**
     * Дата и время назначения комментария супервайзеру.
     */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // === Конструкторы ===

    public SupportComment() {}

    /**
     * Конструктор для создания комментария поддержки.
     *
     * @param text           текст комментария
     * @param user           автор комментария
     * @param parentComment  родительский комментарий (может быть null для корневых комментариев)
     * @param category       категория обращения
     */
    public SupportComment(String text, User user, SupportComment parentComment, String category) {
        this.text = text;
        this.user = user;
        this.parentComment = parentComment;
        this.category = category;
    }

    // === Геттеры и сеттеры ===

    public Long getId() { return id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SupportComment getParentComment() { return parentComment; }
    public void setParentComment(SupportComment parentComment) { this.parentComment = parentComment; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<SupportComment> getReplies() {
        return replies;
    }

    public void setReplies(List<SupportComment> replies) {
        this.replies = replies;
    }

    public User getAssignedSuper() { return assignedSuper; }
    public void setAssignedSuper(User assignedSuper) { this.assignedSuper = assignedSuper; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
}