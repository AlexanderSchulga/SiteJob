package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для автоматической разблокировки пользователей и их постов.
 * <p>
 * Запускается по расписанию и проверяет пользователей с временной блокировкой
 * (у которых указан {@code blockUntil}). Если срок блокировки истек, пользователь
 * автоматически разблокируется вместе со всеми своими постами.
 * </p>
 *
 * <p><b>Что происходит при разблокировке:</b></p>
 * <ul>
 *   <li>Пользователь становится активным ({@code active = true})</li>
 *   <li>Сбрасываются {@code blockReason}, {@code blockedAt}, {@code blockUntil}</li>
 *   <li>Счетчик блокировок {@code blockCount} НЕ сбрасывается (сохраняется история)</li>
 *   <li>Все посты пользователя (товары, услуги, вакансии, резюме) разблокируются</li>
 * </ul>
 *
 * <p><b>Расписание:</b> запуск каждые 30 минут.</p>
 *
 * @author YourTeam
 * @version 1.0
 */
@RequiredArgsConstructor
@Service
public class UserUnblockService {

    private static final Logger log = LoggerFactory.getLogger(UserUnblockService.class);

    private final UserRepository userRepository;
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    /**
     * Автоматически разблокирует пользователей с истекшим сроком блокировки.
     * <p>
     * Запускается по расписанию каждые 30 минут.
     * Находит всех неактивных пользователей, у которых установлена дата окончания блокировки
     * и эта дата уже наступила. Для каждого такого пользователя выполняет разблокировку.
     * </p>
     * <p>
     * <b>Важно:</b> метод транзакционный — при ошибке откатятся все изменения.
     * </p>
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 минут
    @Transactional
    public void autoUnblockUsers() {
        log.debug("Starting automatic user unblock check");

        LocalDateTime now = LocalDateTime.now();

// Находим всех заблокированных пользователей с истекшим сроком блокировки
        List<User> blockedUsers = userRepository.findUsersWithExpiredBlock(now);

        if (blockedUsers.isEmpty()) {
            log.debug("No users with expired block period found");
            return;
        }

        log.debug("Found {} user(s) with expired block period", blockedUsers.size());

        for (User user : blockedUsers) {
            // Автоматическая разблокировка пользователя
            user.setActive(true);
            user.setBlockReason(null);
            user.setBlockedAt(null);
            user.setBlockUntil(null);
            // Счетчик блокировок НЕ сбрасываем — сохраняем историю

            // Разблокируем все посты пользователя
            unblockAllUserPosts(user);
            userRepository.save(user);

            log.info("User automatically unblocked. Block count: {}", user.getBlockCount());
        }

        log.debug("Automatic unblock check completed");
    }

    /**
     * Разблокирует все посты пользователя.
     * <p>
     * Разблокирует товары, услуги, вакансии и резюме указанного пользователя.
     * Для каждого поста сбрасывается флаг {@code blocked} и дата блокировки {@code blockedAt}.
     * </p>
     * <p>
     * <b>Производительность:</b> метод выполняет отдельные запросы для каждого типа постов.
     * При большом количестве постов может потребоваться оптимизация (пагинация или batch-обновление).
     * </p>
     *
     * @param user пользователь, посты которого нужно разблокировать
     */
    private void unblockAllUserPosts(User user) {
        log.debug("Unblocking all posts for user");

        // Разблокировка товаров
        List<PostText> userPosts = postRepo.findByUser(user);
        for (PostText post : userPosts) {
            post.setBlocked(false);
            post.setBlockedAt(null);
        }
        postRepo.saveAll(userPosts);
        log.debug("Unblocked {} products", userPosts.size());

        // Разблокировка услуг
        List<PostTextJob> userPostsJob = postRepoJob.findByUser(user);
        for (PostTextJob postJob : userPostsJob) {
            postJob.setBlocked(false);
            postJob.setBlockedAt(null);
        }
        postRepoJob.saveAll(userPostsJob);
        log.debug("Unblocked {} services", userPostsJob.size());

        // Разблокировка вакансий
        List<Vacancy> userVacancies = postVacancyRepository.findByUser(user);
        for (Vacancy vacancy : userVacancies) {
            vacancy.setBlocked(false);
            vacancy.setBlockedAt(null);
        }
        postVacancyRepository.saveAll(userVacancies);
        log.debug("Unblocked {} vacancies", userVacancies.size());

        // Разблокировка резюме
        List<Resume> userResume = postResumeRepository.findByUser(user);
        for (Resume resume : userResume) {
            resume.setBlocked(false);
            resume.setBlockedAt(null);
        }
        postResumeRepository.saveAll(userResume);
        log.debug("Unblocked {} resumes", userResume.size());
    }
}