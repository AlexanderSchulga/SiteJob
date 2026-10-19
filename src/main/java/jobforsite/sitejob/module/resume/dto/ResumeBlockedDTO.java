package jobforsite.sitejob.module.resume.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDate;
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


    private String placeofwork1;

    private String placeofwork2;

    private String skills;

    private String place1;

    private LocalDate place1Start;

    private LocalDate place1End;

    private String place2;

    private LocalDate place2Start;

    private LocalDate place2End;

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


    public String getPlaceofwork1() {
        return placeofwork1;
    }

    public void setPlaceofwork1(String placeofwork1) {
        this.placeofwork1 = placeofwork1;
    }

    public String getPlaceofwork2() {
        return placeofwork2;
    }

    public void setPlaceofwork2(String placeofwork2) {
        this.placeofwork2 = placeofwork2;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPlace1() {
        return place1;
    }

    public void setPlace1(String place1) {
        this.place1 = place1;
    }

    public LocalDate getPlace1Start() {
        return place1Start;
    }

    public void setPlace1Start(LocalDate place1Start) {
        this.place1Start = place1Start;
    }

    public LocalDate getPlace1End() {
        return place1End;
    }

    public void setPlace1End(LocalDate place1End) {
        this.place1End = place1End;
    }

    public String getPlace2() {
        return place2;
    }

    public void setPlace2(String place2) {
        this.place2 = place2;
    }

    public LocalDate getPlace2Start() {
        return place2Start;
    }

    public void setPlace2Start(LocalDate place2Start) {
        this.place2Start = place2Start;
    }

    public LocalDate getPlace2End() {
        return place2End;
    }

    public void setPlace2End(LocalDate place2End) {
        this.place2End = place2End;
    }

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