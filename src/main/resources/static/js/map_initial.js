// Инициализация карты для просмотра
ymaps.ready(function() {
    const locationText = "[[${postwo[0].location}]]";
    let coords = [51.296659, 37.835734]; // По умолчанию Старый Оскол

    // Пытаемся извлечь координаты из текста - исправленная версия
    const coordsMatch = locationText.match(/Координаты:\s*([\d.-]+),\s*([\d.-]+)/);
    if (coordsMatch && coordsMatch[1] && coordsMatch[2]) {
        coords = [parseFloat(coordsMatch[1]), parseFloat(coordsMatch[2])];
    } else {
        // Если координаты не найдены в тексте, пытаемся геокодировать адрес
        ymaps.geocode(locationText).then(function (res) {
            const firstGeoObject = res.geoObjects.get(0);
            if (firstGeoObject) {
                coords = firstGeoObject.geometry.getCoordinates();
                initMap(coords, locationText);
            } else {
                initMap(coords, locationText);
            }
        }).catch(function() {
            initMap(coords, locationText);
        });
        return;
    }

    initMap(coords, locationText);

    function initMap(coordinates, address) {
        const viewMap = new ymaps.Map('viewMap', {
            center: coordinates,
            zoom: 14,
            controls: ['zoomControl', 'fullscreenControl']
        });

        // Добавляем метку
        const placemark = new ymaps.Placemark(coordinates, {
            hintContent: 'Местоположение товара',
            balloonContent: address
        }, {
            preset: 'islands#blueIcon'
        });

        viewMap.geoObjects.add(placemark);
    }
});