// Обработчик для кнопок удаления из черного списка
document.addEventListener('DOMContentLoaded', function() {
    const removeBlacklistButtons = document.querySelectorAll('.btn-remove-blacklist');

    removeBlacklistButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();

            // Получаем ID пользователя из data-user-id
            const userId = this.getAttribute('data-user-id');

            if (userId && confirm('Удалить пользователя из черного списка?')) {
                const form = document.getElementById('removeBlacklistForm' + userId);
                if (form) {
                    form.submit();
                }
            }
        });
    });
});