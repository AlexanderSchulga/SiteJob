package jobforsite.sitejob.module.shared.config.security;

import jobforsite.sitejob.module.users.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.net.URLEncoder;

/**
 * Конфигурация безопасности приложения.
 * <p>
 * Настраивает:
 * <ul>
 *   <li>Правила авторизации и доступ к эндпоинтам</li>
 *   <li>CSRF защиту с использованием cookie-токенов</li>
 *   <li>Защиту от кликджекинга и Content Security Policy (CSP)</li>
 *   <li>Форму входа и выход из системы</li>
 *   <li>Управление сессиями (защита от фиксации, ограничение количества сессий)</li>
 *   <li>Провайдер аутентификации с BCrypt-шифрованием</li>
 *   <li>Иерархию ролей (SUPER > ADMIN > USER)</li>
 * </ul>
 * </p>
 *
 * <p><b>Особенности безопасности:</b></p>
 * <ul>
 *   <li>Активна только для профилей, отличных от "test" (@Profile("!test"))</li>
 *   <li>BCrypt с коэффициентом стойкости 12</li>
 *   <li>Защита от фиксации сессии (migrateSession)</li>
 *   <li>Максимум 1 активная сессия на пользователя</li>
 *   <li>CSP с разрешениями для Yandex Maps и OpenStreetMap</li>
 * </ul>
 *
 * @see org.springframework.security.web.SecurityFilterChain
 * @see org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 */
@EnableMethodSecurity
@EnableScheduling
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    /**
     * Основная цепочка фильтров безопасности.
     * <p>
     * Настраивает:
     * <ul>
     *   <li>Правила доступа к URL</li>
     *   <li>CSRF-защиту</li>
     *   <li>Заголовки безопасности (CSP, frame-options)</li>
     *   <li>Логин и логаут</li>
     *   <li>Управление сессиями</li>
     * </ul>
     * </p>
     *
     * @param http объект для построения конфигурации безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception при ошибках конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Настройка CSRF с защитой от атак
        // Используем специальный обработчик для корректной работы с атрибутами запроса
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                // ==================== АВТОРИЗАЦИЯ ЗАПРОСОВ ====================
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты (доступны без аутентификации)
                        .requestMatchers(
                                "/",
                                "/index",
                                "/register",
                                "/login",
                                "/about",
                                "/page_one/viewsone",
                                "/page_one/viewsone/**",
                                "/viewsone**",
                                "/page_one/viewsonevac",
                                "/page_one/viewsonevac/**",
                                "/viewsonevac**",
                                "/page_one/viewsoneres",
                                "/page_one/viewsoneres/**",
                                "/viewsoneres**",
                                "/page_one/viewsonejob",
                                "/page_one/viewsonejob/**",
                                "/viewsonejob**",
                                "/postidviews/**",
                                "/postidviewsjob/**",
                                "/postidviewsvac/**",
                                "/postidviewsres/**",
                                "/uploads/**",
                                "/forgot-password",
                                "/debug-users",
                                "/verify-registration",
                                "/reset-password",
                                "/public/api/**",
                                "/confiden_agr/**",
                                "/error",
                                "/promotion",
                                "/api/rating/seller/**",
                                "/error/**",
                                "/maintenance",
                                "/page_one/favorites",
                                "/page_one/favorites/**",
                                "/page_one/reviews",
                                "/page_one/reviews/**",
                                "/page_one/reviews_search",
                                "/page_one/reviews_search/**",
                                "/user/**",
                                "/webfonts/**",
                                "/geo/**",
                                "/api/paid-carousel",
                                "/robots.txt",
                                "/sitemap.xml"
                               // "/phone-verification",
                              //  "/verify-phone",
                               // "/resend-flashcall"

                        ).permitAll()

                        // Статические ресурсы
                        .requestMatchers("/webfonts/**", "/css/**", "/js/**", "/images/**", "/robots.txt", "/sitemap.xml",
                                "/favicon.ico", "/favicon-16x16.png", "/favicon-32x32.png",
                                "/web-app-manifest-192x192.png",
                                "/web-app-manifest-512x512.png",
                                "/apple-touch-icon.png", "/site.webmanifest").permitAll()

                        // Административные эндпоинты (только SUPER-роль)
                        .requestMatchers("/admin-create-user/**").hasRole("SUPER")

                        // Рейтинг: оценка требует авторизации
                        .requestMatchers("/api/rating/rate/**").authenticated()

                        // Все остальные API требуют авторизации
                        .requestMatchers("/api/**").authenticated()

                        // Все остальные запросы требуют авторизации
                        .anyRequest().authenticated()

                )

                // ==================== CSRF ЗАЩИТА ====================
                // Улучшенная CSRF защита с использованием cookie-токенов
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        // Игнорируем CSRF для API и публичных эндпоинтов
                        .ignoringRequestMatchers("/api/rating/rate/**",
                                "/api/**",
                                "/public/api/**",
                                "/favorites/**",
                                "/page_one/favorites/**",
                                "/verify-phone",
                                "/resend-flashcall")
                )

// ==================== ЗАГОЛОВКИ БЕЗОПАСНОСТИ ====================
// Защита от кликджекинга и CSP с разрешением для Яндекс.Метрики, Карт и OpenStreetMap
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://api-maps.yandex.ru https://yastatic.net https://core-renderer-tiles.maps.yandex.net https://mc.yandex.ru; " +
                                                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                                                "font-src 'self' https://cdnjs.cloudflare.com data:; " +
                                                "img-src 'self' data: https: https://mc.yandex.ru; " +
                                                "connect-src 'self' https://api-maps.yandex.ru https://nominatim.openstreetmap.org https://mc.yandex.ru wss://mc.yandex.ru mc.yandex.ru;"
                                )
                        )
                )

                // ==================== ФОРМА ВХОДА ====================
                .formLogin(form -> form
                        .loginPage("/login")
                        // Кастомный обработчик ошибок с сохранением username для UX
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            response.sendRedirect("/login?error=true&username=" +
                                    (username != null ? URLEncoder.encode(username, "UTF-8") : ""));
                        })
                        .defaultSuccessUrl("/index", true)
                        .permitAll()
                )

                // ==================== ВЫХОД ИЗ СИСТЕМЫ ====================
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)   // Инвалидируем сессию
                        .deleteCookies("JSESSIONID")   // Удаляем cookie сессии
                        .permitAll()
                )

                // ==================== УПРАВЛЕНИЕ СЕССИЯМИ ====================
                // Базовая защита от аутентификационных атак
                .sessionManagement(session -> {
                    // Защита от фиксации сессии (создаем новую сессию при логине)
                    session.sessionFixation().migrateSession();

                    // Настройка максимального количества сессий
                    session.maximumSessions(1)
                            .maxSessionsPreventsLogin(false)  // false - разрешаем новый вход, завершая старый
                            .sessionRegistry(sessionRegistry())
                            .expiredUrl("/login?expired");

                    // URL для недействительной сессии
                    session.invalidSessionUrl("/login?invalid");

                    //для автоматического завершения сессий при истечении
                    session.sessionConcurrency(sessionConcurrency ->
                            sessionConcurrency.maximumSessions(1)
                                    .expiredUrl("/login?expired")
                    );
                });

        return http.build();
    }

    /**
     * Провайдер аутентификации на основе DAO.
     * <p>
     * Использует кастомный UserDetailsService для загрузки пользователей по username
     * и BCryptPasswordEncoder для проверки паролей.
     * </p>
     *
     * @return настроенный AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Используем метод загрузки только по username без проверки активности
        provider.setUserDetailsService(username -> userDetailsServiceImpl.loadUserByUsernameOnly(username));
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Шифровальщик паролей на основе BCrypt.
     * <p>
     * Коэффициент стойкости 12 обеспечивает баланс безопасности и производительности.
     * </p>
     *
     * @return PasswordEncoder для хеширования паролей
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Иерархия ролей для гибкого управления доступом.
     * <p>
     * Роли наследуют права нижестоящих ролей:
     * ROLE_SUPER имеет все права ROLE_ADMIN и ROLE_USER
     * ROLE_ADMIN имеет все права ROLE_USER
     * </p>
     *
     * @return настроенная иерархия ролей
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_SUPER > ROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }

    /**
     * Реестр сессий для отслеживания активных сессий пользователей.
     * <p>
     * Используется для ограничения максимального количества сессий на пользователя
     * и управления активными сессиями.
     * </p>
     *
     * @return SessionRegistry для управления сессиями
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Публикатор событий сессий.
     * <p>
     * Необходим для корректной работы SessionRegistry и отслеживания
     * событий создания/уничтожения сессий.
     * </p>
     *
     * @return HttpSessionEventPublisher для публикации событий сессий
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Настройка игнорирования статических ресурсов цепочкой безопасности.
     * <p>
     * Ресурсы из этих директорий не проходят через фильтры безопасности,
     * что повышает производительность.
     * </p>
     *
     * @return WebSecurityCustomizer для настройки игнорируемых путей
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/css/**",
                "/js/**",
                "/images/**",
                "/webfonts/**",
                "/uploads/**",
                "/favicon.ico",
                "/favicon-16x16.png",
                "/favicon-32x32.png",
                "/web-app-manifest-192x192.png",
                "/web-app-manifest-512x512.png",
                "/apple-touch-icon.png",
                "/site.webmanifest",
                "/robots.txt",
                "/sitemap.xml"
        );
    }
}