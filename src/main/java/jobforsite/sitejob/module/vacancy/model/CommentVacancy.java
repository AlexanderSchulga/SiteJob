package jobforsite.sitejob.module.vacancy.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность (Entity) для хранения публичных комментариев к вакансии.
 * В отличие от CommentPersonalVac, эти комментарии видны всем пользователям.
 * Поддерживает иерархическую структуру (ответы на комментарии).
 */
@Entity
public class CommentVacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Текст комментария */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Дата и время создания комментария (автоматически проставляется при создании) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Автор комментария */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Вакансия, к которой оставлен комментарий */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Vacancy vacancy;

    /** Родительский комментарий (для организации веток ответов) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentVacancy parentComment;

    /** Список ответов на данный комментарий */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentVacancy> replies = new ArrayList<>();

    // === Конструкторы ===

    public CommentVacancy() {}

    /**
     * Конструктор для быстрого создания комментария.
     *
     * @param text    текст комментария
     * @param user    автор комментария
     * @param vacancy вакансия
     */
    public CommentVacancy(String text, User user, Vacancy vacancy) {
        this.text = text;
        this.user = user;
        this.vacancy = vacancy;
    }

    // === Геттеры и сеттеры ===

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

    public Vacancy getVacancy() {
        return vacancy;
    }

    public void setVacancy(Vacancy vacancy) {
        this.vacancy = vacancy;
    }

    public CommentVacancy getParentComment() {
        return parentComment;
    }

    public void setParentComment(CommentVacancy parentComment) {
        this.parentComment = parentComment;
    }

    public List<CommentVacancy> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentVacancy> replies) {
        this.replies = replies;
    }

    /**
     * Добавляет ответ к текущему комментарию.
     * Устанавливает двустороннюю связь между комментариями.
     *
     * @param reply комментарий-ответ
     */
    public void addReply(CommentVacancy reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }
}