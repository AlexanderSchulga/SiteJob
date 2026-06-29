package jobforsite.sitejob.module.resume.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность комментария к резюме.
 * Поддерживает древовидную структуру (комментарии и ответы на них).
 */
@Entity
public class CommentPersonalRes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Автор комментария.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Резюме, к которому оставлен комментарий.
     */
    @ManyToOne
    @JoinColumn(name = "res_id", nullable = false)
    private Resume resume;

    /**
     * Получатель комментария (пользователь, которому адресован ответ).
     */
    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    /**
     * Родительский комментарий (для ответов).
     * Если null - комментарий верхнего уровня.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentPersonalRes parentComment;

    /**
     * Список ответов на данный комментарий.
     */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalRes> replies = new ArrayList<>();

    /**
     * Статус прочтения комментария получателем.
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public CommentPersonalRes() {}

    /**
     * Конструктор для создания комментария.
     *
     * @param text          текст комментария
     * @param user          автор
     * @param resume        резюме
     * @param recipient     получатель
     * @param parentComment родительский комментарий
     */
    public CommentPersonalRes(String text, User user, Resume resume, User recipient, CommentPersonalRes parentComment) {
        this.text = text;
        this.user = user;
        this.resume = resume;
        this.recipient = recipient;
        this.parentComment = parentComment;
    }

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

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public CommentPersonalRes getParentComment() {
        return parentComment;
    }

    public void setParentComment(CommentPersonalRes parentComment) {
        this.parentComment = parentComment;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public List<CommentPersonalRes> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentPersonalRes> replies) {
        this.replies = replies;
    }
}