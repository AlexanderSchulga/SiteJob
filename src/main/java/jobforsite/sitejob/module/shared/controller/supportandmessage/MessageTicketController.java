package jobforsite.sitejob.module.shared.controller.supportandmessage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jobforsite.sitejob.module.product.model.CommentPersonal;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.repository.CommentRepoPersonal;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.resume.model.CommentPersonalRes;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.CommentRepoResPersonal;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.CommentPersonalJob;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.service.repository.CommentRepoJobPersonal;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.shared.dto.MessageTicketDTO;
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Контроллер для управления сообщениями и тикетами пользователя.
 * Обеспечивает функциональность:
 * - Загрузка списка тикетов с непрочитанными сообщениями
 * - Подсчёт количества непрочитанных сообщений
 * - Отметка всех сообщений как прочитанных
 * - Rate limiting для защиты от чрезмерных запросов
 * - Кэширование результатов для оптимизации производительности
 */
@RequiredArgsConstructor
@Controller
public class MessageTicketController extends BaseController {

    // Репозитории для комментариев разных типов объявлений
    private final CommentRepoPersonal commentRepoPersonal;           // Товары
    private final CommentRepoJobPersonal commentRepoJobPersonal;     // Услуги
    private final CommentRepoVacPersonal commentRepoVacPersonal;     // Вакансии
    private final CommentRepoResPersonal commentRepoResPersonal;     // Резюме

    // Репозитории для объявлений
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    private static final Logger logger = LoggerFactory.getLogger(MessageTicketController.class);

    // Константы rate limiting для разных типов действий
    private static final int RATE_LIMIT_LOAD = 20;       // Лимит для загрузки тикетов
    private static final int RATE_LIMIT_COUNT = 30;      // Лимит для подсчёта непрочитанных
    private static final int RATE_LIMIT_MARK_ALL = 5;    // Лимит для отметки всех как прочитанных
    private static final int RATE_LIMIT_WINDOW = 60;     // Окно rate limiting в секундах

    private Cache<String, RateLimitInfo> rateLimitCache;

    /**
     * Инициализация кэша для rate limiting.
     * Создаёт Caffeine-кэш с автоматическим истечением записей.
     */
    @PostConstruct
    public void init() {
        rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(RATE_LIMIT_WINDOW, TimeUnit.SECONDS)
                .maximumSize(10000)
                .recordStats()  // Включаем сбор статистики для мониторинга
                .build();
    }

    // ==================== ENDPOINTS ====================

    /**
     * Получение статистики кэша rate limiting.
     * Доступно только для администраторов.
     *
     * @return Статистика кэша
     */
    @GetMapping("/admin/cache-stats")
    @ResponseBody
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("rateLimitCache", rateLimitCache.stats());
        return stats;
    }

    /**
     * Отображает страницу с уведомлениями/сообщениями пользователя.
     *
     * @param authentication Объект аутентификации
     * @param model Модель для передачи данных в представление
     * @return Название представления или редирект на логин
     */
    @GetMapping("/user/notifications_message")
    public String getMessageTicketsPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = (User) authentication.getPrincipal();
        model.addAttribute("currentUser", currentUser);

        return "user/notifications_message";
    }

    /**
     * Загружает тикеты с непрочитанными сообщениями для текущего пользователя.
     * Поддерживает пагинацию и сортировку по времени сообщения.
     *
     * @param authentication Объект аутентификации
     * @param page Номер страницы (начиная с 0)
     * @param size Количество элементов на странице
     * @return Карта с данными тикетов или ошибкой
     */
    @GetMapping("/user/notifications_message/load")
    @ResponseBody
    public Map<String, Object> loadMessageTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {

        // Проверка rate limiting
        if (!checkRateLimit(authentication, "load", RATE_LIMIT_LOAD)) {
            return createRateLimitResponse();
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.singletonMap("error", "Not authenticated");
        }

        User currentUser = (User) authentication.getPrincipal();
        long startTime = System.currentTimeMillis();

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "messageTime"));
            Slice<MessageTicketDTO> ticketSlice = getMessageTicketsSlice(currentUser, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("tickets", ticketSlice.getContent());
            response.put("hasNext", ticketSlice.hasNext());
            response.put("currentPage", page);
            response.put("totalElements", ticketSlice.getNumberOfElements());

            long duration = System.currentTimeMillis() - startTime;
            // Логируем только роль и количество, без id/username
            logger.debug("Loaded {} tickets for user with role {} in {} ms",
                    ticketSlice.getContent().size(), currentUser.getRole(), duration);

            return response;
        } catch (Exception e) {
            logger.error("Error loading tickets: {}", e.getClass().getSimpleName(), e);
            return Collections.singletonMap("error", "Failed to load tickets");
        }
    }

    /**
     * Возвращает общее количество непрочитанных сообщений для пользователя.
     * Результат кэшируется для оптимизации.
     *
     * @param authentication Объект аутентификации
     * @return ResponseEntity с количеством непрочитанных сообщений
     */
    @GetMapping("/user/notifications_message/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUnreadMessagesCount(Authentication authentication) {
        // Проверка rate limiting
        if (!checkRateLimit(authentication, "count", RATE_LIMIT_COUNT)) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "count", 0,
                    "error", "Rate limit exceeded"
            ));
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("success", false, "count", 0));
        }

        User currentUser = (User) authentication.getPrincipal();
        long startTime = System.currentTimeMillis();

        try {
            int totalUnread = getCachedUnreadCount(currentUser);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Counted {} unread messages for user with role {} in {} ms",
                    totalUnread, currentUser.getRole(), duration);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", totalUnread
            ));
        } catch (Exception e) {
            logger.error("Error counting unread messages: {}", e.getClass().getSimpleName(), e);
            return ResponseEntity.ok(Map.of("success", false, "count", 0));
        }
    }

    /**
     * Отмечает все непрочитанные сообщения пользователя как прочитанные.
     * Работает асинхронно для быстрого ответа клиенту.
     * После завершения инвалидирует соответствующие кэши.
     *
     * @param authentication Объект аутентификации
     * @return ResponseEntity с результатом операции
     */
    @PostMapping("/user/notifications_message/mark-all-read")
    @ResponseBody
    @CacheEvict(value = {"unreadCounts", "postIdsWithUnread"}, key = "#authentication.principal.id")
    public ResponseEntity<Map<String, Object>> markAllMessagesAsRead(Authentication authentication) {
        // Проверка rate limiting
        if (!checkRateLimit(authentication, "mark-all", RATE_LIMIT_MARK_ALL)) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Rate limit exceeded. Please try again later."
            ));
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of("success", false));
        }

        User currentUser = (User) authentication.getPrincipal();
        long startTime = System.currentTimeMillis();

        try {
            // Асинхронно отмечаем сообщения как прочитанные (не блокируем ответ)
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                markAllMessagesAsReadAsync(currentUser.getId());
            });

            // Логируем ошибки асинхронной операции
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Error marking messages as read asynchronously: {}", ex.getClass().getSimpleName(), ex);
                }
            });

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Mark all messages as read request completed in {} ms for user with role {}",
                    duration, currentUser.getRole());

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Error marking all messages as read: {}", e.getClass().getSimpleName(), e);
            return ResponseEntity.ok(Map.of("success", false));
        }
    }

    // ==================== КЭШИРУЕМЫЕ МЕТОДЫ ====================

    /**
     * Получает общее количество непрочитанных сообщений для пользователя.
     * Результат кэшируется в Spring Cache "unreadCounts".
     *
     * @param user Пользователь
     * @return Общее количество непрочитанных сообщений
     */
    @Cacheable(value = "unreadCounts", key = "#user.id", unless = "#result == null")
    public int getCachedUnreadCount(User user) {
        int totalUnread = 0;

        // Суммируем непрочитанные сообщения по всем типам объявлений
        List<Long> productPostIds = getCachedPostIdsWithUnread(user.getId(), "product");
        for (Long postId : productPostIds) {
            totalUnread += commentRepoPersonal.findUnreadMessages(postId, user.getId()).size();
        }

        List<Long> servicePostIds = getCachedPostIdsWithUnread(user.getId(), "service");
        for (Long postId : servicePostIds) {
            totalUnread += commentRepoJobPersonal.findUnreadMessages(postId, user.getId()).size();
        }

        List<Long> vacancyPostIds = getCachedPostIdsWithUnread(user.getId(), "vacancy");
        for (Long postId : vacancyPostIds) {
            totalUnread += commentRepoVacPersonal.findUnreadMessages(postId, user.getId()).size();
        }

        List<Long> resumePostIds = getCachedPostIdsWithUnread(user.getId(), "resume");
        for (Long postId : resumePostIds) {
            totalUnread += commentRepoResPersonal.findUnreadMessages(postId, user.getId()).size();
        }

        return totalUnread;
    }

    /**
     * Получает список ID объявлений, в которых есть непрочитанные сообщения для пользователя.
     * Результат кэшируется в Spring Cache "postIdsWithUnread".
     *
     * @param userId ID пользователя
     * @param type Тип объявления (product, service, vacancy, resume)
     * @return Список ID объявлений
     */
    @Cacheable(value = "postIdsWithUnread", key = "#userId + '-' + #type")
    public List<Long> getCachedPostIdsWithUnread(Long userId, String type) {
        switch (type) {
            case "product":
                return commentRepoPersonal.findPostIdsWithUnreadMessages(userId);
            case "service":
                return commentRepoJobPersonal.findPostIdsWithUnreadMessages(userId);
            case "vacancy":
                return commentRepoVacPersonal.findPostVacIdsWithUnreadMessages(userId);
            case "resume":
                return commentRepoResPersonal.findResIdsWithUnreadMessages(userId);
            default:
                logger.warn("Unknown type for postIdsWithUnread: {}", type);
                return Collections.emptyList();
        }
    }

    // ==================== МЕТОДЫ ИНВАЛИДАЦИИ КЭША ====================

    /**
     * Инвалидирует кэш количества непрочитанных сообщений для пользователя.
     *
     * @param userId ID пользователя
     */
    @CacheEvict(value = "unreadCounts", key = "#userId")
    public void evictUnreadCountCache(Long userId) {
        logger.debug("Evicted unreadCounts cache for user ID: {}", userId);
    }

    /**
     * Инвалидирует кэш списка ID объявлений с непрочитанными сообщениями.
     *
     * @param userId ID пользователя
     * @param type Тип объявления
     */
    @CacheEvict(value = "postIdsWithUnread", key = "#userId + '-' + #type")
    public void evictPostIdsCache(Long userId, String type) {
        logger.debug("Evicted postIdsWithUnread cache for user ID: {} and type: {}", userId, type);
    }

    /**
     * Полностью очищает все кэши сообщений.
     * Используется при административных действиях.
     */
    @CacheEvict(value = {"unreadCounts", "postIdsWithUnread"}, allEntries = true)
    public void evictAllMessageCaches() {
        logger.debug("Evicted all message caches");
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Асинхронно отмечает все сообщения пользователя как прочитанные.
     * Выполняет массовое обновление в БД для каждого типа объявлений.
     *
     * @param userId ID пользователя
     */
    private void markAllMessagesAsReadAsync(Long userId) {
        try {
            // Обработка товаров
            List<Long> productPostIds = commentRepoPersonal.findPostIdsWithUnreadMessages(userId);
            for (Long postId : productPostIds) {
                commentRepoPersonal.markMessagesAsRead(postId, userId);
            }

            // Обработка услуг
            List<Long> servicePostIds = commentRepoJobPersonal.findPostIdsWithUnreadMessages(userId);
            for (Long postId : servicePostIds) {
                commentRepoJobPersonal.markMessagesAsRead(postId, userId);
            }

            // Обработка вакансий
            List<Long> vacancyPostIds = commentRepoVacPersonal.findPostVacIdsWithUnreadMessages(userId);
            for (Long postId : vacancyPostIds) {
                commentRepoVacPersonal.markMessagesAsRead(postId, userId);
            }

            // Обработка резюме
            List<Long> resumePostIds = commentRepoResPersonal.findResIdsWithUnreadMessages(userId);
            for (Long postId : resumePostIds) {
                commentRepoResPersonal.markMessagesAsRead(postId, userId);
            }

            logger.debug("All messages marked as read for user ID: {}", userId);
        } catch (Exception e) {
            logger.error("Error in markAllMessagesAsReadAsync for user ID {}: {}", userId, e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    /**
     * Проверяет превышение лимита запросов для конкретного действия.
     *
     * @param authentication Объект аутентификации
     * @param action Название действия (для различения лимитов)
     * @param limit Максимальное количество запросов за окно
     * @return true если запрос разрешён, false если лимит превышен
     */
    private boolean checkRateLimit(Authentication authentication, String action, int limit) {
        if (authentication == null) {
            return true;  // Анонимные запросы не ограничиваем
        }

        String userId = authentication.getName();  // Используем только для ключа кэша
        String key = "rate_limit:" + action + ":" + userId;
        long now = System.currentTimeMillis() / 1000;

        RateLimitInfo info = rateLimitCache.getIfPresent(key);

        if (info == null) {
            rateLimitCache.put(key, new RateLimitInfo(1, now));
            return true;
        }

        // Проверяем, не истекло ли окно
        if (now - info.windowStart > RATE_LIMIT_WINDOW) {
            rateLimitCache.put(key, new RateLimitInfo(1, now));
            return true;
        }

        // Проверяем лимит
        if (info.count >= limit) {
            // Логируем только действие, не userId
            logger.warn("Rate limit exceeded for action: {}", action);
            return false;
        }

        info.count++;
        rateLimitCache.put(key, info);
        return true;
    }

    /**
     * Создаёт стандартный ответ при превышении rate limit.
     *
     * @return Карта с информацией об ошибке
     */
    private Map<String, Object> createRateLimitResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "rate_limit");
        response.put("message", "Слишком много запросов. Пожалуйста, подождите.");
        response.put("tickets", Collections.emptyList());
        response.put("hasNext", false);
        return response;
    }

    /**
     * Получает срез (Slice) тикетов для текущего пользователя.
     * Собирает все непрочитанные сообщения из разных типов объявлений,
     * сортирует их и применяет пагинацию.
     *
     * @param currentUser Текущий пользователь
     * @param pageable Объект пагинации
     * @return Slice с DTO тикетов
     */
    private Slice<MessageTicketDTO> getMessageTicketsSlice(User currentUser, Pageable pageable) {
        List<MessageTicketDTO> allTickets = new ArrayList<>();

        // Сбор тикетов из товаров
        List<Long> productPostIds = getCachedPostIdsWithUnread(currentUser.getId(), "product");
        for (Long postId : productPostIds) {
            Optional<PostText> postOpt = postRepo.findById(postId);
            postOpt.ifPresent(post -> {
                List<CommentPersonal> unreadMessages = commentRepoPersonal.findUnreadMessages(postId, currentUser.getId());
                addTicketsFromMessages(unreadMessages, post.getHashId(), post.getTitle(), "product", allTickets);
            });
        }

        // Сбор тикетов из услуг
        List<Long> servicePostIds = getCachedPostIdsWithUnread(currentUser.getId(), "service");
        for (Long postId : servicePostIds) {
            Optional<PostTextJob> postOpt = postRepoJob.findById(postId);
            postOpt.ifPresent(post -> {
                List<CommentPersonalJob> unreadMessages = commentRepoJobPersonal.findUnreadMessages(postId, currentUser.getId());
                addTicketsFromServiceMessages(unreadMessages, post.getHashId(), post.getTitle(), "service", allTickets);
            });
        }

        // Сбор тикетов из вакансий
        List<Long> vacancyPostIds = getCachedPostIdsWithUnread(currentUser.getId(), "vacancy");
        for (Long postId : vacancyPostIds) {
            Optional<Vacancy> postOpt = postVacancyRepository.findById(postId);
            postOpt.ifPresent(post -> {
                List<CommentPersonalVac> unreadMessages = commentRepoVacPersonal.findUnreadMessages(postId, currentUser.getId());
                addTicketsFromVacancyMessages(unreadMessages, post.getHashId(), post.getTitle(), "vacancy", allTickets);
            });
        }

        // Сбор тикетов из резюме
        List<Long> resumePostIds = getCachedPostIdsWithUnread(currentUser.getId(), "resume");
        for (Long postId : resumePostIds) {
            Optional<Resume> postOpt = postResumeRepository.findById(postId);
            postOpt.ifPresent(post -> {
                List<CommentPersonalRes> unreadMessages = commentRepoResPersonal.findUnreadMessages(postId, currentUser.getId());
                addTicketsFromResumeMessages(unreadMessages, post.getHashId(), post.getTitle(), "resume", allTickets);
            });
        }

        // Сортировка по времени сообщения (от новых к старым)
        allTickets.sort((a, b) -> b.getMessageTime().compareTo(a.getMessageTime()));

        // Применение пагинации
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTickets.size());

        List<MessageTicketDTO> pageContent = start < allTickets.size()
                ? allTickets.subList(start, end)
                : new ArrayList<>();

        return new SliceImpl<>(pageContent, pageable, end < allTickets.size());
    }

    // ==================== МЕТОДЫ КОНВЕРТАЦИИ СООБЩЕНИЙ В DTO ====================

    /**
     * Добавляет тикеты из списка сообщений товаров.
     *
     * @param messages Список сообщений
     * @param hashId Идентификатор объявления
     * @param title Заголовок объявления
     * @param type Тип объявления
     * @param allTickets Список для добавления
     */
    private void addTicketsFromMessages(List<CommentPersonal> messages, String hashId,
                                        String title, String type, List<MessageTicketDTO> allTickets) {
        for (CommentPersonal msg : messages) {
            String preview = msg.getText().length() > 50
                    ? msg.getText().substring(0, 50) + "..."
                    : msg.getText();

            MessageTicketDTO dto = new MessageTicketDTO(
                    msg.getPost().getId(),
                    hashId,
                    title,
                    type,
                    msg.getUser().getNameuse(),
                    msg.getCreatedAt(),
                    preview,
                    false
            );
            dto.setMessageId(msg.getId());
            allTickets.add(dto);
        }
    }

    /**
     * Добавляет тикеты из списка сообщений услуг.
     */
    private void addTicketsFromServiceMessages(List<CommentPersonalJob> messages, String hashId,
                                               String title, String type, List<MessageTicketDTO> allTickets) {
        for (CommentPersonalJob msg : messages) {
            String preview = msg.getText().length() > 50
                    ? msg.getText().substring(0, 50) + "..."
                    : msg.getText();

            MessageTicketDTO dto = new MessageTicketDTO(
                    msg.getPostjob().getId(),
                    hashId,
                    title,
                    type,
                    msg.getUser().getNameuse(),
                    msg.getCreatedAt(),
                    preview,
                    false
            );
            dto.setMessageId(msg.getId());
            allTickets.add(dto);
        }
    }

    /**
     * Добавляет тикеты из списка сообщений вакансий.
     */
    private void addTicketsFromVacancyMessages(List<CommentPersonalVac> messages, String hashId,
                                               String title, String type, List<MessageTicketDTO> allTickets) {
        for (CommentPersonalVac msg : messages) {
            String preview = msg.getText().length() > 50
                    ? msg.getText().substring(0, 50) + "..."
                    : msg.getText();

            MessageTicketDTO dto = new MessageTicketDTO(
                    msg.getVacancy().getId(),
                    hashId,
                    title,
                    type,
                    msg.getUser().getNameuse(),
                    msg.getCreatedAt(),
                    preview,
                    false
            );
            dto.setMessageId(msg.getId());
            allTickets.add(dto);
        }
    }

    /**
     * Добавляет тикеты из списка сообщений резюме.
     */
    private void addTicketsFromResumeMessages(List<CommentPersonalRes> messages, String hashId,
                                              String title, String type, List<MessageTicketDTO> allTickets) {
        for (CommentPersonalRes msg : messages) {
            String preview = msg.getText().length() > 50
                    ? msg.getText().substring(0, 50) + "..."
                    : msg.getText();

            MessageTicketDTO dto = new MessageTicketDTO(
                    msg.getResume().getId(),
                    hashId,
                    title,
                    type,
                    msg.getUser().getNameuse(),
                    msg.getCreatedAt(),
                    preview,
                    false
            );
            dto.setMessageId(msg.getId());
            allTickets.add(dto);
        }
    }

    // ==================== ВНУТРЕННИЕ КЛАССЫ ====================

    /**
     * Информация о rate limiting для конкретного действия и пользователя.
     */
    private static class RateLimitInfo {
        int count;
        long windowStart;

        RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}