package jobforsite.sitejob.module.shared.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Сущность для отслеживания просмотров различных типов контента (постов, вакансий, резюме).
 * <p>
 * Поддерживает как авторизованных пользователей (привязка к {@link User}),
 * так и неавторизованных (идентификация по IP + fingerprint).
 * Уникальность просмотра обеспечивается составными уникальными ограничениями
 * для каждого типа контента и каждой категории пользователей.
 * </p>
 *
 * @see User
 * @see PostText
 * @see PostTextJob
 * @see Vacancy
 * @see Resume
 */
@Entity
@Table(name = "post_views",
        uniqueConstraints = {
                // Для авторизованных пользователей: уникальность по user_id + типу контента + ID контента
                @UniqueConstraint(columnNames = {"user_id", "post_id", "post_type"}),
                @UniqueConstraint(columnNames = {"user_id", "post_job_id", "post_type"}),
                @UniqueConstraint(columnNames = {"user_id", "vacancy_id", "post_type"}),
                @UniqueConstraint(columnNames = {"user_id", "resume_id", "post_type"}),
                // Для неавторизованных: уникальность по IP + fingerprint + типу контента + ID контента
                @UniqueConstraint(columnNames = {"ip_address", "fingerprint", "post_id", "post_type"}),
                @UniqueConstraint(columnNames = {"ip_address", "fingerprint", "post_job_id", "post_type"}),
                @UniqueConstraint(columnNames = {"ip_address", "fingerprint", "vacancy_id", "post_type"}),
                @UniqueConstraint(columnNames = {"ip_address", "fingerprint", "resume_id", "post_type"})
        })

public class PostView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Кто посмотрел (null для неавторизованных пользователей)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Какой именно контент был просмотрен (только одно из полей не-null)
    @ManyToOne
    @JoinColumn(name = "post_id")
    private PostText post;

    @ManyToOne
    @JoinColumn(name = "post_job_id")
    private PostTextJob postJob;

    @ManyToOne
    @JoinColumn(name = "vacancy_id")
    private Vacancy vacancy;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;

    /**
     * Тип просмотренного контента.
     * Возможные значения: "product", "service", "vacancy", "resume"
     */
    @Column(name = "post_type", nullable = false)
    private String postType;

    // Данные для идентификации неавторизованных пользователей

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Хеш браузерного отпечатка на основе User-Agent, Accept-Language и другой информации.
     */
    @Column(name = "fingerprint", length = 64)
    private String fingerprint;

    /**
     * Идентификатор сессии для отслеживания просмотров в рамках одной сессии.
     */
    @Column(name = "session_id", length = 128)
    private String sessionId;

    /**
     * Время просмотра. Автоматически устанавливается при создании записи.
     */
    @CreationTimestamp
    @Column(name = "viewed_at", updatable = false)
    private LocalDateTime viewedAt;

    public PostView() {}

    // === Конструкторы для авторизованных пользователей ===

    public PostView(User user, PostText post) {
        this.user = user;
        this.post = post;
        this.postType = "product";
    }

    public PostView(User user, PostTextJob postJob) {
        this.user = user;
        this.postJob = postJob;
        this.postType = "service";
    }

    public PostView(User user, Vacancy vacancy) {
        this.user = user;
        this.vacancy = vacancy;
        this.postType = "vacancy";
    }

    public PostView(User user, Resume resume) {
        this.user = user;
        this.resume = resume;
        this.postType = "resume";
    }

    // === Конструкторы для неавторизованных пользователей ===

    public PostView(PostText post, String ipAddress, String fingerprint, String sessionId) {
        this.post = post;
        this.postType = "product";
        this.ipAddress = ipAddress;
        this.fingerprint = fingerprint;
        this.sessionId = sessionId;
    }

    public PostView(PostTextJob postJob, String ipAddress, String fingerprint, String sessionId) {
        this.postJob = postJob;
        this.postType = "service";
        this.ipAddress = ipAddress;
        this.fingerprint = fingerprint;
        this.sessionId = sessionId;
    }

    public PostView(Vacancy vacancy, String ipAddress, String fingerprint, String sessionId) {
        this.vacancy = vacancy;
        this.postType = "vacancy";
        this.ipAddress = ipAddress;
        this.fingerprint = fingerprint;
        this.sessionId = sessionId;
    }

    public PostView(Resume resume, String ipAddress, String fingerprint, String sessionId) {
        this.resume = resume;
        this.postType = "resume";
        this.ipAddress = ipAddress;
        this.fingerprint = fingerprint;
        this.sessionId = sessionId;
    }

    // === Геттеры и сеттеры ===

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public PostText getPost() { return post; }
    public void setPost(PostText post) { this.post = post; }

    public PostTextJob getPostJob() { return postJob; }
    public void setPostJob(PostTextJob postJob) { this.postJob = postJob; }

    public Vacancy getVacancy() { return vacancy; }
    public void setVacancy(Vacancy vacancy) { this.vacancy = vacancy; }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }

    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}