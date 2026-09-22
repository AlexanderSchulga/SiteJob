document.addEventListener('DOMContentLoaded', function() {
    const priceSortSelect = document.getElementById('priceSortSelect');
    const priceSortOptions = document.getElementById('priceSortOptions');
    const selectedPriceSortText = document.getElementById('selectedPriceSortText');
    const priceSortOptionsList = document.querySelectorAll('#priceSortOptions .sort-option');

    // Функция для получения текущей сортировки по цене из URL
    function getCurrentPriceSort() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('priceSort') || 'none';
    }

    // Функция для определения типа страницы (товары/услуги/вакансии)
    function getPageType() {
        const path = window.location.pathname;
        if (path.includes('viewsonevac')) return 'vacancy';
        if (path.includes('viewsonejob')) return 'service';
        return 'product';
    }

    // Функция для получения правильного текста в зависимости от типа страницы
    function getPriceSortText(priceSortValue) {
        const pageType = getPageType();

        if (priceSortValue === 'none') return 'По умолчанию';

        if (pageType === 'vacancy') {
            return priceSortValue === 'price_asc'
                ? 'По возрастанию зарплаты'
                : 'По убыванию зарплаты';
        } else {
            return priceSortValue === 'price_asc'
                ? 'По возрастанию цены'
                : 'По убыванию цены';
        }
    }

    // Функция для обновления текста выбранной сортировки
    function updateSelectedPriceSortText() {
        const currentPriceSort = getCurrentPriceSort();
        const text = getPriceSortText(currentPriceSort);
        selectedPriceSortText.textContent = text;
    }

    // Инициализация текста при загрузке
    updateSelectedPriceSortText();

    // Обработчик открытия/закрытия dropdown
    if (priceSortSelect) {
        priceSortSelect.addEventListener('click', function(e) {
            e.stopPropagation();
            const isActive = priceSortOptions.classList.contains('active');

            // Закрываем все другие dropdown
            document.querySelectorAll('.category-options, .sort-options').forEach(menu => {
                if (menu !== priceSortOptions) {
                    menu.classList.remove('active');
                }
            });

            priceSortOptions.classList.toggle('show');
        });
    }

    // Обработчик выбора опции сортировки по цене
    priceSortOptionsList.forEach(option => {
        option.addEventListener('click', function() {
            const priceSortValue = this.getAttribute('data-price-sort');
            const priceSortText = this.textContent;

            // Обновляем отображаемый текст
            selectedPriceSortText.textContent = priceSortText;

            // Закрываем dropdown
            priceSortOptions.classList.remove('show');

            // Создаем URLSearchParams из текущих параметров
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

            // Если выбрана "По умолчанию", удаляем параметр priceSort
            if (priceSortValue === 'none') {
                // Не добавляем priceSort
            } else {
                newParams.set('priceSort', priceSortValue);
                // При сортировке по цене удаляем сортировку по дате
                newParams.delete('sort');
            }

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
        priceSortOptions.classList.remove('show');
    });

    // Предотвращаем закрытие при клике внутри dropdown
    if (priceSortOptions) {
        priceSortOptions.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    }
});