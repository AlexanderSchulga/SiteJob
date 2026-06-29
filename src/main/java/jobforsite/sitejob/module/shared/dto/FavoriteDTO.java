package jobforsite.sitejob.module.shared.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) для избранных элементов (Favorites).
 * Используется для передачи данных об избранных объявлениях,
 * вакансиях, резюме и услугах между слоями приложения.
 */
@Data
public class FavoriteDTO {

    // ================ ИДЕНТИФИКАТОРЫ ================

    /**
     * Уникальный идентификатор записи об избранном
     */
    private Long id;

    /**
     * Идентификатор избранного элемента (ID исходной сущности)
     */
    private Long favoriteId;

    /**
     * Тип избранного элемента: "product" (товар), "service" (услуга),
     * "vacancy" (вакансия), "resume" (резюме)
     */
    private String type; // "product", "service", "vacancy" , "resume"

    /**
     * Хэш-идентификатор для публичных ссылок
     */
    private String hashId;

    // ================ ОСНОВНАЯ ИНФОРМАЦИЯ ================

    /**
     * Заголовок/название избранного элемента
     */
    private String title;

    /**
     * Цена (в валюте площадки)
     */
    private Integer price;

    /**
     * Наличные/бонусы (альтернативная стоимость)
     */
    private Integer cash;

    /**
     * Категория (внутренний идентификатор)
     */
    private String category;

    /**
     * Отображаемое название категории для UI
     */
    private String categoryDisplayName;

    // ================ ОПЫТ РАБОТЫ (для вакансий/резюме) ================

    /**
     * Отображаемое название опыта работы
     */
    private String experienDisplayName;

    /**
     * Идентификатор опыта работы (внутренний код)
     */
    private String experience;

    /**
     * Идентификатор формат работы (внутренний код)
     */
    private String workformat;

    // ================ ЛОКАЦИЯ ================

    /**
     * Местоположение (текстовое представление)
     */
    private String location;

    /**
     * Путь к первому/основному изображению
     */
    private String firstImagePath;

    // ================ ИНФОРМАЦИЯ ОБ АВТОРЕ ================

    /**
     * Идентификатор автора объявления
     */
    private String authorId;

    /**
     * Имя автора
     */
    private String authorName;

    /**
     * Статус онлайн автора
     */
    private Boolean authorOnline;

    // ================ ДАТЫ ================

    /**
     * Дата добавления в избранное (отформатированная строка для UI)
     */
    private String addedAtFormatted;

    /**
     * Дата добавления в избранное (оригинальный timestamp)
     */
    private LocalDateTime addedAt;

    /**
     * Дата модерации (оригинальный timestamp)
     */
    private LocalDateTime ModeratedAt;

    /**
     * Дата модерации (отформатированная строка для UI)
     */
    private String ModeratedAtFormatted;

    // ================ РЕГИОН И ГОРОД ================

    /**
     * Код/идентификатор региона
     */
    private String region;

    /**
     * Код/идентификатор города
     */
    private String city;

    /**
     * Отображаемое название региона
     */
    private String regionDisplayName;

    /**
     * Отображаемое название города
     */
    private String cityDisplayName;


    /**
     * Флаг премиум объявления
     */
    private boolean premium;

    /**
     * Флаг донат объявления
     */
    private boolean donate;
}