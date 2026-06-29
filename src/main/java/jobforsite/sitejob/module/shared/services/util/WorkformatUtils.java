package jobforsite.sitejob.module.shared.services.util;

import jobforsite.sitejob.module.shared.config.categoryconfig.WorkFormatResumeConfig;
import jobforsite.sitejob.module.shared.config.categoryconfig.WorkFormatVacancyConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class WorkformatUtils {

    private final WorkFormatResumeConfig workFormatResumeConfig;
    private final WorkFormatVacancyConfig workFormatVacancyConfig;

    /**
     * Возвращает отображаемое имя для значения формата работы.
     * <p>
     * Особенности обработки:
     * <ul>
     *     <li>Для null, пустой строки или значения "all" возвращается "Не имеет значение"</li>
     *     <li>Для типа "resume" используется конфигурация WorkFormatResumeConfig</li>
     *     <li>Для остальных типов возвращается исходный ключ</li>
     * </ul>
     * </p>
     *
     * @param type тип контента (например, "resume")
     * @param workformatKey ключ формата (например,"all")
     * @return отображаемое имя формата; если для ключа не найдено отображение в конфигурации,
     *         возвращается исходный workformatKey; если workformatKey равен null, пустой или "all" — "Все"
     */
    public String getWorkformatDisplayName(String type, String workformatKey) {
        // Для null или "all" возвращаем "Все"
        if (workformatKey == null || workformatKey.isEmpty() || "all".equals(workformatKey)) {
            return "Все";
        }

        switch (type) {
            case "resume":
                String displayName = workFormatResumeConfig.getWorkformatDisplayName(workformatKey);
                return displayName != null ? displayName : workformatKey;
            case "vacancy":
                String displayNames = workFormatVacancyConfig.getWorkformatDisplayName(workformatKey);
                return displayNames != null ? displayNames : workformatKey;
            default:
                return workformatKey;
        }
    }


}
