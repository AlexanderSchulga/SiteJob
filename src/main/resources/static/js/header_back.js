document.addEventListener('DOMContentLoaded', function() {
    // Функция проверки, нужно ли показывать кнопку "Назад"
    // Новая логика: показываем везде, КРОМЕ главной страницы
    function shouldShowBackButton() {
        const currentPath = window.location.pathname;

        // Список страниц, которые считаются "главными" - здесь кнопка НЕ нужна
        const mainPages = [
            '/',
            '/index',
            '/main'
        ];

        // На главных страницах - скрываем
        if (mainPages.includes(currentPath)) {
            return false;
        }

        // На всех остальных страницах - показываем кнопку "Назад"
        // Даже если нет истории, кнопка будет работать (перенаправлять на главную)
        return true;
    }

    // Обновление видимости всех кнопок "Назад"
    function updateBackButtons() {
        const backButtons = document.querySelectorAll('#backButtonMobile, #backButtonDesktop');
        const showBack = shouldShowBackButton();

        backButtons.forEach(btn => {
            const container = btn.closest('.nav-item');
            if (container) {
                container.style.display = showBack ? 'flex' : 'none';
            }
        });
    }

    // Безопасная навигация назад
    function safeGoBack(event) {
        event.preventDefault();

        // Проверяем, можно ли вернуться назад по внутренней истории
        if (window.history.length > 1 && document.referrer && document.referrer.indexOf(window.location.origin) === 0) {
            window.history.back();
        } else {
            // Если нет внутренней истории - переходим на главную
            window.location.href = '/';
        }
    }

    // Привязываем обработчики к кнопкам
    function bindBackButtons() {
        const backButtons = document.querySelectorAll('#backButtonMobile, #backButtonDesktop');
        backButtons.forEach(btn => {
            // Убираем старый обработчик, если есть
            const newBtn = btn.cloneNode(true);
            btn.parentNode.replaceChild(newBtn, btn);
            newBtn.addEventListener('click', safeGoBack);
        });
    }

    // Инициализация
    updateBackButtons();
    bindBackButtons();

    // Слушаем изменения истории (для SPA навигации)
    window.addEventListener('popstate', function() {
        setTimeout(updateBackButtons, 50);
        setTimeout(bindBackButtons, 50);
    });

    // Для переходов по ссылкам
    document.querySelectorAll('a').forEach(link => {
        // Игнорируем ссылки с target="_blank" и якоря
        if (link.getAttribute('target') === '_blank' || link.getAttribute('href')?.startsWith('#')) {
            return;
        }
        // Игнорируем саму кнопку "Назад"
        if (link.id === 'backButtonMobile' || link.id === 'backButtonDesktop') {
            return;
        }

        link.addEventListener('click', function() {
            setTimeout(updateBackButtons, 150);
            setTimeout(bindBackButtons, 150);
        });
    });

    // Также обновляем при загрузке страницы из кеша
    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            updateBackButtons();
            bindBackButtons();
        }
    });
});