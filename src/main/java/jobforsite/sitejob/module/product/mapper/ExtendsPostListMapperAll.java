package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Маппер для преобразования постов категории  в DTO.
 * <p>
 * Преобразует сущность  в {@link PostTextListDTO},
 * добавляя отображаемые названия категорий, регионов и городов.
 * </p>
 * <p>
 * Используется для отображения списка постов категории".
 * </p>
 *
 * @see PostTextListDTO
 * @see RegionsConfig
 */

@Component
@RequiredArgsConstructor
public class ExtendsPostListMapperAll {

    @Component
    @RequiredArgsConstructor
    public static class AutoPostListMapper implements PostTextListMapper<AutoPostText> {
        private final RegionsConfig regionsConfig;

        @Override
        public PostTextListDTO toListDTO(AutoPostText post) {
            PostTextListDTO dto = new PostTextListDTO();

            dto.setId(post.getId());
            dto.setHashId(post.getHashId());
            dto.setTitle(post.getTitle());
            dto.setPrice(post.getPrice());
            dto.setCategory(post.getCategory());
            dto.setCategoryDisplayName("Автозапчасти и автотовары");
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


    @Component
    @RequiredArgsConstructor
    public static class ConstructionsPostListMapper implements PostTextListMapper<ConstructionsPostText> {
        private final RegionsConfig regionsConfig;

        @Override
        public PostTextListDTO toListDTO(ConstructionsPostText post) {
            PostTextListDTO dto = new PostTextListDTO();

            dto.setId(post.getId());
            dto.setHashId(post.getHashId());
            dto.setTitle(post.getTitle());
            dto.setPrice(post.getPrice());
            dto.setCategory(post.getCategory());
            dto.setCategoryDisplayName("Строительные материалы");
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


    @Component
    @RequiredArgsConstructor
    public static class InteriorPostListMapper implements PostTextListMapper<InteriorPostText> {
        private final RegionsConfig regionsConfig;

        @Override
        public PostTextListDTO toListDTO(InteriorPostText post) {
            PostTextListDTO dto = new PostTextListDTO();

            dto.setId(post.getId());
            dto.setHashId(post.getHashId());
            dto.setTitle(post.getTitle());
            dto.setPrice(post.getPrice());
            dto.setCategory(post.getCategory());
            dto.setCategoryDisplayName("Дом и интерьер");
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



    @Component
    @RequiredArgsConstructor
    public static class ToolsPostListMapper implements PostTextListMapper<ToolsPostText> {
        private final RegionsConfig regionsConfig;

        @Override
        public PostTextListDTO toListDTO(ToolsPostText post) {  // ← принимает ОДИН объект
            PostTextListDTO dto = new PostTextListDTO();

            dto.setId(post.getId());
            dto.setHashId(post.getHashId());
            dto.setTitle(post.getTitle());
            dto.setPrice(post.getPrice());
            dto.setCategory(post.getCategory());
            dto.setCategoryDisplayName("Инструменты");
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

        @Component
        @RequiredArgsConstructor
        public static class AppliancesPostListMapper  implements PostTextListMapper<AppliancesPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(AppliancesPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Бытовая техника");
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


        @Component
        @RequiredArgsConstructor
        public static class ElectronicsPostListMapper implements PostTextListMapper<ElectronicsPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(ElectronicsPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Электроника и гаджеты");
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

        @Component
        @RequiredArgsConstructor
        public static class ClothingPostListMapper implements PostTextListMapper<ClothingPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(ClothingPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Одежда и обувь");
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


        @Component
        @RequiredArgsConstructor
        public static class ChildrenPrPostListMapper implements PostTextListMapper<ChildrenPrPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(ChildrenPrPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Детские товары");
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

        @Component
        @RequiredArgsConstructor
        public static class SportsPostListMapper implements PostTextListMapper<SportsPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(SportsPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Товары для спорта");
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


        @Component
        @RequiredArgsConstructor
        public static class FoodPostListMapper implements PostTextListMapper<FoodPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(FoodPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Продукты питания");
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

        @Component
        @RequiredArgsConstructor
        public static class HealthPostListMapper implements PostTextListMapper<HealthPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(HealthPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Здоровье и красота");
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

        @Component
        @RequiredArgsConstructor
        public static class AnimalsPostListMapper implements PostTextListMapper<AnimalsPostText> {
            private final RegionsConfig regionsConfig;

            @Override
            public PostTextListDTO toListDTO(AnimalsPostText post) {
                PostTextListDTO dto = new PostTextListDTO();

                dto.setId(post.getId());
                dto.setHashId(post.getHashId());
                dto.setTitle(post.getTitle());
                dto.setPrice(post.getPrice());
                dto.setCategory(post.getCategory());
                dto.setCategoryDisplayName("Флора и фауна");
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
    }

