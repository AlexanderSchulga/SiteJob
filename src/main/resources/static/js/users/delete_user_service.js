// JavaScript для страницы черного списка
document.addEventListener('DOMContentLoaded', function() {
    // Обработка для всех модальных окон удаления
    const deleteModals = document.querySelectorAll('[id^="deleteModal"]');

    deleteModals.forEach(function(modal) {
        const input = modal.querySelector('#deleteConfirmation');
        const button = modal.querySelector('#confirmDeleteBtn');

        if (input && button) {
            // Скрываем поле подтверждения и убираем required
            input.style.display = 'none';
            input.removeAttribute('required');
            button.disabled = false;

            // Скрываем label подтверждения
            const label = modal.querySelector('label[for="deleteConfirmation"]');
            if (label) {
                label.style.display = 'none';
            }
        }
    });

    // Обработка для кнопок удаления из черного списка
    const removeBlacklistButtons = document.querySelectorAll('.btn-remove-blacklist');
    removeBlacklistButtons.forEach(function(button) {
        button.addEventListener('click', function() {
            const userId = this.getAttribute('data-user-id');
            const form = document.getElementById('removeBlacklistForm' + userId);
            if (form && confirm('Вы уверены, что хотите удалить пользователя из черного списка?')) {
                form.submit();
            }
        });
    });
});