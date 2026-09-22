package jobforsite.sitejob.module.product.service;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.*;
import jobforsite.sitejob.module.product.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


/**
 * Определяет конкретный тип поста по его hashId.
 * Последовательно перебирает все дочерние репозитории категорий
 * (недвижимость, транспорт, спорт, стройматериалы и т.д.) и возвращает
 * найденную сущность, либо базовый PostText, либо null.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostTypeResolver {

    private final PostRepo postRepo;

    // Все репозитории
    private final SportsPostRepository sportsPostRepository;
    private final ConstructionsPostRepository constructionsPostRepository;
    private final AutoPostRepository autoPostRepository;
    private final InteriorPostRepository interiorPostRepository;
    private final ToolsPostRepository toolsPostRepository;
    private final AppliancesPostRepository appliancesPostRepository;
    private final ElectronicsPostRepository electronicsPostRepository;
    private final ClothingPostRepository clothingPostRepository;
    private final ChildrenPrPostRepository childrenPrPostRepository;
    private final FoodPostRepository foodPostRepository;
    private final HealthPostRepository healthPostRepository;
    private final AnimalsPostRepository animalsPostRepository;

    private final VehiclePostRepository vehiclePostRepository;

    private final RealtyPostRepository realtyPostRepository;

    public PostText loadPostByHashId(String hashId) {
        // Проверяем существование
        if (!postRepo.existsByHashId(hashId)) {
            return null;
        }

        // По очереди проверяем все дочерние таблицы
        PostText post = checkAllSubcategories(hashId);

        if (post != null) {
            return post;
        }

        // Если не нашли - загружаем обычный PostText
        return postRepo.findByHashId(hashId).orElse(null);
    }

    private PostText checkAllSubcategories(String hashId) {

        // Недвижимость
        Optional<RealEstatePostText> realtyPost = realtyPostRepository.findByHashId(hashId);
        if (realtyPost.isPresent()) {
            log.debug("Loaded SportsPostText by hashId: {}", hashId);
            return realtyPost.get();
        }

        // Автотранспорт
        Optional<MotorVehiclesPostText> motoVihPost = vehiclePostRepository.findByHashId(hashId);
        if (motoVihPost.isPresent()) {
            log.debug("Loaded SportsPostText by hashId: {}", hashId);
            return motoVihPost.get();
        }

        // Sports
        Optional<SportsPostText> sportsPost = sportsPostRepository.findByHashId(hashId);
        if (sportsPost.isPresent()) {
            log.debug("Loaded SportsPostText by hashId: {}", hashId);
            return sportsPost.get();
        }

        // Constructions
        Optional<ConstructionsPostText> constructionsPost = constructionsPostRepository.findByHashId(hashId);
        if (constructionsPost.isPresent()) {
            log.debug("Loaded ConstructionsPostText by hashId: {}", hashId);
            return constructionsPost.get();
        }

        // Auto
        Optional<AutoPostText> autoPost = autoPostRepository.findByHashId(hashId);
        if (autoPost.isPresent()) {
            log.debug("Loaded AutoPostText by hashId: {}", hashId);
            return autoPost.get();
        }

        // Interior
        Optional<InteriorPostText> interiorPost = interiorPostRepository.findByHashId(hashId);
        if (interiorPost.isPresent()) {
            log.debug("Loaded InteriorPostText by hashId: {}", hashId);
            return interiorPost.get();
        }

        // Tools
        Optional<ToolsPostText> toolsPost = toolsPostRepository.findByHashId(hashId);
        if (toolsPost.isPresent()) {
            log.debug("Loaded ToolsPostText by hashId: {}", hashId);
            return toolsPost.get();
        }

        // Appliances
        Optional<AppliancesPostText> appliancesPost = appliancesPostRepository.findByHashId(hashId);
        if (appliancesPost.isPresent()) {
            log.debug("Loaded AppliancesPostText by hashId: {}", hashId);
            return appliancesPost.get();
        }

        // Electronics
        Optional<ElectronicsPostText> electronicsPost = electronicsPostRepository.findByHashId(hashId);
        if (electronicsPost.isPresent()) {
            log.debug("Loaded ElectronicsPostText by hashId: {}", hashId);
            return electronicsPost.get();
        }

        // Clothing
        Optional<ClothingPostText> clothingPost = clothingPostRepository.findByHashId(hashId);
        if (clothingPost.isPresent()) {
            log.debug("Loaded ClothingPostText by hashId: {}", hashId);
            return clothingPost.get();
        }

        // Children
        Optional<ChildrenPrPostText> childrenPost = childrenPrPostRepository.findByHashId(hashId);
        if (childrenPost.isPresent()) {
            log.debug("Loaded ChildrenPrPostText by hashId: {}", hashId);
            return childrenPost.get();
        }

        // Food
        Optional<FoodPostText> foodPost = foodPostRepository.findByHashId(hashId);
        if (foodPost.isPresent()) {
            log.debug("Loaded FoodPostText by hashId: {}", hashId);
            return foodPost.get();
        }

        // Health
        Optional<HealthPostText> healthPost = healthPostRepository.findByHashId(hashId);
        if (healthPost.isPresent()) {
            log.debug("Loaded HealthPostText by hashId: {}", hashId);
            return healthPost.get();
        }

        // Animals
        Optional<AnimalsPostText> animalsPost = animalsPostRepository.findByHashId(hashId);
        if (animalsPost.isPresent()) {
            log.debug("Loaded AnimalsPostText by hashId: {}", hashId);
            return animalsPost.get();
        }

        return null;
    }
}