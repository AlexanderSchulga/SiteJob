package jobforsite.sitejob.module.users.repository;

import jobforsite.sitejob.module.users.model.Rating;
import jobforsite.sitejob.module.users.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Репозиторий для работы с рейтингами пользователей.
 * <p>
 * Предоставляет методы для поиска, подсчёта и кэширования рейтингов.
 * Использует кэширование Spring Cache для оптимизации частых запросов
 * среднего рейтинга и количества оценок.
 * </p>
 *
 * @author JobForSite Team
 * @version 1.0
 * @see Rating
 * @see User
 */
public interface RatingRepository extends JpaRepository<Rating, Long> {


    /**
     * Находит оценку, поставленную конкретным пользователем конкретному продавцу.
     * <p>
     * Используется для проверки, оценивал ли уже пользователь данного продавца
     * (можно оценить только один раз).
     * </p>
     *
     * @param rater  пользователь, который ставит оценку
     * @param seller продавец, которому ставят оценку
     * @return Optional с найденной оценкой
     */
    Optional<Rating> findByRaterAndSeller(User rater, User seller);

    /**
     * Вычисляет средний рейтинг продавца.
     *
     * @param seller продавец
     * @return среднее арифметическое всех оценок продавца, или null если оценок нет
     */
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.seller = :seller")
    Double findAverageRatingBySeller(@Param("seller") User seller);

    /**
     * Подсчитывает количество оценок, полученных продавцом.
     *
     * @param seller продавец
     * @return количество оценок
     */
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.seller = :seller")
    Long countRatingsBySeller(@Param("seller") User seller);

    // ========== МЕТОДЫ С КЭШИРОВАНИЕМ ПО ID ==========

    /**
     * Вычисляет средний рейтинг продавца по его ID.
     * <p>
     * Результат кэшируется в {@code sellerRatings} с ключом = ID продавца.
     * </p>
     *
     * @param sellerId ID продавца
     * @return средний рейтинг, или null если оценок нет
     */
    @Cacheable(value = "sellerRatings", key = "#sellerId")
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.seller.id = :sellerId")
    Double findAverageRatingBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Подсчитывает количество оценок продавца по его ID.
     * <p>
     * Результат кэшируется в {@code sellerRatingsCount} с ключом = ID продавца.
     * </p>
     *
     * @param sellerId ID продавца
     * @return количество оценок
     */
    @Cacheable(value = "sellerRatingsCount", key = "#sellerId")
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.seller.id = :sellerId")
    Long countRatingsBySellerId(@Param("sellerId") Long sellerId);

    // ========== МЕТОДЫ С КЭШИРОВАНИЕМ ПО HASH ==========

    /**
     * Вычисляет средний рейтинг продавца по его hashId.
     * <p>
     * Результат кэшируется в {@code sellerRatingsByHash} с ключом = hashId.
     * Реальная реализация должна быть переопределена или использовать другой метод,
     * так как данный метод возвращает null (заглушка для кэша).
     * </p>
     *
     * @param hashId уникальный хеш-идентификатор пользователя
     * @return средний рейтинг, требуется переопределение логики
     */
    @Cacheable(value = "sellerRatingsByHash", key = "#hashId")
    default Double findAverageRatingBySellerHashId(String hashId) {
        // Заглушка — реальная реализация через сервисный слой
        return null;
    }

    /**
     * Подсчитывает количество оценок продавца по его hashId.
     * <p>
     * Результат кэшируется в {@code sellerRatingsCountByHash} с ключом = hashId.
     * </p>
     *
     * @param hashId уникальный хеш-идентификатор пользователя
     * @return количество оценок, требуется переопределение логики
     */
    @Cacheable(value = "sellerRatingsCountByHash", key = "#hashId")
    default Long countRatingsBySellerHashId(String hashId) {

        return null;
    }

    // ========== МЕТОДЫ ОЧИСТКИ КЭША ==========

    /**
     * Очищает кэш рейтингов для конкретного продавца по его ID.
     * <p>
     * Вызывается после добавления/изменения оценки,
     * чтобы при следующем запросе средний рейтинг пересчитался.
     * </p>
     *
     * @param sellerId ID продавца
     */
    @CacheEvict(value = {"sellerRatings", "sellerRatingsCount", "sellerRatingsByHash", "sellerRatingsCountByHash"},
            key = "#sellerId")
    default void evictSellerRatingCache(Long sellerId) {

    }

    /**
     * Очищает кэш рейтингов для конкретного продавца по его hashId.
     *
     * @param hashId уникальный хеш-идентификатор пользователя
     */
    @CacheEvict(value = {"sellerRatingsByHash", "sellerRatingsCountByHash"},
            key = "#hashId")
    default void evictSellerRatingCacheByHash(String hashId) {

    }

    /**
     * Полностью очищает ВСЕ кэши рейтингов.
     * <p>
     * Используется с осторожностью, например, при массовых операциях
     * или принудительном сбросе кэша через админ-панель.
     * </p>
     */
    @CacheEvict(value = {"sellerRatings", "sellerRatingsCount", "sellerRatingsByHash", "sellerRatingsCountByHash"},
            allEntries = true)
    default void evictAllRatingsCache() {

    }
}