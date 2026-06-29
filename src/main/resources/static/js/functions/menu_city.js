// Обновление обработчика выбора города
document.addEventListener('DOMContentLoaded', function() {
    // Находим элементы выбора города
    const citySelect = document.getElementById('citySelect');
    const selectedCityText = document.getElementById('selectedCityText');
    const cityOptions = document.getElementById('cityOptions');
    const cityOptionsList = cityOptions.querySelectorAll('.city-option');

    // Проверяем, что элементы существуют
    if (!citySelect || !cityOptions) return;

    // Обработчик клика на выпадающем меню (для открытия/закрытия)
    citySelect.addEventListener('click', function(e) {
        e.stopPropagation(); // Предотвращаем всплытие
        cityOptions.classList.toggle('show');
    });

    // Обработчик выбора города
    cityOptionsList.forEach(option => {
        option.addEventListener('click', function(e) {
            e.stopPropagation();
            const cityValue = this.getAttribute('data-city');
            const cityText = this.textContent.trim();

            // Обновляем отображаемый текст
            selectedCityText.textContent = cityText;

            // Закрываем меню
            cityOptions.classList.remove('show');

            // Обновляем URL с новыми параметрами
            updateUrlWithCityFilter(cityValue);
        });
    });

    // Закрываем меню при клике вне его
    document.addEventListener('click', function() {
        cityOptions.classList.remove('show');
    });

    // Закрываем меню при нажатии Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            cityOptions.classList.remove('show');
        }
    });
});

function updateUrlWithCityFilter(cityValue) {
    const url = new URL(window.location.href);
    const params = new URLSearchParams(url.search);

    // Получаем параметры из URL, а не из неопределенной переменной urlParams
    const currentExperience = params.get('experience');
    const currentWorkformat = params.get('workformat');
    const currentCategory = params.get('category');
    const currentRegion = params.get('region');
    const currentSearch = params.get('search');
    const currentSort = params.get('sort');
    const currentPriceSort = params.get('priceSort');

    if (cityValue === 'all') {
        params.delete('city');
    } else {
        params.set('city', cityValue);
    }

    // Сохраняем остальные параметры (используем params)
    if (currentExperience && currentExperience !== 'all') {
        params.set('experience', currentExperience);
    }

    if (currentWorkformat && currentWorkformat !== 'all') {
        params.set('workformat', currentWorkformat);
    }

    if (currentCategory && currentCategory !== 'all') {
        params.set('category', currentCategory);
    }

    if (currentRegion && currentRegion !== 'all') {
        params.set('region', currentRegion);
    }

    if (currentSearch) {
        params.set('search', currentSearch);
    }

    if (currentSort && currentSort !== 'newest') {
        params.set('sort', currentSort);
    }

    if (currentPriceSort && currentPriceSort !== 'none') {
        params.set('priceSort', currentPriceSort);
    }

    // Сбрасываем страницу при смене фильтра
    params.delete('page');

    // Переходим на обновленный URL
    window.location.href = url.pathname + '?' + params.toString();
}