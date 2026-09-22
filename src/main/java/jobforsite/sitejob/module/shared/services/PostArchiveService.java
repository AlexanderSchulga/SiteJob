package jobforsite.sitejob.module.shared.services;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.shared.repository.FavorRepo;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для автоматической архивации и очистки устаревших объявлений.
 * <p>
 * Основные задачи:
 * <ul>
 *   <li>Архивация объявлений, прошедших модерацию более 30 дней назад</li>
 *   <li>Удаление объявлений, находящихся в архиве более 30 дней</li>
 *   <li>Автоматическое выполнение по расписанию (ночные задачи)</li>
 * </ul>
 * </p>
 * <p>
 * Архивация не затрагивает объявления на модерации, заблокированные или уже архивированные.
 * При архивации объявление также удаляется из избранного у всех пользователей.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PostArchiveService {
    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;
    private final FavorRepo favorRepo;

    /**
     * Архивирует старые объявления (товары, услуги, вакансии, резюме).
     * <p>
     * Условия архивации:
     * <ul>
     *   <li>Объявление не на модерации</li>
     *   <li>Объявление не заблокировано</li>
     *   <li>Дата модерации не null и старше 90 дней</li>
     *   <li>Объявление ещё не в архиве</li>
     * </ul>
     * При архивации объявление удаляется из избранного у всех пользователей.
     * </p>
     * <p>
     * Запускается ежедневно в 02:00.
     * </p>
     */
    // Запускается каждый день в 2:00 ночи
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void archiveOldPosts() {
        log.info("Запуск архивации старых постов...");

        //LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusMinutes(5);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(90);
        LocalDateTime now = LocalDateTime.now();
        int archivedCount = 0;

        // Архивируем товары старше 90 дней
        List<PostText> oldProducts = postRepo.findAll().stream()
                .filter(post -> !post.isModeration())
                .filter(post -> !post.isBlocked())
                .filter(post -> post.getModeratedAt() != null)
                .filter(post -> post.getModeratedAt().isBefore(thirtyDaysAgo))
                .filter(post -> !post.isArchived())
                .toList();

        for (PostText post : oldProducts) {
            post.setArchived(true);
            post.setArchivedAt(now);
            archivedCount++;

            favorRepo.deleteByPostId(post.getId());
            post.setPremium(false);
            post.setPremiumPriority(0);
            post.setDonate(false);
            log.info("Товар ID: {} отправлен в архив и удален из избранного. Дата модерации: {}",
                    post.getId(), post.getModeratedAt());
        }
        postRepo.saveAll(oldProducts);

        // Архивируем услуги старше 90 дней
        List<PostTextJob> oldServices = postRepoJob.findAll().stream()
                .filter(service -> !service.isModeration())
                .filter(service -> !service.isBlocked())
                .filter(service -> service.getModeratedAt() != null)
                .filter(service -> service.getModeratedAt().isBefore(thirtyDaysAgo))
                .filter(service -> !service.isArchived())
                .toList();

        for (PostTextJob service : oldServices) {
            service.setArchived(true);
            service.setArchivedAt(now);
            archivedCount++;

            favorRepo.deleteByPostJobId(service.getId());
            service.setPremium(false);
            service.setPremiumPriority(0);
            service.setDonate(false);
            log.info("Услуга ID: {} отправлена в архив. Дата модерации: {}",
                    service.getId(), service.getModeratedAt());
        }
        postRepoJob.saveAll(oldServices);

        // Архивируем вакансии старше 90 дней
        List<Vacancy> oldVacancies = postVacancyRepository.findAll().stream()
                .filter(vacancy -> !vacancy.isModeration())
                .filter(vacancy -> !vacancy.isBlocked())
                .filter(vacancy -> vacancy.getModeratedAt() != null)
                .filter(vacancy -> vacancy.getModeratedAt().isBefore(thirtyDaysAgo))
                .filter(vacancy -> !vacancy.isArchived())
                .toList();

        for (Vacancy vacancy : oldVacancies) {
            vacancy.setArchived(true);
            vacancy.setArchivedAt(now);
            archivedCount++;

            favorRepo.deleteByVacancyId(vacancy.getId());
            vacancy.setPremium(false);
            vacancy.setPremiumPriority(0);
            vacancy.setDonate(false);
            log.info("Вакансия ID: {} отправлена в архив. Дата модерации: {}",
                    vacancy.getId(), vacancy.getModeratedAt());
        }
        postVacancyRepository.saveAll(oldVacancies);

        List<Resume> oldResume = postResumeRepository.findAll().stream()
                .filter(resume -> !resume.isModeration())
                .filter(resume -> !resume.isBlocked())
                .filter(resume -> resume.getModeratedAt() != null)
                .filter(resume -> resume.getModeratedAt().isBefore(thirtyDaysAgo))
                .filter(resume -> !resume.isArchived())
                .toList();

        for (Resume resume : oldResume) {
            resume.setArchived(true);
            resume.setArchivedAt(now);
            archivedCount++;

            favorRepo.deleteByResumeId(resume.getId());
            log.info("Резюме ID: {} отправлена в архив. Дата модерации: {}",
                    resume.getId(), resume.getModeratedAt());
        }
        postResumeRepository.saveAll(oldResume);

        log.info("Архивация завершена. Отправлено в архив: {} объявлений", archivedCount);
    }

    /**
     * Окончательно удаляет объявления, находящиеся в архиве более 30 дней.
     * <p>
     * Условия удаления:
     * <ul>
     *   <li>Объявление архивировано (isArchived = true)</li>
     *   <li>Дата архивации не null и старше 60 дней</li>
     * </ul>
     * Объявления удаляются из базы данных без возможности восстановления.
     * </p>
     * <p>
     * Запускается ежедневно в 03:00 (после завершения архивации).
     * </p>
     */
    // Запускается каждый день в 3:00 ночи
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupArchivedPosts() {
        log.info("Запуск очистки архивных постов...");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(60);
        //LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusMinutes(5);
        int deletedCount = 0;

        // Удаляем архивные товары
        List<PostText> ArhivedPosts = postRepo.findAll().stream()
                .filter(post -> post.isArchived() &&
                        post.getArchivedAt() != null &&
                        post.getArchivedAt().isBefore(thirtyDaysAgo))
                .toList();

        for (PostText post : ArhivedPosts) {
            postRepo.delete(post);
            deletedCount++;
            log.info("Удален архивный товар ID: {}, архивирован: {}",
                    post.getId(), post.getArchivedAt());
        }

        // Удаляем архивные услуги
        List<PostTextJob> ArhivedPostsJobs = postRepoJob.findAll().stream()
                .filter(job -> job.isArchived() &&
                        job.getArchivedAt() != null &&
                        job.getArchivedAt().isBefore(thirtyDaysAgo))
                .toList();

        for (PostTextJob job : ArhivedPostsJobs) {
            postRepoJob.delete(job);
            deletedCount++;
            log.info("Удалена архивная услуга ID: {}, архивирована: {}",
                    job.getId(), job.getArchivedAt());
        }

        // Удаляем архивные вакансии
        List<Vacancy> ArhivedPostsVacancies = postVacancyRepository.findAll().stream()
                .filter(vacancy -> vacancy.isArchived() &&
                        vacancy.getArchivedAt() != null &&
                        vacancy.getArchivedAt().isBefore(thirtyDaysAgo))
                .toList();

        for (Vacancy vacancy : ArhivedPostsVacancies) {
            postVacancyRepository.delete(vacancy);
            deletedCount++;
            log.info("Удалена архивная вакансия ID: {}, архивирована: {}",
                    vacancy.getId(), vacancy.getArchivedAt());
        }

        List<Resume> ArhivedPostsResume = postResumeRepository.findAll().stream()
                .filter(resume -> resume.isArchived() &&
                        resume.getArchivedAt() != null &&
                        resume.getArchivedAt().isBefore(thirtyDaysAgo))
                .toList();

        for (Resume resume : ArhivedPostsResume) {
            postResumeRepository.delete(resume);
            deletedCount++;
            log.info("Удалена архивная резюме ID: {}, архивирована: {}",
                    resume.getId(), resume.getArchivedAt());
        }

        log.info("Очистка завершена. Удалено постов: {}", deletedCount);
    }
}