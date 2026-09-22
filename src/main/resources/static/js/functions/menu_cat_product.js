document.addEventListener('DOMContentLoaded', function() {
    const categoryDropdown = document.getElementById('categoryDropdown');
    const categorySelect = document.getElementById('categorySelect');
    const selectedCategoryText = document.getElementById('selectedCategoryText');
    const categoryOptions = document.getElementById('categoryOptions');
    const categoryOptionsList = categoryOptions.querySelectorAll('.category-option');

    // Функция для получения текущей категории из URL
    function getCurrentCategoryFromUrl() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('category');
    }

    // Функция для обновления отображаемого текста категории
    function updateSelectedCategoryText() {
        const currentCategory = getCurrentCategoryFromUrl();

        if (currentCategory && currentCategory !== 'all') {
            // Ищем опцию с таким data-category
            let found = false;
            categoryOptionsList.forEach(option => {
                if (option.getAttribute('data-category') === currentCategory) {
                    selectedCategoryText.textContent = option.textContent;
                    found = true;
                }
            });
            if (!found) {
                selectedCategoryText.textContent = 'Все категории';
            }
        } else {
            selectedCategoryText.textContent = 'Все категории';
        }
    }

    // Обновляем текст при загрузке
    updateSelectedCategoryText();

    // Обработчик открытия/закрытия dropdown
    categorySelect.addEventListener('click', function(e) {
        e.stopPropagation();
        categoryOptions.classList.toggle('show');
    });

    // Обработчик выбора категории
    categoryOptions.addEventListener('click', function(e) {
        if (e.target.classList.contains('category-option')) {
            const selectedCategory = e.target.getAttribute('data-category');
            const selectedText = e.target.textContent;

            // Обновляем текст выбранной категории
            selectedCategoryText.textContent = selectedText;

            // Закрываем dropdown
            categoryOptions.classList.remove('show');

            // Фильтруем
            filterPostsByCategory(selectedCategory);
        }
    });

    // Закрытие dropdown при клике вне его
    document.addEventListener('click', function(e) {
        if (!categoryDropdown.contains(e.target)) {
            categoryOptions.classList.remove('show');
        }
    });

    // Закрытие dropdown при нажатии Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            categoryOptions.classList.remove('show');
        }
    });

    // Функция фильтрации по категориям
    function filterPostsByCategory(category) {
        // Получаем текущие параметры URL
        const currentUrl = new URL(window.location.href);
        const urlParams = new URLSearchParams(currentUrl.search);

        // Получаем текущие параметры
        const currentRegion = urlParams.get('region');
        const currentCity = urlParams.get('city');
        const currentSearch = urlParams.get('search');
        const currentSort = urlParams.get('sort');
        const currentPriceSort = urlParams.get('priceSort');
        const currentExperience = urlParams.get('experience');
        const currentWorkformat = urlParams.get('workformat');

        // Строим новый URL
        const newUrl = new URL(window.location.href);

        // Устанавливаем категорию или удаляем параметр
        if (category === 'all') {
            newUrl.searchParams.delete('category');
        } else {
            newUrl.searchParams.set('category', category);
        }

        // =====СБРАСЫВАЕМ ВСЕ ПАРАМЕТРЫ ФИЛЬТРОВ ПРИ СМЕНЕ КАТЕГОРИИ =====
        // Список параметров фильтров, которые нужно очистить
        const filterParamsToRemove = [
            'propertyType', 'roomsCount', 'floorsApartment', 'floorsCount', 'floorsCountHouse',
            'hasElevator', 'apartmentBuildYear', 'houseBuildYear',
            'houseArea', 'landArea', 'distanceToCity', 'vehicleType', 'vehicleOrigin', 'vehicleYear',
            'materialUp', 'repairUp', 'materialHouse', 'repairHouse', 'distanceToCityHouse',
            'landAreaGarage', 'landAreaCommercial', 'mileage', 'numberOfOwners'
        ];
        filterParamsToRemove.forEach(param => newUrl.searchParams.delete(param));

        // Сохраняем остальные параметры
        if (currentRegion && currentRegion !== 'all') {
            newUrl.searchParams.set('region', currentRegion);
        }
        if (currentCity && currentCity !== 'all' && currentCity !== 'none' && currentCity !== 'other_none') {
            newUrl.searchParams.set('city', currentCity);
        } else if (currentCity === 'none' || currentCity === 'other_none') {
            newUrl.searchParams.set('city', currentCity);
        }
        if (currentSearch) {
            newUrl.searchParams.set('search', currentSearch);
        }
        if (currentSort && currentSort !== 'newest') {
            newUrl.searchParams.set('sort', currentSort);
        }
        if (currentPriceSort && currentPriceSort !== 'none') {
            newUrl.searchParams.set('priceSort', currentPriceSort);
        }

        if (currentExperience && currentExperience !== 'all') {
            newUrl.searchParams.set('experience', currentExperience);
        }

        if (currentWorkformat && currentWorkformat !== 'all') {
            newUrl.searchParams.set('workformat', currentWorkformat);
        }

        // Удаляем параметр страницы для сброса пагинации
        newUrl.searchParams.delete('page');

        // Перенаправляем на новый URL
        window.location.href = newUrl.toString();
    }
});