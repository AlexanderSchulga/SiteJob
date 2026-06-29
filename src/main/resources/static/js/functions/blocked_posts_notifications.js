// Проверка заблокированных постов
function checkBlockedPostsNotifications() {
    fetch('/api/notifications/has-blocked-posts')
        .then(response => response.json())
        .then(data => {
            updateBlockedPostsUI(data);
        })
        .catch(() => {

        });
}

// Обновление UI на основе данных о заблокированных постах
function updateBlockedPostsUI(data) {
    const notificationBadge = document.getElementById('blockedPostsNotification');

    if (data.hasBlockedPosts && notificationBadge) {
        notificationBadge.style.display = 'inline-block';

        // Обновляем текст счета
        const countBadge = notificationBadge.querySelector('.badge');
        if (countBadge && data.blockedCount > 0) {
            countBadge.textContent = data.blockedCount;
        }
    } else if (notificationBadge) {
        notificationBadge.style.display = 'none';
    }
}

// Запускаем каждые 30 секунд
document.addEventListener('DOMContentLoaded', function() {
    checkBlockedPostsNotifications();
    setInterval(checkBlockedPostsNotifications, 30000);
});