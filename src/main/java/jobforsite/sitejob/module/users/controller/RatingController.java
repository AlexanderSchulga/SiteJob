package jobforsite.sitejob.module.users.controller;

import jobforsite.sitejob.module.users.model.Rating;
import jobforsite.sitejob.module.users.model.User;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import jobforsite.sitejob.module.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST-контроллер для управления рейтингами пользователей (продавцов).
 * <p>
 * Обеспечивает возможность аутентифицированным пользователям оценивать продавцов,
 * получать средний рейтинг и количество оценок, а также управлять кэшированием.
 * </p>
 *
 * @author YourTeam
 * @version 1.0
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rating")
public class RatingController {

    private static final Logger logger = LoggerFactory.getLogger(RatingController.class);

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    /**
     * Оценивает продавца по его числовому ID или хэш-идентификатору.
     * <p>
     * Если пользователь уже оценивал данного продавца, обновляет существующую оценку,
     * иначе создаёт новую. После успешной операции сбрасывает все кэши рейтингов.
     * </p>
     *
     * @param sellerId идентификатор продавца (числовой или хэш-строка)
     * @param score    оценка от 1 до 5
     * @param authentication объект аутентификации текущего пользователя
     * @return ResponseEntity с результатом операции и текущим рейтингом продавца
     */
    @CacheEvict(value = {"sellerRatings", "sellerRatingsCount", "sellerRatingsByHash",
            "sellerRatingsCountByHash"}, allEntries = true)
    @PostMapping("/rate/{sellerId}")
    public ResponseEntity<?> rateSeller(
            @PathVariable String sellerId,
            @RequestParam int score,
            Authentication authentication) {

        logger.info("Rating request - seller identifier: {}, score: {}, authenticated: {}",
                sellerId, score, authentication != null && authentication.isAuthenticated());

        try {
            // Проверка авторизации
            if (authentication == null || !authentication.isAuthenticated()) {
                logger.warn("Unauthorized rating attempt");
                return ResponseEntity.status(401).body("Необходима авторизация");
            }

            // Валидация оценки
            if (score < 1 || score > 5) {
                logger.warn("Invalid score value: {}", score);
                return ResponseEntity.badRequest().body("Оценка должна быть от 1 до 5");
            }

            // Получение текущего пользователя (оценщика)
            Object principal = authentication.getPrincipal();
            User rater;

            if (principal instanceof User) {
                rater = (User) principal;
                logger.info("Rater found");
            } else {
                logger.error("Unexpected principal type: {}", principal.getClass().getName());
                return ResponseEntity.status(401).body("Неверный тип аутентификации");
            }

            // Поиск продавца по ID или хэшу
            User seller;
            try {
                Long numericId = Long.parseLong(sellerId);
                seller = userRepository.findById(numericId)
                        .orElseThrow(() -> new RuntimeException("Продавец не найден"));
                logger.info("Seller found by numeric ID");
            } catch (NumberFormatException e) {
                seller = userRepository.findByHashId(sellerId)
                        .orElseThrow(() -> new RuntimeException("Продавец не найден по UUID"));
                logger.info("Seller found by hash ID");
            }

            // Запрет на оценку самого себя
            if (rater.getId().equals(seller.getId())) {
                logger.warn("Self-rating attempt detected");
                return ResponseEntity.badRequest().body("Нельзя оценивать самого себя");
            }

            // Проверка, оценивал ли пользователь этого продавца ранее
            Optional<Rating> existingRating = ratingRepository.findByRaterAndSeller(rater, seller);

            if (existingRating.isPresent()) {
                // Обновление существующей оценки
                Rating rating = existingRating.get();
                rating.setScore(score);
                ratingRepository.save(rating);
                logger.info("Updated existing rating for seller {}", sellerId);
            } else {
                // Создание новой оценки
                Rating rating = new Rating(rater, seller, score);
                ratingRepository.save(rating);
                logger.info("Created new rating for seller {}", sellerId);
            }

            // Получение обновлённых показателей рейтинга
            Double averageRating = ratingRepository.findAverageRatingBySeller(seller);
            Long ratingCount = ratingRepository.countRatingsBySeller(seller);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0);
            response.put("ratingCount", ratingCount);
            response.put("userRating", score);

            logger.info("Rating successful - new average: {}, total ratings: {}", response.get("averageRating"), ratingCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error while processing rating: ", e);
            return ResponseEntity.badRequest().body("Ошибка при оценке: " + e.getMessage());
        }
    }

    /**
     * Возвращает рейтинг продавца по его идентификатору (числовому или хэш-строке).
     * <p>
     * Результат кэшируется с использованием аннотации {@link Cacheable}.
     * Если аутентификация присутствует, также возвращает оценку текущего пользователя.
     * </p>
     *
     * @param sellerId идентификатор продавца
     * @param authentication объект аутентификации (может быть null)
     * @return ResponseEntity с информацией о рейтинге продавца
     */
    @Cacheable(value = "sellerRatings", key = "#sellerId", unless = "#result == null")
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<?> getSellerRating(
            @PathVariable String sellerId,
            Authentication authentication) {

        logger.debug("Cache miss for seller rating: {}", sellerId);

        try {
            User seller;

            // Поиск продавца по ID или хэшу
            try {
                Long numericId = Long.parseLong(sellerId);
                seller = userRepository.findById(numericId)
                        .orElseThrow(() -> new RuntimeException("Продавец не найден"));
            } catch (NumberFormatException e) {
                seller = userRepository.findByHashId(sellerId)
                        .orElseThrow(() -> new RuntimeException("Продавец не найден по UUID"));
            }

            // Получение кэшируемых данных через ID продавца
            Double averageRating = ratingRepository.findAverageRatingBySellerId(seller.getId());
            Long ratingCount = ratingRepository.countRatingsBySellerId(seller.getId());

            Integer userRating = null;
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof User) {
                    User currentUser = (User) principal;
                    Optional<Rating> userRatingObj = ratingRepository.findByRaterAndSeller(currentUser, seller);
                    if (userRatingObj.isPresent()) {
                        userRating = userRatingObj.get().getScore();
                        logger.debug("User rating found for seller {}", sellerId);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0);
            response.put("ratingCount", ratingCount);
            response.put("userRating", userRating);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error while retrieving seller rating: ", e);
            return ResponseEntity.badRequest().body("Ошибка при получении рейтинга");
        }
    }

    /**
     * Получает рейтинг продавца по его хэш-идентификатору (без учёта аутентификации).
     *
     * @param hashId хэш-идентификатор продавца
     * @return ResponseEntity со средним рейтингом и количеством оценок
     */
    @GetMapping("/seller/hash/{hashId}")
    @ResponseBody
    public ResponseEntity<?> getSellerRatingByHashId(@PathVariable String hashId) {
        User seller = userRepository.findByHashId(hashId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Продавец не найден"));

        // Используем кэширование через ID продавца
        Double averageRating = ratingRepository.findAverageRatingBySellerId(seller.getId());
        Long ratingCount = ratingRepository.countRatingsBySellerId(seller.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("averageRating", averageRating != null ? averageRating : 0);
        response.put("ratingCount", ratingCount != null ? ratingCount : 0);

        logger.debug("Rating by hashId {} returned", hashId);
        return ResponseEntity.ok(response);
    }

    /**
     * Оценивает продавца по его хэш-идентификатору.
     * <p>
     * Аналогичен методу {@link #rateSeller(String, int, Authentication)},
     * но использует только хэш-ID продавца и сбрасывает оба связанных кэша.
     * </p>
     *
     * @param hashId хэш-идентификатор продавца
     * @param score оценка от 1 до 5
     * @param authentication объект аутентификации
     * @return ResponseEntity с результатом операции
     */
    @PostMapping("/rate/hash/{hashId}")
    @ResponseBody
    public ResponseEntity<?> rateSellerByHashId(@PathVariable String hashId,
                                                @RequestParam int score,
                                                Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized rating attempt via hashId");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Необходимо авторизоваться"));
        }

        User currentUser = (User) authentication.getPrincipal();
        User seller = userRepository.findByHashId(hashId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Продавец не найден"));

        if (currentUser.getId().equals(seller.getId())) {
            logger.warn("Self-rating attempt via hashId");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Нельзя оценивать самого себя"));
        }

        if (score < 1 || score > 5) {
            logger.warn("Invalid score via hashId: {}", score);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Оценка должна быть от 1 до 5"));
        }

        try {
            Rating rating = ratingRepository.findByRaterAndSeller(currentUser, seller)
                    .orElse(new Rating());
            rating.setRater(currentUser);
            rating.setSeller(seller);
            rating.setScore(score);
            ratingRepository.save(rating);

            // Очистка обоих кэшей (по ID и по хэшу)
            ratingRepository.evictSellerRatingCache(seller.getId());
            ratingRepository.evictSellerRatingCacheByHash(hashId);

            Double averageRating = ratingRepository.findAverageRatingBySellerId(seller.getId());
            Long ratingCount = ratingRepository.countRatingsBySellerId(seller.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("averageRating", averageRating != null ? averageRating : 0);
            response.put("ratingCount", ratingCount != null ? ratingCount : 0);

            logger.info("Rating by hashId successful for seller {}", hashId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error while saving rating by hashId: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Ошибка при сохранении оценки: " + e.getMessage()));
        }
    }
}