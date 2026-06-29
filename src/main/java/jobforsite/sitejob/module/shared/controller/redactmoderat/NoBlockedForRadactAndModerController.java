package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Добавлен импорт для логирования
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Контроллер для управления блокировкой/разблокировкой различных типов записей
 * (товары, услуги, вакансии, резюме) редактором или модератором.
 * Используется на странице /page_two/redactorblocked
 */
@Slf4j // Поддержка логирования
@RequiredArgsConstructor
@Controller
public class NoBlockedForRadactAndModerController {

    // Репозитории для работы с различными типами сущностей
    private final PostRepo postRepo;                     // Товары
    private final PostRepoJob postRepoJob;               // Услуги
    private final PostVacancyRepository postVacancyRepository; // Вакансии
    private final PostResumeRepository postResumeRepository;   // Резюме

    // Сервис для очистки кэша заблокированных записей
    private final BlockedPostsCacheService blockedPostsCacheService;

    // ------------------------------------------------------------------------
    // Публичные методы-обработчики POST-запросов (точки входа)
    // ------------------------------------------------------------------------

    /**
     * Блокировка/разблокировка товара (продукта)
     * @param id - ID товара
     * @param authentication - данные аутентификации текущего пользователя
     * @param model - модель для передачи сообщений об ошибках
     * @return редирект на страницу заблокированных записей или страницу ошибки
     */
    @PostMapping("/page_two/redactorblocked/{id}/toggle-block")
    public String redactorblockedtoggleBlockPost(@PathVariable(value = "id") Long id,
                                                 Authentication authentication,
                                                 Model model) {
        log.info("Запрос на блокировку/разблокировку товара с id: {}", id);
        return redactorblockedtoggleBlock(id, authentication, model, "product");
    }

    /**
     * Блокировка/разблокировка услуги (джоба)
     * @param id - ID услуги
     * @param authentication - данные аутентификации текущего пользователя
     * @param model - модель для передачи сообщений об ошибках
     * @return редирект на страницу заблокированных записей или страницу ошибки
     */
    @PostMapping("/page_two/redactorblocked/job/{id}/toggle-block")
    public String redactorblockedtoggleBlockJob(@PathVariable(value = "id") Long id,
                                                Authentication authentication,
                                                Model model) {
        log.info("Запрос на блокировку/разблокировку услуги с id: {}", id);
        return redactorblockedtoggleBlock(id, authentication, model, "service");
    }

    /**
     * Блокировка/разблокировка вакансии
     * @param id - ID вакансии
     * @param authentication - данные аутентификации текущего пользователя
     * @param model - модель для передачи сообщений об ошибках
     * @return редирект на страницу заблокированных записей или страницу ошибки
     */
    @PostMapping("/page_two/redactorblocked/vac/{id}/toggle-block")
    public String redactorblockedtoggleBlockVacancy(@PathVariable(value = "id") Long id,
                                                    Authentication authentication,
                                                    Model model) {
        log.info("Запрос на блокировку/разблокировку вакансии с id: {}", id);
        return redactorblockedtoggleBlock(id, authentication, model, "vacancy");
    }

    /**
     * Блокировка/разблокировка резюме
     * @param id - ID резюме
     * @param authentication - данные аутентификации текущего пользователя
     * @param model - модель для передачи сообщений об ошибках
     * @return редирект на страницу заблокированных записей или страницу ошибки
     */
    @PostMapping("/page_two/redactorblocked/res/{id}/toggle-block")
    public String redactorblockedtoggleBlockResume(@PathVariable(value = "id") Long id,
                                                   Authentication authentication,
                                                   Model model) {
        log.info("Запрос на блокировку/разблокировку резюме с id: {}", id);
        return redactorblockedtoggleBlock(id, authentication, model, "resume");
    }

    // ------------------------------------------------------------------------
    // Приватный общий метод с основной логикой
    // ------------------------------------------------------------------------

    /**
     * Общий метод для блокировки/разблокировки записей любого поддерживаемого типа.
     *
     * @param id             ID записи
     * @param authentication данные аутентификации (нужны для проверки роли)
     * @param model          модель для сообщений об ошибках
     * @param type           тип записи: "product", "service", "vacancy", "resume"
     * @return редирект на страницу заблокированных записей или страницу ошибки
     */
    private String redactorblockedtoggleBlock(Long id, Authentication authentication, Model model, String type) {
        try {
            // Получаем текущего пользователя и проверяем его роль
            User currentUser = (User) authentication.getPrincipal();

            // Разрешаем доступ только пользователям с ролью SUPER или ADMIN
            if (!currentUser.getRole().equals("ROLE_SUPER") && !currentUser.getRole().equals("ROLE_ADMIN")) {
                log.warn("Отказ в доступе: попытка блокировки записи типа {} с id {} пользователем с недостаточными правами (роль: {})",
                        type, id, currentUser.getRole());
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            log.debug("Пользователь авторизован для блокировки записи типа {} с id {}", type, id);

            // Обрабатываем в зависимости от типа записи
            if ("product".equals(type)) {
                handleProductBlock(id);
            } else if ("service".equals(type)) {
                handleServiceBlock(id);
            } else if ("vacancy".equals(type)) {
                handleVacancyBlock(id);
            } else if ("resume".equals(type)) {
                handleResumeBlock(id);
            } else {
                // Неизвестный тип (на случай ошибки в коде)
                log.error("Неизвестный тип записи при блокировке: {} (id: {})", type, id);
                model.addAttribute("error", "Неизвестный тип записи");
                return "error";
            }

            log.info("Успешно изменен статус блокировки для записи типа {} с id {}", type, id);
            return "redirect:/page_two/redactorblocked";

        } catch (Exception e) {
            log.error("Ошибка при изменении статуса блокировки для типа {} с id {}: {}", type, id, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при блокировке: " + e.getMessage());
            return "error";
        }
    }

    // ------------------------------------------------------------------------
    // Приватные методы для каждого типа записи (логика вынесена для читаемости)
    // ------------------------------------------------------------------------

    /** Обработка блокировки/разблокировки товара */
    private void handleProductBlock(Long id) {
        Optional<PostText> optionalPost = postRepo.findById(id);
        if (optionalPost.isEmpty()) {
            throw new RuntimeException("Запись не найдена");
        }
        PostText post = optionalPost.get();
        post.setBlocked(!post.isBlocked());
        if (post.isBlocked()) {
            post.setBlockedAt(LocalDateTime.now());
            log.debug("Товар с id {} заблокирован", id);
        } else {
            post.setBlockedAt(null);
            log.debug("Товар с id {} разблокирован", id);
        }
        postRepo.save(post);
        blockedPostsCacheService.clearBlockedProductsCache();
    }

    /** Обработка блокировки/разблокировки услуги */
    private void handleServiceBlock(Long id) {
        Optional<PostTextJob> optionalPost = postRepoJob.findById(id);
        if (optionalPost.isEmpty()) {
            throw new RuntimeException("Запись не найдена");
        }
        PostTextJob post = optionalPost.get();
        post.setBlocked(!post.isBlocked());
        if (post.isBlocked()) {
            post.setBlockedAt(LocalDateTime.now());
            log.debug("Услуга с id {} заблокирована", id);
        } else {
            post.setBlockedAt(null);
            log.debug("Услуга с id {} разблокирована", id);
        }
        postRepoJob.save(post);
        blockedPostsCacheService.clearBlockedServicesCache();
    }

    /** Обработка блокировки/разблокировки вакансии */
    private void handleVacancyBlock(Long id) {
        Optional<Vacancy> optionalVacancy = postVacancyRepository.findById(id);
        if (optionalVacancy.isEmpty()) {
            throw new RuntimeException("Запись не найдена");
        }
        Vacancy vacancy = optionalVacancy.get();
        vacancy.setBlocked(!vacancy.isBlocked());
        if (vacancy.isBlocked()) {
            vacancy.setBlockedAt(LocalDateTime.now());
            log.debug("Вакансия с id {} заблокирована", id);
        } else {
            vacancy.setBlockedAt(null);
            log.debug("Вакансия с id {} разблокирована", id);
        }
        postVacancyRepository.save(vacancy);
        blockedPostsCacheService.clearBlockedVacanciesCache();
    }

    /** Обработка блокировки/разблокировки резюме */
    private void handleResumeBlock(Long id) {
        Optional<Resume> optionalResume = postResumeRepository.findById(id);
        if (optionalResume.isEmpty()) {
            throw new RuntimeException("Запись не найдена");
        }
        Resume resume = optionalResume.get();
        resume.setBlocked(!resume.isBlocked());
        if (resume.isBlocked()) {
            resume.setBlockedAt(LocalDateTime.now());
            log.debug("Резюме с id {} заблокировано", id);
        } else {
            resume.setBlockedAt(null);
            log.debug("Резюме с id {} разблокировано", id);
        }
        postResumeRepository.save(resume);
        blockedPostsCacheService.clearBlockedResumeCache();
    }
}