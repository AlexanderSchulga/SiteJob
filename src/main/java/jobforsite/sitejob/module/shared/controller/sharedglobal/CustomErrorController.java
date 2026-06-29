package jobforsite.sitejob.module.shared.controller.sharedglobal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Глобальный контроллер обработки ошибок HTTP.
 * <p>
 * Перехватывает запросы на /error (стандартный путь Spring Boot)
 * и отображает кастомные страницы для статусов: 404, 403, 500, 503.
 * Также поддерживает отдельные эндпоинты /error/{code} и /maintenance.
 * </p>
 *
 * <p><b>Используемые шаблоны:</b> error/404, error/500, error/503, error/403, error/general, error/maintenance</p>
 *
 * @see org.springframework.boot.web.servlet.error.ErrorController
 */
@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);
    private static final String ERROR_PATH = "/error";

    /**
     * Основной обработчик ошибок, вызываемый Spring Boot при возникновении исключения или HTTP-ошибке.
     *
     * @param request HTTP-запрос, содержащий атрибуты ошибки (код, сообщение, исключение)
     * @param model   модель для передачи данных в представление
     * @return имя шаблона Thymeleaf/JSP (или путь к view)
     */
    @RequestMapping(ERROR_PATH)
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Логирование ошибки с уровнем WARN для клиентских ошибок и ERROR для серверных
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            String clientIp = getClientIp(request);

            if (statusCode >= 500) {
                log.error("Server error {} from IP: {} | Message: {} | Exception: {}",
                        statusCode, clientIp, message, exception != null ? exception.getClass().getSimpleName() : "none");
            } else if (statusCode >= 400) {
                log.warn("Client error {} from IP: {} | Message: {}",
                        statusCode, clientIp, message);
            } else {
                log.info("Unhandled status {} from IP: {}", statusCode, clientIp);
            }
        }

        // Определение целевой страницы по HTTP-статусу
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error/500";
            } else if (statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                return "error/503";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (statusCode == 413) {
                return "redirect:/error/413";
            }
        }

        // Дефолтная ошибка (например, 400 Bad Request, 405 Method Not Allowed и т.д.)
        log.debug("Falling back to general error page. Status: {}", status);
        model.addAttribute("errorCode", status != null ? status : "Unknown");
        model.addAttribute("errorMessage", message != null ? message : "Произошла ошибка");
        return "error/general";
    }

    /**
     * Прямой доступ к странице 404 Not Found.
     *
     * @param model модель с данными об ошибке
     * @return шаблон error/404
     */
    @RequestMapping("/error/404")
    public String error404(Model model) {
        log.info("Manual 404 page requested");
        model.addAttribute("errorCode", "404");
        model.addAttribute("errorTitle", "Страница не найдена");
        model.addAttribute("errorMessage", "Запрашиваемая страница не существует или была перемещена.");
        return "error/404";
    }

    /**
     * Прямой доступ к странице 500 Internal Server Error.
     *
     * @param model модель с данными об ошибке
     * @return шаблон error/500
     */
    @RequestMapping("/error/500")
    public String error500(Model model) {
        log.warn("Manual 500 error page requested");
        model.addAttribute("errorCode", "500");
        model.addAttribute("errorTitle", "Внутренняя ошибка сервера");
        model.addAttribute("errorMessage", "На сервере произошла непредвиденная ошибка. Мы уже работаем над её устранением.");
        return "error/500";
    }

    /**
     * Прямой доступ к странице 503 Service Unavailable.
     *
     * @param model модель с данными об ошибке
     * @return шаблон error/503
     */
    @RequestMapping("/error/503")
    public String error503(Model model) {
        log.info("Manual 503 service unavailable page requested");
        model.addAttribute("errorCode", "503");
        model.addAttribute("errorTitle", "Сервис временно недоступен");
        model.addAttribute("errorMessage", "В настоящее время ведутся технические работы. Пожалуйста, попробуйте позже.");
        return "error/503";
    }

    /**
     * Прямой доступ к странице 403 Forbidden.
     *
     * @param model модель с данными об ошибке
     * @return шаблон error/403
     */
    @RequestMapping("/error/403")
    public String error403(Model model) {
        log.warn("Manual 403 forbidden page requested");
        model.addAttribute("errorCode", "403");
        model.addAttribute("errorTitle", "Доступ запрещен");
        model.addAttribute("errorMessage", "У вас недостаточно прав для доступа к этой странице.");
        return "error/403";
    }

    @RequestMapping("/error/413")
    public String error413(Model model) {
        log.warn(" 413 page - превышен размер файла");
        model.addAttribute("errorCode", "413");
        model.addAttribute("errorTitle", "Размер файлов слишком большой");
        model.addAttribute("errorMessage", "Максимальный размер файла: 5 MB. Максимальный размер всех файлов 12MB Пожалуйста, сожмите изображение или выберите файл меньшего размера.");
        return "error/413";
    }

    /**
     * Страница технического обслуживания (maintenance mode).
     * <p>Может использоваться при ручном переключении сервиса в режим обслуживания.</p>
     *
     * @param model модель для передачи заголовка
     * @return шаблон error/maintenance
     */
    @RequestMapping("/maintenance")
    public String maintenance(Model model) {
        log.info("Maintenance mode page displayed");
        model.addAttribute("title", "Технические работы");
        return "error/maintenance";
    }

    /** Вспомогательный метод для получения IP-адреса клиента. */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty() && !"unknown".equalsIgnoreCase(xfHeader)) {
            return xfHeader.split(",")[0];
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }


}