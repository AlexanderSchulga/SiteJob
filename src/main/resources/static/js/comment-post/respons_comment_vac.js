// Функция для декодирования HTML-сущностей (например, &#1088; -> р)
function decodeHtmlEntities(text) {
    if (!text) return '';
    const parser = new DOMParser();
    const decoded = parser.parseFromString(text, 'text/html').documentElement.textContent;
    return decoded;
}

// Функция для декодирования base64 текста с поддержкой UTF-8
function decodeIfEncoded(text) {
    if (!text) return '';

    const base64Regex = /^[A-Za-z0-9+/]*={0,2}$/;

    try {
        if (text.length > 100 || !base64Regex.test(text) || text.includes(' ')) {
            return text;
        }
        const binary = atob(text);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return new TextDecoder('utf-8').decode(bytes);
    } catch (error) {
        return text;
    }
}

// Функция для ответа на комментарий
function replyToComment(commentId, nameuse, commentText) {

    // Устанавливаем parentCommentId
    const parentCommentIdInput = document.getElementById('parentCommentId');
    if (parentCommentIdInput) {
        parentCommentIdInput.value = commentId;
    }

    // Ограничиваем длину отображаемого текста
    const displayText = commentText.length > 50 ?
        commentText.substring(0, 50) + '...' : commentText;

    const replyToText = document.getElementById('replyToText');
    if (replyToText) {
        replyToText.textContent = nameuse + ': ' + displayText;
    }

    const replyTo = document.getElementById('replyTo');
    if (replyTo) {
        replyTo.style.display = 'block';
    }

    const cancelReplyBtn = document.getElementById('cancelReplyBtn');
    if (cancelReplyBtn) {
        cancelReplyBtn.style.display = 'inline-block';
    }

    const commentTextarea = document.getElementById('commentText');
    if (commentTextarea) {
        commentTextarea.focus();
        commentTextarea.placeholder = 'Ваш ответ...(не менее 3-х символов)';
    }

    // Прокрутка к форме
    const privateForm = document.getElementById('private-comment-form');
    if (privateForm) {
        privateForm.scrollIntoView({
            behavior: 'smooth',
            block: 'center'
        });
    }

    // Активируем вкладку личных комментариев
    const privateTab = document.getElementById('private-tab');
    if (privateTab) {
        const tab = new bootstrap.Tab(privateTab);
        tab.show();
        sessionStorage.setItem('activeCommentTab', '#private-comments');
    }
}

// функция отмены ответа
function cancelReply() {
    const parentCommentId = document.getElementById('parentCommentId');
    if (parentCommentId) parentCommentId.value = '';

    const replyTo = document.getElementById('replyTo');
    if (replyTo) replyTo.style.display = 'none';

    const cancelReplyBtn = document.getElementById('cancelReplyBtn');
    if (cancelReplyBtn) cancelReplyBtn.style.display = 'none';

    const commentText = document.getElementById('commentText');
    if (commentText) {
        commentText.value = '';
        commentText.placeholder = 'Ваш личный комментарий...';
    }
}

// Функция свертывания/разворачивания ветки
function toggleCommentBranch(commentId, event) {
    if (event) {
        event.stopPropagation();
    }

    const repliesContainer = document.getElementById(`replies-${commentId}`);
    const toggleBtn = document.getElementById(`toggle-btn-${commentId}`);
    const icon = document.getElementById(`toggle-icon-${commentId}`);

    if (repliesContainer && toggleBtn) {
        if (repliesContainer.style.display === 'none') {
            repliesContainer.style.display = 'block';
            toggleBtn.classList.remove('collapsed');
            if (icon) {
                icon.classList.remove('fa-chevron-down');
                icon.classList.add('fa-chevron-up');
            }
            toggleBtn.setAttribute('aria-expanded', 'true');
        } else {
            repliesContainer.style.display = 'none';
            toggleBtn.classList.add('collapsed');
            if (icon) {
                icon.classList.remove('fa-chevron-up');
                icon.classList.add('fa-chevron-down');
            }
            toggleBtn.setAttribute('aria-expanded', 'false');
        }
    }
}

// ========== ДЕЛЕГИРОВАНИЕ СОБЫТИЙ ==========
document.addEventListener('click', function(e) {
    // Обработчик для кнопок "Ответить"
    const replyBtn = e.target.closest('.reply-btn');
    if (replyBtn) {
        e.preventDefault();
        const commentId = replyBtn.getAttribute('data-comment-id');
        const nameuse = replyBtn.getAttribute('data-nameuse');
        let commentText = replyBtn.getAttribute('data-comment-text') || '';

        if (commentId && nameuse) {
            replyToComment(commentId, nameuse, commentText);
        }
        return;
    }

    // Обработчик для кнопок свертывания/разворачивания
    const toggleBtn = e.target.closest('.toggle-branch-btn');
    if (toggleBtn) {
        e.preventDefault();
        e.stopPropagation();
        const commentId = toggleBtn.getAttribute('data-comment-id');
        if (commentId) {
            toggleCommentBranch(commentId, e);
        }
        return;
    }
});

// Сохранение состояния активной вкладки
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - initializing comments');

    // Инициализация формы личных комментариев
    setupPrivateCommentForm();

    // Восстановление активной вкладки из sessionStorage
    const activeTab = sessionStorage.getItem('activeCommentTab');
    if (activeTab) {
        try {
            const tabElement = document.querySelector(`[data-bs-target="${activeTab}"]`);
            if (tabElement) {
                const tab = new bootstrap.Tab(tabElement);
                tab.show();
                console.log('Restored tab:', activeTab);

                if (activeTab === '#private-comments' && window.commentsPagination && !window.commentsPagination.initialized) {
                    setTimeout(() => {
                        window.commentsPagination.loadInitialData();
                        window.commentsPagination.initialized = true;
                    }, 300);
                }
            }
        } catch (e) {
            console.log('Error restoring tab:', e);
        }
    }

    // Сохранение состояния при переключении вкладок
    const tabButtons = document.querySelectorAll('#commentsTab button[data-bs-toggle="tab"]');
    tabButtons.forEach(button => {
        button.addEventListener('shown.bs.tab', function(event) {
            const target = event.target.getAttribute('data-bs-target');
            sessionStorage.setItem('activeCommentTab', target);
            console.log('Tab changed to:', target);

            if (target === '#private-comments' && window.commentsPagination && !window.commentsPagination.initialized) {
                setTimeout(() => {
                    window.commentsPagination.loadInitialData();
                    window.commentsPagination.initialized = true;
                }, 300);
            }
        });
    });

    console.log('Comment system initialized with event delegation');
});

// Функция отправки личного комментария через AJAX
function setupPrivateCommentForm() {
    const privateForm = document.getElementById('private-comment-form');
    if (!privateForm) return;

    // Удаляем старый обработчик, если есть
    if (privateForm._submitHandler) {
        privateForm.removeEventListener('submit', privateForm._submitHandler);
    }

    // Создаем обработчик
    const submitHandler = async function(e) {
        e.preventDefault();

        const formData = new FormData(this);
        const pathParts = window.location.pathname.split('/');
        const hashId = pathParts[2];

        // Определяем тип поста из URL
        let baseUrl = '';
        const path = window.location.pathname;
        if (path.includes('/postidviewsjob/')) {
            baseUrl = `/postidviewsjob/${hashId}/commentPersonalJob`;
        } else if (path.includes('/postidviewsvac/')) {
            baseUrl = `/postidviewsvac/${hashId}/commentPersonalVac`;
        } else if (path.includes('/postidviewsres/')) {
            baseUrl = `/postidviewsres/${hashId}/commentPersonalRes`;
        } else {
            baseUrl = `/postidviews/${hashId}/commentPersonal`;
        }

        // Сохраняем текущее состояние раскрытых веток
        const expandedBranches = saveExpandedBranchesState();

        // Показываем индикатор загрузки
        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Отправка...';

        try {
            const response = await fetch(baseUrl, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (response.status === 401) {
                console.log('Требуется авторизация для личного комментария');
                this.reset();
                cancelReply();
                restoreExpandedBranchesState(expandedBranches);
                return;
            }

            const data = await response.json();

            if (data.status === 'success') {
                this.reset();
                cancelReply();
                await smartRefreshComments();
                restoreExpandedBranchesState(expandedBranches);
                showSuccessMessage('Комментарий успешно отправлен!');

                const privateComments = document.getElementById('private-comments');
                if (privateComments) {
                    privateComments.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            }
            else if (data.status === 'ignored') {
                console.log('Комментарий проигнорирован (слишком короткий)');
                this.reset();
                cancelReply();
                restoreExpandedBranchesState(expandedBranches);
            } else {
                showErrorMessage(data.message || 'Ошибка отправки комментария');
                restoreExpandedBranchesState(expandedBranches);
            }
        } catch (error) {
            showErrorMessage('Ошибка при отправке комментария');
            restoreExpandedBranchesState(expandedBranches);
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    };

    privateForm._submitHandler = submitHandler;
    privateForm.addEventListener('submit', submitHandler);
}

// Сохраняет состояние раскрытых веток комментариев
function saveExpandedBranchesState() {
    const expandedBranches = new Set();
    const commentElements = document.querySelectorAll('.comment-item');

    commentElements.forEach(comment => {
        const commentId = comment.dataset.commentId;
        const repliesContainer = document.getElementById(`replies-${commentId}`);
        if (repliesContainer && repliesContainer.style.display !== 'none') {
            expandedBranches.add(commentId);
        }
    });

    return expandedBranches;
}

// Восстанавливает состояние раскрытых веток комментариев
function restoreExpandedBranchesState(expandedBranches) {
    expandedBranches.forEach(commentId => {
        const repliesContainer = document.getElementById(`replies-${commentId}`);
        const toggleBtn = document.getElementById(`toggle-btn-${commentId}`);
        const icon = document.getElementById(`toggle-icon-${commentId}`);

        if (repliesContainer && toggleBtn && icon) {
            repliesContainer.style.display = 'block';
            toggleBtn.classList.remove('collapsed');
            icon.classList.remove('fa-chevron-down');
            icon.classList.add('fa-chevron-up');
            toggleBtn.setAttribute('aria-expanded', 'true');
        }
    });
}


// Умное обновление комментариев
async function smartRefreshComments() {
    if (!window.commentsPagination) return;

    const pagination = window.commentsPagination;
    const pagesToLoad = Math.max(pagination.currentPage + 1, 1);

    // Полный сброс и загрузка первой страницы
    await pagination.reload();

    // Догружаем остальные страницы
    for (let i = 1; i < pagesToLoad; i++) {
        if (!pagination.hasMore) break;
        pagination.isLoading = false; // на всякий случай
        await pagination.loadMore(false);
    }
}

// Функция показа успешного сообщения
function showSuccessMessage(message) {
    const existingAlerts = document.querySelectorAll('.alert.alert-success.position-fixed');
    existingAlerts.forEach(el => el.remove());

    const successDiv = document.createElement('div');
    successDiv.className = 'alert alert-success alert-dismissible fade show position-fixed top-0 end-0 m-3';
    successDiv.style.zIndex = '9999';
    successDiv.style.right = '20px';
    successDiv.style.top = '80px';
    successDiv.innerHTML = `
        <strong>Успех!</strong> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(successDiv);

    setTimeout(() => {
        if (successDiv.parentNode) {
            successDiv.remove();
        }
    }, 3000);
}

// Функция показа сообщения об ошибке
function showErrorMessage(message) {
    const existingAlerts = document.querySelectorAll('.alert.alert-danger.position-fixed');
    existingAlerts.forEach(el => el.remove());

    const errorDiv = document.createElement('div');
    errorDiv.className = 'alert alert-danger alert-dismissible fade show position-fixed top-0 end-0 m-3';
    errorDiv.style.zIndex = '9999';
    errorDiv.style.right = '20px';
    errorDiv.style.top = '80px';
    errorDiv.innerHTML = `
        <strong>Ошибка!</strong> ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;

    document.body.appendChild(errorDiv);

    setTimeout(() => {
        if (errorDiv.parentNode) {
            errorDiv.remove();
        }
    }, 5000);
}

// Делаем функции глобально доступными
window.cancelReply = cancelReply;
window.replyToComment = replyToComment;
window.toggleCommentBranch = toggleCommentBranch;
window.decodeHtmlEntities = decodeHtmlEntities;
window.decodeIfEncoded = decodeIfEncoded;