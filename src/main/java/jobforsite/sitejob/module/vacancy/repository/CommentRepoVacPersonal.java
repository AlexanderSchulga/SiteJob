package jobforsite.sitejob.module.vacancy.repository;

import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с персональными комментариями к вакансиям.
 * Обеспечивает функционал личных сообщений между пользователями внутри вакансии:
 * отправка, получение, отметка о прочтении, фильтрация видимых комментариев.
 */
public interface CommentRepoVacPersonal extends JpaRepository<CommentPersonalVac, Long> {

    /**
     * Находит все комментарии к вакансии, отсортированные по дате создания от новых к старым.
     *
     * @param vacancyId идентификатор вакансии
     * @return список комментариев
     */
    List<CommentPersonalVac> findByVacancyIdOrderByCreatedAtDesc(Long vacancyId);

    /**
     * Находит дочерние комментарии (ответы) для указанного родителя,
     * отсортированные по дате создания от старых к новым.
     *
     * @param parentCommentId идентификатор родительского комментария
     * @return список ответов
     */
    List<CommentPersonalVac> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все прямые дочерние комментарии для указанного родителя.
     * Не выполняет рекурсивный поиск по всем уровням вложенности.
     *
     * @param parentId идентификатор родительского комментария
     * @return список прямых потомков
     */
    @Query("SELECT cp FROM CommentPersonalVac cp WHERE cp.parentComment.id = :parentId")
    List<CommentPersonalVac> findAllRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Возвращает идентификаторы вакансий, в которых есть непрочитанные сообщения для указанного получателя.
     * Группировка на уровне БД — каждый пост встречается в результате только один раз.
     *
     * @param recipientId идентификатор получателя (кому адресовано сообщение)
     * @return список ID вакансий с непрочитанными сообщениями
     */
    @Query("SELECT DISTINCT cp.vacancy.id FROM CommentPersonalVac cp WHERE cp.recipient.id = :recipientId AND cp.user.id <> :recipientId AND cp.isRead = false")
    List<Long> findPostVacIdsWithUnreadMessages(@Param("recipientId") Long recipientId);

    /**
     * Отмечает все непрочитанные сообщения пользователя в рамках конкретной вакансии как прочитанные.
     * Изменяет данные в БД, требует транзакции.
     *
     * @param postId      идентификатор вакансии (поста)
     * @param recipientId идентификатор получателя
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommentPersonalVac cp SET cp.isRead = true WHERE cp.vacancy.id = :postId AND cp.recipient.id = :recipientId AND cp.isRead = false")
    void markMessagesAsRead(@Param("postId") Long postId, @Param("recipientId") Long recipientId);

    /**
     * Находит все непрочитанные сообщения для получателя в указанной вакансии,
     * отсортированные от новых к старым.
     *
     * @param postId      идентификатор вакансии
     * @param recipientId идентификатор получателя
     * @return список непрочитанных сообщений
     */
    @Query("SELECT cp FROM CommentPersonalVac cp WHERE cp.vacancy.id = :postId AND cp.recipient.id = :recipientId AND cp.isRead = false ORDER BY cp.createdAt DESC")
    List<CommentPersonalVac> findUnreadMessages(@Param("postId") Long postId, @Param("recipientId") Long recipientId);

    /**
     * Возвращает срез корневых комментариев (без родителя) по идентификатору вакансии с пагинацией.
     *
     * @param vacancyId идентификатор вакансии
     * @param pageable  параметры пагинации
     * @return срез корневых комментариев
     */
    Slice<CommentPersonalVac> findSliceByVacancyIdAndParentCommentIsNull(Long vacancyId, Pageable pageable);

    /**
     * Возвращает видимые комментарии для конкретного пользователя в рамках вакансии.
     * Пользователь видит комментарии, где он является либо отправителем (user),
     * либо получателем (recipient). Возвращаются только корневые комментарии.
     *
     * @param postId   идентификатор вакансии
     * @param userId   идентификатор пользователя
     * @param pageable параметры пагинации
     * @return срез видимых комментариев
     */
    @Query("SELECT cp FROM CommentPersonalVac cp WHERE cp.vacancy.id = :postId AND cp.parentComment IS NULL AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    Slice<CommentPersonalVac> findSliceVisibleCommentsForUser(@Param("postId") Long postId,
                                                              @Param("userId") Long userId,
                                                              Pageable pageable);

    /**
     * Находит все комментарии (включая вложенные) к вакансии, где пользователь
     * является либо отправителем, либо получателем.
     *
     * @param postId идентификатор вакансии
     * @param userId идентификатор пользователя
     * @return список всех комментариев, доступных пользователю по этой вакансии
     */
    @Query("SELECT cp FROM CommentPersonalVac cp WHERE cp.vacancy.id = :postId AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    List<CommentPersonalVac> findByVacancyIdAndUserOrRecipient(@Param("postId") Long postId,
                                                               @Param("userId") Long userId);

    /**
     * Находит все комментарии, отправленные пользователем.
     *
     * @param userId идентификатор отправителя
     * @return список комментариев пользователя (отправленных им)
     */
    List<CommentPersonalVac> findByUserId(Long userId);

    /**
     * Находит все комментарии, адресованные пользователю.
     *
     * @param recipientId идентификатор получателя
     * @return список комментариев, где пользователь является получателем
     */
    List<CommentPersonalVac> findByRecipientId(Long recipientId);

    /**
     * Находит все комментарии по идентификатору родительского комментария.
     *
     * @param parentCommentId идентификатор родителя
     * @return список дочерних комментариев
     */
    List<CommentPersonalVac> findByParentCommentId(Long parentCommentId);
}