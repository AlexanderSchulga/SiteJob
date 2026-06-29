package jobforsite.sitejob.module.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Сущность, хранящая текстовые версии пользовательских соглашений и политики конфиденциальности.
 * <p>
 * Содержит две основные текстовые части: пользовательское соглашение (agree)
 * и политику конфиденциальности (confiden). Идентификатор записи задаётся вручную.
 * </p>
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/">Jakarta Persistence 3.1</a>
 */
@Entity
public class LicenseUser {

    @Id
    private Long Id; // Идентификатор записи лицензионного соглашения

    /**
     * Текст пользовательского соглашения (лицензии).
     * Хранится как TEXT для поддержки больших объёмов текста.
     */
    @Column(columnDefinition = "TEXT")
    private String agree;

    /**
     * Текст политики конфиденциальности.
     * Хранится как TEXT для поддержки больших объёмов текста.
     */
    @Column(columnDefinition = "TEXT")
    private String confiden;

    /**
     * Конструктор по умолчанию, необходимый для JPA.
     */
    public LicenseUser() {
    }

    /**
     * Конструктор для создания объекта с указанием соглашения и политики.
     *
     * @param agree    текст пользовательского соглашения
     * @param confiden текст политики конфиденциальности
     */
    public LicenseUser(String agree, String confiden) {
        this.agree = agree;
        this.confiden = confiden;
    }

    // Геттеры и сеттеры

    public Long getId() {
        return Id;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getAgree() {
        return agree;
    }

    public void setAgree(String agree) {
        this.agree = agree;
    }

    public String getConfiden() {
        return confiden;
    }

    public void setConfiden(String confiden) {
        this.confiden = confiden;
    }
}