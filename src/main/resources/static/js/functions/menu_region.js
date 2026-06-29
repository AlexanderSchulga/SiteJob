document.addEventListener('DOMContentLoaded', function() {
    const regionDropdown = document.getElementById('regionDropdown');
    if (!regionDropdown) return;

    const regionSelect = document.getElementById('regionSelect');
    const selectedRegionText = document.getElementById('selectedRegionText');
    const regionOptions = document.getElementById('regionOptions');
    const regionOptionsList = regionOptions.querySelectorAll('.region-option');

    // Функция для получения текущего региона из URL
    function getCurrentRegionFromUrl() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('region');
    }

    // Функция для обновления отображаемого текста региона
    function updateSelectedRegionText() {
        const currentRegion = getCurrentRegionFromUrl();

        if (currentRegion && currentRegion !== 'all') {
            // Ищем опцию с таким data-region
            let found = false;
            regionOptionsList.forEach(option => {
                if (option.getAttribute('data-region') === currentRegion) {
                    selectedRegionText.textContent = option.textContent;
                    found = true;
                }
            });
            if (!found) {
                selectedRegionText.textContent = 'Все регионы';
            }
        } else {
            selectedRegionText.textContent = 'Все регионы';
        }
    }

    // Обновляем текст при загрузке
    updateSelectedRegionText();

    // Открытие/закрытие выпадающего списка
    regionSelect.addEventListener('click', function(e) {
        e.stopPropagation();
        regionOptions.classList.toggle('show');
    });

    // Обработка выбора региона
    regionOptionsList.forEach(option => {
        option.addEventListener('click', function() {
            const region = this.getAttribute('data-region');
            const regionText = this.textContent;

            selectedRegionText.textContent = regionText;
            regionOptions.classList.remove('show');

            // Получаем текущие параметры URL
            const currentUrl = new URL(window.location.href);
            const urlParams = new URLSearchParams(currentUrl.search);

            // Получаем текущую категорию, город, поиск, сортировки
            const currentCategory = urlParams.get('category');
            const currentCity = urlParams.get('city');
            const currentSearch = urlParams.get('search');
            const currentSort = urlParams.get('sort');
            const currentPriceSort = urlParams.get('priceSort');
            const currentExperience = urlParams.get('experience');
            const currentWorkformat = urlParams.get('workformat');

            // Строим новый URL
            const url = new URL(window.location.href);

            if (region === 'all') {
                url.searchParams.delete('region');
                // При сбросе региона нужно также сбросить город
                url.searchParams.delete('city');
            } else {
                url.searchParams.set('region', region);
                // При смене региона нужно сбросить город, так как города разные
                url.searchParams.delete('city');
            }

            // Сохраняем остальные параметры
            if (currentCategory && currentCategory !== 'all') {
                url.searchParams.set('category', currentCategory);
            }
            if (currentCity && currentCity !== 'all' && region === getCurrentRegionFromUrl()) {
                // Сохраняем город только если регион не менялся
                url.searchParams.set('city', currentCity);
            }
            if (currentSearch) {
                url.searchParams.set('search', currentSearch);
            }
            if (currentSort && currentSort !== 'newest') {
                url.searchParams.set('sort', currentSort);
            }
            if (currentPriceSort && currentPriceSort !== 'none') {
                url.searchParams.set('priceSort', currentPriceSort);
            }

            if (currentExperience && currentExperience !== 'all') {
                url.searchParams.set('experience', currentExperience);
            }

            if (currentWorkformat && currentWorkformat !== 'all') {
                url.searchParams.set('workformat', currentWorkformat );
            }

            // Сбрасываем пагинацию
            url.searchParams.delete('page');

            window.location.href = url.toString();
        });
    });

    // Закрытие выпадающего списка при клике вне его
    document.addEventListener('click', function(e) {
        if (!regionDropdown.contains(e.target)) {
            regionOptions.classList.remove('show');
        }
    });

    // Закрытие списка по клавише Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            regionOptions.classList.remove('show');
        }
    });
});