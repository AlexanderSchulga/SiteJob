package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
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

import java.util.Optional;

/**
 * Контроллер для удаления записей из модерации и из списка заблокированных.
 * Поддерживает типы: product (товар), service (услуга), vacancy (вакансия), resume (резюме).
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class DeleteForRedactBlockedAndModerController {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final BlockedPostsCacheService blockedPostsCacheService;
    private final ModerationPostsCacheService moderationPostsCacheService;
    private final ImageService imageService;

    // ==================== УДАЛЕНИЕ ИЗ МОДЕРАЦИИ ====================

    /** Удаление товара из модерации */
    @PostMapping("/page_two/moderat/{hashId}/remove")
    public String moderatPostRemove(@PathVariable(value = "hashId") Long id, Model model,
                                    Authentication authentication) {
        log.info("Запрос на удаление товара из модерации, ID: {}", id);
        return deletePostModer(id, model, authentication, "product");
    }

    /** Удаление услуги из модерации */
    @PostMapping("/page_two/moderat/job/{id}/remove")
    public String moderatJobPostRemove(@PathVariable(value = "id") Long id, Model model,
                                       Authentication authentication) {
        log.info("Запрос на удаление услуги из модерации, ID: {}", id);
        return deletePostModer(id, model, authentication, "service");
    }

    /** Удаление вакансии из модерации */
    @PostMapping("/page_two/moderat/vac/{id}/remove")
    public String moderatVacancyPostRemove(@PathVariable(value = "id") Long id, Model model,
                                           Authentication authentication) {
        log.info("Запрос на удаление вакансии из модерации, ID: {}", id);
        return deletePostModer(id, model, authentication, "vacancy");
    }

    /** Удаление резюме из модерации */
    @PostMapping("/page_two/moderat/res/{id}/remove")
    public String moderatResumePostRemove(@PathVariable(value = "id") Long id, Model model,
                                          Authentication authentication) {
        log.info("Запрос на удаление резюме из модерации, ID: {}", id);
        return deletePostModer(id, model, authentication, "resume");
    }

    /**
     * Общий метод для удаления постов из модерации.
     * Проверяет права: SUPER, ADMIN или владелец поста.
     * @param id ID записи
     * @param model модель для передачи ошибок
     * @param authentication данные текущего пользователя
     * @param type тип записи (product, service, vacancy, resume)
     * @return редирект или страница ошибки
     */
    private String deletePostModer(Long id, Model model, Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            log.debug("Попытка удаления из модерации типа: {}, ID: {}", type, id);

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findById(id);
                if (optionalPost.isEmpty()) {
                    log.warn("Товар не найден, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostText post = optionalPost.get();

                // Разрешаем доступ как SUPER, ADMIN, так и владельцу
                if (!currentUser.getRole().equals("ROLE_SUPER") &&
                        !currentUser.getRole().equals("ROLE_ADMIN") &&
                        !post.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Доступ запрещен при удалении товара из модерации: нет прав, ID: {}", id);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                imageService.deletePostImages(post);
                postRepo.delete(post);
                blockedPostsCacheService.clearBlockedProductsCache();
                moderationPostsCacheService.clearModerationProductsCache();
                log.info("Товар удален из модерации, ID: {}", id);

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findById(id);
                if (optionalPost.isEmpty()) {
                    log.warn("Услуга не найдена, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostTextJob post = optionalPost.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") &&
                        !currentUser.getRole().equals("ROLE_ADMIN") &&
                        !post.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Доступ запрещен при удалении услуги из модерации: нет прав, ID: {}", id);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                imageService.deletePostJobImages(post);
                postRepoJob.delete(post);
                blockedPostsCacheService.clearBlockedServicesCache();
                moderationPostsCacheService.clearModerationServicesCache();
                log.info("Услуга удалена из модерации, ID: {}", id);

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findById(id);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Вакансия не найдена, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Vacancy vacvac = optionalVacancy.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") &&
                        !currentUser.getRole().equals("ROLE_ADMIN") &&
                        !vacvac.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Доступ запрещен при удалении вакансии из модерации: нет прав, ID: {}", id);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                imageService.deleteVacancyImages(vacvac);
                postVacancyRepository.delete(vacvac);
                blockedPostsCacheService.clearBlockedVacanciesCache();
                moderationPostsCacheService.clearModerationVacanciesCache();
                log.info("Вакансия удалена из модерации, ID: {}", id);

            } else if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findById(id);
                if (optionalResume.isEmpty()) {
                    log.warn("Резюме не найдено, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Resume res = optionalResume.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") &&
                        !currentUser.getRole().equals("ROLE_ADMIN") &&
                        !res.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Доступ запрещен при удалении резюме из модерации: нет прав, ID: {}", id);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                imageService.deleteResumeImages(res);
                postResumeRepository.delete(res);
                blockedPostsCacheService.clearBlockedResumeCache();
                moderationPostsCacheService.clearModerationResumeCache();
                log.info("Резюме удалено из модерации, ID: {}", id);
            }

            return "redirect:/page_two/moderat";

        } catch (Exception e) {
            log.error("Ошибка при удалении из модерации типа: {}, ID: {}: {}", type, id, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при удалении: " + e.getMessage());
            return "error";
        }
    }

    // ==================== УДАЛЕНИЕ ИЗ ЗАБЛОКИРОВАННЫХ ====================

    /** Удаление товара из заблокированных */
    @PostMapping("/page_two/redactorblocked/{id}/remove")
    public String redactorblockedPostRemove(@PathVariable(value = "id") Long id, Model model,
                                            Authentication authentication) {
        log.info("Запрос на удаление товара из заблокированных, ID: {}", id);
        return deletePostModerBlock(id, model, authentication, "product");
    }

    /** Удаление услуги из заблокированных */
    @PostMapping("/page_two/redactorblocked/job/{id}/remove")
    public String redactorblockedJobPostRemove(@PathVariable(value = "id") Long id, Model model,
                                               Authentication authentication) {
        log.info("Запрос на удаление услуги из заблокированных, ID: {}", id);
        return deletePostModerBlock(id, model, authentication, "service");
    }

    /** Удаление вакансии из заблокированных */
    @PostMapping("/page_two/redactorblocked/vac/{id}/remove")
    public String redactorblockedVacancyPostRemove(@PathVariable(value = "id") Long id, Model model,
                                                   Authentication authentication) {
        log.info("Запрос на удаление вакансии из заблокированных, ID: {}", id);
        return deletePostModerBlock(id, model, authentication, "vacancy");
    }

    /** Удаление резюме из заблокированных */
    @PostMapping("/page_two/redactorblocked/res/{id}/remove")
    public String redactorblockedResumePostRemove(@PathVariable(value = "id") Long id, Model model,
                                                  Authentication authentication) {
        log.info("Запрос на удаление резюме из заблокированных, ID: {}", id);
        return deletePostModerBlock(id, model, authentication, "resume");
    }

    /**
     * Общий метод для удаления постов из заблокированных.
     * Требует прав SUPER или ADMIN.
     * @param id ID записи
     * @param model модель для передачи ошибок
     * @param authentication данные текущего пользователя
     * @param type тип записи (product, service, vacancy, resume)
     * @return редирект или страница ошибки
     */
    private String deletePostModerBlock(Long id, Model model, Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            log.debug("Попытка удаления из заблокированных типа: {}, ID: {}", type, id);

            // Только SUPER и ADMIN могут удалять из заблокированных
            if (!currentUser.getRole().equals("ROLE_SUPER") &&
                    !currentUser.getRole().equals("ROLE_ADMIN")) {
                log.warn("Доступ запрещен при удалении из заблокированных: недостаточно прав, тип: {}, ID: {}", type, id);
                model.addAttribute("error", "Доступ запрещен: только администраторы могут удалять из заблокированных");
                return "error";
            }

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findById(id);
                if (optionalPost.isEmpty()) {
                    log.warn("Товар не найден при удалении из заблокированных, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostText post = optionalPost.get();
                imageService.deletePostImages(post);
                postRepo.delete(post);
                blockedPostsCacheService.clearBlockedProductsCache();
                log.info("Товар удален из заблокированных, ID: {}", id);

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findById(id);
                if (optionalPost.isEmpty()) {
                    log.warn("Услуга не найдена при удалении из заблокированных, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostTextJob post = optionalPost.get();
                imageService.deletePostJobImages(post);
                postRepoJob.delete(post);
                blockedPostsCacheService.clearBlockedServicesCache();
                log.info("Услуга удалена из заблокированных, ID: {}", id);

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findById(id);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Вакансия не найдена при удалении из заблокированных, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Vacancy vacvac = optionalVacancy.get();
                imageService.deleteVacancyImages(vacvac);
                postVacancyRepository.delete(vacvac);
                blockedPostsCacheService.clearBlockedVacanciesCache();
                log.info("Вакансия удалена из заблокированных, ID: {}", id);

            } else if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findById(id);
                if (optionalResume.isEmpty()) {
                    log.warn("Резюме не найдено при удалении из заблокированных, ID: {}", id);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Resume res = optionalResume.get();
                imageService.deleteResumeImages(res);
                postResumeRepository.delete(res);
                blockedPostsCacheService.clearBlockedResumeCache();
                log.info("Резюме удалено из заблокированных, ID: {}", id);
            }

            return "redirect:/page_two/redactorblocked";

        } catch (Exception e) {
            log.error("Ошибка при удалении из заблокированных типа: {}, ID: {}: {}", type, id, e.getClass().getSimpleName(), e);
            model.addAttribute("error", "Ошибка при удалении: " + e.getMessage());
            return "error";
        }
    }
}