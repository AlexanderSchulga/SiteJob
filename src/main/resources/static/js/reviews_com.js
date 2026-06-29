// Функции для работы с ответами на отзывы

document.addEventListener('DOMContentLoaded', function() {
    // Обработчики для кнопок "Ответить"
    const replyButtons = document.querySelectorAll('.reply-btn');
    replyButtons.forEach(button => {
        button.addEventListener('click', function() {
            const reviewId = this.getAttribute('data-review-id');
            const nameuse = this.getAttribute('data-nameuse');
            const reviewText = this.getAttribute('data-review-text');

            startReply(reviewId, nameuse, reviewText);
        });
    });
});

function startReply(reviewId, nameuse, reviewText) {
    // Устанавливаем ID родительского отзыва
    document.getElementById('parentCommentId').value = reviewId;

    // Показываем информацию о том, на что отвечаем
    const replyToText = document.getElementById('replyToText');
    const truncatedText = reviewText.length > 100 ? reviewText.substring(0, 100) + '...' : reviewText;
    replyToText.textContent = nameuse + ': ' + truncatedText;

    // Показываем блок с информацией
    document.getElementById('replyTo').style.display = 'block';
    document.getElementById('cancelReplyBtn').style.display = 'inline-block';

    // Прокручиваем к форме
    document.getElementById('review-form').scrollIntoView({ behavior: 'smooth' });

    // Фокусируемся на текстовом поле
    document.getElementById('reviewText').focus();
}

function cancelReply() {
    // Сбрасываем форму
    document.getElementById('parentCommentId').value = '';
    document.getElementById('replyTo').style.display = 'none';
    document.getElementById('cancelReplyBtn').style.display = 'none';
    document.getElementById('reviewText').value = '';
}

// Обработчик отправки формы
document.getElementById('review-form')?.addEventListener('submit', function(e) {
    const text = document.getElementById('reviewText').value.trim();
    if (!text) {
        e.preventDefault();
        alert('Пожалуйста, введите текст отзыва');
        return;
    }
});

// Добавляем плавную анимацию для карточек
document.addEventListener('DOMContentLoaded', function() {
    const cards = document.querySelectorAll('.card');
    cards.forEach(card => {
        card.style.transition = 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out';
    });
});