// Карта для товаров
let productMap;
let productPlacemark;

// Карта для услуг
let serviceMap;
let servicePlacemark;

// Карта для вакансий
let vacancyMap;
let vacancyPlacemark;

// Инициализация карты для товаров
function initProductMap() {
    const mapElement = document.getElementById('product_map');
    const locationInput = document.getElementById('product_locationInput');

    if (!mapElement || !locationInput) return;

    // Начальная локация из поля ввода
    const initialLocation = locationInput.value;

    productMap = new ymaps.Map('product_map', {
        center: initialLocation ? [51.296659, 37.835734] : [51.296659, 37.835734],
        zoom: 12,
        controls: ['zoomControl', 'fullscreenControl']
    });

    // Если есть начальная локация, отображаем её на карте
    if (initialLocation) {
        ymaps.geocode(initialLocation).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            if (firstGeoObject) {
                const coordinates = firstGeoObject.geometry.getCoordinates();
                productMap.setCenter(coordinates, 15);

                productPlacemark = new ymaps.Placemark(coordinates, {
                    balloonContent: initialLocation
                });
                productMap.geoObjects.add(productPlacemark);
            }
        });
    }

    // Обработка клика по карте
    productMap.events.add('click', function (e) {
        const coords = e.get('coords');

        // Удаляем старую метку
        if (productPlacemark) {
            productMap.geoObjects.remove(productPlacemark);
        }

        // Создаем новую метку
        productPlacemark = new ymaps.Placemark(coords, {
            balloonContent: 'Выбранная локация'
        });
        productMap.geoObjects.add(productPlacemark);

        // Получаем адрес по координатам
        ymaps.geocode(coords).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            const address = firstGeoObject.getAddressLine();
            locationInput.value = address;
        });
    });

    // Поиск по вводу адреса
    let searchTimeout;
    locationInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(function() {
            const address = locationInput.value;
            if (address.length > 3) {
                ymaps.geocode(address).then(function (res) {
                    const firstGeoObject = res.geoObjects.get(0);
                    if (firstGeoObject) {
                        const coordinates = firstGeoObject.geometry.getCoordinates();
                        productMap.setCenter(coordinates, 15);

                        // Удаляем старую метку
                        if (productPlacemark) {
                            productMap.geoObjects.remove(productPlacemark);
                        }

                        // Создаем новую метку
                        productPlacemark = new ymaps.Placemark(coordinates, {
                            balloonContent: address
                        });
                        productMap.geoObjects.add(productPlacemark);
                    }
                });
            }
        }, 1000);
    });
}

// Инициализация карты для услуг
function initServiceMap() {
    const mapElement = document.getElementById('service_map');
    const locationInput = document.getElementById('service_locationInput');
    const locationStatus = document.getElementById('service_locationStatus');

    if (!mapElement || !locationInput) return;

    // Начальная локация из поля ввода
    const initialLocation = locationInput.value;

    serviceMap = new ymaps.Map('service_map', {
        center: initialLocation ? [51.296659, 37.835734] : [51.296659, 37.835734],
        zoom: 12,
        controls: ['zoomControl', 'fullscreenControl']
    });

    // Если есть начальная локация, отображаем её на карте
    if (initialLocation) {
        ymaps.geocode(initialLocation).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            if (firstGeoObject) {
                const coordinates = firstGeoObject.geometry.getCoordinates();
                serviceMap.setCenter(coordinates, 15);

                servicePlacemark = new ymaps.Placemark(coordinates, {
                    balloonContent: initialLocation
                });
                serviceMap.geoObjects.add(servicePlacemark);

                if (locationStatus) {
                    locationStatus.style.display = 'block';
                }
            }
        });
    } else {
        if (locationStatus) {
            locationStatus.style.display = 'none';
        }
    }

    // Обработка клика по карте
    serviceMap.events.add('click', function (e) {
        const coords = e.get('coords');

        // Удаляем старую метку
        if (servicePlacemark) {
            serviceMap.geoObjects.remove(servicePlacemark);
        }

        // Создаем новую метку
        servicePlacemark = new ymaps.Placemark(coords, {
            balloonContent: 'Выбранная локация'
        });
        serviceMap.geoObjects.add(servicePlacemark);

        // Получаем адрес по координатам
        ymaps.geocode(coords).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            const address = firstGeoObject.getAddressLine();
            locationInput.value = address;

            if (locationStatus) {
                locationStatus.style.display = 'block';
            }
        });
    });
}

// Инициализация карты для вакансий
function initVacancyMap() {
    const mapElement = document.getElementById('vacancy_map');
    const locationInput = document.getElementById('vacancy_locationInput');
    const locationStatus = document.getElementById('vacancy_locationStatus');

    if (!mapElement || !locationInput) return;

    // Начальная локация из поля ввода
    const initialLocation = locationInput.value;

    vacancyMap = new ymaps.Map('vacancy_map', {
        center: initialLocation ? [51.296659, 37.835734] : [51.296659, 37.835734],
        zoom: 12,
        controls: ['zoomControl', 'fullscreenControl']
    });

    // Если есть начальная локация, отображаем её на карте
    if (initialLocation) {
        ymaps.geocode(initialLocation).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            if (firstGeoObject) {
                const coordinates = firstGeoObject.geometry.getCoordinates();
                vacancyMap.setCenter(coordinates, 15);

                vacancyPlacemark = new ymaps.Placemark(coordinates, {
                    balloonContent: initialLocation
                });
                vacancyMap.geoObjects.add(vacancyPlacemark);

                if (locationStatus) {
                    locationStatus.style.display = 'block';
                }
            }
        });
    } else {
        if (locationStatus) {
            locationStatus.style.display = 'none';
        }
    }

    // Обработка клика по карте
    vacancyMap.events.add('click', function (e) {
        const coords = e.get('coords');

        // Удаляем старую метку
        if (vacancyPlacemark) {
            vacancyMap.geoObjects.remove(vacancyPlacemark);
        }

        // Создаем новую метку
        vacancyPlacemark = new ymaps.Placemark(coords, {
            balloonContent: 'Выбранная локация'
        });
        vacancyMap.geoObjects.add(vacancyPlacemark);

        // Получаем адрес по координатам
        ymaps.geocode(coords).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            const address = firstGeoObject.getAddressLine();
            locationInput.value = address;

            if (locationStatus) {
                locationStatus.style.display = 'block';
            }
        });
    });

    // Поиск по вводу адреса для вакансий
    let searchTimeout;
    locationInput.addEventListener('input', function() {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(function() {
            const address = locationInput.value;
            if (address.length > 3) {
                ymaps.geocode(address).then(function (res) {
                    const firstGeoObject = res.geoObjects.get(0);
                    if (firstGeoObject) {
                        const coordinates = firstGeoObject.geometry.getCoordinates();
                        vacancyMap.setCenter(coordinates, 15);

                        // Удаляем старую метку
                        if (vacancyPlacemark) {
                            vacancyMap.geoObjects.remove(vacancyPlacemark);
                        }

                        // Создаем новую метку
                        vacancyPlacemark = new ymaps.Placemark(coordinates, {
                            balloonContent: address
                        });
                        vacancyMap.geoObjects.add(vacancyPlacemark);

                        if (locationStatus) {
                            locationStatus.style.display = 'block';
                        }
                    }
                });
            }
        }, 1000);
    });
}

// Очистка поля локации
function clearProductLocation() {
    const input = document.getElementById('product_locationInput');
    if (input) {
        input.value = '';
        if (productPlacemark) {
            productMap.geoObjects.remove(productPlacemark);
            productPlacemark = null;
        }
    }
}

function clearServiceLocation() {
    const input = document.getElementById('service_locationInput');
    const status = document.getElementById('service_locationStatus');
    if (input) {
        input.value = '';
        if (servicePlacemark) {
            serviceMap.geoObjects.remove(servicePlacemark);
            servicePlacemark = null;
        }
        if (status) {
            status.style.display = 'none';
        }
    }
}

function clearVacancyLocation() {
    const input = document.getElementById('vacancy_locationInput');
    const status = document.getElementById('vacancy_locationStatus');
    if (input) {
        input.value = '';
        if (vacancyPlacemark) {
            vacancyMap.geoObjects.remove(vacancyPlacemark);
            vacancyPlacemark = null;
        }
        if (status) {
            status.style.display = 'none';
        }
    }
}

// Инициализация всех карт после загрузки API Яндекса
ymaps.ready(function() {
    initProductMap();
    initServiceMap();
    initVacancyMap();
});