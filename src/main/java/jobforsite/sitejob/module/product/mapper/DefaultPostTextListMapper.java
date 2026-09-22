package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * Маппер для преобразования постов (товаров) в DTO.
 * <p>
 * Преобразует сущность {@link PostText} в {@link PostTextListDTO},
 * добавляя отображаемые названия категорий, регионов и городов.
 * </p>
 * <p>
 * Используется для дефолтных товаров без подкатегорий.
 * </p>
 *
 * @see PostTextListDTO
 * @see PostText
 * @see RegionsConfig
 * @see PostTextCategoryConfig
 */

@Component
@RequiredArgsConstructor
public class DefaultPostTextListMapper implements PostTextListMapper<PostText>{

    private final RegionsConfig regionsConfig;
    private final PostTextCategoryConfig categoryConfig;


    @Override
    public PostTextListDTO toListDTO(PostText post) {
        PostTextListDTO dto = new PostTextListDTO();
        dto.setId(post.getId());
        dto.setHashId(post.getHashId());
        dto.setTitle(post.getTitle());
        dto.setPrice(post.getPrice());
        dto.setCategory(post.getCategory());
        dto.setCategoryDisplayName(categoryConfig.getCategoryDisplayName(post.getCategory()));
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

