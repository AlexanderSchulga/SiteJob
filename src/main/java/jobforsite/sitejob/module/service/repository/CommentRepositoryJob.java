package jobforsite.sitejob.module.service.repository;

import jobforsite.sitejob.module.service.model.CommentJob;
import jobforsite.sitejob.module.vacancy.model.CommentVacancy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с обычными комментариями к объявлениям.
 * Обеспечивает пагинированную выборку комментариев, поиск корневых и дочерних комментариев,
 * а также подсчет количества комментариев у объявления.
 */
public interface CommentRepositoryJob extends JpaRepository<CommentJob, Long> {

    /**
     * Загружает ВСЕ комментарии вакансии (и корневые, и все уровни вложенности).
     * Используется для построения полного дерева комментариев на клиенте.
     */
    List<CommentJob> findAllByPostjobIdOrderByCreatedAtAsc(Long postId);

    /**
     * Находит все комментарии к объявлению с пагинацией.
     * Включает как корневые, так и дочерние комментарии.
     *
     * @param postId   ID объявления
     * @param pageable параметры пагинации
     * @return срез (slice) комментариев
     */
    Slice<CommentJob> findSliceByPostjobId(Long postId, Pageable pageable);

    /**
     * Находит только корневые комментарии (без родителя) к объявлению с пагинацией.
     * Дочерние комментарии не включаются в результат.
     *
     * @param postId   ID объявления
     * @param pageable параметры пагинации
     * @return срез (slice) корневых комментариев
     */
    Slice<CommentJob> findSliceByPostjobIdAndParentCommentIsNull(Long postId, Pageable pageable);

    /**
     * Находит дочерние комментарии (ответы) для заданного родительского комментария.
     * Результат сортируется по возрастанию даты создания (старые сверху).
     *
     * @param parentCommentId ID родительского комментария
     * @return список дочерних комментариев
     */
    List<CommentJob> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все прямые дочерние комментарии по ID родителя.
     * Не рекурсивный (только первый уровень вложенности).
     *
     * @param parentId ID родительского комментария
     * @return список дочерних комментариев
     */
    @Query("SELECT c FROM CommentJob c WHERE c.parentComment.id = :parentId")
    List<CommentJob> findAllRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Подсчитывает общее количество комментариев у объявления.
     * Включает как корневые, так и дочерние комментарии.
     *
     * @param id ID объявления
     * @return количество комментариев
     */
    long countByPostjobId(Long id);
}