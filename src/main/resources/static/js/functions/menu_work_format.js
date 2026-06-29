document.addEventListener('DOMContentLoaded', function() {
    const workformatDropdown = document.getElementById('workformatDropdown');
    const workformatSelect = document.getElementById('workformatSelect');
    const selectedWorkformatText = document.getElementById('selectedWorkformatText');
    const workformatOptions = document.getElementById('workformatOptions');

    if (!workformatDropdown || !workformatSelect || !selectedWorkformatText || !workformatOptions) {
        console.log('Элементы фильтра формата не найдены');
        return;
    }

    // Функция для получения текущего формата из URL
    function getCurrentWorkformatFromUrl() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('workformat');
    }

    // Функция для обновления отображаемого текста формата
    function updateSelectedWorkformatText() {
        const currentWorkformat = getCurrentWorkformatFromUrl();

        if (currentWorkformat && currentWorkformat !== 'all') {
            const options = workformatOptions.querySelectorAll('.workformat-option');
            let found = false;
            options.forEach(option => {
                if (option.getAttribute('data-workformat') === currentWorkformat) {
                    selectedWorkformatText.textContent = option.textContent;
                    found = true;
                }
            });
            if (!found) {
                selectedWorkformatText.textContent = 'Все';
            }
        } else {
            selectedWorkformatText.textContent = 'Все';
        }
    }

    // Обновляем текст при загрузке
    updateSelectedWorkformatText();

    // Обработчик открытия/закрытия dropdown
    workformatSelect.addEventListener('click', function(e) {
        e.stopPropagation();
        workformatDropdown.classList.toggle('open');
    });

    // Обработчик выбора формата работы
    workformatOptions.addEventListener('click', function(e) {
        if (e.target.classList.contains('workformat-option')) {
            const selectedWorkformat = e.target.getAttribute('data-workformat');
            const selectedText = e.target.textContent;

            // Обновляем текст выбранного формата
            selectedWorkformatText.textContent = selectedText;

            // Закрываем dropdown
            workformatDropdown.classList.remove('open');

            // Фильтруем резюме по формату
            filterResumesByWorkformat(selectedWorkformat);
        }
    });

    // Закрытие dropdown при клике вне его
    document.addEventListener('click', function(e) {
        if (!workformatDropdown.contains(e.target)) {
            workformatDropdown.classList.remove('open');
        }
    });

    // Закрытие dropdown при нажатии Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            workformatDropdown.classList.remove('open');
        }
    });

    // Функция фильтрации резюме по формату работы
    function filterResumesByWorkformat(currentWorkformat ) {

        // Получаем текущие параметры URL
        const currentUrl = new URL(window.location.href);
        const urlParams = new URLSearchParams(currentUrl.search);

        // Получаем текущие параметры
        const currentCategory = urlParams.get('category');
        const currentRegion = urlParams.get('region');
        const currentCity = urlParams.get('city');
        const currentSearch = urlParams.get('search');
        const currentSort = urlParams.get('sort');
        const currentPriceSort = urlParams.get('priceSort');
        const currentExperience = urlParams.get('experience');

        // Строим новый URL
        const newUrl = new URL(window.location.href);

        // Устанавливаем параметр формата работы
        if (!currentWorkformat  || currentWorkformat  === 'all') {
            newUrl.searchParams.delete('workformat');
        } else {
            newUrl.searchParams.set('workformat', currentWorkformat );
        }

        // Сохраняем остальные параметры
        if (currentCategory && currentCategory !== 'all') {
            newUrl.searchParams.set('category', currentCategory);
        }
        if (currentRegion && currentRegion !== 'all') {
            newUrl.searchParams.set('region', currentRegion);
        }
        if (currentCity && currentCity !== 'all' && currentCity !== 'null') {
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

        // Удаляем параметр страницы для сброса пагинации
        newUrl.searchParams.delete('page');


        // Перенаправляем на новый URL
        window.location.href = newUrl.toString();
    }
});