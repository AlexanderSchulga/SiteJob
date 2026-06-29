package jobforsite.sitejob.module.shared.services.util;

import jobforsite.sitejob.module.shared.config.categoryconfig.ExperienceResumCategoryConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.ExperienceVacancyCategoryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Утилитарный класс для работы с опытом работы (стажем) в различных типах контента.
 * <p>
 * Предоставляет методы для получения отображаемого имени значения опыта
 * на основе типа контента и ключа опыта.
 * </p>
 */
@RequiredArgsConstructor
@Component
public class ExperienceUtils {

    private final ExperienceResumCategoryConfig experienceResumCategoryConfig;
    private final ExperienceVacancyCategoryConfig experienceVacancyCategoryConfig;

    /**
     * Возвращает отображаемое имя для значения опыта работы.
     * <p>
     * Особенности обработки:
     * <ul>
     *     <li>Для null, пустой строки или значения "all" возвращается "Не имеет значение"</li>
     *     <li>Для типа "resume" используется конфигурация ExperienceResumCategoryConfig</li>
     *     <li>Для остальных типов возвращается исходный ключ</li>
     * </ul>
     * </p>
     *
     * @param type тип контента (например, "resume")
     * @param experienKey ключ опыта (например, "1-3", "3-5", "all")
     * @return отображаемое имя опыта; если для ключа не найдено отображение в конфигурации,
     *         возвращается исходный experienKey; если experienKey равен null, пустой или "all" — "Не имеет значение"
     */
    public String getExperienDisplayName(String type, String experienKey) {
        // Для null или "all" возвращаем "Не имеет значение"
        if (experienKey == null || experienKey.isEmpty() || "all".equals(experienKey)) {
            return "Не имеет значение";
        }

        switch (type) {
            case "resume":
                String displayName = experienceResumCategoryConfig.getExperienDisplayName(experienKey);
                return displayName != null ? displayName : experienKey;
            case "vacancy":
                String displayNames = experienceVacancyCategoryConfig.getExperienDisplayName(experienKey);
                return displayNames != null ? displayNames : experienKey;
            default:
                return experienKey;
        }
    }
}