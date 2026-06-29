package jobforsite.sitejob.module.product.repository;

import jobforsite.sitejob.module.product.model.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с сущностью Comment (обычные комментарии).
 * Предоставляет методы для пагинации, поиска корневых и дочерних комментариев,
 * а также рекурсивного поиска ответов.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ========== Slice-методы для пагинации ==========

    /**
     * Находит все комментарии к объявлению с пагинацией.
     * Использует Slice вместо Page для лучшей производительности.
     * @param postId ID объявления
     * @param pageable параметры пагинации
     * @return срез комментариев
     */
    Slice<Comment> findSliceByPostId(Long postId, Pageable pageable);

    /**
     * Находит только корневые комментарии (не являющиеся ответами на другие комментарии) к объявлению.
     * @param postId ID объявления
     * @param pageable параметры пагинации
     * @return срез корневых комментариев
     */
    Slice<Comment> findSliceByPostIdAndParentCommentIsNull(Long postId, Pageable pageable);

    // ========== Методы для работы с иерархией комментариев ==========

    /**
     * Находит дочерние комментарии (ответы) по ID родительского комментария,
     * отсортированные по дате создания (сначала старые).
     * @param parentCommentId ID родительского комментария
     * @return список ответов
     */
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все прямые дочерние комментарии для указанного родительского комментария.
     * @param parentId ID родительского комментария
     * @return список прямых ответов
     */
    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentId")
    List<Comment> findAllRepliesByParentId(@Param("parentId") Long parentId);

    // ========== Вспомогательные методы ==========

    /**
     * Подсчитывает количество комментариев у указанного объявления.
     * @param id ID объявления
     * @return количество комментариев
     */
    long countByPostId(Long id);
}