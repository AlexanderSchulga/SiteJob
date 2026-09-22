package jobforsite.sitejob.module.product.repository;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.users.model.User;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Репозиторий для работы с сущностью PostText (текстовые объявления).
 * Содержит методы для поиска, фильтрации, пагинации объявлений по различным критериям:
 * - активные объявления (не заблокированы, не на модерации, не архивированы)
 * - заблокированные объявления
 * - объявления на модерации
 * - объявления пользователя
 * - поиск по заголовку и имени пользователя
 * - фильтрация по категории, региону, городу
 */
public interface PostRepo extends JpaRepository<PostText, Long> {


    // ========== Базовые методы подсчета и выборки ==========

    /**
     * Подсчитывает количество активных (не архивных, не заблокированных) объявлений пользователя.
     * @param targetUser пользователь
     * @return количество объявлений
     */
    long countByUserAndArchivedFalseAndBlockedFalse(User targetUser);


    // ========== Поиск по заблокированным объявлениям ==========

    /**
     * Поиск заблокированных объявлений по заголовку (без учета регистра, частичное совпадение).
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.blocked = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostText> searchByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по имени пользователя (nameuse) (без учета регистра, частичное совпадение).
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.blocked = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))")
    Slice<PostText> searchByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск заблокированных объявлений по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез заблокированных объявлений
     */
    @Query("SELECT p FROM PostText p WHERE p.blocked = true AND " +
            "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')))")
    Slice<PostText> searchByTitleOrUserName(@Param("query") String query, Pageable pageable);


    // ========== Простые методы поиска ==========

    /**
     * Находит все объявления пользователя.
     * @param user пользователь
     * @return список объявлений пользователя
     */
    List<PostText> findByUser(User user);

    /**
     * Находит объявления по категории.
     * @param category категория
     * @return список объявлений в категории
     */
    List<PostText> findByCategory(String category);

    /**
     * Находит объявления по заголовку (без учета регистра, частичное совпадение).
     * @param title заголовок для поиска
     * @return список объявлений
     */
    List<PostText> findByTitleContainingIgnoreCase(String title);

    /**
     * Подсчитывает количество объявлений пользователя.
     * @param user пользователь
     * @return количество объявлений
     */
    long countByUser(User user);

    /**
     * Находит все объявления с пагинацией.
     * @param pageable параметры пагинации
     * @return страница объявлений
     */
    Page<PostText> findAll(Pageable pageable);

    /**
     * Находит объявления пользователя с пагинацией.
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return страница объявлений пользователя
     */
    Page<PostText> findByUser(User user, Pageable pageable);


    /**
     * Подсчитывает количество объявлений на модерации.
     * @return количество объявлений на модерации
     */
    long countByModerationTrue();

    // ========== Методы работы с hashId ==========

    /**
     * Находит объявление по hashId.
     * @param hashId уникальный хеш-идентификатор
     * @return Optional с объявлением или пустой
     */
    Optional<PostText> findByHashId(String hashId);

    /**
     * Проверяет существование объявления с указанным hashId.
     * @param hashId уникальный хеш-идентификатор
     * @return true если существует
     */
    boolean existsByHashId(String hashId);


// ========== МЕТОДЫ ДЛЯ ВЫБОРКИ ID ПО ТИПАМ ==========

    /**
     * Поиск ID базовых объявлений (TYPE(p) = PostText)
     */
    @Query("SELECT p.id FROM PostText p WHERE TYPE(p) = PostText AND p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:city IS NULL OR p.user.city = :city " +
            "     OR (:city = 'other_none' AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR p.user.city = 'other_none'))) " +
            "ORDER BY p.moderatedAt DESC")
    Slice<Long> findBasePostIdsWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("city") String city,
            Pageable pageable);

    /**
     * УНИВЕРСАЛЬНЫЙ МЕТОД ПОИСКА ID - использует раздельные запросы для каждого типа
     */
    default Slice<Long> findPostIdsWithFiltersSlice(
            PostRepoExtends postRepoExtends,
            String search, String category, String region, String city, String propertyType, Integer roomsCount,
            Integer floorsApartmentMin, Integer floorsApartmentMax, Integer floorsCountMin, Integer floorsCountMax,
            Integer floorsCountHouseMin, Integer floorsCountHouseMax, Boolean hasElevator, Integer buildRealityYearRangeMin,
            Integer buildRealityYearRangeMax, Integer buildHouseYearRangeMin, Integer buildHouseYearRangeMax,
            Integer houseAreaMin, Integer houseAreaMax, Integer landAreaMin, Integer landAreaMax,
            Integer distanceToCityMin, Integer distanceToCityMax, String vehicleType, String vehicleOrigin, Integer vehicleYearMin,
            Integer vehicleYearMax, String materialUp, String repairUp, String materialHouse, String repairHouse,
            Integer distanceToCityHouseMin, Integer distanceToCityHouseMax, Integer landAreaGarageMin,
            Integer landAreaGarageMax, Integer landAreaCommercialMin, Integer landAreaCommercialMax, Integer mileageMin,
            Integer mileageMax, Integer numberOfOwnersMin, Integer numberOfOwnersMax, String autoType, String conditionAuto,
            String constructionsType, String interiorType, String conditionInterior,
            String toolsType, String conditionTools, String appliancesType, String conditionAppliances,
            String electronicsType, String conditionElectronics, String clothingType, String genderClothing,
            String conditionClothing, String genderChildren, String conditionChildren, String sportsKind, String sportsType,
            String genderSports, String conditionSports, String foodKind, String healthKind, String healthType, String genderHealth,
            String animalsKind,
            Pageable pageable) {

        boolean hasRealty = postRepoExtends.hasRealtyFilters(propertyType, roomsCount,
                floorsApartmentMin, floorsApartmentMax,
                floorsCountMin, floorsCountMax,
                floorsCountHouseMin, floorsCountHouseMax,
                hasElevator, buildRealityYearRangeMin, buildRealityYearRangeMax,
                buildHouseYearRangeMin, buildHouseYearRangeMax,
                houseAreaMin, houseAreaMax,
                landAreaMin, landAreaMax,
                distanceToCityMin, distanceToCityMax, materialUp, repairUp,
                materialHouse, repairHouse, distanceToCityHouseMin, distanceToCityHouseMax,
                landAreaGarageMin, landAreaGarageMax, landAreaCommercialMin, landAreaCommercialMax);

        boolean hasVehicle = postRepoExtends.hasVehicleFilters(vehicleType, vehicleOrigin,
                vehicleYearMin, vehicleYearMax);
        boolean hasAuto = PostRepoExtends.hasAutoFilters(autoType, conditionAuto);
        boolean hasConstructions = PostRepoExtends.hasConstructionsFilters(constructionsType);
        boolean hasInterior = PostRepoExtends.hasInteriorFilters(interiorType, conditionInterior);
        boolean hasTools = PostRepoExtends.hasToolsFilters(toolsType, conditionTools);
        boolean hasAppliances = PostRepoExtends.hasAppliancesFilters(appliancesType, conditionAppliances);
        boolean hasElectronics = PostRepoExtends.hasElectronicsFilters(electronicsType, conditionElectronics);
        boolean hasClothing = PostRepoExtends.hasClothingFilters(clothingType, genderClothing, conditionClothing);
        boolean hasChildrenPr = PostRepoExtends.hasChildrenPrFilters(genderChildren, conditionChildren);
        boolean hasSports = PostRepoExtends.hasSportsFilters(sportsKind, sportsType, genderSports, conditionSports);
        boolean hasFood = PostRepoExtends.hasFoodFilters(foodKind);
        boolean hasHealth = PostRepoExtends.hasHealthFilters(healthKind, healthType, genderHealth);
        boolean hasAnimals = PostRepoExtends.hasAnimalsFilters(animalsKind);

        // Если есть специфические фильтры - используем соответствующий запрос
        if (hasRealty) {
            return postRepoExtends.findRealtyPostIdsWithAllFilters(search, category, region, city,
                    propertyType, roomsCount,
                    floorsApartmentMin, floorsApartmentMax,
                    floorsCountMin, floorsCountMax,
                    floorsCountHouseMin, floorsCountHouseMax,
                    hasElevator,
                    buildRealityYearRangeMin, buildRealityYearRangeMax,
                    buildHouseYearRangeMin, buildHouseYearRangeMax,
                    houseAreaMin, houseAreaMax,
                    landAreaMin, landAreaMax,
                    distanceToCityMin, distanceToCityMax,
                    materialUp, repairUp,
                    materialHouse, repairHouse, distanceToCityHouseMin, distanceToCityHouseMax,
                    landAreaGarageMin, landAreaGarageMax, landAreaCommercialMin, landAreaCommercialMax,
                    pageable);
        }

        if (hasVehicle) {
            return postRepoExtends.findVehiclePostIdsWithAllFilters(search, category, region, city,
                    vehicleType, vehicleOrigin, vehicleYearMin, vehicleYearMax, mileageMin, mileageMax,
                    numberOfOwnersMin, numberOfOwnersMax,
                    pageable);
        }

        if (hasAuto) {
            return postRepoExtends.findAutoPostIdsWithAllFilters(search, category, region, city,
                    autoType, conditionAuto,
                    pageable);
        }

        if (hasConstructions) {
            return postRepoExtends.findConstructionsPostIdsWithAllFilters(search, category, region, city,
                    constructionsType,
                    pageable);
        }

        if (hasInterior) {
            return postRepoExtends.findInteriorPostIdsWithAllFilters(search, category, region, city,
                    interiorType,
                    conditionInterior,
                    pageable);
        }
        if (hasTools) {
            return postRepoExtends.findToolsPostIdsWithAllFilters(search, category, region, city,
                    toolsType, conditionTools,
                    pageable);
        }

        if (hasAppliances) {
            return postRepoExtends.findAppliancesPostIdsWithAllFilters(search, category, region, city,
                    appliancesType, conditionAppliances,
                    pageable);
        }

        if (hasElectronics) {
            return postRepoExtends.findElectronicsPostIdsWithAllFilters(search, category, region, city,
                    electronicsType, conditionElectronics,
                    pageable);
        }

        if (hasClothing) {
            return postRepoExtends.findClothingPostIdsWithAllFilters(search, category, region, city,
                    clothingType, genderClothing, conditionClothing,
                    pageable);
        }

        if (hasChildrenPr) {
            return postRepoExtends.findChildrenPrPostIdsWithAllFilters(search, category, region, city,
                    genderChildren, conditionChildren,
                    pageable);
        }

        if (hasSports) {
            return postRepoExtends.findSportsPostIdsWithAllFilters(search, category, region, city,
                    sportsKind, sportsType, genderSports, conditionSports,
                    pageable);
        }

        if (hasFood) {
            return postRepoExtends.findFoodPostIdsWithAllFilters(search, category, region, city,
                    foodKind,
                    pageable);
        }
        if (hasHealth) {
            return postRepoExtends.findHealthPostIdsWithAllFilters(search, category, region, city,
                    healthKind, healthType, genderHealth,
                    pageable);
        }
        if (hasAnimals) {
            return postRepoExtends.findAnimalsPostIdsWithAllFilters(search, category, region, city,
                    animalsKind,
                    pageable);
        }

// ===== НЕТ СПЕЦИФИЧЕСКИХ ФИЛЬТРОВ - собираем ID из ВСЕХ типов =====
        List<Long> allIds = new ArrayList<>();

// БАЗОВЫЕ объявления (без подкатегорий)
        Slice<Long> baseIds = findBasePostIdsWithFilters(search, category, region, city, Pageable.unpaged());
        allIds.addAll(baseIds.getContent());

// Недвижимость (без специфических фильтров)
        Slice<Long> realtyIds = postRepoExtends.findAllRealtyIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(realtyIds.getContent());

// Транспорт (без специфических фильтров)
        Slice<Long> vehicleIds = postRepoExtends.findAllVehicleIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(vehicleIds.getContent());

// Автозапчасти (без специфических фильтров)
        Slice<Long> autoIds = postRepoExtends.findAllAutoIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(autoIds.getContent());

// Строительные материалы (без специфических фильтров)
        Slice<Long> constructionsIds = postRepoExtends.findAllConstructionsIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(constructionsIds.getContent());

// Дом и интерьер (без специфических фильтров)
        Slice<Long> interiorIds = postRepoExtends.findAllInteriorIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(interiorIds.getContent());

// Инструменты (без специфических фильтров)
        Slice<Long> toolsIds = postRepoExtends.findAllToolsIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(toolsIds.getContent());

// Бытовая техника (без специфических фильтров)
        Slice<Long> appliancesIds = postRepoExtends.findAllAppliancesIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(appliancesIds.getContent());

// Электроника и гаджеты (без специфических фильтров)
        Slice<Long> electronicsIds = postRepoExtends.findAllElectronicsIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(electronicsIds.getContent());

// Одежда и обувь (без специфических фильтров)
        Slice<Long> clothingIds = postRepoExtends.findAllClothingIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(clothingIds.getContent());

// Детские товары (без специфических фильтров)
        Slice<Long> childrenPrIds = postRepoExtends.findAllChildrenPrIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(childrenPrIds.getContent());

// Товары для спорта (без специфических фильтров)
        Slice<Long> sportsIds = postRepoExtends.findAllSportsIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(sportsIds.getContent());

// Продукты питания (без специфических фильтров)
        Slice<Long> foodIds = postRepoExtends.findAllFoodIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(foodIds.getContent());

// Здоровье и красота (без специфических фильтров)
        Slice<Long> healthIds = postRepoExtends.findAllHealthIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(healthIds.getContent());

// Флора и фауна (без специфических фильтров)
        Slice<Long> animalsIds = postRepoExtends.findAllAnimalsIds(search, category, region, city, Pageable.unpaged());
        allIds.addAll(animalsIds.getContent());

        // Если ничего не найдено
        if (allIds.isEmpty()) {

            return new SliceImpl<>(new ArrayList<>(), pageable, false);
        }

        // Убираем дубликаты
        allIds = allIds.stream().distinct().collect(Collectors.toList());

        // Загружаем посты для сортировки
        List<PostText> postsForSort = findPostsByIdsWithImages(allIds);

        // Получаем сортировку из pageable
        Sort sort = pageable.getSort();
        String sortField = "moderatedAt";
        boolean isAsc = false;

        if (!sort.isEmpty()) {
            Sort.Order order = sort.iterator().next();
            sortField = order.getProperty();
            isAsc = order.isAscending();
        }

        // Сортируем в Java
        Comparator<PostText> comparator;
        if ("price".equals(sortField)) {
            comparator = Comparator.comparing(PostText::getPrice,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            // По умолчанию по дате moderatedAt
            comparator = Comparator.comparing(PostText::getModeratedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if (!isAsc) {
            comparator = comparator.reversed();
        }

        postsForSort.sort(comparator);

        // Извлекаем ID в правильном порядке
        List<Long> sortedIds = postsForSort.stream()
                .map(PostText::getId)
                .collect(Collectors.toList());

        // Применяем пагинацию
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedIds.size());
        boolean hasMore = end < sortedIds.size();

        if (start >= sortedIds.size()) {
            return new SliceImpl<>(new ArrayList<>(), pageable, false);
        }

        List<Long> pagedIds = sortedIds.subList(start, end);

        return new SliceImpl<>(pagedIds, pageable, hasMore);
    }


    // ========== Slice-методы для оптимизированной пагинации ==========

    /**
     * Находит ID активных объявлений пользователя с пагинацией (Slice).
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.user = :user AND p.archived = false AND p.blocked = false AND p.moderation = false ORDER BY p.createdAt DESC")
    Page<Long> findPostIdsByUserWithPagination(@Param("user") User user, Pageable pageable);

    /**
     * Загружает объявления по списку ID с предварительной загрузкой изображений и пользователя.
     * @param ids список ID объявлений
     * @return список объявлений
     */
    @Query("SELECT DISTINCT p FROM PostText p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids")
    List<PostText> findPostsByIdsWithImages(@Param("ids") List<Long> ids);


    /**
     * Находит ID активных объявлений в городах, не входящих в список стандартных городов.
     * @param search поисковый запрос
     * @param category категория
     * @param region регион
     * @param standardCities список стандартных городов
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.archived = false AND p.blocked = false AND p.moderation = false " +
            "AND (:region IS NULL OR p.user.region = :region) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND ((:search IS NULL) OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (p.user.city IS NULL OR p.user.city = '' OR p.user.city = 'other' OR " +
            "p.user.city = 'other_none' OR " +
            "(p.user.city NOT IN :standardCities)) " +
            "ORDER BY p.moderatedAt DESC")
    Slice<Long> findPostIdsByOtherCitiesSlice(
            @Param("search") String search,
            @Param("category") String category,
            @Param("region") String region,
            @Param("standardCities") List<String> standardCities,
            Pageable pageable);

    // ========== Методы для заблокированных объявлений (Slice с ID) ==========

    /**
     * Находит ID заблокированных объявлений активных пользователей.
     * @param pageable параметры пагинации
     * @return срез ID заблокированных объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> findBlockedProductIds(Pageable pageable);

    /**
     * Поиск ID заблокированных объявлений по заголовку.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedProductIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных объявлений по имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedProductIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID заблокированных объявлений по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.blocked = true AND p.user.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.blockedAt DESC, p.id DESC")
    Slice<Long> searchBlockedProductIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Подсчитывает количество заблокированных объявлений от активных пользователей.
     * @return количество
     */
    @Query("SELECT COUNT(p) FROM PostText p WHERE p.blocked = true AND p.user.active = true")
    long countByBlockedTrueAndUserActiveTrue();

    // ========== Методы для объявлений на модерации (Slice с ID) ==========

    /**
     * Находит ID объявлений на модерации.
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true ORDER BY p.createdAt DESC")
    Slice<Long> findModerationIds(Pageable pageable);

    /**
     * Поиск ID объявлений на модерации по заголовку.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true AND LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitle(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID объявлений на модерации по имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true AND LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByUserName(@Param("query") String query, Pageable pageable);

    /**
     * Поиск ID объявлений на модерации по заголовку ИЛИ имени пользователя.
     * @param query поисковый запрос
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.moderation = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.user.nameuse) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    Slice<Long> searchModerationIdsByTitleOrUserName(@Param("query") String query, Pageable pageable);

    /**
     * Загружает объявления по списку ID с предварительной загрузкой изображений и пользователя.
     * @param ids список ID объявлений
     * @return список объявлений
     */
    @Query("SELECT DISTINCT p FROM PostText p " +
            "LEFT JOIN FETCH p.images " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.id IN :ids " +
            "ORDER BY p.createdAt DESC")
    List<PostText> findByIdsWithImagesAndUser(@Param("ids") List<Long> ids);


    // ========== Методы для редактора (управление объявлениями) ==========

    /**
     * Находит ID всех объявлений пользователя для редактора.
     * @param user пользователь
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.user = :user ORDER BY p.createdAt DESC")
    Slice<Long> findProductIdsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Находит ID объявлений пользователя по категории.
     * @param user пользователь
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.user = :user AND p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findProductIdsByUserAndCategory(@Param("user") User user, @Param("category") String category, Pageable pageable);

    /**
     * Находит ID всех объявлений (для администратора/редактора).
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p ORDER BY p.createdAt DESC")
    Slice<Long> findAllProductIds(Pageable pageable);

    /**
     * Находит ID объявлений по категории (для администратора/редактора).
     * @param category категория
     * @param pageable параметры пагинации
     * @return срез ID объявлений
     */
    @Query("SELECT p.id FROM PostText p WHERE p.category = :category ORDER BY p.createdAt DESC")
    Slice<Long> findProductIdsByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM PostText p WHERE p.premium = true AND p.moderatedAt IS NOT NULL AND p.moderatedAt >= :cutoffDate AND p.blocked = false ORDER BY p.premiumPriority DESC")
    List<PostText> findPremiumActivePosts(@Param("cutoffDate") LocalDateTime cutoffDate);

}