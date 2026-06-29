package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Сервис для получения статистики пользователя (количество постов разных типов).
 * <p>
 * Предоставляет методы для подсчета активных (не архивных и не заблокированных)
 * постов пользователя: товаров, услуг, вакансий и резюме.
 * </p>
 *
 * <p><b>Кеширование:</b></p>
 * <ul>
 *   <li>Все результаты кешируются в кеше {@code userCounts}</li>
 *   <li>Ключ кеша формируется по шаблону: {@code тип_поста_ + ID пользователя}</li>
 *   <li>При изменении любого поста необходимо вызвать {@link #clearUserCountsCache()}</li>
 * </ul>
 *
 * <p><b>Производительность:</b> благодаря кешированию повторные вызовы
 * не нагружают базу данных.</p>
 *
 * @author YourTeam
 * @version 1.0
 * @see PostRepo
 * @see PostRepoJob
 * @see PostVacancyRepository
 * @see PostResumeRepository
 */
@RequiredArgsConstructor
@Service
public class UserStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(UserStatisticsService.class);

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    /**
     * Возвращает количество активных товаров пользователя.
     * <p>
     * Активными считаются товары, у которых {@code archived = false} и {@code blocked = false}.
     * Результат кешируется.
     * </p>
     *
     * @param user пользователь
     * @return количество активных товаров
     */
    @Cacheable(value = "userCounts", key = "'products_' + #user.id")
    public long getProductsCount(User user) {
        log.debug("Cache miss for products count");
        long count = postRepo.countByUserAndArchivedFalseAndBlockedFalse(user);
        log.debug("Products count: {}", count);
        return count;
    }

    /**
     * Возвращает количество активных услуг пользователя.
     * <p>
     * Активными считаются услуги, у которых {@code archived = false} и {@code blocked = false}.
     * Результат кешируется.
     * </p>
     *
     * @param user пользователь
     * @return количество активных услуг
     */
    @Cacheable(value = "userCounts", key = "'services_' + #user.id")
    public long getServicesCount(User user) {
        log.debug("Cache miss for services count");
        long count = postRepoJob.countByUserAndArchivedFalseAndBlockedFalse(user);
        log.debug("Services count: {}", count);
        return count;
    }

    /**
     * Возвращает количество активных вакансий пользователя.
     * <p>
     * Активными считаются вакансии, у которых {@code archived = false} и {@code blocked = false}.
     * Результат кешируется.
     * </p>
     *
     * @param user пользователь
     * @return количество активных вакансий
     */
    @Cacheable(value = "userCounts", key = "'vacancies_' + #user.id")
    public long getVacanciesCount(User user) {
        log.debug("Cache miss for vacancies count");
        long count = postVacancyRepository.countByUserAndArchivedFalseAndBlockedFalse(user);
        log.debug("Vacancies count: {}", count);
        return count;
    }

    /**
     * Возвращает количество активных резюме пользователя.
     * <p>
     * Активными считаются резюме, у которых {@code archived = false} и {@code blocked = false}.
     * Результат кешируется.
     * </p>
     *
     * @param user пользователь
     * @return количество активных резюме
     */
    @Cacheable(value = "userCounts", key = "'resume_' + #user.id")
    public long getResumeCount(User user) {
        log.debug("Cache miss for resume count");
        long count = postResumeRepository.countByUserAndArchivedFalseAndBlockedFalse(user);
        log.debug("Resume count: {}", count);
        return count;
    }

    /**
     * Очищает весь кеш счетчиков пользователей.
     * <p>
     * Должен вызываться при любых изменениях, влияющих на количество постов:
     * <ul>
     *   <li>Создание нового поста</li>
     *   <li>Удаление поста</li>
     *   <li>Архивация/разархивация поста</li>
     *   <li>Блокировка/разблокировка поста</li>
     * </ul>
     * </p>
     * <p>
     * <b>Важно:</b> метод очищает ВЕСЬ кеш {@code userCounts}, а не только для конкретного пользователя.
     * Если нагрузка высокая, можно переделать на точечную очистку по ключу.
     * </p>
     */
    @CacheEvict(value = "userCounts", allEntries = true)
    public void clearUserCountsCache() {
        log.debug("User counts cache cleared (all entries)");
    }
}