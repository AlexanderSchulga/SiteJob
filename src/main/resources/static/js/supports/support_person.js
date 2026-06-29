// Функции для работы с ответами
function replyToComment(commentId, username, nameuse, commentText, replyId = null) {
    document.getElementById('parentCommentId').value = commentId;

    // Если это ответ на ответ, сохраняем replyId
    if (replyId) {
        document.getElementById('replyId').value = replyId;
    }

    const decodedText = commentText
        .replace(/&#39;/g, "'")
        .replace(/&quot;/g, '"')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&amp;/g, '&');

    const displayText = decodedText.length > 50 ?
        decodedText.substring(0, 50) + '...' : decodedText;

    document.getElementById('replyToText').textContent = username + ': ' + nameuse + ': ' + displayText;
    document.getElementById('replyTo').style.display = 'block';
    document.getElementById('cancelReplyBtn').style.display = 'inline-block';
    document.getElementById('commentText').focus();
    document.getElementById('commentText').placeholder = 'Ваш ответ...';

    document.getElementById('support-comment-form').scrollIntoView({
        behavior: 'smooth',
        block: 'center'
    });
}

function cancelReply() {
    document.getElementById('parentCommentId').value = '';
    document.getElementById('replyTo').style.display = 'none';
    document.getElementById('cancelReplyBtn').style.display = 'none';
    document.getElementById('commentText').value = '';
    document.getElementById('commentText').placeholder = 'Ваше сообщение...';
}

function selectCategory(category) {
    window.location.href = '/support_service?category=' + category;
}

// Новая функция для выбора категории из карточки
function selectCategoryFromCard(element) {
    const category = element.getAttribute('data-category');
    selectCategory(category);
}

// Функция для переключения отображения ответов
function toggleReplies(button) {
    const commentId = button.getAttribute('data-comment-id');
    const repliesContainer = document.getElementById('replies-' + commentId);
    const icon = button.querySelector('i');

    if (!repliesContainer) return;

    if (repliesContainer.classList.contains('collapsed')) {
        // Разворачиваем
        repliesContainer.classList.remove('collapsed');
        repliesContainer.style.display = 'block';
        button.classList.remove('collapsed');
        icon.style.transform = 'rotate(0deg)';
    } else {
        // Сворачиваем
        repliesContainer.classList.add('collapsed');
        button.classList.add('collapsed');
        icon.style.transform = 'rotate(-90deg)';

        // Ждем окончания анимации перед скрытием
        setTimeout(() => {
            if (repliesContainer.classList.contains('collapsed')) {
                repliesContainer.style.display = 'none';
            }
        }, 300);
    }

    // Обновляем счетчик
    const repliesCount = countReplies(commentId);
    addRepliesCountBadge(button, repliesCount);
}

// Функция для подсчета количества ответов
function countReplies(commentId) {
    const repliesContainer = document.getElementById('replies-' + commentId);
    if (!repliesContainer) return 0;

    // Фильтруем только видимые ответы
    const visibleReplies = repliesContainer.querySelectorAll('.reply-comment:not(.d-none)');
    return visibleReplies.length;
}

// Функция для добавления бейджа с количеством ответов
function addRepliesCountBadge(button, count) {
    // Удаляем существующий бейдж
    const existingBadge = button.querySelector('.replies-count-badge');
    if (existingBadge) {
        existingBadge.remove();
    }

    // Добавляем новый бейдж если есть ответы
    if (count > 0) {
        const badge = document.createElement('span');
        badge.className = 'replies-count-badge';
        badge.textContent = count;
        button.appendChild(badge);
    }
}

// Функции для массового управления
function collapseAll() {
    const toggleButtons = document.querySelectorAll('.toggle-replies-btn');
    toggleButtons.forEach(button => {
        const commentId = button.getAttribute('data-comment-id');
        const repliesContainer = document.getElementById('replies-' + commentId);
        const icon = button.querySelector('i');

        if (!repliesContainer.classList.contains('collapsed')) {
            repliesContainer.classList.add('collapsed');
            button.classList.add('collapsed');
            icon.style.transform = 'rotate(-90deg)';
            setTimeout(() => {
                repliesContainer.style.display = 'none';
            }, 300);
        }
    });
}

function expandAll() {
    const toggleButtons = document.querySelectorAll('.toggle-replies-btn');
    toggleButtons.forEach(button => {
        const commentId = button.getAttribute('data-comment-id');
        const repliesContainer = document.getElementById('replies-' + commentId);
        const icon = button.querySelector('i');

        if (repliesContainer.classList.contains('collapsed')) {
            repliesContainer.classList.remove('collapsed');
            repliesContainer.style.display = 'block';
            button.classList.remove('collapsed');
            icon.style.transform = 'rotate(0deg)';
        }
    });
}

// Инициализация кнопок ответа и сворачивания
document.addEventListener('DOMContentLoaded', function() {
    // Инициализация кнопок ответа
    const replyButtons = document.querySelectorAll('.reply-btn');
    replyButtons.forEach(button => {
        button.addEventListener('click', function() {
            const commentId = this.getAttribute('data-comment-id');
            const username = this.getAttribute('data-username');
            const nameuse = this.getAttribute('data-nameuse');
            const commentText = this.getAttribute('data-comment-text');
            replyToComment(commentId, username, nameuse, commentText);
        });
    });

    // Инициализация кнопок сворачивания/разворачивания
    const toggleButtons = document.querySelectorAll('.toggle-replies-btn');
    toggleButtons.forEach(button => {
        const commentId = button.getAttribute('data-comment-id');
        const repliesCount = countReplies(commentId);

        // Добавляем бейдж с количеством ответов
        addRepliesCountBadge(button, repliesCount);

        // Добавляем класс если есть ответы
        if (repliesCount > 0) {
            button.closest('.comment-card').classList.add('has-replies');
        }

        // Обработчик клика
        button.addEventListener('click', function() {
            toggleReplies(this);
        });
    });

});