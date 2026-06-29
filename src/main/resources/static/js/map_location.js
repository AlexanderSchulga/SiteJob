// Глобальные переменные для карты
let map;
let placemark;
let selectedLocation = null;

// Функция инициализации карты
function initMap() {
    console.log('Инициализация карты...');

    try {
        // Создаем карту с центром в Старом Осколе
        map = new ymaps.Map('map', {
            center: [51.296659, 37.835734],
            zoom: 12,
            controls: ['zoomControl', 'fullscreenControl']
        });

        console.log('Карта создана успешно');

        // Обработчик клика по карте
        map.events.add('click', function (e) {
            const coords = e.get('coords');
            console.log('Выбраны координаты');

            if (placemark) {
                map.geoObjects.remove(placemark);
            }

            placemark = new ymaps.Placemark(coords, {
                hintContent: 'Выбранное местоположение',
                balloonContent: 'Загрузка адреса...'
            }, {
                preset: 'islands#redIcon',
                draggable: true
            });

            map.geoObjects.add(placemark);
            getAddress(coords);
            updateLocationStatus(true, coords);
        });

    } catch {
        console.log('Ошибка при создании карты');
    }
}

function getAddress(coords) {
    console.log('Запрос адреса для координат');

    const tempAddress = `Координаты: ${coords[0].toFixed(6)}, ${coords[1].toFixed(6)}`;
    updateLocationInput(tempAddress, coords);

    // OpenStreetMap Nominatim
    fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${coords[0]}&lon=${coords[1]}&zoom=16&addressdetails=1&accept-language=ru`)
        .then(response => response.json())
        .then(data => {
            let address = tempAddress;

            if (data && data.address) {
                address = formatRussianAddress(data.address);
                console.log('Получен адрес');

                // Обновляем балун метки
                if (placemark) {
                    placemark.properties.set('balloonContent', address);
                }
            }

            updateLocationInput(address, coords);
        })
        .catch(error => {
            console.log('Ошибка геокодирования');
            updateLocationInput(tempAddress, coords);
        });
}

function formatRussianAddress(addr) {
    const parts = [];

    if (addr.city) parts.push(addr.city);
    else if (addr.town) parts.push(addr.town);
    else if (addr.village) parts.push(addr.village);
    else if (addr.municipality) parts.push(addr.municipality);

    if (addr.road) {
        let streetPart = 'ул. ' + addr.road;
        if (addr.house_number) {
            streetPart += ', д. ' + addr.house_number;
        }
        parts.push(streetPart);
    }

    return parts.length > 0 ? parts.join(', ') : 'Адрес не определен';
}

function updateLocationInput(address, coords) {
    const locationInput = document.getElementById('locationInput');
    if (locationInput) {
        locationInput.value = address;
    }

    selectedLocation = {
        coords: coords,
        address: address
    };
}

function updateLocationStatus(isSelected, coords) {
    const statusElement = document.getElementById('locationStatus');
    if (statusElement) {
        if (isSelected) {
            statusElement.innerHTML = `✅ Местоположение выбрано`;
            statusElement.className = 'form-text text-success';
        } else {
            statusElement.innerHTML = '❌ Местоположение не выбрано';
            statusElement.className = 'form-text text-warning';
        }
    }
}

function clearLocation() {
    if (placemark) {
        map.geoObjects.remove(placemark);
        placemark = null;
    }

    const locationInput = document.getElementById('locationInput');
    if (locationInput) {
        locationInput.value = '';
    }

    selectedLocation = null;
    updateLocationStatus(false);
}

// Функция проверки валидности местоположения
function isLocationValid() {
    const locationInput = document.getElementById('locationInput');
    const locationValue = locationInput ? locationInput.value.trim() : '';

    // Валидно, если есть текст в поле ИЛИ выбран location на карте
    return locationValue !== '' || selectedLocation !== null;
}

// Инициализация после загрузки страницы
document.addEventListener('DOMContentLoaded', function() {
    // Загружаем Яндекс.Карты
    loadYandexMaps().then(() => {
        console.log('Яндекс.Карты загружены, инициализируем карту...');
        initMap();
    }).catch(error => {
        console.log('Ошибка загрузки Яндекс.Карт');
        // Не показываем alert, чтобы не мешать пользователю
        console.log('Карты не загрузились, но ручной ввод адреса доступен');
    });

    // Получаем все формы на странице
    const forms = ['productForm', 'serviceForm'];

    forms.forEach(formId => {
        const form = document.getElementById(formId);
        if (form) {
            form.addEventListener('submit', function(e) {
                const locationInput = document.getElementById('locationInput');
                const locationValue = locationInput ? locationInput.value.trim() : '';

                // Проверяем: есть ли текст в поле ввода ИЛИ выбран ли location на карте
                if (locationValue === '' && selectedLocation === null) {
                    e.preventDefault();
                    alert('Пожалуйста, укажите местоположение: введите адрес вручную или выберите на карте');
                    return false;
                }

                // Если адрес введен вручную, но selectedLocation не создан - создаем его
                if (locationValue !== '' && selectedLocation === null) {
                    selectedLocation = {
                        coords: null,
                        address: locationValue,
                        isManual: true
                    };
                }

                return true;
            });
        }
    });

    // Отслеживаем ручной ввод в поле location
    const locationInput = document.getElementById('locationInput');
    if (locationInput) {
        locationInput.addEventListener('input', function() {
            const value = this.value.trim();
            if (value !== '') {
                // При ручном вводе создаем временный selectedLocation
                selectedLocation = {
                    coords: null,
                    address: value,
                    isManual: true
                };
                updateLocationStatus(true, null);
            } else if (value === '' && !placemark) {
                // Если поле пустое и нет метки на карте
                selectedLocation = null;
                updateLocationStatus(false);
            }
        });
    }

    // Валидация количества файлов
    const fileInput = document.getElementById('files');
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const files = e.target.files;
            if (files.length > 10) {
                alert('Можно загрузить не более 10 изображений');
                e.target.value = '';
            }
        });
    }
});