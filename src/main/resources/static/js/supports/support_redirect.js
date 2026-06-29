document.addEventListener('DOMContentLoaded', function() {
    // Автоматическое скрытие flash-сообщений
    const flashMessage = document.querySelector('.flash-message');
    if (flashMessage) {
        setTimeout(function() {
            flashMessage.style.opacity = '0';
            setTimeout(function() {
                flashMessage.remove();
            }, 300);
        }, 5000);
    }

    // Обработка кликов по уведомлениям
    const notificationLinks = document.querySelectorAll('.support-notification-link');
    notificationLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const ticketId = this.getAttribute('data-ticket-id');
            const replyId = this.getAttribute('data-reply-id');

            let url = `/message/support/${ticketId}`;
            if (replyId) {
                url += `?replyId=${replyId}`;
            }

            window.location.href = url;
        });
    });
});