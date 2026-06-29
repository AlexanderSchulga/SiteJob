
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

    // Обработка для модального окна удаления аккаунта
    const deleteMyAccountModal = document.getElementById('deleteMyAccountModal');
    if (deleteMyAccountModal) {
        deleteMyAccountModal.addEventListener('show.bs.modal', function() {
            const passwordInput = document.getElementById('confirmPassword');
            if (passwordInput) {
                passwordInput.value = '';
            }
        });

        deleteMyAccountModal.addEventListener('shown.bs.modal', function() {
            const passwordInput = document.getElementById('confirmPassword');
            if (passwordInput) {
                passwordInput.focus();
            }
        });
    }

    // Валидация формы удаления аккаунта
    const deleteAccountForm = document.getElementById('deleteAccountForm');
    if (deleteAccountForm) {
        deleteAccountForm.addEventListener('submit', function(e) {
            const passwordInput = document.getElementById('confirmPassword');
            if (!passwordInput || passwordInput.value.trim() === '') {
                e.preventDefault();
                alert('Пожалуйста, введите пароль для подтверждения удаления аккаунта.');
                passwordInput.focus();
            }
        });
    }
});