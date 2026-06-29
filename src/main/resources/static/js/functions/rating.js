
// Упрощенный кэш с меньшим временем жизни
const ratingCache = new Map();
const RATING_CACHE_TTL = 10000; // 10 секунд

// Флаги для предотвращения множественных вызовов
let isLoadingRatings = false;
let ratingLoadTimeout = null;

// Хранилище для обработчиков
let ratingHandlers = [];

// Экспорт функций в глобальную область
window.loadAllSellerRatings = loadAllSellerRatings;
window.showRatingStars = showRatingStars;
window.initRatingHandlers = initRatingHandlers;
window.cleanupRatingHandlers = cleanupRatingHandlers;
window.clearRatingCache = clearRatingCache;
window.refreshSellerRating = refreshSellerRating;

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    loadAllSellerRatings();
    initRatingHandlers();
});

// Очистка кэша
function clearRatingCache(keepExisting = false) {
    if (keepExisting) {
        const now = Date.now();
        for (const [sellerId, timestamp] of ratingCache.entries()) {
            if (now - timestamp > 5000) {
                ratingCache.delete(sellerId);
            }
        }
    } else {
        ratingCache.clear();
    }
}

// Основная функция загрузки всех рейтингов
function loadAllSellerRatings() {
    if (isLoadingRatings) return;

    if (ratingLoadTimeout) clearTimeout(ratingLoadTimeout);

    ratingLoadTimeout = setTimeout(() => {
        isLoadingRatings = true;

        const ratingElements = document.querySelectorAll('.rating-stars[data-seller-id]');

        if (ratingElements.length === 0) {
            isLoadingRatings = false;
            return;
        }

        const sellerIds = new Set();
        ratingElements.forEach(el => {
            const sellerId = el.getAttribute('data-seller-id');
            if (sellerId && sellerId !== 'null' && sellerId !== 'undefined' && sellerId !== '') {
                sellerIds.add(sellerId);
            }
        });

        if (sellerIds.size === 0) {
            isLoadingRatings = false;
            return;
        }

        const loadPromises = Array.from(sellerIds).map(sellerId => {
            const lastLoad = ratingCache.get(sellerId);
            const now = Date.now();

            if (!lastLoad || now - lastLoad > RATING_CACHE_TTL) {
                ratingCache.set(sellerId, now);
                return loadSellerRating(sellerId);
            }
            return Promise.resolve();
        });

        Promise.all(loadPromises).finally(() => {
            isLoadingRatings = false;
        });
    }, 100);
}

// Обновление рейтинга конкретного продавца
function refreshSellerRating(sellerHashId) {
    ratingCache.delete(sellerHashId);
    return loadSellerRating(sellerHashId);
}

// Загрузка рейтинга конкретного продавца по hashId
function loadSellerRating(sellerHashId) {
    if (!sellerHashId || sellerHashId === 'null' || sellerHashId === 'undefined') {
        return Promise.resolve();
    }

    return fetch(`/api/rating/seller/hash/${sellerHashId}`, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        },
        credentials: 'include'
    })
        .then(response => {
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        })
        .then(data => {
            updateRatingDisplay(sellerHashId, data);
        })
        .catch(error => {
            showDefaultRating(sellerHashId);
        });
}

// Показать рейтинг по умолчанию
function showDefaultRating(sellerHashId) {
    const ratingElements = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerHashId}"]`);

    ratingElements.forEach(ratingElement => {
        const sellerRatingElement = ratingElement.closest('.seller-rating');
        if (!sellerRatingElement) return;

        const averageRatingElement = sellerRatingElement.querySelector('.average-rating');
        const ratingCountElement = sellerRatingElement.querySelector('.rating-count');

        if (averageRatingElement) averageRatingElement.textContent = '0';
        if (ratingCountElement) ratingCountElement.textContent = '0';

        renderEmptyStars(ratingElement);
    });
}

// Отрисовка пустых звезд
function renderEmptyStars(ratingElement) {
    if (!ratingElement) return;

    ratingElement.innerHTML = '';
    for (let i = 1; i <= 5; i++) {
        const star = document.createElement('span');
        star.className = 'rating-star empty-star';
        star.innerHTML = '☆';
        star.style.cssText = 'color:#ddd;cursor:default;font-size:16px;margin-right:2px;';
        ratingElement.appendChild(star);
    }
}

// Обновление отображения рейтинга
function updateRatingDisplay(sellerHashId, ratingData) {
    const ratingElements = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerHashId}"]`);

    ratingElements.forEach(ratingElement => {
        const sellerRatingElement = ratingElement.closest('.seller-rating');
        if (!sellerRatingElement) return;

        const averageRatingElement = sellerRatingElement.querySelector('.average-rating');
        const ratingCountElement = sellerRatingElement.querySelector('.rating-count');

        const averageRating = parseFloat(ratingData.averageRating) || 0;
        const ratingCount = ratingData.ratingCount || 0;

        if (averageRatingElement) averageRatingElement.textContent = averageRating.toFixed(1);
        if (ratingCountElement) ratingCountElement.textContent = ratingCount;

        ratingElement.innerHTML = '';
        for (let i = 1; i <= 5; i++) {
            const star = document.createElement('span');
            star.className = 'rating-star';
            star.style.cssText = 'cursor:default;font-size:18px;margin-right:2px;';

            if (i <= Math.floor(averageRating)) {
                star.innerHTML = '★';
                star.style.color = '#ffc107';
            } else if (i - 0.5 <= averageRating) {
                star.innerHTML = '★';
                star.style.color = '#ffc107';
                star.style.opacity = '0.7';
            } else {
                star.innerHTML = '☆';
                star.style.color = '#ddd';
            }

            ratingElement.appendChild(star);
        }
    });
}

// Инициализация обработчиков для оценки
function initRatingHandlers() {
    cleanupRatingHandlers();

    document.querySelectorAll('.rate-seller-link').forEach(link => {
        const handler = function(e) {
            e.preventDefault();
            const sellerHashId = this.getAttribute('data-seller-id');
            showRatingStars(sellerHashId);
        };

        link.addEventListener('click', handler);
        ratingHandlers.push({ element: link, handler });
    });
}

function cleanupRatingHandlers() {
    ratingHandlers.forEach(({ element, handler }) => {
        element.removeEventListener('click', handler);
    });
    ratingHandlers = [];
}

// Показать интерактивные звезды для оценки
function showRatingStars(sellerHashId) {
    const ratingElements = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerHashId}"]`);
    const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerHashId}"]`);

    if (!ratingElements.length) return;

    rateLinks.forEach(rateLink => {
        rateLink.style.display = 'none';
    });

    ratingElements.forEach(ratingElement => {
        ratingElement.innerHTML = '';

        for (let i = 1; i <= 5; i++) {
            const star = document.createElement('span');
            star.className = 'rating-star interactive-star';
            star.innerHTML = '☆';
            star.setAttribute('data-rating', i);
            star.style.cssText = 'cursor:pointer;font-size:16px;margin-right:2px;color:#6c757d;';

            star.addEventListener('mouseover', () => highlightStars(sellerHashId, i));
            star.addEventListener('mouseout', () => resetStars(sellerHashId));
            star.addEventListener('click', () => submitRating(sellerHashId, i));

            ratingElement.appendChild(star);
        }
    });
}

function highlightStars(sellerHashId, rating) {
    const starsContainers = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerHashId}"]`);

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

function resetStars(sellerHashId) {
    const starsContainers = document.querySelectorAll(`.rating-stars[data-seller-id="${sellerHashId}"]`);

    starsContainers.forEach(container => {
        const stars = container.querySelectorAll('.interactive-star');
        stars.forEach(star => {
            star.innerHTML = '☆';
            star.style.color = '#6c757d';
        });
    });
}

// Отправка оценки - ИСПРАВЛЕНО: используем hashId
function submitRating(sellerHashId, rating) {
    fetch(`/api/rating/rate/hash/${sellerHashId}?score=${rating}`, {  // ← ИЗМЕНЕНО: используем /hash/ эндпоинт
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
                updateRatingDisplay(sellerHashId, data);
                showRatingSuccess();
                ratingCache.set(sellerHashId, Date.now());

                setTimeout(() => {
                    const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerHashId}"]`);
                    rateLinks.forEach(rateLink => {
                        rateLink.style.display = 'inline';
                    });
                }, 2000);
            } else {
                alert('Ошибка при оценке: ' + (data.message || 'Неизвестная ошибка'));
                const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerHashId}"]`);
                rateLinks.forEach(rateLink => {
                    rateLink.style.display = 'inline';
                });
            }
        })
        .catch(error => {
            alert('Произошла ошибка при отправке оценки' );
            const rateLinks = document.querySelectorAll(`.rate-seller-link[data-seller-id="${sellerHashId}"]`);
            rateLinks.forEach(rateLink => {
                rateLink.style.display = 'inline';
            });
        });
}

function showRatingSuccess() {
    const successMsg = document.createElement('div');
    successMsg.className = 'alert alert-success position-fixed top-0 start-50 translate-middle-x mt-3';
    successMsg.style.cssText = 'z-index:9999;background-color:#d4edda;color:#155724;padding:10px 20px;border-radius:5px;box-shadow:0 2px 10px rgba(0,0,0,0.1);';
    successMsg.textContent = 'Спасибо за вашу оценку!';
    document.body.appendChild(successMsg);
    setTimeout(() => successMsg.remove(), 3000);
}

// Подавляем 404 ошибки для изображений
document.addEventListener('error', function(e) {
    const target = e.target;
    if (target.tagName === 'IMG' && target.src.includes('/uploads/')) {
        e.preventDefault();
        target.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
        target.classList.add('image-missing');
    }
}, true);