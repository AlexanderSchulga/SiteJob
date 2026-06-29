
document.addEventListener('DOMContentLoaded', function() {
    // Инициализация кнопок избранного
    initFavoriteButtons();

    // Проверка статуса избранного для текущей страницы
    checkCurrentPageFavoriteStatus();
});

function initFavoriteButtons() {
    const favoriteButtons = document.querySelectorAll('.favorite-btn');

    favoriteButtons.forEach(button => {
        // Проверить текущий статус
        checkFavoriteStatus(button);

        // Обработчик клика
        button.addEventListener('click', function() {
            toggleFavorite(this);
        });
    });
}

function checkCurrentPageFavoriteStatus() {
    const favoriteBtn = document.getElementById('favoriteBtn');
    if (favoriteBtn) {
        checkFavoriteStatus(favoriteBtn);
    }
}

function checkFavoriteStatus(button) {
    const type = button.getAttribute('data-type');
    const id = button.getAttribute('data-id');

    fetch(`/page_one/favorites/check/${type}/${id}`, {
        method: 'GET', // явно указываем метод
        headers: {
            'X-CSRF-TOKEN': getCsrfToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            updateButtonAppearance(button, data.isFavorite);
        })
        .catch(error => {
            console.log('Error checking favorite status');
        });
}

function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') ||
        document.querySelector('input[name="_csrf"]')?.value;
}


function toggleFavorite(button) {
    const type = button.getAttribute('data-type');
    const id = button.getAttribute('data-id');

    // Блокируем кнопку на время запроса
    button.disabled = true;

    fetch(`/page_one/favorites/toggle/${type}/${id}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                // ОСОБАЯ ОБРАБОТКА ДЛЯ РАЗНЫХ ОШИБОК
                if (data.error.includes("архивный")) {
                    updateButtonAsArchived(button);
                    showMessage(data.error, 'warning');
                } else if (data.error.includes("свой собственный")) {
                    updateButtonAsOwnPost(button);
                    showMessage(data.error, 'info');
                } else {
                    showMessage(data.error, 'error');
                }
            } else {
                updateButtonAppearance(button, data.isFavorite);
                showMessage(data.message, 'success');
            }
        })
        .catch(error => {
            console.log('Error toggling favorite');
            showMessage('Ошибка при обновлении избранного', 'error');
        })
        .finally(() => {
            button.disabled = false;
        });
}

// Обновление вида кнопки для собственных постов
function updateButtonAsOwnPost(button) {
    const icon = button.querySelector('i');
    const text = button.querySelector('span');

    button.classList.remove('btn-outline-warning', 'btn-warning');
    button.classList.add('btn-outline-info');
    button.disabled = true;
    icon.classList.remove('far', 'fas');
    icon.classList.add('fas', 'fa-user-check');
    if (text) text.textContent = ' Ваш пост';
}

function updateButtonAppearance(button, isFavorite) {
    const icon = button.querySelector('i');
    const text = button.querySelector('span');

    if (isFavorite) {
        button.classList.remove('btn-outline-warning');
        button.classList.add('btn-warning');
        icon.classList.remove('far');
        icon.classList.add('fas');
        if (text) text.textContent = ' В избранном';
    } else {
        button.classList.remove('btn-warning');
        button.classList.add('btn-outline-warning');
        icon.classList.remove('fas');
        icon.classList.add('far');
        if (text) text.textContent = ' В избранное';
    }
}

function showMessage(message, type) {
    // Создаем временное уведомление
    const alert = document.createElement('div');
    alert.className = `alert alert-${type === 'error' ? 'danger' : 'success'} alert-dismissible fade show`;
    alert.style.position = 'fixed';
    alert.style.top = '20px';
    alert.style.right = '20px';
    alert.style.zIndex = '9999';
    alert.style.minWidth = '300px';
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(alert);

    // Автоматически скрываем через 3 секунды
    setTimeout(() => {
        if (alert.parentNode) {
            alert.parentNode.removeChild(alert);
        }
    }, 3000);
}