package jobforsite.sitejob.module.product.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность комментария к объявлению.
 * Поддерживает иерархическую структуру (комментарии и ответы на них).
 */
@Entity
public class Comment {
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

    /** Объявление, к которому оставлен комментарий */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private PostText post;

    /** Родительский комментарий (для ответов) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    /** Список ответов на данный комментарий */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> replies = new ArrayList<>();

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

    public Comment getParentComment() {
        return parentComment;
    }

    public void setParentComment(Comment parentComment) {
        this.parentComment = parentComment;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    // ========== Вспомогательные методы ==========

    /**
     * Добавляет ответ к текущему комментарию.
     * Автоматически устанавливает родительскую связь.
     * @param reply комментарий-ответ
     */
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }
}