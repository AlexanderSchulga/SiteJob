package jobforsite.sitejob.module.users.controller;

import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * MVC-контроллер для управления пользователями администраторами.
 * <p>
 * Обеспечивает функциональность для администраторов и суперадминистраторов:
 * <ul>
 *     <li>Редактирование данных пользователей</li>
 *     <li>Обновление информации о пользователе</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
@Controller
@Slf4j
public class UpdateUser {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegionsConfig regionsConfig;
    private final UpdateUserPost updateUserPost;


    // ======================================================================
    // УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ
    // ======================================================================

    /**
     * Отображает страницу редактирования пользователя для администратора.
     *
     * @param hashId         хэш-идентификатор пользователя
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации текущего пользователя
     * @return имя представления или редирект при ошибке
     */
    @GetMapping("/users/{hashId}/page_two/admin_edit_user")
    public String editUser(@PathVariable(value = "hashId") String hashId,
                           Model model,
                           Authentication authentication) {
        log.info("Accessing user edit page - target user hash: {}", hashId);

        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Access denied to edit user page - insufficient privileges");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/users";
            }

            Optional<User> userOptional = userRepository.findByHashId(hashId);
            if (userOptional.isEmpty()) {
                log.warn("User not found for edit - hash: {}", hashId);
                model.addAttribute("error", "Пользователь не найден");
                return "redirect:/users";
            }

            User user = userOptional.get();
            ArrayList<User> result = new ArrayList<>();
            result.add(user);

            // Загружаем контент пользователя
            updateUserPost.loadUserContent(user, model);

            model.addAttribute("user", result);
            model.addAttribute("regions", regionsConfig.getRegions());
            model.addAttribute("title", "Редактирование пользователя");

            return "page_two/admin_edit_user";

        } catch (Exception e) {
            log.error("Error loading user edit page: ", e);
            model.addAttribute("error", "Ошибка при загрузке пользователя: " + e.getMessage());
            return "redirect:/users";
        }
    }

    /**
     * Обновляет данные пользователя.
     * <p>
     * Суперадмин может изменять username и роль, обычный админ — нет.
     * Проверяет уникальность email и телефона при изменении.
     * При запросе сбрасывает пароль на "123456".
     * </p>
     */
    @PostMapping("/users/{hashId}/page_two/admin_edit_user/update")
    public String updateUser(@PathVariable(value = "hashId") String hashId,
                             @RequestParam String username,
                             @RequestParam String nameuse,
                             @RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam(required = false) String phonePost,
                             @RequestParam String region,
                             @RequestParam(required = false) String city,
                             @RequestParam String role,
                             @RequestParam boolean active,
                             @RequestParam(required = false) Boolean resetPassword,
                             Model model,
                             Authentication authentication) {
        log.info("Updating user - target hash: {}", hashId);

        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Access denied to update user - insufficient privileges");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/users";
            }

            Optional<User> optionalUser = userRepository.findByHashId(hashId);
            if (optionalUser.isEmpty()) {
                log.warn("User not found for update - hash: {}", hashId);
                model.addAttribute("error", "Пользователь не найден");
                return "redirect:/users";
            }

            User user = optionalUser.get();
            User currentAdmin = (User) authentication.getPrincipal();
            boolean isSuperAdmin = currentAdmin.getRole().equals("ROLE_SUPER");
            boolean isAdmin = currentAdmin.getRole().equals("ROLE_ADMIN");

            // Обработка username - только суперадмин может его менять
            if (isSuperAdmin) {
                if (!user.getUsername().equals(username)) {
                    Optional<User> existingUser = userRepository.findByUsername(username);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                        log.warn("Username already taken - update blocked");
                        model.addAttribute("error", "Имя пользователя уже занято");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                    user.setUsername(username);
                    log.debug("Username updated by superadmin");
                }
            } else if (isAdmin) {
                if (!user.getUsername().equals(username)) {
                    log.debug("Admin attempted to change username - ignored");
                }
            }

            // Проверяем уникальность email
            if (!user.getEmail().equals(email)) {
                Optional<User> existingUser = userRepository.findByEmail(email);
                if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                    log.warn("Email already taken - update blocked");
                    model.addAttribute("error", "Email уже занят");
                    return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                }
                user.setEmail(email);
                log.debug("Email updated");
            }

            // Проверяем уникальность телефона

            // нормализация: "+7 (", "+7", "", пробелы -> null
            phone = normalizePhone(phone);

            if (!Objects.equals(user.getPhone(), phone)) {
                if (phone != null) {
                    Optional<User> existingUser = userRepository.findByPhone(phone);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                        log.warn("Phone already taken - update blocked");
                        model.addAttribute("error", "Телефон 1 уже занят");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                }
                user.setPhone(phone);
                log.debug("Телефон успешно изменен");
            }

            // нормализация: "+7 (", "+7", "", пробелы -> null
            phonePost = normalizePhone(phonePost);

            // --- Обновление телефона для объявлений ---
            //user.setPhonePost(phonePost);

            if (!Objects.equals(user.getPhonePost(), phonePost)) {
                if (phonePost != null) {
                    Optional<User> existingUser = userRepository.findByPhonePost(phonePost);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                        log.warn("Phone already taken - update blocked");
                        model.addAttribute("error", "Телефон 2 уже занят");
                        return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
                    }
                }
                user.setPhonePost(phonePost);
                log.debug("Телефон успешно изменен");
            }


            user.setNameuse(nameuse);
            //user.setPhonePost(phonePost);
            user.setRegion(region);

            String processedCity = (city != null && !city.trim().isEmpty()) ? city.trim() : null;
            user.setCity(processedCity);

            // Управление ролью - только суперадмин может менять роли
            if (isSuperAdmin && !user.getRole().equals(role)) {
                user.setRole(role);
                log.debug("Role updated by superadmin");
            }

            user.setActive(active);

            if (resetPassword != null && resetPassword) {
                user.setPassword(passwordEncoder.encode("123456"));
                log.info("Password reset requested");
            }

            userRepository.save(user);
            log.info("User successfully updated");

            model.addAttribute("success", "Данные пользователя успешно обновлены");
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user?success";

        } catch (Exception e) {
            log.error("Error updating user: ", e);
            model.addAttribute("error", "Ошибка при обновлении пользователя: " + e.getMessage());
            return "redirect:/users/" + hashId + "/page_two/admin_edit_user";
        }
    }

    private String normalizePhone(String phon) {
        if (phon == null) return null;
        String digits = phon.replaceAll("\\D", ""); // только цифры
        // если осталась только ведущая 7 (или ничего) — считаем пустым
        if (digits.isEmpty() || digits.equals("7")) return null;
        return phon.trim();
    }

    // ======================================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ======================================================================

    private boolean hasSuperAccess(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER")
                        || a.getAuthority().equals("ROLE_ADMIN"));
    }
}