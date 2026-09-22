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
        return 'product'; // viewsone
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
                ? 'От возрастанию цены'
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

            console.log('Price sort selected:');

            // Обновляем отображаемый текст
            selectedPriceSortText.textContent = priceSortText;

            // Закрываем dropdown
            priceSortOptions.classList.remove('show');

            // Создаем URLSearchParams из текущих параметров
            const urlParams = new URLSearchParams(window.location.search);
            const currentExperience = urlParams.get('experience');
            const currentWorkformat = urlParams.get('workformat');


            // Если выбрана "По умолчанию", удаляем параметр priceSort
            if (priceSortValue === 'none') {
                urlParams.delete('priceSort');
            } else {
                // Иначе устанавливаем значение и сбрасываем сортировку по дате
                urlParams.set('priceSort', priceSortValue);

                // Сбрасываем сортировку по дате при выборе сортировки по цене
                urlParams.delete('sort'); // Удаляем сортировку по дате
            }

            if (currentExperience && currentExperience !== 'all') {
                urlParams.searchParams.set('experience', currentExperience);
            }

            if (currentWorkformat && currentWorkformat !== 'all') {
                urlParams.searchParams.set('workformat', currentWorkformat );
            }

            // Если сортировка по дате не указана, устанавливаем по умолчанию 'newest'
            // НЕ устанавливаем, чтобы была либо сортировка по дате, либо по цене

            // Создаем новый URL
            const newUrl = window.location.pathname +
                (urlParams.toString() ? '?' + urlParams.toString() : '');

            console.log('Applying price sort');

            // Переходим по новому URL
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