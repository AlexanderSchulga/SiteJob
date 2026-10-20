package jobforsite.sitejob.module.users.repository;

import jobforsite.sitejob.module.users.model.Blacklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Репозиторий для работы с чёрным списком пользователей.
 * <p>
 * Предоставляет методы для поиска заблокированных контактов
 * по email, основному и дополнительному телефону,
 * а также проверку наличия любого из контактов в чёрном списке.
 * </p>
 *
 * @author JobForSite Team
 * @version 1.0
 * @see Blacklist
 */
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {


    /**
     * Поиск записи в чёрном списке по email.
     *
     * @param email email пользователя
     * @return Optional с записью, если найдена
     */
    Optional<Blacklist> findByEmail(String email);

    /**
     * Поиск записи в чёрном списке по основному телефону.
     *
     * @param phone основной номер телефона
     * @return Optional с записью, если найдена
     */
    //Optional<Blacklist> findByPhone(String phone);

    /**
     * Поиск записи в чёрном списке по дополнительному телефону.
     *
     * @param phonePost дополнительный номер телефона
     * @return Optional с записью, если найдена
     */
   // Optional<Blacklist> findByPhonePost(String phonePost);

    /**
     * Проверяет, содержится ли хотя бы один из переданных контактов в чёрном списке.
     * <p>
     * Используется при регистрации нового пользователя для предотвращения
     * повторной блокировки.
     * </p>
     *
     * @param email     email для проверки
    // * @param phone     основной телефон для проверки
     //* @param phonePost дополнительный телефон для проверки
     * @return true, если хотя бы один контакт найден в чёрном списке
     */
    @Query("SELECT COUNT(b) > 0 FROM Blacklist b WHERE b.email = :email")
    boolean existsByAnyContact(@Param("email") String email);
    //                               @Param("phone") String phone,
    //                               @Param("phonePost") String phonePost);

}