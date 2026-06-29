package jobforsite.sitejob.module.users.controller;

import jakarta.servlet.http.HttpSession;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
import jobforsite.sitejob.module.shared.services.cachrateservice.PostCacheService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MVC-контроллер для управления контентом пользователей администраторами.
 * <p>
 * Обеспечивает функциональность для администраторов и суперадминистраторов:
 * <ul>
 *     <li>Блокировка/разблокировка постов (товары, услуги, вакансии, резюме)</li>
 *     <li>Удаление контента пользователей</li>
 *     <li>Редактирование контента пользователей</li>
 * </ul>
 * </p>
 *
 * <p><b>Права доступа:</b></p>
 * <ul>
 *     <li>ROLE_SUPER — полный доступ ко всем операциям</li>
 *     <li>ROLE_ADMIN — доступ к блокировке/удалению/редактированию</li>
 *     <li>Обычный пользователь — только к своему контенту</li>
 * </ul>
 *
 * <p><b>Типы контента:</b></p>
 * <ul>
 *     <li>Товары (PostText)</li>
 *     <li>Услуги (PostTextJob)</li>
 *     <li>Вакансии (Vacancy)</li>
 *     <li>Резюме (Resume)</li>
 * </ul>
 *
 * @see PostText
 * @see PostTextJob
 * @see Vacancy
 * @see Resume
 */
@RequiredArgsConstructor
@Controller
public class UpdateUserPost {

    private static final Logger logger = LoggerFactory.getLogger(UpdateUserPost.class);

    private final UserRepository userRepository;
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

    private final ImageService imageService;
    private final FavorRepo favorRepo;
    private final BlockedPostsCacheService blockedPostsCacheService;

    private final PostCacheService postCacheService;

    // ======================================================================
    // ЗАГРУЗКА КОНТЕНТА ПОЛЬЗОВАТЕЛЯ
    // ======================================================================

    /**
     * Загружает контент пользователя в модель.
     * <p>
     * Вызывается при редактировании пользователя администратором.
     * Загружает последние 8 записей каждого типа с сортировкой по дате создания (DESC).
     * </p>
     *
     * <p><b>Загружаемые данные:</b></p>
     * <ul>
     *     <li>Товары пользователя (UserPosts)</li>
     *     <li>Услуги пользователя (userPostsJob)</li>
     *     <li>Вакансии пользователя (userVacancies)</li>
     *     <li>Резюме пользователя (userResume)</li>
     *     <li>Общее количество каждого типа контента</li>
     * </ul>
     *
     * @param user  пользователь, контент которого загружается
     * @param model модель для добавления атрибутов
     */
    public void loadUserContent(User user, Model model) {
        // Создаем pageable с лимитом 8 записей, сортировка по дате создания (новые сверху)
        Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Загружаем страницы контента каждого типа
        Page<PostText> userPostsPage = postRepo.findByUser(user, pageable);
        Page<PostTextJob> userPostsJobPage = postRepoJob.findByUser(user, pageable);
        Page<Vacancy> userVacanciesPage = postVacancyRepository.findByUser(user, pageable);
        Page<Resume> userResumePage = postResumeRepository.findByUser(user, pageable);

        // Подсчитываем общее количество записей каждого типа
        long totalProducts = postRepo.countByUser(user);
        long totalServices = postRepoJob.countByUser(user);
        long totalVacancies = postVacancyRepository.countByUser(user);
        long totalResume = postResumeRepository.countByUser(user);

        logger.debug("User content loaded - products: {}, services: {}, vacancies: {}, resumes: {}",
                totalProducts, totalServices, totalVacancies, totalResume);

        // Добавляем данные в модель
        model.addAttribute("userPosts", userPostsPage.getContent());
        model.addAttribute("userPostsJob", userPostsJobPage.getContent());
        model.addAttribute("userVacancies", userVacanciesPage.getContent());
        model.addAttribute("userResume", userResumePage.getContent());
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalServices", totalServices);
        model.addAttribute("totalVacancies", totalVacancies);
        model.addAttribute("totalResume", totalResume);
    }

    // ======================================================================
    // УПРАВЛЕНИЕ ТОВАРАМИ (PostText)
    // ======================================================================

    /**
     * Переключает статус блокировки товара.
     * <p>
     * Доступно только для суперадминистраторов.
     * При блокировке удаляет товар из избранного у всех пользователей.
     * При разблокировке проверяет, что пользователь не заблокирован.
     * </p>
     *
     * @param hashId         хэш-идентификатор пользователя (для редиректа)
     * @param postId         ID товара
     * @param model          модель для сообщений об ошибках
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования пользователя
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/post/{postId}/toggle-block")
    public String toggleBlockPost(@PathVariable String hashId,
                                  @PathVariable Long postId,
                                  Model model,
                                  Authentication authentication) {
        logger.info("Toggling product block status");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to toggle product block");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<PostText> postOptional = postRepo.findById(postId);
            if (postOptional.isPresent()) {
                PostText post = postOptional.get();
                boolean isCurrentlyBlocked = post.isBlocked();

                if (isCurrentlyBlocked) {
                    // Разблокировка: проверяем, не заблокирован ли пользователь
                    if (post.getUser() != null && !post.getUser().isActive()) {
                        logger.warn("Cannot unblock product - user is still blocked");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                    post.setBlocked(false);
                    post.setBlockedAt(null);
                } else {
                    // Блокировка
                    post.setBlocked(true);
                    post.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByPostId(postId); // Удаляем из избранного
                    post.setPremium(false);
                    post.setPremiumPriority(0);
                    post.setDonate(false);
                }
                postRepo.save(post);
                blockedPostsCacheService.clearBlockedProductsCache();
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error toggling product block: ", e);
            model.addAttribute("error", "Ошибка при изменении статуса товара: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    /**
     * Удаляет товар пользователя.
     * <p>
     * Доступно только для суперадминистраторов.
     * Удаляет связанные изображения и записи из избранного.
     * </p>
     *
     * @param hashId         хэш-идентификатор пользователя (для редиректа)
     * @param postId         ID товара
     * @param model          модель для сообщений об ошибках
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования пользователя
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/post/{postId}/remove")
    public String removePost(@PathVariable String hashId,
                             @PathVariable Long postId,
                             Model model,
                             Authentication authentication) {
        logger.info("Removing product");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to remove product");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<PostText> postOptional = postRepo.findById(postId);
            if (postOptional.isPresent()) {
                PostText post = postOptional.get();
                imageService.deletePostImages(post);      // Удаляем изображения
                favorRepo.deleteByPostId(postId);         // Удаляем из избранного
                postRepo.deleteById(postId);              // Удаляем запись
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error removing product: ", e);
            model.addAttribute("error", "Ошибка при удалении товара: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    // ======================================================================
    // УПРАВЛЕНИЕ УСЛУГАМИ (PostTextJob)
    // ======================================================================

    /**
     * Переключает статус блокировки услуги.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param jobId          ID услуги
     * @param model          модель для ошибок
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/job/{jobId}/toggle-block")
    public String toggleBlockJob(@PathVariable String hashId,
                                 @PathVariable Long jobId,
                                 Model model,
                                 Authentication authentication) {
        logger.info("Toggling service block status");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to toggle service block");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<PostTextJob> jobOptional = postRepoJob.findById(jobId);
            if (jobOptional.isPresent()) {
                PostTextJob job = jobOptional.get();
                boolean isCurrentlyBlocked = job.isBlocked();

                if (isCurrentlyBlocked) {
                    // Разблокировка: проверяем статус пользователя
                    if (job.getUser() != null && !job.getUser().isActive()) {
                        logger.warn("Cannot unblock service - user is still blocked");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                    job.setBlocked(false);
                    job.setBlockedAt(null);
                } else {
                    // Блокировка
                    job.setBlocked(true);
                    job.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByPostJobId(jobId);
                    job.setPremium(false);
                    job.setPremiumPriority(0);
                    job.setDonate(false);
                }
                postRepoJob.save(job);
                blockedPostsCacheService.clearBlockedServicesCache();
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error toggling service block: ", e);
            model.addAttribute("error", "Ошибка при изменении статуса услуги: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    /**
     * Удаляет услугу пользователя.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param jobId          ID услуги
     * @param model          модель для ошибок
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/job/{jobId}/remove")
    public String removeJob(@PathVariable String hashId,
                            @PathVariable Long jobId,
                            Model model,
                            Authentication authentication) {
        logger.info("Removing service");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to remove service");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<PostTextJob> jobOptional = postRepoJob.findById(jobId);
            if (jobOptional.isPresent()) {
                PostTextJob job = jobOptional.get();
                imageService.deletePostJobImages(job);
                favorRepo.deleteByPostJobId(jobId);
                postRepoJob.deleteById(jobId);
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error removing service: ", e);
            model.addAttribute("error", "Ошибка при удалении услуги: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    // ======================================================================
    // УПРАВЛЕНИЕ ВАКАНСИЯМИ (Vacancy)
    // ======================================================================

    /**
     * Переключает статус блокировки вакансии.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param vacancyId      ID вакансии
     * @param model          модель для ошибок
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/vacancy/{vacancyId}/toggle-block")
    public String toggleBlockVacancy(@PathVariable String hashId,
                                     @PathVariable Long vacancyId,
                                     Model model,
                                     Authentication authentication) {
        logger.info("Toggling vacancy block status");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to toggle vacancy block");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<Vacancy> vacancyOptional = postVacancyRepository.findById(vacancyId);
            if (vacancyOptional.isPresent()) {
                Vacancy vacancy = vacancyOptional.get();
                boolean isCurrentlyBlocked = vacancy.isBlocked();

                if (isCurrentlyBlocked) {
                    // Разблокировка: проверяем статус пользователя
                    if (vacancy.getUser() != null && !vacancy.getUser().isActive()) {
                        logger.warn("Cannot unblock vacancy - user is still blocked");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                    vacancy.setBlocked(false);
                    vacancy.setBlockedAt(null);
                } else {
                    // Блокировка
                    vacancy.setBlocked(true);
                    vacancy.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByVacancyId(vacancyId);
                    vacancy.setPremium(false);
                    vacancy.setPremiumPriority(0);
                    vacancy.setDonate(false);
                }
                postVacancyRepository.save(vacancy);
                blockedPostsCacheService.clearBlockedVacanciesCache();
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error toggling vacancy block: ", e);
            model.addAttribute("error", "Ошибка при изменении статуса вакансии: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    /**
     * Удаляет вакансию пользователя.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param vacancyId      ID вакансии
     * @param model          модель для ошибок
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/vacancy/{vacancyId}/remove")
    public String removeVacancy(@PathVariable String hashId,
                                @PathVariable Long vacancyId,
                                Model model,
                                Authentication authentication) {
        logger.info("Removing vacancy");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to remove vacancy");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<Vacancy> vacancyOptional = postVacancyRepository.findById(vacancyId);
            if (vacancyOptional.isPresent()) {
                Vacancy vacancy = vacancyOptional.get();
                imageService.deleteVacancyImages(vacancy);
                favorRepo.deleteByVacancyId(vacancyId);
                postVacancyRepository.deleteById(vacancyId);
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error removing vacancy: ", e);
            model.addAttribute("error", "Ошибка при удалении вакансии: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    // ======================================================================
    // УПРАВЛЕНИЕ РЕЗЮМЕ (Resume)
    // ======================================================================

    /**
     * Переключает статус блокировки резюме.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param resumeId       ID резюме
     * @param model          модель для ошибок
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/resume/{resumeId}/toggle-block")
    public String toggleBlockResume(@PathVariable String hashId,
                                    @PathVariable Long resumeId,
                                    Model model,
                                    Authentication authentication) {
        logger.info("Toggling resume block status");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to toggle resume block");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<Resume> resumeOptional = postResumeRepository.findById(resumeId);
            if (resumeOptional.isPresent()) {
                Resume resume = resumeOptional.get();
                boolean isCurrentlyBlocked = resume.isBlocked();

                if (isCurrentlyBlocked) {
                    // Разблокировка: проверяем статус пользователя
                    if (resume.getUser() != null && !resume.getUser().isActive()) {
                        logger.warn("Cannot unblock resume - user is still blocked");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                    resume.setBlocked(false);
                    resume.setBlockedAt(null);
                } else {
                    // Блокировка
                    resume.setBlocked(true);
                    resume.setBlockedAt(LocalDateTime.now());
                    favorRepo.deleteByResumeId(resumeId);
                }
                postResumeRepository.save(resume);
                blockedPostsCacheService.clearBlockedResumeCache();
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error toggling resume block: ", e);
            model.addAttribute("error", "Ошибка при изменении статуса резюме: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    /**
     * Удаляет резюме пользователя.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param resumeId       ID резюме
     * @param model          модель для ошибок
     * @param authentication аутентификация для проверки прав
     * @return redirect на страницу редактирования
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/resume/{resumeId}/remove")
    public String removeResume(@PathVariable String hashId,
                               @PathVariable Long resumeId,
                               Model model,
                               Authentication authentication) {
        logger.info("Removing resume");

        try {
            if (!hasSuperAccess(authentication)) {
                logger.warn("Access denied to remove resume");
                return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
            }

            Optional<Resume> resumeOptional = postResumeRepository.findById(resumeId);
            if (resumeOptional.isPresent()) {
                Resume resume = resumeOptional.get();
                imageService.deleteResumeImages(resume);
                favorRepo.deleteByResumeId(resumeId);
                postResumeRepository.deleteById(resumeId);
            }
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        } catch (Exception e) {
            logger.error("Error removing resume: ", e);
            model.addAttribute("error", "Ошибка при удалении резюме: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    // ======================================================================
    // РЕДАКТИРОВАНИЕ КОНТЕНТА (GET-редиректы)
    // ======================================================================

    // Эти методы перенаправляют на соответствующие формы редактирования
    // Администраторы и суперадминистраторы перенаправляются на admin-версию формы

    @GetMapping("/users/{hashId}/page_two/admin_edit_user/post/{postId}/edit")
    public String editPost(@PathVariable String hashId,
                           @PathVariable Long postId,
                           Model model,
                           Authentication authentication) {
        return "redirect:/page_two/admin_edit_user/" + postId + "/page_two/edit_admin?hashId=" + hashId;
    }

    @GetMapping("/users/{hashId}/page_two/admin_edit_user/job/{jobId}/edit")
    public String editJob(@PathVariable String hashId,
                          @PathVariable Long jobId,
                          Model model,
                          Authentication authentication) {
        return "redirect:/page_two/admin_edit_user/job/" + jobId + "/page_two/edit_admin?hashId=" + hashId;
    }

    @GetMapping("/users/{hashId}/page_two/admin_edit_user/vacancy/{vacancyId}/edit")
    public String editVacancy(@PathVariable String hashId,
                              @PathVariable Long vacancyId,
                              Model model,
                              Authentication authentication) {
        return "redirect:/page_two/admin_edit_user/vac/" + vacancyId + "/page_two/edit_admin?hashId=" + hashId;
    }

    @GetMapping("/users/{hashId}/page_two/admin_edit_user/resume/{resumeId}/edit")
    public String editResume(@PathVariable String hashId,
                             @PathVariable Long resumeId,
                             Model model,
                             Authentication authentication) {
        return "redirect:/page_two/admin_edit_user/res/" + resumeId + "/page_two/edit_admin?hashId=" + hashId;
    }

    // ======================================================================
    // СТРАНИЦЫ РЕДАКТИРОВАНИЯ КОНТЕНТА (GET для отображения формы)
    // ======================================================================

    /**
     * Отображает форму редактирования товара.
     * <p>
     * Доступна администраторам и владельцу контента.
     * </p>
     *
     * @param hashId        хэш-идентификатор товара
     * @param userHashId    хэш-идентификатор пользователя (для редиректа)
     * @param model         модель для данных формы
     * @param authentication аутентификация для проверки прав
     * @return страница редактирования или ошибка
     */
    @GetMapping("/page_two/admin_edit_user/{hashId}/page_two/edit_admin")
    public String redactorDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                      @RequestParam(value = "userHashId", required = false) String userHashId,
                                      Model model,
                                      Authentication authentication,
                                      HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        Optional<PostText> postwo = postRepo.findByHashId(hashId);
        if (postwo.isEmpty()) {
            return "redirect:/admin_edit_user";
        }

        User currentUser = (User) authentication.getPrincipal();
        PostText post = postwo.get();

        if (!hasEditAccess(currentUser, post.getUser().getId())) {
            model.addAttribute("error", "Доступ запрещен");
            return "error";
        }

        model.addAttribute("postwo", List.of(post));
        model.addAttribute("title", "redactor_admin");
        model.addAttribute("type", "product");
        model.addAttribute("categories", postTextCategoryConfig.getCategories());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("hashId", userHashId);
        model.addAttribute("isPremium", post.isPremium());
        model.addAttribute("premiumPriority", post.getPremiumPriority());
        model.addAttribute("isDonate", post.isDonate());
        return "page_two/edit_admin";
    }

    /**
     * Отображает форму редактирования услуги.
     *
     * @param hashId        хэш-идентификатор услуги
     * @param userHashId    хэш-идентификатор пользователя
     * @param model         модель для данных формы
     * @param authentication аутентификация для проверки прав
     * @return страница редактирования или ошибка
     */
    @GetMapping("/page_two/admin_edit_user/job/{hashId}/page_two/edit_admin")
    public String redactorJobDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                         @RequestParam(value = "userHashId", required = false) String userHashId,
                                         Model model,
                                         Authentication authentication,
                                         HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        Optional<PostTextJob> postwo = postRepoJob.findByHashId(hashId);
        if (postwo.isEmpty()) {
            return "redirect:/admin_edit_user";
        }

        User currentUser = (User) authentication.getPrincipal();
        PostTextJob post = postwo.get();

        if (!hasEditAccess(currentUser, post.getUser().getId())) {
            model.addAttribute("error", "Доступ запрещен");
            return "error";
        }

        model.addAttribute("postwo", List.of(post));
        model.addAttribute("title", "redactor_admin");
        model.addAttribute("type", "service");
        model.addAttribute("categories", postTextJobCategoryConfig.getCategories());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("hashId", userHashId);
        model.addAttribute("isPremium", post.isPremium());
        model.addAttribute("premiumPriority", post.getPremiumPriority());
        model.addAttribute("isDonate", post.isDonate());
        return "page_two/edit_admin";
    }

    /**
     * Отображает форму редактирования вакансии.
     *
     * @param hashId        хэш-идентификатор вакансии
     * @param userHashId    хэш-идентификатор пользователя
     * @param model         модель для данных формы
     * @param authentication аутентификация для проверки прав
     * @return страница редактирования или ошибка
     */
    @GetMapping("/page_two/admin_edit_user/vac/{hashId}/page_two/edit_admin")
    public String redactorVacancyDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                             @RequestParam(value = "userHashId", required = false) String userHashId,
                                             Model model,
                                             Authentication authentication,
                                             HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        Optional<Vacancy> vacvac = postVacancyRepository.findByHashId(hashId);
        if (vacvac.isEmpty()) {
            return "redirect:/admin_edit_user";
        }

        User currentUser = (User) authentication.getPrincipal();
        Vacancy vacancy = vacvac.get();

        if (!hasEditAccess(currentUser, vacancy.getUser().getId())) {
            model.addAttribute("error", "Доступ запрещен");
            return "error";
        }

        model.addAttribute("vacvac", List.of(vacancy));
        model.addAttribute("title", "redactor_admin");
        model.addAttribute("type", "vacancy");
        model.addAttribute("categories", postVacancyCategoryConfig.getCategories());
        model.addAttribute("experience", experienceVacancyCategoryConfig.getExperience());
        model.addAttribute("workformat", workFormatVacancyConfig.getWorkformat());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("hashId", userHashId);
        model.addAttribute("isPremium", vacancy.isPremium());
        model.addAttribute("premiumPriority", vacancy.getPremiumPriority());
        model.addAttribute("isDonate", vacancy.isDonate());
        return "page_two/edit_admin";
    }

    /**
     * Отображает форму редактирования резюме.
     *
     * @param hashId        хэш-идентификатор резюме
     * @param userHashId    хэш-идентификатор пользователя
     * @param model         модель для данных формы
     * @param authentication аутентификация для проверки прав
     * @return страница редактирования или ошибка
     */
    @GetMapping("/page_two/admin_edit_user/res/{hashId}/page_two/edit_admin")
    public String redactorResumeDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                            @RequestParam(value = "userHashId", required = false) String userHashId,
                                            Model model,
                                            Authentication authentication,
                                            HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        Optional<Resume> resum = postResumeRepository.findByHashId(hashId);
        if (resum.isEmpty()) {
            return "redirect:/admin_edit_user";
        }

        User currentUser = (User) authentication.getPrincipal();
        Resume resume = resum.get();

        if (!hasEditAccess(currentUser, resume.getUser().getId())) {
            model.addAttribute("error", "Доступ запрещен");
            return "error";
        }

        model.addAttribute("resume", List.of(resume));
        model.addAttribute("title", "redactor_admin");
        model.addAttribute("type", "resume");
        model.addAttribute("categories", postResumeCategoryConfig.getCategories());
        model.addAttribute("experience", experienceResumCategoryConfig.getExperience());
        model.addAttribute("workformat", workFormatResumeConfig.getWorkformat());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        model.addAttribute("isAdmin", isAdmin(currentUser));
        model.addAttribute("hashId", userHashId);
        return "page_two/edit_admin";
    }

    // ======================================================================
    // ОБНОВЛЕНИЕ КОНТЕНТА (POST-обработчики)
    // ======================================================================

    // Эти методы принимают данные из форм редактирования и вызывают соответствующие приватные методы

    @PostMapping("/page_two/admin_edit_user/{hashId}/page_two/edit_admin")
    public String redactorDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                            @RequestParam String title,
                                            @RequestParam Integer price,
                                            @RequestParam String full_text,
                                            @RequestParam String category,
                                            @RequestParam String location,
                                            @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                            @RequestParam(value = "files", required = false) MultipartFile[] files,
                                            @RequestParam(value = "userHashId", required = false) String userHashId,
                                            @RequestParam(value = "premium", required = false, defaultValue = "false") Boolean premium,
                                            @RequestParam(value = "premiumPriority", required = false, defaultValue = "0") Integer premiumPriority,
                                            @RequestParam(value = "donate", required = false, defaultValue = "false") Boolean donate,
                                            Model model,
                                            Authentication authentication) {
        Optional<PostText> postOptional = postRepo.findByHashId(hashId);
        if (postOptional.isEmpty()) {
            model.addAttribute("error", "Запись не найдена");
            return "error";
        }
        return updateProductPost(postOptional.get().getId(), title, price, full_text, category, location,
                deleteImageIds, files, userHashId, premium, premiumPriority, donate, model, authentication);
    }

    @PostMapping("/page_two/admin_edit_user/job/{hashId}/page_two/edit_admin")
    public String redactorJobDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                               @RequestParam String title,
                                               @RequestParam Integer price,
                                               @RequestParam String full_text,
                                               @RequestParam String category,
                                               @RequestParam String location,
                                               @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                               @RequestParam(value = "files", required = false) MultipartFile[] files,
                                               @RequestParam(value = "userHashId", required = false) String userHashId,
                                               @RequestParam(value = "premium", required = false, defaultValue = "false") Boolean premium,
                                               @RequestParam(value = "premiumPriority", required = false, defaultValue = "0") Integer premiumPriority,
                                               @RequestParam(value = "donate", required = false, defaultValue = "false") Boolean donate,
                                               Model model,
                                               Authentication authentication) {
        Optional<PostTextJob> postOptional = postRepoJob.findByHashId(hashId);
        if (postOptional.isEmpty()) {
            model.addAttribute("error", "Запись не найдена");
            return "error";
        }
        return updateServicePost(postOptional.get().getId(), title, price, full_text, category, location,
                deleteImageIds, files, userHashId, premium, premiumPriority, donate, model, authentication);
    }

    @PostMapping("/page_two/admin_edit_user/vac/{hashId}/page_two/edit_admin")
    public String redactorVacancyDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                                   @RequestParam String title,
                                                   @RequestParam Integer cash,
                                                   @RequestParam String full_text,
                                                   @RequestParam String category,
                                                   @RequestParam String experience,
                                                   @RequestParam String workformat,
                                                   @RequestParam String location,
                                                   @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                                   @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                   @RequestParam(value = "userHashId", required = false) String userHashId,
                                                   @RequestParam(value = "premium", required = false, defaultValue = "false") Boolean premium,
                                                   @RequestParam(value = "premiumPriority", required = false, defaultValue = "0") Integer premiumPriority,
                                                   @RequestParam(value = "donate", required = false, defaultValue = "false") Boolean donate,
                                                   Model model,
                                                   Authentication authentication) {
        Optional<Vacancy> vacancyOptional = postVacancyRepository.findByHashId(hashId);
        if (vacancyOptional.isEmpty()) {
            model.addAttribute("error", "Запись не найдена");
            return "error";
        }
        return updatePostVacancy(vacancyOptional.get().getId(), title, cash, full_text, category, experience, workformat, location,
                deleteImageIds, files, userHashId, premium, premiumPriority, donate, model, authentication);
    }

    @PostMapping("/page_two/admin_edit_user/res/{hashId}/page_two/edit_admin")
    public String redactorResumeDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                                  @RequestParam String title,
                                                  @RequestParam Integer cash,
                                                  @RequestParam String full_text,
                                                  @RequestParam String category,
                                                  @RequestParam String experience,
                                                  @RequestParam String workformat,
                                                  @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                                  @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                  @RequestParam(value = "userHashId", required = false) String userHashId,
                                                  Model model,
                                                  Authentication authentication) {
        Optional<Resume> resumeOptional = postResumeRepository.findByHashId(hashId);
        if (resumeOptional.isEmpty()) {
            model.addAttribute("error", "Запись не найдена");
            return "error";
        }
        return updatePostResume(resumeOptional.get().getId(), title, cash, full_text, category, experience, workformat,
                deleteImageIds, files, userHashId, model, authentication);
    }

    // ======================================================================
    // ПРИВАТНЫЕ МЕТОДЫ ОБНОВЛЕНИЯ
    // ======================================================================

    /**
     * Обновляет товар.
     */
    private String updateProductPost(Long id, String title, Integer price, String full_text,
                                     String category, String location, List<Long> deleteImageIds,
                                     MultipartFile[] files, String userHashId,
                                     Boolean premium, Integer premiumPriority,
                                     Boolean donate, Model model,
                                     Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<PostText> optionalPost = postRepo.findById(id);
            if (optionalPost.isEmpty()) {
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            PostText post = optionalPost.get();
            if (!hasEditAccess(currentUser, post.getUser().getId())) {
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            // Обработка изображений
            processImages(post.getImages().size(), deleteImageIds, files, post, null, null, null, model);

            // Обновление полей
            post.setTitle(title);
            post.setPrice(price);
            post.setFull_text(full_text);
            post.setCategory(category);
            post.setLocation(location);

            if (premium != null) {
                post.setPremium(premium);
            }
            if (premiumPriority != null) {
                post.setPremiumPriority(premiumPriority);
            }

            if (donate != null) {
                post.setDonate(donate);
            }

            // Обновляем дату публикации, чтобы объявление попало в карусель
            if (premium != null && premium) {
                post.setModeratedAt(LocalDateTime.now());
                logger.info("Updated moderatedAt for premium product id: {}", post.getId());
            }
            // ==================================

            postRepo.save(post);


            postCacheService.clearIndexCache();

            // Удаление помеченных изображений
            deleteMarkedImagesAfterUpdate(post, null, null, null);

            String returnHash = (userHashId != null && !userHashId.isEmpty()) ? userHashId : post.getUser().getHashId();
            return "redirect:/page_two/admin_edit_user/" + post.getHashId() + "/page_two/edit_admin?userHashId=" + returnHash;
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postTextCategoryConfig.getCategories());

            return "redirect:/page_two/admin_edit_user?error=size_limit";

        } catch (Exception e) {
            logger.error("Error updating product: ", e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Обновляет услугу.
     */
    private String updateServicePost(Long id, String title, Integer price, String full_text,
                                     String category, String location, List<Long> deleteImageIds,
                                     MultipartFile[] files, String userHashId,
                                     Boolean premium, Integer premiumPriority,
                                     Boolean donate, Model model,
                                     Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<PostTextJob> optionalPost = postRepoJob.findById(id);
            if (optionalPost.isEmpty()) {
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            PostTextJob post = optionalPost.get();
            if (!hasEditAccess(currentUser, post.getUser().getId())) {
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            processImages(post.getImages().size(), deleteImageIds, files, null, post, null, null, model);
            post.setTitle(title);
            post.setPrice(price);
            post.setFull_text(full_text);
            post.setCategory(category);
            post.setLocation(location);

            if (premium != null) {
                post.setPremium(premium);
            }
            if (premiumPriority != null) {
                post.setPremiumPriority(premiumPriority);
            }

            if (donate != null) {
                post.setDonate(donate);
            }

            // Обновляем дату публикации, чтобы объявление попало в карусель
            if (premium != null && premium) {
                post.setModeratedAt(LocalDateTime.now());
                logger.info("Updated moderatedAt for premium service id: {}", post.getId());
            }
            // ==================================

            postRepoJob.save(post);

            postCacheService.clearIndexCache();

            deleteMarkedImagesAfterUpdate(null, post, null, null);

            String returnHash = (userHashId != null && !userHashId.isEmpty()) ? userHashId : post.getUser().getHashId();
            return "redirect:/page_two/admin_edit_user/job/" + post.getHashId() + "/page_two/edit_admin?userHashId=" + returnHash;
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postTextJobCategoryConfig.getCategories());

            return "redirect:/page_two/admin_edit_user?error=size_limit";

        } catch (Exception e) {
            logger.error("Error updating service: ", e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Обновляет вакансию.
     */
    private String updatePostVacancy(Long id, String title, Integer cash, String full_text,
                                     String category, String experience, String workformat, String location, List<Long> deleteImageIds,
                                     MultipartFile[] files, String userHashId,
                                     Boolean premium, Integer premiumPriority,
                                     Boolean donate, Model model,
                                     Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<Vacancy> optionalVac = postVacancyRepository.findById(id);
            if (optionalVac.isEmpty()) {
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            Vacancy vaca = optionalVac.get();
            if (!hasEditAccess(currentUser, vaca.getUser().getId())) {
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            processImages(vaca.getImages().size(), deleteImageIds, files, null, null, vaca, null, model);
            vaca.setTitle(title);
            vaca.setCash(cash);
            vaca.setFull_text(full_text);
            vaca.setCategory(category);
            vaca.setExperience(experience);
            vaca.setWorkformat(workformat);
            vaca.setLocation(location);

            if (premium != null) {
                vaca.setPremium(premium);
            }
            if (premiumPriority != null) {
                vaca.setPremiumPriority(premiumPriority);
            }

            if (donate != null) {
                vaca.setDonate(donate);
            }

            // Обновляем дату публикации, чтобы объявление попало в карусель
            if (premium != null && premium) {
                vaca.setModeratedAt(LocalDateTime.now());
                logger.info("Updated moderatedAt for premium vacancy id: {}", vaca.getId());
            }
            // ==================================

            postVacancyRepository.save(vaca);

            postCacheService.clearIndexCache();

            deleteMarkedImagesAfterUpdate(null, null, vaca, null);

            String returnHash = (userHashId != null && !userHashId.isEmpty()) ? userHashId : vaca.getUser().getHashId();
            return "redirect:/page_two/admin_edit_user/vac/" + vaca.getHashId() + "/page_two/edit_admin?userHashId=" + returnHash;
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postVacancyCategoryConfig.getCategories());

            return "redirect:/page_two/admin_edit_user?error=size_limit";

        } catch (Exception e) {
            logger.error("Error updating vacancy: ", e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Обновляет резюме.
     */
    private String updatePostResume(Long id, String title, Integer cash, String full_text,
                                    String category, String experience, String workformat, List<Long> deleteImageIds,
                                    MultipartFile[] files, String userHashId, Model model,
                                    Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            Optional<Resume> optionalRes = postResumeRepository.findById(id);
            if (optionalRes.isEmpty()) {
                model.addAttribute("error", "Запись не найдена");
                return "error";
            }

            Resume res = optionalRes.get();
            if (!hasEditAccess(currentUser, res.getUser().getId())) {
                model.addAttribute("error", "Доступ запрещен");
                return "error";
            }

            // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА ==========
            imageService.validateFilesSize(files, 20, 20);
            // ================================================

            processImages(res.getImages().size(), deleteImageIds, files, null, null, null, res, model);
            res.setTitle(title);
            res.setCash(cash);
            res.setFull_text(full_text);
            res.setCategory(category);
            res.setExperience(experience);
            res.setWorkformat(workformat);
            postResumeRepository.save(res);
            deleteMarkedImagesAfterUpdate(null, null, null, res);

            String returnHash = (userHashId != null && !userHashId.isEmpty()) ? userHashId : res.getUser().getHashId();
            return "redirect:/page_two/admin_edit_user/res/" + res.getHashId() + "/page_two/edit_admin?userHashId=" + returnHash;
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка валидации размера файлов: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", postResumeCategoryConfig.getCategories());

            return "redirect:/page_two/admin_edit_user?error=size_limit";

        } catch (Exception e) {
            logger.error("Error updating resume: ", e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    // ======================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ======================================================================

    /**
     * Проверяет, имеет ли пользователь права суперадминистратора.
     *
     * @param authentication аутентификация пользователя
     * @return true если роль ROLE_SUPER или ROLE_ADMIN
     */
    private boolean hasSuperAccess(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Проверяет, имеет ли пользователь право редактировать контент.
     * <p>
     * Право есть у ROLE_SUPER, ROLE_ADMIN или владельца контента.
     * </p>
     *
     * @param currentUser текущий пользователь
     * @param postOwnerId ID владельца контента
     * @return true если доступ разрешен
     */
    private boolean hasEditAccess(User currentUser, Long postOwnerId) {
        return currentUser.getRole().equals("ROLE_SUPER") ||
                currentUser.getRole().equals("ROLE_ADMIN") ||
                currentUser.getId().equals(postOwnerId);
    }

    /**
     * Проверяет, является ли пользователь администратором.
     *
     * @param user пользователь
     * @return true если роль ROLE_ADMIN или ROLE_SUPER
     */
    private boolean isAdmin(User user) {
        return user.getRole().equals("ROLE_ADMIN") || user.getRole().equals("ROLE_SUPER");
    }

    /**
     * Обрабатывает изображения при обновлении поста.
     * <p>
     * Помечает изображения для удаления и загружает новые.
     * Проверяет лимит в 10 изображений на пост.
     * </p>
     *
     * @param currentImageCount текущее количество изображений
     * @param deleteImageIds    ID изображений для удаления
     * @param files             новые файлы для загрузки
     * @param post              товар (может быть null)
     * @param postJob           услуга (может быть null)
     * @param vacancy           вакансия (может быть null)
     * @param resume            резюме (может быть null)
     * @param model             модель для ошибок
     * @throws IOException если ошибка при работе с файлами
     */
    private void processImages(int currentImageCount, List<Long> deleteImageIds, MultipartFile[] files,
                               PostText post, PostTextJob postJob, Vacancy vacancy, Resume resume, Model model) throws IOException {
        // Помечаем изображения для удаления
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            imageService.markImagesForDeletion(deleteImageIds);
        }

        // Загружаем новые изображения
        if (files != null && files.length > 0) {
            int remainingCount = currentImageCount - (deleteImageIds != null ? deleteImageIds.size() : 0);
            if (remainingCount + files.length > 5) {
                throw new IllegalArgumentException("Нельзя загрузить более 5 изображений");
            }
            imageService.saveImages(files, post, postJob, vacancy, resume);
        }
    }

    /**
     * Удаляет помеченные изображения после обновления.
     *
     * @param post    товар (может быть null)
     * @param postJob услуга (может быть null)
     * @param vacancy вакансия (может быть null)
     * @param resume  резюме (может быть null)
     */
    private void deleteMarkedImagesAfterUpdate(PostText post, PostTextJob postJob, Vacancy vacancy, Resume resume) {
        try {
            if (post != null) imageService.deleteMarkedImagesForPost(post);
            else if (postJob != null) imageService.deleteMarkedImagesForPostJob(postJob);
            else if (vacancy != null) imageService.deleteMarkedImagesForVacancy(vacancy);
            else if (resume != null) imageService.deleteMarkedImagesForResume(resume);
        } catch (Exception e) {
            logger.error("Error deleting marked images: ", e);
        }
    }
}