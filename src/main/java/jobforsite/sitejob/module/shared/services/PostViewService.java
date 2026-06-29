package jobforsite.sitejob.module.shared.services;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.model.PostView;
import jobforsite.sitejob.module.shared.repository.PostViewRepository;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Сервис для регистрации и учёта просмотров объявлений (товаров, услуг, вакансий, резюме).
 * <p>
 * Основные возможности:
 * <ul>
 *   <li>Регистрация просмотров для авторизованных и неавторизованных пользователей</li>
 *   <li>Защита от накрутки (свои просмотры не считаются, дубли по IP + fingerprint блокируются)</li>
 *   <li>Генерация отпечатка браузера (fingerprint) для идентификации уникальных посетителей</li>
 *   <li>Автоматическое увеличение счётчика просмотров у соответствующего объявления</li>
 * </ul>
 * </p>
 * <p>
 * Для неавторизованных пользователей используется комбинация: IP-адрес + fingerprint + sessionId.
 * Для авторизованных — привязка к объекту {@link User}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PostViewService {

    private static final Logger log = LoggerFactory.getLogger(PostViewService.class);

    private final PostViewRepository postViewRepository;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    /**
     * Получает реальный IP-адрес клиента с учётом прокси и балансировщиков.
     * <p>
     * Проверяет заголовок {@code X-Forwarded-For} (если есть), иначе возвращает remote address.
     * </p>
     *
     * @param request HTTP-запрос
     * @return IP-адрес клиента
     */
    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Генерирует уникальный отпечаток браузера (fingerprint) на основе заголовков запроса.
     * <p>
     * Использует комбинацию:
     * <ul>
     *   <li>User-Agent</li>
     *   <li>Accept-Language</li>
     *   <li>sec-ch-ua-platform (информация о платформе из Chrome/Edge)</li>
     * </ul>
     * Данные хэшируются алгоритмом SHA-256.
     * </p>
     *
     * @param request HTTP-запрос
     * @return SHA-256 хэш в виде шестнадцатеричной строки (или обрезанная строка при ошибке)
     */
    public String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        String platform = request.getHeader("sec-ch-ua-platform"); // Chrome/Edge
        if (platform == null) platform = "unknown";

        String fingerprintData = userAgent + "|" + acceptLanguage + "|" + platform;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintData.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 не поддерживается, используется fallback: {}", e.getClass().getSimpleName());
            return fingerprintData.substring(0, Math.min(64, fingerprintData.length()));
        }
    }

    /**
     * Регистрирует просмотр товара.
     * <p>
     * Условия регистрации:
     * <ul>
     *   <li>Товар не null</li>
     *   <li>Просматривающий не является владельцем товара</li>
     *   <li>Не было повторного просмотра от того же пользователя/устройства</li>
     * </ul>
     * </p>
     *
     * @param post    товар
     * @param request HTTP-запрос
     * @param user    текущий пользователь (может быть null для неавторизованных)
     * @return true если просмотр зарегистрирован, false если нет
     */
    @Transactional
    public boolean registerProductView(PostText post, HttpServletRequest request, User user) {
        if (post == null) return false;

        // Проверяем, не владелец ли просматривает
        if (user != null && user.getId().equals(post.getUser().getId())) {
            log.debug("Просмотр товара ID: {} владельцем не зарегистрирован", post.getId());
            return false;
        }

        boolean alreadyViewed;

        if (user != null) {
            // Авторизованный пользователь
            alreadyViewed = postViewRepository.existsByUserAndPost(user, post);
            if (!alreadyViewed) {
                PostView view = new PostView(user, post);
                postViewRepository.save(view);
                incrementProductViewCount(post);
                log.info("Зарегистрирован просмотр товара ID: {} пользователем (авторизован)", post.getId());
                return true;
            }
        } else {
            // Неавторизованный пользователь
            String ipAddress = getClientIp(request);
            String fingerprint = generateFingerprint(request);

            alreadyViewed = postViewRepository.existsByIpAndFingerprintAndPost(ipAddress, fingerprint, post);
            if (!alreadyViewed) {
                String sessionId = request.getSession(true).getId();
                PostView view = new PostView(post, ipAddress, fingerprint, sessionId);
                postViewRepository.save(view);
                incrementProductViewCount(post);
                log.info("Зарегистрирован просмотр товара ID: {} (неавторизованный, IP: {})", post.getId(), ipAddress);
                return true;
            }
        }

        log.debug("Повторный просмотр товара ID: {} не зарегистрирован", post.getId());
        return false;
    }

    /**
     * Регистрирует просмотр услуги.
     * <p>
     * Аналогичен {@link #registerProductView}, но для сущности {@link PostTextJob}.
     * </p>
     *
     * @param post    услуга
     * @param request HTTP-запрос
     * @param user    текущий пользователь (может быть null)
     * @return true если просмотр зарегистрирован
     */
    @Transactional
    public boolean registerServiceView(PostTextJob post, HttpServletRequest request, User user) {
        if (post == null) return false;

        if (user != null && user.getId().equals(post.getUser().getId())) {
            log.debug("Просмотр услуги ID: {} владельцем не зарегистрирован", post.getId());
            return false;
        }

        boolean alreadyViewed;

        if (user != null) {
            alreadyViewed = postViewRepository.existsByUserAndPostJob(user, post);
            if (!alreadyViewed) {
                PostView view = new PostView(user, post);
                postViewRepository.save(view);
                incrementServiceViewCount(post);
                log.info("Зарегистрирован просмотр услуги ID: {} пользователем (авторизован)", post.getId());
                return true;
            }
        } else {
            String ipAddress = getClientIp(request);
            String fingerprint = generateFingerprint(request);

            alreadyViewed = postViewRepository.existsByIpAndFingerprintAndPostJob(ipAddress, fingerprint, post);
            if (!alreadyViewed) {
                String sessionId = request.getSession(true).getId();
                PostView view = new PostView(post, ipAddress, fingerprint, sessionId);
                postViewRepository.save(view);
                incrementServiceViewCount(post);
                log.info("Зарегистрирован просмотр услуги ID: {} (неавторизованный, IP: {})", post.getId(), ipAddress);
                return true;
            }
        }

        log.debug("Повторный просмотр услуги ID: {} не зарегистрирован", post.getId());
        return false;
    }

    /**
     * Регистрирует просмотр вакансии.
     *
     * @param vacancy вакансия
     * @param request HTTP-запрос
     * @param user    текущий пользователь (может быть null)
     * @return true если просмотр зарегистрирован
     */
    @Transactional
    public boolean registerVacancyView(Vacancy vacancy, HttpServletRequest request, User user) {
        if (vacancy == null) return false;

        if (user != null && user.getId().equals(vacancy.getUser().getId())) {
            log.debug("Просмотр вакансии ID: {} владельцем не зарегистрирован", vacancy.getId());
            return false;
        }

        boolean alreadyViewed;

        if (user != null) {
            alreadyViewed = postViewRepository.existsByUserAndVacancy(user, vacancy);
            if (!alreadyViewed) {
                PostView view = new PostView(user, vacancy);
                postViewRepository.save(view);
                incrementVacancyViewCount(vacancy);
                log.info("Зарегистрирован просмотр вакансии ID: {} пользователем (авторизован)", vacancy.getId());
                return true;
            }
        } else {
            String ipAddress = getClientIp(request);
            String fingerprint = generateFingerprint(request);

            alreadyViewed = postViewRepository.existsByIpAndFingerprintAndVacancy(ipAddress, fingerprint, vacancy);
            if (!alreadyViewed) {
                String sessionId = request.getSession(true).getId();
                PostView view = new PostView(vacancy, ipAddress, fingerprint, sessionId);
                postViewRepository.save(view);
                incrementVacancyViewCount(vacancy);
                log.info("Зарегистрирован просмотр вакансии ID: {} (неавторизованный, IP: {})", vacancy.getId(), ipAddress);
                return true;
            }
        }

        log.debug("Повторный просмотр вакансии ID: {} не зарегистрирован", vacancy.getId());
        return false;
    }

    /**
     * Регистрирует просмотр резюме.
     *
     * @param resume  резюме
     * @param request HTTP-запрос
     * @param user    текущий пользователь (может быть null)
     * @return true если просмотр зарегистрирован
     */
    @Transactional
    public boolean registerResumeView(Resume resume, HttpServletRequest request, User user) {
        if (resume == null) return false;

        if (user != null && user.getId().equals(resume.getUser().getId())) {
            log.debug("Просмотр резюме ID: {} владельцем не зарегистрирован", resume.getId());
            return false;
        }

        boolean alreadyViewed;

        if (user != null) {
            alreadyViewed = postViewRepository.existsByUserAndResume(user, resume);
            if (!alreadyViewed) {
                PostView view = new PostView(user, resume);
                postViewRepository.save(view);
                incrementResumeViewCount(resume);
                log.info("Зарегистрирован просмотр резюме ID: {} пользователем (авторизован)", resume.getId());
                return true;
            }
        } else {
            String ipAddress = getClientIp(request);
            String fingerprint = generateFingerprint(request);

            alreadyViewed = postViewRepository.existsByIpAndFingerprintAndResume(ipAddress, fingerprint, resume);
            if (!alreadyViewed) {
                String sessionId = request.getSession(true).getId();
                PostView view = new PostView(resume, ipAddress, fingerprint, sessionId);
                postViewRepository.save(view);
                incrementResumeViewCount(resume);
                log.info("Зарегистрирован просмотр резюме ID: {} (неавторизованный, IP: {})", resume.getId(), ipAddress);
                return true;
            }
        }

        log.debug("Повторный просмотр резюме ID: {} не зарегистрирован", resume.getId());
        return false;
    }

    /**
     * Увеличивает счётчик просмотров товара на 1.
     *
     * @param post товар
     */
    private void incrementProductViewCount(PostText post) {
        post.setViewCount(post.getViewCount() + 1);
        postRepo.save(post);
    }

    /**
     * Увеличивает счётчик просмотров услуги на 1.
     *
     * @param post услуга
     */
    private void incrementServiceViewCount(PostTextJob post) {
        post.setViewCount(post.getViewCount() + 1);
        postRepoJob.save(post);
    }

    /**
     * Увеличивает счётчик просмотров вакансии на 1.
     *
     * @param vacancy вакансия
     */
    private void incrementVacancyViewCount(Vacancy vacancy) {
        vacancy.setViewCount(vacancy.getViewCount() + 1);
        postVacancyRepository.save(vacancy);
    }

    /**
     * Увеличивает счётчик просмотров резюме на 1.
     *
     * @param resume резюме
     */
    private void incrementResumeViewCount(Resume resume) {
        resume.setViewCount(resume.getViewCount() + 1);
        postResumeRepository.save(resume);
    }

    /**
     * Возвращает общее количество просмотров товара.
     *
     * @param post товар
     * @return количество просмотров
     */
    public long getProductViewCount(PostText post) {
        return post == null ? 0 : postViewRepository.countByPost(post);
    }

    /**
     * Возвращает общее количество просмотров услуги.
     *
     * @param post услуга
     * @return количество просмотров
     */
    public long getServiceViewCount(PostTextJob post) {
        return post == null ? 0 : postViewRepository.countByPostJob(post);
    }

    /**
     * Возвращает общее количество просмотров вакансии.
     *
     * @param vacancy вакансия
     * @return количество просмотров
     */
    public long getVacancyViewCount(Vacancy vacancy) {
        return vacancy == null ? 0 : postViewRepository.countByVacancy(vacancy);
    }

    /**
     * Возвращает общее количество просмотров резюме.
     *
     * @param resume резюме
     * @return количество просмотров
     */
    public long getResumeViewCount(Resume resume) {
        return resume == null ? 0 : postViewRepository.countByResume(resume);
    }
}