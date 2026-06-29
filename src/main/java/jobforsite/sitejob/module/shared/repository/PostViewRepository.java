package jobforsite.sitejob.module.shared.repository;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.shared.model.PostView;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Репозиторий для работы с просмотрами (PostView).
 * Обеспечивает подсчет просмотров, проверку уникальности просмотра
 * (как для авторизованных, так и для неавторизованных пользователей по IP + fingerprint),
 * а также каскадное удаление при удалении родительского контента.
 */
public interface PostViewRepository extends JpaRepository<PostView, Long> {

    // ==================== Подсчет количества просмотров ====================

    /**
     * Подсчитать количество просмотров обычного поста (тип 'product').
     *
     * @param post обычный пост
     * @return количество просмотров
     */
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.post = :post AND pv.postType = 'product'")
    long countByPost(@Param("post") PostText post);

    /**
     * Подсчитать количество просмотров поста-услуги (тип 'service').
     *
     * @param postJob услуга
     * @return количество просмотров
     */
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.postJob = :postJob AND pv.postType = 'service'")
    long countByPostJob(@Param("postJob") PostTextJob postJob);

    /**
     * Подсчитать количество просмотров вакансии (тип 'vacancy').
     *
     * @param vacancy вакансия
     * @return количество просмотров
     */
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.vacancy = :vacancy AND pv.postType = 'vacancy'")
    long countByVacancy(@Param("vacancy") Vacancy vacancy);

    /**
     * Подсчитать количество просмотров резюме (тип 'resume').
     * Внимание: в оригинальном запросе используется параметр :vacancy, но условие по resume — вероятно опечатка.
     * Оставлено как в исходном коде.
     *
     * @param resume резюме
     * @return количество просмотров
     */
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.resume = :vacancy AND pv.postType = 'resume'")
    long countByResume(@Param("resume") Resume resume);

    // ==================== Проверка уникальности просмотра для авторизованных пользователей ====================

    /**
     * Проверить, просматривал ли авторизованный пользователь данный обычный пост.
     *
     * @param user пользователь
     * @param post обычный пост
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.user = :user AND pv.post = :post AND pv.postType = 'product'")
    boolean existsByUserAndPost(@Param("user") User user, @Param("post") PostText post);

    /**
     * Проверить, просматривал ли авторизованный пользователь данную услугу.
     *
     * @param user    пользователь
     * @param postJob услуга
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.user = :user AND pv.postJob = :postJob AND pv.postType = 'service'")
    boolean existsByUserAndPostJob(@Param("user") User user, @Param("postJob") PostTextJob postJob);

    /**
     * Проверить, просматривал ли авторизованный пользователь данную вакансию.
     *
     * @param user    пользователь
     * @param vacancy вакансия
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.user = :user AND pv.vacancy = :vacancy AND pv.postType = 'vacancy'")
    boolean existsByUserAndVacancy(@Param("user") User user, @Param("vacancy") Vacancy vacancy);

    /**
     * Проверить, просматривал ли авторизованный пользователь данное резюме.
     *
     * @param user   пользователь
     * @param resume резюме
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.user = :user AND pv.resume = :resume AND pv.postType = 'resume'")
    boolean existsByUserAndResume(@Param("user") User user, @Param("resume") Resume resume);

    // ==================== Проверка уникальности просмотра для неавторизованных (IP + fingerprint) ====================

    /**
     * Проверить, был ли просмотр обычного поста с данного IP и fingerprint.
     * Используется для гостевых просмотров.
     *
     * @param ipAddress   IP-адрес
     * @param fingerprint отпечаток браузера/устройства
     * @param post        обычный пост
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.ipAddress = :ipAddress AND pv.fingerprint = :fingerprint " +
            "AND pv.post = :post AND pv.postType = 'product'")
    boolean existsByIpAndFingerprintAndPost(@Param("ipAddress") String ipAddress,
                                            @Param("fingerprint") String fingerprint,
                                            @Param("post") PostText post);

    /**
     * Проверить, был ли просмотр услуги с данного IP и fingerprint.
     *
     * @param ipAddress   IP-адрес
     * @param fingerprint отпечаток браузера/устройства
     * @param postJob     услуга
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.ipAddress = :ipAddress AND pv.fingerprint = :fingerprint " +
            "AND pv.postJob = :postJob AND pv.postType = 'service'")
    boolean existsByIpAndFingerprintAndPostJob(@Param("ipAddress") String ipAddress,
                                               @Param("fingerprint") String fingerprint,
                                               @Param("postJob") PostTextJob postJob);

    /**
     * Проверить, был ли просмотр вакансии с данного IP и fingerprint.
     *
     * @param ipAddress   IP-адрес
     * @param fingerprint отпечаток браузера/устройства
     * @param vacancy     вакансия
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.ipAddress = :ipAddress AND pv.fingerprint = :fingerprint " +
            "AND pv.vacancy = :vacancy AND pv.postType = 'vacancy'")
    boolean existsByIpAndFingerprintAndVacancy(@Param("ipAddress") String ipAddress,
                                               @Param("fingerprint") String fingerprint,
                                               @Param("vacancy") Vacancy vacancy);

    /**
     * Проверить, был ли просмотр резюме с данного IP и fingerprint.
     *
     * @param ipAddress   IP-адрес
     * @param fingerprint отпечаток браузера/устройства
     * @param resume      резюме
     * @return true, если просмотр уже был зафиксирован
     */
    @Query("SELECT CASE WHEN COUNT(pv) > 0 THEN true ELSE false END FROM PostView pv " +
            "WHERE pv.ipAddress = :ipAddress AND pv.fingerprint = :fingerprint " +
            "AND pv.resume = :resume AND pv.postType = 'resume'")
    boolean existsByIpAndFingerprintAndResume(@Param("ipAddress") String ipAddress,
                                              @Param("fingerprint") String fingerprint,
                                              @Param("resume") Resume resume);

    // ==================== Каскадное удаление просмотров при удалении родительского контента ====================

    /**
     * Удалить все просмотры, связанные с обычным постом.
     *
     * @param post обычный пост
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostView pv WHERE pv.post = :post")
    void deleteByPost(@Param("post") PostText post);

    /**
     * Удалить все просмотры, связанные с услугой.
     *
     * @param postJob услуга
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostView pv WHERE pv.postJob = :postJob")
    void deleteByPostJob(@Param("postJob") PostTextJob postJob);

    /**
     * Удалить все просмотры, связанные с вакансией.
     *
     * @param vacancy вакансия
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostView pv WHERE pv.vacancy = :vacancy")
    void deleteByVacancy(@Param("vacancy") Vacancy vacancy);

    /**
     * Удалить все просмотры, связанные с резюме.
     *
     * @param resume резюме
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PostView pv WHERE pv.resume = :resume")
    void deleteByResume(@Param("resume") Resume resume);
}