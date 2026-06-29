package jobforsite.sitejob.module.service.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность приватного комментария (личного сообщения) к объявлению.
 * Отличается от {@link CommentJob} наличием получателя (recipient)
 * и флагом прочтения (isRead).
 * <p>
 * Поддерживает иерархическую структуру (родительский комментарий и ответы),
 * что позволяет организовать переписку в виде диалогов (threads).
 * <p>
 * Связи:
 * - ManyToOne с User (отправитель, автор комментария)
 * - ManyToOne с User (получатель)
 * - ManyToOne с PostTextJob (объявление, к которому относится сообщение)
 * - Self-referencing для поддержки вложенных ответов
 */
@Entity
public class CommentPersonalJob {

    private static final Logger log = LoggerFactory.getLogger(CommentPersonalJob.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Текст сообщения (обязательное поле) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Дата и время создания сообщения (заполняется автоматически) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Отправитель сообщения (автор) */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Объявление, к которому относится сообщение */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private PostTextJob postjob;

    /** Получатель сообщения */
    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    /** Родительский комментарий (для поддержки цепочек диалога) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentPersonalJob parentComment;

    /** Список ответов на данный комментарий */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalJob> replies = new ArrayList<>();

    /** Флаг прочтения сообщения получателем */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // ===== Конструкторы =====

    /** Конструктор по умолчанию (требуется JPA) */
    public CommentPersonalJob() {
        log.debug("Created new CommentPersonalJob instance");
    }

    /**
     * Конструктор для быстрого создания приватного сообщения.
     *
     * @param text           текст сообщения
     * @param user           отправитель
     * @param postjob        объявление
     * @param recipient      получатель
     * @param parentComment  родительский комментарий (может быть null)
     */
    public CommentPersonalJob(String text, User user, PostTextJob postjob, User recipient, CommentPersonalJob parentComment) {
        log.debug("Creating CommentPersonalJob: postId={}, recipientId={}",
                postjob != null ? postjob.getId() : null,
                recipient != null ? recipient.getId() : null);
        this.text = text;
        this.user = user;
        this.postjob = postjob;
        this.recipient = recipient;
        this.parentComment = parentComment;
    }

    // ===== Геттеры и сеттеры =====

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

    public PostTextJob getPostjob() {
        return postjob;
    }

    public void setPostjob(PostTextJob postjob) {
        this.postjob = postjob;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public CommentPersonalJob getParentComment() {
        return parentComment;
    }

    public void setParentComment(CommentPersonalJob parentComment) {
        this.parentComment = parentComment;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public List<CommentPersonalJob> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentPersonalJob> replies) {
        this.replies = replies;
    }
}