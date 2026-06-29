package jobforsite.sitejob.module.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jobforsite.sitejob.module.auth.dto.RegistrationDto;
import jobforsite.sitejob.module.auth.service.EmailService;
import jobforsite.sitejob.module.auth.service.VerificationCodeService;
import jobforsite.sitejob.module.shared.repository.ReviewsRepo;
import jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.BlacklistRepository;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Контроллер аутентификации и регистрации пользователей
 * Обрабатывает запросы на регистрацию, вход, подтверждение телефона и создание пользователей администратором
 */
@RequiredArgsConstructor
@Slf4j
@Controller
public class AuthController {
    private final UserRepository userRepository;
    private final BlacklistRepository blacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;
    private final RateLimitService rateLimitService;
    private final ReviewsRepo reviewsRepo;
    //private final SmsRuService smsRuService;

    /**
     * Хранилище временных данных регистрации до подтверждения телефона
     * Ключ: номер телефона, Значение: данные регистрации
     */
    private final Map<String, RegistrationData> pendingRegistrations = new HashMap<>();

    /**
     * Список разрешенных доменов email для регистрации (только российские почтовые сервисы)
     */
    private static final List<String> ALLOWED_EMAIL_DOMAINS = Arrays.asList(
            "mail.ru", "bk.ru", "list.ru", "inbox.ru",
            "yandex.ru", "yandex.ua", "yandex.by", "yandex.kz", "yandex.com", "ya.ru",
            "rambler.ru", "lenta.ru", "autorambler.ru", "myrambler.ru", "ro.ru", "r0.ru",
            "vk.com", "vkontakte.ru"
    );

    /**
     * Проверяет, является ли email допустимым российским почтовым адресом
     *
     * @param email Email для проверки
     * @return true если email соответствует допустимому домену, false в противном случае
     */
    private boolean isValidRussianEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return false;
        }
        String domain = normalizedEmail.substring(normalizedEmail.indexOf('@') + 1);
        return ALLOWED_EMAIL_DOMAINS.stream()
                .anyMatch(allowedDomain -> domain.equals(allowedDomain) || domain.endsWith("." + allowedDomain));
    }

    /**
     * Проверяет, соответствует ли пароль требованиям безопасности
     *
     * @param password Пароль для проверки
     * @return true если пароль содержит только латинские буквы и цифры и имеет длину не менее 6 символов
     */
    private boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.matches("^[a-zA-Z0-9]{6,}$");
    }

    /**
     * Проверяет корректность формата основного номера телефона
     *
     * @param phone Номер телефона для проверки
     * @return true если номер соответствует формату +7XXXXXXXXXX
     */
    private boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        String cleanPhone = phone.replaceAll("[^+\\d]", "");
        return cleanPhone.matches("^\\+7\\d{10}$");
    }

    /**
     * Проверяет корректность формата дополнительного номера телефона (для объявлений)
     *
     * @param phonePost Дополнительный номер телефона для проверки
     * @return true если номер соответствует формату +7XXXXXXXXXX или пустой
     */
    private boolean isValidPhonePost(String phonePost) {
        if (phonePost == null || phonePost.trim().isEmpty()) {
            return true;
        }
        String cleanPhonePost = phonePost.replaceAll("[^+\\d]", "");
        return cleanPhonePost.matches("^\\+7\\d{10}$");
    }

    /**
     * Внутренний класс для хранения данных регистрации до подтверждения телефона
     */
    private static class RegistrationData {
        String username;
        String nameuse;
        String password;
        String email;
        String phone;
        String phonePost;
        String region;
        String city;

        RegistrationData(String username, String nameuse, String password, String email,
                         String phone, String phonePost, String region, String city) {
            this.username = username;
            this.nameuse = nameuse;
            this.password = password;
            this.email = email;
            this.phone = phone;
            this.phonePost = phonePost;
            this.region = region;
            this.city = city;
        }
    }

    /**
     * Получает реальный IP-адрес клиента с учетом прокси и заголовков X-Forwarded-For
     *
     * @param request HTTP-запрос
     * @return IP-адрес клиента
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Отображает форму регистрации
     *
     * @param model   Модель для передачи данных в представление
     * @param request HTTP-запрос
     * @return Имя шаблона для отображения формы регистрации
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeRegisterGet(clientIp)) {
            return handleRateLimitError(model, "Слишком частые запросы. Пожалуйста, подождите.");
        }

        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new RegistrationDto());
        }

        // Генерируем цель капчи от 2 до 6
        HttpSession session = request.getSession();
        int captchaTarget = (int)(Math.random() * 5) + 2; // 2-6
        session.setAttribute("captchaTarget", captchaTarget);
        session.setAttribute("captchaTimestamp", System.currentTimeMillis());

        model.addAttribute("captchaTarget", captchaTarget);
        model.addAttribute("title", "Регистрация");

        return "register";
    }

    /**
     * Вспомогательный метод для обработки ошибок rate limit
     *
     * @param model       Модель для передачи данных в представление
     * @param errorMessage Сообщение об ошибке
     * @return Имя шаблона для отображения формы регистрации с ошибкой
     */
    private String handleRateLimitError(Model model, String errorMessage) {
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new RegistrationDto());
        }
        model.addAttribute("error", errorMessage);
        model.addAttribute("title", "Регистрация");
        model.addAttribute("captchaTarget", 2); // заглушка (2-6)
        return "register";
    }

    /**
     * Обрабатывает отправку формы регистрации
     * Выполняет валидацию данных, проверку капчи, отправку звонка для подтверждения телефона
     *
     * @param dto          Данные регистрации
     * @param sliderValue  Значение слайдера капчи
     * @param request      HTTP-запрос
     * @param model        Модель для передачи данных в представление
     * @return Имя шаблона для отображения страницы подтверждения телефона или формы регистрации с ошибками
     */
    @PostMapping("/register")
    public String registerUser(@ModelAttribute RegistrationDto dto,
                               @RequestParam("captchaSlider") int sliderValue,
                               HttpServletRequest request,
                               Model model) {

        String clientIp = getClientIp(request);
        HttpSession session = request.getSession();

        log.info("Регистрация пользователя: никнейм {}, email: {}, IP: {}",
                dto.getNameuse(), dto.getEmail(), clientIp);

        if (rateLimitService.isIpBlocked(clientIp)) {
            model.addAttribute("error", "Слишком много попыток. Попробуйте через 15 минут.");
            model.addAttribute("registrationDto", dto);
            generateNewCaptcha(session, model);
            return "register";
        }

        if (!rateLimitService.tryConsumeRegisterPost(clientIp)) {
            rateLimitService.recordFailedAttempt(clientIp);
            model.addAttribute("error", "Слишком много попыток регистрации. Попробуйте через час.");
            model.addAttribute("registrationDto", dto);
            generateNewCaptcha(session, model);
            return "register";
        }

        List<String> errors = new ArrayList<>();

        // ========== ПРОВЕРКА КАПЧИ ==========
        Integer storedTarget = (Integer) session.getAttribute("captchaTarget");
        Long timestamp = (Long) session.getAttribute("captchaTimestamp");

        if (storedTarget == null || timestamp == null ||
                System.currentTimeMillis() - timestamp > 300000) {
            errors.add("Сессия капчи истекла. Обновите страницу.");
            rateLimitService.recordFailedAttempt(clientIp);
        } else if (sliderValue != storedTarget) {
            errors.add("Неверное значение капчи. Установите ползунок на число, которое показано.");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        // ========== ВАЛИДАЦИЯ EMAIL  ==========
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            errors.add("Email обязателен для регистрации. Нужен для подтверждения и восстановления пароля.");
            rateLimitService.recordFailedAttempt(clientIp);
        } else if (!isValidRussianEmail(dto.getEmail())) {
            errors.add("Разрешены только почтовые сервисы: Mail.ru, Яндекс, Рамблер, VK.com");
            rateLimitService.recordFailedAttempt(clientIp);
        } else if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            errors.add("Пользователь с таким email уже зарегистрирован");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        // ========== ВАЛИДАЦИЯ ТЕЛЕФОНА (обязательное ==========
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            errors.add("Телефон обязателен для регистрации");
            rateLimitService.recordFailedAttempt(clientIp);
        } else if (!isValidPhone(dto.getPhone())) {
            errors.add("Некорректный формат основного телефона. Используйте формат +7 (XXX) XXX-XX-XX");
            rateLimitService.recordFailedAttempt(clientIp);
        } else if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
            errors.add("Пользователь с таким телефоном зарегистрирован");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        // ========== ОСТАЛЬНЫЕ ПРОВЕРКИ ==========
        if (blacklistRepository.existsByAnyContact(dto.getEmail(), dto.getPhone(), dto.getPhonePost())) {
            errors.add("Контактные данные находятся в черном списке");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (dto.getLicense() == null || !dto.getLicense()) {
            errors.add("Примите лицензионное соглашение");
        }

        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            errors.add("Имя пользователя уже занято");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (userRepository.findByNameuse(dto.getNameuse()).isPresent()) {
            errors.add("Никнейм уже занят");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (reviewsRepo.countByReviewedNameuse(dto.getNameuse()) > 0) {
            errors.add("Никнейм ранее использовался удаленным пользователем");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (!isValidPassword(dto.getPassword())) {
            errors.add("Пароль может содержать только английские буквы и цифры (минимум 6 символов)");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            errors.add("Пароли не совпадают");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (dto.getUsername() == null || !dto.getUsername().matches("^[a-zA-Z0-9]+$")) {
            errors.add("Логин может содержать только английские буквы и цифры");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        if (dto.getNameuse() == null || !dto.getNameuse().matches("^[a-zA-Zа-яА-ЯёЁ0-9\\s]+$")) {
            errors.add("Никнейм может содержать только буквы (русские или английские), цифры и пробелы");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        boolean isPhonePostValid = true;
        if (dto.getPhonePost() != null && !dto.getPhonePost().trim().isEmpty()) {
            String phonePostTrimmed = dto.getPhonePost().trim();
            if (!phonePostTrimmed.equals("+7 (") && !phonePostTrimmed.equals("+7") && !phonePostTrimmed.equals("+7()")) {
                isPhonePostValid = isValidPhonePost(phonePostTrimmed);
            }
        }

        if (!isPhonePostValid) {
            errors.add("Некорректный формат телефона для объявлений. Используйте формат +7 (XXX) XXX-XX-XX");
            rateLimitService.recordFailedAttempt(clientIp);
        }

        String processedCity = null;
        if (dto.getCity() != null && !dto.getCity().trim().isEmpty() && !dto.getCity().equals("manual_input")) {
            processedCity = dto.getCity().trim();
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("registrationDto", dto);
            generateNewCaptcha(session, model);

            if (dto.getRegion() != null) {
                model.addAttribute("selectedRegion", dto.getRegion());
            }
            if (processedCity != null) {
                model.addAttribute("selectedCity", processedCity);
            }
            rateLimitService.recordFailedAttempt(clientIp);
            return "register";
        }

        // ========== КАПЧА ПРОЙДЕНА ==========
        session.removeAttribute("captchaTarget");
        session.removeAttribute("captchaTimestamp");

        try {
            // ========== ОТПРАВЛЯЕМ КОД НА EMAIL==========
            String code = verificationCodeService.createRegistrationCode(dto.getEmail());
            log.info("Сгенерирован код для {}: {}", dto.getEmail(), code);

            emailService.sendRegistrationCode(dto.getEmail(), code);
            log.info("Код отправлен на: {}", dto.getEmail());

            // Сохраняем данные до подтверждения (ключ — email, а не телефон!)
            pendingRegistrations.put(dto.getEmail(), new RegistrationData(
                    dto.getUsername(), dto.getNameuse(), dto.getPassword(),
                    dto.getEmail(), dto.getPhone(), dto.getPhonePost(),
                    dto.getRegion(), processedCity));

            model.addAttribute("email", dto.getEmail());
            model.addAttribute("title", "Подтверждение email");

            rateLimitService.clearFailedAttempts(clientIp);

            // ========== ВОЗВРАЩАЕМ ШАБЛОН verify-email ==========
            return "verify-email";

        } catch (Exception e) {
            log.info("Ошибка при регистрации");
            model.addAttribute("error", "Ошибка при отправке кода: " + e.getMessage());
            model.addAttribute("registrationDto", dto);
            rateLimitService.recordFailedAttempt(clientIp);
            generateNewCaptcha(session, model);
            return "register";
        }
    }

    /**
     * POST /verify-registration - Подтверждение email-кода и создание пользователя
     */
    @PostMapping("/verify-registration")
    @Transactional
    public String verifyRegistration(
            @RequestParam String email,
            @RequestParam String code,
            HttpServletRequest request,
            Model model
    ) {
        String clientIp = getClientIp(request);

        log.info("=== НАЧАЛО ПОДТВЕРЖДЕНИЯ РЕГИСТРАЦИИ ===");
        log.info("Попытка подтверждения для email: {}, код: {}, IP: {}", email, code, clientIp);

        // Rate limiting
        if (!rateLimitService.tryConsumeVerifyRegistration(clientIp)) {
            model.addAttribute("error", "Слишком много попыток. Попробуйте через минуту.");
            model.addAttribute("email", email);
            return "verify-email";
        }

        try {
            // Проверка кода
            log.info("Проверка кода...");
            if (!verificationCodeService.verifyRegistrationCode(email, code)) {
                log.warn("Неверный код для {}: {}", email, code);
                rateLimitService.recordFailedAttempt(clientIp);
                model.addAttribute("error", "Неверный код подтверждения");
                model.addAttribute("email", email);
                return "verify-email";
            }
            log.info("Код подтвержден успешно");

            // Повторная проверка существования (на случай повторной регистрации)
            log.info("Проверка существования пользователя с email: {}", email);
            if (userRepository.findByEmail(email).isPresent()) {
                log.warn("Пользователь с email {} уже существует!", email);
                rateLimitService.recordFailedAttempt(clientIp);
                model.addAttribute("error", "Пользователь с таким email уже зарегистрирован");
                model.addAttribute("email", email);
                return "verify-email";
            }

            // Получение временных данных
            log.info("Поиск данных регистрации для: {}", email);
            RegistrationData data = pendingRegistrations.get(email);
            if (data == null) {
                log.error("Данные регистрации не найдены для: {}", email);
                log.error("Доступные ключи: {}", pendingRegistrations.keySet());
                rateLimitService.recordFailedAttempt(clientIp);
                model.addAttribute("error", "Время регистрации истекло. Пожалуйста, начните заново.");
                return "register";
            }
            log.info("Данные регистрации найдены: никнейм={}, region={}, city={}",
                    data.nameuse, data.region, data.city);

            // Создание нового пользователя
            log.info("Создание пользователя: никнейм={}, email={}", data.nameuse, data.email);
            User user = new User();
            user.setUsername(data.username);
            user.setNameuse(data.nameuse);
            user.setEmail(data.email);
            user.setPhone(data.phone);
            user.setPhonePost(data.phonePost);
            user.setPassword(passwordEncoder.encode(data.password));
            user.setRole("ROLE_USER");
            user.setActive(true);
            user.setRegion(data.region);
            user.setCity(data.city);

            log.info("Сохранение пользователя в базу...");
            User savedUser = userRepository.save(user);
            log.info("Пользователь успешно сохранен: никнейм={}, email={}, ID={}",
                    savedUser.getNameuse(), savedUser.getEmail(), savedUser.getId());

            // Очистка временных данных
            log.info("Очистка временных данных...");
            pendingRegistrations.remove(email);
            verificationCodeService.removeRegistrationCode(email);

            rateLimitService.clearFailedAttempts(clientIp);

            log.info("=== РЕГИСТРАЦИЯ УСПЕШНО ЗАВЕРШЕНА ===");
            return "redirect:/login?success";

        } catch (Exception e) {
            log.error("КРИТИЧЕСКАЯ ОШИБКА при подтверждении регистрации: ", e);
            rateLimitService.recordFailedAttempt(clientIp);
            model.addAttribute("error", "Критическая ошибка при регистрации: " + e.getMessage());
            model.addAttribute("email", email);
            return "verify-email";
        }
    }

//    /**
//     * Обрабатывает подтверждение телефона
//     * Проверяет код подтверждения и создает нового пользователя
//     *
//     * @param phone      Номер телефона для подтверждения
//     * @param flashCode  Код подтверждения из звонка
//     * @param request    HTTP-запрос
//     * @param model      Модель для передачи данных в представление
//     * @return Перенаправление на страницу входа или возврат к подтверждению с ошибкой
//     */
//    @PostMapping("/verify-phone")
//    @Transactional
//    public String verifyPhone(
//            @RequestParam String phone,
//            @RequestParam String flashCode,
//            HttpServletRequest request,
//            Model model
//    ) {
//        String clientIp = getClientIp(request);
//
//        log.info("Подтверждение телефона: {}, код: {}, IP: {}", phone, flashCode, clientIp);
//
//        if (!rateLimitService.tryConsumeVerifyRegistration(clientIp)) {
//            model.addAttribute("error", "Слишком много попыток. Попробуйте через минуту.");
//            model.addAttribute("phone", phone);
//            return "phone-verification";
//        }
//
//        try {
//            if (!verificationCodeService.verifyFlashCallCode(phone, flashCode)) {
//                log.warn("Неверный код для номера: {}", phone);
//                rateLimitService.recordFailedAttempt(clientIp);
//                model.addAttribute("error", "Неверный код. Введите 4 цифры из звонка.");
//                model.addAttribute("phone", phone);
//                return "phone-verification";
//            }
//
//            if (userRepository.findByPhone(phone).isPresent()) {
//                log.warn("Пользователь с телефоном {} уже существует!", phone);
//                rateLimitService.recordFailedAttempt(clientIp);
//                model.addAttribute("error", "Пользователь с таким телефоном уже зарегистрирован");
//                model.addAttribute("phone", phone);
//                return "phone-verification";
//            }
//
//            RegistrationData data = pendingRegistrations.get(phone);
//            if (data == null) {
//                log.error("Данные регистрации не найдены для номера: {}", phone);
//                rateLimitService.recordFailedAttempt(clientIp);
//                model.addAttribute("error", "Время регистрации истекло. Начните заново.");
//                return "register";
//            }
//
//            log.info("Создание пользователя: никнейм={}, телефон={}", data.nameuse, data.phone);
//
//            User user = new User();
//            user.setUsername(data.username);
//            user.setNameuse(data.nameuse);
//            if (data.email != null && !data.email.trim().isEmpty()) {
//                user.setEmail(data.email);
//            } else {
//                user.setEmail(null);
//            }
//            user.setPhone(data.phone);
//            user.setPhonePost(data.phonePost);
//            user.setPassword(passwordEncoder.encode(data.password));
//            user.setRole("ROLE_USER");
//            user.setActive(true);
//            user.setRegion(data.region);
//            user.setCity(data.city);
//
//            User savedUser = userRepository.save(user);
//            log.info("Пользователь сохранён в БД: никнейм={}, телефон={}, ID={}",
//                    savedUser.getNameuse(), savedUser.getPhone(), savedUser.getId());
//
//            pendingRegistrations.remove(phone);
//            verificationCodeService.removeFlashCallCode(phone);
//
//            rateLimitService.clearFailedAttempts(clientIp);
//
//            log.info("Регистрация успешно завершена для пользователя: {}", savedUser.getNameuse());
//
//            return "redirect:/login?success";
//
//        } catch (Exception e) {
//            log.error("КРИТИЧЕСКАЯ ОШИБКА при сохранении пользователя: ", e);
//            rateLimitService.recordFailedAttempt(clientIp);
//            model.addAttribute("error", "Критическая ошибка при регистрации: " + e.getMessage());
//            model.addAttribute("phone", phone);
//            return "phone-verification";
//        }
//    }

//    /**
//     * Повторная отправка звонка для подтверждения телефона
//     *
//     * @param phone   Номер телефона для отправки звонка
//     * @param request HTTP-запрос
//     * @return JSON-ответ с результатом операции
//     */
//    @PostMapping("/resend-flashcall")
//    @ResponseBody
//    public Map<String, Object> resendFlashCall(
//            @RequestParam String phone,
//            HttpServletRequest request
//    ) {
//        String clientIp = getClientIp(request);
//        Map<String, Object> response = new HashMap<>();
//
//        if (!rateLimitService.tryConsumeVerifyRegistration(clientIp)) {
//            response.put("success", false);
//            response.put("message", "Слишком много попыток. Подождите минуту.");
//            return response;
//        }
//
//        try {
//            String cleanPhone = phone.replaceAll("[^\\d]", "");
//            if (cleanPhone.startsWith("8")) {
//                cleanPhone = "7" + cleanPhone.substring(1);
//            } else if (!cleanPhone.startsWith("7")) {
//                cleanPhone = "7" + cleanPhone;
//            }
//
//            String userIp = getClientIp(request);
//
//            // Отправляем звонок и получаем новый код
//            String flashCode = smsRuService.sendFlashCall(cleanPhone, userIp);
//
//            if (flashCode == null) {
//                response.put("success", false);
//                response.put("message", "Не удалось отправить звонок");
//                return response;
//            }
//
//            // Сохраняем новый код
//            verificationCodeService.saveFlashCallCode(phone, flashCode);
//            log.info("Повторный Flashcall отправлен на номер: {}, код: {}", phone, flashCode);
//
//            response.put("success", true);
//            response.put("message", "Звонок отправлен повторно!");
//
//        } catch (Exception e) {
//            log.error("Ошибка повторной отправки: {}", e.getMessage());
//            response.put("success", false);
//            response.put("message", "Ошибка: " + e.getMessage());
//        }
//
//        return response;
//    }

    /**
     * Обновляет капчу на новое число
     *
     * @param request HTTP-запрос
     * @return JSON-ответ с новым значением капчи
     */
    @PostMapping("/api/captcha/refresh")
    @ResponseBody
    public Map<String, Object> refreshCaptcha(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Map<String, Object> response = new HashMap<>();

        if (!rateLimitService.tryConsumeCaptchaRefresh(clientIp)) {
            response.put("success", false);
            response.put("message", "Слишком частые запросы обновления капчи. Подождите минуту.");
            return response;
        }

        HttpSession session = request.getSession();
        int newTarget = (int)(Math.random() * 5) + 2; // 2-6
        session.setAttribute("captchaTarget", newTarget);
        session.setAttribute("captchaTimestamp", System.currentTimeMillis());

        response.put("success", true);
        response.put("target", newTarget);
        return response;
    }

    /**
     * Отображает форму входа в систему
     * Обрабатывает ошибки входа, проверку блокировки аккаунта и rate limiting
     *
     * @param error   Параметр ошибки (опционально)
     * @param username Имя пользователя (опционально)
     * @param blocked Параметр блокировки (опционально)
     * @param request HTTP-запрос
     * @param model   Модель для передачи данных в представление
     * @return Имя шаблона для отображения формы входа
     */
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String blocked,
            HttpServletRequest request,
            Model model) {

        model.addAttribute("title", "Вход");

        String clientIp = getClientIp(request);

        if (rateLimitService.isIpBlocked(clientIp)) {
            log.warn("IP {} заблокирован", clientIp);
            model.addAttribute("blockedError",
                    "Слишком много неудачных попыток. Попробуйте через 15 минут.");
            return "login";
        }

        if (!rateLimitService.tryConsumeLoginGet(clientIp)) {
            log.warn("Rate limit для IP {} (GET /login)", clientIp);
            model.addAttribute("rateLimitError",
                    "Слишком частые запросы. Пожалуйста, подождите.");
            return "login";
        }

        if (error != null && username != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!user.isActive()) {
                    if (user.getBlockUntil() != null &&
                            user.getBlockUntil().isBefore(LocalDateTime.now())) {
                        user.setActive(true);
                        user.setBlockReason(null);
                        user.setBlockedAt(null);
                        user.setBlockUntil(null);
                        userRepository.save(user);
                        model.addAttribute("error", "Неверное имя пользователя или пароль");
                    } else {
                        String blockReason = user.getBlockReason() != null ?
                                user.getBlockReason() : "Нарушение правил";

                        if (user.getBlockUntil() != null) {
                            Duration duration = Duration.between(LocalDateTime.now(), user.getBlockUntil());
                            long daysLeft = duration.toDays();
                            long hoursLeft = duration.toHours() % 24;
                            long minutesLeft = duration.toMinutes() % 60;

                            String timeLeft = "";
                            if (daysLeft > 0) timeLeft += daysLeft + " д. ";
                            if (hoursLeft > 0) timeLeft += hoursLeft + " ч. ";
                            if (minutesLeft > 0) timeLeft += minutesLeft + " мин.";

                            model.addAttribute("bannedError",
                                    "Аккаунт заблокирован. Причина: " + blockReason +
                                            " (Блокировка №" + user.getBlockCount() + ")" +
                                            ". Снятие через " + timeLeft);
                        } else {
                            model.addAttribute("bannedError",
                                    "Аккаунт заблокирован. Причина: " + blockReason +
                                            " (Блокировка №" + user.getBlockCount() + ")");
                        }
                    }
                } else {
                    model.addAttribute("error", "Неверное имя пользователя или пароль");
                }
            } else {
                model.addAttribute("error", "Неверное имя пользователя или пароль");
            }
        } else if (error != null) {
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }

        return "login";
    }

    /**
     * Обрабатывает выход из системы
     *
     * @param model Модель для передачи данных в представление
     * @return Перенаправление на страницу входа
     */
    @GetMapping("/logout")
    public String logoutExit(Model model) {
        model.addAttribute("title", "Выход");
        return "login";
    }

    /**
     * Отображает форму создания пользователя администратором (только для ROLE_SUPER)
     *
     * @param model          Модель для передачи данных в представление
     * @param authentication Объект аутентификации для проверки прав
     * @param request        HTTP-запрос
     * @return Имя шаблона для отображения формы создания пользователя или перенаправление на главную
     */
    @GetMapping("/admin-create-user")
    public String showAdminCreateForm(Model model, Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER"))) {
            return "redirect:/index";
        }

        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeAdminGet(clientIp)) {
            model.addAttribute("error", "Слишком частые запросы. Пожалуйста, подождите.");
        }

        model.addAttribute("title", "Создать пользователя");
        return "admin-create-user";
    }

    /**
     * Обрабатывает создание пользователя администратором (только для ROLE_SUPER)
     *
     * @param username       Имя пользователя (логин)
     * @param nameuse        Никнейм пользователя
     * @param password       Пароль
     * @param role           Роль пользователя
     * @param email          Email пользователя
     * @param phone          Телефон
     * @param phonePost      Дополнительный телефон (опционально)
     * @param region         Регион
     * @param city           Город (опционально)
     * @param model          Модель для передачи данных в представление
     * @param authentication Объект аутентификации для проверки прав
     * @param request        HTTP-запрос
     * @return Перенаправление на главную или возврат к форме с ошибкой
     */
    @PostMapping("/admin-create-user")
    public String createUser(
            @RequestParam String username,
            @RequestParam String nameuse,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String phonePost,
            @RequestParam String region,
            @RequestParam(required = false) String city,
            Model model,
            Authentication authentication,
            HttpServletRequest request
    ){
        String clientIp = getClientIp(request);

        if (!rateLimitService.tryConsumeAdminPost(clientIp)) {
            model.addAttribute("error", "Слишком много запросов. Попробуйте через минуту.");
            return "admin-create-user";
        }

        try {
            if (!authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER"))) {
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/index";
            }

            if (!isValidRussianEmail(email)) {
                model.addAttribute("error", "Разрешены только почтовые сервисы: Mail.ru, Яндекс, Рамблер, VK.com");
                rateLimitService.recordFailedAttempt(clientIp);
                return "admin-create-user";
            }

            if (userRepository.findByUsername(username).isPresent()) {
                model.addAttribute("error", "Логин занят");
                rateLimitService.recordFailedAttempt(clientIp);
                return "admin-create-user";
            }
            if (userRepository.findByNameuse(nameuse).isPresent()) {
                model.addAttribute("error", "Никнейм занят");
                rateLimitService.recordFailedAttempt(clientIp);
                return "admin-create-user";
            }
            if (userRepository.findByEmail(email).isPresent()) {
                model.addAttribute("error", "Пользователь с таким email уже существует");
                rateLimitService.recordFailedAttempt(clientIp);
                return "admin-create-user";
            }
            if (userRepository.findByPhone(phone).isPresent()) {
                model.addAttribute("error", "Пользователь с таким телефоном зарегистрирован");
                rateLimitService.recordFailedAttempt(clientIp);
                return "admin-create-user";
            }

            if (reviewsRepo.countByReviewedNameuse(nameuse) > 0) {
                model.addAttribute("error", "Никнейм ранее использовался удаленным пользователем");
                rateLimitService.recordFailedAttempt(clientIp);
                return "admin-create-user";
            }

            User user = new User();
            user.setUsername(username);
            user.setNameuse(nameuse);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPhonePost(phonePost);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(role.startsWith("ROLE_") ? role : "ROLE_" + role);
            user.setRegion(region);

            String processedCity = null;
            if (city != null && !city.trim().isEmpty() && !city.equals("manual_input")) {
                processedCity = city.trim();
            }
            user.setCity(processedCity);

            userRepository.save(user);

            rateLimitService.clearFailedAttempts(clientIp);

            return "redirect:/index";

        } catch (Exception e) {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
            rateLimitService.recordFailedAttempt(clientIp);
            return "admin-create-user";
        }
    }

    /**
     * Генерирует новое значение капчи и сохраняет его в сессии и модели
     *
     * @param session Сессия для хранения значения капчи
     * @param model   Модель для передачи значения в представление
     */
    private void generateNewCaptcha(HttpSession session, Model model) {
        // Генерируем число от 2 до 6 (исключаем 1)
        int newTarget = (int)(Math.random() * 5) + 2; // 2,3,4,5,6
        session.setAttribute("captchaTarget", newTarget);
        session.setAttribute("captchaTimestamp", System.currentTimeMillis());
        model.addAttribute("captchaTarget", newTarget);
    }
}