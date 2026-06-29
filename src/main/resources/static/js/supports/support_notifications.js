// Проверка статуса уведомлений поддержки
function checkSupportNotifications() {
    fetch('/api/support/notificationStatus')
        .then(response => response.json())
        .then(data => {
            const notificationBell = document.getElementById('supportNotificationBell');
            if (notificationBell) {
                if (data.hasAnyNotification) {
                    notificationBell.style.display = 'inline-block';

                    // Обновляем tooltip с информацией
                    let tooltipText = '';
                    if (data.userType === 'SUPER') {
                        const reasons = [];
                        if (data.hasUnreadMessages) reasons.push('Непрочитанные сообщения');
                        if (data.hasUnassignedTickets) reasons.push('Невзятые тикеты');
                        tooltipText = 'SUPER: ' + reasons.join(', ');
                    } else {
                        if (data.hasUnreadSuperReplies) {
                            tooltipText = 'Есть новые ответы от поддержки';
                        }
                    }

                    if (tooltipText) {
                        notificationBell.setAttribute('title', tooltipText);
                        notificationBell.setAttribute('data-bs-toggle', 'tooltip');
                        notificationBell.setAttribute('data-bs-placement', 'bottom');
                    }
                } else {
                    notificationBell.style.display = 'none';
                }
            }
        })
        .catch(() => console.log('Error checking notifications'));
}

// Запускаем каждые 30 секунд
document.addEventListener('DOMContentLoaded', function() {
    checkSupportNotifications();
    setInterval(checkSupportNotifications, 30000);
});