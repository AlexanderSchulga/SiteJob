document.addEventListener('DOMContentLoaded', function() {
    const experienceDropdown = document.getElementById('experienceDropdown');
    const experienceSelect = document.getElementById('experienceSelect');
    const selectedExperienceText = document.getElementById('selectedExperienceText');
    const experienceOptions = document.getElementById('experienceOptions');


    if (!experienceDropdown || !experienceSelect || !selectedExperienceText || !experienceOptions) {
        console.log('Элементы фильтра опыта не найдены');
        return;
    }

    // Обработчик открытия/закрытия dropdown
    experienceSelect.addEventListener('click', function(e) {
        e.stopPropagation();
        experienceDropdown.classList.toggle('open');
    });

    // Обработчик выбора опыта
    experienceOptions.addEventListener('click', function(e) {
        if (e.target.classList.contains('experience-option')) {
            const selectedExperience = e.target.getAttribute('data-experience');
            const selectedText = e.target.textContent;

            // Обновляем текст выбранного опыта
            selectedExperienceText.textContent = selectedText;

            // Закрываем dropdown
            experienceDropdown.classList.remove('open');

            // Фильтруем резюме по опыту
            filterVacancyByExperience(selectedExperience);
        }
    });

    // Закрытие dropdown при клике вне его
    document.addEventListener('click', function(e) {
        if (!experienceDropdown.contains(e.target)) {
            experienceDropdown.classList.remove('open');
        }
    });

    // Закрытие dropdown при нажатии Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            experienceDropdown.classList.remove('open');
        }
    });


    // Функция фильтрации по опыту работы
    function filterVacancyByExperience(experience) {

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
        const currentWorkformat = urlParams.get('workformat');

        // Строим новый URL
        const newUrl = new URL(window.location.href);

        // Устанавливаем параметр опыта
        if (experience === 'all') {
            newUrl.searchParams.delete('experience');
        } else {
            newUrl.searchParams.set('experience', experience);
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
        if (currentSort) {
            newUrl.searchParams.set('sort', currentSort);
        }

        if (currentPriceSort && currentPriceSort !== 'none') {
            newUrl.searchParams.set('priceSort', currentPriceSort);
        } else if (currentPriceSort === 'none') {
            newUrl.searchParams.delete('priceSort');
        }

        if (currentWorkformat && currentWorkformat !== 'all') {
            newUrl.searchParams.set('workformat', currentWorkformat );
        }

        // Удаляем параметр страницы для сброса пагинации
        newUrl.searchParams.delete('page');

        // Перенаправляем на новый URL
        window.location.href = newUrl.toString();
    }
});