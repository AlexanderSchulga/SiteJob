package jobforsite.sitejob.module.shared.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность для хранения отзывов пользователей.
 * <p>
 * Поддерживает иерархическую структуру (отзыв + ответы на него).
 * Сохраняет копии данных о пользователях (авторе и том, на кого оставлен отзыв)
 * на момент написания отзыва, что позволяет корректно отображать информацию
 * даже после удаления учётных записей.
 * </p>
 *
 * @see User
 */
@Entity
@Table(name = "reviews")
public class Reviews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Информация о пользователе, на которого оставлен отзыв ===
    // Эти поля сохраняются даже после удаления пользователя и остаются неизменными

    @Column(nullable = false)
    private Long reviewedUserId;

    @Column(nullable = false)
    private String reviewedUsername;

    @Column(nullable = false)
    private String reviewedNameuse;

    private String reviewedUserEmail;
    private String reviewedUserPhonePost;

    // === Информация об авторе отзыва ===

    /**
     * Автор отзыва (может стать null, если пользователь удалён).
     * Связь ленивая для оптимизации производительности.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    // Сохраняем информацию об авторе на момент написания отзыва
    private String authorUsername;
    private String authorNameuse;
    private String authorEmail;

    /**
     * Текст отзыва.
     */
    @Column(columnDefinition = "TEXT")
    private String text;

    /**
     * Дата и время создания отзыва. Устанавливается автоматически.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    // === Иерархическая структура отзывов ===

    /**
     * Родительский отзыв (если текущий отзыв является ответом на другой отзыв).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_review_id")
    private Reviews parentReview;

    /**
     * Хеш-идентификатор пользователя, на которого оставлен отзыв.
     * Используется для дополнительной анонимизации или внешних ссылок.
     */
    @Column(name = "reviewed_user_hash_id")
    private String reviewedUserHashId;

    /**
     * Список ответов на данный отзыв.
     * При удалении родительского отзыва все ответы удаляются каскадно.
     */
    @OneToMany(mappedBy = "parentReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reviews> replies = new ArrayList<>();

    // === Флаги удаления пользователей ===

    /**
     * Флаг, указывающий, что автор отзыва был удалён из системы.
     * Позволяет корректно отображать информацию об авторе.
     */
    private boolean authorDeleted = false;

    /**
     * Флаг, указывающий, что пользователь, на которого оставлен отзыв, был удалён.
     */
    private boolean reviewedUserDeleted = false;

    // === Конструкторы ===

    public Reviews() {}

    /**
     * Конструктор для создания отзыва с полной информацией о пользователях.
     *
     * @param reviewedUser пользователь, на которого оставляют отзыв
     * @param author       автор отзыва
     * @param text         текст отзыва
     */
    public Reviews(User reviewedUser, User author, String text) {
        this.reviewedUserId = reviewedUser.getId();
        this.reviewedUsername = reviewedUser.getUsername();
        this.reviewedNameuse = reviewedUser.getNameuse();
        this.reviewedUserEmail = reviewedUser.getEmail();
        this.reviewedUserPhonePost = reviewedUser.getPhonePost();
        this.author = author;
        this.authorUsername = author.getUsername();
        this.authorNameuse = author.getNameuse();
        this.authorEmail = author.getEmail();
        this.text = text;
    }

    // === Геттеры и сеттеры ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReviewedUserId() { return reviewedUserId; }
    public void setReviewedUserId(Long reviewedUserId) { this.reviewedUserId = reviewedUserId; }

    public String getReviewedUsername() { return reviewedUsername; }
    public void setReviewedUsername(String reviewedUsername) { this.reviewedUsername = reviewedUsername; }

    public String getReviewedUserEmail() { return reviewedUserEmail; }
    public void setReviewedUserEmail(String reviewedUserEmail) { this.reviewedUserEmail = reviewedUserEmail; }

    public String getReviewedUserPhonePost() { return reviewedUserPhonePost; }
    public void setReviewedUserPhonePost(String reviewedUserPhonePost) { this.reviewedUserPhonePost = reviewedUserPhonePost; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Reviews getParentReview() { return parentReview; }
    public void setParentReview(Reviews parentReview) { this.parentReview = parentReview; }

    public List<Reviews> getReplies() { return replies; }
    public void setReplies(List<Reviews> replies) { this.replies = replies; }

    public boolean isAuthorDeleted() { return authorDeleted; }
    public void setAuthorDeleted(boolean authorDeleted) { this.authorDeleted = authorDeleted; }

    public boolean isReviewedUserDeleted() { return reviewedUserDeleted; }
    public void setReviewedUserDeleted(boolean reviewedUserDeleted) { this.reviewedUserDeleted = reviewedUserDeleted; }

    public String getReviewedNameuse() {
        return reviewedNameuse;
    }

    public void setReviewedNameuse(String reviewedNameuse) {
        this.reviewedNameuse = reviewedNameuse;
    }

    public String getAuthorNameuse() {
        return authorNameuse;
    }

    public void setAuthorNameuse(String authorNameuse) {
        this.authorNameuse = authorNameuse;
    }

    public String getReviewedUserHashId() {
        return reviewedUserHashId;
    }

    public void setReviewedUserHashId(String reviewedUserHashId) {
        this.reviewedUserHashId = reviewedUserHashId;
    }

    // === Вспомогательные методы для отображения имени (с учётом удаления) ===

    /**
     * Возвращает отображаемое имя автора отзыва.
     * Если автор удалён, добавляется пометка "(пользователь удален)".
     *
     * @return отображаемое имя автора
     */
    public String getDisplayAuthorName() {
        if (authorDeleted) {
            return authorUsername + " (пользователь удален)";
        }
        return authorUsername;
    }

    public String getDisplayAuthorNameuse() {
        if (authorDeleted) {
            return authorNameuse + " (пользователь удален)";
        }
        return authorNameuse;
    }

    /**
     * Возвращает отображаемое имя пользователя, на которого оставлен отзыв.
     * Если пользователь удалён, добавляется пометка "(пользователь удален)".
     *
     * @return отображаемое имя пользователя
     */
    public String getDisplayReviewedUserName() {
        if (reviewedUserDeleted) {
            return reviewedUsername + " (пользователь удален)";
        }
        return reviewedUsername;
    }
}