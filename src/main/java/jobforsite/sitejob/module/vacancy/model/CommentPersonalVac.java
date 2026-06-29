package jobforsite.sitejob.module.vacancy.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность (Entity) для хранения личных комментариев к вакансии.
 * Поддерживает иерархическую структуру (ответы на комментарии) и
 * систему приватных сообщений между пользователями в рамках вакансии.
 */
@Entity
public class CommentPersonalVac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Текст комментария или сообщения */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    /** Дата и время создания комментария (автоматически проставляется при создании) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Автор комментария (кто написал) */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Вакансия, которой принадлежит комментарий */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Vacancy vacancy;

    /** Получатель сообщения (для приватных диалогов). Если null — комментарий публичный */
    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    /** Родительский комментарий (для организации веток ответов) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private CommentPersonalVac parentComment;

    /** Список ответов на данный комментарий */
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalVac> replies = new ArrayList<>();

    /** Флаг, указывающий, прочитано ли сообщение получателем */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    public CommentPersonalVac() {}

    /**
     * Конструктор для создания нового комментария.
     *
     * @param text          текст комментария
     * @param user          автор комментария
     * @param vacancy       вакансия
     * @param recipient     получатель (для приватных сообщений)
     * @param parentComment родительский комментарий (для ответов)
     */
    public CommentPersonalVac(String text, User user, Vacancy vacancy, User recipient, CommentPersonalVac parentComment) {
        this.text = text;
        this.user = user;
        this.vacancy = vacancy;
        this.recipient = recipient;
        this.parentComment = parentComment;
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

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public CommentPersonalVac getParentComment() {
        return parentComment;
    }

    public void setParentComment(CommentPersonalVac parentComment) {
        this.parentComment = parentComment;
    }

    public Boolean getRead() {
        return isRead;
    }

    public void setRead(Boolean read) {
        isRead = read;
    }

    public List<CommentPersonalVac> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentPersonalVac> replies) {
        this.replies = replies;
    }
}