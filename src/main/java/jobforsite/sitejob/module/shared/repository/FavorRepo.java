package jobforsite.sitejob.module.shared.repository;

import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с избранным (Favorites).
 * Предоставляет методы для поиска, проверки существования, удаления и пагинации избранных записей.
 */
public interface FavorRepo extends JpaRepository<Favorites, Long> {

    // ==================== Базовые методы поиска ====================

    /**
     * Найти все избранные записи пользователя.
     *
     * @param user пользователь
     * @return список избранного
     */
    List<Favorites> findByUser(User user);

    // ==================== Проверка существования записи по типу ====================

    /**
     * Найти избранное по пользователю и ID обычного поста.
     *
     * @param user   пользователь
     * @param postId ID поста
     * @return Optional с записью, если найдена
     */
    Optional<Favorites> findByUserAndPostId(User user, Long postId);

    /**
     * Найти избранное по пользователю и ID поста-услуги.
     *
     * @param user       пользователь
     * @param postJobId ID услуги
     * @return Optional с записью, если найдена
     */
    Optional<Favorites> findByUserAndPostJobId(User user, Long postJobId);

    /**
     * Найти избранное по пользователю и ID вакансии.
     *
     * @param user      пользователь
     * @param vacancyId ID вакансии
     * @return Optional с записью, если найдена
     */
    Optional<Favorites> findByUserAndVacancyId(User user, Long vacancyId);

    /**
     * Найти избранное по пользователю и ID резюме.
     *
     * @param user     пользователь
     * @param resumeId ID резюме
     * @return Optional с записью, если найдена
     */
    Optional<Favorites> findByUserAndResumeId(User user, Long resumeId);

    // ==================== Универсальная проверка наличия в избранном ====================

    /**
     * Проверить, находится ли элемент с указанным ID в избранном у пользователя.
     * Поиск выполняется по всем типам: post, postJob, vacancy, resume.
     *
     * @param user   пользователь
     * @param itemId ID элемента
     * @return true, если элемент в избранном
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorites f WHERE f.user = :user AND " +
            "(f.post.id = :itemId OR f.postJob.id = :itemId OR f.vacancy.id = :itemId OR f.resume.id = :itemId)")
    boolean existsByUserAndAnyItemId(@Param("user") User user, @Param("itemId") Long itemId);

    // ==================== Поиск избранного у авторов контента ====================

    /**
     * Найти все избранные записи, где обычный пост принадлежит указанному пользователю (автору).
     *
     * @param user автор поста
     * @return список избранного
     */
    @Query("SELECT f FROM Favorites f WHERE f.post IN (SELECT p FROM PostText p WHERE p.user = :user)")
    List<Favorites> findByPostAuthor(@Param("user") User user);

    /**
     * Найти все избранные записи, где пост-услуга принадлежит указанному пользователю (автору).
     *
     * @param user автор услуги
     * @return список избранного
     */
    @Query("SELECT f FROM Favorites f WHERE f.postJob IN (SELECT p FROM PostTextJob p WHERE p.user = :user)")
    List<Favorites> findByPostJobAuthor(@Param("user") User user);

    /**
     * Найти все избранные записи, где вакансия принадлежит указанному пользователю (автору).
     *
     * @param user автор вакансии
     * @return список избранного
     */
    @Query("SELECT f FROM Favorites f WHERE f.vacancy IN (SELECT p FROM Vacancy p WHERE p.user = :user)")
    List<Favorites> findByVacancyAuthor(@Param("user") User user);

    /**
     * Найти все избранные записи, где резюме принадлежит указанному пользователю (автору).
     *
     * @param user автор резюме
     * @return список избранного
     */
    @Query("SELECT f FROM Favorites f WHERE f.resume IN (SELECT p FROM Resume p WHERE p.user = :user)")
    List<Favorites> findByResumeAuthor(@Param("user") User user);

    // ==================== Каскадное удаление при удалении оригинального контента ====================

    /**
     * Удалить все избранные записи, связанные с обычным постом по его ID.
     * Используется при удалении самого поста.
     *
     * @param postId ID поста
     */
    @Modifying
    @Transactional
    void deleteByPostId(Long postId);

    /**
     * Удалить все избранные записи, связанные с постом-услугой по его ID.
     *
     * @param postJobId ID услуги
     */
    @Modifying
    @Transactional
    void deleteByPostJobId(Long postJobId);

    /**
     * Удалить все избранные записи, связанные с вакансией по её ID.
     *
     * @param vacancyId ID вакансии
     */
    @Modifying
    @Transactional
    void deleteByVacancyId(Long vacancyId);

    /**
     * Удалить все избранные записи, связанные с резюме по его ID.
     *
     * @param resumeId ID резюме
     */
    @Modifying
    @Transactional
    void deleteByResumeId(Long resumeId);

    // ==================== Пагинация (Slice) ====================

    /**
     * Вернуть слайс (пагинацию) избранного пользователя.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return Slice с записями
     */
    Slice<Favorites> findByUser(User user, Pageable pageable);

    /**
     * Вернуть слайс избранного пользователя, где тип — обычный пост.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return Slice с записями
     */
    Slice<Favorites> findByUserAndPostNotNull(User user, Pageable pageable);

    /**
     * Вернуть слайс избранного пользователя, где тип — пост-услуга.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return Slice с записями
     */
    Slice<Favorites> findByUserAndPostJobNotNull(User user, Pageable pageable);

    /**
     * Вернуть слайс избранного пользователя, где тип — вакансия.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return Slice с записями
     */
    Slice<Favorites> findByUserAndVacancyNotNull(User user, Pageable pageable);

    /**
     * Вернуть слайс избранного пользователя, где тип — резюме.
     *
     * @param user     пользователь
     * @param pageable параметры пагинации
     * @return Slice с записями
     */
    Slice<Favorites> findByUserAndResumeNotNull(User user, Pageable pageable);
}