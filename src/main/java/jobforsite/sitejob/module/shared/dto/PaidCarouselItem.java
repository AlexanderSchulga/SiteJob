package jobforsite.sitejob.module.shared.dto;

import lombok.Data;

@Data
public class PaidCarouselItem {
    private String type;
    private String hashId;
    private String title;
    private String city;
    private String image;
    private String link;
    private int priority;
    private boolean isPlaceholder = false;
    private String alt;
}