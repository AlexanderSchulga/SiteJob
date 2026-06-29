package jobforsite.sitejob.module.shared.services.cachrateservice;

import jobforsite.sitejob.module.product.model.PostText;
import jobforsite.sitejob.module.resume.model.Resume;
import jobforsite.sitejob.module.resume.repository.PostResumeRepository;
import jobforsite.sitejob.module.service.model.PostTextJob;
import jobforsite.sitejob.module.vacancy.model.Vacancy;
import jobforsite.sitejob.module.product.repository.PostRepo;
import jobforsite.sitejob.module.service.repository.PostRepoJob;
import jobforsite.sitejob.module.vacancy.repository.PostVacancyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Сервис для работы с кэшем заблокированных объявлений.
 * Обеспечивает кэширование постраничных результатов (Slice) и количества
 * для каждого типа сущностей: товары, услуги, вакансии, резюме.
 * <p>
 * Паттерн "два запроса": сначала получаем только ID с пагинацией и сортировкой,
 * затем загружаем полные сущности с коллекциями по этим ID, сохраняя порядок.
 * </p>
 */
@RequiredArgsConstructor
@Service
public class BlockedPostsCacheService {

    private static final Logger log = LoggerFactory.getLogger(BlockedPostsCacheService.class);

    private final PostRepo postRepo;
    private final PostRepoJob postRepoJob;
    private final PostVacancyRepository postVacancyRepository;
    private final PostResumeRepository postResumeRepository;

    // ========== ТОВАРЫ ==========

    /**
     * Возвращает Slice заблокированных товаров с поддержкой поиска и пагинации.
     * Результат кэшируется в "blockedProductsSlice".
     *
     * @param page        номер страницы (начиная с 0)
     * @param size        размер страницы
     * @param searchQuery поисковый запрос (может быть null)
     * @param searchType  тип поиска: "title", "nameuse", "all"
     * @return Slice с товарами
     */
    @Cacheable(value = "blockedProductsSlice",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public Slice<PostText> getBlockedProductsSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Cache miss for blocked products slice: page={}, size={}, searchQuery={}, searchType={}",
                page, size, searchQuery, searchType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blockedAt"));
        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        // 1. Получаем только ID с пагинацией
        Slice<Long> idSlice;

        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idSlice = postRepo.searchBlockedProductIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idSlice = postRepo.searchBlockedProductIdsByUserName(normalizedQuery, pageable);
                    break;
                default:
                    idSlice = postRepo.searchBlockedProductIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idSlice = postRepo.findBlockedProductIds(pageable);
        }

        // Если нет ID, возвращаем пустой Slice
        if (idSlice.isEmpty()) {
            return idSlice.map(id -> null);
        }

        // 2. Загружаем полные данные с коллекциями по ID
        List<PostText> posts = postRepo.findPostsByIdsWithImages(idSlice.getContent());

        // Сохраняем порядок из первого запроса
        posts.sort((a, b) -> {
            int indexA = idSlice.getContent().indexOf(a.getId());
            int indexB = idSlice.getContent().indexOf(b.getId());
            return Integer.compare(indexA, indexB);
        });

        return new SliceImpl<>(posts, pageable, idSlice.hasNext());
    }

    // ========== УСЛУГИ ==========

    /**
     * Возвращает Slice заблокированных услуг с поддержкой поиска и пагинации.
     * Результат кэшируется в "blockedServicesSlice".
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return Slice с услугами
     */
    @Cacheable(value = "blockedServicesSlice",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public Slice<PostTextJob> getBlockedServicesSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Cache miss for blocked services slice: page={}, size={}, searchQuery={}, searchType={}",
                page, size, searchQuery, searchType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blockedAt"));
        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        Slice<Long> idSlice;

        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idSlice = postRepoJob.searchBlockedServiceIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idSlice = postRepoJob.searchBlockedServiceIdsByUserName(normalizedQuery, pageable);
                    break;
                default:
                    idSlice = postRepoJob.searchBlockedServiceIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idSlice = postRepoJob.findBlockedServiceIds(pageable);
        }

        if (idSlice.isEmpty()) {
            return idSlice.map(id -> null);
        }

        List<PostTextJob> services = postRepoJob.findPostsByIdsWithImages(idSlice.getContent());

        services.sort((a, b) -> {
            int indexA = idSlice.getContent().indexOf(a.getId());
            int indexB = idSlice.getContent().indexOf(b.getId());
            return Integer.compare(indexA, indexB);
        });

        return new SliceImpl<>(services, pageable, idSlice.hasNext());
    }

    // ========== ВАКАНСИИ ==========

    /**
     * Возвращает Slice заблокированных вакансий с поддержкой поиска и пагинации.
     * Результат кэшируется в "blockedVacanciesSlice".
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return Slice с вакансиями
     */
    @Cacheable(value = "blockedVacanciesSlice",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public Slice<Vacancy> getBlockedVacanciesSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Cache miss for blocked vacancies slice: page={}, size={}, searchQuery={}, searchType={}",
                page, size, searchQuery, searchType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blockedAt"));
        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        Slice<Long> idSlice;

        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idSlice = postVacancyRepository.searchBlockedVacancyIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idSlice = postVacancyRepository.searchBlockedVacancyIdsByUserName(normalizedQuery, pageable);
                    break;
                default:
                    idSlice = postVacancyRepository.searchBlockedVacancyIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idSlice = postVacancyRepository.findBlockedVacancyIds(pageable);
        }

        if (idSlice.isEmpty()) {
            return idSlice.map(id -> null);
        }

        List<Vacancy> vacancies = postVacancyRepository.findVacanciesByIdsWithImages(idSlice.getContent());

        vacancies.sort((a, b) -> {
            int indexA = idSlice.getContent().indexOf(a.getId());
            int indexB = idSlice.getContent().indexOf(b.getId());
            return Integer.compare(indexA, indexB);
        });

        return new SliceImpl<>(vacancies, pageable, idSlice.hasNext());
    }

    // ========== РЕЗЮМЕ ==========

    /**
     * Возвращает Slice заблокированных резюме с поддержкой поиска и пагинации.
     * Результат кэшируется в "blockedResumeSlice".
     *
     * @param page        номер страницы
     * @param size        размер страницы
     * @param searchQuery поисковый запрос
     * @param searchType  тип поиска
     * @return Slice с резюме
     */
    @Cacheable(value = "blockedResumeSlice",
            key = "#page + '_' + #size + '_' + (#searchQuery ?: 'nosearch') + '_' + (#searchType ?: 'all')",
            unless = "#result == null")
    public Slice<Resume> getBlockedResumeSlice(int page, int size, String searchQuery, String searchType) {
        log.debug("Cache miss for blocked resume slice: page={}, size={}, searchQuery={}, searchType={}",
                page, size, searchQuery, searchType);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "blockedAt"));
        boolean isSearchMode = searchQuery != null && !searchQuery.trim().isEmpty();

        Slice<Long> idSlice;

        if (isSearchMode) {
            String normalizedQuery = searchQuery.trim();
            switch (searchType != null ? searchType : "all") {
                case "title":
                    idSlice = postResumeRepository.searchBlockedResumeIdsByTitle(normalizedQuery, pageable);
                    break;
                case "nameuse":
                    idSlice = postResumeRepository.searchBlockedResumeIdsByUserName(normalizedQuery, pageable);
                    break;
                default:
                    idSlice = postResumeRepository.searchBlockedResumeIdsByTitleOrUserName(normalizedQuery, pageable);
                    break;
            }
        } else {
            idSlice = postResumeRepository.findBlockedResumeIds(pageable);
        }

        if (idSlice.isEmpty()) {
            return idSlice.map(id -> null);
        }

        List<Resume> res = postResumeRepository.findResumeByIdsWithImages(idSlice.getContent());

        res.sort((a, b) -> {
            int indexA = idSlice.getContent().indexOf(a.getId());
            int indexB = idSlice.getContent().indexOf(b.getId());
            return Integer.compare(indexA, indexB);
        });

        return new SliceImpl<>(res, pageable, idSlice.hasNext());
    }

    // ========== МЕТОДЫ ДЛЯ ПОДСЧЕТА ==========

    /**
     * Возвращает общее количество заблокированных товаров у активных пользователей.
     * Результат кэшируется в "blockedProductsCount".
     *
     * @return количество
     */
    @Cacheable(value = "blockedProductsCount", unless = "#result == null")
    public long getBlockedProductsCount() {
        log.debug("Cache miss for blocked products count");
        return postRepo.countByBlockedTrueAndUserActiveTrue();
    }

    /**
     * Возвращает общее количество заблокированных услуг у активных пользователей.
     *
     * @return количество
     */
    @Cacheable(value = "blockedServicesCount", unless = "#result == null")
    public long getBlockedServicesCount() {
        log.debug("Cache miss for blocked services count");
        return postRepoJob.countByBlockedTrueAndUserActiveTrue();
    }

    /**
     * Возвращает общее количество заблокированных вакансий у активных пользователей.
     *
     * @return количество
     */
    @Cacheable(value = "blockedVacanciesCount", unless = "#result == null")
    public long getBlockedVacanciesCount() {
        log.debug("Cache miss for blocked vacancies count");
        return postVacancyRepository.countByBlockedTrueAndUserActiveTrue();
    }

    /**
     * Возвращает общее количество заблокированных резюме у активных пользователей.
     *
     * @return количество
     */
    @Cacheable(value = "blockedResumeCount", unless = "#result == null")
    public long getBlockedResumeCount() {
        log.debug("Cache miss for blocked resume count");
        return postResumeRepository.countByBlockedTrueAndUserActiveTrue();
    }

    // ========== МЕТОДЫ ОЧИСТКИ КЭША ==========

    /**
     * Полностью очищает все кэши, связанные с заблокированными объявлениями.
     */
    @CacheEvict(value = {
            "blockedProductsSlice", "blockedServicesSlice", "blockedVacanciesSlice", "blockedResumeSlice",
            "blockedProductsCount", "blockedServicesCount", "blockedVacanciesCount", "blockedResumeCount"
    }, allEntries = true)
    public void clearAllBlockedCaches() {
        log.info("All blocked posts caches cleared");
    }

    /**
     * Очищает кэши только для товаров (список и количество).
     */
    @CacheEvict(value = {"blockedProductsSlice", "blockedProductsCount"}, allEntries = true)
    public void clearBlockedProductsCache() {
        log.info("Blocked products cache cleared");
    }

    /**
     * Очищает кэши только для услуг.
     */
    @CacheEvict(value = {"blockedServicesSlice", "blockedServicesCount"}, allEntries = true)
    public void clearBlockedServicesCache() {
        log.info("Blocked services cache cleared");
    }

    /**
     * Очищает кэши только для вакансий.
     */
    @CacheEvict(value = {"blockedVacanciesSlice", "blockedVacanciesCount"}, allEntries = true)
    public void clearBlockedVacanciesCache() {
        log.info("Blocked vacancies cache cleared");
    }

    /**
     * Очищает кэши только для резюме.
     */
    @CacheEvict(value = {"blockedResumeSlice", "blockedResumeCount"}, allEntries = true)
    public void clearBlockedResumeCache() {
        log.info("Blocked resume cache cleared");
    }

    /**
     * Вспомогательная реализация интерфейса {@link Slice} для ручного создания Slice
     * без зависимости от реализации Spring Data.
     *
     * @param <T> тип элементов в слайсе
     */
    private static class SliceImpl<T> implements Slice<T> {
        private final List<T> content;
        private final Pageable pageable;
        private final boolean hasNext;

        public SliceImpl(List<T> content, Pageable pageable, boolean hasNext) {
            this.content = content;
            this.pageable = pageable;
            this.hasNext = hasNext;
        }

        @Override
        public List<T> getContent() {
            return content;
        }

        @Override
        public Pageable getPageable() {
            return pageable;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public boolean hasPrevious() {
            return pageable.hasPrevious();
        }

        @Override
        public boolean isFirst() {
            return !pageable.hasPrevious();
        }

        @Override
        public boolean isLast() {
            return !hasNext;
        }

        @Override
        public Pageable nextPageable() {
            return hasNext ? pageable.next() : Pageable.unpaged();
        }

        @Override
        public Pageable previousPageable() {
            return pageable.hasPrevious() ? pageable.previousOrFirst() : Pageable.unpaged();
        }

        @Override
        public <U> Slice<U> map(Function<? super T, ? extends U> converter) {
            return null;
        }

        @Override
        public int getNumber() {
            return pageable.getPageNumber();
        }

        @Override
        public int getSize() {
            return pageable.getPageSize();
        }

        @Override
        public int getNumberOfElements() {
            return content.size();
        }

        @Override
        public boolean hasContent() {
            return !content.isEmpty();
        }

        @Override
        public Sort getSort() {
            return pageable.getSort();
        }

        @Override
        public Iterator<T> iterator() {
            return content.iterator();
        }
    }
}