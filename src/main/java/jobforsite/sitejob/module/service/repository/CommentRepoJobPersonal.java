package jobforsite.sitejob.module.service.repository;

import jobforsite.sitejob.module.service.model.CommentPersonalJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с персональными комментариями (личные переписки к объявлениям).
 * Обеспечивает выборку комментариев по постам, родительским комментариям,
 * статусу прочтения, а также поиск непрочитанных сообщений.
 */
public interface CommentRepoJobPersonal extends JpaRepository<CommentPersonalJob, Long> {

    /**
     * Находит все комментарии к конкретному объявлению, отсортированные по убыванию даты создания.
     *
     * @param postjobId ID объявления
     * @return список комментариев
     */
    List<CommentPersonalJob> findByPostjobIdOrderByCreatedAtDesc(Long postjobId);

    /**
     * Находит дочерние комментарии (ответы) для заданного родительского комментария.
     *
     * @param parentCommentId ID родительского комментария
     * @return список дочерних комментариев, отсортированных по возрастанию даты
     */
    List<CommentPersonalJob> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все прямые дочерние комментарии по ID родителя.
     * Не рекурсивный (только первый уровень вложенности).
     *
     * @param parentId ID родительского комментария
     * @return список дочерних комментариев
     */
    @Query("SELECT cp FROM CommentPersonalJob cp WHERE cp.parentComment.id = :parentId")
    List<CommentPersonalJob> findAllRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Находит ID объявлений, в которых есть непрочитанные сообщения для указанного получателя.
     * Используется для уведомлений о новых сообщениях.
     *
     * @param recipientId ID получателя
     * @return список ID объявлений с непрочитанными сообщениями
     */
    @Query("SELECT DISTINCT cp.postjob.id FROM CommentPersonalJob cp WHERE cp.recipient.id = :recipientId AND cp.user.id <> :recipientId AND cp.isRead = false")
    List<Long> findPostIdsWithUnreadMessages(@Param("recipientId") Long recipientId);

    /**
     * Отмечает все сообщения в рамках конкретного объявления для указанного получателя как прочитанные.
     * Выполняется в транзакции с модификацией данных.
     *
     * @param postId      ID объявления
     * @param recipientId ID получателя
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommentPersonalJob cp SET cp.isRead = true WHERE cp.postjob.id = :postId AND cp.recipient.id = :recipientId AND cp.isRead = false")
    void markMessagesAsRead(@Param("postId") Long postId, @Param("recipientId") Long recipientId);

    /**
     * Находит непрочитанные сообщения для конкретного объявления и получателя.
     *
     * @param postId      ID объявления
     * @param recipientId ID получателя
     * @return список непрочитанных сообщений (от новых к старым)
     */
    @Query("SELECT cp FROM CommentPersonalJob cp WHERE cp.postjob.id = :postId AND cp.recipient.id = :recipientId AND cp.isRead = false ORDER BY cp.createdAt DESC")
    List<CommentPersonalJob> findUnreadMessages(@Param("postId") Long postId, @Param("recipientId") Long recipientId);

    /**
     * Находит корневые комментарии (без родителя) к объявлению с пагинацией.
     *
     * @param postId   ID объявления
     * @param pageable параметры пагинации
     * @return срез (slice) корневых комментариев
     */
    Slice<CommentPersonalJob> findSliceByPostjobIdAndParentCommentIsNull(Long postId, Pageable pageable);

    /**
     * Находит корневые комментарии, видимые для конкретного пользователя.
     * Пользователь видит комментарии, где он является отправителем или получателем.
     *
     * @param postId   ID объявления
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return срез (slice) видимых комментариев
     */
    @Query("SELECT cp FROM CommentPersonalJob cp WHERE cp.postjob.id = :postId AND cp.parentComment IS NULL AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    Slice<CommentPersonalJob> findSliceVisibleCommentsForUser(@Param("postId") Long postId,
                                                              @Param("userId") Long userId,
                                                              Pageable pageable);

    /**
     * Находит все комментарии к объявлению, где пользователь является отправителем ИЛИ получателем.
     * Используется для получения полной переписки по объявлению между двумя участниками.
     *
     * @param postId ID объявления
     * @param userId ID пользователя
     * @return список комментариев (все уровни вложенности)
     */
    @Query("SELECT cp FROM CommentPersonalJob cp WHERE cp.postjob.id = :postId AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    List<CommentPersonalJob> findByPostIdAndUserOrRecipient(@Param("postId") Long postId,
                                                            @Param("userId") Long userId);

    /**
     * Находит все комментарии, отправленные указанным пользователем.
     *
     * @param userId ID отправителя
     * @return список отправленных комментариев
     */
    List<CommentPersonalJob> findByUserId(Long userId);

    /**
     * Находит все комментарии, адресованные указанному получателю.
     *
     * @param recipientId ID получателя
     * @return список полученных комментариев
     */
    List<CommentPersonalJob> findByRecipientId(Long recipientId);

    /**
     * Находит все дочерние комментарии по ID родителя.
     * Аналогичен findByParentCommentIdOrderByCreatedAtAsc, но без сортировки.
     *
     * @param parentCommentId ID родительского комментария
     * @return список дочерних комментариев
     */
    List<CommentPersonalJob> findByParentCommentId(Long parentCommentId);
}