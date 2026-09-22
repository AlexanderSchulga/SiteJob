package jobforsite.sitejob.module.shared.controller.redactmoderat;

import jakarta.servlet.http.HttpSession;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.MotorVehiclesPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.RealEstatePostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.product.service.PostTypeResolver;
import jobforsite.sitejob.module.product.service.ProductService;
import jobforsite.sitejob.module.product.service.filter.ExtendsUpdateProductService;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.config.categoryconfig.*;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.dto.product.PostUpdateRequestDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

@Slf4j
@RequiredArgsConstructor
@Controller
public class RedactorController extends BaseController {

    // Репозитории для работы с различными типами постов
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

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

    private final FilterConfigService filterConfigService;

    private final PostTypeResolver postTypeResolver;

    private final ExtendsUpdateProductService extendsUpdateProductService;

    private final ProductService productService;


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
            log.warn("Ошибка загрузки страницы редактора");
            return "index";
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

        // загружаем пост с правильным типом
        PostText post = postTypeResolver.loadPostByHashId(hashId);

        if (post == null) {
            return "redirect:/index";
        }

        User currentUser = (User) authentication.getPrincipal();

        // Проверка прав доступа
        if (!currentUser.getRole().equals("ROLE_SUPER") &&
                !post.getUser().getId().equals(currentUser.getId())) {
            log.warn("Попытка редактирования чужого товара");
            model.addAttribute("error", "Доступ запрещен: это не ваш пост");
            return "error";
        }

        ArrayList<PostText> res = new ArrayList<>();
        res.add(post);
        model.addAttribute("postwo", res);
        model.addAttribute("title", "redactor");
        model.addAttribute("type", "product");
        model.addAttribute("categories", postTextCategoryConfig.getCategories());
        model.addAttribute("isSuper", currentUser.getRole().equals("ROLE_SUPER"));

        model.addAttribute("filterService", filterConfigService);

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
     * Обновление товара (пользователь)
     * Фото должны ТОЛЬКО помечаться на удаление, физическое удаление после модерации
     */
    @PostMapping("/page_two/redactor/{hashId}/page_two/edit")
    public String redactorDetailsPostUpdate(@PathVariable("hashId") String postHashId,
                                            @ModelAttribute PostUpdateRequestDTO request,
                                            Authentication authentication,
                                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            PostText post = postTypeResolver.loadPostByHashId(postHashId);

            if (post == null) {
                throw new IllegalArgumentException("Пост не найден");
            }

            if (!currentUser.getRole().equals("ROLE_SUPER") && !post.getUser().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Доступ запрещен");
                return "redirect:/page_two/redactor";
            }

            // Удаляем из избранного
            favorRepo.deleteByPostId(post.getId());

            // Нормализуем files (убираем пустые)
            MultipartFile[] files = request.getFiles();
            if (files != null) {
                files = java.util.Arrays.stream(files)
                        .filter(f -> f != null && !f.isEmpty())
                        .toArray(MultipartFile[]::new);
                request.setFiles(files.length > 0 ? files : null);
            }

            // Обработка изображений происходит ВНУТРИ сервиса (isAdminRequest=false)
            PostText updatedPost;

            if (post instanceof RealEstatePostText || post instanceof MotorVehiclesPostText) {
                updatedPost = productService.updatePostFromDTO(
                        post.getId(), currentUser, request, request.getFiles(), false
                );
            } else {
                updatedPost = extendsUpdateProductService.updateExtendsPostFromDTO(
                        post.getId(), currentUser, request, request.getFiles(), false
                );
            }

            // Сбрасываем на модерацию
            updatedPost.resetForModeration();
            updatedPost.setPremium(false);
            updatedPost.setPremiumPriority(0);

            postRepo.save(updatedPost);

            moderationPostsCacheService.clearAllModerationCaches();

            redirectAttributes.addFlashAttribute("success", "Объявление успешно обновлено и отправлено на модерацию");
            return "redirect:/page_two/redactor";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при обновлении товара: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/page_two/redactor/" + postHashId + "/page_two/edit";
        } catch (Exception e) {
            log.warn("Ошибка при обновлении товара: ", e);
            redirectAttributes.addFlashAttribute("error", "Произошла ошибка при обновлении. Попробуйте позже.");
            return "redirect:/page_two/redactor/" + postHashId + "/page_two/edit";
        }
    }


    /**
     * Обработка изображений для товара (пользователь)
     * - Помечает на удаление выбранные
     * - Снимает пометку с тех, что больше не выбраны
     * - Добавляет новые
     * НЕ УДАЛЯЕТ ФИЗИЧЕСКИ!
     */
    private void processImagesForProduct(PostText post, List<Long> deleteImageIds, MultipartFile[] files) throws IOException {
        List<ImagesFoto> allImages = imageRepository.findByPost(post);

        // 1. Снимаем пометку с изображений, которые НЕ в списке удаления
        List<Long> imagesToUnmark = allImages.stream()
                .filter(img -> img.isMarkedForDeletion() && (deleteImageIds == null || !deleteImageIds.contains(img.getId())))
                .map(ImagesFoto::getId)
                .collect(Collectors.toList());

        if (!imagesToUnmark.isEmpty()) {
            imageService.unmarkImagesForDeletion(imagesToUnmark);
        }

        // 2. Помечаем на удаление выбранные изображения
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<Long> imagesToMark = deleteImageIds.stream()
                    .filter(id -> {
                        Optional<ImagesFoto> opt = imageRepository.findById(id);
                        return opt.isPresent() && !opt.get().isMarkedForDeletion();
                    })
                    .collect(Collectors.toList());

            if (!imagesToMark.isEmpty()) {
                imageService.markImagesForDeletion(imagesToMark);
            }
        }

        // 3. Добавляем новые изображения с проверкой лимита
        MultipartFile[] nonEmptyFiles = files == null ? null :
                java.util.Arrays.stream(files)
                        .filter(f -> f != null && !f.isEmpty())
                        .toArray(MultipartFile[]::new);

        if (nonEmptyFiles != null && nonEmptyFiles.length > 0) {
            imageService.validateFilesSize(nonEmptyFiles, 20, 20);

            int currentActiveCount = (int) allImages.stream()
                    .filter(img -> !img.isMarkedForDeletion())
                    .count();

            if (currentActiveCount + nonEmptyFiles.length > 5) {
                throw new IllegalArgumentException("Нельзя загрузить более 5 изображений");
            }

            imageService.saveImages(nonEmptyFiles, post, null, null, null);
        }
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
                                               @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                                               @RequestParam(value = "files", required = false) MultipartFile[] files,
                                               Model model,
                                               Authentication authentication,
                                               RedirectAttributes redirectAttributes) {
        return updatePostService(hashId, title, price, full_text, category, location, deleteImageIds, files, model, authentication, "service", redirectAttributes);
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
                                                   @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                                                   @RequestParam(value = "files", required = false) MultipartFile[] files,
                                                   Model model,
                                                   Authentication authentication,
                                                   RedirectAttributes redirectAttributes) {
        return updatePostVacancy(hashId, title, cash, full_text, category, experience, workformat, location, deleteImageIds, files, model, authentication, "vacancy", redirectAttributes);
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
                                                  Authentication authentication,
                                                  RedirectAttributes redirectAttributes) {
        return updatePostResume(hashId, title, cash, full_text, category, experience, workformat, deleteImageIds, files, model, authentication, "resume", redirectAttributes);
    }

    /**
     * Обновление услуги (пользователь)
     * Фото должны помечаться на удаление (как в резюме)
     */
    private String updatePostService(String hashId, String title, Integer price, String full_text,
                                     String category, String location,
                                     List<Long> deleteImageIds, MultipartFile[] files, Model model,
                                     Authentication authentication, String type,
                                     RedirectAttributes redirectAttributes) {
        try {
            User currentUser = (User) authentication.getPrincipal();

            if ("service".equals(type)) {
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

                processImagesForService(post, deleteImageIds, files);

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

            moderationPostsCacheService.clearAllModerationCaches();
            return "redirect:/page_two/redactor";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при обновлении услуги: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/page_two/redactor/job/" + hashId + "/page_two/edit";
        } catch (Exception e) {
            log.error("Ошибка при обновлении услуги: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/page_two/redactor/job/" + hashId + "/page_two/edit";
        }
    }

    /**
     * Обработка изображений для услуги (пользователь)
     * - Помечает на удаление выбранные
     * - Снимает пометку с тех, что больше не выбраны
     * - Добавляет новые
     * НЕ УДАЛЯЕТ ФИЗИЧЕСКИ!
     */
    private void processImagesForService(PostTextJob post, List<Long> deleteImageIds, MultipartFile[] files) throws IOException {
        List<ImagesFoto> allImages = imageRepository.findByPostjob(post);

        List<Long> imagesToUnmark = allImages.stream()
                .filter(img -> img.isMarkedForDeletion() && (deleteImageIds == null || !deleteImageIds.contains(img.getId())))
                .map(ImagesFoto::getId)
                .collect(Collectors.toList());

        if (!imagesToUnmark.isEmpty()) {
            imageService.unmarkImagesForDeletion(imagesToUnmark);
        }

        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<Long> imagesToMark = deleteImageIds.stream()
                    .filter(id -> {
                        Optional<ImagesFoto> opt = imageRepository.findById(id);
                        return opt.isPresent() && !opt.get().isMarkedForDeletion();
                    })
                    .collect(Collectors.toList());

            if (!imagesToMark.isEmpty()) {
                imageService.markImagesForDeletion(imagesToMark);
            }
        }

        MultipartFile[] nonEmptyFiles = files == null ? null :
                java.util.Arrays.stream(files)
                        .filter(f -> f != null && !f.isEmpty())
                        .toArray(MultipartFile[]::new);

        if (nonEmptyFiles != null && nonEmptyFiles.length > 0) {
            imageService.validateFilesSize(nonEmptyFiles, 20, 20);

            int currentActiveCount = (int) allImages.stream()
                    .filter(img -> !img.isMarkedForDeletion())
                    .count();

            if (currentActiveCount + nonEmptyFiles.length > 5) {
                throw new IllegalArgumentException("Нельзя загрузить более 5 изображений");
            }

            imageService.saveImages(nonEmptyFiles, null, post, null, null);
        }
    }


    /**
     * Обновление вакансии (пользователь)
     * Фото должны помечаться на удаление (как в резюме)
     */
    private String updatePostVacancy(String hashId, String title, Integer cash, String full_text,
                                     String category, String experience, String workformat,
                                     String location,
                                     List<Long> deleteImageIds, MultipartFile[] files, Model model,
                                     Authentication authentication, String type,
                                     RedirectAttributes redirectAttributes) {
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

                processImagesForVacancy(vaca, deleteImageIds, files);

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
            return "redirect:/page_two/redactor";

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при обновлении вакансии: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/page_two/redactor/vac/" + hashId + "/page_two/edit";
        } catch (Exception e) {
            log.error("Ошибка при обновлении вакансии: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/page_two/redactor/vac/" + hashId + "/page_two/edit";
        }
    }

    /**
     * Обработка изображений для вакансии (пользователь)
     * - Помечает на удаление выбранные
     * - Снимает пометку с тех, что больше не выбраны
     * - Добавляет новые
     * НЕ УДАЛЯЕТ ФИЗИЧЕСКИ!
     */
    private void processImagesForVacancy(Vacancy vacancy, List<Long> deleteImageIds, MultipartFile[] files) throws IOException {
        List<ImagesFoto> allImages = imageRepository.findByVacancy(vacancy);

        List<Long> imagesToUnmark = allImages.stream()
                .filter(img -> img.isMarkedForDeletion() && (deleteImageIds == null || !deleteImageIds.contains(img.getId())))
                .map(ImagesFoto::getId)
                .collect(Collectors.toList());

        if (!imagesToUnmark.isEmpty()) {
            imageService.unmarkImagesForDeletion(imagesToUnmark);
        }

        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<Long> imagesToMark = deleteImageIds.stream()
                    .filter(id -> {
                        Optional<ImagesFoto> opt = imageRepository.findById(id);
                        return opt.isPresent() && !opt.get().isMarkedForDeletion();
                    })
                    .collect(Collectors.toList());

            if (!imagesToMark.isEmpty()) {
                imageService.markImagesForDeletion(imagesToMark);
            }
        }

        MultipartFile[] nonEmptyFiles = files == null ? null :
                java.util.Arrays.stream(files)
                        .filter(f -> f != null && !f.isEmpty())
                        .toArray(MultipartFile[]::new);

        if (nonEmptyFiles != null && nonEmptyFiles.length > 0) {
            imageService.validateFilesSize(nonEmptyFiles, 20, 20);

            int currentActiveCount = (int) allImages.stream()
                    .filter(img -> !img.isMarkedForDeletion())
                    .count();

            if (currentActiveCount + nonEmptyFiles.length > 5) {
                throw new IllegalArgumentException("Нельзя загрузить более 5 изображений");
            }

            imageService.saveImages(nonEmptyFiles, null, null, vacancy, null);
        }
    }

    /**
     * Общий метод для обновления резюме.
     */
    private String updatePostResume(String hashId, String title, Integer cash, String full_text, String category,
                                    String experience, String workformat, List<Long> deleteImageIds, MultipartFile[] files, Model model,
                                    Authentication authentication, String type,
                                    RedirectAttributes redirectAttributes) {
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

                MultipartFile[] nonEmptyFiles = files == null ? null :
                        java.util.Arrays.stream(files)
                                .filter(f -> f != null && !f.isEmpty())
                                .toArray(MultipartFile[]::new);

                processImagesForMarking(res.getImages().size(), deleteImageIds, nonEmptyFiles, null, null, null, res, model);

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

        } catch (IllegalArgumentException e) {
            log.warn("Ошибка валидации при обновлении резюме: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/page_two/redactor/res/" + hashId + "/page_two/edit_res";
        } catch (Exception e) {
            log.error("Ошибка при обновлении резюме: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/page_two/redactor/res/" + hashId + "/page_two/edit_res";
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

        if (files != null && files.length > 0) {
            imageService.validateFilesSize(files, 20, 20);
        }

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
            if (remainingCount + files.length > 5) {
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
     * Перед удалением удаляет связанные записи из избранного, просмотров и все изображения.
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

                // Удаляем из избранного
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getPost() != null && f.getPost().getId().equals(post.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                // Удаляем просмотры
                postViewRepository.deleteByPost(post);

                // Удаляем все изображения поста (физически и из БД)
                imageService.deletePostImages(post);

                // Удаляем пост
                postRepo.delete(post);
                log.info("Товар успешно удален вместе с изображениями");

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

                // Удаляем из избранного
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getPostJob() != null && f.getPostJob().getId().equals(post.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                // Удаляем просмотры
                postViewRepository.deleteByPostJob(post);

                // Удаляем все изображения услуги (физически и из БД)
                imageService.deletePostJobImages(post);

                // Удаляем услугу
                postRepoJob.delete(post);
                log.info("Услуга успешно удалена вместе с изображениями");

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

                // Удаляем из избранного
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getVacancy() != null && f.getVacancy().getId().equals(vacvac.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                // Удаляем просмотры
                postViewRepository.deleteByVacancy(vacvac);

                // Удаляем все изображения вакансии (физически и из БД)
                imageService.deleteVacancyImages(vacvac);

                // Удаляем вакансию
                postVacancyRepository.delete(vacvac);
                log.info("Вакансия успешно удалена вместе с изображениями");

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

                // Удаляем из избранного
                List<Favorites> favorites = favorRepo.findAll().stream()
                        .filter(f -> f.getResume() != null && f.getResume().getId().equals(res.getId()))
                        .collect(Collectors.toList());
                favorRepo.deleteAll(favorites);

                // Удаляем просмотры
                postViewRepository.deleteByResume(res);

                // Удаляем все изображения резюме (физически и из БД)
                imageService.deleteResumeImages(res);

                // Удаляем резюме
                postResumeRepository.delete(res);
                log.info("Резюме успешно удалено вместе с изображениями");
            }

            moderationPostsCacheService.clearAllModerationCaches();
            log.debug("Кэши модерации очищены после удаления поста");

            return "redirect:/page_two/redactor";

        } catch (Exception e) {
            log.warn("Ошибка при удалении поста типа: {}", type);
            model.addAttribute("error", "Ошибка при удалении");
            return "error";
        }
    }
}