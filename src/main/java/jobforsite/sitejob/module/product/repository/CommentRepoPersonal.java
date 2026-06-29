package jobforsite.sitejob.module.product.repository;

import jobforsite.sitejob.module.product.model.CommentPersonal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с сущностью CommentPersonal (персональные комментарии/личные сообщения).
 * Предоставляет методы для CRUD операций, поиска комментариев, работы с непрочитанными сообщениями
 * и управления иерархической структурой комментариев.
 */
public interface CommentRepoPersonal extends JpaRepository<CommentPersonal, Long> {

    // ========== Базовые методы поиска ==========

    /**
     * Находит все комментарии к объявлению, отсортированные по дате создания (сначала новые).
     * @param postId ID объявления
     * @return список комментариев
     */
    List<CommentPersonal> findByPostIdOrderByCreatedAtDesc(Long postId);

    /**
     * Находит дочерние комментарии (ответы) по ID родительского комментария,
     * отсортированные по дате создания (сначала старые).
     * @param parentCommentId ID родительского комментария
     * @return список ответов
     */
    List<CommentPersonal> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все ответы на комментарий (только прямые потомки, без рекурсии по вложенным).
     * @param parentId ID родительского комментария
     * @return список прямых ответов
     */
    @Query("SELECT cp FROM CommentPersonal cp WHERE cp.parentComment.id = :parentId")
    List<CommentPersonal> findAllRepliesByParentId(@Param("parentId") Long parentId);

    // ========== Методы для работы с непрочитанными сообщениями ==========

    /**
     * Находит ID объявлений, в которых есть непрочитанные сообщения для указанного пользователя.
     * Исключает сообщения, отправленные самим пользователем.
     * @param recipientId ID получателя
     * @return список ID объявлений с непрочитанными сообщениями
     */
    @Query("SELECT DISTINCT cp.post.id FROM CommentPersonal cp WHERE cp.recipient.id = :recipientId AND cp.user.id <> :recipientId AND cp.isRead = false")
    List<Long> findPostIdsWithUnreadMessages(@Param("recipientId") Long recipientId);

    /**
     * Отмечает все сообщения в указанном объявлении для указанного получателя как прочитанные.
     * @param postId ID объявления
     * @param recipientId ID получателя
     */
    @Modifying
    @Transactional
    @Query("UPDATE CommentPersonal cp SET cp.isRead = true WHERE cp.post.id = :postId AND cp.recipient.id = :recipientId AND cp.isRead = false")
    void markMessagesAsRead(@Param("postId") Long postId, @Param("recipientId") Long recipientId);

    /**
     * Находит все непрочитанные сообщения пользователя в указанном объявлении.
     * @param postId ID объявления
     * @param recipientId ID получателя
     * @return список непрочитанных сообщений, отсортированных по дате создания (сначала новые)
     */
    @Query("SELECT cp FROM CommentPersonal cp WHERE cp.post.id = :postId AND cp.recipient.id = :recipientId AND cp.isRead = false ORDER BY cp.createdAt DESC")
    List<CommentPersonal> findUnreadMessages(@Param("postId") Long postId, @Param("recipientId") Long recipientId);

    // ========== Slice-методы для пагинации ==========

    /**
     * Находит корневые комментарии (не являющиеся ответами) к объявлению с пагинацией.
     * Использует Slice вместо Page для лучшей производительности.
     * @param postId ID объявления
     * @param pageable параметры пагинации
     * @return срез комментариев
     */
    Slice<CommentPersonal> findSliceByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    /**
     * Находит комментарии к объявлению, которые видит пользователь (где он является отправителем или получателем).
     * Использует Slice для пагинации.
     * @param postId ID объявления
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return срез видимых комментариев
     */
    @Query("SELECT cp FROM CommentPersonal cp WHERE cp.post.id = :postId AND cp.parentComment IS NULL AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    Slice<CommentPersonal> findVisibleCommentsSliceForUser(@Param("postId") Long postId,
                                                           @Param("userId") Long userId,
                                                           Pageable pageable);

    /**
     * Находит все комментарии к объявлению (включая ответы), где пользователь является
     * отправителем ИЛИ получателем.
     * @param postId ID объявления
     * @param userId ID пользователя
     * @return список комментариев
     */
    @Query("SELECT cp FROM CommentPersonal cp WHERE cp.post.id = :postId AND " +
            "(cp.user.id = :userId OR cp.recipient.id = :userId)")
    List<CommentPersonal> findByPostIdAndUserOrRecipient(@Param("postId") Long postId,
                                                         @Param("userId") Long userId);

    // ========== Простые методы поиска ==========

    /**
     * Находит все комментарии, отправленные пользователем.
     * @param userId ID пользователя-отправителя
     * @return список комментариев пользователя
     */
    List<CommentPersonal> findByUserId(Long userId);

    /**
     * Находит все комментарии, адресованные пользователю.
     * @param recipientId ID пользователя-получателя
     * @return список комментариев для получателя
     */
    List<CommentPersonal> findByRecipientId(Long recipientId);

    /**
     * Находит все комментарии с указанным родительским ID.
     * @param parentCommentId ID родительского комментария
     * @return список дочерних комментариев
     */
    List<CommentPersonal> findByParentCommentId(Long parentCommentId);
}