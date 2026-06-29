package jobforsite.sitejob.module.users.model;

import jakarta.persistence.*;
import jobforsite.sitejob.module.users.model.User;
import org.hibernate.annotations.CreationTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Сущность "Рейтинг/Оценка".
 * <p>
 * Хранит оценки, которые один пользователь (оценщик) ставит другому (продавцу).
 * Уникальность: один оценщик может оценить одного продавца только один раз.
 * </p>
 */
@Entity
@Table(name = "ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"rater_id", "seller_id"})
})
public class Rating {

    private static final Logger log = LoggerFactory.getLogger(Rating.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Пользователь, который ставит оценку (оценщик). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rater_id", nullable = false)
    private User rater;

    /** Пользователь-продавец, которому ставят оценку. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    /** Оценка (от 1 до 5). */
    @Column(nullable = false)
    private int score;

    /** Дата и время создания оценки (автозаполняемое). */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ===== Конструкторы =====

    /** Конструктор по умолчанию (требуется JPA). */
    public Rating() {
        log.info("Создан экземпляр Rating");
    }

    /**
     * Конструктор для создания оценки.
     *
     * @param rater  пользователь, который ставит оценку
     * @param seller пользователь-продавец, которому ставят оценку
     * @param score  оценка (1-5)
     */
    public Rating(User rater, User seller, int score) {
        this.rater = rater;
        this.seller = seller;
        this.score = score;
        log.info("Создана новая оценка: {}", score);
    }

    // ===== Геттеры и сеттеры =====

    public Long getId() { return id; }

    /**
     * Устанавливает ID (обычно используется только Hibernate).
     * @param id идентификатор
     */
    public void setId(Long id) {
        this.id = id;
    }

    public User getRater() { return rater; }

    public void setRater(User rater) {
        this.rater = rater;
    }

    public User getSeller() { return seller; }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public int getScore() { return score; }

    /**
     * Устанавливает оценку (от 1 до 5).
     * @param score новая оценка
     */
    public void setScore(int score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Вспомогательный метод для маскирования информации о продавце в логах.
     * Без id и username пользователя.
     *
     * @return маскированная информация
     */
    private String maskSellerInfo() {
        if (seller == null) {
            return "неизвестный продавец";
        }
        // Используем только хеш-код объекта вместо реальных данных пользователя
        return "продавец#" + Integer.toHexString(System.identityHashCode(seller));
    }
}