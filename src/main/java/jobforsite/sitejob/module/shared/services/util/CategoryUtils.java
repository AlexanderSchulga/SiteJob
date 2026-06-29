package jobforsite.sitejob.module.shared.services.util;

import jobforsite.sitejob.module.shared.config.categoryconfig.PostResumeCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostTextJobCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.PostVacancyCategoryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Утилитарный класс для работы с категориями различных типов контента.
 * <p>
 * Предоставляет методы для получения отображаемого имени категории
 * на основе типа контента (product, service, vacancy, resume) и ключа категории.
 * </p>
 */
@RequiredArgsConstructor
@Component
public class CategoryUtils {

    private final PostTextCategoryConfig postTextCategoryConfig;
    private final PostTextJobCategoryConfig postTextJobCategoryConfig;
    private final PostVacancyCategoryConfig postVacancyCategoryConfig;
    private final PostResumeCategoryConfig postResumeCategoryConfig;

    /**
     * Возвращает отображаемое имя категории для указанного типа контента.
     * <p>
     * Поддерживаемые типы:
     * <ul>
     *     <li>"product" — для товаров/услуг (через PostTextCategoryConfig)</li>
     *     <li>"service" — для сервисов (через PostTextJobCategoryConfig)</li>
     *     <li>"vacancy" — для вакансий (через PostVacancyCategoryConfig)</li>
     *     <li>"resume" — для резюме (через PostResumeCategoryConfig)</li>
     * </ul>
     * </p>
     *
     * @param type тип контента (product, service, vacancy, resume)
     * @param categoryKey ключ категории (например, "it", "marketing")
     * @return отображаемое имя категории или "Не указано", если categoryKey пустой;
     *         если тип не распознан, возвращается исходный categoryKey
     */
    public String getCategoryDisplayName(String type, String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return "Не указано";
        }

        switch (type) {
            case "product":
                return postTextCategoryConfig.getCategoryDisplayName(categoryKey);
            case "service":
                return postTextJobCategoryConfig.getCategoryDisplayName(categoryKey);
            case "vacancy":
                return postVacancyCategoryConfig.getCategoryDisplayName(categoryKey);
            case "resume":
                return postResumeCategoryConfig.getCategoryDisplayName(categoryKey);
            default:
                return categoryKey;
        }
    }
}