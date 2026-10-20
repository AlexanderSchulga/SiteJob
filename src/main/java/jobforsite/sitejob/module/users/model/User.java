package jobforsite.sitejob.module.users.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.product.model.Comment;
import jobforsite.sitejob.module.product.model.CommentPersonal;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.CommentPersonalRes;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.service.model.CommentJob;
import jobforsite.sitejob.module.service.model.CommentPersonalJob;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.shared.model.Favorites;
import jobforsite.sitejob.module.shared.model.SupportComment;
import jobforsite.sitejob.module.vacancy.model.CommentPersonalVac;
import jobforsite.sitejob.module.vacancy.model.CommentVacancy;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Сущность пользователя системы.
 * Реализует {@link UserDetails} для интеграции со Spring Security.
 * <p>
 * Содержит личную информацию, настройки безопасности,
 * а также связи с постами, комментариями, вакансиями, резюме,
 * рейтингами и избранным.
 * </p>
 *
 * @author JobForSite Team
 * @version 1.0
 * @see UserDetails
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    private static final Logger log = LoggerFactory.getLogger(User.class);

    // ========== ПОЛЯ ИДЕНТИФИКАЦИИ ==========

    /** Уникальный идентификатор пользователя. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальное имя пользователя (логин). */
    @Column(unique = true, nullable = false)
    private String username;

    /** Отображаемое имя пользователя. */
    //@Column(nullable = false)  было до рефакторинга
    @Column(unique = true, nullable = false)
    private String nameuse;

    /** email пользователя. */
    @Column(unique = true, nullable = false)
    private String email;

    /** Уникальный номер телефона. */
    //@Column(unique = true, nullable = false)  было до рефакторинга
    @Column(unique = true)
    private String phone;

    /** Дополнительный номер телефона */
    @Column(name = "phone_post")
    private String phonePost;

    /** Хешированный пароль. */
    @Column(nullable = false)
    private String password;

    /** Регион проживания/работы. */
    @Column(name = "region", nullable = false)
    private String region = "other";

    /** Город. */
    @Column(name = "city")
    private String city = "other";

    /** Уникальный хеш-идентификатор для внешних ссылок. */
    @Column(name = "hash_id", unique = true, nullable = false)
    private String hashId;

    /** Описание профиля (биография, навыки и т.д.). */
    @Column(name = "descript", columnDefinition = "TEXT", nullable = true)
    private String descript;

    /**
     * Генерация уникального hashId перед сохранением в БД.
     * Вызывается автоматически.
     */
    @PrePersist
    public void generateHashId() {
        if (this.hashId == null) {
            this.hashId = UUID.randomUUID().toString().replace("-", "");
        }
    }

    // ========== ПОЛЯ БЕЗОПАСНОСТИ И СТАТУСА ==========

    /** Роль пользователя (например, ROLE_USER, ROLE_ADMIN). */
    @Column(nullable = false)
    private String role;

    /** Активен ли аккаунт (не заблокирован). */
    @Column(nullable = false)
    private boolean active = true;

    /** Помечен ли аккаунт как удалённый (мягкое удаление). */
    @Column(nullable = false)
    private boolean deleted = false;

    /** Дата и время регистрации (автоматически проставляется). */
    @CreationTimestamp
    @Column(name = "registration_date", updatable = false)
    private LocalDateTime registrationDate;

    /** Счётчик блокировок (для автоматической блокировки). */
    @Column(name = "block_count", nullable = false)
    private int blockCount = 0;

    /** Дата и время, до которого аккаунт заблокирован. */
    @Column(name = "block_until")
    private LocalDateTime blockUntil;

    /** Причина блокировки (текст). */
    @Column(name = "block_reason", length = 1000)
    private String blockReason;

    /** Дата и время блокировки. */
    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    /** Дата и время последнего визита. */
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    /** Флаг онлайн-статуса. */
    @Column(name = "is_online")
    private Boolean online = false;

    // ========== СВЯЗИ С ПОСТАМИ ==========

    /** Посты пользователя в обычной ленте. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostText> posts = new ArrayList<>();

    /** Посты пользователя в ленте услуг. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTextJob> postsjob = new ArrayList<>();

    /** Вакансии пользователя. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vacancy> vacancy = new ArrayList<>();

    /** Резюме пользователя. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resume = new ArrayList<>();

    // ========== СВЯЗИ С КОММЕНТАРИЯМИ (ОБЫЧНЫЕ) ==========

    /** Комментарии к обычным постам. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    /** Комментарии к постам услуг. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentJob> commentsjob = new ArrayList<>();

    /** Комментарии к вакансиям. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentVacancy> commentVacancies = new ArrayList<>();

    // ========== СВЯЗИ С КОММЕНТАРИЯМИ (ЛИЧНЫЕ) ==========

    /** Личные комментарии пользователя (к обычным постам). */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonal> commentsPersonal = new ArrayList<>();

    /** Личные комментарии пользователя (к постам услуг). */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalJob> commentsJobPersonal = new ArrayList<>();

    /** Личные комментарии пользователя (к вакансиям). */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalVac> commentVacancyPersonal = new ArrayList<>();

    /** Личные комментарии пользователя (к резюме). */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalRes> commentResumePersonal = new ArrayList<>();

    /** Личные комментарии, полученные пользователем (обычные посты). */
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonal> receivedCommentsPersonal = new ArrayList<>();

    /** Личные комментарии, полученные пользователем (посты услуг). */
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalJob> receivedCommentsJobPersonal = new ArrayList<>();

    /** Личные комментарии, полученные пользователем (вакансии). */
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalVac> receivedCommentVacancyPersonal = new ArrayList<>();

    /** Личные комментарии, полученные пользователем (резюме). */
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentPersonalRes> receivedCommentResumePersonal = new ArrayList<>();

    /** Рейтинги, полученные пользователем (как продавец). */
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> receivedRatings = new ArrayList<>();

    /** Рейтинги, поставленные пользователем (как оценщик). */
    @OneToMany(mappedBy = "rater", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> givenRatings = new ArrayList<>();

    /** Избранные записи пользователя. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorites> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "assignedSuper", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupportComment> supportComments;

    // ========== КОНСТРУКТОРЫ ==========

    /** Конструктор по умолчанию (для JPA). */
    public User() {
        // Пустая инициализация для Hibernate
    }

    /**
     * Конструктор для создания нового пользователя с основными данными.
     *
     * @param username   уникальное имя пользователя
     * @param nameuse    отображаемое имя
     * @param email      электронная почта
     * @param phone      основной телефон
     * @param phonePost  дополнительный телефон
     * @param password   хешированный пароль
     * @param region     регион
     * @param city       город
     * @param descript   описание профиля
     */
    public User(String username, String nameuse, String email, String phone,
                String phonePost, String password, String region, String city, String descript) {
        this.username = username;
        this.nameuse = nameuse;
        setEmail(email);
        this.phone = phone;
        this.phonePost = phonePost;
        this.password = password;
        this.region = region;
        this.city = city;
        this.role = "ROLE_USER";
        this.active = true;
        this.descript = descript;
    }

    // ========== МЕТОДЫ UserDetails ==========

    /**
     * Возвращает роли/ authorities пользователя.
     *
     * @return коллекция GrantedAuthority
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    /** Аккаунт не истекает. */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Аккаунт не заблокирован, если активен.
     *
     * @return true если аккаунт активен
     */
    @Override
    public boolean isAccountNonLocked() {
        return isActive();
    }

    /** Учётные данные не истекают. */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Пользователь включён, если активен и не удалён.
     *
     * @return true если аккаунт активен и не удалён
     */
    @Override
    public boolean isEnabled() {
        return isActive() && !isDeleted();
    }

    // ========== СПЕЦИАЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Возвращает ID пользователя для использования в качестве principal.
     *
     * @return ID пользователя
     */
    @Transient
    public Long getPrincipalId() {
        return this.id;
    }

    /**
     * Устанавливает email с приведением к нижнему регистру и проверкой.
     *
     * @param email email пользователя
     * @throws IllegalArgumentException если email null или пустой
     */
    public void setEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            this.email = email.trim().toLowerCase();
        } else {
            this.email = null;
        }
    }

    // ========== ГЕТТЕРЫ И СЕТТЕРЫ (ОСНОВНЫЕ) ==========

    public String getDescript() {
        return descript;
    }

    public void setDescript(String descript) {
        this.descript = descript;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNameuse() {
        return nameuse;
    }

    public void setNameuse(String nameuse) {
        this.nameuse = nameuse;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhonePost() {
        return phonePost;
    }

    public void setPhonePost(String phonePost) {
        this.phonePost = phonePost;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
        if (deleted) {
        }
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    // ========== ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ ПОСТОВ ==========

    public List<PostText> getPosts() {
        return posts;
    }

    public void setPosts(List<PostText> posts) {
        this.posts = posts;
    }

    public List<PostTextJob> getPostsjob() {
        return postsjob;
    }

    public void setPostsjob(List<PostTextJob> postsjob) {
        this.postsjob = postsjob;
    }

    public List<Vacancy> getVacancy() {
        return vacancy;
    }

    public void setVacancy(List<Vacancy> vacancy) {
        this.vacancy = vacancy;
    }

    public List<Resume> getResume() {
        return resume;
    }

    public void setResume(List<Resume> resume) {
        this.resume = resume;
    }

    // ========== ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ КОММЕНТАРИЕВ (ОБЫЧНЫХ) ==========

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<CommentJob> getCommentsjob() {
        return commentsjob;
    }

    public void setCommentsjob(List<CommentJob> commentsjob) {
        this.commentsjob = commentsjob;
    }

    public List<CommentVacancy> getCommentVacancies() {
        return commentVacancies;
    }

    public void setCommentVacancies(List<CommentVacancy> commentVacancies) {
        this.commentVacancies = commentVacancies;
    }

    // ========== ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ КОММЕНТАРИЕВ (ЛИЧНЫХ) ==========

    public List<CommentPersonal> getCommentsPersonal() {
        return commentsPersonal;
    }

    public void setCommentsPersonal(List<CommentPersonal> commentsPersonal) {
        this.commentsPersonal = commentsPersonal;
    }

    public List<CommentPersonalJob> getCommentsJobPersonal() {
        return commentsJobPersonal;
    }

    public void setCommentsJobPersonal(List<CommentPersonalJob> commentsJobPersonal) {
        this.commentsJobPersonal = commentsJobPersonal;
    }

    public List<CommentPersonalVac> getCommentVacancyPersonal() {
        return commentVacancyPersonal;
    }

    public void setCommentVacancyPersonal(List<CommentPersonalVac> commentVacancyPersonal) {
        this.commentVacancyPersonal = commentVacancyPersonal;
    }

    public List<CommentPersonalRes> getCommentResumePersonal() {
        return commentResumePersonal;
    }

    public void setCommentResumePersonal(List<CommentPersonalRes> commentResumePersonal) {
        this.commentResumePersonal = commentResumePersonal;
    }

    public List<CommentPersonal> getReceivedCommentsPersonal() {
        return receivedCommentsPersonal;
    }

    public void setReceivedCommentsPersonal(List<CommentPersonal> receivedCommentsPersonal) {
        this.receivedCommentsPersonal = receivedCommentsPersonal;
    }

    public List<CommentPersonalJob> getReceivedCommentsJobPersonal() {
        return receivedCommentsJobPersonal;
    }

    public void setReceivedCommentsJobPersonal(List<CommentPersonalJob> receivedCommentsJobPersonal) {
        this.receivedCommentsJobPersonal = receivedCommentsJobPersonal;
    }

    public List<CommentPersonalVac> getReceivedCommentVacancyPersonal() {
        return receivedCommentVacancyPersonal;
    }

    public void setReceivedCommentVacancyPersonal(List<CommentPersonalVac> receivedCommentVacancyPersonal) {
        this.receivedCommentVacancyPersonal = receivedCommentVacancyPersonal;
    }

    public List<CommentPersonalRes> getReceivedCommentResumePersonal() {
        return receivedCommentResumePersonal;
    }

    public void setReceivedCommentResumePersonal(List<CommentPersonalRes> receivedCommentResumePersonal) {
        this.receivedCommentResumePersonal = receivedCommentResumePersonal;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public List<Rating> getReceivedRatings() {
        return receivedRatings;
    }

    public void setReceivedRatings(List<Rating> receivedRatings) {
        this.receivedRatings = receivedRatings;
    }

    public List<Rating> getGivenRatings() {
        return givenRatings;
    }

    public void setGivenRatings(List<Rating> givenRatings) {
        this.givenRatings = givenRatings;
    }

    public List<Favorites> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<Favorites> favorites) {
        this.favorites = favorites;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    /**
     * Генерирует короткий уникальный хеш для пользователя.
     *
     * @return строка длиной 20 символов
     */
    public static String generateUserHash() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
        if (blockCount > 3) {
        }
    }

    public LocalDateTime getBlockUntil() {
        return blockUntil;
    }

    public void setBlockUntil(LocalDateTime blockUntil) {
        this.blockUntil = blockUntil;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online != null ? online : false;
    }

    public boolean isOnline() {
        return online != null ? online : false;
    }

    // ========== ПЕРЕОПРЕДЕЛЁННЫЕ МЕТОДЫ ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}