package jobforsite.sitejob.module.shared.repository;

import jobforsite.sitejob.module.shared.model.Reviews;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с отзывами (рецензиями) о пользователях.
 * <p>
 * Предоставляет методы для поиска отзывов по автору, получателю,
 * полнотекстового поиска по имени пользователя, проверки существования отзывов.
 * </p>
 */
public interface ReviewsRepo extends JpaRepository<Reviews, Long> {

    /**
     * Возвращает корневые отзывы (без родительского отзыва) о пользователе с заданным ID.
     * <p>
     * Используется жадная загрузка (EntityGraph) для replies и author.
     * Возвращает Slice для поддержки пагинации.
     * </p>
     *
     * @param userId идентификатор пользователя, о котором оставлены отзывы
     * @param pageable параметры пагинации
     * @return срез (Slice) корневых отзывов
     */
    @EntityGraph(attributePaths = {"replies", "author"})
    Slice<Reviews> findByReviewedUserIdAndParentReviewIsNull(Long userId, Pageable pageable);

    /**
     * Находит все отзывы о пользователе (включая ответы и удалённые, если они не скрыты фильтром).
     * Сортировка по дате создания: от новых к старым.
     *
     * @param reviewedUserId идентификатор пользователя, о котором отзывы
     * @return список отзывов
     */
    List<Reviews> findByReviewedUserIdOrderByCreatedAtDesc(Long reviewedUserId);

    /**
     * Находит только корневые отзывы (без ответов) о пользователе.
     * Сортировка по дате создания: от новых к старым.
     *
     * @param reviewedUserId идентификатор пользователя, о котором отзывы
     * @return список корневых отзывов
     */
    List<Reviews> findByReviewedUserIdAndParentReviewIsNullOrderByCreatedAtDesc(Long reviewedUserId);

    /**
     * Полнотекстовый поиск отзывов по имени пользователя (reviewed_nameuse).
     * Используется расширенный булевый режим FULLTEXT с подстановкой '*' (префиксный поиск).
     *
     * @param searchTerm поисковый запрос
     * @return список отзывов, где reviewed_nameuse содержит searchTerm как префикс
     */
    @Query(value = "SELECT * FROM reviews WHERE MATCH(reviewed_nameuse) AGAINST(CONCAT('*', :searchTerm, '*') IN BOOLEAN MODE)",
            nativeQuery = true)
    List<Reviews> findByReviewedUserSearchTerm(@Param("searchTerm") String searchTerm);

    /**
     * Комплексный полнотекстовый поиск по имени пользователя (reviewed_nameuse) в естественном режиме.
     * Аналогичен findByReviewedUserSearchTerm, но без префиксов.
     *
     * @param searchTerm поисковый запрос
     * @return список отзывов
     */
    @Query(value = "SELECT * FROM reviews WHERE MATCH(reviewed_nameuse) AGAINST(:searchTerm IN NATURAL LANGUAGE MODE)",
            nativeQuery = true)
    List<Reviews> findByReviewedUserContacts(@Param("searchTerm") String searchTerm);

    /**
     * Проверяет, есть ли хотя бы один отзыв о пользователе с указанным ID.
     *
     * @param reviewedUserId идентификатор пользователя
     * @return true, если отзывы существуют, иначе false
     */
    boolean existsByReviewedUserId(Long reviewedUserId);

    /**
     * Находит все отзывы, которые оставил автор с указанным ID.
     * Сортировка по дате создания: от новых к старым.
     *
     * @param authorId идентификатор автора отзыва
     * @return список отзывов, написанных автором
     */
    List<Reviews> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /**
     * Проверяет существование отзывов по имени пользователя (reviewed_nameuse).
     *
     * @param nameuse имя пользователя
     * @return количество отзывов
     */
    @Query(value = "SELECT COUNT(*) FROM reviews WHERE reviewed_nameuse = :nameuse",
            nativeQuery = true)
    int countByReviewedNameuse(@Param("nameuse") String nameuse);
}