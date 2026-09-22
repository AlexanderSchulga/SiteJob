document.addEventListener('DOMContentLoaded', function() {
    const sortSelect = document.getElementById('sortSelect');
    const sortOptions = document.getElementById('sortOptions');
    const selectedSortText = document.getElementById('selectedSortText');
    const sortOptionsList = document.querySelectorAll('.sort-option');

    // Функция для получения текущего значения сортировки из URL
    function getCurrentSort() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('sort') || 'newest';
    }

    // Функция для обновления текста выбранной сортировки
    function updateSelectedSortText() {
        const currentSort = getCurrentSort();
        selectedSortText.textContent = currentSort === 'oldest' ? 'Сначала старые' : 'Сначала свежие';
    }

    // Инициализация текста при загрузке
    updateSelectedSortText();

    // Обработчик открытия/закрытия dropdown
    sortSelect.addEventListener('click', function(e) {
        e.stopPropagation();

        // Закрываем все другие dropdown
        document.querySelectorAll('.category-options, .sort-options, .region-options, .city-options').forEach(menu => {
            if (menu !== sortOptions) {
                menu.classList.remove('show');
            }
        });

        sortOptions.classList.toggle('show');
    });

    // Обработчик выбора опции сортировки
    sortOptionsList.forEach(option => {
        option.addEventListener('click', function() {
            const sortValue = this.getAttribute('data-sort');
            const sortText = this.textContent;

            // Обновляем отображаемый текст
            selectedSortText.textContent = sortText;

            // Закрываем dropdown
            sortOptions.classList.remove('show');

            // Получаем текущие параметры URL
            const urlParams = new URLSearchParams(window.location.search);
            const currentCategory = urlParams.get('category');
            const currentSearch = urlParams.get('search') || '';
            const currentRegion = urlParams.get('region');
            const currentCity = urlParams.get('city');
            const currentExperience = urlParams.get('experience');
            const currentWorkformat = urlParams.get('workformat');

            // Создаем новый объект URLSearchParams
            const newParams = new URLSearchParams();

            // Сохраняем поисковый запрос если есть
            if (currentSearch) {
                newParams.append('search', currentSearch);
            }

            // Сохраняем категорию если она указана И НЕ равна 'all'
            if (currentCategory && currentCategory !== 'all') {
                newParams.append('category', currentCategory);
            }

            // Сохраняем регион если он указан И НЕ равен 'all'
            if (currentRegion && currentRegion !== 'all') {
                newParams.append('region', currentRegion);
            }

            // Сохраняем город если он указан
            if (currentCity && currentCity !== 'all') {
                newParams.append('city', currentCity);
            }

            if (currentExperience && currentExperience !== 'all') {
                newParams.searchParams.set('experience', currentExperience);
            }

            if (currentWorkformat && currentWorkformat !== 'all') {
                newParams.searchParams.set('workformat', currentWorkformat );
            }

            // Сбрасываем сортировку по цене
            urlParams.delete('priceSort');

            // Всегда добавляем сортировку
            newParams.append('sort', sortValue);

            // Создаем новый URL
            const newUrl = window.location.pathname +
                (newParams.toString() ? '?' + newParams.toString() : '');

            window.location.href = newUrl;
        });
    });

    // Закрытие dropdown при клике вне его
    document.addEventListener('click', function() {
        sortOptions.classList.remove('show');
    });

    // Предотвращаем закрытие при клике внутри dropdown
    sortOptions.addEventListener('click', function(e) {
        e.stopPropagation();
    });
});