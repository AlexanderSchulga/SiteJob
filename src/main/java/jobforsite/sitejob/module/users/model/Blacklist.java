package jobforsite.sitejob.module.users.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.hibernate.annotations.CreationTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Сущность "Чёрный список".
 * <p>
 * Хранит заблокированные email, телефоны и постоплатные номера,
 * а также причину блокировки и кто заблокировал.
 * </p>
 */
@Entity
@Table(name = "blacklist")
public class Blacklist {

    private static final Logger log = LoggerFactory.getLogger(Blacklist.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Заблокированный email (уникальный). */
    @Column(unique = true)
    private String email;

    /** Заблокированный телефон (уникальный). */
    @Column(unique = true)
    private String phone;

    /** Заблокированный постоплатный телефон (уникальный). */
    @Column(unique = true)
    private String phonePost;

    /** Причина блокировки (обязательное поле). */
    @Column(nullable = false)
    private String reason;

    /** Дата создания записи (автозаполняемое, неизменяемое поле). */
    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    /** Администратор, который добавил запись в чёрный список. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_user_id")
    private User bannedBy;

    // ===== Конструкторы =====

    /** Конструктор по умолчанию (требуется JPA). */
    public Blacklist() {
        log.info("Создан экземпляр Blacklist");
    }

    /**
     * Конструктор для создания записи в чёрном списке.
     *
     * @param email     заблокированный email (может быть null)
     * @param phone     заблокированный телефон (может быть null)
     * @param phonePost заблокированный постоплатный телефон (может быть null)
     * @param reason    причина блокировки
     * @param bannedBy  администратор, который заблокировал
     */
    public Blacklist(String email, String phone, String phonePost, String reason, User bannedBy) {
        this.email = email;
        this.phone = phone;
        this.phonePost = phonePost;
        this.reason = reason;
        this.bannedBy = bannedBy;

        log.info("Добавлена запись в чёрный список.");
    }

    // ===== Геттеры и сеттеры =====

    public Long getId() { return id; }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() { return phone; }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhonePost() { return phonePost; }

    public void setPhonePost(String phonePost) {
        this.phonePost = phonePost;
    }

    public String getReason() { return reason; }

    public void setReason(String reason) {
        this.reason = reason;

    }

    public LocalDateTime getCreatedDate() { return createdDate; }

    public User getBannedBy() { return bannedBy; }

    public void setBannedBy(User bannedBy) {
        this.bannedBy = bannedBy;

    }

    /**
     * Вспомогательный метод для маскирования контактов в логах.
     * Пример: "user@example.com" -> "us****om"
     *
     * @param contact контакт (email или телефон)
     * @return замаскированный контакт или "null"
     */
    private String maskContact(String contact) {
        if (contact == null) return "null";
        if (contact.length() <= 4) return "***";
        return contact.substring(0, 2) + "****" + contact.substring(contact.length() - 2);
    }
}