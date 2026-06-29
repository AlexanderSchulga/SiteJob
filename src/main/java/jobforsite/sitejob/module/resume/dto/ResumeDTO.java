package jobforsite.sitejob.module.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO для резюме.
 * Используется при отображении и обработке резюме на основном сайте.
 */
@Data
public class ResumeDTO {

    private Long id;
    private String hashId;
    private String title;
    private Integer cash;
    private String category;

    private String experience;

    private String workformat;

    private String workformatDisplayName;

    private String categoryDisplayName;

    private String experienceDisplayName;
    private String firstImagePath;
    private String fullText;
    private LocalDateTime createdAt;

    private LocalDateTime moderatedAt;
    private LocalDateTime updatedAt;
    private boolean blocked;
    private LocalDateTime blockedAt;
    private boolean moderation;
    private boolean archived;
    private UserDTO user;
    private int viewCount;

    /**
     * Внутренний DTO для пользователя, связанного с резюме.
     */
    @Data
    public static class UserDTO {
        private Long id;
        private String nameuse;
        private String username;
    }

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
}