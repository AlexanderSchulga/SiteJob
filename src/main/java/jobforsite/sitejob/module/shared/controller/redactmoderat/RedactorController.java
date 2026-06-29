package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.model.ImagesFoto;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.shared.repository.ImageRepo;
import jobforsite.sitejob.module.shared.repository.PostViewRepository;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.shared.services.cachrateservice.ModerationPostsCacheService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
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

import jakarta.servlet.http.HttpSession;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Контроллер для управления редакторскими функциями: отображение, редактирование и удаление
 * постов (товары, услуги, вакансии, резюме) с поддержкой пагинации и управлением изображениями.
 *
 * @author Developer
 * @version 1.0
 */
@RequiredArgsConstructor
@Controller
public class RedactorController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(RedactorController.class);

    // Репозитории для работы с различными типами постов
    private final PostRepo postRepo;                          // Товары
    private final PostRepoJob postRepoJob;                    // Услуги
    private final PostVacancyRepository postVacancyRepository; // Вакансии
    private final PostResumeRepository postResumeRepository;   // Резюме

    // Конфигурации категорий для каждого типа постов
    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;
    private final ExperienceResumCategoryConfig experienceResumCategoryConfig;

    private final ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;

    private final WorkFormatVacancyConfig workFormatVacancyConfig;

    private final WorkFormatResumeConfig workFormatResumeConfig;

    private final PostResumeCategoryConfig postResumeCategoryConfig;

    // Сервисы и репозитории для изображений, комментариев и кэширования
    private final ImageService imageService;
    private final CommentRepoPersonal commentRepoPersonal;
    private final CommentRepoJobPersonal commentRepoJobPersonal;
    private final CommentRepoVacPersonal commentRepoVacPersonal;
    private final CommentRepoResPersonal commentRepoResPersonal;
    private final ModerationPostsCacheService moderationPostsCacheService;
    private final FavorRepo favorRepo;
    private final ImageRepo imageRepository;
    private final PostViewRepository postViewRepository;

    /**
     * Отображает главную страницу редактора со списками всех типов постов (товары, услуги, вакансии, резюме).
     * Поддерживает отдельную пагинацию для каждого типа.
     *
     * @param productPage   номер страницы для товаров
     * @param servicePage   номер страницы для услуг
     * @param vacancyPage   номер страницы для вакансий
     * @param resumePage    номер страницы для резюме
     * @param size          количество элементов на странице
     * @param model         модель для передачи данных в представление
     * @param authentication объект аутентификации текущего пользователя
     * @return имя представления или редирект на логин при ошибке аутентификации
     */
    @GetMapping("/page_two/redactor")
    public String redactorMain(
            @RequestParam(value = "productPage", defaultValue = "0") int productPage,
            @RequestParam(value = "servicePage", defaultValue = "0") int servicePage,
            @RequestParam(value = "vacancyPage", defaultValue = "0") int vacancyPage,
            @RequestParam(value = "resumePage", defaultValue = "0") int resumePage,
            @RequestParam(value = "size", defaultValue = "8") int size,
            Model model,
            Authentication authentication) {

        try {
            // Проверка аутентификации
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Попытка доступа к редактору без аутентификации");
                return "redirect:/login";
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof User)) {
                log.warn("Неверный тип principal в аутентификации");
                return "redirect:/login";
            }

            User currentUser = (User) principal;
            log.debug("Загрузка страницы редактора для пользователя с ролью: {}", currentUser.getRole());

            // Создаем Pageable объекты для пагинации (сортировка по дате модерации DESC)
            Pageable productPageable = PageRequest.of(productPage, size, Sort.by(Sort.Direction.DESC, "moderatedAt"));
            Pageable servicePageable = PageRequest.of(servicePage, size, Sort.by(Sort.Direction.DESC, "moderatedAt"));
            Pageable vacancyPageable = PageRequest.of(vacancyPage, size, Sort.by(Sort.Direction.DESC, "moderatedAt"));
            Pageable resumePageable = PageRequest.of(resumePage, size, Sort.by(Sort.Direction.DESC, "moderatedAt"));

            // Загружаем посты с пагинацией (для суперпользователя - все, иначе только свои)
            Page<PostText> postsPage;
            Page<PostTextJob> postsJobPage;
            Page<Vacancy> vacanciesPage;
            Page<Resume> resumesPage;

            if ("ROLE_SUPER".equals(currentUser.getRole())) {
                log.debug("Загрузка всех постов для суперпользователя");
                postsPage = postRepo.findAll(productPageable);
                postsJobPage = postRepoJob.findAll(servicePageable);
                vacanciesPage = postVacancyRepository.findAll(vacancyPageable);
                resumesPage = postResumeRepository.findAll(resumePageable);
            } else {
                log.debug("Загрузка только своих постов для обычного пользователя");
                postsPage = postRepo.findByUser(currentUser, productPageable);
                postsJobPage = postRepoJob.findByUser(currentUser, servicePageable);
                vacanciesPage = postVacancyRepository.findByUser(currentUser, vacancyPageable);
                resumesPage = postResumeRepository.findByUser(currentUser, resumePageable);
            }

            // Получаем ID постов с непрочитанными сообщениями для отображения индикаторов
            List<Long> postsWithUnreadMessages = commentRepoPersonal != null
                    ? commentRepoPersonal.findPostIdsWithUnreadMessages(currentUser.getId())
                    : new ArrayList<>();

            List<Long> postsWithUnreadMessagesJob = commentRepoJobPersonal != null
                    ? commentRepoJobPersonal.findPostIdsWithUnreadMessages(currentUser.getId())
                    : new ArrayList<>();

            List<Long> postsWithUnreadMessagesVac = commentRepoVacPersonal != null
                    ? commentRepoVacPersonal.findPostVacIdsWithUnreadMessages(currentUser.getId())
                    : new ArrayList<>();

            List<Long> postsWithUnreadMessagesRes = commentRepoResPersonal != null
                    ? commentRepoResPersonal.findResIdsWithUnreadMessages(currentUser.getId())
                    : new ArrayList<>();

            // Добавляем данные в модель
            model.addAttribute("posts", postsPage.getContent());
            model.addAttribute("title", "Редактор объявлений");
            model.addAttribute("isSuper", "ROLE_SUPER".equals(currentUser.getRole()));
            model.addAttribute("postsWithUnreadMessages", postsWithUnreadMessages);
            model.addAttribute("postsJob", postsJobPage.getContent());
            model.addAttribute("postsWithUnreadMessagesJob", postsWithUnreadMessagesJob);
            model.addAttribute("vacancies", vacanciesPage.getContent());
            model.addAttribute("postsWithUnreadMessagesVac", postsWithUnreadMessagesVac);
            model.addAttribute("resume", resumesPage.getContent());
            model.addAttribute("postsWithUnreadMessagesRes", postsWithUnreadMessagesRes);

            // Информация о пагинации
            model.addAttribute("hasMoreProducts", postsPage.hasNext());
            model.addAttribute("hasMoreServices", postsJobPage.hasNext());
            model.addAttribute("hasMoreVacancies", vacanciesPage.hasNext());
            model.addAttribute("hasMoreResume", resumesPage.hasNext());
            model.addAttribute("currentProductPage", productPage);
            model.addAttribute("currentServicePage", servicePage);
            model.addAttribute("currentVacancyPage", vacancyPage);
            model.addAttribute("currentResumePage", resumesPage);
            model.addAttribute("pageSize", size);

            return "page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка загрузки страницы редактора: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка загрузки статей: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Отображает форму редактирования товара.
     *
     * @param hashId уникальный идентификатор поста
     * @param model модель для передачи данных
     * @param authentication объект аутентификации
     * @return имя представления редактирования или редирект/ошибка
     */
    @GetMapping("/page_two/redactor/{hashId}/page_two/edit")
    public String redactorDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                      Model model,
                                      Authentication authentication,
                                      HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        if (!postRepo.existsByHashId(hashId)) {
            log.warn("Редактирование товара: пост с hashId {} не найден", hashId);
            return "redirect:/index";
        }

        Optional<PostText> postwo = postRepo.findByHashId(hashId);
        User currentUser = (User) authentication.getPrincipal();

        // Проверка прав доступа
        if (!currentUser.getRole().equals("ROLE_SUPER") &&
                postwo.isPresent() && !postwo.get().getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка редактирования чужого товара пользователем с ролью: {}", currentUser.getRole());
            model.addAttribute("error", "Доступ запрещен: это не ваш пост");
            return "error";
        }

        ArrayList<PostText> res = new ArrayList<>();
        postwo.ifPresent(res::add);
        model.addAttribute("postwo", res);
        model.addAttribute("title", "redactor");
        model.addAttribute("type", "product");
        model.addAttribute("categories", postTextCategoryConfig.getCategories());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        return "page_two/edit";
    }

    /**
     * Отображает форму редактирования услуги.
     *
     * @param hashId уникальный идентификатор поста
     * @param model модель для передачи данных
     * @param authentication объект аутентификации
     * @return имя представления редактирования или редирект/ошибка
     */
    @GetMapping("/page_two/redactor/job/{hashId}/page_two/edit")
    public String redactorJobDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                         Model model,
                                         Authentication authentication,
                                         HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        if (!postRepoJob.existsByHashId(hashId)) {
            log.warn("Редактирование услуги: пост с hashId {} не найден", hashId);
            return "redirect:/index";
        }

        Optional<PostTextJob> postwo = postRepoJob.findByHashId(hashId);
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getRole().equals("ROLE_SUPER") &&
                postwo.isPresent() && !postwo.get().getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка редактирования чужой услуги");
            model.addAttribute("error", "Доступ запрещен: это не ваш пост");
            return "error";
        }

        ArrayList<PostTextJob> res = new ArrayList<>();
        postwo.ifPresent(res::add);
        model.addAttribute("postwo", res);
        model.addAttribute("title", "redactor");
        model.addAttribute("type", "service");
        model.addAttribute("categories", postTextJobCategoryConfig.getCategories());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        return "page_two/edit";
    }

    /**
     * Отображает форму редактирования вакансии.
     *
     * @param hashId уникальный идентификатор вакансии
     * @param model модель для передачи данных
     * @param authentication объект аутентификации
     * @return имя представления редактирования или редирект/ошибка
     */
    @GetMapping("/page_two/redactor/vac/{hashId}/page_two/edit")
    public String redactorVacancyDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                             Model model,
                                             Authentication authentication,
                                             HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        if (!postVacancyRepository.existsByHashId(hashId)) {
            log.warn("Редактирование вакансии: пост с hashId {} не найден", hashId);
            return "redirect:/index";
        }

        Optional<Vacancy> vacvac = postVacancyRepository.findByHashId(hashId);
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getRole().equals("ROLE_SUPER") &&
                vacvac.isPresent() && !vacvac.get().getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка редактирования чужой вакансии");
            model.addAttribute("error", "Доступ запрещен: это не ваш пост");
            return "error";
        }

        ArrayList<Vacancy> resvac = new ArrayList<>();
        vacvac.ifPresent(resvac::add);
        model.addAttribute("vacvac", resvac);
        model.addAttribute("title", "redactor");
        model.addAttribute("type", "vacancy");
        model.addAttribute("categories", postVacancyCategoryConfig.getCategories());
        model.addAttribute("experience", experienceVacancyCategoryConfig.getExperience());
        model.addAttribute("workformat", workFormatVacancyConfig.getWorkformat());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        return "page_two/edit";
    }

    /**
     * Отображает форму редактирования резюме.
     *
     * @param hashId уникальный идентификатор резюме
     * @param model модель для передачи данных
     * @param authentication объект аутентификации
     * @return имя представления редактирования или редирект/ошибка
     */
    @GetMapping("/page_two/redactor/res/{hashId}/page_two/edit_res")
    public String redactorResumeDetailsEdit(@PathVariable(value = "hashId") String hashId,
                                            Model model,
                                            Authentication authentication,
                                            HttpSession session) {

        if (session.getAttribute("uploadError") != null) {
            model.addAttribute("error", session.getAttribute("uploadError"));
            session.removeAttribute("uploadError");
        }

        if (!postResumeRepository.existsByHashId(hashId)) {
            log.warn("Редактирование резюме: пост с hashId {} не найден", hashId);
            return "redirect:/index";
        }

        Optional<Resume> resume = postResumeRepository.findByHashId(hashId);
        User currentUser = (User) authentication.getPrincipal();

        if (!currentUser.getRole().equals("ROLE_SUPER") &&
                resume.isPresent() && !resume.get().getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка редактирования чужого резюме");
            model.addAttribute("error", "Доступ запрещен: это не ваш пост");
            return "error";
        }

        ArrayList<Resume> res = new ArrayList<>();
        resume.ifPresent(res::add);
        model.addAttribute("res", res);
        model.addAttribute("title", "redactor");
        model.addAttribute("type", "resume");
        model.addAttribute("categories", postResumeCategoryConfig.getCategories());
        model.addAttribute("experience", experienceResumCategoryConfig.getExperience());
        model.addAttribute("workformat", workFormatResumeConfig.getWorkformat());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));
        return "page_two/edit_res";
    }

    /**
     * Обрабатывает обновление товара.
     *
     * @param hashId идентификатор поста
     * @param title заголовок
     * @param price цена
     * @param full_text полный текст
     * @param category категория
     * @param location местоположение
     * @param deleteImageIds ID изображений для удаления
     * @param files новые файлы изображений
     * @param model модель
     * @param authentication аутентификация
     * @return редирект на страницу редактора
     */
    @PostMapping("/page_two/redactor/{hashId}/page_two/edit")
    public String redactorDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                            @RequestParam String title,
                                            @RequestParam Integer price,
                                            @RequestParam String full_text,
                                            @RequestParam String category,
                                            @RequestParam String location,
                                            @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                            @RequestParam(value = "files", required = false) MultipartFile[] files,
                                            Model model,
                                            Authentication authentication) {
        return updatePost(hashId, title, price, full_text, category, location, deleteImageIds, files, model, authentication, "product");
    }

    /**
     * Обрабатывает обновление услуги.
     */
    @PostMapping("/page_two/redactor/job/{hashId}/page_two/edit")
    public String redactorJobDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                               @RequestParam String title,
                                               @RequestParam Integer price,
                                               @RequestParam String full_text,
                                               @RequestParam String category,
                                               @RequestParam String location,
                                               @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                               @RequestParam(value = "files", required = false) MultipartFile[] files,
                                               Model model,
                                               Authentication authentication) {
        return updatePost(hashId, title, price, full_text, category, location, deleteImageIds, files, model, authentication, "service");
    }

    /**
     * Обрабатывает обновление вакансии.
     */
    @PostMapping("/page_two/redactor/vac/{hashId}/page_two/edit")
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
                                                   Model model,
                                                   Authentication authentication) {
        return updatePostVacancy(hashId, title, cash, full_text, category, experience, workformat, location, deleteImageIds, files, model, authentication, "vacancy");
    }

    /**
     * Обрабатывает обновление резюме.
     */
    @PostMapping("/page_two/redactor/res/{hashId}/page_two/edit_res")
    public String redactorResumeDetailsPostUpdate(@PathVariable(value = "hashId") String hashId,
                                                  @RequestParam String title,
                                                  @RequestParam Integer cash,
                                                  @RequestParam String full_text,
                                                  @RequestParam String category,
                                                  @RequestParam String experience,
                                                  @RequestParam String workformat,
                                                  @RequestParam(value = "deleteImages", required = false) List<Long> deleteImageIds,
                                                  @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                  Model model,
                                                  Authentication authentication) {
        return updatePostResume(hashId, title, cash, full_text, category, experience, workformat, deleteImageIds, files, model, authentication, "resume");
    }

    /**
     * Общий метод для обновления постов (товаров и услуг).
     *
     * @param hashId идентификатор
     * @param title заголовок
     * @param price цена
     * @param full_text полный текст
     * @param category категория
     * @param location местоположение
     * @param deleteImageIds ID удаляемых изображений
     * @param files новые файлы
     * @param model модель
     * @param authentication аутентификация
     * @param type тип поста ("product" или "service")
     * @return редирект на страницу редактора
     */
    private String updatePost(String hashId, String title, Integer price, String full_text, String category, String location,
                              List<Long> deleteImageIds, MultipartFile[] files, Model model,
                              Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.error("Обновление товара: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostText post = optionalPost.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") && !post.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка обновления чужого товара");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                // Удаляем из избранного (помечаем на пересчет)
                favorRepo.deleteByPostId(post.getId());
                processImagesForMarking(post.getImages().size(), deleteImageIds, files, post, null, null, null, model);

                post.setTitle(title);
                post.setPrice(price);
                post.setFull_text(full_text);
                post.setCategory(category);
                post.setLocation(location);
                post.resetForModeration();
                post.setPremium(false);
                post.setPremiumPriority(0);
                postRepo.save(post);
                log.info("Товар успешно обновлен, отправлен на модерацию");

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.error("Обновление услуги: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                PostTextJob post = optionalPost.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") && !post.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка обновления чужой услуги");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                favorRepo.deleteByPostJobId(post.getId());
                processImagesForMarking(post.getImages().size(), deleteImageIds, files, null, post, null, null, model);

                post.setTitle(title);
                post.setPrice(price);
                post.setFull_text(full_text);
                post.setCategory(category);
                post.setLocation(location);
                post.resetForModeration();
                post.setPremium(false);
                post.setPremiumPriority(0);
                postRepoJob.save(post);
                log.info("Услуга успешно обновлена, отправлена на модерацию");
            }

            // Очистка всех кэшей модерации
            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Кэши модерации очищены после обновления поста");

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при обновлении поста типа {}: {}", type, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Общий метод для обновления вакансий.
     */
    private String updatePostVacancy(String hashId, String title, Integer cash, String full_text, String category, String experience, String workformat,
                                     String location,
                                     List<Long> deleteImageIds, MultipartFile[] files, Model model,
                                     Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVac = postVacancyRepository.findByHashId(hashId);
                if (optionalVac.isEmpty()) {
                    log.error("Обновление вакансии: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Vacancy vaca = optionalVac.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") && !vaca.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка обновления чужой вакансии");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                favorRepo.deleteByVacancyId(vaca.getId());
                processImagesForMarking(vaca.getImages().size(), deleteImageIds, files, null, null, vaca, null, model);

                vaca.setTitle(title);
                vaca.setCash(cash);
                vaca.setFull_text(full_text);
                vaca.setCategory(category);
                vaca.setExperience(experience);
                vaca.setWorkformat(workformat);
                vaca.setLocation(location);
                vaca.resetForModeration();
                vaca.setPremium(false);
                vaca.setPremiumPriority(0);
                postVacancyRepository.save(vaca);
                log.info("Вакансия успешно обновлена, отправлена на модерацию");
            }

            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Кэши модерации очищены после обновления вакансии");

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при обновлении вакансии: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Общий метод для обновления резюме.
     */
    private String updatePostResume(String hashId, String title, Integer cash, String full_text, String category,
                                    String experience, String workformat, List<Long> deleteImageIds, MultipartFile[] files, Model model,
                                    Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findByHashId(hashId);
                if (optionalResume.isEmpty()) {
                    log.error("Обновление резюме: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }

                Resume res = optionalResume.get();

                if (!currentUser.getRole().equals("ROLE_SUPER") && !res.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка обновления чужого резюме");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }

                favorRepo.deleteByResumeId(res.getId());
                processImagesForMarking(res.getImages().size(), deleteImageIds, files, null, null, null, res, model);

                res.setTitle(title);
                res.setCash(cash);
                res.setFull_text(full_text);
                res.setCategory(category);
                res.setExperience(experience);
                res.setWorkformat(workformat);
                res.resetForModeration();
                postResumeRepository.save(res);
                log.info("Резюме успешно обновлено, отправлено на модерацию");
            }

            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Кэши модерации очищены после обновления резюме");

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при обновлении резюме: {}", e.getMessage(), e);
            model.addAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "error";
        }
    }

    /**
     * Обрабатывает изображения при редактировании поста:
     * - снимает отметки на удаление с изображений, которые были сняты пользователем
     * - отмечает на удаление выбранные изображения
     * - добавляет новые изображения с проверкой лимита в 10 изображений
     *
     * @param currentImageCount текущее количество изображений
     * @param deleteImageIds список ID изображений для удаления
     * @param files новые файлы
     * @param post пост-товар
     * @param postJob пост-услуга
     * @param vacancy вакансия
     * @param resume резюме
     * @param model модель для ошибок
     * @throws IOException при ошибках работы с файлами
     */
    private void processImagesForMarking(int currentImageCount, List<Long> deleteImageIds, MultipartFile[] files,
                                         PostText post, PostTextJob postJob, Vacancy vacancy,
                                         Resume resume, Model model) throws IOException {

        // ========== СЕРВЕРНАЯ ПРОВЕРКА РАЗМЕРА НОВЫХ ФАЙЛОВ ==========
        if (files != null && files.length > 0) {
            imageService.validateFilesSize(files, 20, 20);
        }
        // ===============================================================

        // Получаем ВСЕ изображения, принадлежащие этому посту
        List<ImagesFoto> allImages;
        if (post != null) {
            allImages = imageRepository.findByPost(post);
        } else if (postJob != null) {
            allImages = imageRepository.findByPostjob(postJob);
        } else if (vacancy != null) {
            allImages = imageRepository.findByVacancy(vacancy);
        } else if (resume != null) {
            allImages = imageRepository.findByResume(resume);
        } else {
            log.warn("processImagesForMarking: все типы постов равны null");
            return;
        }

        // 1. Снимаем отметки на удаление со всех изображений, которые НЕ в списке deleteImageIds
        List<Long> imagesToUnmark = new ArrayList<>();
        for (ImagesFoto image : allImages) {
            if (image.isMarkedForDeletion() && (deleteImageIds == null || !deleteImageIds.contains(image.getId()))) {
                imagesToUnmark.add(image.getId());
            }
        }

        if (!imagesToUnmark.isEmpty()) {
            imageService.unmarkImagesForDeletion(imagesToUnmark);
            log.debug("Снята отметка на удаление с {} изображений", imagesToUnmark.size());
        }

        // 2. Отмечаем на удаление выбранные изображения (те, что в новом списке)
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<Long> imagesToMark = new ArrayList<>();
            for (Long id : deleteImageIds) {
                Optional<ImagesFoto> optImage = imageRepository.findById(id);
                if (optImage.isPresent() && !optImage.get().isMarkedForDeletion()) {
                    imagesToMark.add(id);
                }
            }

            if (!imagesToMark.isEmpty()) {
                imageService.markImagesForDeletion(imagesToMark);
                log.debug("Отмечено на удаление {} изображений", imagesToMark.size());
            }
        }

        // 3. Добавляем новые изображения с проверкой лимита (максимум 5)
        if (files != null && files.length > 0) {
            int currentActiveCount = currentImageCount;
            if (deleteImageIds != null) {
                currentActiveCount -= deleteImageIds.size();
            }

            if (currentActiveCount + files.length > 5) {
                log.warn("Превышение лимита изображений: текущее активное {}, пытаемся добавить {}",
                        currentActiveCount, files.length);
                throw new IllegalArgumentException("Нельзя загрузить более 5 изображений. Текущее активное количество: " + currentActiveCount);
            }

            imageService.saveImages(files, post, postJob, vacancy, resume);
            log.debug("Добавлено {} новых изображений", files.length);
        }
    }

    /**
     * Устаревший метод обработки изображений (без отметок на удаление).
     * Оставлен для обратной совместимости, но не используется в основном потоке.
     */
    private void processImages(int currentImageCount, List<Long> deleteImageIds, MultipartFile[] files,
                               PostText post, PostTextJob postJob, Vacancy vacancy,
                               Resume resume, Model model) throws IOException {
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            imageService.deleteImagesByIds(deleteImageIds);
            log.debug("Удалено {} изображений", deleteImageIds.size());
        }

        if (files != null && files.length > 0) {
            int remainingCount = currentImageCount - (deleteImageIds != null ? deleteImageIds.size() : 0);
            if (remainingCount + files.length > 10) {
                log.warn("Превышение лимита изображений: {}, попытка добавить {}", remainingCount, files.length);
                throw new IllegalArgumentException("Нельзя загрузить более 10 изображений. Текущее количество: " + remainingCount);
            }
            imageService.saveImages(files, post, postJob, vacancy, resume);
        }
    }

    /**
     * Обрабатывает удаление товара.
     */
    @PostMapping("/page_two/redactor/{hashId}/remove")
    public String redactorDetailsPostRemove(@PathVariable(value = "hashId") String hashId, Model model,
                                            Authentication authentication) {
        return deletePost(hashId, model, authentication, "product");
    }

    /**
     * Обрабатывает удаление услуги.
     */
    @PostMapping("/page_two/redactor/job/{hashId}/remove")
    public String redactorJobDetailsPostRemove(@PathVariable(value = "hashId") String hashId, Model model,
                                               Authentication authentication) {
        return deletePost(hashId, model, authentication, "service");
    }

    /**
     * Обрабатывает удаление вакансии.
     */
    @PostMapping("/page_two/redactor/vac/{hashId}/remove")
    public String redactorVacancyDetailsPostRemove(@PathVariable(value = "hashId") String hashId, Model model,
                                                   Authentication authentication) {
        return deletePost(hashId, model, authentication, "vacancy");
    }

    /**
     * Обрабатывает удаление резюме.
     */
    @PostMapping("/page_two/redactor/res/{hashId}/remove")
    public String redactorResumeDetailsPostRemove(@PathVariable(value = "hashId") String hashId, Model model,
                                                  Authentication authentication) {
        return deletePost(hashId, model, authentication, "resume");
    }

    /**
     * Общий метод для удаления поста любого типа.
     * Перед удалением удаляет связанные записи из избранного и просмотров.
     *
     * @param hashId идентификатор
     * @param model модель
     * @param authentication аутентификация
     * @param type тип поста
     * @return редирект на страницу редактора
     */
    private String deletePost(String hashId, Model model, Authentication authentication, String type) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            boolean isSuper = "ROLE_SUPER".equals(currentUser.getRole());

            if ("product".equals(type)) {
                Optional<PostText> optionalPost = postRepo.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.error("Удаление товара: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostText post = optionalPost.get();
                if (!isSuper && !post.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка удаления чужого товара");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getPost() != null && f.getPost().getId().equals(post.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);
                postViewRepository.deleteByPost(post);
                postRepo.delete(post);
                log.info("Товар успешно удален");

            } else if ("service".equals(type)) {
                Optional<PostTextJob> optionalPost = postRepoJob.findByHashId(hashId);
                if (optionalPost.isEmpty()) {
                    log.error("Удаление услуги: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                PostTextJob post = optionalPost.get();
                if (!isSuper && !post.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка удаления чужой услуги");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getPostJob() != null && f.getPostJob().getId().equals(post.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);
                postViewRepository.deleteByPostJob(post);
                postRepoJob.delete(post);
                log.info("Услуга успешно удалена");

            } else if ("vacancy".equals(type)) {
                Optional<Vacancy> optionalVacancy = postVacancyRepository.findByHashId(hashId);
                if (optionalVacancy.isEmpty()) {
                    log.error("Удаление вакансии: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Vacancy vacvac = optionalVacancy.get();
                if (!isSuper && !vacvac.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка удаления чужой вакансии");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getVacancy() != null && f.getVacancy().getId().equals(vacvac.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);
                postViewRepository.deleteByVacancy(vacvac);
                postVacancyRepository.delete(vacvac);
                log.info("Вакансия успешно удалена");

            } else if ("resume".equals(type)) {
                Optional<Resume> optionalResume = postResumeRepository.findByHashId(hashId);
                if (optionalResume.isEmpty()) {
                    log.error("Удаление резюме: запись с hashId {} не найдена", hashId);
                    model.addAttribute("error", "Запись не найдена");
                    return "error";
                }
                Resume res = optionalResume.get();
                if (!isSuper && !res.getUser().getId().equals(currentUser.getId())) {
                    log.warn("Попытка удаления чужого резюме");
                    model.addAttribute("error", "Доступ запрещен: это не ваш пост");
                    return "error";
                }
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getResume() != null && f.getResume().getId().equals(res.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);
                postViewRepository.deleteByResume(res);
                postResumeRepository.delete(res);
                log.info("Резюме успешно удалено");
            }

            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Кэши модерации очищены после удаления поста");

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.error("Ошибка при удалении поста типа {}: {}", type, e.getMessage(), e);
            model.addAttribute("error", "Ошибка при удалении: " + e.getMessage());
            return "error";
        }
    }
}