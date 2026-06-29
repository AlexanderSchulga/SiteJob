package jobforsite.sitejob.module.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO для заблокированных резюме.
 * Используется при модерации и отображении заблокированных объявлений.
 */
@Data
public class ResumeBlockedDTO {

    private Long id;
    private String hashId;
    private String title;
    private Integer cash;
    private String category;
    private String experience;

    private String workformat;
    private String categoryDisplayName;
    private String experienDisplayName;

    private String workformatDisplayName;
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

    /**
     * Внутренний DTO для пользователя, связанного с резюме.
     */
    @Data
    public static class UserDTO {
        private Long id;
        private String username;
        private String nameuse;
        private String phonePost;
        private String region;
        private String city;
        private String regionDisplayName;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getNameuse() { return nameuse; }
        public void setNameuse(String nameuse) { this.nameuse = nameuse; }

        public String getPhonePost() { return phonePost; }
        public void setPhonePost(String phonePost) { this.phonePost = phonePost; }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getRegionDisplayName() { return regionDisplayName; }
        public void setRegionDisplayName(String regionDisplayName) { this.regionDisplayName = regionDisplayName; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getHashId() { return hashId; }
    public void setHashId(String hashId) { this.hashId = hashId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getCash() { return cash; }
    public void setCash(Integer cash) { this.cash = cash; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getExperienDisplayName() { return experienDisplayName; }
    public void setExperienDisplayName(String experienDisplayName) { this.experienDisplayName = experienDisplayName; }

    public String getCategoryDisplayName() { return categoryDisplayName; }
    public void setCategoryDisplayName(String categoryDisplayName) { this.categoryDisplayName = categoryDisplayName; }

    public String getFirstImagePath() { return firstImagePath; }
    public void setFirstImagePath(String firstImagePath) { this.firstImagePath = firstImagePath; }

    public String getFullText() { return fullText; }
    public void setFullText(String fullText) { this.fullText = fullText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(LocalDateTime moderatedAt) { this.moderatedAt = moderatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public LocalDateTime getBlockedAt() { return blockedAt; }
    public void setBlockedAt(LocalDateTime blockedAt) { this.blockedAt = blockedAt; }

    public boolean isModeration() { return moderation; }
    public void setModeration(boolean moderation) { this.moderation = moderation; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }


    public String getWorkformat() {
        return workformat;
    }

    public void setWorkformat(String workformat) {
        this.workformat = workformat;
    }

    public String getWorkformatDisplayName() {
        return workformatDisplayName;
    }

    public void setWorkformatDisplayName(String workformatDisplayName) {
        this.workformatDisplayName = workformatDisplayName;
    }



}