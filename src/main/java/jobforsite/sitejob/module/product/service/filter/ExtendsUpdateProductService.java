package jobforsite.sitejob.module.product.service.filter;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.product.repository.PostTextRepository;
import jobforsite.sitejob.module.shared.dto.PostUpdateData;
import jobforsite.sitejob.module.shared.dto.product.PostUpdateRequestDTO;
import jobforsite.sitejob.module.shared.mapper.PostUpdateRequestMapper;
import jobforsite.sitejob.module.shared.services.ImageService;
import jobforsite.sitejob.module.users.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExtendsUpdateProductService {

    private final PostTextRepository postTextRepository;

    private final ImageService imageService;

    private final PostUpdateRequestMapper postUpdateRequestMapper;

    /**
     * Обновляет пост из DTO с обработкой изображений.
     */
    @Transactional
    public PostText updateExtendsPostFromDTO(Long postId, User currentUser, PostUpdateRequestDTO request, MultipartFile[] files, boolean isAdminRequest) throws IOException {
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
            // Проверка размера файлов (должна быть в imageService)
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
        if (post instanceof AutoPostText auto) {
            auto.setAutoType(data.getAutoType());
            auto.setConditionAuto(data.getConditionAuto());
        }
        if (post instanceof ConstructionsPostText constructions) {
            constructions.setConstructionsType(data.getConstructionsType());
        }
        if (post instanceof InteriorPostText interior) {
            interior.setInteriorType(data.getInteriorType());
            interior.setConditionInterior(data.getConditionInterior());
        }

        if (post instanceof ToolsPostText tools) {
            tools.setToolsType(data.getToolsType());
            tools.setConditionTools(data.getConditionTools());
        }
        if (post instanceof AppliancesPostText appliances) {
            appliances.setAppliancesType(data.getAppliancesType());
            appliances.setConditionAppliances(data.getConditionAppliances());
        }
        if (post instanceof ElectronicsPostText electronics) {
            electronics.setElectronicsType(data.getElectronicsType());
            electronics.setConditionElectronics(data.getConditionElectronics());
        }
        if (post instanceof ClothingPostText clothing) {
            clothing.setClothingType(data.getClothingType());
            clothing.setGenderClothing(data.getGenderClothing());
            clothing.setConditionClothing(data.getConditionClothing());
        }
        if (post instanceof ChildrenPrPostText childrenPr) {
            childrenPr.setGenderChildrenPr(data.getGenderChildrenPr());
            childrenPr.setConditionChildrenPr(data.getConditionChildrenPr());
        }
        if (post instanceof SportsPostText sports) {
            sports.setSportsKind(data.getSportsKind());
            sports.setSportsType(data.getSportsType());
            sports.setGenderSports(data.getGenderSports());
            sports.setConditionSports(data.getConditionSports());
        }
        if (post instanceof FoodPostText food) {
            food.setFoodKind(data.getFoodKind());
        }
        if (post instanceof HealthPostText health) {
            health.setHealthKind(data.getHealthKind());
            health.setHealthType(data.getHealthType());
            health.setGenderHealth(data.getGenderHealth());
        }
        if (post instanceof AnimalsPostText animals) {
            animals.setAnimalsKind(data.getAnimalsKind());
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

//        // Если НЕ админский запрос - сбрасываем на модерацию
//        if (!isAdminRequest) {
//            post.resetForModeration();
//        }

        // Админское редактирование — публикуем сразу
        if (isAdminRequest) {
            post.publishAfterAdminEdit();
        } else {
            post.resetForModeration();
        }

        // Сохраняем
        PostText savedPost = postTextRepository.save(post);

        // Обработка изображений
        //  Удаление отмеченных
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            imageService.markImagesForDeletion(request.getDeleteImageIds());
        }

        // Добавление новых
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

        // Физическое удаление ТОЛЬКО для админских запросов
        // Для пользователей - удаление после модерации
        if (isAdminRequest) {
            deleteMarkedImagesForPost(savedPost);
        }

        log.info("Обновлён пост: id={}, hashId={}, user={}, isAdmin={}",
                savedPost.getId(), savedPost.getHashId(), currentUser.getNameuse(), isAdminRequest);

        return savedPost;
    }



    /**
     * Устаревший метод для обратной совместимости.
     */
    @Deprecated
    @Transactional
    public PostText updateExtendsPostFromDTO(Long postId, User currentUser, PostUpdateRequestDTO request, MultipartFile[] files) throws IOException {
        // По умолчанию для пользователей (не админ)
        return updateExtendsPostFromDTO(postId, currentUser, request, files, false);
    }

    /**
     * Удаляет отмеченные изображения для поста.
     */
    private void deleteMarkedImagesForPost(PostText post) {
        try {
            imageService.deleteMarkedImagesForPost(post);
        } catch (Exception e) {
            log.error("Ошибка при удалении отмеченных изображений для postId={}", post.getId(), e);
        }
    }


}
