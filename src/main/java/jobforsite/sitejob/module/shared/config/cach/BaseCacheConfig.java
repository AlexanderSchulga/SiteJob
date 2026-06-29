package jobforsite.sitejob.module.shared.config.cach;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Абстрактная базовая конфигурация для управления кэшами на основе Caffeine.
 * <p>
 * Предоставляет утилитарные методы для создания и регистрации кэшей с различными параметрами:
 * <ul>
 *     <li>Стандартный кэш с TTL 5 минут и максимум 10 000 записей</li>
 *     <li>Кратковременные кэши (секунды) — для счетчиков и частых обновлений</li>
 *     <li>Долгоживущие кэши (часы) — для статических и редко меняющихся данных</li>
 *     <li>Полностью кастомные кэши</li>
 * </ul>
 * </p>
 * <p>
 * Все кэши по умолчанию включают сбор статистики (recordStats).
 * </p>
 *
 * @author JobForSite Team
 * @see CaffeineCacheManager
 * @see Caffeine
 */
public abstract class BaseCacheConfig {


    /** Значение TTL по умолчанию (минуты) */
    protected static final long DEFAULT_EXPIRE_MINUTES = 5;

    /** Максимальный размер кэша по умолчанию (количество записей) */
    protected static final long DEFAULT_MAX_SIZE = 10000;

    /**
     * Создает и настраивает {@link CaffeineCacheManager} с базовой конфигурацией.
     * <p>
     * Базовый кэш использует:
     * <ul>
     *     <li>Время жизни записи: {@value #DEFAULT_EXPIRE_MINUTES} минут</li>
     *     <li>Максимальный размер: {@value #DEFAULT_MAX_SIZE} записей</li>
     *     <li>Сбор статистики: включен</li>
     * </ul>
     * </p>
     *
     * @param cacheNames имена кэшей, которые будут управляться менеджером
     * @param customizer функция для дополнительной настройки менеджера (может быть null)
     * @return настроенный экземпляр {@link CaffeineCacheManager}
     */
    protected CaffeineCacheManager createCacheManager(String[] cacheNames, Consumer<CaffeineCacheManager> customizer) {

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(cacheNames);

        // Базовая конфигурация Caffeine
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(DEFAULT_MAX_SIZE)
                .recordStats());


        // Применяем кастомную настройку если есть
        if (customizer != null) {
            customizer.accept(cacheManager);
        }

        return cacheManager;
    }

    /**
     * Регистрирует кастомный кэш с указанными временем жизни (в минутах) и максимальным размером.
     *
     * @param cacheManager   менеджер кэшей, в который регистрируется новый кэш
     * @param name           имя кэша (должно быть уникальным)
     * @param expireMinutes  время жизни записи в минутах
     * @param maxSize        максимальное количество записей в кэше
     */
    protected void registerCustomCache(CaffeineCacheManager cacheManager, String name,
                                       long expireMinutes, long maxSize) {

        cacheManager.registerCustomCache(name, Caffeine.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .recordStats()
                .build());

    }

    /**
     * Регистрирует кастомный кэш с указанными временем жизни (в секундах) и максимальным размером.
     * <p>
     * Полезно для кэшей, требующих TTL меньше минуты.
     * </p>
     *
     * @param cacheManager   менеджер кэшей
     * @param name           имя кэша
     * @param expireSeconds  время жизни записи в секундах
     * @param maxSize        максимальное количество записей
     */
    protected void registerCustomCacheSeconds(CaffeineCacheManager cacheManager, String name,
                                              long expireSeconds, long maxSize) {

        cacheManager.registerCustomCache(name, Caffeine.newBuilder()
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .recordStats()
                .build());

    }

    /**
     * Регистрирует кастомный кэш с параметрами по умолчанию.
     * <p>
     * Использует {@value #DEFAULT_EXPIRE_MINUTES} минут TTL и {@value #DEFAULT_MAX_SIZE} записей.
     * </p>
     *
     * @param cacheManager менеджер кэшей
     * @param name         имя кэша
     */
    protected void registerCustomCache(CaffeineCacheManager cacheManager, String name) {

        registerCustomCache(cacheManager, name, DEFAULT_EXPIRE_MINUTES, DEFAULT_MAX_SIZE);
    }

    /**
     * Регистрирует кратковременный кэш (для счетчиков, частых обновлений).
     * <p>
     * Рекомендуемый TTL: 10-60 секунд.
     * </p>
     *
     * @param cacheManager    менеджер кэшей
     * @param name            имя кэша
     * @param expireSeconds   время жизни записи в секундах (обычно небольшое)
     * @param maxSize         максимальное количество записей
     */
    protected void registerShortLivedCache(CaffeineCacheManager cacheManager, String name,
                                           long expireSeconds, long maxSize) {
        registerCustomCacheSeconds(cacheManager, name, expireSeconds, maxSize);
    }

    /**
     * Регистрирует долгоживущий кэш (для статики, редко меняющихся данных).
     * <p>
     * Рекомендуемый TTL: 1-24 часа.
     * </p>
     *
     * @param cacheManager  менеджер кэшей
     * @param name          имя кэша
     * @param expireHours   время жизни записи в часах
     * @param maxSize       максимальное количество записей
     */
    protected void registerLongLivedCache(CaffeineCacheManager cacheManager, String name,
                                          long expireHours, long maxSize) {

        cacheManager.registerCustomCache(name, Caffeine.newBuilder()
                .expireAfterWrite(expireHours, TimeUnit.HOURS)
                .maximumSize(maxSize)
                .recordStats()
                .build());

    }
}