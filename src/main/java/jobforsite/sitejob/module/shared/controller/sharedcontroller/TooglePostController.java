package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Контроллер для управления блокировкой и удалением различных типов постов (товары, услуги, вакансии, резюме).
 * Доступ к методам контроллера имеют только пользователи с правами супер-администратора.
 *
 * @see BaseController
 * @see PostText
 * @see PostTextJob
 * @see Vacancy
 * @see Resume
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class TooglePostController extends BaseController {

    // Репозитории для работы с различными типами сущностей
    private final PostRepo postRepo;                       // Товары
    private final PostRepoJob postRepoJob;                 // Услуги
    private final PostVacancyRepository postVacancyRepository; // Вакансии
    private final PostResumeRepository postResumeRepository;    // Резюме

    private final FavorRepo favorRepo;                     // Репозиторий избранного
    private final PostCacheService postCacheService;       // Сервис кэширования постов
    private final BlockedPostsCacheService blockedPostsCacheService; // Сервис кэширования заблокированных постов
    private final ImageService imageService;               // Сервис для работы с изображениями

    /**
     * Переключение статуса блокировки товара.
     * - При блокировке: установка флага blocked = true, времени блокировки и удаление из избранного у всех пользователей.
     * - При разблокировке: сброс флага blocked и времени блокировки.
     *
     * @param hashId         хеш-идентификатор товара
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка товаров
     */
    @PostMapping("/viewsone/post/{hashId}/toggle-block")
    public String toggleBlockPost(@PathVariable String hashId,
                                  Model model,
                                  Authentication authentication) {
        log.debug("Запрос на переключение блокировки товара с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка переключения блокировки товара без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsone";
            }

            Optional<PostText> postOptional = postRepo.findByHashId(hashId);
            if (postOptional.isPresent()) {
                PostText post = postOptional.get();
                boolean isCurrentlyBlocked = post.isBlocked();

                if (isCurrentlyBlocked) {
                    // Разблокировка: снимаем блокировку
                    post.setBlocked(false);
                    post.setBlockedAt(null);
                    log.debug("Товар с id {} разблокирован", post.getId());
                } else {
                    // Блокировка: устанавливаем блокировку и удаляем из избранного у всех пользователей
                    post.setBlocked(true);
                    post.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByPostId(post.getId());
                    post.setPremium(false);
                    post.setPremiumPriority(0);
                    post.setDonate(false);
                    log.debug("Товар с id {} заблокирован, избранное очищено", post.getId());
                }
                postRepo.save(post);
            } else {
                log.warn("Товар с hashId {} не найден", hashId);
            }

            // Очищаем кэш
            postCacheService.clearProductsCache();
            blockedPostsCacheService.clearBlockedProductsCache();
            log.debug("Кэш товаров очищен");

            return "redirect:/page_one/viewsone";
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса товара с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при изменении статуса товара: " + e.getMessage());
            return "redirect:/page_one/viewsone";
        }
    }

    /**
     * Полное удаление товара.
     * Удаляет изображения, записи из избранного и сам товар.
     *
     * @param hashId         хеш-идентификатор товара
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка товаров
     */
    @PostMapping("/viewsone/post/{hashId}/remove")
    @Transactional
    public String removePost(@PathVariable String hashId,
                             Model model,
                             Authentication authentication) {
        log.debug("Запрос на удаление товара с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка удаления товара без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsone";
            }

            Optional<PostText> postOptional = postRepo.findByHashId(hashId);
            if (postOptional.isPresent()) {
                PostText post = postOptional.get();

                // Удаляем изображения поста
                imageService.deletePostImages(post);
                log.debug("Изображения товара с id {} удалены", post.getId());

                // Удаляем из избранного
                favorRepo.deleteByPostId(post.getId());
                log.debug("Записи избранного для товара с id {} удалены", post.getId());

                // Удаляем пост
                postRepo.delete(post);
                log.debug("Товар с id {} удален", post.getId());
            } else {
                log.warn("Товар с hashId {} не найден для удаления", hashId);
            }

            // Очищаем кэш
            postCacheService.clearProductsCache();
            blockedPostsCacheService.clearBlockedProductsCache();
            log.debug("Кэш товаров очищен");

            return "redirect:/page_one/viewsone";
        } catch (Exception e) {
            log.error("Ошибка при удалении товара с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при удалении товара: " + e.getMessage());
            return "redirect:/page_one/viewsone";
        }
    }

    /**
     * Переключение статуса блокировки услуги.
     *
     * @param hashId         хеш-идентификатор услуги
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка услуг
     */
    @PostMapping("/viewsonejob/job/{hashId}/toggle-block")
    public String toggleBlockJob(@PathVariable String hashId,
                                 Model model,
                                 Authentication authentication) {
        log.debug("Запрос на переключение блокировки услуги с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка переключения блокировки услуги без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsonejob";
            }

            Optional<PostTextJob> jobOptional = postRepoJob.findByHashId(hashId);
            if (jobOptional.isPresent()) {
                PostTextJob job = jobOptional.get();
                boolean isCurrentlyBlocked = job.isBlocked();

                if (isCurrentlyBlocked) {
                    job.setBlocked(false);
                    job.setBlockedAt(null);
                    log.debug("Услуга с id {} разблокирована", job.getId());
                } else {
                    job.setBlocked(true);
                    job.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByPostJobId(job.getId());
                    job.setPremium(false);
                    job.setPremiumPriority(0);
                    job.setDonate(false);
                    log.debug("Услуга с id {} заблокирована, избранное очищено", job.getId());
                }
                postRepoJob.save(job);
            } else {
                log.warn("Услуга с hashId {} не найдена", hashId);
            }

            postCacheService.clearServicesCache();
            blockedPostsCacheService.clearBlockedServicesCache();
            log.debug("Кэш услуг очищен");

            return "redirect:/page_one/viewsonejob";
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса услуги с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при изменении статуса услуги: " + e.getMessage());
            return "redirect:/page_one/viewsonejob";
        }
    }

    /**
     * Полное удаление услуги.
     *
     * @param hashId         хеш-идентификатор услуги
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка услуг
     */
    @PostMapping("/viewsonejob/job/{hashId}/remove")
    @Transactional
    public String removePostJob(@PathVariable String hashId,
                                Model model,
                                Authentication authentication) {
        log.debug("Запрос на удаление услуги с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка удаления услуги без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsonejob";
            }

            Optional<PostTextJob> jobOptional = postRepoJob.findByHashId(hashId);
            if (jobOptional.isPresent()) {
                PostTextJob job = jobOptional.get();

                imageService.deletePostJobImages(job);
                log.debug("Изображения услуги с id {} удалены", job.getId());

                favorRepo.deleteByPostJobId(job.getId());
                log.debug("Записи избранного для услуги с id {} удалены", job.getId());

                postRepoJob.delete(job);
                log.debug("Услуга с id {} удалена", job.getId());
            } else {
                log.warn("Услуга с hashId {} не найдена для удаления", hashId);
            }

            postCacheService.clearServicesCache();
            blockedPostsCacheService.clearBlockedServicesCache();
            log.debug("Кэш услуг очищен");

            return "redirect:/page_one/viewsonejob";
        } catch (Exception e) {
            log.error("Ошибка при удалении услуги с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при удалении услуги: " + e.getMessage());
            return "redirect:/page_one/viewsonejob";
        }
    }

    /**
     * Переключение статуса блокировки вакансии.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка вакансий
     */
    @PostMapping("/viewsonevac/vacancy/{hashId}/toggle-block")
    public String toggleBlockVacancy(@PathVariable String hashId,
                                     Model model,
                                     Authentication authentication) {
        log.debug("Запрос на переключение блокировки вакансии с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка переключения блокировки вакансии без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsonevac";
            }

            Optional<Vacancy> vacancyOptional = postVacancyRepository.findByHashId(hashId);
            if (vacancyOptional.isPresent()) {
                Vacancy vacancy = vacancyOptional.get();
                boolean isCurrentlyBlocked = vacancy.isBlocked();

                if (isCurrentlyBlocked) {
                    vacancy.setBlocked(false);
                    vacancy.setBlockedAt(null);
                    log.debug("Вакансия с id {} разблокирована", vacancy.getId());
                } else {
                    vacancy.setBlocked(true);
                    vacancy.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByVacancyId(vacancy.getId());
                    vacancy.setPremium(false);
                    vacancy.setPremiumPriority(0);
                    vacancy.setDonate(false);
                    log.debug("Вакансия с id {} заблокирована, избранное очищено", vacancy.getId());
                }
                postVacancyRepository.save(vacancy);
            } else {
                log.warn("Вакансия с hashId {} не найдена", hashId);
            }

            postCacheService.clearVacanciesCache();
            blockedPostsCacheService.clearBlockedVacanciesCache();
            log.debug("Кэш вакансий очищен");

            return "redirect:/page_one/viewsonevac";
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса вакансии с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при изменении статуса вакансии: " + e.getMessage());
            return "redirect:/page_one/viewsonevac";
        }
    }

    /**
     * Полное удаление вакансии.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка вакансий
     */
    @PostMapping("/viewsonevac/vacancy/{hashId}/remove")
    @Transactional
    public String removePostVac(@PathVariable String hashId,
                                Model model,
                                Authentication authentication) {
        log.debug("Запрос на удаление вакансии с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка удаления вакансии без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsonevac";
            }

            Optional<Vacancy> vacancyOptional = postVacancyRepository.findByHashId(hashId);
            if (vacancyOptional.isPresent()) {
                Vacancy vacancy = vacancyOptional.get();

                imageService.deleteVacancyImages(vacancy);
                log.debug("Изображения вакансии с id {} удалены", vacancy.getId());

                favorRepo.deleteByVacancyId(vacancy.getId());
                log.debug("Записи избранного для вакансии с id {} удалены", vacancy.getId());

                postVacancyRepository.delete(vacancy);
                log.debug("Вакансия с id {} удалена", vacancy.getId());
            } else {
                log.warn("Вакансия с hashId {} не найдена для удаления", hashId);
            }

            postCacheService.clearVacanciesCache();
            blockedPostsCacheService.clearBlockedVacanciesCache();
            log.debug("Кэш вакансий очищен");

            return "redirect:/page_one/viewsonevac";
        } catch (Exception e) {
            log.error("Ошибка при удалении вакансии с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при удалении вакансии: " + e.getMessage());
            return "redirect:/page_one/viewsonevac";
        }
    }

    /**
     * Переключение статуса блокировки резюме.
     *
     * @param hashId         хеш-идентификатор резюме
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка резюме
     */
    @PostMapping("/viewsoneres/resume/{hashId}/toggle-block")
    public String toggleBlockResume(@PathVariable String hashId,
                                    Model model,
                                    Authentication authentication) {
        log.debug("Запрос на переключение блокировки резюме с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка переключения блокировки резюме без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsoneres";
            }

            Optional<Resume> resumeOptional = postResumeRepository.findByHashId(hashId);
            if (resumeOptional.isPresent()) {
                Resume res = resumeOptional.get();
                boolean isCurrentlyBlocked = res.isBlocked();

                if (isCurrentlyBlocked) {
                    res.setBlocked(false);
                    res.setBlockedAt(null);
                    log.debug("Резюме с id {} разблокировано", res.getId());
                } else {
                    res.setBlocked(true);
                    res.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByResumeId(res.getId());
                    log.debug("Резюме с id {} заблокировано, избранное очищено", res.getId());
                }
                postResumeRepository.save(res);
            } else {
                log.warn("Резюме с hashId {} не найдено", hashId);
            }

            postCacheService.clearResumeCache();
            blockedPostsCacheService.clearBlockedResumeCache();
            log.debug("Кэш резюме очищен");

            return "redirect:/page_one/viewsoneres";
        } catch (Exception e) {
            log.error("Ошибка при изменении статуса резюме с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при изменении статуса резюме: " + e.getMessage());
            return "redirect:/page_one/viewsoneres";
        }
    }

    /**
     * Полное удаление резюме.
     *
     * @param hashId         хеш-идентификатор резюме
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации для проверки прав
     * @return перенаправление на страницу списка резюме
     */
    @PostMapping("/viewsoneres/resume/{hashId}/remove")
    @Transactional
    public String removePostRes(@PathVariable String hashId,
                                Model model,
                                Authentication authentication) {
        log.debug("Запрос на удаление резюме с hashId: {}", hashId);
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка удаления резюме без прав супер-администратора");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/page_one/viewsoneres";
            }

            Optional<Resume> resumeOptional = postResumeRepository.findByHashId(hashId);
            if (resumeOptional.isPresent()) {
                Resume res = resumeOptional.get();

                imageService.deleteResumeImages(res);
                log.debug("Изображения резюме с id {} удалены", res.getId());

                favorRepo.deleteByResumeId(res.getId());
                log.debug("Записи избранного для резюме с id {} удалены", res.getId());

                postResumeRepository.delete(res);
                log.debug("Резюме с id {} удалено", res.getId());
            } else {
                log.warn("Резюме с hashId {} не найдено для удаления", hashId);
            }

            postCacheService.clearResumeCache();
            blockedPostsCacheService.clearBlockedResumeCache();
            log.debug("Кэш резюме очищен");

            return "redirect:/page_one/viewsoneres";
        } catch (Exception e) {
            log.error("Ошибка при удалении резюме с hashId {}: {}", hashId, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при удалении резюме: " + e.getMessage());
            return "redirect:/page_one/viewsoneres";
        }
    }
}