function validateEmail(email) {
    const allowedDomains = [
        'mail.ru', 'bk.ru', 'list.ru', 'inbox.ru',
        'yandex.ru', 'yandex.ua', 'yandex.by', 'yandex.kz', 'yandex.com', 'ya.ru',
        'rambler.ru', 'lenta.ru', 'autorambler.ru', 'myrambler.ru', 'ro.ru', 'r0.ru',
        'vk.com', 'vkontakte.ru'
    ];

    if (!email || !email.includes('@')) return false;

    const domain = email.split('@')[1].toLowerCase();
    return allowedDomains.some(allowed => domain === allowed || domain.endsWith('.' + allowed));
}

// Валидация почты - ТОЛЬКО клиентская, без preventDefault для сохранения данных
document.addEventListener('DOMContentLoaded', function() {
    const emailInput = document.querySelector('input[name="email"]');
    const form = document.getElementById('registrationForm');

    if (emailInput && form) {
        // Убираем submit валидацию, так как она будет на сервере с сохранением данных
        // Добавляем валидацию при потере фокуса
        emailInput.addEventListener('blur', function() {
            const email = this.value;
            const existingError = document.getElementById('emailDomainError');
            if (existingError) existingError.remove();

            if (email && !validateEmail(email)) {
                const errorDiv = document.createElement('div');
                errorDiv.id = 'emailDomainError';
                errorDiv.className = 'text-danger mt-1';
                errorDiv.textContent = 'Используйте почту: Mail.ru, Яндекс, Рамблер и т.д.';
                this.parentNode.appendChild(errorDiv);
            }
        });

        emailInput.addEventListener('input', function() {
            const existingError = document.getElementById('emailDomainError');
            if (existingError) existingError.remove();
        });
    }
});