package jobforsite.sitejob.module.product.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность персонального комментария (личное сообщение пользователю).
 * Отличается от обычного комментария наличием получателя (recipient).
 * Поддерживает иерархическую структуру ответов.
 */
@Entity
public class CommentPersonal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Автор комментария */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Объявление, к которому относится комментарий */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private PostText post;

    /** Получатель комментария (кому адресовано сообщение) */
    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    /** Родительский комментарий (для ответов) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentPersonal parentComment;

    /** Список ответов на данный комментарий */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonal> replies = new ArrayList<>();

    /** Флаг, прочитан ли комментарий получателем */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // ========== Конструкторы ==========

    public CommentPersonal() {}

    /**
     * Конструктор для создания персонального комментария.
     * @param text текст комментария
     * @param user автор
     * @param post объявление
     * @param recipient получатель
     * @param parentComment родительский комментарий (может быть null)
     */
    public CommentPersonal(String text, User user, PostText post, User recipient, CommentPersonal parentComment) {
        this.text = text;
        this.user = user;
        this.post = post;
        this.recipient = recipient;
        this.parentComment = parentComment;
    }

    // ========== Геттеры и сеттеры ==========

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PostText getPost() {
        return post;
    }

    public void setPost(PostText post) {
        this.post = post;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public CommentPersonal getParentComment() {
        return parentComment;
    }

    public void setParentComment(CommentPersonal parentComment) {
        this.parentComment = parentComment;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public List<CommentPersonal> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentPersonal> replies) {
        this.replies = replies;
    }
}