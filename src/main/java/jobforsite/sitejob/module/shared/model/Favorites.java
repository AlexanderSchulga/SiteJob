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
 * Сущность "Избранное" для хранения пользовательских закладок.
 *
 * <p>Позволяет пользователю добавлять в избранное разные типы контента:
 * <ul>
 *   <li>Товары (PostText)</li>
 *   <li>Услуги (PostTextJob)</li>
 *   <li>Вакансии (Vacancy)</li>
 *   <li>Резюме (Resume)</li>
 * </ul>
 *
 * <p>📌 Особенности:
 * <ul>
 *   <li>Одна запись = одна закладка (связь ManyToOne с пользователем)</li>
 *   <li>Только один из четырёх типов контента может быть заполнен</li>
 *   <li>Автоматическая простановка времени добавления (@CreationTimestamp)</li>
 * </ul>
 *
 * @author YourTeam
 */
@Entity
@Table(name = "favorites")
public class Favorites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь, который добавил в избранное.
     * Обязательное поле.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Товар (из раздела продуктов/объявлений).
     * Заполняется только если тип избранного - товар.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private PostText post;

    /**
     * Услуга (из сервисного раздела).
     * Заполняется только если тип избранного - услуга.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_job_id")
    private PostTextJob postJob;

    /**
     * Вакансия (из раздела работы).
     * Заполняется только если тип избранного - вакансия.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacancy_id")
    private Vacancy vacancy;

    /**
     * Резюме (из раздела работы).
     * Заполняется только если тип избранного - резюме.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    /**
     * Дата и время добавления в избранное.
     * Устанавливается автоматически при создании записи.
     */
    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    // ==================== Конструкторы ====================

    public Favorites() {}

    /**
     * Конструктор для добавления товара в избранное.
     *
     * @param user пользователь
     * @param post товар
     */
    public Favorites(User user, PostText post) {
        this.user = user;
        this.post = post;
    }

    /**
     * Конструктор для добавления услуги в избранное.
     *
     * @param user    пользователь
     * @param postJob услуга
     */
    public Favorites(User user, PostTextJob postJob) {
        this.user = user;
        this.postJob = postJob;
    }

    /**
     * Конструктор для добавления вакансии в избранное.
     *
     * @param user    пользователь
     * @param vacancy вакансия
     */
    public Favorites(User user, Vacancy vacancy) {
        this.user = user;
        this.vacancy = vacancy;
    }

    /**
     * Конструктор для добавления резюме в избранное.
     *
     * @param user   пользователь
     * @param resume резюме
     */
    public Favorites(User user, Resume resume) {
        this.user = user;
        this.resume = resume;
    }

    // ==================== Геттеры и сеттеры ====================

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

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    // ==================== Вспомогательные методы ====================

    /**
     * Возвращает тип добавленного в избранное элемента.
     *
     * @return строка: "product", "service", "vacancy", "resume" или "unknown"
     */
    public String getType() {
        if (post != null) return "product";
        if (postJob != null) return "service";
        if (vacancy != null) return "vacancy";
        if (resume != null) return "resume";
        return "unknown";
    }

    /**
     * Возвращает ID добавленного в избранное элемента.
     *
     * @return ID контента или null, если тип не определён
     */
    public Long getItemId() {
        if (post != null) return post.getId();
        if (postJob != null) return postJob.getId();
        if (vacancy != null) return vacancy.getId();
        if (resume != null) return resume.getId();
        return null;
    }
}