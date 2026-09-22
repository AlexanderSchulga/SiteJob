package jobforsite.sitejob.module.product.mapper;

import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.model.PostText;

/**
 * Интерфейс маппера для преобразования постов в DTO для отображения в списках.
 * <p>
 * Определяет контракт для преобразования сущностей, наследующих {@link PostText},
 * в {@link PostTextListDTO} для отображения в списках (каталог, поиск, главная страница).
 * </p>
 * <p>
 * Реализации должны предоставлять маппинг для конкретных типов постов из пакета
 * {@code jobforsite.sitejob.module.product.model.extendsPost.modelExtends}.
 * </p>
 *
 * @param <T> тип поста, наследующий {@link PostText}
 * @see PostTextListDTO
 * @see PostText
 */

public interface PostTextListMapper <T extends PostText> {
        PostTextListDTO toListDTO(T post);
}
