package jobforsite.sitejob.module.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("Превышен максимальный размер загружаемого файла: {}", e.getMessage());
        return "error/413";
    }

    /** ResponseStatusException (404, 403 и т.д.) — отдаём как есть, не логируем как 500. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException e, HttpServletRequest request) {
        log.warn("ResponseStatusException at {}: status={}, reason={}",
                request.getRequestURI(), e.getStatusCode(), e.getReason());
        return ResponseEntity.status(e.getStatusCode()).build();
    }

    /** Общий обработчик: для /api/** — пустой 500, для остального — HTML-страница ошибки. */
    @ExceptionHandler(Exception.class)
    public Object handleAll(Exception e, HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri != null && uri.startsWith("/api/")) {
            log.error("API error at {}: {}", uri, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.error("MVC error at {}: {}", uri, e.getMessage(), e);
        return "error/500";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResource(NoResourceFoundException e) {
        // не логируем как ERROR — это просто отсутствующий статический ресурс
        log.debug("Static resource not found: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }
}