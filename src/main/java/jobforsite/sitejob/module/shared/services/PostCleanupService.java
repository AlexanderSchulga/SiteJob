package jobforsite.sitejob.module.shared.services;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для автоматической очистки заблокированных объявлений.
 * <p>
 * Основная задача:
 * <ul>
 *   <li>Удаление объявлений (товаров, услуг, вакансий, резюме), которые находятся в статусе "заблокировано" более 5 дней</li>
 * </ul>
 * </p>
 * <p>
 * Заблокированные объявления не удаляются сразу, а получают "отсрочку" в 5 дней.
 * Это позволяет администратору или пользователю при необходимости разблокировать объявление
 * до его окончательного удаления.
 * </p>
 */
@RequiredArgsConstructor
@Service
public class PostCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(PostCleanupService.class);

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    /**
     * Удаляет заблокированные объявления, находящиеся в статусе "заблокировано" более 5 дней.
     * <p>
     * Условия удаления:
     * <ul>
     *   <li>Объявление заблокировано (isBlocked = true)</li>
     *   <li>Дата блокировки не null и старше 5 дней</li>
     * </ul>
     * Объявления удаляются из базы данных без возможности восстановления.
     * </p>
     * <p>
     * Запускается каждые 6 часов.
     * </p>
     */
    // Запускается каждые 6 часов (можно изменить на нужный интервал)
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupBlockedPosts() {
        logger.info("Запуск очистки заблокированных постов...");

        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(15);
        int deletedCount = 0;

        // Удаляем заблокированные товары
        List<PostText> blockedPosts = postRepo.findAll().stream()
                .filter(post -> post.isBlocked() &&
                        post.getBlockedAt() != null &&
                        post.getBlockedAt().isBefore(fiveDaysAgo))
                .toList();

        for (PostText post : blockedPosts) {
            postRepo.delete(post);
            deletedCount++;
            logger.info("Удален заблокированный товар ID: {}, заблокирован: {}",
                    post.getId(), post.getBlockedAt());
        }

        // Удаляем заблокированные услуги
        List<PostTextJob> blockedJobs = postRepoJob.findAll().stream()
                .filter(job -> job.isBlocked() &&
                        job.getBlockedAt() != null &&
                        job.getBlockedAt().isBefore(fiveDaysAgo))
                .toList();

        for (PostTextJob job : blockedJobs) {
            postRepoJob.delete(job);
            deletedCount++;
            logger.info("Удалена заблокированная услуга ID: {}, заблокирована: {}",
                    job.getId(), job.getBlockedAt());
        }

        // Удаляем заблокированные вакансии
        List<Vacancy> blockedVacancies = postVacancyRepository.findAll().stream()
                .filter(vacancy -> vacancy.isBlocked() &&
                        vacancy.getBlockedAt() != null &&
                        vacancy.getBlockedAt().isBefore(fiveDaysAgo))
                .toList();

        for (Vacancy vacancy : blockedVacancies) {
            postVacancyRepository.delete(vacancy);
            deletedCount++;
            logger.info("Удалена заблокированная вакансия ID: {}, заблокирована: {}",
                    vacancy.getId(), vacancy.getBlockedAt());
        }

        // Удаляем заблокированные резюме
        List<Resume> blockedResume = postResumeRepository.findAll().stream()
                .filter(resume -> resume.isBlocked() &&
                        resume.getBlockedAt() != null &&
                        resume.getBlockedAt().isBefore(fiveDaysAgo))
                .toList();

        for (Resume resume : blockedResume) {
            postResumeRepository.delete(resume);
            deletedCount++;
            logger.info("Удалена заблокированная резюме ID: {}, заблокирована: {}",
                    resume.getId(), resume.getBlockedAt());
        }

        logger.info("Очистка завершена. Удалено постов: {}", deletedCount);
    }
}