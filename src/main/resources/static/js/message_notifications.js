let checkInterval;
let isPageActive = true;
let lastCheckTimestamp = Date.now();

document.addEventListener('DOMContentLoaded', function() {
    // Запускаем проверку уведомлений
    startMessageNotifications();

    // Сбрасываем уведомления при заходе на страницу сообщений
    checkIfOnMessagesPage();

    // Отслеживаем изменение видимости страницы
    document.addEventListener('visibilitychange', function() {
        isPageActive = !document.hidden;
        if (isPageActive) {
            // Если страница стала активной, проверяем уведомления
            checkUnreadMessages();
            // И проверяем, не на странице ли мы сообщений
            checkIfOnMessagesPage();
        }
    });
});

function startMessageNotifications() {
    // Проверяем сразу при загрузке
    checkUnreadMessages();

    // Устанавливаем интервал проверки каждые 30 секунд
    checkInterval = setInterval(checkUnreadMessages, 30000);
}

function checkUnreadMessages() {
    // Делаем AJAX запрос для проверки наличия непрочитанных сообщений
    fetch('/user/notifications_message/count', {
        method: 'GET',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                updateNotificationBell(data.count);
            }
        })
        .catch(error => {
            console.log('Error checking messages');
        });
}

function updateNotificationBell(count) {
    // Мобильные элементы
    const mobileBell = document.getElementById('userNotificationBell');


    // Десктопные элементы
    const desktopBell = document.getElementById('userNotificationBellDesktop');


    // Обновляем мобильную версию
    if (mobileBell) {
        if (count > 0) {
            mobileBell.style.display = 'inline-block';
            mobileBell.classList.add('notification-pulse-active');
        } else {
            mobileBell.style.display = 'none';
            mobileBell.classList.remove('notification-pulse-active');
        }
    }

    // Обновляем десктопную версию
    if (desktopBell) {
        if (count > 0) {
            desktopBell.style.display = 'inline-block';
            desktopBell.classList.add('notification-pulse-active');

        } else {
            desktopBell.style.display = 'none';
            desktopBell.classList.remove('notification-pulse-active');

        }
    }

    // Обновляем сайдбар, если он существует
    if (window.mainSidebar) {
        window.mainSidebar.updateMessageCount(count);
    }
}

function checkIfOnMessagesPage() {
    // Проверяем, находимся ли мы на странице сообщений
    if (window.location.pathname === '/user/notifications_message') {
        resetNotifications();
    }
}

function resetNotifications() {
    // Мобильные элементы
    const mobileBell = document.getElementById('userNotificationBell');


    // Десктопные элементы
    const desktopBell = document.getElementById('userNotificationBellDesktop');


    // Скрываем мобильные уведомления
    if (mobileBell) {
        mobileBell.style.display = 'none';
        mobileBell.classList.remove('notification-pulse-active');
    }


    // Скрываем десктопные уведомления
    if (desktopBell) {
        desktopBell.style.display = 'none';
        desktopBell.classList.remove('notification-pulse-active');
    }

    // Обновляем сайдбар
    if (window.mainSidebar) {
        window.mainSidebar.updateMessageCount(0);
    }

    // Отправляем запрос на сервер для сброса уведомлений
    fetch('/user/notifications_message/mark-all-read', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    }).catch(error => console.log('Error marking messages as read'));
}

// Останавливаем интервал при уходе со страницы
window.addEventListener('beforeunload', function() {
    if (checkInterval) {
        clearInterval(checkInterval);
    }
});