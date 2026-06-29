package jobforsite.sitejob.module.vacancy.repository;

import jobforsite.sitejob.module.vacancy.model.CommentVacancy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с комментариями вакансий.
 * Предоставляет методы для получения комментариев с пагинацией,
 * а также для поиска родительских и дочерних комментариев.
 */
public interface CommentRepositoryVac extends JpaRepository<CommentVacancy, Long> {

    /**
     * Возвращает срез (slice) комментариев по идентификатору вакансии с пагинацией.
     * Slice не содержит общее количество страниц, что эффективнее для больших нагрузок.
     *
     * @param vacancyId идентификатор вакансии
     * @param pageable параметры пагинации (номер страницы, размер)
     * @return срез комментариев
     */
    Slice<CommentVacancy> findSliceByVacancyId(Long vacancyId, Pageable pageable);

    /**
     * Возвращает срез корневых комментариев (без родителя) по идентификатору вакансии.
     * Используется для отображения первого уровня комментариев.
     *
     * @param vacancyId идентификатор вакансии
     * @param pageable параметры пагинации
     * @return срез корневых комментариев
     */
    Slice<CommentVacancy> findSliceByVacancyIdAndParentCommentIsNull(Long vacancyId, Pageable pageable);

    /**
     * Находит все дочерние комментарии для указанного родительского комментария,
     * отсортированные по дате создания по возрастанию (старые сверху).
     *
     * @param parentCommentId идентификатор родительского комментария
     * @return список дочерних комментариев
     */
    List<CommentVacancy> findByParentCommentIdOrderByCreatedAtAsc(Long parentCommentId);

    /**
     * Находит все дочерние комментарии рекурсивно по идентификатору родителя.
     * Использует JPQL-запрос для получения всех ответов на комментарий.
     *
     * @param parentId идентификатор родительского комментария
     * @return список дочерних комментариев (только прямые потомки, не рекурсивно всех уровней)
     */
    @Query("SELECT c FROM CommentVacancy c WHERE c.parentComment.id = :parentId")
    List<CommentVacancy> findAllRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Подсчитывает общее количество комментариев у вакансии.
     *
     * @param id идентификатор вакансии
     * @return количество комментариев
     */
    long countByVacancyId(Long id);
}