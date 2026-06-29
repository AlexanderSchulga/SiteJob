package jobforsite.sitejob.module.users.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.model.Reviews;
import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.shared.repository.ReviewsRepo;
import jobforsite.sitejob.module.shared.repository.SupportCommentRepo;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер для управления личным кабинетом пользователя.
 * <p>
 * Обеспечивает отображение формы редактирования профиля, обновление данных
 * пользователя (включая логин, имя, email, телефон, пароль), а также удаление
 * аккаунта с каскадной обработкой связанных сущностей (избранное, отзывы,
 * комментарии поддержки).
 * </p>
 */
@RequiredArgsConstructor
@Controller
public class UpdateUserMe extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserMe.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupportCommentRepo supportCommentRepo;
    private final ReviewsRepo reviewsRepo;
    private final FavorRepo favorRepo;

    /**
     * Отображает страницу личного кабинета пользователя для редактирования.
     *
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации текущего пользователя
     * @return имя шаблона для отображения или редирект в случае ошибки
     */
    @GetMapping("/page_two/user_edit")
    public String userEdit(Model model, Authentication authentication) {
        try {
            if (authentication == null) {
                log.warn("Попытка доступа к редактированию профиля без аутентификации");
                return "redirect:/login";
            }

            String username = authentication.getName();
            Optional<User> user = userRepository.findByUsername(username);

            if (user.isEmpty()) {
                log.warn("Пользователь не найден при попытке редактирования профиля");
                model.addAttribute("error", "Пользователь не найден");
                return "redirect:/index";
            }

            ArrayList<User> result = new ArrayList<>();
            user.ifPresent(result::add);

            model.addAttribute("user", result);
            model.addAttribute("title", "Личный кабинет");

            log.debug("Страница редактирования профиля успешно загружена");
            return "page_two/user_edit";

        } catch (Exception e) {
            log.error("Ошибка при загрузке данных профиля: ", e);
            model.addAttribute("error", "Ошибка при загрузке данных: " + e.getMessage());
            return "redirect:/index";
        }
    }

    /**
     * Обновляет данные пользователя в личном кабинете.
     * <p>
     * Поддерживает изменение логина и имени только для пользователей с ролью SUPER_ADMIN.
     * Email, телефон, телефон для объявлений и описание могут изменять все пользователи.
     * Смена пароля возможна при корректном вводе текущего и совпадении новых паролей.
     * </p>
     *
     * @param username           новый логин
     * @param nameuse            новое имя
     * @param email              новый email
     * @param phone              новый телефон
     * @param phonePost          телефон для объявлений (опционально)
     * @param currentPassword    текущий пароль (для смены пароля)
     * @param newPassword        новый пароль
     * @param confirmPassword    подтверждение нового пароля
     * @param descript           описание профиля
     * @param redirectAttributes атрибуты для flash-сообщений
     * @param authentication     объект аутентификации текущего пользователя
     * @return редирект на страницу редактирования профиля
     */
    @PostMapping("/page_two/user_edit/update")
    public String updateUserMe(@RequestParam String username,
                               @RequestParam String nameuse,
                               @RequestParam String email,
                               @RequestParam String phone,
                               @RequestParam(required = false) String phonePost,
                               @RequestParam(required = false) String currentPassword,
                               @RequestParam(required = false) String newPassword,
                               @RequestParam(required = false) String confirmPassword,
                               @RequestParam(required = false) String descript,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        try {
            if (authentication == null) {
                log.warn("Попытка обновления профиля без аутентификации");
                return "redirect:/login";
            }

            String currentUsername = authentication.getName();
            Optional<User> optionalUser = userRepository.findByUsername(currentUsername);

            if (optionalUser.isEmpty()) {
                log.warn("Пользователь не найден при обновлении профиля");
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/page_two/user_edit";
            }

            User user = optionalUser.get();

            // Проверка роли SUPER_ADMIN
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_SUPER"));

            // --- Изменение логина (только для SUPER_ADMIN) ---
            if (!user.getUsername().equals(username)) {
                if (!isSuperAdmin) {
                    log.warn("Попытка изменения логина без прав SUPER_ADMIN");
                    redirectAttributes.addFlashAttribute("error", "Изменение логина доступно только для администраторов");
                    return "redirect:/page_two/user_edit";
                }

                Optional<User> existingUser = userRepository.findByUsername(username);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    log.warn("Попытка установить уже занятый логин");
                    redirectAttributes.addFlashAttribute("error", "Логин уже занят");
                    return "redirect:/page_two/user_edit";
                }
                user.setUsername(username);
                log.debug("Логин успешно изменен");
            }

            // --- Изменение имени (только для SUPER_ADMIN) ---
            if (!user.getNameuse().equals(nameuse)) {
                if (!isSuperAdmin) {
                    log.warn("Попытка изменения имени без прав SUPER_ADMIN");
                    redirectAttributes.addFlashAttribute("error", "Изменение имени доступно только для администраторов");
                    return "redirect:/page_two/user_edit";
                }

                Optional<User> existingUser = userRepository.findByNameuse(nameuse);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    log.warn("Попытка установить уже занятое имя");
                    redirectAttributes.addFlashAttribute("error", "Имя уже занято");
                    return "redirect:/page_two/user_edit";
                }
                user.setNameuse(nameuse);
                log.debug("Имя успешно изменено");
            }

            // --- Изменение email (доступно всем) ---
            if (!user.getEmail().equals(email)) {
                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    log.warn("Попытка установить уже занятый email");
                    redirectAttributes.addFlashAttribute("error", "Email уже занят");
                    return "redirect:/page_two/user_edit";
                }
                user.setEmail(email);
                log.debug("Email успешно изменен");
            }

            // --- Обновление описания ---
            user.setDescript(descript);

            // --- Изменение телефона (доступно всем) ---
            if (!user.getPhone().equals(phone)) {
                Optional<User> existingUser = userRepository.findByPhone(phone);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    log.warn("Попытка установить уже занятый телефон");
                    redirectAttributes.addFlashAttribute("error", "Телефон уже занят");
                    return "redirect:/page_two/user_edit";
                }
                user.setPhone(phone);
                log.debug("Телефон успешно изменен");
            }

            // --- Обновление телефона для объявлений ---
            user.setPhonePost(phonePost);

            // --- Смена пароля (доступно всем, если заполнены все поля) ---
            if (currentPassword != null && !currentPassword.isEmpty() &&
                    newPassword != null && !newPassword.isEmpty() &&
                    confirmPassword != null && !confirmPassword.isEmpty()) {

                if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                    log.warn("Неверный текущий пароль при попытке смены пароля");
                    redirectAttributes.addFlashAttribute("error", "Текущий пароль неверен");
                    return "redirect:/page_two/user_edit";
                }

                if (!newPassword.equals(confirmPassword)) {
                    log.warn("Новые пароли не совпадают при попытке смены пароля");
                    redirectAttributes.addFlashAttribute("error", "Новые пароли не совпадают");
                    return "redirect:/page_two/user_edit";
                }

                if (newPassword.length() < 6) {
                    log.warn("Новый пароль короче 6 символов");
                    redirectAttributes.addFlashAttribute("error", "Пароль должен содержать минимум 6 символов");
                    return "redirect:/page_two/user_edit";
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                log.debug("Пароль успешно изменен");
            }

            userRepository.save(user);
            log.info("Профиль пользователя успешно обновлен");

            redirectAttributes.addFlashAttribute("success", "Данные успешно обновлены");
            return "redirect:/page_two/user_edit?success";

        } catch (Exception e) {
            log.error("Ошибка при обновлении данных профиля: ", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении данных: " + e.getMessage());
            return "redirect:/page_two/user_edit";
        }
    }

    /**
     * Удаляет аккаунт текущего пользователя.
     * <p>
     * Перед удалением проверяет пароль, затем каскадно обрабатывает:
     * <ul>
     *     <li>Избранное (записи пользователя и записи, где посты пользователя добавлены в избранное)</li>
     *     <li>Отзывы (помечает автора или рецензируемого как удаленного)</li>
     *     <li>Комментарии поддержки (удаляет комментарии пользователя и отвязывает ответы)</li>
     * </ul>
     * После удаления инвалидирует сессию и очищает контекст безопасности.
     * </p>
     *
     * @param confirmPassword пароль для подтверждения удаления
     * @param model           модель для передачи сообщений
     * @param authentication  объект аутентификации текущего пользователя
     * @param request         HTTP-запрос для получения сессии
     * @return редирект на страницу входа с сообщением об удалении
     */
    @PostMapping("index/delete")
    @Transactional
    public String deleteUserAccount(
            @RequestParam String confirmPassword,
            Model model,
            Authentication authentication,
            HttpServletRequest request
    ) {
        try {
            if (authentication == null) {
                log.warn("Попытка удаления аккаунта без аутентификации");
                return "redirect:/login";
            }

            String currentUsername = authentication.getName();
            User currentUser = userRepository.findByUsername(currentUsername)
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

            // Проверка пароля
            if (!passwordEncoder.matches(confirmPassword, currentUser.getPassword())) {
                log.warn("Неверный пароль при попытке удаления аккаунта");
                model.addAttribute("error", "Неверный пароль для подтверждения удаления");
                model.addAttribute("title", "Biranaku");

                // Возвращаем главную страницу с сообщением об ошибке
                return "index";  // Или "page/index" в зависимости от настроек ViewResolver
            }

            log.info("Начало процесса удаления аккаунта");

            // Удаление связанных данных
            handleFavorites(currentUser);
            handleUserReviews(currentUser);
            handleSupportComments(currentUser);

            // Удаление пользователя
            userRepository.delete(currentUser);
            log.info("Пользователь успешно удален из базы данных");

            // Инвалидация сессии
            if (request != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                    log.debug("Сессия пользователя инвалидирована");
                }
            }

            SecurityContextHolder.clearContext();
            log.info("Процесс удаления аккаунта завершен успешно");

            return "redirect:/login?accountDeleted";

        } catch (Exception e) {
            log.error("Ошибка при удалении аккаунта: ", e);
            model.addAttribute("error", "Ошибка при удалении аккаунта: " + e.getMessage());
            return "redirect:index";
        }
    }

    /**
     * Обрабатывает удаление всех записей из избранного, связанных с пользователем.
     * <p>
     * Удаляет:
     * <ul>
     *     <li>Записи, где пользователь добавил что-то в избранное</li>
     *     <li>Записи, где посты пользователя добавлены в избранное другими (обычные посты, услуги, вакансии, резюме)</li>
     * </ul>
     * </p>
     *
     * @param user пользователь, чьи данные об избранном удаляются
     */
    private void handleFavorites(User user) {
        log.debug("Обработка избранного пользователя");

        // Избранное, добавленное пользователем
        List<Favorites> userFavorites = favorRepo.findByUser(user);
        favorRepo.deleteAll(userFavorites);

        // Избранное, где фигурируют посты пользователя
        List<Favorites> favoritesWithUserRegularPosts = favorRepo.findByPostAuthor(user);
        favorRepo.deleteAll(favoritesWithUserRegularPosts);

        List<Favorites> favoritesWithUserJobPosts = favorRepo.findByPostJobAuthor(user);
        favorRepo.deleteAll(favoritesWithUserJobPosts);

        List<Favorites> favoritesWithUserVacPosts = favorRepo.findByVacancyAuthor(user);
        favorRepo.deleteAll(favoritesWithUserVacPosts);

        List<Favorites> favoritesWithUserResPosts = favorRepo.findByResumeAuthor(user);
        favorRepo.deleteAll(favoritesWithUserResPosts);

        log.debug("Обработка избранного завершена");
    }

    /**
     * Обрабатывает отзывы, связанные с пользователем, при удалении его аккаунта.
     * <p>
     * Отзывы, где пользователь был автором, помечаются как "автор удален".
     * Отзывы о пользователе помечаются как "пользователь удален".
     * </p>
     *
     * @param user удаляемый пользователь
     */
    private void handleUserReviews(User user) {
        log.debug("Обработка отзывов пользователя");

        // Отзывы, написанные пользователем
        List<Reviews> authoredReviews = reviewsRepo.findByAuthorIdOrderByCreatedAtDesc(user.getId());
        for (Reviews review : authoredReviews) {
            review.setAuthor(null);
            review.setAuthorDeleted(true);
            reviewsRepo.save(review);
        }

        // Отзывы о пользователе
        List<Reviews> reviewsAboutUser = reviewsRepo.findByReviewedUserIdOrderByCreatedAtDesc(user.getId());
        for (Reviews review : reviewsAboutUser) {
            review.setReviewedUserDeleted(true);
            reviewsRepo.save(review);
        }

        log.debug("Обработка отзывов завершена");
    }

    /**
     * Обрабатывает комментарии поддержки, связанные с пользователем.
     * <p>
     * Удаляет все комментарии пользователя, предварительно отвязывая от них ответы.
     * Также отвязывает комментарии, где пользователь указан как родительский комментатор.
     * </p>
     *
     * @param user удаляемый пользователь
     */
    private void handleSupportComments(User user) {
        log.debug("Обработка комментариев поддержки пользователя");

        // Комментарии, написанные пользователем
        List<SupportComment> userComments = supportCommentRepo.findByUser(user);

        for (SupportComment comment : userComments) {
            if (!comment.getReplies().isEmpty()) {
                for (SupportComment reply : comment.getReplies()) {
                    reply.setParentComment(null);
                    supportCommentRepo.save(reply);
                }
            }
            supportCommentRepo.delete(comment);
        }

        // Комментарии, где пользователь указан как родительский комментатор
        List<SupportComment> commentsWhereUserIsParent = supportCommentRepo.findByParentCommentUser(user);
        for (SupportComment comment : commentsWhereUserIsParent) {
            comment.setParentComment(null);
            supportCommentRepo.save(comment);
        }

        log.debug("Обработка комментариев поддержки завершена");
    }
}