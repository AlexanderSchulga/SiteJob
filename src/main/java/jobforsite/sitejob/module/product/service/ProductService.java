package jobforsite.sitejob.module.product.service;

import jobforsite.sitejob.module.product.dto.PostCreateRequestDTO;
import jobforsite.sitejob.module.product.dto.PostCreationData;
import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.mapper.PostCreateRequestMapper;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.MotorVehiclesPostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.RealEstatePostText;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.RealtyPostRepository;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.VehiclePostRepository;
import jobforsite.sitejob.module.product.repository.PostTextRepository;
import jobforsite.sitejob.module.shared.dto.PostUpdateData;
import jobforsite.sitejob.module.shared.dto.product.PostUpdateRequestDTO;
import jobforsite.sitejob.module.shared.mapper.PostUpdateRequestMapper;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.users.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для управления товарами (постами).
 * <p>
 * Предоставляет полный набор операций для работы с постами:
 * <ul>
 *   <li>Создание постов с изображениями</li>
 *   <li>Обновление постов с обработкой изображений</li>
 *   <li>Фильтрация и поиск постов по категориям</li>
 *   <li>Подсчет количества постов для пагинации</li>
 *   <li>Специфичная фильтрация для недвижимости и автотранспорта</li>
 * </ul>
 * </p>
 *
 * @author SiteJob Team
 * @version 1.0
 * @see PostText
 * @see ProductFilterDTO
 * @see PostFactory
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final PostTextRepository postTextRepository;
    private final RealtyPostRepository realtyPostRepository;
    private final VehiclePostRepository vehiclePostRepository;
    private final PostFactory postFactory;

    private final ImageService imageService;

    private final PostCreateRequestMapper requestMapper;

    private final PostUpdateRequestMapper postUpdateRequestMapper;

    // ============================================================
    // СОЗДАНИЕ ПОСТОВ
    // ============================================================

    /**
     * Создает пост из DTO с обработкой загруженных изображений.
     * <p>
     * Выполняет валидацию изображений (не более 5, минимум 1),
     * создает пост через фабрику, сохраняет в БД и обрабатывает изображения.
     * </p>
     *
     * @param user    пользователь, создающий пост
     * @param request DTO с данными поста
     * @param files   массив загруженных изображений
     * @return созданный пост
     * @throws IllegalArgumentException если изображения не выбраны или их больше 5
     * @throws IOException             если ошибка при сохранении изображений
     */
    @Transactional
    public PostText createPostFromDTO(User user, PostCreateRequestDTO request, MultipartFile[] files) throws IOException {
        // Валидация изображений (ручная)
        if (files == null || files.length == 0 || files[0].isEmpty()) {
            throw new IllegalArgumentException("Пожалуйста, выберите хотя бы одно изображение");
        }

        if (files.length > 5) {
            throw new IllegalArgumentException("Можно загрузить не более 5 изображений");
        }

        // Конвертируем DTO в PostCreationData
        PostCreationData data = requestMapper.toCreationData(request);

        // Создаём пост через фабрику
        PostText post = postFactory.createPost(data.getCategory(), user, data);
        PostText savedPost = postTextRepository.save(post);

        // Сохраняем изображения
        imageService.saveImages(files, savedPost, null, null, null);

        log.info("Создан новый пост: id={}, hashId={}, category={}, user={}",
                savedPost.getId(), savedPost.getHashId(), savedPost.getCategory(), user.getNameuse());

        return savedPost;
    }


    /**
     * Обновляет пост из DTO с обработкой изображений.
     * <p>
     * Выполняет проверку прав доступа, обновляет общие и специфичные поля,
     * обрабатывает добавление и удаление изображений.
     * </p>
     *
     * @param postId         ID поста для обновления
     * @param currentUser    текущий пользователь
     * @param request        DTO с данными для обновления
     * @param files          массив новых изображений
     * @param isAdminRequest флаг админского запроса (для физического удаления)
     * @return обновленный пост
     * @throws IllegalArgumentException если пост не найден или превышен лимит изображений
     * @throws SecurityException       если нет прав доступа
     * @throws IOException             если ошибка при сохранении изображений
     */
    @Transactional
    public PostText updatePostFromDTO(Long postId, User currentUser, PostUpdateRequestDTO request,
                                      MultipartFile[] files, boolean isAdminRequest) throws IOException {
        PostText post = postTextRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        // Проверка прав доступа
        if (!currentUser.getRole().equals("ROLE_SUPER") &&
                !currentUser.getRole().equals("ROLE_ADMIN") &&
                !currentUser.getId().equals(post.getUser().getId())) {
            throw new SecurityException("Доступ запрещен");
        }

        // Валидация файлов (если есть)
        if (files != null && files.length > 0) {
            if (files.length > 5) {
                throw new IllegalArgumentException("Можно загрузить не более 5 изображений");
            }
        }

        // Конвертируем DTO в PostUpdateData
        PostUpdateData data = postUpdateRequestMapper.toUpdateData(request);

        // Обновляем поля
        post.setTitle(data.getTitle());
        post.setPrice(data.getPrice());
        post.setFull_text(data.getFullText());
        post.setCategory(data.getCategory());
        post.setLocation(data.getLocation());

        // Обновление специфичных полей
        if (post instanceof RealEstatePostText realty) {
            realty.setPropertyType(data.getPropertyType());
            realty.setRoomsCount(data.getRoomsCount());
            realty.setFloorsApartment(data.getFloorsApartment());
            realty.setFloorsCount(data.getFloorsCount());
            realty.setFloorsCountHouse(data.getFloorsCountHouse());
            realty.setHasElevator(data.getHasElevator());
            realty.setHouseArea(data.getHouseArea());
            realty.setLandArea(data.getLandArea());
            realty.setDistanceToCity(data.getDistanceToCity());
            realty.setBuildYear(data.getBuildYear());
            realty.setMaterialUp(data.getMaterialUp());
            realty.setRepairUp(data.getRepairUp());
            realty.setMaterialHouse(data.getMaterialHouse());
            realty.setRepairHouse(data.getRepairHouse());
            realty.setDistanceToCityHouse(data.getDistanceToCityHouse());
            realty.setLandAreaGarage(data.getLandAreaGarage());
            realty.setLandAreaCommercial(data.getLandAreaCommercial());

            // Особый случай для дома - landArea может приходить как houseLandArea
            if ("house".equals(data.getPropertyType()) && request.getHouseLandArea() != null) {
                realty.setLandArea(request.getHouseLandArea());
            }
        }

        if (post instanceof MotorVehiclesPostText vehicle) {
            vehicle.setVehicleType(data.getVehicleType());
            vehicle.setVehicleOrigin(data.getVehicleOrigin());
            vehicle.setVehicleYear(data.getVehicleYear());
            vehicle.setMileage(data.getMileage());
            vehicle.setNumberOfOwners(data.getNumberOfOwners());
        }

        // Админские флаги
        if (data.getPremium() != null) {
            post.setPremium(data.getPremium());
        }
        if (data.getPremiumPriority() != null) {
            post.setPremiumPriority(data.getPremiumPriority());
        }
        if (data.getDonate() != null) {
            post.setDonate(data.getDonate());
        }

        // Если премиум - обновляем moderatedAt для показа в карусели
        if (data.getPremium() != null && data.getPremium()) {
            post.setModeratedAt(LocalDateTime.now());
        }

        // Сброс на модерацию
       // post.resetForModeration();

// Админское редактирование — публикуем сразу
        if (isAdminRequest) {
            post.publishAfterAdminEdit();
        } else {
            post.resetForModeration();
        }

        // Сохраняем
        PostText savedPost = postTextRepository.save(post);

        // Обработка изображений
        // 1. Удаление отмеченных
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            imageService.markImagesForDeletion(request.getDeleteImageIds());
        }

        // 2. Добавление новых
        MultipartFile[] nonEmptyFiles = files == null ? null :
                java.util.Arrays.stream(files)
                        .filter(f -> f != null && !f.isEmpty())
                        .toArray(MultipartFile[]::new);

        if (nonEmptyFiles != null && nonEmptyFiles.length > 0) {
            int currentCount = savedPost.getImages().size() -
                    (request.getDeleteImageIds() != null ? request.getDeleteImageIds().size() : 0);
            if (currentCount + nonEmptyFiles.length > 5) {
                throw new IllegalArgumentException("Нельзя загрузить более 5 изображений");
            }
            imageService.saveImages(nonEmptyFiles, savedPost, null, null, null);
        }

        // 3. Физическое удаление ТОЛЬКО для админских запросов
        if (isAdminRequest) {
            deleteMarkedImagesForPost(savedPost);
        }

        log.info("Обновлён пост: id={}, hashId={}, user={}, isAdmin={}",
                savedPost.getId(), savedPost.getHashId(), currentUser.getNameuse(), isAdminRequest);

        return savedPost;
    }

    /**
     * Устаревший метод для обратной совместимости.
     *
     * @param postId      ID поста
     * @param currentUser текущий пользователь
     * @param request     данные для обновления
     * @param files       изображения
     * @return обновленный пост
     * @deprecated Используйте {@link #updatePostFromDTO(Long, User, PostUpdateRequestDTO, MultipartFile[], boolean)}
     */
    @Deprecated
    @Transactional
    public PostText updatePostFromDTO(Long postId, User currentUser, PostUpdateRequestDTO request,
                                      MultipartFile[] files) throws IOException {
        return updatePostFromDTO(postId, currentUser, request, files);
    }

    /**
     * Удаляет отмеченные изображения для поста.
     *
     * @param post пост, для которого удаляются изображения
     */
    private void deleteMarkedImagesForPost(PostText post) {
        try {
            imageService.deleteMarkedImagesForPost(post);
        } catch (Exception e) {
            log.error("Ошибка при удалении отмеченных изображений для postId={}", post.getId(), e);
        }
    }

    // ============================================================
    // ОБЩИЙ ПОИСК И ФИЛЬТРАЦИЯ
    // ============================================================

    /**
     * Выполняет поиск постов с фильтрацией по региону и городу.
     *
     * @param category категория поста
     * @param search   поисковый запрос
     * @param region   регион
     * @param city     город
     * @param pageable параметры пагинации
     * @return список постов
     */
    public List<PostText> findPostsWithRegionAndCity(String category, String search,
                                                     String region, String city,
                                                     Pageable pageable) {
        Slice<Long> idsSlice = postTextRepository.findActivePostIdsWithRegionAndCity(
                category, search, region, city, pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<PostText> posts = postTextRepository.findPostsByIdsWithImages(idsSlice.getContent());

        Map<Long, PostText> postMap = posts.stream()
                .collect(Collectors.toMap(PostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов с фильтрацией по региону и городу.
     *
     * @param category категория поста
     * @param search   поисковый запрос
     * @param region   регион
     * @param city     город
     * @return количество постов
     */
    public long countPostsWithRegionAndCity(String category, String search,
                                            String region, String city) {
        return postTextRepository.countActivePostsWithRegionAndCity(category, search, region, city);
    }

    /**
     * Выполняет оптимизированный поиск постов (без региона).
     *
     * @param category категория поста
     * @param search   поисковый запрос
     * @param pageable параметры пагинации
     * @return список постов
     */
    public List<PostText> findPostsOptimized(String category, String search, Pageable pageable) {
        Slice<Long> idsSlice = postTextRepository.findActivePostIdsSlice(category, search, pageable);
        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<PostText> posts = postTextRepository.findPostsByIdsWithImages(idsSlice.getContent());

        Map<Long, PostText> postMap = posts.stream()
                .collect(Collectors.toMap(PostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ФИЛЬТРАЦИЯ НЕДВИЖИМОСТИ
    // ============================================================

    /**
     * Выполняет фильтрацию постов недвижимости с учетом региона, города и сортировки.
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable параметры пагинации
     * @return список постов недвижимости
     */
    public List<RealEstatePostText> filterRealtyWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = realtyPostRepository.findRealtyIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getPropertyType(),
                filter.getRoomsCount(),
                filter.getMinFloorsApartment(),
                filter.getMaxFloorsApartment(),
                filter.getFloorsCountMin(),
                filter.getFloorsCountMax(),
                filter.getFloorsCountHouseMin(),
                filter.getFloorsCountHouseMax(),
                filter.getHasElevator(),
                filter.getMinBuildYear(),
                filter.getMaxBuildYear(),
                filter.getMinHouseArea(),
                filter.getMaxHouseArea(),
                filter.getMinLandArea(),
                filter.getMaxLandArea(),
                filter.getMinDistanceToCity(),
                filter.getMaxDistanceToCity(),
                filter.getMaterialUp(),
                filter.getRepairUp(),
                filter.getMaterialHouse(),
                filter.getRepairHouse(),
                filter.getMinDistanceToCityHouse(),
                filter.getMaxDistanceToCityHouse(),
                filter.getMinLandAreaGarage(),
                filter.getMaxLandAreaGarage(),
                filter.getMinLandAreaCommercial(),
                filter.getMaxLandAreaCommercial(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<RealEstatePostText> posts = realtyPostRepository.findRealtyByIdsWithImages(idsSlice.getContent());

        Map<Long, RealEstatePostText> postMap = posts.stream()
                .collect(Collectors.toMap(RealEstatePostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов недвижимости с фильтрами.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов
     */
    public long countRealtyWithFilters(ProductFilterDTO filter) {
        return realtyPostRepository.countRealtyWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getPropertyType(),
                filter.getRoomsCount(),
                filter.getMinFloorsApartment(),
                filter.getMaxFloorsApartment(),
                filter.getFloorsCountMin(),
                filter.getFloorsCountMax(),
                filter.getFloorsCountHouseMin(),
                filter.getFloorsCountHouseMax(),
                filter.getHasElevator(),
                filter.getMinBuildYear(),
                filter.getMaxBuildYear(),
                filter.getMinHouseArea(),
                filter.getMaxHouseArea(),
                filter.getMinLandArea(),
                filter.getMaxLandArea(),
                filter.getMinDistanceToCity(),
                filter.getMaxDistanceToCity(),
                filter.getMaterialUp(),
                filter.getRepairUp(),
                filter.getMaterialHouse(),
                filter.getRepairHouse(),
                filter.getMinDistanceToCityHouse(),
                filter.getMaxDistanceToCityHouse(),
                filter.getMinLandAreaGarage(),
                filter.getMaxLandAreaGarage(),
                filter.getMinLandAreaCommercial(),
                filter.getMaxLandAreaCommercial()
        );
    }


    /**
     * Выполняет детальную фильтрацию квартир.
     *
     * @param roomsCount  количество комнат
     * @param minFloors   минимальный этаж
     * @param maxFloors   максимальный этаж
     * @param hasElevator наличие лифта
     * @param minYear     минимальный год постройки
     * @param maxYear     максимальный год постройки
     * @param pageable    параметры пагинации
     * @return список квартир
     */
    public List<RealEstatePostText> filterApartmentsOptimized(Integer roomsCount, Integer minFloors,
                                                              Integer maxFloors, Boolean hasElevator,
                                                              Integer minYear, Integer maxYear,
                                                              Pageable pageable) {
        Slice<Long> idsSlice = realtyPostRepository.filterApartmentsIdsSlice(
                roomsCount, minFloors, maxFloors, hasElevator, minYear, maxYear, pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<RealEstatePostText> posts = realtyPostRepository.findRealtyByIdsWithImages(idsSlice.getContent());

        Map<Long, RealEstatePostText> postMap = posts.stream()
                .collect(Collectors.toMap(RealEstatePostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ============================================================
    // ФИЛЬТРАЦИЯ АВТОТРАНСПОРТА
    // ============================================================

    /**
     * Выполняет фильтрацию постов автотранспорта с учетом региона, города и сортировки.
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable параметры пагинации
     * @return список постов автотранспорта
     */
    public List<MotorVehiclesPostText> filterVehiclesWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = vehiclePostRepository.findVehicleIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getVehicleType(),
                filter.getVehicleOrigin(),
                filter.getMinYear(),
                filter.getMaxYear(),
                filter.getMinMileage(),
                filter.getMaxMileage(),
                filter.getMinNumberOfOwners(),
                filter.getMaxNumberOfOwners(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<MotorVehiclesPostText> posts = vehiclePostRepository.findVehiclesByIdsWithImages(idsSlice.getContent());

        Map<Long, MotorVehiclesPostText> postMap = posts.stream()
                .collect(Collectors.toMap(MotorVehiclesPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов автотранспорта с фильтрами.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов
     */
    public long countVehiclesWithFilters(ProductFilterDTO filter) {
        return vehiclePostRepository.countVehicleWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getVehicleType(),
                filter.getVehicleOrigin(),
                filter.getMinYear(),
                filter.getMaxYear(),
                filter.getMinMileage(),
                filter.getMaxMileage(),
                filter.getMinNumberOfOwners(),
                filter.getMaxNumberOfOwners()
        );
    }


}