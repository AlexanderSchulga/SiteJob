package jobforsite.sitejob.module.shared.controller.sharedglobal;

import jakarta.servlet.http.HttpServletRequest;
import jobforsite.sitejob.module.shared.config.globalconfig.RegionsConfig;
import jobforsite.sitejob.module.users.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Базовый абстрактный контроллер для всех POST-контроллеров (товары, услуги, вакансии, резюме).
 * <p>
 * Предоставляет общие методы для наследников:
 * <ul>
 *     <li>Получение IP-адреса клиента</li>
 *     <li>Форматирование дат</li>
 *     <li>Построение меню городов</li>
 *     <li>Настройка моделей для страниц списков</li>
 *     <li>Проверка доступа к постам с предупреждениями</li>
 *     <li>Загрузка изображений</li>
 *     <li>Обработка ошибок</li>
 *     <li>Добавление рейтинга продавца</li>
 *     <li>Настройка приватных комментариев</li>
 *     <li>Rate limit проверки</li>
 *     <li>Валидация комментариев</li>
 * </ul>
 *
 * @see BaseController
 * @see RegionsConfig
 */
@Slf4j
public abstract class BasePostController extends BaseController {

    /**
     * Получение реального IP-адреса клиента с учетом прокси и балансировщиков.
     * Анализирует заголовок X-Forwarded-For, если он присутствует.
     *
     * @param request HTTP-запрос
     * @return IP-адрес клиента
     */
    protected String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Берем первый IP из цепочки прокси
            String clientIp = xForwardedFor.split(",")[0].trim();
            log.debug("IP клиента получен из X-Forwarded-For: {}", clientIp);
            return clientIp;
        }
        String remoteAddr = request.getRemoteAddr();
        log.debug("IP клиента получен из RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Форматирование даты и времени в строку.
     *
     * @param dateTime дата и время для форматирования
     * @return отформатированная строка в формате "dd.MM.yyyy, HH:mm" или пустая строка, если dateTime = null
     */
    protected String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            log.debug("Форматирование даты: получен null, возвращена пустая строка");
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
        String formatted = dateTime.format(formatter);
        log.debug("Дата отформатирована: {}", formatted);
        return formatted;
    }

    /**
     * Построение структуры меню городов для выбранного региона.
     *
     * @param region        выбранный регион
     * @param regionsConfig конфигурация регионов и городов
     * @return карта групп городов (standard, other, none)
     */
    protected Map<String, List<String>> getCitiesForMenu(String region, RegionsConfig regionsConfig) {
        Map<String, List<String>> menuGroups = new LinkedHashMap<>();
        if (region != null && !region.isEmpty() && !"all".equals(region)) {
            // Стандартные города региона
            List<String> standardCities = regionsConfig.getCitiesByRegion(region);
            if (standardCities != null && !standardCities.isEmpty()) {
                menuGroups.put("standard", standardCities);
                log.debug("Для региона '{}' добавлена стандартная группа городов ({} городов)", region, standardCities.size());
            }
            // Группа "другие" (опционально)
            List<String> otherGroup = new ArrayList<>();
            otherGroup.add("other_none");
            menuGroups.put("other", otherGroup);
            // Группа "без города"
            List<String> noneGroup = new ArrayList<>();
            noneGroup.add("none");
            menuGroups.put("none", noneGroup);
        } else {
            log.debug("Регион не выбран или выбран 'all' - меню городов не построено");
        }
        return menuGroups;
    }

    /**
     * Универсальная настройка модели для страницы списка (товары/услуги/вакансии).
     *
     * @param model          модель для передачи в представление
     * @param items          список элементов на текущей странице
     * @param totalItems     общее количество элементов
     * @param page           номер текущей страницы (0-based)
     * @param size           размер страницы
     * @param title          заголовок страницы
     * @param categories     список категорий
     * @param regionsConfig  конфигурация регионов
     * @param category       выбранная категория
     * @param region         выбранный регион
     * @param city           выбранный город
     * @param sort           тип сортировки
     * @param priceSort      сортировка по цене
     * @param authentication объект аутентификации
     * @param <T>            тип элементов списка
     */
    protected <T> void setupListModel(Model model, List<T> items, long totalItems, int page, int size,
                                      String title, Object categories, RegionsConfig regionsConfig,
                                      String category, String region, String city, String sort, String priceSort,
                                      Authentication authentication) {
        log.debug("Настройка модели для страницы списка: title={}, totalItems={}, page={}, size={}", title, totalItems, page, size);

        boolean hasMore = items.size() == size;
        if (page > 0 && items.isEmpty()) {
            hasMore = false;
        }
        int totalPages = (int) Math.ceil((double) totalItems / size);

        Map<String, List<String>> citiesByGroup = getCitiesForMenu(region, regionsConfig);

        model.addAttribute("title", title);
        model.addAttribute("categories", categories);
        model.addAttribute("regions", regionsConfig.getRegions());
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("regionsConfig", regionsConfig);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("selectedCategory", category != null ? category : "all");
        model.addAttribute("selectedRegion", region != null ? region : "all");
        model.addAttribute("selectedCity", city != null ? city : "all");
        model.addAttribute("currentSort", sort != null ? sort : "newest");
        model.addAttribute("currentPriceSort", priceSort != null ? priceSort : "none");

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            model.addAttribute("currentUser", currentUser);
            log.debug("Текущий пользователь добавлен в модель");
        }

        log.debug("Настройка модели страницы списка завершена");
    }

    /**
     * Универсальная настройка модели для страницы списка резюме.
     * Отличается наличием опций опыта работы.
     *
     * @param model               модель для передачи в представление
     * @param items               список элементов на текущей странице
     * @param totalItems          общее количество элементов
     * @param page                номер текущей страницы (0-based)
     * @param size                размер страницы
     * @param title               заголовок страницы
     * @param categories          список категорий
     * @param experienceOptions   список вариантов опыта работы
     * @param workformatOptions   список вариантов формата работы
     * @param regionsConfig       конфигурация регионов
     * @param category            выбранная категория
     * @param selectedExperience  выбранный опыт работы
     * @param selectedWorkformat  выбранный формат работы
     * @param region              выбранный регион
     * @param city                выбранный город
     * @param sort                тип сортировки
     * @param cashSort           сортировка по зарплате
     * @param authentication      объект аутентификации
     * @param <T>                 тип элементов списка
     */
    protected <T> void setupResumListModel(Model model, List<T> items, long totalItems, int page, int size,
                                           String title, Object categories, Object experienceOptions, Object workformatOptions, RegionsConfig regionsConfig,
                                           String category, String selectedExperience, String selectedWorkformat, String region, String city, String sort, String cashSort,
                                           Authentication authentication) {
        log.debug("Настройка модели для страницы списка резюме: title={}, totalItems={}, page={}, size={}, selectedExperience={}",
                title, totalItems, page, size, selectedExperience);

        boolean hasMore = items.size() == size;
        if (page > 0 && items.isEmpty()) {
            hasMore = false;
        }
        int totalPages = (int) Math.ceil((double) totalItems / size);

        Map<String, List<String>> citiesByGroup = getCitiesForMenu(region, regionsConfig);

        // Устанавливаем значение по умолчанию для selectedExperience, если null
        String defaultExperience = (selectedExperience != null && !selectedExperience.isEmpty())
                ? selectedExperience
                : "all";

        // Устанавливаем значение по умолчанию для selectedWorkformat, если null
        String defaultWorkformat = (selectedWorkformat != null && !selectedWorkformat.isEmpty())
                ? selectedWorkformat
                : "all";

        model.addAttribute("title", title);
        model.addAttribute("categories", categories);
        model.addAttribute("experience", experienceOptions);  // Коллекция всех вариантов опыта
        model.addAttribute("workformat", workformatOptions);  // Коллекция всех вариантов формата
        model.addAttribute("regions", regionsConfig.getRegions());
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("regionsConfig", regionsConfig);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("selectedCategory", category != null ? category : "all");
        model.addAttribute("selectedExperience", defaultExperience);
        model.addAttribute("selectedWorkformat", defaultWorkformat);
        model.addAttribute("selectedRegion", region != null ? region : "all");
        model.addAttribute("selectedCity", city != null ? city : "all");
        model.addAttribute("currentSort", sort != null ? sort : "newest");
        model.addAttribute("currentCashSort", cashSort != null ? cashSort : "none");

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            model.addAttribute("currentUser", currentUser);
            log.debug("Текущий пользователь добавлен в модель");
        }

        log.debug("Настройка модели страницы списка резюме завершена");
    }


    /**
     * Универсальная настройка модели для страницы списка вакансий.
     * Отличается наличием опций опыта работы.
     *
     * @param model               модель для передачи в представление
     * @param items               список элементов на текущей странице
     * @param totalItems          общее количество элементов
     * @param page                номер текущей страницы (0-based)
     * @param size                размер страницы
     * @param title               заголовок страницы
     * @param categories          список категорий
     * @param experienceOptions   список вариантов опыта работы
     * @param workformatOptions   список вариантов формата работы
     * @param regionsConfig       конфигурация регионов
     * @param category            выбранная категория
     * @param selectedExperience  выбранный опыт работы
     * @param selectedWorkformat  выбранный формат работы
     * @param region              выбранный регион
     * @param city                выбранный город
     * @param sort                тип сортировки
     * @param priceSort           сортировка по зарплате
     * @param authentication      объект аутентификации
     * @param <T>                 тип элементов списка
     */
    protected <T> void setupVacancyListModel(Model model, List<T> items, long totalItems, int page, int size,
                                           String title, Object categories, Object experienceOptions, Object workformatOptions, RegionsConfig regionsConfig,
                                           String category, String selectedExperience, String selectedWorkformat, String region, String city, String sort, String priceSort,
                                           Authentication authentication) {
        log.debug("Настройка модели для страницы списка резюме: title={}, totalItems={}, page={}, size={}, selectedExperience={}",
                title, totalItems, page, size, selectedExperience);

        boolean hasMore = items.size() == size;
        if (page > 0 && items.isEmpty()) {
            hasMore = false;
        }
        int totalPages = (int) Math.ceil((double) totalItems / size);

        Map<String, List<String>> citiesByGroup = getCitiesForMenu(region, regionsConfig);

        // Устанавливаем значение по умолчанию для selectedExperience, если null
        String defaultExperience = (selectedExperience != null && !selectedExperience.isEmpty())
                ? selectedExperience
                : "all";


        // Устанавливаем значение по умолчанию для selectedWorkformat, если null
        String defaultWorkformat = (selectedWorkformat != null && !selectedWorkformat.isEmpty())
                ? selectedWorkformat
                : "all";

        model.addAttribute("title", title);
        model.addAttribute("categories", categories);
        model.addAttribute("experience", experienceOptions);  // Коллекция всех вариантов опыта
        model.addAttribute("workformat", workformatOptions);  // Коллекция всех вариантов формата
        model.addAttribute("regions", regionsConfig.getRegions());
        model.addAttribute("citiesByGroup", citiesByGroup);
        model.addAttribute("regionsConfig", regionsConfig);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("selectedCategory", category != null ? category : "all");
        model.addAttribute("selectedExperience", defaultExperience);
        model.addAttribute("selectedWorkformat", defaultWorkformat);
        model.addAttribute("selectedRegion", region != null ? region : "all");
        model.addAttribute("selectedCity", city != null ? city : "all");
        model.addAttribute("currentSort", sort != null ? sort : "newest");
        model.addAttribute("currentPriceSort", priceSort != null ? priceSort : "none");

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            model.addAttribute("currentUser", currentUser);
            log.debug("Текущий пользователь добавлен в модель");
        }

        log.debug("Настройка модели страницы списка резюме завершена");
    }

    /**
     * Универсальная проверка доступа к посту и добавление предупреждений в модель.
     * <p>
     * Проверяет, виден ли пост пользователю с учетом:
     * <ul>
     *     <li>Статусов поста (blocked, archived, moderation)</li>
     *     <li>Прав администратора</li>
     *     <li>Прав владельца поста</li>
     * </ul>
     *
     * @param post           объект поста (товар, услуга, вакансия, резюме)
     * @param authentication объект аутентификации
     * @param model          модель для добавления предупреждений
     * @return true - доступ разрешен, false - доступ запрещен (пост скрыт)
     */
    protected boolean checkPostAccessAndWarnings(Object post, Authentication authentication, Model model) {
        log.debug("Проверка доступа к посту типа: {}", post.getClass().getSimpleName());

        boolean hasAdminAccess = hasSuperAccess(authentication);
        boolean isOwner = false;

        if (authentication != null && authentication.isAuthenticated()) {
            User currentUser = (User) authentication.getPrincipal();
            // Рефлексия для получения user из поста (без создания интерфейсов)
            try {
                java.lang.reflect.Method getUserMethod = post.getClass().getMethod("getUser");
                User postUser = (User) getUserMethod.invoke(post);
                isOwner = postUser != null && postUser.getId().equals(currentUser.getId());
                log.debug("Проверка владельца: isOwner={}", isOwner);
            } catch (Exception e) {
                log.warn("Ошибка при получении владельца поста через рефлексию: {}", e.getClass().getSimpleName());
                isOwner = false;
            }
        }

        // Проверка статусов через рефлексию
        boolean isBlocked = getBooleanField(post, "blocked");
        boolean isArchived = getBooleanField(post, "archived");
        boolean isModeration = getBooleanField(post, "moderation");

        log.debug("Статусы поста: blocked={}, archived={}, moderation={}, hasAdminAccess={}, isOwner={}",
                isBlocked, isArchived, isModeration, hasAdminAccess, isOwner);

        boolean isAvailable = hasAdminAccess || isOwner || (!isBlocked && !isArchived && !isModeration);
        log.debug("Пост доступен для просмотра: {}", isAvailable);

        // Добавляем предупреждение для админа/владельца, если пост заблокирован или на модерации
        if ((hasAdminAccess || isOwner) && (isBlocked || isModeration)) {
            String type = getPostType(post);
            if (isBlocked) {
                model.addAttribute("adminWarning", "Этот " + type + " ЗАБЛОКИРОВАН. Он виден только администраторам и владельцу.");
                log.debug("Добавлено предупреждение о блокировке поста");
            } else if (isModeration) {
                model.addAttribute("adminWarning", "Этот " + type + " на МОДЕРАЦИИ. Он виден только администраторам и владельцу.");
                log.debug("Добавлено предупреждение о модерации поста");
            }
        }

        model.addAttribute("isAdminUser", hasAdminAccess);
        model.addAttribute("isBlocked", isBlocked);
        model.addAttribute("isModeration", isModeration);
        model.addAttribute("isArchived", isArchived);

        return isAvailable;
    }

    /**
     * Вспомогательный метод для получения boolean-поля объекта через рефлексию.
     *
     * @param obj       объект для анализа
     * @param fieldName имя поля (ожидается геттер вида isFieldName)
     * @return значение поля или false в случае ошибки
     */
    private boolean getBooleanField(Object obj, String fieldName) {
        try {
            String getterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            java.lang.reflect.Method method = obj.getClass().getMethod(getterName);
            boolean value = (boolean) method.invoke(obj);
            log.debug("Поле '{}' получено через рефлексию: значение={}", fieldName, value);
            return value;
        } catch (Exception e) {
            log.debug("Не удалось получить поле '{}' через рефлексию: {}", fieldName, e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Определение типа поста по имени класса.
     *
     * @param post объект поста
     * @return строковое представление типа (товар, услуга, вакансия, резюме)
     */
    private String getPostType(Object post) {
        String className = post.getClass().getSimpleName();
        String type;
        if (className.contains("PostText")) {
            type = "товар";
        } else if (className.contains("PostTextJob")) {
            type = "услуга";
        } else if (className.contains("Vacancy")) {
            type = "вакансия";
        } else if (className.contains("Resume")) {
            type = "резюме";
        } else {
            type = "пост";
        }
        log.debug("Определен тип поста: {} -> {}", className, type);
        return type;
    }

    /**
     * Универсальная загрузка изображений.
     * Выполняет базовую валидацию количества и наличия файлов.
     *
     * @param files         массив загружаемых файлов
     * @param postProduct   объект товара (может быть null)
     * @param postService   объект услуги (может быть null)
     * @param postVacancy   объект вакансии (может быть null)
     * @param postResume    объект резюме (может быть null)
     * @param model         модель для добавления ошибок
     * @param errorView     представление для ошибок
     * @throws java.io.IOException при ошибках ввода-вывода
     */
    protected void uploadImages(MultipartFile[] files, Object postProduct, Object postService,
                                Object postVacancy, Object postResume, Model model, String errorView) throws java.io.IOException {
        log.debug("Загрузка изображений: количество файлов={}", files != null ? files.length : 0);

        if (files == null || files.length == 0 || files[0].isEmpty()) {
            log.warn("Попытка загрузки без выбранных файлов");
            model.addAttribute("error", "Пожалуйста, выберите хотя бы один файл для загрузки");
            throw new IllegalArgumentException("No files");
        }
        if (files.length > 10) {
            log.warn("Превышено максимальное количество файлов: {}", files.length);
            model.addAttribute("error", "Можно загрузить не более 10 изображений");
            throw new IllegalArgumentException("Too many files");
        }
        log.debug("Валидация файлов пройдена успешно");
    }

    /**
     * Универсальная обработка ошибок для страниц создания (товары, услуги, вакансии).
     *
     * @param e          исключение
     * @param model      модель для добавления ошибок
     * @param categories список категорий
     * @param errorView  представление для ошибок
     * @return путь к представлению ошибки
     */
    protected String handleCreationError(Exception e, Model model, Object categories, String errorView) {
        log.error("Ошибка при создании: {}", e.getClass().getSimpleName(), e);

        if (e instanceof IllegalArgumentException && e.getMessage().equals("No files")) {
            log.debug("Обработка ошибки: отсутствуют файлы");
            model.addAttribute("categories", categories);
            return errorView;
        }
        if (e instanceof IllegalArgumentException && e.getMessage().equals("Too many files")) {
            log.debug("Обработка ошибки: слишком много файлов");
            model.addAttribute("categories", categories);
            return errorView;
        }
        if (e instanceof java.io.IOException) {
            log.debug("Обработка ошибки ввода-вывода");
            model.addAttribute("error", "Ошибка при загрузке файлов: " + e.getMessage());
        } else {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("categories", categories);
        return errorView;
    }

    /**
     * Универсальная обработка ошибок для страниц создания резюме.
     * Отличается наличием опций опыта работы.
     *
     * @param e          исключение
     * @param model      модель для добавления ошибок
     * @param categories список категорий
     * @param experience список опций опыта
     * @param workformat список формата работы
     * @param errorView  представление для ошибок
     * @return путь к представлению ошибки
     */
    protected String handleCreationErrorResume(Exception e, Model model, Object categories, Object experience, Object workformat, String errorView) {
        log.error("Ошибка при создании резюме: {}", e.getClass().getSimpleName(), e);

        if (e instanceof IllegalArgumentException && e.getMessage().equals("No files")) {
            log.debug("Обработка ошибки: отсутствуют файлы");
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            return errorView;
        }
        if (e instanceof IllegalArgumentException && e.getMessage().equals("Too many files")) {
            log.debug("Обработка ошибки: слишком много файлов");
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            return errorView;
        }
        if (e instanceof java.io.IOException) {
            log.debug("Обработка ошибки ввода-вывода");
            model.addAttribute("error", "Ошибка при загрузке файлов: " + e.getMessage());
        } else {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("categories", categories);
        model.addAttribute("experience", experience);
        model.addAttribute("workformat", workformat);
        return errorView;
    }


    protected String handleCreationErrorVacancy(Exception e, Model model, Object categories, Object experience, Object workformat, String errorView) {
        log.error("Ошибка при создании вакансии: {}", e.getClass().getSimpleName(), e);

        if (e instanceof IllegalArgumentException && e.getMessage().equals("No files")) {
            log.debug("Обработка ошибки: отсутствуют файлы");
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            return errorView;
        }
        if (e instanceof IllegalArgumentException && e.getMessage().equals("Too many files")) {
            log.debug("Обработка ошибки: слишком много файлов");
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            return errorView;
        }
        if (e instanceof java.io.IOException) {
            log.debug("Обработка ошибки ввода-вывода");
            model.addAttribute("error", "Ошибка при загрузке файлов: " + e.getMessage());
        } else {
            model.addAttribute("error", "Ошибка: " + e.getMessage());
        }
        model.addAttribute("categories", categories);
        model.addAttribute("experience", experience);
        model.addAttribute("workformat", workformat);
        return errorView;
    }

    /**
     * Универсальный метод для добавления рейтинга продавца в модель.
     *
     * @param post            объект поста (для получения владельца)
     * @param model           модель для добавления атрибутов
     * @param ratingRepository репозиторий рейтингов
     */
    protected void addSellerRatingToModel(Object post, Model model,
                                          jobforsite.sitejob.module.users.repository.RatingRepository ratingRepository) {
        log.debug("Добавление рейтинга продавца в модель");

        User seller = null;
        try {
            java.lang.reflect.Method getUserMethod = post.getClass().getMethod("getUser");
            seller = (User) getUserMethod.invoke(post);
            log.debug("Продавец получен из поста");
        } catch (Exception e) {
            log.debug("Не удалось получить продавца из поста: {}", e.getClass().getSimpleName());
        }

        if (seller != null) {
            Double averageRating = ratingRepository.findAverageRatingBySellerId(seller.getId());
            Long ratingCount = ratingRepository.countRatingsBySellerId(seller.getId());
            model.addAttribute("averageRating", averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0);
            model.addAttribute("ratingCount", ratingCount != null ? ratingCount : 0);
            model.addAttribute("targetUser", seller.isOnline());
            log.debug("Рейтинг продавца добавлен: avg={}, count={}",
                    averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0,
                    ratingCount != null ? ratingCount : 0);
        } else {
            model.addAttribute("averageRating", 0);
            model.addAttribute("ratingCount", 0);
            model.addAttribute("targetUser", false);
            log.debug("Продавец не найден, установлены значения по умолчанию");
        }
    }

    /**
     * Универсальная настройка приватных комментариев.
     * Определяет, какие комментарии должен видеть пользователь в зависимости от его роли.
     *
     * @param post               объект поста
     * @param postId             идентификатор поста
     * @param authentication     объект аутентификации
     * @param hasAdminAccess     флаг наличия прав администратора
     * @param findAllByPostId    функция для получения всех комментариев поста
     * @param findByPostIdAndUser функция для получения комментариев для конкретного пользователя
     * @param markAsRead         функция для отметки комментариев как прочитанных
     * @param <C>                тип комментария
     * @return список доступных комментариев
     */
    protected <C> List<C> setupPrivateComments(Object post, Long postId, Authentication authentication,
                                               boolean hasAdminAccess,
                                               java.util.function.Function<Long, List<C>> findAllByPostId,
                                               java.util.function.BiFunction<Long, Long, List<C>> findByPostIdAndUser,
                                               java.util.function.BiConsumer<Long, Long> markAsRead) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("Настройка приватных комментариев: пользователь не аутентифицирован, возвращен пустой список");
            return Collections.emptyList();
        }

        User currentUser = (User) authentication.getPrincipal();

        // Отмечаем сообщения как прочитанные
        if (markAsRead != null) {
            markAsRead.accept(postId, currentUser.getId());
            log.debug("Комментарии отмечены как прочитанные для поста id={}", postId);
        }

        User postOwner = null;
        try {
            java.lang.reflect.Method getUserMethod = post.getClass().getMethod("getUser");
            postOwner = (User) getUserMethod.invoke(post);
        } catch (Exception e) {
            log.debug("Не удалось получить владельца поста для приватных комментариев: {}", e.getClass().getSimpleName());
        }

        Long ownerId = postOwner != null ? postOwner.getId() : null;

        List<C> comments;
        if (hasAdminAccess) {
            comments = findAllByPostId.apply(postId);
            log.debug("Приватные комментарии: администратор, получены все комментарии ({} шт.)", comments.size());
        } else if (ownerId != null && currentUser.getId().equals(ownerId)) {
            comments = findAllByPostId.apply(postId);
            log.debug("Приватные комментарии: владелец поста, получены все комментарии ({} шт.)", comments.size());
        } else {
            comments = findByPostIdAndUser.apply(postId, currentUser.getId());
            log.debug("Приватные комментарии: обычный пользователь, получены только его комментарии ({} шт.)", comments.size());
        }

        return comments;
    }

    /**
     * Универсальная проверка rate limit для просмотра страницы (товары, услуги, вакансии, резюме).
     *
     * @param clientIp          IP-адрес клиента
     * @param rateLimitService  сервис rate limit
     * @param type              тип контента ("product", "service", "vacancy", "resume")
     * @param model             модель для добавления ошибки
     * @param title             заголовок страницы
     * @param categories        список категорий
     * @param regionsConfig     конфигурация регионов
     * @return true - запрос разрешен, false - превышен лимит
     */
    protected boolean checkRateLimitForView(String clientIp,
                                            jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                            String type, Model model, String title, Object categories,
                                            RegionsConfig regionsConfig) {
        log.debug("Проверка rate limit для просмотра: type={}, clientIp={}", type, clientIp);

        boolean allowed;
        if ("product".equals(type)) {
            allowed = rateLimitService.tryConsumeProductView(clientIp);
        } else if ("service".equals(type)) {
            allowed = rateLimitService.tryConsumeServiceView(clientIp);
        } else if ("vacancy".equals(type)) {
            allowed = rateLimitService.tryConsumeVacancyView(clientIp);
        } else {
            allowed = rateLimitService.tryConsumeResumeView(clientIp);
        }

        if (!allowed) {
            log.warn("Превышен rate limit для просмотра: type={}, clientIp={}", type, clientIp);
            model.addAttribute("error", "Слишком частые запросы. Пожалуйста, подождите.");
            model.addAttribute("title", title);
            model.addAttribute("categories", categories);
            model.addAttribute("regions", regionsConfig.getRegions());
            return false;
        }
        log.debug("Rate limit проверка пройдена");
        return true;
    }

    /**
     * Универсальная проверка rate limit для создания поста.
     *
     * @param clientIp          IP-адрес клиента
     * @param rateLimitService  сервис rate limit
     * @param type              тип контента
     * @param model             модель для добавления ошибки
     * @param categories        список категорий
     * @param errorView         представление для ошибок
     * @return true - запрос разрешен, false - превышен лимит
     */
    protected boolean checkRateLimitForCreate(String clientIp,
                                              jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                              String type, Model model, Object categories, String errorView) {
        log.debug("Проверка rate limit для создания: type={}, clientIp={}", type, clientIp);

        boolean allowed;
        if ("product".equals(type)) {
            allowed = rateLimitService.tryConsumeProductCreate(clientIp);
        } else if ("service".equals(type)) {
            allowed = rateLimitService.tryConsumeServiceCreate(clientIp);
        } else if ("vacancy".equals(type)) {
            allowed = rateLimitService.tryConsumeVacancyCreate(clientIp);
        } else {
            allowed = rateLimitService.tryConsumeResumeCreate(clientIp);
        }

        if (!allowed) {
            log.warn("Превышен rate limit для создания: type={}, clientIp={}", type, clientIp);
            model.addAttribute("error", "Слишком много попыток создания. Попробуйте через час.");
            model.addAttribute("categories", categories);
            return false;
        }
        log.debug("Rate limit проверка создания пройдена");
        return true;
    }

    /**
     * Универсальная проверка rate limit для просмотра страницы резюме.
     *
     * @param clientIp          IP-адрес клиента
     * @param rateLimitService  сервис rate limit
     * @param type              тип контента (ожидается "resume")
     * @param model             модель для добавления ошибки
     * @param title             заголовок страницы
     * @param categories        список категорий
     * @param experience        список опций опыта
     * @param workformat        список формата работы
     * @param regionsConfig     конфигурация регионов
     * @return true - запрос разрешен, false - превышен лимит
     */
    protected boolean checkRateLimitForViewResume(String clientIp,
                                                  jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                                  String type, Model model, String title, Object categories, Object experience, Object workformat,
                                                  RegionsConfig regionsConfig) {
        log.debug("Проверка rate limit для просмотра резюме: type={}, clientIp={}", type, clientIp);

        boolean allowed = rateLimitService.tryConsumeResumeView(clientIp);

        if (!allowed) {
            log.warn("Превышен rate limit для просмотра резюме: clientIp={}", clientIp);
            model.addAttribute("error", "Слишком частые запросы. Пожалуйста, подождите.");
            model.addAttribute("title", title);
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            model.addAttribute("regions", regionsConfig.getRegions());
            return false;
        }
        log.debug("Rate limit проверка просмотра резюме пройдена");
        return true;
    }

    /**
     * Универсальная проверка rate limit для создания резюме.
     *
     * @param clientIp          IP-адрес клиента
     * @param rateLimitService  сервис rate limit
     * @param type              тип контента (ожидается "resume")
     * @param model             модель для добавления ошибки
     * @param categories        список категорий
     * @param experience        список опций опыта
     * @param workformat        список формата работы
     * @param errorView         представление для ошибок
     * @return true - запрос разрешен, false - превышен лимит
     */
    protected boolean checkRateLimitForCreateResume(String clientIp,
                                                    jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                                    String type, Model model, Object categories, Object experience, Object workformat, String errorView) {
        log.debug("Проверка rate limit для создания резюме: type={}, clientIp={}", type, clientIp);

        boolean allowed = rateLimitService.tryConsumeResumeCreate(clientIp);

        if (!allowed) {
            log.warn("Превышен rate limit для создания резюме: clientIp={}", clientIp);
            model.addAttribute("error", "Слишком много попыток создания. Попробуйте через час.");
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            return false;
        }
        log.debug("Rate limit проверка создания резюме пройдена");
        return true;
    }



    protected boolean checkRateLimitForViewVacancy(String clientIp,
                                                  jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                                  String type, Model model, String title, Object categories, Object experience, Object workformat,
                                                  RegionsConfig regionsConfig) {
        log.debug("Проверка rate limit для просмотра ваканси1: type={}, clientIp={}", type, clientIp);

        boolean allowed = rateLimitService.tryConsumeVacancyView(clientIp);

        if (!allowed) {
            log.warn("Превышен rate limit для просмотра вакансий: clientIp={}", clientIp);
            model.addAttribute("error", "Слишком частые запросы. Пожалуйста, подождите.");
            model.addAttribute("title", title);
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            model.addAttribute("regions", regionsConfig.getRegions());
            return false;
        }
        log.debug("Rate limit проверка просмотра вакансий пройдена");
        return true;
    }

    /**
     * Универсальная проверка rate limit для создания резюме.
     *
     * @param clientIp          IP-адрес клиента
     * @param rateLimitService  сервис rate limit
     * @param type              тип контента (ожидается "resume")
     * @param model             модель для добавления ошибки
     * @param categories        список категорий
     * @param experience        список опций опыта
     * @param workformat        список формата работы
     * @param errorView         представление для ошибок
     * @return true - запрос разрешен, false - превышен лимит
     */
    protected boolean checkRateLimitForCreateVacancy(String clientIp,
                                                    jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                                    String type, Model model, Object categories, Object experience, Object workformat, String errorView) {
        log.debug("Проверка rate limit для создания вакансии: type={}, clientIp={}", type, clientIp);

        boolean allowed = rateLimitService.tryConsumeVacancyCreate(clientIp);

        if (!allowed) {
            log.warn("Превышен rate limit для создания вакансии: clientIp={}", clientIp);
            model.addAttribute("error", "Слишком много попыток создания. Попробуйте через час.");
            model.addAttribute("categories", categories);
            model.addAttribute("experience", experience);
            model.addAttribute("workformat", workformat);
            return false;
        }
        log.debug("Rate limit проверка создания резюме пройдена");
        return true;
    }


    /**
     * Универсальная проверка комментария на валидность.
     *
     * @param text                     текст комментария
     * @param clientIp                 IP-адрес клиента
     * @param enhancedRateLimitService улучшенный сервис rate limit
     * @param rateLimitService         базовый сервис rate limit
     * @param commentType              тип комментария ("personal" или другой)
     * @return true - комментарий валиден, false - не прошел валидацию
     */
    protected boolean validateComment(String text, String clientIp,
                                      jobforsite.sitejob.module.shared.services.cachrateservice.EnhancedRateLimitService enhancedRateLimitService,
                                      jobforsite.sitejob.module.shared.services.cachrateservice.RateLimitService rateLimitService,
                                      String commentType) {
        log.debug("Валидация комментария: длина текста={}, type={}, clientIp={}", text != null ? text.length() : 0, commentType, clientIp);

        if (text.length() < 3) {
            enhancedRateLimitService.recordFailedAttempt(clientIp);
            log.warn("Комментарий отклонен: слишком короткий текст, clientIp={}", clientIp);
            return false;
        }

        boolean allowed;
        if ("personal".equals(commentType)) {
            allowed = rateLimitService.tryConsumePersonalCommentAdd(clientIp);
        } else {
            allowed = rateLimitService.tryConsumeCommentAdd(clientIp);
        }

        if (!allowed) {
            log.warn("Комментарий отклонен: превышен rate limit, type={}, clientIp={}", commentType, clientIp);
            return false;
        }

        log.debug("Комментарий успешно прошел валидацию");
        return true;
    }

    /**
     * Интерфейс для унификации проверки доступа к постам.
     * Может быть реализован для типизированной работы с постами.
     */
    public interface PostAccessInfo {
        User getUser();
        boolean isBlocked();
        boolean isArchived();
        boolean isModeration();
    }
}