document.getElementById('registrationForm').addEventListener('submit', function(e) {
    const username = document.getElementById('username').value;
    const nameuse = document.getElementById('nameuse').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const passwordError = document.getElementById('passwordError');
    const passwordField = document.getElementById('password');

    // Убираем старые ошибки
    let existingError = document.getElementById('passwordFormatError');
    if (existingError) existingError.remove();

    let existingUsernameError = document.getElementById('usernameFormatError');
    if (existingUsernameError) existingUsernameError.remove();

    let existingNameuseError = document.getElementById('nameuseFormatError');
    if (existingNameuseError) existingNameuseError.remove();

    // ========== ПРОВЕРКА USERNAME ==========
    // Только английские буквы и цифры (без пробелов!)
    const usernamePattern = /^[a-zA-Z0-9]+$/;
    if (!usernamePattern.test(username)) {
        e.preventDefault();
        const errorDiv = document.createElement('div');
        errorDiv.id = 'usernameFormatError';
        errorDiv.className = 'text-danger mt-1';
        errorDiv.textContent = 'Логин может содержать только английские буквы и цифры (без пробелов)';
        document.getElementById('username').parentNode.appendChild(errorDiv);
        document.getElementById('username').focus();
        return;
    }

    // ========== ПРОВЕРКА NAMEUSE ==========
    // Английские, русские буквы, цифры и пробелы
    const nameusePattern = /^[a-zA-Zа-яА-ЯёЁ0-9\s]+$/;
    if (!nameusePattern.test(nameuse)) {
        e.preventDefault();
        const errorDiv = document.createElement('div');
        errorDiv.id = 'nameuseFormatError';
        errorDiv.className = 'text-danger mt-1';
        errorDiv.textContent = 'Имя пользователя может содержать только буквы (русские или английские), цифры и пробелы';
        document.getElementById('nameuse').parentNode.appendChild(errorDiv);
        document.getElementById('nameuse').focus();
        return;
    }

    // ========== ПРОВЕРКА ПАРОЛЯ ==========
    // Проверка длины пароля
    if (password.length < 6) {
        e.preventDefault();
        alert('Длина пароля должна быть не менее 6 символов');
        passwordField.focus();
        return;
    }

    // Проверка: только английские буквы и цифры
    const englishLettersAndNumbersOnly = /^[a-zA-Z0-9]+$/;
    if (!englishLettersAndNumbersOnly.test(password)) {
        e.preventDefault();
        const errorDiv = document.createElement('div');
        errorDiv.id = 'passwordFormatError';
        errorDiv.className = 'text-danger mt-1';
        errorDiv.textContent = 'Пароль может содержать только английские буквы и цифры';
        passwordField.parentNode.insertAdjacentElement('afterend', errorDiv);
        passwordField.focus();
        return;
    }

    // Проверка совпадения паролей
    if (password !== confirmPassword) {
        e.preventDefault();
        passwordError.style.display = 'block';
        document.getElementById('confirmPassword').focus();
    } else {
        passwordError.style.display = 'none';
    }
});

// ========== ПРОВЕРКА ПРИ ВВОДЕ (реального времени) ==========

// Проверка username при вводе
const usernameInput = document.getElementById('username');
if (usernameInput) {
    usernameInput.addEventListener('input', function() {
        const error = document.getElementById('usernameFormatError');
        if (error) error.remove();

        const usernamePattern = /^[a-zA-Z0-9]+$/;
        if (this.value.length > 0 && !usernamePattern.test(this.value)) {
            const errorDiv = document.createElement('div');
            errorDiv.id = 'usernameFormatError';
            errorDiv.className = 'text-danger mt-1';
            errorDiv.textContent = 'Логин может содержать только английские буквы и цифры (без пробелов)';
            this.parentNode.appendChild(errorDiv);
            this.classList.add('is-invalid');
        } else {
            this.classList.remove('is-invalid');
        }
    });
}

// Проверка nameuse при вводе
const nameuseInput = document.getElementById('nameuse');
if (nameuseInput) {
    nameuseInput.addEventListener('input', function() {
        const error = document.getElementById('nameuseFormatError');
        if (error) error.remove();

        const nameusePattern = /^[a-zA-Zа-яА-ЯёЁ0-9\s]+$/;
        if (this.value.length > 0 && !nameusePattern.test(this.value)) {
            const errorDiv = document.createElement('div');
            errorDiv.id = 'nameuseFormatError';
            errorDiv.className = 'text-danger mt-1';
            errorDiv.textContent = 'Имя может содержать только буквы (русские или английские), цифры и пробелы';
            this.parentNode.appendChild(errorDiv);
            this.classList.add('is-invalid');
        } else {
            this.classList.remove('is-invalid');
        }
    });
}

// Проверка пароля при вводе
const confirmPasswordInput = document.getElementById('confirmPassword');
const passwordInput = document.getElementById('password');

if (confirmPasswordInput) {
    confirmPasswordInput.addEventListener('input', function() {
        const password = passwordInput ? passwordInput.value : '';
        const confirmPassword = this.value;
        const passwordError = document.getElementById('passwordError');

        if (password === confirmPassword) {
            passwordError.style.display = 'none';
        } else {
            passwordError.style.display = 'block';
        }
    });
}

if (passwordInput) {
    passwordInput.addEventListener('input', function() {
        const formatError = document.getElementById('passwordFormatError');
        if (formatError) formatError.remove();

        const englishLettersAndNumbersOnly = /^[a-zA-Z0-9]+$/;
        if (this.value.length > 0 && !englishLettersAndNumbersOnly.test(this.value)) {
            const errorDiv = document.createElement('div');
            errorDiv.id = 'passwordFormatError';
            errorDiv.className = 'text-danger mt-1';
            errorDiv.textContent = 'Пароль может содержать только английские буквы и цифры';
            this.parentNode.insertAdjacentElement('afterend', errorDiv);
            this.classList.add('is-invalid');
        } else {
            this.classList.remove('is-invalid');
        }

        // Также проверяем совпадение, если confirmPassword уже заполнен
        const confirmPassword = confirmPasswordInput ? confirmPasswordInput.value : '';
        const passwordError = document.getElementById('passwordError');
        if (confirmPassword && this.value !== confirmPassword) {
            passwordError.style.display = 'block';
        } else if (confirmPassword && this.value === confirmPassword) {
            passwordError.style.display = 'none';
        }
    });
}