package jobforsite.sitejob.module.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class VerificationCodeService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);

    private final Map<String, String> registrationCodes = new ConcurrentHashMap<>();
    private final Map<String, String> passwordResetCodes = new ConcurrentHashMap<>();
    private final Map<String, String> flashCallCodes = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private String generateCode() {
        return String.format("%06d", random.nextInt(999999));
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String normalized = phone.replaceAll("[^\\d]", "");
        if (normalized.startsWith("8")) {
            normalized = "7" + normalized.substring(1);
        }
        return normalized;
    }

    // ========== REGISTRATION ==========
    public String createRegistrationCode(String email) {
        logger.info("Создание кода регистрации для email: {}", email);
        String code = generateCode();
        registrationCodes.put(email, code);
        return code;
    }

    public boolean verifyRegistrationCode(String email, String code) {
        String savedCode = registrationCodes.get(email);
        boolean isValid = savedCode != null && savedCode.equals(code);
        if (isValid) {
            logger.info("Успешная верификация кода регистрации для email: {}", email);
        } else {
            logger.warn("Неудачная верификация кода регистрации для email: {}", email);
        }
        return isValid;
    }

    public void removeRegistrationCode(String email) {
        registrationCodes.remove(email);
        logger.info("Код регистрации удален для email: {}", email);
    }

    // ========== PASSWORD RESET ==========
    public String createPasswordResetCode(String email) {
        logger.info("Создание кода восстановления пароля для email: {}", email);
        String code = generateCode();
        passwordResetCodes.put(email, code);
        return code;
    }

    public boolean verifyPasswordResetCode(String email, String code) {
        String savedCode = passwordResetCodes.get(email);
        boolean isValid = savedCode != null && savedCode.equals(code);
        if (isValid) {
            logger.info("Успешная верификация кода восстановления для email: {}", email);
        } else {
            logger.warn("Неудачная верификация кода восстановления для email: {}", email);
        }
        return isValid;
    }

    public void removePasswordResetCode(String email) {
        passwordResetCodes.remove(email);
        logger.info("Код восстановления пароля удален для email: {}", email);
    }

    // ========== FLASHCALL ==========
    public void saveFlashCallCode(String phone, String code) {
        String normalized = normalizePhone(phone);
        flashCallCodes.put(normalized, code);
        logger.info("Flashcall код сохранен для номера: {} (нормализован: {})", phone, normalized);
    }

    public boolean verifyFlashCallCode(String phone, String code) {
        String normalized = normalizePhone(phone);
        String savedCode = flashCallCodes.get(normalized);
        boolean isValid = savedCode != null && savedCode.equals(code);
        if (isValid) {
            logger.info("Flashcall код подтвержден для номера: {}", phone);
        } else {
            logger.warn("Неверный Flashcall код для номера: {} (ожидался: {})", phone, savedCode);
        }
        return isValid;
    }

    public void removeFlashCallCode(String phone) {
        String normalized = normalizePhone(phone);
        flashCallCodes.remove(normalized);
        logger.info("Flashcall код удален для номера: {}", phone);
    }

    public boolean hasRegistrationCode(String email) {
        return registrationCodes.containsKey(email);
    }

    public boolean hasPasswordResetCode(String email) {
        return passwordResetCodes.containsKey(email);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanExpiredCodes() {
        int regSize = registrationCodes.size();
        int resetSize = passwordResetCodes.size();
        int flashSize = flashCallCodes.size();

        registrationCodes.clear();
        passwordResetCodes.clear();
        flashCallCodes.clear();

        logger.info("Очистка кодов: {} регистрационных, {} восстановления, {} Flashcall",
                regSize, resetSize, flashSize);
    }

    public boolean hasFlashCallCode(String phone) {
        String normalized = normalizePhone(phone);
        return flashCallCodes.containsKey(normalized);
    }
}