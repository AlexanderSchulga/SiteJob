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
 * Сущность комментария к объявлению (услуге).
 * Поддерживает иерархическую структуру: комментарии могут иметь родительские и дочерние ответы.
 * <p>
 * Связи:
 * - ManyToOne с User (автор комментария)
 * - ManyToOne с PostTextJob (объявление, к которому оставлен комментарий)
 * - Self-referencing для поддержки вложенных комментариев (parentComment/replies)
 */
@Entity
public class CommentJob {

    private static final Logger log = LoggerFactory.getLogger(CommentJob.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Текст комментария (обязательное поле, не может быть пустым) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Дата и время создания комментария (заполняется автоматически, не обновляется) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Автор комментария (обязательная связь с пользователем) */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Объявление, к которому оставлен комментарий (обязательная связь) */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private PostTextJob postjob;

    /** Родительский комментарий (для поддержки вложенных ответов) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentJob parentComment;

    /** Список дочерних комментариев (ответов на данный комментарий) */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentJob> replies = new ArrayList<>();

    // ===== Конструкторы =====

    /**
     * Конструктор по умолчанию (требуется JPA).
     */
    public CommentJob() {
        log.debug("Created new CommentJob instance");
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

    public CommentJob getParentComment() {
        return parentComment;
    }

    public void setParentComment(CommentJob parentComment) {
        this.parentComment = parentComment;
    }

    public List<CommentJob> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentJob> replies) {
        this.replies = replies;
    }

    /**
     * Добавляет ответ (дочерний комментарий) к текущему комментарию.
     * Автоматически устанавливает родительскую связь у добавляемого комментария.
     *
     * @param reply дочерний комментарий для добавления
     */
    public void addReply(CommentJob reply) {
        log.debug("Adding reply to comment ID: {}", this.id);
        replies.add(reply);
        reply.setParentComment(this);
    }
}