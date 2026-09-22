package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.PostTextDetailDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.users.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования поста в детальное DTO.
 * <p>
 * Преобразует сущность {@link PostText} в {@link PostTextDetailDTO}.
 * Обрабатывает все типы постов: недвижимость, автотранспорт,
 * автозапчасти, строительные материалы, дом и интерьер и т.д.
 * </p>
 * <p>
 * Добавляет отображаемые названия для всех специфических полей,
 * а также информацию о пользователе (рейтинг, статус онлайн).
 * </p>
 *
 * @see PostTextDetailDTO
 * @see PostText
 * @see RegionsConfig
 * @see PostTextCategoryConfig
 */

@Component
@RequiredArgsConstructor
public class PostDetailMapper {

    private final PostTextCategoryConfig categoryConfig;
    private final RegionsConfig regionsConfig;
    private final RatingRepository ratingRepository;

    public PostTextDetailDTO toDetailDTO(PostText post) {
        if (post == null) {
            return null;
        }

        PostTextDetailDTO dto = new PostTextDetailDTO();

        // ===== ОБЩИЕ ПОЛЯ =====
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName(categoryConfig.getCategoryDisplayName(post.getCategory()));
        dto.setLocation(post.getLocation());
        dto.setFullText(post.getFull_text());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setModeratedAt(post.getModeratedAt());
        dto.setViewCount(post.getViewCount());
        dto.setBlocked(post.isBlocked());
        dto.setModeration(post.isModeration());
        dto.setArchived(post.isArchived());
        dto.setPremium(post.isPremium());
        dto.setPremiumPriority(post.getPremiumPriority());
        dto.setDonate(post.isDonate());

        // ===== ИЗОБРАЖЕНИЯ =====
        if (post.getImages() != null) {
            List<String> imagePaths = post.getImages().stream()
                    .filter(img -> img.getImagePath() != null && !img.getImagePath().isEmpty())
                    .map(img -> img.getImagePath())
                    .collect(Collectors.toList());
            dto.setImagePaths(imagePaths);
        }

        // ===== ПОЛЬЗОВАТЕЛЬ =====
        if (post.getUser() != null) {
            PostTextDetailDTO.UserDetailDTO userDto = new PostTextDetailDTO.UserDetailDTO();
            userDto.setId(post.getUser().getId());
            userDto.setHashId(post.getUser().getHashId());
            userDto.setNameuse(post.getUser().getNameuse());
            userDto.setPhonePost(post.getUser().getPhonePost());
            userDto.setRegion(post.getUser().getRegion());
            userDto.setRegionDisplayName(regionsConfig.getRegionDisplayName(post.getUser().getRegion()));
            userDto.setCity(post.getUser().getCity());
            userDto.setOnline(post.getUser().isOnline());

            Double avgRating = ratingRepository.findAverageRatingBySellerId(post.getUser().getId());
            Long countRating = ratingRepository.countRatingsBySellerId(post.getUser().getId());
            userDto.setAverageRating(avgRating != null ? avgRating : 0.0);
            userDto.setRatingCount(countRating != null ? countRating : 0L);

            dto.setUser(userDto);
        }

        // ===== СПЕЦИФИЧНЫЕ ПОЛЯ =====
        if (post instanceof RealEstatePostText realty) {
            dto.setPostType("realty");
            dto.setPropertyType(realty.getPropertyType());
            dto.setPropertyTypeDisplayName(categoryConfig.getPropertyTypeDisplayName(realty.getPropertyType()));
            dto.setRoomsCount(realty.getRoomsCount());
            dto.setFloorsCount(realty.getFloorsCount());
            dto.setFloorsCountHouse(realty.getFloorsCountHouse());
            dto.setHasElevator(realty.getHasElevator());
            dto.setBuildYear(realty.getBuildYear());
            dto.setFloorsApartment(realty.getFloorsApartment());
            dto.setHouseArea(realty.getHouseArea());
            dto.setLandArea(realty.getLandArea());
            dto.setDistanceToCity(realty.getDistanceToCity());

            dto.setMaterialUp(realty.getMaterialUp());
            dto.setMaterialUpDisplayName(categoryConfig.getMaterialUpDisplayName(realty.getMaterialUp()));
            dto.setRepairUp(realty.getRepairUp());
            dto.setRepairUpDisplayName(categoryConfig.getRepairUpDisplayName(realty.getRepairUp()));
            dto.setMaterialHouse(realty.getMaterialHouse());
            dto.setMaterialHouseDisplayName(categoryConfig.getMaterialHouseDisplayName(realty.getMaterialHouse()));
            dto.setRepairHouse(realty.getRepairHouse());
            dto.setRepairHouseDisplayName(categoryConfig.getRepairHouseDisplayName(realty.getRepairHouse()));
            dto.setDistanceToCityHouse(realty.getDistanceToCityHouse());
            dto.setLandAreaGarage(realty.getLandAreaGarage());
            dto.setLandAreaCommercial(realty.getLandAreaCommercial());

        } else if (post instanceof MotorVehiclesPostText vehicle) {
            dto.setPostType("vehicle");
            dto.setVehicleType(vehicle.getVehicleType());
            dto.setVehicleTypeDisplayName(categoryConfig.getVehicleTypeDisplayName(vehicle.getVehicleType()));
            dto.setVehicleOrigin(vehicle.getVehicleOrigin());
            dto.setVehicleOriginDisplayName(categoryConfig.getVehicleOriginDisplayName(vehicle.getVehicleOrigin()));
            dto.setVehicleYear(vehicle.getVehicleYear());
            dto.setMileage(vehicle.getMileage());
            dto.setNumberOfOwners(vehicle.getNumberOfOwners());

        } else if (post instanceof AutoPostText auto) {
            dto.setPostType("auto");
            dto.setAutoType(auto.getAutoType());
            dto.setAutoTypeDisplayName(categoryConfig.getAutoDisplayName(auto.getAutoType()));
            dto.setConditionAuto(auto.getConditionAuto());
            dto.setConditionAutoDisplayName(categoryConfig.getConditionAutoDisplayName(auto.getConditionAuto()));

        } else if (post instanceof ConstructionsPostText constructions) {
            dto.setPostType("constructions");
            dto.setConstructionsType(constructions.getConstructionsType());
            dto.setConstructionsTypeDisplayName(categoryConfig.getConstructionsDisplayName(constructions.getConstructionsType()));

        } else if (post instanceof InteriorPostText interior) {
            dto.setPostType("interior");
            dto.setInteriorType(interior.getInteriorType());
            dto.setInteriorTypeDisplayName(categoryConfig.getInteriorDisplayName(interior.getInteriorType()));
            dto.setConditionInterior(interior.getConditionInterior());
            dto.setConditionInteriorDisplayName(categoryConfig.getConditionInteriorDisplayName(interior.getConditionInterior()));

        } else if (post instanceof ToolsPostText tools) {
            dto.setPostType("tools");
            dto.setToolsType(tools.getToolsType());
            dto.setToolsTypeDisplayName(categoryConfig.getToolsDisplayName(tools.getToolsType()));
            dto.setConditionTools(tools.getConditionTools());
            dto.setConditionToolsDisplayName(categoryConfig.getConditionToolsDisplayName(tools.getConditionTools()));

        } else if (post instanceof AppliancesPostText appliances) {
            dto.setPostType("appliances");
            dto.setAppliancesType(appliances.getAppliancesType());
            dto.setAppliancesTypeDisplayName(categoryConfig.getAppliancesDisplayName(appliances.getAppliancesType()));
            dto.setConditionAppliances(appliances.getConditionAppliances());
            dto.setConditionAppliancesDisplayName(categoryConfig.getConditionAppliancesDisplayName(appliances.getConditionAppliances()));

        } else if (post instanceof ElectronicsPostText electronics) {
            dto.setPostType("electronics");
            dto.setElectronicsType(electronics.getElectronicsType());
            dto.setElectronicsTypeDisplayName(categoryConfig.getElectronicsDisplayName(electronics.getElectronicsType()));
            dto.setConditionElectronics(electronics.getConditionElectronics());
            dto.setConditionElectronicsDisplayName(categoryConfig.getConditionElectronicsDisplayName(electronics.getConditionElectronics()));

        } else if (post instanceof ClothingPostText clothing) {
            dto.setPostType("clothing");
            dto.setClothingType(clothing.getClothingType());
            dto.setClothingTypeDisplayName(categoryConfig.getClothingDisplayName(clothing.getClothingType()));
            dto.setGenderClothing(clothing.getGenderClothing());
            dto.setGenderClothingDisplayName(categoryConfig.getGenderClothingDisplayName(clothing.getGenderClothing()));
            dto.setConditionClothing(clothing.getConditionClothing());
            dto.setConditionClothingDisplayName(categoryConfig.getConditionClothingDisplayName(clothing.getConditionClothing()));

        } else if (post instanceof ChildrenPrPostText childrenPr) {
            dto.setPostType("childrenPr");
            dto.setGenderChildrenPr(childrenPr.getGenderChildrenPr());
            dto.setGenderChildrenPrDisplayName(categoryConfig.getGenderChildrenDisplayName(childrenPr.getGenderChildrenPr()));
            dto.setConditionChildrenPr(childrenPr.getConditionChildrenPr());
            dto.setConditionChildrenPrDisplayName(categoryConfig.getConditionChildrenDisplayName(childrenPr.getConditionChildrenPr()));

        } else if (post instanceof SportsPostText sports) {
            dto.setPostType("sports");
            dto.setSportsKind(sports.getSportsKind());
            dto.setSportsKindDisplayName(categoryConfig.getSportsKindDisplayName(sports.getSportsKind()));
            dto.setSportsType(sports.getSportsType());
            dto.setSportsTypeDisplayName(categoryConfig.getSportsTypeDisplayName(sports.getSportsType()));
            dto.setGenderSports(sports.getGenderSports());
            dto.setGenderSportsDisplayName(categoryConfig.getGenderSportsDisplayName(sports.getGenderSports()));
            dto.setConditionSports(sports.getConditionSports());
            dto.setConditionSportsDisplayName(categoryConfig.getConditionSportsDisplayName(sports.getConditionSports()));

        } else if (post instanceof FoodPostText food) {
            dto.setPostType("food");
            dto.setFoodKind(food.getFoodKind());
            dto.setFoodKindDisplayName(categoryConfig.getFoodKindDisplayName(food.getFoodKind()));

        } else if (post instanceof HealthPostText health) {
            dto.setPostType("health");
            dto.setHealthKind(health.getHealthKind());
            dto.setHealthKindDisplayName(categoryConfig.getHealthKindDisplayName(health.getHealthKind()));
            dto.setHealthType(health.getHealthType());
            dto.setHealthTypeDisplayName(categoryConfig.getHealthTypeDisplayName(health.getHealthType()));
            dto.setGenderHealth(health.getGenderHealth());
            dto.setGenderHealthDisplayName(categoryConfig.getGenderHealthDisplayName(health.getGenderHealth()));

        } else if (post instanceof AnimalsPostText animals) {
            dto.setPostType("animals");
            dto.setAnimalsKind(animals.getAnimalsKind());
            dto.setAnimalsKindDisplayName(categoryConfig.getAnimalsKindDisplayName(animals.getAnimalsKind()));

        } else {
            dto.setPostType("default");
        }

        return dto;
    }
}