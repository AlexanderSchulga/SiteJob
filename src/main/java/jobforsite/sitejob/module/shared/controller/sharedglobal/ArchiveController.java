package jobforsite.sitejob.module.shared.controller.sharedglobal;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Контроллер для управления архивацией и восстановлением различных типов постов.
 * <p>
 * Обеспечивает функциональность:
 * <ul>
 *     <li>Восстановление постов из архива с отправкой на модерацию</li>
 *     <li>Ручное архивирование (скрытие) постов</li>
 * </ul>
 * Доступ к операциям имеют: SUPER, ADMIN и владельцы постов.
 *
 * @see PostText
 * @see PostTextJob
 * @see Vacancy
 * @see Resume
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class ArchiveController {

    // Репозитории для работы с различными типами сущностей
    private final PostRepo postRepo;                       // Товары
    private final PostRepoJob postRepoJob;                 // Услуги
    private final PostVacancyRepository postVacancyRepository; // Вакансии
    private final PostResumeRepository postResumeRepository;    // Резюме

    private final ModerationPostsCacheService moderationPostsCacheService; // Сервис кэширования для модерации

    /**
     * Восстановление архивного товара.
     * После восстановления пост отправляется на модерацию (moderation = true).
     *
     * @param hashId         хеш-идентификатор товара
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/archive/{hashId}/renew")
    public String renewArchivedPost(@PathVariable(value = "hashId") String hashId,
                                    Authentication authentication,
                                    Model model) {
        log.debug("Запрос на восстановление архивного товара с hashId: {}", hashId);
        return renewArchive(hashId, authentication, model, "product");
    }

    /**
     * Восстановление архивной услуги.
     *
     * @param hashId         хеш-идентификатор услуги
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/job/archive/{hashId}/renew")
    public String renewArchivedJob(@PathVariable(value = "hashId") String hashId,
                                   Authentication authentication,
                                   Model model) {
        log.debug("Запрос на восстановление архивной услуги с hashId: {}", hashId);
        return renewArchive(hashId, authentication, model, "service");
    }

    /**
     * Восстановление архивной вакансии.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/vac/archive/{hashId}/renew")
    public String renewArchivedVacancy(@PathVariable(value = "hashId") String hashId,
                                       Authentication authentication,
                                       Model model) {
        log.debug("Запрос на восстановление архивной вакансии с hashId: {}", hashId);
        return renewArchive(hashId, authentication, model, "vacancy");
    }

    /**
     * Восстановление архивного резюме.
     *
     * @param hashId         хеш-идентификатор резюме
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/res/archive/{hashId}/renew")
    public String renewArchivedResume(@PathVariable(value = "hashId") String hashId,
                                      Authentication authentication,
                                      Model model) {
        log.debug("Запрос на восстановление архивного резюме с hashId: {}", hashId);
        return renewArchive(hashId, authentication, model, "resume");
    }

    /**
     * Общий метод для восстановления архивных постов разных типов.
     * <p>
     * Выполняет:
     * <ul>
     *     <li>Проверку прав доступа (SUPER, ADMIN или владелец)</li>
     *     <li>Проверку, что пост действительно находится в архиве</li>
     *     <li>Установку статусов (moderation=true, archived=false)</li>
     *     <li>Очистку кэшей модерации</li>
     * </ul>
     *
     * @param hashId         хеш-идентификатор поста
     * @param authentication объект аутентификации
     * @param model          модель для сообщений об ошибках
     * @param type           тип сущности: "product", "service", "vacancy", "resume"
     * @return путь для редиректа или имя страницы ошибки
     */
    private String renewArchive(String hashId, Authentication authentication, Model model, String type) {
        log.debug("Восстановление из архива: type={}, hashId={}", type, hashId);
        try {
            User currentUser = (User) authentication.getPrincipal();

            if (type.equals("product")) {
                Optional<PostText> optionalPost = postRepo.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Товар с hashId {} не найден при попытке восстановления", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostText post = optionalPost.get();

                // Проверка прав доступа
                boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                        currentUser.getRole().equals("ROLE_ADMIN") ||
                        post.getUser().getId().equals(currentUser.getId());
                if (!hasAccess) {
                    log.warn("Отказ в доступе: попытка восстановления товара с hashId {} пользователем без прав", hashId);
                    model.addAttribute("error", "Доступ запрещен");
                    return "error";
                }
                if (!post.isArchived()) {
                    log.warn("Товар с hashId {} не находится в архиве, восстановление невозможно", hashId);
                    model.addAttribute("error", "Пост не находится в архиве");
                    return "error";
                }
                // Отправляем на модерацию и выводим из архива
                post.setModeration(true);
                post.setArchived(false);
                postRepo.save(post);
                log.debug("Товар с hashId {} успешно восстановлен из архива", hashId);

            } else if (type.equals("service")) {
                Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Услуга с hashId {} не найдена при попытке восстановления", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostTextJob post = optionalPost.get();

                boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                        currentUser.getRole().equals("ROLE_ADMIN") ||
                        post.getUser().getId().equals(currentUser.getId());
                if (!hasAccess) {
                    log.warn("Отказ в доступе: попытка восстановления услуги с hashId {} пользователем без прав", hashId);
                    model.addAttribute("error", "Доступ запрещен");
                    return "error";
                }
                if (!post.isArchived()) {
                    log.warn("Услуга с hashId {} не находится в архиве, восстановление невозможно", hashId);
                    model.addAttribute("error", "Услуга не находится в архиве");
                    return "error";
                }
                post.setModeration(true);
                post.setArchived(false);
                postRepoJob.save(post);
                log.debug("Услуга с hashId {} успешно восстановлена из архива", hashId);

            } else if (type.equals("vacancy")) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findByHashId(hashId);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Вакансия с hashId {} не найдена при попытке восстановления", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Vacancy vacancy = optionalVacancy.get();

                boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                        currentUser.getRole().equals("ROLE_ADMIN") ||
                        vacancy.getUser().getId().equals(currentUser.getId());
                if (!hasAccess) {
                    log.warn("Отказ в доступе: попытка восстановления вакансии с hashId {} пользователем без прав", hashId);
                    model.addAttribute("error", "Доступ запрещен");
                    return "error";
                }
                if (!vacancy.isArchived()) {
                    log.warn("Вакансия с hashId {} не находится в архиве, восстановление невозможно", hashId);
                    model.addAttribute("error", "Вакансия не находится в архиве");
                    return "error";
                }
                vacancy.setModeration(true);
                vacancy.setArchived(false);
                postVacancyRepository.save(vacancy);
                log.debug("Вакансия с hashId {} успешно восстановлена из архива", hashId);

            } else if (type.equals("resume")) {
                Optional<Resume> optionaResume = postResumeRepository.findByHashId(hashId);
                if (optionaResume.isEmpty()) {
                    log.warn("Резюме с hashId {} не найдено при попытке восстановления", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Resume res = optionaResume.get();

                boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                        currentUser.getRole().equals("ROLE_ADMIN") ||
                        res.getUser().getId().equals(currentUser.getId());
                if (!hasAccess) {
                    log.warn("Отказ в доступе: попытка восстановления резюме с hashId {} пользователем без прав", hashId);
                    model.addAttribute("error", "Доступ запрещен");
                    return "error";
                }
                if (!res.isArchived()) {
                    log.warn("Резюме с hashId {} не находится в архиве, восстановление невозможно", hashId);
                    model.addAttribute("error", "Резюме не находится в архиве");
                    return "error";
                }
                res.setModeration(true);
                res.setArchived(false);
                postResumeRepository.save(res);
                log.debug("Резюме с hashId {} успешно восстановлено из архива", hashId);
            }

            // Очищаем все кэши модерации
            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Кэши модерации очищены после восстановления");

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при восстановлении из архива: type={}, hashId={}, error={}", type, hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Ручное архивирование товара (скрытие).
     * Устанавливает статус archived=true и заполняет время архивации.
     *
     * @param hashId         хеш-идентификатор товара
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/{hashId}/manual-archive")
    public String manualArchivePost(@PathVariable(value = "hashId") String hashId,
                                    Authentication authentication,
                                    Model model) {
        log.debug("Запрос на ручное архивирование товара с hashId: {}", hashId);
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<PostText> optionalPost = postRepo.findByHashId(hashId);

            if (optionalPost.isEmpty()) {
                log.warn("Товар с hashId {} не найден при попытке архивации", hashId);
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            PostText post = optionalPost.get();

            // Проверка прав доступа
            boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                    currentUser.getRole().equals("ROLE_ADMIN") ||
                    post.getUser().getId().equals(currentUser.getId());

            if (!hasAccess) {
                log.warn("Отказ в доступе: попытка архивации товара с hashId {} пользователем без прав", hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // Отправляем в архив
            post.setArchived(true);
            post.setArchivedAt(LocalDateTime.now());
            post.setPremium(false);
            post.setPremiumPriority(0);
            post.setDonate(false);
            postRepo.save(post);
            log.debug("Товар с hashId {} успешно отправлен в архив", hashId);

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при архивации товара с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при скрытии: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Ручное архивирование услуги.
     *
     * @param hashId         хеш-идентификатор услуги
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/job/{hashId}/manual-archive")
    public String manualArchiveJob(@PathVariable(value = "hashId") String hashId,
                                   Authentication authentication,
                                   Model model) {
        log.debug("Запрос на ручное архивирование услуги с hashId: {}", hashId);
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);

            if (optionalPost.isEmpty()) {
                log.warn("Услуга с hashId {} не найдена при попытке архивации", hashId);
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            PostTextJob post = optionalPost.get();

            // Проверка прав доступа
            boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                    currentUser.getRole().equals("ROLE_ADMIN") ||
                    post.getUser().getId().equals(currentUser.getId());

            if (!hasAccess) {
                log.warn("Отказ в доступе: попытка архивации услуги с hashId {} пользователем без прав", hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // Отправляем в архив
            post.setArchived(true);
            post.setArchivedAt(LocalDateTime.now());
            post.setPremium(false);
            post.setPremiumPriority(0);
            post.setDonate(false);
            postRepoJob.save(post);
            log.debug("Услуга с hashId {} успешно отправлена в архив", hashId);

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при архивации услуги с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при скрытии: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Ручное архивирование вакансии.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/vac/{hashId}/manual-archive")
    public String manualArchiveVacancy(@PathVariable(value = "hashId") String hashId,
                                       Authentication authentication,
                                       Model model) {
        log.debug("Запрос на ручное архивирование вакансии с hashId: {}", hashId);
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<Vacancy> optionalVacancy = postVacancyRepository.findByHashId(hashId);

            if (optionalVacancy.isEmpty()) {
                log.warn("Вакансия с hashId {} не найдена при попытке архивации", hashId);
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            Vacancy vacancy = optionalVacancy.get();

            // Проверка прав доступа
            boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                    currentUser.getRole().equals("ROLE_ADMIN") ||
                    vacancy.getUser().getId().equals(currentUser.getId());

            if (!hasAccess) {
                log.warn("Отказ в доступе: попытка архивации вакансии с hashId {} пользователем без прав", hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // Отправляем в архив
            vacancy.setArchived(true);
            vacancy.setArchivedAt(LocalDateTime.now());
            vacancy.setPremium(false);
            vacancy.setPremiumPriority(0);
            vacancy.setDonate(false);
            postVacancyRepository.save(vacancy);
            log.debug("Вакансия с hashId {} успешно отправлена в архив", hashId);

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при архивации вакансии с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при скрытии: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Ручное архивирование резюме.
     *
     * @param hashId         хеш-идентификатор резюме
     * @param authentication объект аутентификации для проверки прав
     * @param model          модель для передачи данных в представление
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/res/{hashId}/manual-archive")
    public String manualArchiveResume(@PathVariable(value = "hashId") String hashId,
                                      Authentication authentication,
                                      Model model) {
        log.debug("Запрос на ручное архивирование резюме с hashId: {}", hashId);
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<Resume> optionalResume = postResumeRepository.findByHashId(hashId);

            if (optionalResume.isEmpty()) {
                log.warn("Резюме с hashId {} не найдено при попытке архивации", hashId);
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            Resume res = optionalResume.get();

            // Проверка прав доступа
            boolean hasAccess = currentUser.getRole().equals("ROLE_SUPER") ||
                    currentUser.getRole().equals("ROLE_ADMIN") ||
                    res.getUser().getId().equals(currentUser.getId());

            if (!hasAccess) {
                log.warn("Отказ в доступе: попытка архивации резюме с hashId {} пользователем без прав", hashId);
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // Отправляем в архив
            res.setArchived(true);
            res.setArchivedAt(LocalDateTime.now());
            postResumeRepository.save(res);
            log.debug("Резюме с hashId {} успешно отправлено в архив", hashId);

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при архивации резюме с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при скрытии: " + e.getMessage());
            return "error";
        }
    }
}