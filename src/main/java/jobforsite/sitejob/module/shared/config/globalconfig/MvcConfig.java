package jobforsite.sitejob.module.shared.config.globalconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC конфигурация для обработки статических ресурсов и страниц ошибок.
 * <p>
 * Настраивает:
 * <ul>
 *     <li>Обработку статических ресурсов (загруженные файлы)</li>
 *     <li>Страницы ошибок для различных HTTP статусов и исключений</li>
 * </ul>
 * </p>
 *
 * @author JobForsite Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(MvcConfig.class);

    /**
     * Настраивает обработчики статических ресурсов.
     * <p>
     * Добавляет ресурсный обработчик для пути "/uploads/**",
     * который обслуживает файлы из локальной директории "./uploads/".
     * </p>
     *
     * @param registry реестр обработчиков ресурсов
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.debug("Регистрация обработчика статических ресурсов для uploads");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");

        log.debug("Обработчик ресурсов /uploads/** зарегистрирован с локацией file:./uploads/");
    }

    /**
     * Создает регистратор страниц ошибок.
     * <p>
     * Возвращает бин {@link ErrorPageRegistrar}, который настраивает
     * отображение HTTP статусов и исключений на пользовательские страницы ошибок.
     * </p>
     *
     * @return регистратор страниц ошибок
     */
    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        log.debug("Создание регистратора страниц ошибок");
        return new MyErrorPageRegistrar();
    }

    /**
     * Внутренний класс для регистрации страниц ошибок.
     * <p>
     * Настраивает отображение следующих ошибок:
     * <ul>
     *     <li>404 NOT_FOUND → /error/404</li>
     *     <li>500 INTERNAL_SERVER_ERROR → /error/500</li>
     *     <li>503 SERVICE_UNAVAILABLE → /error/503</li>
     *     <li>403 FORBIDDEN → /error/403</li>
     *     <li>Exception (любые исключения) → /error/500</li>
     * </ul>
     * </p>
     */
    private static class MyErrorPageRegistrar implements ErrorPageRegistrar {

        private static final Logger log = LoggerFactory.getLogger(MyErrorPageRegistrar.class);

        /**
         * Регистрирует страницы ошибок в приложении.
         *
         * @param registry реестр страниц ошибок
         */
        @Override
        public void registerErrorPages(ErrorPageRegistry registry) {
            log.info("Настройка страниц ошибок приложения");

            registry.addErrorPages(
                    new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"),
                    new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500"),
                    new ErrorPage(HttpStatus.SERVICE_UNAVAILABLE, "/error/503"),
                    new ErrorPage(HttpStatus.FORBIDDEN, "/error/403"),
                    new ErrorPage(HttpStatus.PAYLOAD_TOO_LARGE, "/error/413"),
                    new ErrorPage(HttpStatus.TOO_MANY_REQUESTS, "/error/429"),
                    new ErrorPage(Exception.class, "/error/500")
            );

            log.info("Страницы ошибок зарегистрированы: 404, 403, 500, 503, и для всех исключений");
        }
    }
}