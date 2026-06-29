package jobforsite.sitejob.module.users.service;

import jobforsite.sitejob.module.users.dto.UserRatingDTO;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для работы с пользователями и их рейтингами.
 * <p>
 * Предоставляет методы для получения рейтинга пользователя (средняя оценка и количество отзывов)
 * как по ID, так и по хэш-идентификатору.
 * </p>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *   <li>Если пользователь не найден по хэшу — возвращается рейтинг с нулевыми значениями</li>
 *   <li>Если у пользователя нет отзывов — средняя оценка = 0, количество = 0</li>
 *   <li>Рейтинг вычисляется на лету, без кеширования (кеширование на уровне контроллера или выше)</li>
 * </ul>
 *
 * @author YourTeam
 * @version 1.0
 * @see UserRatingDTO
 * @see RatingRepository
 */
@RequiredArgsConstructor
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    /**
     * Возвращает рейтинг пользователя по его ID.
     * <p>
     * Вычисляет среднюю оценку и общее количество отзывов для указанного пользователя.
     * Если отзывов нет, возвращается рейтинг 0.0 с количеством 0.
     * </p>
     *
     * @param userId идентификатор пользователя
     * @return объект {@link UserRatingDTO} с рейтингом и количеством отзывов
     */
    public UserRatingDTO getUserRating(Long userId) {
        log.debug("Calculating rating for user");

        // Создаем временный объект User только с ID для запросов к репозиторию
        User user = new User();
        user.setId(userId);

        Double averageRating = ratingRepository.findAverageRatingBySeller(user);
        Long ratingCount = ratingRepository.countRatingsBySeller(user);

        double finalRating = averageRating != null ? averageRating : 0.0;
        int finalCount = ratingCount != null ? ratingCount.intValue() : 0;

        log.debug("Rating calculated: {} reviews, average {}", finalCount, finalRating);

        return new UserRatingDTO(finalRating, finalCount);
    }

    /**
     * Возвращает рейтинг пользователя по его хэш-идентификатору.
     * <p>
     * Сначала находит пользователя по хэшу. Если пользователь не найден,
     * возвращается рейтинг с нулевыми значениями (без выбрасывания исключения).
     * </p>
     *
     * @param hashId уникальный хэш-идентификатор пользователя
     * @return объект {@link UserRatingDTO} с рейтингом и количеством отзывов,
     *         или пустой рейтинг (0.0, 0) если пользователь не найден
     */
    public UserRatingDTO getUserRatingHash(String hashId) {
        log.debug("Looking up user by hash for rating calculation");

        Optional<User> userOpt = userRepository.findByHashId(hashId);
        if (userOpt.isEmpty()) {
            log.debug("User not found by hash, returning empty rating");
            return new UserRatingDTO(0.0, 0);
        }

        User user = userOpt.get();
        Double averageRating = ratingRepository.findAverageRatingBySeller(user);
        Long ratingCount = ratingRepository.countRatingsBySeller(user);

        double finalRating = averageRating != null ? averageRating : 0.0;
        int finalCount = ratingCount != null ? ratingCount.intValue() : 0;

        log.debug("Rating calculated for hash user: {} reviews, average {}", finalCount, finalRating);

        return new UserRatingDTO(finalRating, finalCount);
    }
}