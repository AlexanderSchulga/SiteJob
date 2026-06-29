package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserDetailsServiceAuth;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Реализация сервиса загрузки пользователей для Spring Security.
 * <p>
 * Предоставляет два способа аутентификации:
 * <ul>
 *   <li>{@link #loadUserByUsername(String)} — поиск по username ИЛИ nameuse (для обратной совместимости)</li>
 *   <li>{@link #loadUserByUsernameOnly(String)} — поиск ТОЛЬКО по username (основной режим)</li>
 * </ul>
 * </p>
 *
 * <p><b>Назначение методов:</b></p>
 * <ul>
 *   <li><b>loadUserByUsername</b> — используется в текущей production-среде,
 *       где часть пользователей зарегистрирована через nameuse</li>
 *   <li><b>loadUserByUsernameOnly</b> — для новой аутентификации,
 *       рекомендуется постепенно мигрировать на этот метод</li>
 * </ul>
 *
 * @author YourTeam
 * @version 1.0
 * @see UserDetailsService
 * @see UserDetailsServiceAuth
 */
@RequiredArgsConstructor
@Service
public class UserDetailsServiceImpl implements UserDetailsService, UserDetailsServiceAuth {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    /**
     * Загружает пользователя по username ИЛИ nameuse.
     * <p>
     * <b>Важно:</b> этот метод ищет сначала по полю {@code username},
     * затем по полю {@code nameuse}. Используется для обратной совместимости
     * со старыми аккаунтами.
     * </p>
     * <p>
     * Если пользователь не найден ни по одному из полей,
     * выбрасывается {@link UsernameNotFoundException}.
     * </p>
     *
     * @param identifier логин пользователя (может быть username или nameuse)
     * @return объект {@link UserDetails} (фактически сам {@link User})
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Поиск сначала по username, затем по nameuse (для обратной совместимости)
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByNameuse(identifier))
                .orElseThrow(() -> {
                    log.warn("User not found during authentication (search by username/nameuse)");
                    return new UsernameNotFoundException("User not found");
                });

        log.debug("User loaded successfully via username/nameuse search");
        return user;
    }

    /**
     * Загружает пользователя ТОЛЬКО по username.
     * <p>
     * <b>Рекомендуемый метод</b> для новой аутентификации.
     * Поиск выполняется исключительно по полю {@code username}.
     * </p>
     * <p>
     * Если пользователь не найден, выбрасывается {@link UsernameNotFoundException}.
     * </p>
     *
     * @param username логин пользователя (только поле username)
     * @return объект {@link UserDetails} (фактически сам {@link User})
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    public UserDetails loadUserByUsernameOnly(String username) throws UsernameNotFoundException {
        // Поиск только по username (основной метод аутентификации)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found during authentication (search by username only)");
                    return new UsernameNotFoundException("User not found");
                });

        log.debug("User loaded successfully via username-only search");
        return user;
    }
}