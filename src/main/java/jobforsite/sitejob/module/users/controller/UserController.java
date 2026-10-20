package jobforsite.sitejob.module.users.controller;

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
import jobforsite.sitejob.module.shared.controller.sharedglobal.BaseController;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.shared.repository.ReviewsRepo;
import jobforsite.sitejob.module.shared.repository.SupportCommentRepo;
import jobforsite.sitejob.module.users.repository.BlacklistRepository;
import jobforsite.sitejob.module.shared.services.cachrateservice.BlockedPostsCacheService;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.users.dto.UserDTO;
import jobforsite.sitejob.module.shared.model.Reviews;
import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.users.model.Blacklist;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.UserRepository;
import jobforsite.sitejob.module.users.service.SessionManagementService;
import jobforsite.sitejob.module.users.service.UserBlockReasonCacheService;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.vacancy.repository.CommentRepoVacPersonal;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления пользователями (административный интерфейс).
 * <p>
 * Предоставляет функционал для:
 * <ul>
 *     <li>Просмотра и поиска пользователей с пагинацией</li>
 *     <li>Блокировки/разблокировки пользователей с автоматической блокировкой всех постов</li>
 *     <li>Удаления пользователей с каскадной обработкой связанных данных</li>
 *     <li>Управления черным списком (бан по email/телефону)</li>
 *     <li>Сброса счетчика блокировок</li>
 *     <li>Просмотра информации о блокировке через AJAX</li>
 * </ul>
 * Доступ имеют только пользователи с ролями ADMIN и SUPER.
 * </p>
 */
@RequiredArgsConstructor
@Controller
@RequestMapping("/users")
public class UserController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserRepository userRepository;
    private final SupportCommentRepo supportCommentRepo;
    private final BlacklistRepository blacklistRepository;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final CommentRepoVacPersonal commentRepoVacPersonal;
    private final CommentRepoJobPersonal commentRepoJobPersonal;
    private final CommentRepoPersonal commentRepoPersonal;
    private final CommentRepoResPersonal commentRepoResPersonal;
    private final FavorRepo favorRepo;
    private final ReviewsRepo reviewsRepo;
    private final BlockedPostsCacheService blockedPostsCacheService;
    private final UserBlockReasonCacheService userBlockReasonCacheService;
    private final SessionManagementService sessionManagementService;
    private final ImageService imageService;

    /**
     * Отображает страницу со списком пользователей.
     * <p>
     * Поддерживает поиск по имени, логину, email, телефону.
     * ADMIN видит только обычных пользователей, SUPER видит всех кроме себя.
     * </p>
     *
     * @param search         поисковый запрос (опционально)
     * @param page           номер страницы
     * @param size           размер страницы
     * @param model          модель для передачи данных в представление
     * @param authentication объект аутентификации текущего пользователя
     * @return имя шаблона для отображения
     */
    @GetMapping
    public String showUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Model model,
            Authentication authentication) {

        log.debug("Запрос списка пользователей: page={}, size={}, search={}", page, size, search);

        // Получаем текущего пользователя
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDate"));

        Slice<User> usersSlice;

        if (search != null && !search.trim().isEmpty()) {
            // Поиск с учетом роли текущего пользователя
            if (currentUser.getRole().equals("ROLE_ADMIN")) {
                // Админ ищет только обычных пользователей
                usersSlice = userRepository.findBySearchTermForAdmin(search.trim(), currentUser.getId(), pageable);
                log.debug("Поиск пользователей (админ): найдено {}", usersSlice.getNumberOfElements());
            } else if (currentUser.getRole().equals("ROLE_SUPER")) {
                // Суперадмин ищет всех кроме себя
                usersSlice = userRepository.findBySearchTermExcludingUser(search.trim(), currentUser.getId(), pageable);
                log.debug("Поиск пользователей (суперадмин): найдено {}", usersSlice.getNumberOfElements());
            } else {
                usersSlice = new SliceImpl<>(new ArrayList<>(), pageable, false);
                log.warn("Пользователь с ролью {} попытался получить доступ к списку пользователей", currentUser.getRole());
            }
            model.addAttribute("searchTerm", search.trim());
            model.addAttribute("searchResultsCount", usersSlice.getNumberOfElements());
        } else {
            // Загрузка без поиска с учетом роли
            if (currentUser.getRole().equals("ROLE_ADMIN")) {
                usersSlice = userRepository.findAllUsersForAdmin(pageable);
                log.debug("Загрузка всех пользователей (админ): {} элементов", usersSlice.getNumberOfElements());
            } else if (currentUser.getRole().equals("ROLE_SUPER")) {
                usersSlice = userRepository.findAllExcludingUser(currentUser.getId(), pageable);
                log.debug("Загрузка всех пользователей (суперадмин): {} элементов", usersSlice.getNumberOfElements());
            } else {
                usersSlice = new SliceImpl<>(new ArrayList<>(), pageable, false);
                log.warn("Пользователь с ролью {} попытался получить доступ к списку пользователей", currentUser.getRole());
            }
        }

        model.addAttribute("users", usersSlice.getContent());
        model.addAttribute("currentPage", usersSlice.getNumber());
        model.addAttribute("hasMore", usersSlice.hasNext());
        model.addAttribute("pageSize", size);
        model.addAttribute("currentUserRole", currentUser.getRole());

        return "users";
    }

    /**
     * Получает информацию о блокировке пользователя для AJAX-запросов.
     *
     * @param id идентификатор пользователя
     * @return ResponseEntity с информацией о блокировке
     */
    @GetMapping("/{id}/block-info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserBlockInfo(@PathVariable Long id) {
        log.debug("Запрос информации о блокировке пользователя");

        Map<String, Object> response = new HashMap<>();

        try {
            UserBlockReasonCacheService.BlockInfo blockInfo =
                    userBlockReasonCacheService.getUserBlockInfo(id);

            if (blockInfo != null) {
                response.put("status", "success");
                response.put("isBlocked", true);
                response.put("reason", blockInfo.getReason());
                response.put("blockedAt", blockInfo.getFormattedBlockedAt());
                response.put("blockUntil", blockInfo.getFormattedBlockUntil());
                response.put("blockCount", blockInfo.getBlockCount());
                response.put("remainingDays", blockInfo.getRemainingDays());
                log.debug("Информация о блокировке получена успешно");
            } else {
                response.put("status", "success");
                response.put("isBlocked", false);
                response.put("message", "Пользователь не заблокирован");
                log.debug("Пользователь не заблокирован");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Ошибка при получении информации о блокировке: ", e);
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Загружает следующую страницу пользователей для AJAX-пагинации.
     * <p>
     * Поддерживает поиск и сортировку (новые/старые).
     * </p>
     *
     * @param page           номер страницы
     * @param size           размер страницы
     * @param search         поисковый запрос (опционально)
     * @param sort           тип сортировки (newest/oldest)
     * @param authentication объект аутентификации
     * @return ResponseEntity с DTO пользователей и мета-информацией
     */
    @GetMapping("/load-more")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication) {

        log.debug("AJAX загрузка пользователей: page={}, size={}, sort={}, search={}", page, size, sort, search);

        // Получаем текущего пользователя
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        // Настройка сортировки
        Pageable pageable;
        if ("oldest".equals(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "registrationDate"));
        } else {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDate"));
        }

        // Загрузка данных с учетом поиска и роли
        Slice<User> usersSlice;
        if (search != null && !search.trim().isEmpty()) {
            if (currentUser.getRole().equals("ROLE_ADMIN")) {
                usersSlice = userRepository.findBySearchTermForAdmin(search, currentUser.getId(), pageable);
            } else if (currentUser.getRole().equals("ROLE_SUPER")) {
                usersSlice = userRepository.findBySearchTermExcludingUser(search, currentUser.getId(), pageable);
            } else {
                usersSlice = new SliceImpl<>(new ArrayList<>(), pageable, false);
            }
        } else {
            if (currentUser.getRole().equals("ROLE_ADMIN")) {
                usersSlice = userRepository.findAllUsersForAdmin(pageable);
            } else if (currentUser.getRole().equals("ROLE_SUPER")) {
                usersSlice = userRepository.findAllExcludingUser(currentUser.getId(), pageable);
            } else {
                usersSlice = new SliceImpl<>(new ArrayList<>(), pageable, false);
            }
        }

        // Получение информации о текущем администраторе
        User currentAdmin = null;
        Long currentAdminId = null;
        String currentAdminRole = null;
        if (authentication != null) {
            currentAdmin = userRepository.findByNameuse(authentication.getName()).orElse(null);
            if (currentAdmin != null) {
                currentAdminId = currentAdmin.getId();
                currentAdminRole = currentAdmin.getRole();
            }
        }

        // Конвертация в DTO
        List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : usersSlice.getContent()) {
            boolean isInBlacklist = isUserInBlacklist(user);

            boolean deletable;
            if (currentAdminRole != null && currentAdminRole.equals("ROLE_SUPER")) {
                // Супер-админ может удалять всех, кроме самого себя
                deletable = !currentAdminId.equals(user.getId());
            } else {
                // Обычный админ может удалять только обычных пользователей (не SUPER и не ADMIN)
                deletable = !user.getRole().equals("ROLE_SUPER") &&
                        !user.getRole().equals("ROLE_ADMIN") &&
                        (currentAdminId == null || !currentAdminId.equals(user.getId()));
            }

            // Формирование информации о блокировке
            String blockDuration = "";
            if (!user.isActive() && user.getBlockCount() > 0) {
                int currentDuration = calculateBlockDuration(user.getBlockCount());
                blockDuration = currentDuration + " дней (Блокировка №" + user.getBlockCount() + ")";
            }

            UserDTO dto = new UserDTO(
                    user.getId(),
                    user.getUsername(),
                    user.getNameuse(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getPhonePost(),
                    user.getRole(),
                    user.isActive(),
                    user.getRegistrationDate(),
                    isInBlacklist,
                    deletable,
                    user.getBlockReason(),
                    user.getBlockedAt(),
                    user.getRegion(),
                    user.getHashId(),
                    user.getBlockCount(),
                    user.getBlockUntil(),
                    blockDuration,
                    user.getDescript()
            );
            userDTOs.add(dto);
        }

        // Формирование ответа
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("users", userDTOs);
        response.put("currentPage", usersSlice.getNumber());
        response.put("nextPage", usersSlice.hasNext() ? usersSlice.getNumber() + 1 : usersSlice.getNumber());
        response.put("hasMore", usersSlice.hasNext());
        response.put("pageSize", size);

        if (search != null && !search.trim().isEmpty()) {
            response.put("searchTerm", search);
        }

        if (currentAdmin != null) {
            response.put("currentAdminId", currentAdminId);
            response.put("currentAdminRole", currentAdminRole);
        }

        log.debug("AJAX загрузка завершена: {} пользователей, hasMore={}", userDTOs.size(), usersSlice.hasNext());
        return ResponseEntity.ok(response);
    }

    /**
     * Рассчитывает длительность блокировки в днях на основе количества блокировок.
     * <p>
     * Схема: 1-я блокировка → 1 день, 2-я → 5 дней, 3-я → 15 дней, 4+ → 30 дней.
     * </p>
     *
     * @param blockCount количество предыдущих блокировок
     * @return количество дней блокировки
     */
    private int calculateBlockDuration(int blockCount) {
        switch (blockCount) {
            case 1: return 1;   // 1 день
            case 2: return 5;   // 5 дней
            case 3: return 15;  // 15 дней
            default: return 30; // 30 дней (для 4+ блокировок)
        }
    }

    /**
     * Блокирует все посты пользователя (объявления, услуги, вакансии, резюме).
     * <p>
     * Также удаляет записи об этих постах из избранного и очищает кэш заблокированных постов.
     * </p>
     *
     * @param user пользователь, чьи посты блокируются
     */
    private void blockAllUserPosts(User user) {
        log.debug("Блокировка всех постов пользователя");

        // Блокировка обычных объявлений
        List<PostText> userPosts = postRepo.findByUser(user);
        for (PostText post : userPosts) {
            post.setBlocked(true);
            post.setBlockedAt(LocalDateTime.now());
            favorRepo.deleteByPostId(post.getId());
        }
        postRepo.saveAll(userPosts);

        // Блокировка услуг
        List<PostTextJob> userPostsJob = postRepoJob.findByUser(user);
        for (PostTextJob postJob : userPostsJob) {
            postJob.setBlocked(true);
            postJob.setBlockedAt(LocalDateTime.now());
            favorRepo.deleteByPostJobId(postJob.getId());
        }
        postRepoJob.saveAll(userPostsJob);

        // Блокировка вакансий
        List<Vacancy> userVacancies = postVacancyRepository.findByUser(user);
        for (Vacancy vacancy : userVacancies) {
            vacancy.setBlocked(true);
            vacancy.setBlockedAt(LocalDateTime.now());
            favorRepo.deleteByVacancyId(vacancy.getId());
        }
        postVacancyRepository.saveAll(userVacancies);

        // Блокировка резюме
        List<Resume> userResume = postResumeRepository.findByUser(user);
        for (Resume res : userResume) {
            res.setBlocked(true);
            res.setBlockedAt(LocalDateTime.now());
            favorRepo.deleteByVacancyId(res.getId());
        }
        postResumeRepository.saveAll(userResume);

        // Очистка кэша после блокировки всех постов
        if (blockedPostsCacheService != null) {
            blockedPostsCacheService.clearAllBlockedCaches();
            log.debug("Кэш заблокированных постов очищен после блокировки пользователя");
        }
    }

    /**
     * Разблокирует все посты пользователя и очищает кэш.
     *
     * @param user пользователь, чьи посты разблокируются
     */
    private void unblockAllUserPosts(User user) {
        log.debug("Разблокировка всех постов пользователя");

        // Разблокировка обычных объявлений
        List<PostText> userPosts = postRepo.findByUser(user);
        for (PostText post : userPosts) {
            post.setBlocked(false);
            post.setBlockedAt(null);
        }
        postRepo.saveAll(userPosts);

        // Разблокировка услуг
        List<PostTextJob> userPostsJob = postRepoJob.findByUser(user);
        for (PostTextJob postJob : userPostsJob) {
            postJob.setBlocked(false);
            postJob.setBlockedAt(null);
        }
        postRepoJob.saveAll(userPostsJob);

        // Разблокировка вакансий
        List<Vacancy> userVacancies = postVacancyRepository.findByUser(user);
        for (Vacancy vacancy : userVacancies) {
            vacancy.setBlocked(false);
            vacancy.setBlockedAt(null);
        }
        postVacancyRepository.saveAll(userVacancies);

        // Разблокировка резюме
        List<Resume> userResume = postResumeRepository.findByUser(user);
        for (Resume res : userResume) {
            res.setBlocked(false);
            res.setBlockedAt(null);
        }
        postResumeRepository.saveAll(userResume);

        // Очистка кэша после разблокировки всех постов
        if (blockedPostsCacheService != null) {
            blockedPostsCacheService.clearAllBlockedCaches();
            log.debug("Кэш заблокированных постов очищен после разблокировки пользователя");
        }
    }

    /**
     * Переключает статус пользователя (блокировка/разблокировка).
     * <p>
     * При блокировке:
     * <ul>
     *     <li>Увеличивается счетчик блокировок</li>
     *     <li>Рассчитывается длительность (1, 5, 15 или 30 дней)</li>
     *     <li>Блокируются все посты</li>
     *     <li>Завершаются активные сессии</li>
     * </ul>
     * При разблокировке сбрасываются поля блокировки, но счетчик сохраняется.
     * </p>
     *
     * @param id           идентификатор пользователя
     * @param blockReason  причина блокировки (обязательна при блокировке)
     * @param model        модель для передачи сообщений
     * @param authentication объект аутентификации
     * @return редирект на страницу пользователей
     */
    @PostMapping("/{id}/toggle")
    @Transactional
    public String toggleUserStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String blockReason,
            Model model,
            Authentication authentication) {
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка переключения статуса пользователя без прав SUPER_ACCESS");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/users";
            }

            User user = userRepository.findById(id).orElseThrow();
            String username = user.getUsername(); // Сохраняем username до изменений

            if (user.isActive() && blockReason != null && !blockReason.trim().isEmpty()) {
                // === БЛОКИРОВКА ПОЛЬЗОВАТЕЛЯ ===
                int newBlockCount = user.getBlockCount() + 1;
                user.setBlockCount(newBlockCount);

                // Рассчитываем длительность блокировки
                int blockDays = calculateBlockDuration(newBlockCount);

                // Устанавливаем время разблокировки
                LocalDateTime now = LocalDateTime.now();
                user.setBlockReason(blockReason.trim());
                user.setActive(false);
                user.setBlockedAt(now);
                user.setBlockUntil(now.plusDays(blockDays));

                // Блокируем все посты пользователя
                blockAllUserPosts(user);
                log.info("Пользователь заблокирован на {} дней. Причина: {} (Блокировка №{})",
                        blockDays, blockReason, newBlockCount);

                // Очищаем кэш причины блокировки
                userBlockReasonCacheService.clearBlockCache(user.getId());

                // Завершаем активные сессии
                int expiredSessions = sessionManagementService.expireUserSessions(username);
                log.debug("Завершено {} сессий заблокированного пользователя", expiredSessions);

            } else if (!user.isActive()) {
                // === РАЗБЛОКИРОВКА ПОЛЬЗОВАТЕЛЯ ===
                user.setBlockReason(null);
                user.setActive(true);
                user.setBlockedAt(null);
                user.setBlockUntil(null);
                // Счетчик НЕ сбрасываем при разблокировке (сбрасывается только вручную)

                // Разблокируем все посты пользователя
                unblockAllUserPosts(user);
                log.info("Пользователь разблокирован");

                // Очищаем кэш причины блокировки
                userBlockReasonCacheService.clearBlockCache(user.getId());
            }

            userRepository.save(user);

            // Дополнительная очистка кэша на всякий случай
            if (blockedPostsCacheService != null) {
                blockedPostsCacheService.clearAllBlockedCaches();
            }

            return "redirect:/users";

        } catch (Exception e) {
            log.error("Ошибка при изменении статуса пользователя: ", e);
            model.addAttribute("error", "Ошибка при изменении статуса пользователя: " + e.getMessage());
            return "users";
        }
    }

    /**
     * Удаляет пользователя и все связанные с ним данные.
     * <p>
     * Доступ: SUPER может удалять всех, ADMIN — только обычных пользователей (не SUPER и не ADMIN).
     * Нельзя удалить самого себя.
     * Каскадно обрабатываются: изображения, отзывы, комментарии поддержки, личные комментарии.
     * </p>
     *
     * @param id             идентификатор удаляемого пользователя
     * @param model          модель для передачи сообщений
     * @param authentication объект аутентификации
     * @return редирект на страницу пользователей
     */
    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteUser(
            @PathVariable Long id,
            Model model,
            Authentication authentication
    ) {
        try {
            // Проверяем авторизацию
            User currentAdmin = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

            // Проверяем, не пытается ли пользователь удалить сам себя
            if (currentAdmin.getId().equals(id)) {
                log.warn("Попытка удаления собственного аккаунта");
                model.addAttribute("error", "Нельзя удалить собственный аккаунт");
                return "redirect:/users";
            }

            User userToDelete = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь с таким ID не найден"));

            // Сохраняем username до удаления для логов
            String usernameToDelete = userToDelete.getUsername();

            // === ПРОВЕРКИ ПРАВ НА УДАЛЕНИЕ ===
            if (currentAdmin.getRole().equals("ROLE_SUPER")) {
                // Супер-админ может удалять всех (кроме себя, проверено выше)
                log.debug("Супер-админ удаляет пользователя");
            } else if (currentAdmin.getRole().equals("ROLE_ADMIN")) {
                // Обычный админ может удалять только обычных пользователей
                if (userToDelete.getRole().equals("ROLE_SUPER") ||
                        userToDelete.getRole().equals("ROLE_ADMIN")) {
                    log.warn("Администратор попытался удалить пользователя с ролью {}", userToDelete.getRole());
                    model.addAttribute("error", "Администраторы могут удалять только обычных пользователей");
                    return "redirect:/users";
                }
            } else {
                log.warn("Пользователь с ролью {} попытался удалить другого пользователя", currentAdmin.getRole());
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/users";
            }

            // Завершаем сессии перед удалением пользователя
            int expiredSessions = sessionManagementService.expireUserSessions(usernameToDelete);
            log.debug("Завершено {} сессий удаляемого пользователя", expiredSessions);

            // Удаляем все изображения пользователя
            imageService.deleteAllUserImages(userToDelete);

            // Обрабатываем связанные данные
            handleUserReviews(userToDelete);
            handleSupportComments(userToDelete);
            handlePersonalComments(userToDelete);
            handlePersonalJobComments(userToDelete);
            handlePersonalVacComments(userToDelete);
            handlePersonalResComments(userToDelete);

            // Удаляем пользователя
            userRepository.delete(userToDelete);
            log.info("Пользователь удален");

            // Очищаем кэш после удаления
            if (blockedPostsCacheService != null) {
                blockedPostsCacheService.clearAllBlockedCaches();
            }

            return "redirect:/users?deleteSuccess";

        } catch (UsernameNotFoundException e) {
            log.error("Пользователь не авторизован при попытке удаления");
            model.addAttribute("error", "Пользователь не авторизован");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            log.error("Пользователь с ID {} не найден", id);
            model.addAttribute("error", "Пользователь с таким ID не найден");
            return "redirect:/users";
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя с ID {}: ", id, e);
            model.addAttribute("error", "Ошибка при удалении пользователя: " + e.getMessage());
            return "redirect:/users";
        }
    }

    /**
     * Обрабатывает отзывы, связанные с пользователем, при его удалении.
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

    /**
     * Переключает статус пользователя в черном списке.
     *
     * @param id           идентификатор пользователя
     * @param reason       причина добавления в черный список
     * @param model        модель для передачи сообщений
     * @param authentication объект аутентификации
     * @return редирект на страницу пользователей
     */
    @PostMapping("/{id}/toggle-blacklist")
    @Transactional
    public String toggleBlacklist(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Model model,
            Authentication authentication
    ) {
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка управления черным списком без прав SUPER_ACCESS");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/users";
            }

            User currentAdmin = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

            if (currentAdmin.getId().equals(id)) {
                log.warn("Попытка добавить себя в черный список");
                model.addAttribute("error", "Нельзя выполнить это действие с собственным аккаунтом");
                return "redirect:/users";
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь с таким ID не найден"));

            if (isUserInBlacklist(user)) {
                removeUserFromBlacklist(user);
                log.info("Пользователь удален из черного списка");
                return "redirect:/users?blacklistRemoveSuccess";
            } else {
                if (reason == null || reason.trim().isEmpty()) {
                    log.warn("Попытка добавления в черный список без указания причины");
                    model.addAttribute("error", "Необходимо указать причину для добавления в черный список");
                    return "redirect:/users";
                }

                addUserToBlacklist(user, reason.trim(), currentAdmin);
                log.info("Пользователь добавлен в черный список. Причина: {}", reason);
                return "redirect:/users?blacklistSuccess";
            }

        } catch (Exception e) {
            log.error("Ошибка при работе с черным списком: ", e);
            model.addAttribute("error", "Ошибка при работе с черным списком: " + e.getMessage());
            return "redirect:/users";
        }
    }

    /**
     * Проверяет, находится ли пользователь в черном списке.
     *
     * @param user проверяемый пользователь
     * @return true, если email, телефон или телефон для объявлений есть в черном списке
     */
    public boolean isUserInBlacklist(User user) {
        return blacklistRepository.findByEmail(user.getEmail()).isPresent();
        //                blacklistRepository.findByPhone(user.getPhone()).isPresent() ||
        //                blacklistRepository.findByPhonePost(user.getPhonePost()).isPresent()
    }

    /**
     * Добавляет пользователя в черный список.
     *
     * @param user   пользователь для добавления
     * @param reason причина добавления
     * @param admin  администратор, выполнивший действие
     */
    private void addUserToBlacklist(User user, String reason, User admin) {
        Blacklist blacklistEntry = new Blacklist(
                user.getEmail(),
                reason,
                admin
        );
        blacklistRepository.save(blacklistEntry);
        log.debug("Запись в черном списке создана");
    }

    /**
     * Удаляет пользователя из черного списка по всем его контактным данным.
     *
     * @param user пользователь для удаления
     */
    private void removeUserFromBlacklist(User user) {
        List<Blacklist> entriesToDelete = new ArrayList<>();

        blacklistRepository.findByEmail(user.getEmail()).ifPresent(entriesToDelete::add);

        blacklistRepository.deleteAll(entriesToDelete);
        log.debug("Записи из черного списка удалены");
    }

    /**
     * Добавляет пользователя в черный список (альтернативный endpoint).
     *
     * @param id     идентификатор пользователя
     * @param reason причина
     * @param model  модель
     * @param authentication аутентификация
     * @return редирект
     */
    @PostMapping("/{id}/blacklist")
    @Transactional
    public String addToBlacklist(
            @PathVariable Long id,
            @RequestParam String reason,
            Model model,
            Authentication authentication
    ) {
        return toggleBlacklist(id, reason, model, authentication);
    }

    /**
     * Удаляет пользователя из черного списка по ID.
     *
     * @param userId         идентификатор пользователя
     * @param model          модель
     * @param authentication аутентификация
     * @return редирект
     */
    @PostMapping("/blacklist/user/{userId}/remove")
    @Transactional
    public String removeFromBlacklistByUserId(
            @PathVariable Long userId,
            Model model,
            Authentication authentication
    ) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            removeUserFromBlacklist(user);
            log.info("Пользователь удален из черного списка (по ID)");
            return "redirect:/users?blacklistRemoveSuccess";
        } catch (Exception e) {
            log.error("Ошибка при удалении из черного списка: ", e);
            model.addAttribute("error", "Ошибка при удалении из черного списка: " + e.getMessage());
            return "redirect:/users";
        }
    }

    /**
     * Сбрасывает счетчик блокировок пользователя.
     *
     * @param id            идентификатор пользователя
     * @param resetReason   причина сброса
     * @param model         модель
     * @param authentication аутентификация
     * @return редирект
     */
    @PostMapping("/{id}/reset-block-count")
    @Transactional
    public String resetBlockCount(
            @PathVariable Long id,
            @RequestParam(required = false) String resetReason,
            Model model,
            Authentication authentication) {
        try {
            if (!hasSuperAccess(authentication)) {
                log.warn("Отказ в доступе: попытка сброса счетчика блокировок без прав SUPER_ACCESS");
                model.addAttribute("error", "Доступ запрещен");
                return "redirect:/users";
            }

            User user = userRepository.findById(id).orElseThrow();

            // Сбрасываем счетчик блокировок
            user.setBlockCount(0);
            userRepository.save(user);
            log.info("Счетчик блокировок сброшен. Причина: {}", resetReason);

            return "redirect:/users?resetSuccess";

        } catch (Exception e) {
            log.error("Ошибка при сбросе счетчика блокировок: ", e);
            model.addAttribute("error", "Ошибка при сбросе счетчика: " + e.getMessage());
            return "redirect:/users";
        }
    }

    // ==================== Обработка личных комментариев ====================

    /**
     * Обрабатывает личные комментарии пользователя (обычные объявления).
     *
     * @param user удаляемый пользователь
     */
    private void handlePersonalComments(User user) {
        log.debug("Обработка личных комментариев пользователя");

        // Комментарии, где пользователь получатель
        List<CommentPersonal> receivedComments = commentRepoPersonal.findByRecipientId(user.getId());
        for (CommentPersonal comment : receivedComments) {
            comment.setRecipient(null);
            commentRepoPersonal.save(comment);
        }

        // Комментарии, где пользователь автор
        List<CommentPersonal> userComments = commentRepoPersonal.findByUserId(user.getId());
        for (CommentPersonal comment : userComments) {
            deleteCommentAndAllReplies(comment);
        }

        log.debug("Обработка личных комментариев завершена");
    }

    /**
     * Рекурсивно удаляет комментарий и все его ответы.
     *
     * @param comment удаляемый комментарий
     */
    private void deleteCommentAndAllReplies(CommentPersonal comment) {
        List<CommentPersonal> directReplies = commentRepoPersonal.findByParentCommentId(comment.getId());
        for (CommentPersonal reply : directReplies) {
            deleteCommentAndAllReplies(reply);
        }
        commentRepoPersonal.delete(comment);
    }

    /**
     * Обрабатывает личные комментарии пользователя (услуги).
     *
     * @param user удаляемый пользователь
     */
    private void handlePersonalJobComments(User user) {
        log.debug("Обработка личных комментариев к услугам");

        List<CommentPersonalJob> receivedComments = commentRepoJobPersonal.findByRecipientId(user.getId());
        for (CommentPersonalJob comment : receivedComments) {
            comment.setRecipient(null);
            commentRepoJobPersonal.save(comment);
        }

        List<CommentPersonalJob> userComments = commentRepoJobPersonal.findByUserId(user.getId());
        for (CommentPersonalJob comment : userComments) {
            deleteJobCommentAndAllReplies(comment);
        }
    }

    /**
     * Рекурсивно удаляет комментарий к услуге и все его ответы.
     *
     * @param comment удаляемый комментарий
     */
    private void deleteJobCommentAndAllReplies(CommentPersonalJob comment) {
        List<CommentPersonalJob> directReplies = commentRepoJobPersonal.findByParentCommentId(comment.getId());
        for (CommentPersonalJob reply : directReplies) {
            deleteJobCommentAndAllReplies(reply);
        }
        commentRepoJobPersonal.delete(comment);
    }

    /**
     * Обрабатывает личные комментарии пользователя (вакансии).
     *
     * @param user удаляемый пользователь
     */
    private void handlePersonalVacComments(User user) {
        log.debug("Обработка личных комментариев к вакансиям");

        List<CommentPersonalVac> receivedComments = commentRepoVacPersonal.findByRecipientId(user.getId());
        for (CommentPersonalVac comment : receivedComments) {
            comment.setRecipient(null);
            commentRepoVacPersonal.save(comment);
        }

        List<CommentPersonalVac> userComments = commentRepoVacPersonal.findByUserId(user.getId());
        for (CommentPersonalVac comment : userComments) {
            deleteVacCommentAndAllReplies(comment);
        }
    }

    /**
     * Рекурсивно удаляет комментарий к вакансии и все его ответы.
     *
     * @param comment удаляемый комментарий
     */
    private void deleteVacCommentAndAllReplies(CommentPersonalVac comment) {
        List<CommentPersonalVac> directReplies = commentRepoVacPersonal.findByParentCommentId(comment.getId());
        for (CommentPersonalVac reply : directReplies) {
            deleteVacCommentAndAllReplies(reply);
        }
        commentRepoVacPersonal.delete(comment);
    }

    /**
     * Обрабатывает личные комментарии пользователя (резюме).
     *
     * @param user удаляемый пользователь
     */
    private void handlePersonalResComments(User user) {
        log.debug("Обработка личных комментариев к резюме");

        List<CommentPersonalRes> receivedComments = commentRepoResPersonal.findByRecipientId(user.getId());
        for (CommentPersonalRes comment : receivedComments) {
            comment.setRecipient(null);
            commentRepoResPersonal.save(comment);
        }

        List<CommentPersonalRes> userComments = commentRepoResPersonal.findByUserId(user.getId());
        for (CommentPersonalRes comment : userComments) {
            deleteResCommentAndAllReplies(comment);
        }
    }

    /**
     * Рекурсивно удаляет комментарий к резюме и все его ответы.
     *
     * @param comment удаляемый комментарий
     */
    private void deleteResCommentAndAllReplies(CommentPersonalRes comment) {
        List<CommentPersonalRes> directReplies = commentRepoResPersonal.findByParentCommentId(comment.getId());
        for (CommentPersonalRes reply : directReplies) {
            deleteResCommentAndAllReplies(reply);
        }
        commentRepoResPersonal.delete(comment);
    }
}