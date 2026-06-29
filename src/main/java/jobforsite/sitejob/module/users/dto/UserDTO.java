package jobforsite.sitejob.module.users.dto;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для передачи данных пользователя между слоями приложения.
 * <p>
 * Используется в административном интерфейсе для отображения списка пользователей
 * с дополнительной информацией о блокировках, черном списке и правах на удаление.
 * </p>
 * <p>
 * Основные группы полей:
 * <ul>
 *     <li><b>Основная информация</b> — id, username, nameuse, email, phone, phonePost, role, region, descript</li>
 *     <li><b>Статусные флаги</b> — active, inBlacklist, deletable</li>
 *     <li><b>Информация о блокировке</b> — blockReason, blockedAt, blockUntil, blockCount, blockDuration</li>
 *     <li><b>Служебные данные</b> — registrationDate, hashId</li>
 * </ul>
 * </p>
 */
public class UserDTO {

    // ==================== Основные поля пользователя ====================

    /** Уникальный идентификатор пользователя */
    private Long id;

    /** Логин пользователя */
    private String username;

    /** Отображаемое имя пользователя */
    private String nameuse;

    /** Email пользователя */
    private String email;

    /** Контактный телефон */
    private String phone;

    /** Телефон для объявлений (отдельное поле для публикаций) */
    private String phonePost;

    /** Роль пользователя (ROLE_USER, ROLE_ADMIN, ROLE_SUPER) */
    private String role;

    /** Активен ли пользователь (не заблокирован) */
    private boolean active;

    /** Регион пользователя */
    private String region;

    /** Описание профиля пользователя */
    private String descript;

    // ==================== Служебные поля ====================

    /** Хеш-идентификатор для безопасных URL */
    private String hashId;

    /** Дата регистрации пользователя */
    private LocalDateTime registrationDate;

    // ==================== Поля для административного интерфейса ====================

    /** Флаг: находится ли пользователь в черном списке */
    private boolean inBlacklist;

    /** Флаг: можно ли удалять пользователя (нельзя удалить суперадмина или самого себя) */
    private boolean deletable;

    // ==================== Информация о блокировке ====================

    /** Причина текущей/последней блокировки */
    private String blockReason;

    /** Дата и время блокировки */
    private LocalDateTime blockedAt;

    /** Дата и время окончания блокировки (null при разблокировке) */
    private LocalDateTime blockUntil;

    /** Количество блокировок пользователя (история) */
    private int blockCount;

    /** Текстовое представление длительности блокировки (например, "5 дней (Блокировка №2)") */
    private String blockDuration;

    /**
     * Конструктор со всеми полями DTO.
     * <p>
     * Создает полный объект UserDTO для передачи в представление.
     * </p>
     *
     * @param id               уникальный идентификатор
     * @param username         логин
     * @param nameuse          отображаемое имя
     * @param email            email
     * @param phone            контактный телефон
     * @param phonePost        телефон для объявлений
     * @param role             роль пользователя
     * @param active           активен ли пользователь
     * @param registrationDate дата регистрации
     * @param inBlacklist      в черном списке?
     * @param deletable        можно ли удалить?
     * @param blockReason      причина блокировки
     * @param blockedAt        дата блокировки
     * @param region           регион
     * @param hashId           хеш-идентификатор
     * @param blockCount       счетчик блокировок
     * @param blockUntil       дата окончания блокировки
     * @param blockDuration    текстовое описание длительности блокировки
     * @param descript         описание профиля
     */
    public UserDTO(Long id, String username, String nameuse, String email,
                   String phone, String phonePost, String role,
                   boolean active, LocalDateTime registrationDate,
                   boolean inBlacklist, boolean deletable,
                   String blockReason, LocalDateTime blockedAt,
                   String region, String hashId,
                   int blockCount, LocalDateTime blockUntil, String blockDuration, String descript) {
        this.id = id;
        this.username = username;
        this.nameuse = nameuse;
        this.email = email;
        this.phone = phone;
        this.phonePost = phonePost;
        this.role = role;
        this.active = active;
        this.registrationDate = registrationDate;
        this.inBlacklist = inBlacklist;
        this.deletable = deletable;
        this.blockReason = blockReason;
        this.blockedAt = blockedAt;
        this.region = region;
        this.hashId = hashId;
        this.blockCount = blockCount;
        this.blockUntil = blockUntil;
        this.blockDuration = blockDuration;
        this.descript = descript;
    }

    // ==================== Геттеры ====================

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNameuse() {
        return nameuse;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPhonePost() {
        return phonePost;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public boolean isInBlacklist() {
        return inBlacklist;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public String getRegion() {
        return region;
    }

    public String getHashId() {
        return hashId;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public LocalDateTime getBlockUntil() {
        return blockUntil;
    }

    public String getBlockDuration() {
        return blockDuration;
    }

    public String getDescript() {
        return descript;
    }

    // ==================== Сеттеры ====================

    public void setInBlacklist(boolean inBlacklist) {
        this.inBlacklist = inBlacklist;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public void setBlockUntil(LocalDateTime blockUntil) {
        this.blockUntil = blockUntil;
    }

    public void setBlockDuration(String blockDuration) {
        this.blockDuration = blockDuration;
    }

    public void setDescript(String descript) {
        this.descript = descript;
    }
}