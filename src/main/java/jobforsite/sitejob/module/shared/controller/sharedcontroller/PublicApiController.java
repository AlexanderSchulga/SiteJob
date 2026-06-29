package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Публичное REST API для доступа к постам всех типов.
 * <p>
 * Предоставляет открытые эндпоинты для получения:
 * <ul>
 *   <li>Товаров (products) - с фильтрацией по категории и пагинацией</li>
 *   <li>Услуг (services) - с фильтрацией по категории и пагинацией</li>
 *   <li>Вакансий (vacancies) - с фильтрацией по категории и пагинацией</li>
 *   <li>Резюме (resume) - с фильтрацией по категории и пагинацией</li>
 *   <li>Универсальный поиск по всем типам</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Внимание:</strong> Эндпоинты публичные, не требуют авторизации.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/public/api")
public class PublicApiController {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;

    private final PostResumeRepository postResumeRepository;
    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;

    private final PostResumeCategoryConfig postResumeCategoryConfig;

    private final ExperienceResumCategoryConfig experienceResumCategoryConfig;

    private final ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;

    private final WorkFormatVacancyConfig workFormatVacancyConfig;

    private final WorkFormatResumeConfig workFormatResumeConfig;



    // ==================== Эндпоинты по категориям ====================

    /**
     * Получает список товаров по указанной категории.
     * <p>
     * Если категория = "all", возвращает все товары.
     * </p>
     *
     * @param category категория товара или "all" для всех
     * @return список товаров
     */
    @GetMapping("/products/category/{category}")
    public List<PostText> getProductsByCategory(@PathVariable String category) {
        log.debug("API запрос: получение товаров по категории: {}", category);

        if ("all".equals(category)) {
            log.debug("Возвращаем все товары");
            return (List<PostText>) postRepo.findAll();
        }

        List<PostText> result = postRepo.findByCategory(category);
        log.debug("Найдено товаров: {}", result.size());
        return result;
    }

    /**
     * Получает список услуг по указанной категории.
     *
     * @param category категория услуги или "all" для всех
     * @return список услуг
     */
    @GetMapping("/services/category/{category}")
    public List<PostTextJob> getServicesByCategory(@PathVariable String category) {
        log.debug("API запрос: получение услуг по категории: {}", category);

        if ("all".equals(category)) {
            log.debug("Возвращаем все услуги");
            return (List<PostTextJob>) postRepoJob.findAll();
        }

        List<PostTextJob> result = postRepoJob.findByCategory(category);
        log.debug("Найдено услуг: {}", result.size());
        return result;
    }

    /**
     * Получает список вакансий по указанной категории.
     *
     * @param category категория вакансии или "all" для всех
     * @return список вакансий
     */
    @GetMapping("/vacancy/category/{category}")
    public List<Vacancy> getVacancyByCategory(@PathVariable String category) {
        log.debug("API запрос: получение вакансий по категории: {}", category);

        if ("all".equals(category)) {
            log.debug("Возвращаем все вакансии");
            return (List<Vacancy>) postVacancyRepository.findAll();
        }

        List<Vacancy> result = postVacancyRepository.findByCategory(category);
        log.debug("Найдено вакансий: {}", result.size());
        return result;
    }

    /**
     * Получает список резюме по указанной категории.
     *
     * @param category категория резюме или "all" для всех
     * @return список резюме
     */
    @GetMapping("/resume/category/{category}")
    public List<Resume> getResumeByCategory(@PathVariable String category) {
        log.debug("API запрос: получение резюме по категории: {}", category);

        if ("all".equals(category)) {
            log.debug("Возвращаем все резюме");
            return (List<Resume>) postResumeRepository.findAll();
        }

        List<Resume> result = postResumeRepository.findByCategory(category);
        log.debug("Найдено резюме: {}", result.size());
        return result;
    }

    // ==================== Универсальный поиск ====================

    /**
     * Универсальный поиск по всем типам постов (товары, услуги, вакансии, резюме).
     * <p>
     * Выполняет поиск по полю title (без учета регистра) во всех репозиториях,
     * объединяет результаты в единый список с указанием типа каждого поста.
     * </p>
     *
     * @param q поисковый запрос (обязательный)
     * @return список Map-объектов с полями: id, title, price/cash, category,
     *         categoryName, type, и для резюме и вакансий - experience, experienceName, workformat, workformatName
     */
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        log.info("Публичный API поиск: запрос='{}'", q);

        List<Map<String, Object>> results = new ArrayList<>();

        try {
            // Поиск в товарах
            List<PostText> productResults = postRepo.findByTitleContainingIgnoreCase(q);
            log.debug("Найдено товаров: {}", productResults.size());

            for (PostText product : productResults) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", product.getId());
                result.put("title", product.getTitle());
                result.put("price", product.getPrice());
                result.put("category", product.getCategory());
                result.put("categoryName", postTextCategoryConfig.getCategoryDisplayName(product.getCategory()));
                result.put("type", "product");
                results.add(result);
            }

            // Поиск в услугах
            List<PostTextJob> serviceResults = postRepoJob.findByTitleContainingIgnoreCase(q);
            log.debug("Найдено услуг: {}", serviceResults.size());

            for (PostTextJob service : serviceResults) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", service.getId());
                result.put("title", service.getTitle());
                result.put("price", service.getPrice());
                result.put("category", service.getCategory());
                result.put("categoryName", postTextJobCategoryConfig.getCategoryDisplayName(service.getCategory()));
                result.put("type", "service");
                results.add(result);
            }

            // Поиск в вакансиях
            List<Vacancy> vacancyResults = postVacancyRepository.findByTitleContainingIgnoreCase(q);
            log.debug("Найдено вакансий: {}", vacancyResults.size());

            for (Vacancy vacancy : vacancyResults) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", vacancy.getId());
                result.put("title", vacancy.getTitle());
                result.put("cash", vacancy.getCash());
                result.put("category", vacancy.getCategory());
                result.put("categoryName", postVacancyCategoryConfig.getCategoryDisplayName(vacancy.getCategory()));
                result.put("experience", vacancy.getExperience());
                result.put("workformat", vacancy.getWorkformat());
                result.put("experienceName", experienceVacancyCategoryConfig.getExperienDisplayName(vacancy.getExperience()));
                result.put("workformatName", workFormatVacancyConfig.getWorkformatDisplayName(vacancy.getWorkformat()));
                result.put("type", "vacancy");
                results.add(result);
            }

            // Поиск в резюме
            List<Resume> resumeResults = postResumeRepository.findByTitleContainingIgnoreCase(q);
            log.debug("Найдено резюме: {}", resumeResults.size());

            for (Resume resume : resumeResults) {
                Map<String, Object> result = new HashMap<>();
                result.put("id", resume.getId());
                result.put("title", resume.getTitle());
                result.put("cash", resume.getCash());
                result.put("category", resume.getCategory());
                result.put("categoryName", postResumeCategoryConfig.getCategoryDisplayName(resume.getCategory()));
                result.put("experience", resume.getExperience());
                result.put("workformat", resume.getWorkformat());
                result.put("experienceName", experienceResumCategoryConfig.getExperienDisplayName(resume.getExperience()));
                result.put("workformatName", workFormatResumeConfig.getWorkformatDisplayName(resume.getWorkformat()));
                result.put("type", "resume");
                results.add(result);
            }

            log.info("Публичный API поиск завершен: запрос='{}', всего результатов={}", q, results.size());

        } catch (Exception e) {
            log.error("Ошибка при публичном поиске: запрос='{}', ошибка={}", q, e.getClass().getSimpleName(), e);
        }

        return results;
    }

    // ==================== Эндпоинты с пагинацией ====================

    /**
     * Получает список всех товаров с пагинацией.
     *
     * @param page номер страницы (начиная с 0, по умолчанию 0)
     * @param size размер страницы (по умолчанию 20)
     * @return список товаров на указанной странице
     */
    @GetMapping("/products")
    public List<PostText> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("API запрос: получение товаров с пагинацией: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<PostText> result = postRepo.findAll(pageable).getContent();

        log.debug("Возвращено товаров: {}", result.size());
        return result;
    }

    /**
     * Получает список всех услуг с пагинацией.
     *
     * @param page номер страницы (начиная с 0, по умолчанию 0)
     * @param size размер страницы (по умолчанию 20)
     * @return список услуг на указанной странице
     */
    @GetMapping("/services")
    public List<PostTextJob> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("API запрос: получение услуг с пагинацией: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<PostTextJob> result = postRepoJob.findAll(pageable).getContent();

        log.debug("Возвращено услуг: {}", result.size());
        return result;
    }

    /**
     * Получает список всех вакансий с пагинацией.
     *
     * @param page номер страницы (начиная с 0, по умолчанию 0)
     * @param size размер страницы (по умолчанию 20)
     * @return список вакансий на указанной странице
     */
    @GetMapping("/vacancy")
    public List<Vacancy> getAllVacancy(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("API запрос: получение вакансий с пагинацией: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<Vacancy> result = postVacancyRepository.findAll(pageable).getContent();

        log.debug("Возвращено вакансий: {}", result.size());
        return result;
    }

    /**
     * Получает список всех резюме с пагинацией.
     *
     * @param page номер страницы (начиная с 0, по умолчанию 0)
     * @param size размер страницы (по умолчанию 20)
     * @return список резюме на указанной странице
     */
    @GetMapping("/resume")
    public List<Resume> getAllResume(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("API запрос: получение резюме с пагинацией: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<Resume> result = postResumeRepository.findAll(pageable).getContent();

        log.debug("Возвращено резюме: {}", result.size());
        return result;
    }
}