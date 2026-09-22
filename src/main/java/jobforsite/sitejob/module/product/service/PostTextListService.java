package jobforsite.sitejob.module.product.service;

import jobforsite.sitejob.module.product.dto.PostTextListDTO;
import jobforsite.sitejob.module.product.mapper.*;
import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для преобразования списков постов в DTO для отображения.
 * <p>
 * Предоставляет функциональность для конвертации различных типов постов
 * (наследников {@link PostText}) в универсальные DTO {@link PostTextListDTO}
 * для передачи на клиентскую сторону.
 * </p>
 * <p>
 * Использует паттерн Стратегия для выбора подходящего маппера в зависимости
 * от конкретного типа поста. Каждый тип поста имеет свой специализированный
 * маппер, который знает, как преобразовать специфичные поля.
 * </p>
 *
 * @see PostText
 * @see PostTextListDTO
 * @see PostTextListMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostTextListService {

    private final DefaultPostTextListMapper defaultMapper;
    private final RealtyPostListMapper realtyMapper;
    private final VehiclePostListMapper vehicleMapper;

    private final ExtendsPostListMapperAll.AutoPostListMapper autoMapper;
    private final ExtendsPostListMapperAll.ConstructionsPostListMapper constructionsMapper;
    private final ExtendsPostListMapperAll.InteriorPostListMapper interiorMapper;
    private final ExtendsPostListMapperAll.ToolsPostListMapper toolsMapper;
    private final ExtendsPostListMapperAll.AppliancesPostListMapper appliancesMapper;
    private final ExtendsPostListMapperAll.ElectronicsPostListMapper electronicsMapper;
    private final ExtendsPostListMapperAll.ClothingPostListMapper clothingMapper;
    private final ExtendsPostListMapperAll.ChildrenPrPostListMapper childrenPrMapper;
    private final ExtendsPostListMapperAll.SportsPostListMapper sportsMapper;
    private final ExtendsPostListMapperAll.FoodPostListMapper foodMapper;
    private final ExtendsPostListMapperAll.HealthPostListMapper healthMapper;
    private final ExtendsPostListMapperAll.AnimalsPostListMapper animalsMapper;

    /**
     * Преобразует список постов любого типа в список DTO {@link PostTextListDTO}.
     * <p>
     * Для каждого поста в списке определяет его конкретный тип и применяет
     * соответствующий маппер. Если список пуст или равен {@code null},
     * возвращает пустой список.
     * </p>
     *
     * @param posts список постов для преобразования (может быть {@code null})
     * @return список DTO {@link PostTextListDTO}, либо пустой список,
     *         если входной список пуст или {@code null}
     */
    public List<PostTextListDTO> toListDTO(List<? extends PostText> posts) {
        if (posts == null || posts.isEmpty()) {
            return Collections.emptyList();
        }

        return posts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Преобразует один пост в DTO {@link PostTextListDTO}.
     * <p>
     * Определяет конкретный тип поста и выбирает соответствующий маппер:
     * <ul>
     *   <li>Для недвижимости - {@link RealtyPostListMapper}</li>
     *   <li>Для автотранспорта - {@link VehiclePostListMapper}</li>
     *   <li>Для остальных типов - {@link DefaultPostTextListMapper}</li>
     *   *   <li> и другие</li>
     * </ul>
     * </p>
     *
     * @param post пост для преобразования
     * @return DTO {@link PostTextListDTO} с данными поста
     * @throws IllegalArgumentException если пост имеет неподдерживаемый тип
     */
    private PostTextListDTO mapToDTO(PostText post) {
        PostTextListMapper<?> mapper;

        if (post instanceof RealEstatePostText) {
            mapper = realtyMapper;
        } else if (post instanceof MotorVehiclesPostText) {
            mapper = vehicleMapper;
        } else if (post instanceof AutoPostText) {
            mapper = autoMapper;
        } else if (post instanceof ConstructionsPostText) {
            mapper = constructionsMapper;
        } else if (post instanceof InteriorPostText) {
            mapper = interiorMapper;
        } else if (post instanceof ToolsPostText) {
            mapper = toolsMapper;
        } else if (post instanceof AppliancesPostText) {
            mapper = appliancesMapper;
        } else if (post instanceof ElectronicsPostText) {
            mapper = electronicsMapper;
        } else if (post instanceof ClothingPostText) {
            mapper = clothingMapper;
        } else if (post instanceof ChildrenPrPostText) {
            mapper = childrenPrMapper;
        } else if (post instanceof SportsPostText) {
            mapper = sportsMapper;
        } else if (post instanceof FoodPostText) {
            mapper = foodMapper;
        } else if (post instanceof HealthPostText) {
            mapper = healthMapper;
        } else if (post instanceof AnimalsPostText) {
            mapper = animalsMapper;
        } else {
            log.warn("Используется дефолтный маппер для неизвестного типа поста: {}",
                    post.getClass().getSimpleName());
            mapper = defaultMapper;
        }

        // Используем сырой тип (unsafe cast), но это безопасно,
        // т.к. мапперы имплементируют один интерфейс и все умеют обрабатывать PostText
        @SuppressWarnings("unchecked")
        PostTextListMapper<PostText> castedMapper = (PostTextListMapper<PostText>) mapper;
        return castedMapper.toListDTO(post);
    }
}