package jobforsite.sitejob.module.shared.services;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для проверки наличия заблокированных постов у пользователя.
 *
 * <p>Используется для отображения уведомлений пользователю о том,
 * что его объявления (товары, услуги, вакансии, резюме) были заблокированы модератором.
 *
 * <p>Работает с четырьмя типами сущностей:
 * <ul>
 *   <li>PostText - товары</li>
 *   <li>PostTextJob - услуги</li>
 *   <li>Vacancy - вакансии</li>
 *   <li>Resume - резюме</li>
 * </ul>
 *
 * @author Team
 * @version 1.0
 */
@RequiredArgsConstructor
@Service
public class BlockedPostsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BlockedPostsNotificationService.class);

    private final PostRepo postRepo;               // Репозиторий для товаров
    private final PostRepoJob postRepoJob;         // Репозиторий для услуг
    private final PostVacancyRepository postVacancyRepository; // Репозиторий для вакансий
    private final PostResumeRepository postResumeRepository;   // Репозиторий для резюме

    /**
     * Проверяет, есть ли у пользователя хотя бы один заблокированный пост.
     *
     * <p>Метод выполняет запросы ко всем четырем репозиториям и проверяет
     * флаг {@code blocked} у каждой сущности. Если хотя бы одна запись
     * заблокирована, возвращает {@code true}.
     *
     * @param user пользователь, для которого выполняется проверка
     * @return {@code true} если есть хотя бы один заблокированный пост,
     *         {@code false} если все посты активны или их нет
     */
    public boolean hasBlockedPosts(User user) {
        log.debug("Checking for blocked posts");

        // Проверяем товары
        List<PostText> userPosts = postRepo.findByUser(user);
        boolean hasBlockedProducts = userPosts.stream()
                .anyMatch(PostText::isBlocked);

        // Проверяем услуги
        List<PostTextJob> userPostsJob = postRepoJob.findByUser(user);
        boolean hasBlockedServices = userPostsJob.stream()
                .anyMatch(PostTextJob::isBlocked);

        // Проверяем вакансии
        List<Vacancy> userVacancies = postVacancyRepository.findByUser(user);
        boolean hasBlockedVacancies = userVacancies.stream()
                .anyMatch(Vacancy::isBlocked);

        // Проверяем резюме
        List<Resume> userResume = postResumeRepository.findByUser(user);
        boolean hasBlockedResume = userResume.stream()
                .anyMatch(Resume::isBlocked);

        boolean result = hasBlockedProducts || hasBlockedServices || hasBlockedVacancies || hasBlockedResume;

        if (result) {
            log.info("User has blocked posts: products={}, services={}, vacancies={}, resumes={}",
                    hasBlockedProducts, hasBlockedServices, hasBlockedVacancies, hasBlockedResume);
        } else {
            log.debug("User has no blocked posts");
        }

        return result;
    }

    /**
     * Возвращает общее количество заблокированных постов пользователя.
     *
     * <p>Суммирует количество заблокированных записей по всем типам:
     * товары, услуги, вакансии, резюме.
     *
     * @param user пользователь, для которого выполняется подсчет
     * @return общее количество заблокированных постов
     */
    public int getBlockedPostsCount(User user) {
        log.debug("Counting blocked posts");

        int count = 0;

        // Считаем заблокированные товары
        List<PostText> userPosts = postRepo.findByUser(user);
        long blockedProducts = userPosts.stream()
                .filter(PostText::isBlocked)
                .count();
        count += blockedProducts;

        // Считаем заблокированные услуги
        List<PostTextJob> userPostsJob = postRepoJob.findByUser(user);
        long blockedServices = userPostsJob.stream()
                .filter(PostTextJob::isBlocked)
                .count();
        count += blockedServices;

        // Считаем заблокированные вакансии
        List<Vacancy> userVacancies = postVacancyRepository.findByUser(user);
        long blockedVacancies = userVacancies.stream()
                .filter(Vacancy::isBlocked)
                .count();
        count += blockedVacancies;

        // Считаем заблокированные резюме
        List<Resume> userResume = postResumeRepository.findByUser(user);
        long blockedResumes = userResume.stream()
                .filter(Resume::isBlocked)
                .count();
        count += blockedResumes;

        if (count > 0) {
            log.info("Blocked posts count: total={}, products={}, services={}, vacancies={}, resumes={}",
                    count, blockedProducts, blockedServices, blockedVacancies, blockedResumes);
        } else {
            log.debug("No blocked posts found");
        }

        return count;
    }
}