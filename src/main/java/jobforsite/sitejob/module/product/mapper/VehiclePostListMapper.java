package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.MotorVehiclesPostText;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * Маппер для преобразования постов категории "Автотранспорт" в DTO.
 * <p>
 * Преобразует сущность {@link MotorVehiclesPostText} в {@link PostTextListDTO},
 * добавляя отображаемые названия категорий, регионов и городов.
 * </p>
 * <p>
 * Используется для отображения списка постов категории "Автотранспорт".
 * </p>
 *
 * @see PostTextListDTO
 * @see MotorVehiclesPostText
 * @see RegionsConfig
 */

@Component
@RequiredArgsConstructor
public class VehiclePostListMapper implements PostTextListMapper<MotorVehiclesPostText>{

    private final RegionsConfig regionsConfig;

    @Override
    public PostTextListDTO toListDTO(MotorVehiclesPostText post) {
        PostTextListDTO dto = new PostTextListDTO();

        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName("Автотранспорт"); // Или post.getCategoryDisplayName()
        dto.setLocation(post.getLocation());
        dto.setFirstImagePath(post.getFirstImagePath());
        dto.setModeratedAt(post.getModeratedAt());
        dto.setPremium(post.isPremium());
        dto.setDonate(post.isDonate());
        dto.setBlocked(post.isBlocked());

        if (post.getUser() != null) {
            dto.setUserHashId(post.getUser().getHashId());
            dto.setNameUse(post.getUser().getNameuse());
            dto.setUserRegion(post.getUser().getRegion());
            dto.setUserRegionName(regionsConfig.getRegionDisplayName(post.getUser().getRegion()));
            dto.setUserCity(post.getUser().getCity());
            dto.setUserCityDisplay(regionsConfig.getCityDisplayName(post.getUser().getRegion(), post.getUser().getCity()));
            dto.setUserOnline(post.getUser().isOnline());
        }

        return dto;
    }


}
