document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form');
    const currentPassword = document.getElementById('currentPassword');
    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmPassword');

    // Функция проверки формата пароля (только английские буквы и цифры, мин 6 символов)
    function isValidPasswordFormat(password) {
        return /^[a-zA-Z0-9]{6,}$/.test(password);
    }

    // Функция для отображения ошибки под полем
    function showFieldError(field, message) {
        // Удаляем существующую ошибку для этого поля
        const existingError = document.getElementById(field.id + 'Error');
        if (existingError) existingError.remove();

        // Создаем новую ошибку
        const errorDiv = document.createElement('div');
        errorDiv.id = field.id + 'Error';
        errorDiv.className = 'text-danger mt-1';
        errorDiv.textContent = message;
        field.parentNode.parentNode.appendChild(errorDiv);

        // Подсвечиваем поле
        field.classList.add('is-invalid');
    }

    function clearFieldError(field) {
        const existingError = document.getElementById(field.id + 'Error');
        if (existingError) existingError.remove();
        field.classList.remove('is-invalid');
    }

    // Очистка ошибок при вводе
    newPassword.addEventListener('input', function() {
        clearFieldError(newPassword);
        // Также проверяем совпадение при вводе
        if (confirmPassword.value && this.value !== confirmPassword.value) {
            showFieldError(confirmPassword, 'Пароли не совпадают');
        } else if (confirmPassword.value && this.value === confirmPassword.value) {
            clearFieldError(confirmPassword);
        }

        // Проверка формата при вводе
        if (this.value.length > 0 && !isValidPasswordFormat(this.value)) {
            showFieldError(this, 'Пароль может содержать только английские буквы и цифры (минимум 6 символов)');
        } else if (this.value.length > 0 && isValidPasswordFormat(this.value)) {
            clearFieldError(this);
        }
    });

    confirmPassword.addEventListener('input', function() {
        clearFieldError(confirmPassword);
        if (newPassword.value !== this.value) {
            showFieldError(this, 'Пароли не совпадают');
        } else if (newPassword.value === this.value) {
            clearFieldError(confirmPassword);
        }
    });

    form.addEventListener('submit', function(e) {
        // Очищаем все ошибки перед проверкой
        clearFieldError(currentPassword);
        clearFieldError(newPassword);
        clearFieldError(confirmPassword);

        // Проверяем, заполнено ли хоть одно поле пароля
        const isCurrentFilled = currentPassword.value.trim() !== '';
        const isNewFilled = newPassword.value.trim() !== '';
        const isConfirmFilled = confirmPassword.value.trim() !== '';

        // Если заполнено хотя бы одно поле, проверяем все
        if (isCurrentFilled || isNewFilled || isConfirmFilled) {
            let hasError = false;

            if (!isCurrentFilled) {
                showFieldError(currentPassword, 'Для смены пароля введите текущий пароль');
                hasError = true;
            }

            if (!isNewFilled) {
                showFieldError(newPassword, 'Для смены пароля введите новый пароль');
                hasError = true;
            }

            if (!isConfirmFilled) {
                showFieldError(confirmPassword, 'Для смены пароля подтвердите новый пароль');
                hasError = true;
            }

            if (!hasError && newPassword.value.length < 6) {
                showFieldError(newPassword, 'Новый пароль должен содержать не менее 6 символов');
                hasError = true;
            }

            // НОВАЯ ПРОВЕРКА: формат пароля (только английские буквы и цифры)
            if (!hasError && !isValidPasswordFormat(newPassword.value)) {
                showFieldError(newPassword, 'Новый пароль может содержать только английские буквы и цифры (минимум 6 символов)');
                hasError = true;
            }

            if (!hasError && newPassword.value !== confirmPassword.value) {
                showFieldError(confirmPassword, 'Новый пароль и подтверждение не совпадают');
                hasError = true;
            }

            if (hasError) {
                e.preventDefault();
                return;
            }
        }
    });
});