package jobforsite.sitejob.module.shared.controller.sharedcontroller;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.shared.services.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для удаления постов (товаров, услуг, вакансий, резюме) обычными пользователями.
 * Обрабатывает POST-запросы на удаление по hashId с проверкой владельца.
 * Перед удалением очищает связанные изображения и записи в избранном.
 */
@Slf4j
@RequiredArgsConstructor
@Controller
public class DeletePostUserController {

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final FavorRepo favorRepo;
    private final ImageService imageService;

    /**
     * Удаление товара (продукта) обычным пользователем.
     *
     * @param hashId         хеш-идентификатор удаляемого поста
     * @param model          MVC модель для передачи сообщений об ошибках
     * @param authentication данные аутентификации текущего пользователя
     * @return перенаправление на страницу редактора или страницу ошибки
     */
    @PostMapping("/page_two/redactor/{hashId}/remove-user")
    public String redactorDetailsPostRemoveUser(@PathVariable(value = "hashId") String hashId, Model model,
                                                Authentication authentication) {
        return deletePostByUser(hashId, model, authentication, "product");
    }

    /**
     * Удаление услуги обычным пользователем.
     *
     * @param hashId         хеш-идентификатор удаляемой услуги
     * @param model          MVC модель
     * @param authentication данные аутентификации
     * @return перенаправление или ошибка
     */
    @PostMapping("/page_two/redactor/job/{hashId}/remove-user")
    public String redactorJobDetailsPostRemoveUser(@PathVariable(value = "hashId") String hashId, Model model,
                                                   Authentication authentication) {
        return deletePostByUser(hashId, model, authentication, "service");
    }

    /**
     * Удаление вакансии обычным пользователем.
     *
     * @param hashId         хеш-идентификатор вакансии
     * @param model          MVC модель
     * @param authentication данные аутентификации
     * @return перенаправление или ошибка
     */
    @PostMapping("/page_two/redactor/vac/{hashId}/remove-user")
    public String redactorVacancyDetailsPostRemoveUser(@PathVariable(value = "hashId") String hashId, Model model,
                                                       Authentication authentication) {
        return deletePostByUser(hashId, model, authentication, "vacancy");
    }

    /**
     * Удаление резюме обычным пользователем.
     *
     * @param hashId         хеш-идентификатор резюме
     * @param model          MVC модель
     * @param authentication данные аутентификации
     * @return перенаправление или ошибка
     */
    @PostMapping("/page_two/redactor/res/{hashId}/remove-user")
    public String redactorResumeDetailsPostRemoveUser(@PathVariable(value = "hashId") String hashId, Model model,
                                                      Authentication authentication) {
        return deletePostByUser(hashId, model, authentication, "resume");
    }

    /**
     * Общий метод для удаления постов обычными пользователями по hashId.
     * Выполняет проверку владельца, удаляет изображения и записи из избранного.
     *
     * @param hashId         хеш удаляемого объекта
     * @param model          MVC модель
     * @param authentication аутентификация текущего пользователя
     * @param type           тип поста (product, service, vacancy, resume)
     * @return путь для редиректа или имя шаблона ошибки
     */
    private String deletePostByUser(String hashId, Model model, Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            Long userId = currentUser.getId();

            log.info("Delete attempt: type={}, hashId={}", type, hashId);

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Product not found for deletion: hashId={}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostText post = optionalPost.get();

                // Проверяем, что пост принадлежит пользователю
                if (!post.getUser().getId().equals(userId)) {
                    log.warn("Unauthorized product deletion attempt: hashId={}", hashId);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                // Удаляем изображения поста
                imageService.deletePostImages(post);

                // Удаляем пост из избранного всех пользователей перед удалением
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getPost() != null && f.getPost().getId().equals(post.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                postRepo.delete(post);
                log.info("Product deleted successfully: hashId={}", hashId);

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.warn("Service not found for deletion: hashId={}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostTextJob post = optionalPost.get();

                if (!post.getUser().getId().equals(userId)) {
                    log.warn("Unauthorized service deletion attempt: hashId={}", hashId);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                // Удаляем изображения услуги
                imageService.deletePostJobImages(post);

                // Удаляем услугу из избранного всех пользователей перед удалением
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getPostJob() != null && f.getPostJob().getId().equals(post.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                postRepoJob.delete(post);
                log.info("Service deleted successfully: hashId={}", hashId);

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findByHashId(hashId);
                if (optionalVacancy.isEmpty()) {
                    log.warn("Vacancy not found for deletion: hashId={}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Vacancy vacvac = optionalVacancy.get();

                if (!vacvac.getUser().getId().equals(userId)) {
                    log.warn("Unauthorized vacancy deletion attempt: hashId={}", hashId);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                // Удаляем изображения вакансии
                imageService.deleteVacancyImages(vacvac);

                // Удаляем вакансию из избранного всех пользователей перед удалением
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getVacancy() != null && f.getVacancy().getId().equals(vacvac.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                postVacancyRepository.delete(vacvac);
                log.info("Vacancy deleted successfully: hashId={}", hashId);

            } else if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findByHashId(hashId);
                if (optionalResume.isEmpty()) {
                    log.warn("Resume not found for deletion: hashId={}", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Resume resume = optionalResume.get();

                if (!resume.getUser().getId().equals(userId)) {
                    log.warn("Unauthorized resume deletion attempt: hashId={}", hashId);
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                // Удаляем изображения резюме
                imageService.deleteResumeImages(resume);

                // Удаляем резюме из избранного всех пользователей перед удалением
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getResume() != null && f.getResume().getId().equals(resume.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                postResumeRepository.delete(resume);
                log.info("Resume deleted successfully: hashId={}", hashId);
            }

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.warn("Error deleting {} with hashId: {}", type, hashId);
            model.addAttribute("error", "Ошибка при удалении");
            return "error";
        }
    }
}