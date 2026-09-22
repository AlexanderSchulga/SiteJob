document.addEventListener('DOMContentLoaded', function() {
    console.log('Vacancy search script loaded');

    const searchInput = document.getElementById('searchInput');
    const searchButton = document.getElementById('searchButton');

    if (!searchInput || !searchButton) {
        return;
    }

    // ===== ВОССТАНАВЛИВАЕМ ЗНАЧЕНИЕ ПОИСКА ИЗ URL =====
    const urlParams = new URLSearchParams(window.location.search);
    const searchQuery = urlParams.get('search');
    if (searchQuery) {
        searchInput.value = searchQuery;
    }

    // Поиск при нажатии кнопки лупы
    searchButton.addEventListener('click', function() {
        performSearch();
    });

    // Поиск при нажатии Enter
    searchInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            performSearch();
        }
    });

    function performSearch() {
        const query = searchInput.value.trim();
        if (query.length >= 2) {
            // Перенаправляем на ту же страницу с параметром поиска
            const currentUrl = new URL(window.location.href);
            const urlParams = new URLSearchParams(window.location.search);
            const currentExperience = urlParams.get('experience');
            const currentWorkformat = urlParams.get('workformat');

            // Устанавливаем поисковый запрос
            currentUrl.searchParams.set('search', query);

            if (currentExperience && currentExperience !== 'all') {
                currentUrl.searchParams.set('experience', currentExperience);
            }

            if (currentWorkformat && currentWorkformat !== 'all') {
                currentUrl.searchParams.set('workformat', currentWorkformat);
            }

            // НЕ удаляем другие параметры фильтрации
            // Категория и регион сохраняются автоматически из URL

            // Сбрасываем пагинацию
            currentUrl.searchParams.delete('page');

            window.location.href = currentUrl.toString();
        } else if (query.length === 0) {
            // Если поиск пустой - удаляем параметр поиска
            const currentUrl = new URL(window.location.href);
            currentUrl.searchParams.delete('search');
            currentUrl.searchParams.delete('page');
            window.location.href = currentUrl.toString();
        } else {
            alert('Введите хотя бы 2 символа для поиска');
        }
    }
});

// Кнопка очистки поиска
const clearSearchButton = document.getElementById('clearSearchButton');
if (clearSearchButton) {
    clearSearchButton.addEventListener('click', function() {
        const currentUrl = new URL(window.location.href);
        currentUrl.searchParams.delete('search');
        currentUrl.searchParams.delete('page');
        window.location.href = currentUrl.toString();
    });
}