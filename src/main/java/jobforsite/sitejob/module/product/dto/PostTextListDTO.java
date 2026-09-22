package jobforsite.sitejob.module.product.dto;


import lombok.Data;

import java.time.LocalDateTime;

/**DTO для мапинга обязательных полей пользователя и его обьявления*/
@Data
public class PostTextListDTO {

    private Long id;
    private String hashId;
    private String title;
    private Integer price;
    private String category;
    private String categoryDisplayName;
    private String location;
    private String firstImagePath;
    private LocalDateTime moderatedAt;
    private boolean premium;
    private boolean donate;
    private boolean blocked;

    // Информация о пользователе
    private String userHashId;
    private String nameUse;
    private String userRegion;
    private String userRegionName;
    private String userCity;
    private String userCityDisplay;
    private boolean userOnline;

}
