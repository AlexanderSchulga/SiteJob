package jobforsite.sitejob.module.shared.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;

/**
 * Сущность, представляющая изображение, связанное с различными типами контента:
 * постом, вакансией, резюме или специализированным постом услуги.
 * <p>
 * Изображение хранит путь к файлу и флаг пометки на удаление,
 * что позволяет реализовать мягкое удаление или отложенную очистку.
 * </p>
 *
 * @see PostText
 * @see PostTextJob
 * @see Vacancy
 * @see Resume
 */
@Entity
public class ImagesFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Путь к файлу изображения в файловой системе или хранилище.
     */
    private String imagePath;

    /**
     * Флаг, указывающий, что изображение помечено для удаления.
     * Используется для отложенной или каскадной очистки.
     */
    @Column(name = "marked_for_deletion", nullable = false)
    private boolean markedForDeletion = false;

    // Связи с владельцами изображения (только одна из них может быть не-null)

    @ManyToOne
    @JoinColumn(name = "post_id")
    private PostText post;

    @ManyToOne
    @JoinColumn(name = "postjob_id")
    private PostTextJob postjob;

    @ManyToOne
    @JoinColumn(name = "vac_id")
    private Vacancy vacancy;

    @ManyToOne
    @JoinColumn(name = "res_id")
    private Resume resume;

    /**
     * Конструктор по умолчанию, необходимый для JPA.
     */
    public ImagesFoto() {}

    /**
     * Конструктор для создания изображения только с путём.
     *
     * @param imagePath путь к файлу изображения
     */
    public ImagesFoto(String imagePath) {
        this.imagePath = imagePath;
    }

    // Геттеры и сеттеры

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    public void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    public PostText getPost() {
        return post;
    }

    public void setPost(PostText post) {
        this.post = post;
    }

    public PostTextJob getPostjob() {
        return postjob;
    }

    public void setPostjob(PostTextJob postjob) {
        this.postjob = postjob;
    }

    public Vacancy getVacancy() {
        return vacancy;
    }

    public void setVacancy(Vacancy vacancy) {
        this.vacancy = vacancy;
    }

    public Resume getResume() {
        return resume;
    }

    public void setResume(Resume resume) {
        this.resume = resume;
    }
}