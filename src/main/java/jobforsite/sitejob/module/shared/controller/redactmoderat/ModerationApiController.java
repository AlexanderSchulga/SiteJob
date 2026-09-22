package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST-контроллер для работы с модерацией публикаций.
 * <p>
 * Предоставляет API для получения статистики по количеству
 * объектов, находящихся на модерации (товары, услуги, вакансии, резюме).
 */
@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
public class ModerationApiController {

    private final ModerationPostsCacheService moderationPostsCacheService;

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getModerationCount() {
        Map<String, Long> response = new HashMap<>();
        response.put("total", moderationPostsCacheService.getTotalModerationCount());
        response.put("products", moderationPostsCacheService.getModerationProductsCount());
        response.put("services", moderationPostsCacheService.getModerationServicesCount());
        response.put("vacancies", moderationPostsCacheService.getModerationVacanciesCount());
        response.put("resume", moderationPostsCacheService.getModerationResumeCount());
        return ResponseEntity.ok(response);
    }
}