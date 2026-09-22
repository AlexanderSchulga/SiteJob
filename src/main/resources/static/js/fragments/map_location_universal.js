/**
 * Универсальная инициализация карт
 */

// Хранилище экземпляров карт
const mapInstances = {};

// Функция инициализации карты
function initMap(containerId) {
    console.log('initMap called for:', containerId);

    // Проверяем, что контейнер существует
    const container = document.getElementById(containerId);
    if (!container) {
        console.warn('Container not found:', containerId);
        return;
    }

    // Проверяем, что карта уже не инициализирована
    if (mapInstances[containerId]) {
        console.log('Map already exists for:', containerId);
        return;
    }

    // Проверяем, что Яндекс.Карты загружены
    if (typeof ymaps === 'undefined' || !ymaps.Map) {
        console.warn('Yandex Maps not loaded yet, queue init for:', containerId);
        if (!window._mapQueue) {
            window._mapQueue = [];
        }
        window._mapQueue.push(containerId);
        return;
    }

    // Инициализируем карту
    try {
        // Получаем данные из data-атрибутов
        const inputId = container.dataset.inputId;
        const input = document.getElementById(inputId);
        const initialLocation = container.dataset.location || (input ? input.value : '');

        console.log('Initializing map:', containerId, 'with location:', initialLocation);

        // Создаем карту
        const map = new ymaps.Map(containerId, {
            center: [55.753994, 37.622093], // Москва по умолчанию
            zoom: 10,
            controls: ['zoomControl', 'geolocationControl']
        });

        // Добавляем поиск
        const searchControl = new ymaps.control.SearchControl({
            options: {
                float: 'right',
                provider: 'yandex#search',
                noPlacemark: false,
                resultsPerPage: 5
            }
        });
        map.controls.add(searchControl);

        // Обработчик выбора результата поиска
        searchControl.events.add('select', function(e) {
            const selection = e.get('selection');
            selection.get('result').then(function(result) {
                const coords = result.geometry.getCoordinates();
                const address = result.getAddressLine() || result.getDisplayName();

                if (input) {
                    input.value = address;
                }

                map.geoObjects.removeAll();

                const placemark = new ymaps.Placemark(coords, {
                    hintContent: address,
                    balloonContent: address
                });
                map.geoObjects.add(placemark);
            });
        });

        // Обработчик клика по карте
        map.events.add('click', function(e) {
            const coords = e.get('coords');
            ymaps.geocode(coords, {
                results: 1
            }).then(function(res) {
                const first = res.geoObjects.get(0);
                if (first) {
                    const address = first.getAddressLine() || first.getDisplayName();

                    if (input) {
                        input.value = address;
                    }

                    map.geoObjects.removeAll();

                    const placemark = new ymaps.Placemark(coords, {
                        hintContent: address,
                        balloonContent: address
                    });
                    map.geoObjects.add(placemark);
                }
            });
        });

        // Если есть начальное значение, ставим метку
        if (input && input.value && input.value.trim() !== '') {
            ymaps.geocode(input.value, {
                results: 1
            }).then(function(res) {
                const first = res.geoObjects.get(0);
                if (first) {
                    const coords = first.geometry.getCoordinates();
                    map.setCenter(coords, 12);

                    const placemark = new ymaps.Placemark(coords, {
                        hintContent: input.value,
                        balloonContent: input.value
                    });
                    map.geoObjects.add(placemark);
                }
            });
        }

        // Сохраняем экземпляр карты
        mapInstances[containerId] = map;
        console.log('Map initialized successfully:', containerId);

    } catch (error) {
        console.error('Error initializing map:', containerId, error);
    }
}

// Функция очистки локации
function clearLocation(mapId) {
    console.log('clearLocation called for:', mapId);

    const inputId = 'locationInput_' + mapId;
    const input = document.getElementById(inputId);
    if (input) {
        input.value = '';
    }

    const containerId = 'map_' + mapId;
    if (mapInstances[containerId]) {
        mapInstances[containerId].geoObjects.removeAll();
        console.log('Cleared markers for:', containerId);
    }
}

// Инициализация всех карт на странице
function initAllMaps() {
    console.log('initAllMaps called');

    // Проверяем, что Яндекс.Карты загружены
    if (typeof ymaps === 'undefined' || !ymaps.Map) {
        console.warn('Yandex Maps not loaded, waiting...');
        return;
    }

    // Находим все контейнеры карт
    const containers = document.querySelectorAll('.map-container');
    console.log('Found map containers:', containers.length);

    containers.forEach(function(container) {
        const containerId = container.id;
        if (containerId && containerId.startsWith('map_')) {
            if (!mapInstances[containerId]) {
                console.log('Initializing map for container:', containerId);
                initMap(containerId);
            }
        }
    });
}

// Загрузка Яндекс.Карт (исправленная версия)
function loadYandexMaps() {
    console.log('loadYandexMaps called');

    // Проверяем, уже загружены
    if (window.ymaps && ymaps.Map) {
        console.log('Yandex Maps already loaded');
        ymaps.ready(function() {
            initAllMaps();
        });
        return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
        try {
            // Удаляем старый скрипт, если есть
            const oldScript = document.querySelector('script[src*="api-maps.yandex.ru"]');
            if (oldScript) {
                oldScript.remove();
                console.log('Removed old Yandex Maps script');
            }

            // Загружаем новый скрипт
            const script = document.createElement('script');
            // Используем более стабильную версию API
            script.src = 'https://api-maps.yandex.ru/2.1/?lang=ru_RU&apikey=a29aa5a3-6b5d-4e6e-a513-27f390d21ae1';
            script.async = true;
            script.onload = function() {
                console.log('Yandex Maps script loaded');
                ymaps.ready(function() {
                    console.log('Yandex Maps ready');
                    initAllMaps();
                    resolve();
                });
            };
            script.onerror = function(error) {
                console.error('Failed to load Yandex Maps:', error);
                reject(error);
            };
            document.head.appendChild(script);

        } catch (error) {
            console.error('Error loading Yandex Maps:', error);
            reject(error);
        }
    });
}

// Функция для повторной инициализации (для AJAX)
function reinitMaps() {
    console.log('reinitMaps called');
    if (window.ymaps && ymaps.Map) {
        ymaps.ready(function() {
            initAllMaps();
        });
    } else {
        loadYandexMaps();
    }
}

// Инициализация при загрузке DOM
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, loading maps...');

    // Загружаем карты с задержкой
    setTimeout(function() {
        loadYandexMaps().catch(function(error) {
            console.error('Failed to load maps:', error);
            // Повторная попытка через 3 секунды
            setTimeout(function() {
                console.log('Retrying map load...');
                loadYandexMaps();
            }, 3000);
        });
    }, 500);
});

// Экспортируем функции в глобальный объект
window.initMap = initMap;
window.initAllMaps = initAllMaps;
window.clearLocation = clearLocation;
window.loadYandexMaps = loadYandexMaps;
window.reinitMaps = reinitMaps;
window.mapInstances = mapInstances;

console.log('map_location_universal.js loaded');