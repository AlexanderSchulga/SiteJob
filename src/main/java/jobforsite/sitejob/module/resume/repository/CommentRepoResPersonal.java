package jobforsite.sitejob.module.resume.repository;

import jobforsite.sitejob.module.resume.model.CommentPersonalRes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с комментариями к резюме.
 * Предоставляет методы для поиска комментариев, ответов, управления статусом прочтения.
 */
public interface CommentRepoResPersonal extends JpaRepository<CommentPersonalRes, Long> {

    /**
     * Находит все комментарии к резюме по его ID, отсортированные по дате создания (новые сверху).
     *
     * @param resId ID резюме
     * @return список комментариев
     */
    List<CommentPersonalRes> findByResumeIdOrderByCreatedAtDesc(Long resId);

    /**
     * Находит дочерние комментарии (ответы) по ID родительского комментария.
     *
     * @param parentCommentId ID родительского комментария
     * @return список ответов, отсортированных по дате (старые сверху)
     */
    List<CommentPersonalRes> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все дочерние комментарии (включая вложенные) по ID родительского комментария.
     *
     * @param parentId ID родительского комментария
     * @return список всех ответов
     */
    @Query("SELECT cp FROM CommentPersonalRes cp WHERE cp.parentComment.id = :parentId")
    List<CommentPersonalRes> findAllRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Находит ID резюме, у которых есть непрочитанные сообщения для указанного пользователя-получателя.
     * Исключает сообщения, где пользователь является автором (свои сообщения не считаются непрочитанными).
     *
     * @param recipientId ID получателя
     * @return список ID резюме с непрочитанными сообщениями
     */
    @Query("SELECT DISTINCT cp.resume.id FROM CommentPersonalRes cp WHERE cp.recipient.id = :recipientId AND cp.user.id <> :recipientId AND cp.isRead = false")
    List<Long> findResIdsWithUnreadMessages(@Param("recipientId") Long recipientId);

    /**
     * Отмечает все сообщения по резюме для указанного получателя как прочитанные.
     *
     * @param resId       ID резюме
     * @param recipientId ID получателя
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommentPersonalRes cp SET cp.isRead = true WHERE cp.resume.id = :resId AND cp.recipient.id = :recipientId AND cp.isRead = false")
    void markMessagesAsRead(@Param("resId") Long resId, @Param("recipientId") Long recipientId);

    /**
     * Находит непрочитанные сообщения по резюме для указанного получателя.
     *
     * @param resId       ID резюме
     * @param recipientId ID получателя
     * @return список непрочитанных сообщений, отсортированных по дате (новые сверху)
     */
    @Query("SELECT cp FROM CommentPersonalRes cp WHERE cp.resume.id = :resId AND cp.recipient.id = :recipientId AND cp.isRead = false ORDER BY cp.createdAt DESC")
    List<CommentPersonalRes> findUnreadMessages(@Param("resId") Long resId, @Param("recipientId") Long recipientId);

    /**
     * Находит корневые комментарии (не ответы) к резюме с пагинацией в виде Slice.
     *
     * @param resId    ID резюме
     * @param pageable параметры пагинации
     * @return Slice с комментариями
     */
    Slice<CommentPersonalRes> findSliceByResumeIdAndParentCommentIsNull(Long resId, Pageable pageable);

    /**
     * Находит корневые комментарии к резюме, видимые для конкретного пользователя.
     * Видимыми считаются комментарии, где пользователь является автором или получателем.
     *
     * @param resId    ID резюме
     * @param userId   ID пользователя
     * @param pageable параметры пагинации
     * @return Slice с видимыми комментариями
     */
    @Query("SELECT cp FROM CommentPersonalRes cp WHERE cp.resume.id = :resId AND cp.parentComment IS NULL AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    Slice<CommentPersonalRes> findVisibleCommentsSliceForUser(@Param("resId") Long resId,
                                                              @Param("userId") Long userId,
                                                              Pageable pageable);

    /**
     * Находит все комментарии к резюме, где пользователь является автором или получателем.
     *
     * @param resId  ID резюме
     * @param userId ID пользователя
     * @return список комментариев
     */
    @Query("SELECT cp FROM CommentPersonalRes cp WHERE cp.resume.id = :resId AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    List<CommentPersonalRes> findByResumeIdAndUserOrRecipient(@Param("resId") Long resId,
                                                              @Param("userId") Long userId);

    /**
     * Находит все комментарии, оставленные пользователем.
     *
     * @param userId ID пользователя-автора
     * @return список комментариев
     */
    List<CommentPersonalRes> findByUserId(Long userId);

    /**
     * Находит все комментарии, адресованные пользователю.
     *
     * @param recipientId ID пользователя-получателя
     * @return список комментариев
     */
    List<CommentPersonalRes> findByRecipientId(Long recipientId);

    /**
     * Находит все комментарии по ID родительского комментария.
     *
     * @param parentCommentId ID родительского комментария
     * @return список дочерних комментариев
     */
    List<CommentPersonalRes> findByParentCommentId(Long parentCommentId);
}