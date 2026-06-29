package jobforsite.sitejob.module.users.repository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Расширенный сервис для загрузки данных пользователя.
 * <p>
 * Отличается от стандартного {@link org.springframework.security.core.userdetails.UserDetailsService}
 * тем, что не требует обязательной загрузки всех связей (например, ролей и authorities),
 * а загружает только базовую информацию по username.
 * </p>
 * <p>
 * Используется в сценариях, где нужен минимальный набор данных о пользователе
 * без накладных расходов на связи.
 * </p>
 *
 * @author JobForSite Team
 * @version 1.0
 * @see UserDetails
 */
public interface UserDetailsServiceAuth {

    /**
     * Загружает пользователя только по username (без дополнительных связей).
     * <p>
     * В отличие от стандартного {@code loadUserByUsername}, этот метод может
     * использоваться для оптимизированных запросов, где не требуются
     * коллекции Authorities или другие связанные сущности.
     * </p>
     *
     * @param username имя пользователя для поиска
     * @return {@link UserDetails} с базовой информацией о пользователе
     * @throws UsernameNotFoundException если пользователь с таким username не найден
     */
    UserDetails loadUserByUsernameOnly(String username) throws UsernameNotFoundException;
}