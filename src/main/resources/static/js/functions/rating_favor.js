window.loadAllSellerRatings = loadAllSellerRatings;
window.showRatingStars = showRatingStars;
window.initRatingHandlers = initRatingHandlers;



document.addEventListener('DOMContentLoaded', function() {
    // Загрузка рейтингов для всех продавцов на странице
    loadAllSellerRatings();

    // Инициализация обработчиков для кнопки "Оценить" (только для авторизованных)
    initRatingHandlers();
});

function loadAllSellerRatings() {
    console.log('Loading all seller ratings...');
    initAllRatings();
}

function loadSellerRating(sellerId) {

    fetch(`/api/rating/seller/${sellerId}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        },
        credentials: 'include'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            updateRatingDisplay(sellerId, data);
        })
        .catch(error => {
            showDefaultRating(sellerId);
        });
}

function showDefaultRating(sellerId) {
    const ratingElements = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerId}"]`);

    ratingElements.forEach(ratingElement => {
        const sellerRatingElement = ratingElement.closest('.seller-rating');
        if (!sellerRatingElement) return;

        const averageRatingElement = sellerRatingElement.querySelector('.average-rating');
        const ratingCountElement = sellerRatingElement.querySelector('.rating-count');

        // Устанавливаем значения по умолчанию
        if (averageRatingElement) {
            averageRatingElement.textContent = '0';
        }
        if (ratingCountElement) {
            ratingCountElement.textContent = '0';
        }

        // Показываем пустые звезды
        renderEmptyStars(ratingElement);
    });
}

function renderEmptyStars(ratingElement) {
    ratingElement.innerHTML = '';
    for (let i = 1; i <= 5; i++) {
        const star = document.createElement('span');
        star.className = 'rating-star empty-star';
        star.innerHTML = '☆';
        star.style.color = '#ddd';
        star.style.cursor = 'default';
        star.style.fontSize = '16px';
        star.style.marginRight = '2px';
        ratingElement.appendChild(star);
    }
}

function updateRatingDisplay(sellerId, ratingData) {


    const ratingElements = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerId}"]`);

    if (!ratingElements.length) {

        return;
    }

    ratingElements.forEach(ratingElement => {
        const sellerRatingElement = ratingElement.closest('.seller-rating');
        if (!sellerRatingElement) {
            console.warn('No parent seller-rating element found');
            return;
        }

        const averageRatingElement = sellerRatingElement.querySelector('.average-rating');
        const ratingCountElement = sellerRatingElement.querySelector('.rating-count');

        // Обновляем числовые значения
        const averageRating = parseFloat(ratingData.averageRating) || 0;
        const ratingCount = ratingData.ratingCount || 0;
        const userRating = ratingData.userRating || 0;

        if (averageRatingElement) {
            averageRatingElement.textContent = averageRating.toFixed(1);
        }
        if (ratingCountElement) {
            ratingCountElement.textContent = ratingCount;
        }

        // Очищаем звезды
        ratingElement.innerHTML = '';

        // Создаем звезды для отображения среднего рейтинга
        for (let i = 1; i <= 5; i++) {
            const star = document.createElement('span');
            star.className = 'rating-star';
            star.style.cursor = 'default';
            star.style.fontSize = '18px';
            star.style.marginRight = '2px';

            if (i <= averageRating) {
                // Полная звезда
                star.innerHTML = '★';
                star.style.color = '#ffc107';
            } else if (i - 0.5 <= averageRating) {
                // Половина звезды (можно использовать HTML entity для полузвезды)
                star.innerHTML = '★';
                star.style.color = '#ffc107';
                star.style.opacity = '0.7';
            } else {
                // Пустая звезда
                star.innerHTML = '☆';
                star.style.color = '#ddd';
            }

            ratingElement.appendChild(star);
        }
    });
}

// функция для инициализации при загрузке страницы
function initAllRatings() {
    console.log('Initializing all ratings...');

    // Находим все элементы с рейтингом на странице
    const ratingElements = document.querySelectorAll('.rating-stars[data-seller-id]');

    if (!ratingElements.length) {
        console.log('No rating elements found on page');
        return;
    }

    // Собираем уникальные ID продавцов
    const sellerIds = new Set();
    ratingElements.forEach(el => {
        const sellerId = el.getAttribute('data-seller-id');
        if (sellerId) sellerIds.add(sellerId);
    });

    // Загружаем рейтинг для каждого продавца
    sellerIds.forEach(sellerId => {
        loadSellerRating(sellerId);
    });
}

function initRatingHandlers() {
    // Обработчики для ссылки "Оценить" - только для авторизованных пользователей
    document.querySelectorAll('.rate-seller-link').forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            const sellerId = this.getAttribute('data-seller-id');
            showRatingStars(sellerId);
        });
    });
}

function showRatingStars(sellerId) {
    const ratingElements = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerId}"]`);
    const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerId}"]`);

    if (!ratingElements.length) return;

    // Скрываем все ссылки "Оценить" для этого продавца
    rateLinks.forEach(rateLink => {
        rateLink.style.display = 'none';
    });

    // Создаем интерактивные звезды для всех элементов рейтинга этого продавца
    ratingElements.forEach(ratingElement => {
        // Очищаем и создаем интерактивные звезды
        ratingElement.innerHTML = '';

        for (let i = 1; i <= 5; i++) {
            const star = document.createElement('span');
            star.className = 'rating-star interactive-star';
            star.innerHTML = '☆';
            star.setAttribute('data-rating', i);
            star.style.cursor = 'pointer';
            star.style.fontSize = '16px';
            star.style.marginRight = '2px';
            star.style.color = '#6c757d';

            star.addEventListener('mouseover', function() {
                highlightStars(sellerId, i);
            });

            star.addEventListener('mouseout', function() {
                resetStars(sellerId);
            });

            star.addEventListener('click', function() {
                submitRating(sellerId, i);
            });

            ratingElement.appendChild(star);
        }
    });
}

function highlightStars(sellerId, rating) {
    const starsContainers = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerId}"]`);

    starsContainers.forEach(container => {
        const stars = container.querySelectorAll('.interactive-star');
        stars.forEach((star, index) => {
            if (index < rating) {
                star.innerHTML = '★';
                star.style.color = '#ffc107';
            } else {
                star.innerHTML = '☆';
                star.style.color = '#6c757d';
            }
        });
    });
}

function resetStars(sellerId) {
    const starsContainers = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerId}"]`);

    starsContainers.forEach(container => {
        const stars = container.querySelectorAll('.interactive-star');
        stars.forEach(star => {
            star.innerHTML = '☆';
            star.style.color = '#6c757d';
        });
    });
}

function submitRating(sellerId, rating) {
    fetch(`/api/rating/rate/${sellerId}?score=${rating}`, {
        method: 'POST',
        credentials: 'include'
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(errorText => {
                    throw new Error(errorText || `HTTP error! status: ${response.status}`);
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // Обновляем отображение рейтинга для всех элементов
                updateRatingDisplay(sellerId, data);
                // Показываем сообщение об успехе
                showRatingSuccess();

                // Восстанавливаем обычное отображение звезд через 2 секунды
                setTimeout(() => {
                    loadSellerRating(sellerId);
                    const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerId}"]`);
                    rateLinks.forEach(rateLink => {
                        rateLink.style.display = 'inline';
                    });
                }, 2000);
            } else {
                alert('Ошибка при оценке: ' + (data.message || 'Неизвестная ошибка'));
                // Восстанавливаем ссылки при ошибке
                const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerId}"]`);
                rateLinks.forEach(rateLink => {
                    rateLink.style.display = 'inline';
                });
            }
        })
        .catch(error => {
            alert('Произошла ошибка при отправке оценки');
            // Восстанавливаем ссылки при ошибке
            const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerId}"]`);
            rateLinks.forEach(rateLink => {
                rateLink.style.display = 'inline';
            });
        });
}

function showRatingSuccess() {
    const successMsg = document.createElement('div');
    successMsg.className = 'alert alert-success position-fixed top-0 start-50 translate-middle-x mt-3';
    successMsg.style.zIndex = '9999';
    successMsg.textContent = 'Спасибо за вашу оценку!';
    document.body.appendChild(successMsg);

    setTimeout(() => {
        successMsg.remove();
    }, 3000);
}