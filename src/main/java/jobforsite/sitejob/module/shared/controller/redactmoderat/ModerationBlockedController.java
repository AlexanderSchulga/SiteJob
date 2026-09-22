package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Контроллер для управления блокировкой/разблокировкой сущностей
 * (товары, услуги, вакансии, резюме) в процессе модерации.
 * <p>
 * Доступен только для пользователей с ролями ROLE_SUPER и ROLE_ADMIN.
 * </p>
 */
@RequiredArgsConstructor
@Controller
public class ModerationBlockedController {

    private static final Logger log = LoggerFactory.getLogger(ModerationBlockedController.class);

    // Репозитории для работы с сущностями
    private final PostRepo postRepo;                       // Товары
    private final PostRepoJob postRepoJob;                 // Услуги
    private final PostVacancyRepository postVacancyRepository; // Вакансии
    private final PostResumeRepository postResumeRepository;    // Резюме

    // Сервисы для инвалидации кэша
    private final BlockedPostsCacheService blockedPostsCacheService;
    private final ModerationPostsCacheService moderationPostsCacheService;

    /**
     * Блокировка/разблокировка товара.
     *
     * @param id             идентификатор товара
     * @param authentication данные аутентификации текущего пользователя
     * @param model          модель для передачи ошибок на view
     * @return перенаправление на страницу модерации или страницу ошибки
     */
    @PostMapping("/page_two/moderat/{id}/toggle-block")
    public String toggleBlockModerPost(@PathVariable(value = "id") Long id,
                                       Authentication authentication,
                                       Model model) {
        return toggleBlockModer(id, authentication, model, "product");
    }

    /**
     * Блокировка/разблокировка услуги.
     *
     * @param id             идентификатор услуги
     * @param authentication данные аутентификации текущего пользователя
     * @param model          модель для передачи ошибок на view
     * @return перенаправление на страницу модерации или страницу ошибки
     */
    @PostMapping("/page_two/moderat/job/{id}/toggle-block")
    public String toggleBlockModerJob(@PathVariable(value = "id") Long id,
                                      Authentication authentication,
                                      Model model) {
        return toggleBlockModer(id, authentication, model, "service");
    }

    /**
     * Блокировка/разблокировка вакансии.
     *
     * @param id             идентификатор вакансии
     * @param authentication данные аутентификации текущего пользователя
     * @param model          модель для передачи ошибок на view
     * @return перенаправление на страницу модерации или страницу ошибки
     */
    @PostMapping("/page_two/moderat/vac/{id}/toggle-block")
    public String toggleBlockModerVacancy(@PathVariable(value = "id") Long id,
                                          Authentication authentication,
                                          Model model) {
        return toggleBlockModer(id, authentication, model, "vacancy");
    }

    /**
     * Блокировка/разблокировка резюме.
     *
     * @param id             идентификатор резюме
     * @param authentication данные аутентификации текущего пользователя
     * @param model          модель для передачи ошибок на view
     * @return перенаправление на страницу модерации или страницу ошибки
     */
    @PostMapping("/page_two/moderat/res/{id}/toggle-block")
    public String toggleBlockModerResume(@PathVariable(value = "id") Long id,
                                         Authentication authentication,
                                         Model model) {
        return toggleBlockModer(id, authentication, model, "resume");
    }

    /**
     * Общий метод для блокировки/разблокировки сущности в модерации.
     * <p>
     * Выполняет проверку прав доступа, изменяет статусы blocked, moderation,
     * устанавливает даты blockedAt/moderatedAt, сохраняет сущность и инвалидирует кэши.
     * </p>
     *
     * @param id             идентификатор сущности
     * @param authentication данные аутентификации текущего пользователя
     * @param model          модель для передачи ошибок на view
     * @param type           тип сущности: "product", "service", "vacancy", "resume"
     * @return перенаправление на страницу модерации или страницу ошибки
     */
    private String toggleBlockModer(Long id, Authentication authentication, Model model, String type) {
        log.info("Попытка переключения статуса блокировки для сущности типа {} с id={}", type, id);

        try {
            User currentUser = (User) authentication.getPrincipal();
            // Разрешаем доступ только SUPER и ADMIN
            if (!currentUser.getRole().equals("ROLE_SUPER") && !currentUser.getRole().equals("ROLE_ADMIN")) {
                log.warn("Отказ в доступе: недостаточно прав для типа {} с id={}", type, id);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findById(id);
                if (optionalPost.isEmpty()) {
                    log.warn("Товар с id={} не найден", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostText post = optionalPost.get();
                boolean wasBlocked = post.isBlocked();
                post.setBlocked(!wasBlocked);

                if (post.isBlocked()) {
                    post.setModeration(false);
                    post.setModeratedAt(null);
                    post.setBlockedAt(LocalDateTime.now());
                    post.setPremium(false);
                    post.setPremiumPriority(0);
                    log.info("Товар с id={} заблокирован. Модерация сброшена.", id);
                } else {
                    post.setBlockedAt(null);
                    log.info("Товар с id={} разблокирован.", id);
                }
                postRepo.save(post);
                blockedPostsCacheService.clearBlockedProductsCache();
                moderationPostsCacheService.clearModerationProductsCache();

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findById(id);
                if (optionalPost.isEmpty()) {
                    log.warn("Услуга с id={} не найдена", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostTextJob post = optionalPost.get();
                boolean wasBlocked = post.isBlocked();
                post.setBlocked(!wasBlocked);

                if (post.isBlocked()) {
                    post.setModeration(false);
                    post.setModeratedAt(null);
                    post.setBlockedAt(LocalDateTime.now());
                    post.setPremium(false);
                    post.setPremiumPriority(0);
                    log.info("Услуга с id={} заблокирована. Модерация сброшена.", id);
                } else {
                    post.setBlockedAt(null);
                    log.info("Услуга с id={} разблокирована.", id);
                }
                postRepoJob.save(post);
                blockedPostsCacheService.clearBlockedServicesCache();
                moderationPostsCacheService.clearModerationServicesCache();

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findById(id);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Вакансия с id={} не найдена", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Vacancy vacancy = optionalVacancy.get();
                boolean wasBlocked = vacancy.isBlocked();
                vacancy.setBlocked(!wasBlocked);

                if (vacancy.isBlocked()) {
                    vacancy.setModeration(false);
                    vacancy.setModeratedAt(null);
                    vacancy.setBlockedAt(LocalDateTime.now());
                    vacancy.setPremium(false);
                    vacancy.setPremiumPriority(0);
                    log.info("Вакансия с id={} заблокирована. Модерация сброшена.", id);
                } else {
                    vacancy.setBlockedAt(null);
                    log.info("Вакансия с id={} разблокирована.", id);
                }
                postVacancyRepository.save(vacancy);
                blockedPostsCacheService.clearBlockedVacanciesCache();
                moderationPostsCacheService.clearModerationVacanciesCache();

            } else if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findById(id);
                if (optionalResume.isEmpty()) {
                    log.warn("Резюме с id={} не найдено", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Resume resume = optionalResume.get();
                boolean wasBlocked = resume.isBlocked();
                resume.setBlocked(!wasBlocked);

                if (resume.isBlocked()) {
                    resume.setModeration(false);
                    resume.setModeratedAt(null);
                    resume.setBlockedAt(LocalDateTime.now());
                    log.info("Резюме с id={} заблокировано. Модерация сброшена.", id);
                } else {
                    resume.setBlockedAt(null);
                    log.info("Резюме с id={} разблокировано.", id);
                }
                postResumeRepository.save(resume);
                blockedPostsCacheService.clearBlockedResumeCache();
                moderationPostsCacheService.clearModerationResumeCache();
            }

            log.info("Успешное переключение статуса блокировки для типа {} с id={}", type, id);
            return "redirect:/page_two/moderat";

        } catch (Exception e) {
            log.warn("Ошибка при переключении статуса блокировки для типа {}, id={}", type, id);
            model.addAttribute("error", "Ошибка при блокировке");
            return "error";
        }
    }
}