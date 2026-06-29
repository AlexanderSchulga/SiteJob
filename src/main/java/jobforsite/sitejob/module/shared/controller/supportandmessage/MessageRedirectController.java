package jobforsite.sitejob.module.shared.controller.supportandmessage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Контроллер для перенаправления пользователей по ссылкам из уведомлений.
 * Обрабатывает переходы к объявлениям (товары, услуги, вакансии, резюме)
 * с проверкой доступности объекта и отметкой сообщений как прочитанных.
 */
@RequiredArgsConstructor
@Controller
public class MessageRedirectController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(MessageRedirectController.class);

    // Константы ограничения частоты запросов (rate limiting)
    private static final int RATE_LIMIT = 10;           // Максимум запросов за окно
    private static final int RATE_LIMIT_WINDOW = 60;    // Окно в секундах

    // Репозитории для разных типов объявлений
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    // Репозитории для комментариев/сообщений
    private final CommentRepoPersonal commentRepoPersonal;
    private final CommentRepoJobPersonal commentRepoJobPersonal;
    private final CommentRepoVacPersonal commentRepoVacPersonal;
    private final CommentRepoResPersonal commentRepoResPersonal;

    // Дополнительные зависимости
    private final MessageTicketController messageTicketController;

    // In-memory кэш для ограничения частоты запросов (на основе userId или анонимного ключа)
    private Cache<String, RateLimitInfo> rateLimitCache;

    /**
     * Инициализация кэша rate limiting после создания бина.
     * Создаёт Caffeine-кэш с автоматическим истечением записей.
     */
    @PostConstruct
    public void init() {
        rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(RATE_LIMIT_WINDOW, TimeUnit.SECONDS)
                .maximumSize(10000) // Ограничиваем максимальное количество записей в кэше
                .build();
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ-РЕДИРЕКТЫ ====================

    /**
     * Перенаправление к объявлению типа "товар/услуга" (Post).
     * Проверяет доступность объявления, отмечает сообщение как прочитанное,
     * выполняет rate limiting.
     *
     * @param hashId Уникальный идентификатор объявления
     * @param messageId ID сообщения, которое нужно отметить как прочитанное (опционально)
     * @param authentication Объект аутентификации Spring Security
     * @param redirectAttributes Атрибуты для flash-сообщений при редиректе
     * @return Название view для редиректа
     */
    @GetMapping("/message/post/{hashId}")
    public String redirectToPost(
            @PathVariable String hashId,
            @RequestParam(required = false) Long messageId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        try {
            // Rate limiting check
            RateLimitCheckResult rateLimitResult = checkRateLimit(authentication);
            if (!rateLimitResult.allowed) {
                logger.warn("Rate limit exceeded for user request. Retry after: {} seconds", rateLimitResult.retryAfter);
                addFlashMessage(redirectAttributes, "warning",
                        "Слишком много запросов",
                        "Пожалуйста, подождите " + rateLimitResult.retryAfter + " секунд");
                return "redirect:/user/notifications_message";
            }

            // Проверка аутентификации
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.debug("Unauthenticated access attempt to post redirect");
                return "redirect:/login";
            }

            User currentUser = (User) authentication.getPrincipal();
            // Логируем только роль, не id и не username
            logger.debug("User with role {} requested redirect to post: {}", currentUser.getRole(), hashId);

            // Сначала проверяем статус поста (c кэшированием)
            PostStatus status = checkPostStatusCached(hashId);

            if (status == null) {
                logger.info("Post not found for hashId: {}", hashId);

                // Отмечаем сообщение как прочитанное СИНХРОННО перед редиректом
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "product");
                }

                addFlashMessage(redirectAttributes, "error",
                        "Объявление не найдено", "Объявление было удалено");
                return "redirect:/user/notifications_message";
            }

            if (!status.isAvailable()) {
                logger.info("Post unavailable: hashId={}, reason={}", hashId, status.getReason());

                // Отмечаем сообщение как прочитанное СИНХРОННО перед редиректом
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "product");
                }

                addFlashMessage(redirectAttributes,
                        status.getMessageType(),
                        status.getMessageTitle(),
                        status.getMessageText());
                return "redirect:/user/notifications_message";
            }

            // Пост доступен - отмечаем сообщение как прочитанное и редиректим на пост
            if (messageId != null) {
                markMessageAsReadSync(messageId, currentUser.getId(), "product");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Redirect to post {} completed in {} ms", hashId, duration);

            return "redirect:/postidviews/" + hashId;

        } catch (Exception e) {
            logger.error("Error processing redirect for post {}: {}", hashId, e.getClass().getSimpleName(), e);
            addFlashMessage(redirectAttributes, "error",
                    "Ошибка", "Произошла ошибка при обработке запроса");
            return "redirect:/user/notifications_message";
        }
    }

    /**
     * Перенаправление к объявлению типа "услуга" (Service).
     *
     * @param hashId Уникальный идентификатор услуги
     * @param messageId ID сообщения для отметки прочитанным
     * @param authentication Объект аутентификации
     * @param redirectAttributes Атрибуты для flash-сообщений
     * @return Название view для редиректа
     */
    @GetMapping("/message/service/{hashId}")
    public String redirectToService(
            @PathVariable String hashId,
            @RequestParam(required = false) Long messageId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        try {
            RateLimitCheckResult rateLimitResult = checkRateLimit(authentication);
            if (!rateLimitResult.allowed) {
                logger.warn("Rate limit exceeded for service redirect. Retry after: {} seconds", rateLimitResult.retryAfter);
                addFlashMessage(redirectAttributes, "warning",
                        "Слишком много запросов",
                        "Пожалуйста, подождите " + rateLimitResult.retryAfter + " секунд");
                return "redirect:/user/notifications_message";
            }

            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            User currentUser = (User) authentication.getPrincipal();
            logger.debug("User with role {} requested redirect to service: {}", currentUser.getRole(), hashId);

            // Сначала проверяем статус услуги (c кэшированием)
            PostServiceStatus status = checkServiceStatusCached(hashId);

            if (status == null) {
                logger.info("Service not found: {}", hashId);
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "service");
                }
                addFlashMessage(redirectAttributes, "error",
                        "Объявление не найдено", "Объявление было удалено");
                return "redirect:/user/notifications_message";
            }

            if (!status.isAvailable()) {
                logger.info("Service unavailable: hashId={}, reason={}", hashId, status.getReason());
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "service");
                }
                addFlashMessage(redirectAttributes,
                        status.getMessageType(),
                        status.getMessageTitle(),
                        status.getMessageText());
                return "redirect:/user/notifications_message";
            }

            // Услуга доступна - отмечаем сообщение как прочитанное и редиректим
            if (messageId != null) {
                markMessageAsReadSync(messageId, currentUser.getId(), "service");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Redirect to service {} completed in {} ms", hashId, duration);

            return "redirect:/postidviewsjob/" + hashId;

        } catch (Exception e) {
            logger.error("Error processing redirect for service {}: {}", hashId, e.getClass().getSimpleName(), e);
            addFlashMessage(redirectAttributes, "error",
                    "Ошибка", "Произошла ошибка при обработке запроса");
            return "redirect:/user/notifications_message";
        }
    }

    /**
     * Перенаправление к объявлению типа "вакансия" (Vacancy).
     *
     * @param hashId Уникальный идентификатор вакансии
     * @param messageId ID сообщения для отметки прочитанным
     * @param authentication Объект аутентификации
     * @param redirectAttributes Атрибуты для flash-сообщений
     * @return Название view для редиректа
     */
    @GetMapping("/message/vacancy/{hashId}")
    public String redirectToVacancy(
            @PathVariable String hashId,
            @RequestParam(required = false) Long messageId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        try {
            RateLimitCheckResult rateLimitResult = checkRateLimit(authentication);
            if (!rateLimitResult.allowed) {
                logger.warn("Rate limit exceeded for vacancy redirect. Retry after: {} seconds", rateLimitResult.retryAfter);
                addFlashMessage(redirectAttributes, "warning",
                        "Слишком много запросов",
                        "Пожалуйста, подождите " + rateLimitResult.retryAfter + " секунд");
                return "redirect:/user/notifications_message";
            }

            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            User currentUser = (User) authentication.getPrincipal();
            logger.debug("User with role {} requested redirect to vacancy: {}", currentUser.getRole(), hashId);

            // Сначала проверяем статус вакансии (c кэшированием)
            PostVacancyStatus status = checkVacancyStatusCached(hashId);

            if (status == null) {
                logger.info("Vacancy not found: {}", hashId);
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "vacancy");
                }
                addFlashMessage(redirectAttributes, "error",
                        "Объявление не найдено", "Объявление было удалено");
                return "redirect:/user/notifications_message";
            }

            if (!status.isAvailable()) {
                logger.info("Vacancy unavailable: hashId={}, reason={}", hashId, status.getReason());
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "vacancy");
                }
                addFlashMessage(redirectAttributes,
                        status.getMessageType(),
                        status.getMessageTitle(),
                        status.getMessageText());
                return "redirect:/user/notifications_message";
            }

            // Вакансия доступна - отмечаем сообщение как прочитанное и редиректим
            if (messageId != null) {
                markMessageAsReadSync(messageId, currentUser.getId(), "vacancy");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Redirect to vacancy {} completed in {} ms", hashId, duration);

            return "redirect:/postidviewsvac/" + hashId;

        } catch (Exception e) {
            logger.error("Error processing redirect for vacancy {}: {}", hashId, e.getClass().getSimpleName(), e);
            addFlashMessage(redirectAttributes, "error",
                    "Ошибка", "Произошла ошибка при обработке запроса");
            return "redirect:/user/notifications_message";
        }
    }

    /**
     * Перенаправление к объявлению типа "резюме" (Resume).
     *
     * @param hashId Уникальный идентификатор резюме
     * @param messageId ID сообщения для отметки прочитанным
     * @param authentication Объект аутентификации
     * @param redirectAttributes Атрибуты для flash-сообщений
     * @return Название view для редиректа
     */
    @GetMapping("/message/resume/{hashId}")
    public String redirectToResume(
            @PathVariable String hashId,
            @RequestParam(required = false) Long messageId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        long startTime = System.currentTimeMillis();

        try {
            RateLimitCheckResult rateLimitResult = checkRateLimit(authentication);
            if (!rateLimitResult.allowed) {
                logger.warn("Rate limit exceeded for resume redirect. Retry after: {} seconds", rateLimitResult.retryAfter);
                addFlashMessage(redirectAttributes, "warning",
                        "Слишком много запросов",
                        "Пожалуйста, подождите " + rateLimitResult.retryAfter + " секунд");
                return "redirect:/user/notifications_message";
            }

            if (authentication == null || !authentication.isAuthenticated()) {
                return "redirect:/login";
            }

            User currentUser = (User) authentication.getPrincipal();
            logger.debug("User with role {} requested redirect to resume: {}", currentUser.getRole(), hashId);

            // Сначала проверяем статус резюме (c кэшированием)
            PostResumeStatus status = checkResumeStatusCached(hashId);

            if (status == null) {
                logger.info("Resume not found: {}", hashId);
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "resume");
                }
                addFlashMessage(redirectAttributes, "error",
                        "Объявление не найдено", "Объявление было удалено");
                return "redirect:/user/notifications_message";
            }

            if (!status.isAvailable()) {
                logger.info("Resume unavailable: hashId={}, reason={}", hashId, status.getReason());
                if (messageId != null) {
                    markMessageAsReadSync(messageId, currentUser.getId(), "resume");
                }
                addFlashMessage(redirectAttributes,
                        status.getMessageType(),
                        status.getMessageTitle(),
                        status.getMessageText());
                return "redirect:/user/notifications_message";
            }

            // Резюме доступно - отмечаем сообщение как прочитанное и редиректим
            if (messageId != null) {
                markMessageAsReadSync(messageId, currentUser.getId(), "resume");
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Redirect to resume {} completed in {} ms", hashId, duration);

            return "redirect:/postidviewsres/" + hashId;

        } catch (Exception e) {
            logger.error("Error processing redirect for resume {}: {}", hashId, e.getClass().getSimpleName(), e);
            addFlashMessage(redirectAttributes, "error",
                    "Ошибка", "Произошла ошибка при обработке запроса");
            return "redirect:/user/notifications_message";
        }
    }

    // ==================== МЕТОДЫ ПРОВЕРКИ СТАТУСОВ С КЭШИРОВАНИЕМ ====================

    /**
     * Проверяет статус доступности объявления (PostText) по hashId.
     * Результат кэшируется в Spring Cache "postStatus".
     *
     * @param hashId Уникальный идентификатор объявления
     * @return PostStatus с информацией о доступности, или null если объявление не найдено
     */
    @Cacheable(value = "postStatus", key = "#hashId", unless = "#result == null")
    public PostStatus checkPostStatusCached(String hashId) {
        Optional<PostText> postOpt = postRepo.findByHashId(hashId);

        if (postOpt.isEmpty()) {
            return null;
        }

        PostText post = postOpt.get();
        User author = post.getUser();

        // Проверки статуса автора и объявления
        if (author == null || !author.isActive() || author.isDeleted()) {
            return new PostStatus(false, "error",
                    "Пользователь заблокирован",
                    "Пользователь, создавший объявление, заблокирован");
        }
        if (post.isBlocked()) {
            return new PostStatus(false, "warning",
                    "Объявление заблокировано",
                    "Объявление заблокировано администрацией");
        }
        if (post.isArchived()) {
            return new PostStatus(false, "info",
                    "Объявление в архиве",
                    "Объявление находится в архиве");
        }
        if (post.isModeration()) {
            return new PostStatus(false, "info",
                    "На модерации",
                    "Объявление проходит модерацию");
        }

        // Объявление доступно
        return new PostStatus(true, null, null, null);
    }

    /**
     * Проверяет статус доступности услуги (PostTextJob) по hashId.
     * Результат кэшируется в Spring Cache "serviceStatus".
     *
     * @param hashId Уникальный идентификатор услуги
     * @return PostServiceStatus с информацией о доступности, или null если услуга не найдена
     */
    @Cacheable(value = "serviceStatus", key = "#hashId", unless = "#result == null")
    public PostServiceStatus checkServiceStatusCached(String hashId) {
        Optional<PostTextJob> serviceOpt = postRepoJob.findByHashId(hashId);

        if (serviceOpt.isEmpty()) {
            return null;
        }

        PostTextJob service = serviceOpt.get();
        User author = service.getUser();

        if (author == null || !author.isActive() || author.isDeleted()) {
            return new PostServiceStatus(false, "error",
                    "Пользователь заблокирован",
                    "Пользователь, создавший объявление, заблокирован");
        }
        if (service.isBlocked()) {
            return new PostServiceStatus(false, "warning",
                    "Объявление заблокировано",
                    "Объявление заблокировано администрацией");
        }
        if (service.isArchived()) {
            return new PostServiceStatus(false, "info",
                    "Объявление в архиве",
                    "Объявление находится в архиве");
        }
        if (service.isModeration()) {
            return new PostServiceStatus(false, "info",
                    "На модерации",
                    "Объявление проходит модерацию");
        }

        return new PostServiceStatus(true, null, null, null);
    }

    /**
     * Проверяет статус доступности вакансии (Vacancy) по hashId.
     * Результат кэшируется в Spring Cache "vacancyStatus".
     *
     * @param hashId Уникальный идентификатор вакансии
     * @return PostVacancyStatus с информацией о доступности, или null если вакансия не найдена
     */
    @Cacheable(value = "vacancyStatus", key = "#hashId", unless = "#result == null")
    public PostVacancyStatus checkVacancyStatusCached(String hashId) {
        Optional<Vacancy> vacancyOpt = postVacancyRepository.findByHashId(hashId);

        if (vacancyOpt.isEmpty()) {
            return null;
        }

        Vacancy vacancy = vacancyOpt.get();
        User author = vacancy.getUser();

        if (author == null || !author.isActive() || author.isDeleted()) {
            return new PostVacancyStatus(false, "error",
                    "Пользователь заблокирован",
                    "Пользователь, создавший объявление, заблокирован");
        }
        if (vacancy.isBlocked()) {
            return new PostVacancyStatus(false, "warning",
                    "Объявление заблокировано",
                    "Объявление заблокировано администрацией");
        }
        if (vacancy.isArchived()) {
            return new PostVacancyStatus(false, "info",
                    "Объявление в архиве",
                    "Объявление находится в архиве");
        }
        if (vacancy.isModeration()) {
            return new PostVacancyStatus(false, "info",
                    "На модерации",
                    "Объявление проходит модерацию");
        }

        return new PostVacancyStatus(true, null, null, null);
    }

    /**
     * Проверяет статус доступности резюме (Resume) по hashId.
     * Результат кэшируется в Spring Cache "resumeStatus".
     *
     * @param hashId Уникальный идентификатор резюме
     * @return PostResumeStatus с информацией о доступности, или null если резюме не найдено
     */
    @Cacheable(value = "resumeStatus", key = "#hashId", unless = "#result == null")
    public PostResumeStatus checkResumeStatusCached(String hashId) {
        Optional<Resume> resumeOpt = postResumeRepository.findByHashId(hashId);

        if (resumeOpt.isEmpty()) {
            return null;
        }

        Resume res = resumeOpt.get();
        User author = res.getUser();

        if (author == null || !author.isActive() || author.isDeleted()) {
            return new PostResumeStatus(false, "error",
                    "Пользователь заблокирован",
                    "Пользователь, создавший объявление, заблокирован");
        }
        if (res.isBlocked()) {
            return new PostResumeStatus(false, "warning",
                    "Объявление заблокировано",
                    "Объявление заблокировано администрацией");
        }
        if (res.isArchived()) {
            return new PostResumeStatus(false, "info",
                    "Объявление в архиве",
                    "Объявление находится в архиве");
        }
        if (res.isModeration()) {
            return new PostResumeStatus(false, "info",
                    "На модерации",
                    "Объявление проходит модерацию");
        }

        return new PostResumeStatus(true, null, null, null);
    }

    /**
     * Очищает все кэши статусов для указанного hashId.
     * Используется при изменении статуса объявления.
     *
     * @param hashId Уникальный идентификатор объявления
     */
    @CacheEvict(value = {"postStatus", "serviceStatus", "vacancyStatus", "resumeStatus"}, key = "#hashId")
    public void evictAllStatusCaches(String hashId) {
        logger.debug("Cache evicted for all statuses with hashId: {}", hashId);
    }

    // ==================== РАБОТА С СООБЩЕНИЯМИ ====================

    /**
     * Синхронно отмечает сообщение как прочитанное.
     * Обновляет соответствующую запись в БД и инвалидирует кэши сообщений.
     *
     * @param messageId ID сообщения
     * @param userId ID пользователя-получателя (используется только для проверки прав)
     * @param type Тип объявления: "product", "service", "vacancy", "resume"
     */
    private void markMessageAsReadSync(Long messageId, Long userId, String type) {
        try {
            switch (type) {
                case "product":
                    commentRepoPersonal.findById(messageId).ifPresent(comment -> {
                        if (!comment.getIsRead() && comment.getRecipient() != null
                                && comment.getRecipient().getId().equals(userId)) {
                            comment.setIsRead(true);
                            commentRepoPersonal.save(comment);
                            logger.debug("Product message {} marked as read", messageId);
                            evictMessageCaches(userId);
                        }
                    });
                    break;
                case "service":
                    commentRepoJobPersonal.findById(messageId).ifPresent(comment -> {
                        if (!comment.getRead() && comment.getRecipient() != null
                                && comment.getRecipient().getId().equals(userId)) {
                            comment.setRead(true);
                            commentRepoJobPersonal.save(comment);
                            logger.debug("Service message {} marked as read", messageId);
                            evictMessageCaches(userId);
                        }
                    });
                    break;
                case "vacancy":
                    commentRepoVacPersonal.findById(messageId).ifPresent(comment -> {
                        if (!comment.getRead() && comment.getRecipient() != null
                                && comment.getRecipient().getId().equals(userId)) {
                            comment.setRead(true);
                            commentRepoVacPersonal.save(comment);
                            logger.debug("Vacancy message {} marked as read", messageId);
                            evictMessageCaches(userId);
                        }
                    });
                    break;
                case "resume":
                    commentRepoResPersonal.findById(messageId).ifPresent(comment -> {
                        if (!comment.getRead() && comment.getRecipient() != null
                                && comment.getRecipient().getId().equals(userId)) {
                            comment.setRead(true);
                            commentRepoResPersonal.save(comment);
                            logger.debug("Resume message {} marked as read", messageId);
                            evictMessageCaches(userId);
                        }
                    });
                    break;
                default:
                    logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Failed to mark message as read synchronously: messageId={}, type={}", messageId, type, e);
        }
    }

    /**
     * Инвалидирует кэши сообщений для указанного пользователя.
     * Вызывает методы контроллера сообщений для очистки.
     *
     * @param userId ID пользователя
     */
    private void evictMessageCaches(Long userId) {
        try {
            messageTicketController.evictUnreadCountCache(userId);
            messageTicketController.evictPostIdsCache(userId, "product");
            messageTicketController.evictPostIdsCache(userId, "service");
            messageTicketController.evictPostIdsCache(userId, "vacancy");
            messageTicketController.evictPostIdsCache(userId, "resume");
        } catch (Exception e) {
            logger.error("Error evicting message caches for {}", e.getClass().getSimpleName());
        }
    }

    /**
     * Асинхронный вариант отметки сообщения как прочитанного.
     * Оставлен для обратной совместимости, внутри вызывает синхронный метод.
     *
     * @param messageId ID сообщения
     * @param userId ID пользователя
     * @param type Тип объявления
     */
    private void markMessageAsReadAsync(Long messageId, Long userId, String type) {
        CompletableFuture.runAsync(() -> markMessageAsReadSync(messageId, userId, type));
    }

    // ==================== RATE LIMITING ====================

    /**
     * Проверяет превышение лимита запросов для пользователя.
     * Использует in-memory Caffeine кэш для хранения счётчиков.
     *
     * @param authentication Объект аутентификации (может быть null для анонимных)
     * @return RateLimitCheckResult с информацией о разрешении и времени ожидания
     */
    private RateLimitCheckResult checkRateLimit(Authentication authentication) {
        String userId = authentication != null ? authentication.getName() : "anonymous";

        // Для неаутентифицированных запросов ограничение не применяем (или можно применить - на ваше усмотрение)
        if (authentication == null) {
            return new RateLimitCheckResult(true, userId, 0);
        }

        String key = "rate_limit:" + userId;
        long now = System.currentTimeMillis() / 1000; // текущее время в секундах

        RateLimitInfo info = rateLimitCache.getIfPresent(key);

        if (info == null) {
            // Первый запрос пользователя
            rateLimitCache.put(key, new RateLimitInfo(1, now));
            return new RateLimitCheckResult(true, userId, 0);
        }

        // Проверяем, не истекло ли окно
        if (now - info.windowStart > RATE_LIMIT_WINDOW) {
            // Окно истекло, сбрасываем счетчик
            rateLimitCache.put(key, new RateLimitInfo(1, now));
            return new RateLimitCheckResult(true, userId, 0);
        }

        // Проверяем лимит
        if (info.count >= RATE_LIMIT) {
            long retryAfter = RATE_LIMIT_WINDOW - (now - info.windowStart);
            return new RateLimitCheckResult(false, userId, retryAfter > 0 ? retryAfter : 0);
        }

        // Увеличиваем счетчик
        info.count++;
        rateLimitCache.put(key, info);
        return new RateLimitCheckResult(true, userId, 0);
    }

    /**
     * Вспомогательный метод для добавления flash-сообщения при редиректе.
     *
     * @param redirectAttributes Атрибуты редиректа
     * @param type Тип сообщения (error, warning, info, success)
     * @param title Заголовок сообщения
     * @param text Текст сообщения
     */
    private void addFlashMessage(RedirectAttributes redirectAttributes,
                                 String type, String title, String text) {
        redirectAttributes.addFlashAttribute("messageType", type);
        redirectAttributes.addFlashAttribute("messageTitle", title);
        redirectAttributes.addFlashAttribute("messageText", text);
    }

    // ==================== ВНУТРЕННИЕ КЛАССЫ ДЛЯ СТАТУСОВ ====================

    /**
     * Статус доступности объявления (PostText).
     */
    private static class PostStatus {
        private final boolean available;
        private final String messageType;
        private final String messageTitle;
        private final String messageText;
        private final String reason;

        PostStatus(boolean available, String messageType, String messageTitle, String messageText) {
            this.available = available;
            this.messageType = messageType;
            this.messageTitle = messageTitle;
            this.messageText = messageText;
            this.reason = messageTitle;
        }

        public boolean isAvailable() { return available; }
        public String getMessageType() { return messageType; }
        public String getMessageTitle() { return messageTitle; }
        public String getMessageText() { return messageText; }
        public String getReason() { return reason; }
    }

    /** Статус доступности услуги (PostTextJob). */
    private static class PostServiceStatus {
        private final boolean available;
        private final String messageType;
        private final String messageTitle;
        private final String messageText;
        private final String reason;

        PostServiceStatus(boolean available, String messageType, String messageTitle, String messageText) {
            this.available = available;
            this.messageType = messageType;
            this.messageTitle = messageTitle;
            this.messageText = messageText;
            this.reason = messageTitle;
        }

        public boolean isAvailable() { return available; }
        public String getMessageType() { return messageType; }
        public String getMessageTitle() { return messageTitle; }
        public String getMessageText() { return messageText; }
        public String getReason() { return reason; }
    }

    /** Статус доступности вакансии (Vacancy). */
    private static class PostVacancyStatus {
        private final boolean available;
        private final String messageType;
        private final String messageTitle;
        private final String messageText;
        private final String reason;

        PostVacancyStatus(boolean available, String messageType, String messageTitle, String messageText) {
            this.available = available;
            this.messageType = messageType;
            this.messageTitle = messageTitle;
            this.messageText = messageText;
            this.reason = messageTitle;
        }

        public boolean isAvailable() { return available; }
        public String getMessageType() { return messageType; }
        public String getMessageTitle() { return messageTitle; }
        public String getMessageText() { return messageText; }
        public String getReason() { return reason; }
    }

    /** Статус доступности резюме (Resume). */
    private static class PostResumeStatus {
        private final boolean available;
        private final String messageType;
        private final String messageTitle;
        private final String messageText;
        private final String reason;

        PostResumeStatus(boolean available, String messageType, String messageTitle, String messageText) {
            this.available = available;
            this.messageType = messageType;
            this.messageTitle = messageTitle;
            this.messageText = messageText;
            this.reason = messageTitle;
        }

        public boolean isAvailable() { return available; }
        public String getMessageType() { return messageType; }
        public String getMessageTitle() { return messageTitle; }
        public String getMessageText() { return messageText; }
        public String getReason() { return reason; }
    }

    /**
     * Информация для rate limiting (хранится в кэше).
     */
    private static class RateLimitInfo {
        int count;
        long windowStart;

        RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    /**
     * Результат проверки rate limiting.
     */
    private static class RateLimitCheckResult {
        boolean allowed;
        String userId;
        long retryAfter;

        RateLimitCheckResult(boolean allowed, String userId, long retryAfter) {
            this.allowed = allowed;
            this.userId = userId;
            this.retryAfter = retryAfter;
        }
    }
}