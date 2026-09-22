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

            // Создаем новый объект URLSearchParams
            const newParams = new URLSearchParams();

            // Сохраняем ВСЕ существующие параметры из URL
            urlParams.forEach((value, key) => {
                // Пропускаем параметры сортировки, которые будем устанавливать заново
                if (key === 'sort' || key === 'priceSort' || key === 'page') {
                    return;
                }
                newParams.append(key, value);
            });

            // Если выбрана сортировка по дате, удаляем сортировку по цене
            newParams.delete('priceSort');

            // Всегда добавляем сортировку по дате
            newParams.append('sort', sortValue);

            // Сбрасываем пагинацию
            newParams.delete('page');

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