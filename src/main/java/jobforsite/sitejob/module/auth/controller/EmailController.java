package jobforsite.sitejob.module.auth.controller;

import jobforsite.sitejob.module.auth.service.EmailService;
import jobforsite.sitejob.module.auth.service.VerificationCodeService;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Контроллер для восстановления пароля через email
 *
 * Поток: forgot-password (ввод email + капча) ->
 *        отправка кода ->
 *        reset-password (код + новый пароль)
 */
@RequiredArgsConstructor
@Slf4j
@Controller
public class EmailController {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;

    // Определение реального IP клиента (с учётом прокси/X-Forwarded-For)
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * GET /forgot-password - Показ формы ввода email для восстановления пароля
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeForgotPasswordGet(clientIp)) {
            log.warn("Слишком частые GET запросы от IP: {}", clientIp);
            model.addAttribute("error", "Слишком частые запросы. Пожалуйста, подождите.");
        }

        model.addAttribute("title", "Восстановление пароля");
        return "forgot-password";
    }

    /**
     * POST /forgot-password - Отправка кода восстановления на email
     */
    @PostMapping("/forgot-password")
    public String sendPasswordResetCode(
            @RequestParam String email,
            @RequestParam(name = "captcha", required = false) Boolean captcha,
            HttpServletRequest request,
            Model model
    ) {
        String clientIp = getClientIp(request);

        // Rate limiting для отправки кода восстановления
        if (!rateLimitService.tryConsumeForgotPasswordPost(clientIp)) {
            log.warn("Слишком много POST запросов от IP: {}", clientIp);
            model.addAttribute("error", "Слишком много запросов. Попробуйте через час.");
            return "forgot-password";
        }

        // Капча
        if (captcha == null || !captcha) {
            log.warn("Неудачная попытка восстановления пароля с IP: {} - не пройдена капча", clientIp);
            model.addAttribute("error", "Подтвердите, что вы не робот");
            return "forgot-password";
        }

        try {
            // Проверка существования email
            if (userRepository.findByEmail(email).isEmpty()) {
                log.warn("Попытка восстановления пароля для несуществующего email: {} с IP: {}", email, clientIp);
                model.addAttribute("error", "Пользователь с таким email не найден");
                rateLimitService.recordFailedAttempt(clientIp);
                return "forgot-password";
            }

            // Получаем пользователя для логирования ника
            User user = userRepository.findByEmail(email).get();

            // Генерация и отправка кода
            String code = verificationCodeService.createPasswordResetCode(email);
            emailService.sendPasswordResetCode(email, code);

            log.info("Код восстановления пароля отправлен на email: {} для пользователя: {}", email, user.getNameuse());

            model.addAttribute("email", email);
            model.addAttribute("title", "Восстановление пароля");

            rateLimitService.clearFailedAttempts(clientIp);

            return "reset-password";

        } catch (Exception e) {
            log.error("Ошибка при отправке кода восстановления с IP: {}", clientIp, e);
            model.addAttribute("error", "Ошибка при отправке кода: " + e.getMessage());
            rateLimitService.recordFailedAttempt(clientIp);
            return "forgot-password";
        }
    }

    /**
     * POST /reset-password - Проверка кода и установка нового пароля
     */
    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String newPassword,
            HttpServletRequest request,
            Model model
    ) {
        String clientIp = getClientIp(request);

        // Rate limiting для сброса пароля
        if (!rateLimitService.tryConsumeResetPassword(clientIp)) {
            log.warn("Слишком много попыток сброса пароля от IP: {}", clientIp);
            model.addAttribute("error", "Слишком много попыток. Попробуйте через минуту.");
            model.addAttribute("email", email);
            return "reset-password";
        }

        try {
            // Проверка кода
            if (!verificationCodeService.verifyPasswordResetCode(email, code)) {
                log.warn("Неверный код подтверждения для email: {} с IP: {}", email, clientIp);
                model.addAttribute("error", "Неверный код подтверждения");
                model.addAttribute("email", email);
                rateLimitService.recordFailedAttempt(clientIp);
                return "reset-password";
            }

            // Обновление пароля
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // Очистка временных данных
            verificationCodeService.removePasswordResetCode(email);

            rateLimitService.clearFailedAttempts(clientIp);

            log.info("Пароль успешно изменен для пользователя: {} с email: {}", user.getNameuse(), email);

            return "redirect:/login";

        } catch (Exception e) {
            log.error("Ошибка при сбросе пароля для email: {} с IP: {}", email, clientIp, e);
            model.addAttribute("error", "Ошибка при сбросе пароля: " + e.getMessage());
            model.addAttribute("email", email);
            rateLimitService.recordFailedAttempt(clientIp);
            return "reset-password";
        }
    }
}