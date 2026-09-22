package jobforsite.sitejob.module.product.service.filter;

import jobforsite.sitejob.module.product.dto.ProductFilterDTO;
import jobforsite.sitejob.module.product.model.extendsPost.modelExtends.*;
import jobforsite.sitejob.module.product.model.extendsPost.repositoryExtendsPostText.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для фильтрации и получения постов различных категорий продуктов.
 * <p>
 * Предоставляет методы для фильтрации постов с учетом региона, города,
 * сортировки и других специфических для категории параметров.
 * Оптимизирован через двухэтапную загрузку: сначала получаются ID записей,
 * затем по ним загружаются полные данные с изображениями.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtendsProductService {

    private final AutoPostRepository autoPostRepository;
    private final ConstructionsPostRepository constructionsPostRepository;
    private final InteriorPostRepository interiorPostRepository;
    private final ToolsPostRepository toolsPostRepository;
    private final AppliancesPostRepository appliancesPostRepository;
    private final ElectronicsPostRepository electronicsPostRepository;
    private final ClothingPostRepository clothingPostRepository;
    private final ChildrenPrPostRepository childrenPrPostRepository;
    private final SportsPostRepository sportsPostRepository;
    private final FoodPostRepository foodPostRepository;
    private final HealthPostRepository healthPostRepository;
    private final AnimalsPostRepository animalsPostRepository;

    // ============================================================
    // АВТОЗАПЧАСТИ И АВТОТОВАРЫ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Автозапчасти и автотовары"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории AutoPostText
     */
    public List<AutoPostText> filterAutoWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = autoPostRepository.findAutoPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAutoType(),
                filter.getConditionAuto(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<AutoPostText> posts = autoPostRepository.findAutoPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, AutoPostText> postMap = posts.stream()
                .collect(Collectors.toMap(AutoPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Автозапчасти и автотовары"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countAutoWithFilters(ProductFilterDTO filter) {
        return autoPostRepository.countAutoPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAutoType(),
                filter.getConditionAuto()
        );
    }



    // ============================================================
    // СТРОИТЕЛЬНЫЕ МАТЕРИАЛЫ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Строительные материалы"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории ConstructionsPostText
     */
    public List<ConstructionsPostText> filterConstructionsWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = constructionsPostRepository.findConstructionsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getConstructionsType(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<ConstructionsPostText> posts = constructionsPostRepository.findConstructionsPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, ConstructionsPostText> postMap = posts.stream()
                .collect(Collectors.toMap(ConstructionsPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Строительные материалы"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countConstructionsWithFilters(ProductFilterDTO filter) {
        return constructionsPostRepository.countConstructionsPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getConstructionsType()
        );
    }


    // ============================================================
    // ДОМ И ИНТЕРЬЕР
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Дом и интерьер"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории InteriorPostText
     */
    public List<InteriorPostText> filterInteriorWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = interiorPostRepository.findInteriorPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getInteriorType(),
                filter.getConditionInterior(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<InteriorPostText> posts = interiorPostRepository.findInteriorPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, InteriorPostText> postMap = posts.stream()
                .collect(Collectors.toMap(InteriorPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Дом и интерьер"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countInteriorWithFilters(ProductFilterDTO filter) {
        return interiorPostRepository.countInteriorPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getInteriorType(),
                filter.getConditionInterior()
        );
    }


    // ============================================================
    // ИНСТРУМЕНТЫ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Инструменты"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории ToolsPostText
     */
    public List<ToolsPostText> filterToolsWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = toolsPostRepository.findToolsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getToolsType(),
                filter.getConditionTools(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolsPostText> posts = toolsPostRepository.findToolsPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, ToolsPostText> postMap = posts.stream()
                .collect(Collectors.toMap(ToolsPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Инструменты"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countToolsWithFilters(ProductFilterDTO filter) {
        return toolsPostRepository.countToolsPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getToolsType(),
                filter.getConditionTools()
        );
    }


    // ============================================================
    // БЫТОВАЯ ТЕХНИКА
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Бытовая техника"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории AppliancesPostText
     */
    public List<AppliancesPostText> filterAppliancesWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = appliancesPostRepository.findAppliancesPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAppliancesType(),
                filter.getConditionAppliances(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<AppliancesPostText> posts = appliancesPostRepository.findAppliancesPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, AppliancesPostText> postMap = posts.stream()
                .collect(Collectors.toMap(AppliancesPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Бытовая техника"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countAppliancesWithFilters(ProductFilterDTO filter) {
        return appliancesPostRepository.countAppliancesPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAppliancesType(),
                filter.getConditionAppliances()
        );
    }


    // ============================================================
    // ЭЛЕКТРОНИКА И ГАДЖЕТЫ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Электроника и гаджеты"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории ElectronicsPostText
     */
    public List<ElectronicsPostText> filterElectronicsWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = electronicsPostRepository.findElectronicsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getElectronicsType(),
                filter.getConditionElectronics(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<ElectronicsPostText> posts = electronicsPostRepository.findElectronicsPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, ElectronicsPostText> postMap = posts.stream()
                .collect(Collectors.toMap(ElectronicsPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Электроника и гаджеты"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countElectronicsWithFilters(ProductFilterDTO filter) {
        return electronicsPostRepository.countElectronicsPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getElectronicsType(),
                filter.getConditionElectronics()
        );
    }


    // ============================================================
    // ОДЕЖДА
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Одежда"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории ClothingPostText
     */
    public List<ClothingPostText> filterClothingWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = clothingPostRepository.findClothingPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getClothingType(),
                filter.getGenderClothing(),
                filter.getConditionClothing(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<ClothingPostText> posts = clothingPostRepository.findClothingPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, ClothingPostText> postMap = posts.stream()
                .collect(Collectors.toMap(ClothingPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Одежда"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countClothingWithFilters(ProductFilterDTO filter) {
        return clothingPostRepository.countClothingPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getClothingType(),
                filter.getGenderClothing(),
                filter.getConditionClothing()
        );
    }


    // ============================================================
    // ДЕТСКИЕ ТОВАРЫ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Детские товары"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории ChildrenPrPostText
     */
    public List<ChildrenPrPostText> filterChildrenPrWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = childrenPrPostRepository.findChildrenPrPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getGenderChildrenPr(),
                filter.getConditionChildrenPr(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChildrenPrPostText> posts = childrenPrPostRepository.findChildrenPrPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, ChildrenPrPostText> postMap = posts.stream()
                .collect(Collectors.toMap(ChildrenPrPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Детские товары"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countChildrenPrWithFilters(ProductFilterDTO filter) {
        return childrenPrPostRepository.countChildrenPrPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getGenderChildrenPr(),
                filter.getConditionChildrenPr()
        );
    }


    // ============================================================
    // СПОРТ И ОТДЫХ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Спорт и отдых"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории SportsPostText
     */
    public List<SportsPostText> filterSportsWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = sportsPostRepository.findSportsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getSportsKind(),
                filter.getSportsType(),
                filter.getGenderSports(),
                filter.getConditionSports(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<SportsPostText> posts = sportsPostRepository.findSportsPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, SportsPostText> postMap = posts.stream()
                .collect(Collectors.toMap(SportsPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Спорт и отдых"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countSportsWithFilters(ProductFilterDTO filter) {
        return sportsPostRepository.countSportsPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getSportsKind(),
                filter.getSportsType(),
                filter.getGenderSports(),
                filter.getConditionSports()
        );
    }


    // ============================================================
    // ПРОДУКТЫ ПИТАНИЯ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Продукты питания"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории FoodPostText
     */
    public List<FoodPostText> filterFoodWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = foodPostRepository.findFoodPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getFoodKind(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<FoodPostText> posts = foodPostRepository.findFoodPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, FoodPostText> postMap = posts.stream()
                .collect(Collectors.toMap(FoodPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Продукты питания"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countFoodWithFilters(ProductFilterDTO filter) {
        return foodPostRepository.countFoodPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getFoodKind()
        );
    }


    // ============================================================
    // ЗДОРОВЬЕ И МЕДИЦИНА
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Здоровье и медицина"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории HealthPostText
     */
    public List<HealthPostText> filterHealthWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = healthPostRepository.findHealthPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getHealthKind(),
                filter.getHealthType(),
                filter.getGenderHealth(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<HealthPostText> posts = healthPostRepository.findHealthPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, HealthPostText> postMap = posts.stream()
                .collect(Collectors.toMap(HealthPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Здоровье и медицина"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countHealthWithFilters(ProductFilterDTO filter) {
        return healthPostRepository.countHealthPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getHealthKind(),
                filter.getHealthType(),
                filter.getGenderHealth()
        );
    }


    // ============================================================
    // ЖИВОТНЫЕ И РАСТЕНИЯ
    // ============================================================

    /**
     * Выполняет фильтрацию постов категории "Животные и растения"
     * с учетом региона, города и сортировки.
     * <p>
     * Метод выполняет двухэтапную загрузку данных:
     * 1. Получение ID постов с применением всех фильтров и пагинацией
     * 2. Загрузка полных данных постов с изображениями по полученным ID
     * </p>
     *
     * @param filter   объект с параметрами фильтрации
     * @param pageable объект пагинации и сортировки
     * @return список постов категории AnimalsPostText
     */
    public List<AnimalsPostText> filterAnimalsWithRegionAndSort(
            ProductFilterDTO filter, Pageable pageable) {

        Slice<Long> idsSlice = animalsPostRepository.findAnimalsPostTextIdsWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAnimalsKind(),
                pageable
        );

        if (idsSlice.isEmpty()) {
            return Collections.emptyList();
        }

        List<AnimalsPostText> posts = animalsPostRepository.findAnimalsPostTextByIdsWithImages(idsSlice.getContent());

        Map<Long, AnimalsPostText> postMap = posts.stream()
                .collect(Collectors.toMap(AnimalsPostText::getId, Function.identity()));

        return idsSlice.getContent().stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество постов категории "Животные и растения"
     * с учетом всех примененных фильтров.
     *
     * @param filter объект с параметрами фильтрации
     * @return количество постов, соответствующих фильтрам
     */
    public long countAnimalsWithFilters(ProductFilterDTO filter) {
        return animalsPostRepository.countAnimalsPostTextWithFilters(
                filter.getSearch(),
                filter.getCategory(),
                filter.getRegion(),
                filter.getCity(),
                filter.getAnimalsKind()
        );
    }

}