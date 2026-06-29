package jobforsite.sitejob.module.vacancy.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для передачи основной информации о вакансии.
 * Используется для отображения вакансии в публичных списках и на странице просмотра.
 * Не содержит полей, связанных с блокировкой и модерацией (кроме базовых флагов).
 */
@Data
public class VacancyDTO {

    /** Идентификатор вакансии в базе данных */
    private Long id;

    /** Хэш-идентификатор для безопасного доступа к вакансии через URL */
    private String hashId;

    /** Заголовок вакансии */
    private String title;

    /** Местоположение (город/регион) вакансии */
    private String location;

    /** Размер оплаты или зарплата */
    private Integer cash;

    /** Технический идентификатор категории */
    private String category;

    private String experience;

    private String workformat;

    private String workformatDisplayName;

    /** Отображаемое имя категории для пользователя */
    private String categoryDisplayName;

    private String experienceDisplayName;

    /** Путь к первому изображению вакансии (для превью в списках) */
    private String firstImagePath;

    /** Полный текст описания вакансии */
    private String fullText;

    /** Дата и время создания вакансии */
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления */
    private LocalDateTime updatedAt;

    /** Дата и время последней модерации */
    private LocalDateTime moderatedAt;

    /** Флаг блокировки вакансии (скрыта от публичного просмотра) */
    private boolean blocked;

    /** Дата и время блокировки вакансии */
    private LocalDateTime blockedAt;

    /** Флаг прохождения модерации (true - прошла модерацию) */
    private boolean moderation;

    /** Флаг архивации вакансии (скрыта, но не удалена) */
    private boolean archived;

    /** Информация об авторе вакансии */
    private UserDTO user;

    /** Количество просмотров вакансии */
    private int viewCount;

    /** Флаг платного объявления (поднятие в промо-карусель) */
    private boolean premium = false;

    /** Приоритет показа в промо-карусели (чем выше, тем выше) */
    private int premiumPriority = 0;


    private boolean donate = false;

    /**
     * DTO для передачи базовой информации об авторе вакансии.
     * Используется в публичных списках и на странице просмотра.
     */
    @Data
    public static class UserDTO {
        /** Идентификатор пользователя */
        private Long id;

        /** Отображаемое имя пользователя */
        private String nameuse;

        /** Логин пользователя */
        private String username;
    }

    // === Геттеры и сеттеры с явной реализацией ===

    public String getHashId() {
        return hashId;
    }

    public void setHashId(String hashId) {
        this.hashId = hashId;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}