package jobforsite.sitejob.module.shared.repository;

import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с обращениями в поддержку (тикетами).
 * <p>
 * Поддерживает ролевую модель: SUPER, ADMIN и обычные пользователи.
 * Реализует логику назначения (assignedSuper), статуса прочтения (isRead), категорий тикетов.
 * </p>
 */
public interface SupportCommentRepo extends JpaRepository<SupportComment, Long> {

    // ================ МЕТОДЫ ДЛЯ ТРЕБОВАНИЙ (заголовок, уведомления) ================

    /**
     * Проверяет наличие невзятых (unassigned) сообщений в указанной категории для SUPER/ADMIN.
     * Используется для отображения индикатора в шапке сайта.
     *
     * @param category категория тикета (например, "general", "finance")
     * @return true, если есть хотя бы одно невзятое сообщение от обычного пользователя
     */
    @Query("SELECT COUNT(sc) > 0 FROM SupportComment sc WHERE " +
            "sc.assignedSuper IS NULL AND sc.user.role NOT IN ('ROLE_SUPER', 'ROLE_ADMIN') " +
            "AND sc.category = :category")
    boolean hasUnassignedMessagesForSuperInCategory(@Param("category") String category);

    /**
     * Возвращает список категорий, в которых есть невзятые сообщения (для SUPER/ADMIN).
     *
     * @return список категорий с невзятыми тикетами
     */
    @Query("SELECT DISTINCT sc.category FROM SupportComment sc WHERE " +
            "sc.assignedSuper IS NULL AND sc.user.role NOT IN ('ROLE_SUPER', 'ROLE_ADMIN')")
    List<String> findCategoriesWithUnassignedMessages();

    /**
     * Проверяет, есть ли у пользователя непрочитанные ответы от SUPER/ADMIN.
     * Используется для отображения уведомлений в шапке и по категориям.
     *
     * @param userId идентификатор пользователя (получателя ответа)
     * @return true, если есть хотя бы один непрочитанный ответ от SUPER/ADMIN
     */
    @Query("SELECT COUNT(sc) > 0 FROM SupportComment sc WHERE " +
            "sc.user.role IN ('ROLE_SUPER', 'ROLE_ADMIN') AND sc.isRead = false " +
            "AND sc.parentComment.user.id = :userId")
    boolean hasUnreadSuperRepliesForUser(@Param("userId") Long userId);

    /**
     * Возвращает список категорий, в которых у пользователя есть непрочитанные ответы от SUPER/ADMIN.
     *
     * @param userId идентификатор пользователя
     * @return список категорий с непрочитанными ответами
     */
    @Query("SELECT DISTINCT sc.category FROM SupportComment sc WHERE " +
            "sc.user.role IN ('ROLE_SUPER', 'ROLE_ADMIN') AND sc.isRead = false " +
            "AND sc.parentComment.user.id = :userId")
    List<String> findCategoriesWithUnreadSuperRepliesForUser(@Param("userId") Long userId);

    // ================ МЕТОДЫ С SLICE ПАГИНАЦИЕЙ (списки тикетов) ================

    /**
     * SUPER видит ВСЕ тикеты (корневые комментарии) в указанной категории без каких-либо фильтров.
     *
     * @param category категория тикета
     * @param pageable параметры пагинации
     * @return срез (Slice) корневых тикетов
     */
    @Query("SELECT sc FROM SupportComment sc WHERE sc.category = :category " +
            "AND sc.parentComment IS NULL " +
            "ORDER BY sc.createdAt DESC")
    Slice<SupportComment> findAllRootCommentsForSuperAll(
            @Param("category") String category,
            Pageable pageable);

    /**
     * ADMIN видит свободные (unassigned) + свои взятые тикеты в указанной категории.
     *
     * @param category категория тикета
     * @param superId идентификатор SUPER/ADMIN (тот, кто взял тикет в работу)
     * @param pageable параметры пагинации
     * @return срез доступных и назначенных текущему SUPER/ADMIN тикетов
     */
    @Query("SELECT sc FROM SupportComment sc WHERE sc.category = :category " +
            "AND sc.parentComment IS NULL " +
            "AND (sc.assignedSuper IS NULL OR sc.assignedSuper.id = :superId) " +
            "ORDER BY sc.createdAt DESC")
    Slice<SupportComment> findAllRootCommentsForSuper(
            @Param("category") String category,
            @Param("superId") Long superId,
            Pageable pageable);

    /**
     * Обычный пользователь видит ТОЛЬКО свои собственные тикеты.
     *
     * @param category категория тикета
     * @param userId идентификатор пользователя (автора тикета)
     * @param pageable параметры пагинации
     * @return срез тикетов, созданных указанным пользователем
     */
    @Query("SELECT sc FROM SupportComment sc WHERE sc.category = :category " +
            "AND sc.parentComment IS NULL " +
            "AND sc.user.id = :userId " +
            "ORDER BY sc.createdAt DESC")
    Slice<SupportComment> findUserTicketsIncludingAssigned(
            @Param("category") String category,
            @Param("userId") Long userId,
            Pageable pageable);

    // ================ УТИЛИТЫ ДЛЯ НЕПРОЧИТАННЫХ (isRead) ================

    /**
     * Возвращает список категорий, где у SUPER/ADMIN есть непрочитанные сообщения от обычных пользователей.
     *
     * @param superId идентификатор SUPER/ADMIN
     * @return список категорий с непрочитанными для SUPER/ADMIN сообщениями
     */
    @Query("SELECT DISTINCT sc.category FROM SupportComment sc WHERE " +
            "sc.isRead = false AND sc.user.role NOT IN ('ROLE_SUPER', 'ROLE_ADMIN') " +
            "AND (sc.assignedSuper IS NULL OR sc.assignedSuper.id = :superId)")
    List<String> findCategoriesWithUnreadForSuper(@Param("superId") Long superId);

    /**
     * Помечает все сообщения в указанной категории как прочитанные для SUPER/ADMIN.
     *
     * @param category категория тикетов
     * @param superId идентификатор SUPER/ADMIN (для проверки прав назначения)
     */
    @Modifying
    @Transactional
    @Query("UPDATE SupportComment sc SET sc.isRead = true WHERE " +
            "sc.category = :category AND sc.isRead = false " +
            "AND sc.user.role NOT IN ('ROLE_SUPER', 'ROLE_ADMIN') " +
            "AND (sc.assignedSuper IS NULL OR sc.assignedSuper.id = :superId)")
    void markCategoryAsReadForSuper(@Param("category") String category,
                                    @Param("superId") Long superId);

    /**
     * Помечает все сообщения в указанной категории как прочитанные для обычного пользователя.
     *
     * @param category категория тикетов
     * @param userId идентификатор пользователя (получателя)
     */
    @Modifying
    @Transactional
    @Query("UPDATE SupportComment sc SET sc.isRead = true WHERE " +
            "sc.category = :category AND sc.isRead = false " +
            "AND sc.parentComment.user.id = :userId AND sc.user.role IN ('ROLE_SUPER', 'ROLE_ADMIN')")
    void markCategoryAsReadForUser(@Param("category") String category,
                                   @Param("userId") Long userId);

    /**
     * Проверяет, есть ли у SUPER/ADMIN хотя бы одно непрочитанное сообщение в любых категориях.
     *
     * @param superId идентификатор SUPER/ADMIN
     * @return true, если есть непрочитанные сообщения
     */
    @Query("SELECT COUNT(sc) > 0 FROM SupportComment sc WHERE " +
            "sc.isRead = false AND sc.user.role NOT IN ('ROLE_SUPER', 'ROLE_ADMIN') " +
            "AND (sc.assignedSuper IS NULL OR sc.assignedSuper.id = :superId)")
    boolean hasUnreadForSuper(@Param("superId") Long superId);

    // ================ СТАНДАРТНЫЕ МЕТОДЫ ПОИСКА ПО ПОЛЬЗОВАТЕЛЮ ================

    /**
     * Находит все сообщения поддержки, созданные указанным пользователем.
     *
     * @param user пользователь-автор
     * @return список комментариев
     */
    List<SupportComment> findByUser(User user);

    /**
     * Находит все сообщения поддержки, где указанный пользователь является автором родительского комментария.
     *
     * @param user пользователь-автор родительского тикета
     * @return список комментариев
     */
    List<SupportComment> findByParentCommentUser(User user);
}