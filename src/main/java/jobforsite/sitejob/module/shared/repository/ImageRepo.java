package jobforsite.sitejob.module.shared.repository;

import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.shared.model.ImagesFoto;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с изображениями (ImagesFoto).
 * Предоставляет методы для поиска, пометки на удаление, массового удаления и подсчета изображений,
 * привязанных к различным типам сущностей (пост, услуга, вакансия, резюме).
 */
public interface ImageRepo extends JpaRepository<ImagesFoto, Long> {

    // ==================== Поиск изображений по родительским объектам ====================

    /**
     * Найти все изображения, привязанные к обычному посту.
     *
     * @param post обычный пост
     * @return список изображений
     */
    List<ImagesFoto> findByPost(PostText post);

    /**
     * Найти все изображения, привязанные к посту-услуге.
     *
     * @param postJob услуга
     * @return список изображений
     */
    List<ImagesFoto> findByPostjob(PostTextJob postJob);

    /**
     * Найти все изображения, привязанные к вакансии.
     *
     * @param vacancy вакансия
     * @return список изображений
     */
    List<ImagesFoto> findByVacancy(Vacancy vacancy);

    /**
     * Найти все изображения, привязанные к резюме.
     *
     * @param resume резюме
     * @return список изображений
     */
    List<ImagesFoto> findByResume(Resume resume);

    // ==================== Поиск изображений, отмеченных на удаление ====================

    /**
     * Найти все изображения, отмеченные флагом markedForDeletion = true.
     * Используется для фоновой очистки неиспользуемых файлов.
     *
     * @return список изображений на удаление
     */
    List<ImagesFoto> findByMarkedForDeletionTrue();

    /**
     * Найти изображения обычного поста, отмеченные на удаление.
     *
     * @param post обычный пост
     * @return список отмеченных изображений
     */
    @Query("SELECT i FROM ImagesFoto i WHERE i.post = :post AND i.markedForDeletion = true")
    List<ImagesFoto> findMarkedForDeletionByPost(@Param("post") PostText post);

    /**
     * Найти изображения услуги, отмеченные на удаление.
     *
     * @param postJob услуга
     * @return список отмеченных изображений
     */
    @Query("SELECT i FROM ImagesFoto i WHERE i.postjob = :postJob AND i.markedForDeletion = true")
    List<ImagesFoto> findMarkedForDeletionByPostjob(@Param("postJob") PostTextJob postJob);

    /**
     * Найти изображения вакансии, отмеченные на удаление.
     *
     * @param vacancy вакансия
     * @return список отмеченных изображений
     */
    @Query("SELECT i FROM ImagesFoto i WHERE i.vacancy = :vacancy AND i.markedForDeletion = true")
    List<ImagesFoto> findMarkedForDeletionByVacancy(@Param("vacancy") Vacancy vacancy);

    /**
     * Найти изображения резюме, отмеченные на удаление.
     *
     * @param resume резюме
     * @return список отмеченных изображений
     */
    @Query("SELECT i FROM ImagesFoto i WHERE i.resume = :resume AND i.markedForDeletion = true")
    List<ImagesFoto> findMarkedForDeletionByResume(@Param("resume") Resume resume);

    // ==================== Массовое удаление отмеченных изображений ====================

    /**
     * Удалить ВСЕ изображения, отмеченные флагом markedForDeletion = true.
     * Используется в планировщике для очистки накопившихся "мусорных" записей.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImagesFoto i WHERE i.markedForDeletion = true")
    void deleteAllMarkedForDeletion();

    /**
     * Удалить отмеченные изображения для конкретного обычного поста.
     *
     * @param post обычный пост
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImagesFoto i WHERE i.post = :post AND i.markedForDeletion = true")
    void deleteMarkedForDeletionByPost(@Param("post") PostText post);

    /**
     * Удалить отмеченные изображения для конкретной услуги.
     *
     * @param postJob услуга
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImagesFoto i WHERE i.postjob = :postJob AND i.markedForDeletion = true")
    void deleteMarkedForDeletionByPostjob(@Param("postJob") PostTextJob postJob);

    /**
     * Удалить отмеченные изображения для конкретной вакансии.
     *
     * @param vacancy вакансия
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImagesFoto i WHERE i.vacancy = :vacancy AND i.markedForDeletion = true")
    void deleteMarkedForDeletionByVacancy(@Param("vacancy") Vacancy vacancy);

    /**
     * Удалить отмеченные изображения для конкретного резюме.
     *
     * @param resume резюме
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ImagesFoto i WHERE i.resume = :resume AND i.markedForDeletion = true")
    void deleteMarkedForDeletionByResume(@Param("resume") Resume resume);

    // ==================== Полное удаление всех изображений родителя (при удалении самого родителя) ====================

    /**
     * Удалить ВСЕ изображения обычного поста (без проверки флага markedForDeletion).
     * Используется при полном удалении поста.
     *
     * @param post обычный пост
     */
    @Modifying
    @Transactional
    void deleteByPost(PostText post);

    /**
     * Удалить ВСЕ изображения услуги.
     *
     * @param postJob услуга
     */
    @Modifying
    @Transactional
    void deleteByPostjob(PostTextJob postJob);

    /**
     * Удалить ВСЕ изображения вакансии.
     *
     * @param vacancy вакансия
     */
    @Modifying
    @Transactional
    void deleteByVacancy(Vacancy vacancy);

    /**
     * Удалить ВСЕ изображения резюме.
     *
     * @param resume резюме
     */
    @Modifying
    @Transactional
    void deleteByResume(Resume resume);

    // ==================== Подсчет количества изображений ====================

    /**
     * Подсчитать количество изображений у обычного поста.
     *
     * @param post обычный пост
     * @return количество изображений
     */
    int countByPost(PostText post);

    /**
     * Подсчитать количество изображений у услуги.
     *
     * @param postJob услуга
     * @return количество изображений
     */
    int countByPostjob(PostTextJob postJob);

    /**
     * Подсчитать количество изображений у вакансии.
     *
     * @param vacancy вакансия
     * @return количество изображений
     */
    int countByVacancy(Vacancy vacancy);

    /**
     * Подсчитать количество изображений у резюме.
     *
     * @param resume резюме
     * @return количество изображений
     */
    int countByResume(Resume resume);
}