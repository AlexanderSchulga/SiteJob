/**
 * Функция проверки наличия объявлений на модерации
 * и обновления внешнего вида кнопки
 */
function checkModerationStatus() {
    const moderationBtn = document.getElementById('moderationBtn');
    if (!moderationBtn) return;

    // Проверяем, есть ли данные о модерации из серверной части
    const hasModeration = document.querySelector('[data-moderation-count]');
    if (hasModeration) {
        const count = parseInt(hasModeration.dataset.moderationCount) || 0;
        updateModerationButton(count);
        return;
    }

    // Если данных нет, делаем AJAX-запрос
    fetch('/api/moderation/count', {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || ''
        },
        credentials: 'same-origin'
    })
        .then(response => response.json())
        .then(data => {
            if (data.total > 0) {
                updateModerationButton(data.total);
            }
        })
        .catch(err => console.log('Moderation check error:', err));
}

/**
 * Обновление внешнего вида кнопки модерации
 */
function updateModerationButton(count) {
    const moderationBtn = document.getElementById('moderationBtn');
    if (!moderationBtn) return;

    if (count > 0) {
        moderationBtn.classList.add('has-moderation');

        // Добавляем бейдж с количеством, если его нет
        let badge = moderationBtn.querySelector('.badge');
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'badge bg-warning text-dark ms-1';
            const dot = document.createElement('i');
            dot.className = 'fas fa-circle';
            dot.style.fontSize = '0.5rem';
            badge.appendChild(dot);
            const countSpan = document.createElement('span');
            countSpan.textContent = count;
            badge.appendChild(countSpan);
            moderationBtn.appendChild(badge);
        } else {
            const countSpan = badge.querySelector('span');
            if (countSpan) countSpan.textContent = count;
        }
    } else {
        moderationBtn.classList.remove('has-moderation');
        const badge = moderationBtn.querySelector('.badge');
        if (badge) badge.remove();
    }
}

// Запускаем проверку при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Проверяем, авторизован ли пользователь и имеет ли доступ к модерации
    const moderationBtn = document.getElementById('moderationBtn');
    if (moderationBtn) {
        checkModerationStatus();
    }
});

// Периодическая проверка каждые 30 секунд
setInterval(checkModerationStatus, 30000);