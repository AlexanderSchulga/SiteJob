package jobforsite.sitejob.module.users.repository;

import jobforsite.sitejob.module.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с пользователями.
 * <p>
 * Предоставляет методы для поиска пользователей по различным полям,
 * пагинированного поиска для админ-панели с разделением прав (админ/суперадмин),
 * а также массового обновления онлайн-статуса.
 * </p>
 *
 * @author JobForSite Team
 * @version 1.0
 * @see User
 */
public interface UserRepository extends JpaRepository<User, Long> {


    // ========== ПОИСК ПО УНИКАЛЬНЫМ ПОЛЯМ ==========

    /**
     * Поиск пользователя по логину (username).
     *
     * @param username имя пользователя
     * @return Optional с пользователем
     */
    Optional<User> findByUsername(String username);

    /**
     * Поиск пользователя по отображаемому имени (nameuse).
     *
     * @param nameuse отображаемое имя
     * @return Optional с пользователем
     */
    Optional<User> findByNameuse(String nameuse);

    /**
     * Поиск пользователя по email.
     *
     * @param email электронная почта
     * @return Optional с пользователем
     */
    Optional<User> findByEmail(String email);

    /**
     * Поиск пользователя по основному телефону.
     *
     * @param phone номер телефона
     * @return Optional с пользователем
     */
    Optional<User> findByPhone(String phone);

    /**
     * Поиск пользователя по ID.
     *
     * @param Id идентификатор пользователя
     * @return Optional с пользователем
     */
    Optional<User> findById(Long Id);

    /**
     * Поиск пользователя по hashId (для публичных ссылок).
     *
     * @param hashId уникальный хеш-идентификатор
     * @return Optional с пользователем
     */
    Optional<User> findByHashId(String hashId);

    // ========== ПОИСК С ИСКЛЮЧЕНИЕМ АДМИНОВ ==========

    /**
     * Поиск пользователей по поисковому запросу с исключением суперадминов и администраторов.
     * <p>
     * Используется для публичных страниц поиска.
     * </p>
     *
     * @param searchTerm поисковая строка (ищет в nameuse)
     * @return список пользователей
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nameuse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
            "u.role <> 'ROLE_SUPER' AND u.role <> 'ROLE_ADMIN' ")
    List<User> findBySearchTermExcludingSuper(@Param("searchTerm") String searchTerm);

    // ========== МЕТОДЫ ДЛЯ АДМИН-ПАНЕЛИ ==========

    /**
     * Получение всех обычных пользователей (ROLE_USER) для админа.
     * <p>
     * Админ не видит других админов и суперадминов.
     * Результат отсортирован по дате регистрации (новые сверху).
     * </p>
     *
     * @param pageable параметры пагинации
     * @return Slice с пользователями
     */
    @Query("SELECT u FROM User u WHERE u.role = 'ROLE_USER' ORDER BY u.registrationDate DESC")
    Slice<User> findAllUsersForAdmin(Pageable pageable);

    /**
     * Получение всех пользователей, кроме текущего, для суперадмина.
     * <p>
     * Суперадмин видит всех, включая админов, но не видит себя.
     * </p>
     *
     * @param currentUserId ID текущего суперадмина
     * @param pageable      параметры пагинации
     * @return Slice с пользователями
     */
    @Query("SELECT u FROM User u WHERE u.id <> :currentUserId ORDER BY u.registrationDate DESC")
    Slice<User> findAllExcludingUser(@Param("currentUserId") Long currentUserId, Pageable pageable);

    /**
     * Поиск среди обычных пользователей (ROLE_USER) для админа.
     * <p>
     * Поиск ведётся по: nameuse, email, phone, phonePost.
     * Исключаются все не-пользователи.
     * </p>
     *
     * @param searchTerm поисковая строка
     * @param currentUserId ID текущего админа (не используется в запросе, оставлен для совместимости)
     * @param pageable параметры пагинации
     * @return Slice с найденными пользователями
     */
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.nameuse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "u.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
            "u.phonePost LIKE CONCAT('%', :searchTerm, '%')) AND " +
            "u.role = 'ROLE_USER'")
    Slice<User> findBySearchTermForAdmin(@Param("searchTerm") String searchTerm,
                                         @Param("currentUserId") Long currentUserId,
                                         Pageable pageable);

    /**
     * Поиск среди всех пользователей, кроме текущего, для суперадмина.
     * <p>
     * Поиск ведётся по: nameuse, email, phone, phonePost.
     * Исключается только суперадмин (игнорируются роли).
     * </p>
     *
     * @param searchTerm     поисковая строка
     * @param currentUserId  ID текущего суперадмина (исключается из результатов)
     * @param pageable       параметры пагинации
     * @return Slice с найденными пользователями
     */
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.nameuse) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "u.phone LIKE CONCAT('%', :searchTerm, '%') OR " +
            "u.phonePost LIKE CONCAT('%', :searchTerm, '%')) AND " +
            "u.id <> :currentUserId")
    Slice<User> findBySearchTermExcludingUser(@Param("searchTerm") String searchTerm,
                                              @Param("currentUserId") Long currentUserId,
                                              Pageable pageable);

    // ========== МАССОВЫЕ ОПЕРАЦИИ ==========

    /**
     * Получение списка пользователей по списку ролей.
     *
     * @param roles список ролей (например, ['ROLE_USER', 'ROLE_ADMIN'])
     * @return список пользователей
     */
    List<User> findByRoleIn(List<String> roles);

    /**
     * Прямое обновление lastSeen и online статуса без загрузки сущности в контекст.
     * <p>
     * Используется оптимизированный UPDATE запрос.
     * </p>
     *
     * @param userId    ID пользователя
     * @param timestamp время последнего визита
     * @return количество обновлённых строк (0 или 1)
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastSeen = :timestamp, u.online = true WHERE u.id = :userId")
    int updateLastSeenAndOnline(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);


    /**
     * Принудительно устанавливает онлайн-статус пользователя.
     *
     * @param userId ID пользователя
     * @param online новый статус онлайн (true/false)
     * @return количество обновлённых строк
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.online = :online WHERE u.id = :userId")
    int updateOnlineStatus(@Param("userId") Long userId, @Param("online") boolean online);


    /**
     * Сбрасывает онлайн-статус для пользователей, чей last_seen старше указанного времени.
     *
     * @param cutoffTime время отсечки
     * @return количество обновлённых пользователей
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.online = false WHERE u.online = true AND u.lastSeen < :cutoffTime")
    int resetStaleOnlineStatuses(@Param("cutoffTime") LocalDateTime cutoffTime);


    /**
     * Находит всех заблокированных пользователей с истекшим сроком блокировки.
     * <p>
     * Использует JPQL-запрос вместо загрузки всех пользователей в память.
     * </p>
     *
     * @param now текущее время
     * @return список пользователей с истекшей блокировкой
     */
    @Query("SELECT u FROM User u WHERE u.active = false AND u.blockUntil IS NOT NULL AND u.blockUntil < :now")
    List<User> findUsersWithExpiredBlock(@Param("now") LocalDateTime now);

}
