class LoadMoreCommentsPagination {
    constructor() {
        this.containerId = 'commentsContainer';
        this.buttonId = 'loadMoreCommentsBtn';
        this.buttonContainerId = 'loadMoreCommentsContainer';
        this.loadedMessageId = 'allLoadedCommentsMessage';
        this.noCommentsId = 'noCommentsMessage';

        this.currentPage = 0;
        this.size = 3;
        this.isLoading = false;
        this.hasMore = true;
        this.loadedIds = new Set();
        this.expandedBranches = new Set();
        this.initialized = false;

        // Определяем тип объявления из пути URL
        this.postType = this.determinePostType();

        // Извлекаем hashId (он всегда на 2-й позиции)
        const pathParts = window.location.pathname.split('/');
        this.hashId = pathParts[2];

        this.currentUserId = document.body.getAttribute('data-current-user-id');
        this.csrfToken = this.getCsrfToken();

        this.hasSuperAccess = document.body.hasAttribute('data-is-super') ||
            document.body.getAttribute('data-user-role') === 'ROLE_SUPER';
        this.isAdminUser = document.body.hasAttribute('data-is-admin') ||
            document.body.getAttribute('data-user-role') === 'ROLE_ADMIN';
        this.hasOnlySuperAccess = this.hasSuperAccess || this.isAdminUser;

        this.init();
        this.createModalElements();
    }

    /**
     * Создает модальные окна для удаления комментариев
     */
    createModalElements() {
        // Проверяем, существуют ли уже модальные окна
        if (!document.getElementById('deleteCommentModal')) {
            // Модальное окно для удаления одного комментария
            const deleteCommentModal = document.createElement('div');
            deleteCommentModal.innerHTML = `
                <div class="modal fade" id="deleteCommentModal" tabindex="-1" aria-labelledby="deleteCommentModalLabel" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="deleteCommentModalLabel">Удаление комментария</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Закрыть"></button>
                            </div>
                            <div class="modal-body">
                                <p>Вы уверены, что хотите удалить этот комментарий?</p>
                                <p class="text-muted small">Это действие нельзя отменить.</p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                                <button type="button" class="btn btn-danger" id="confirmDeleteCommentBtn">Удалить</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(deleteCommentModal.firstElementChild);

            // Модальное окно для удаления ветки комментариев
            const deleteBranchModal = document.createElement('div');
            deleteBranchModal.innerHTML = `
                <div class="modal fade" id="deleteBranchModal" tabindex="-1" aria-labelledby="deleteBranchModalLabel" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="deleteBranchModalLabel">Удаление ветки комментариев</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Закрыть"></button>
                            </div>
                            <div class="modal-body">
                                <p>Это действие <strong>нельзя отменить</strong>.</p>
                                <p class="text-muted small">Будут удалены все комментарии в этой ветке.</p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                                <button type="button" class="btn btn-danger" id="confirmDeleteBranchBtn">Удалить всю ветку</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(deleteBranchModal.firstElementChild);
        }

        // Инициализируем обработчики для модальных окон
        this.initModalHandlers();
    }

    /**
     * Инициализирует обработчики для модальных окон
     */
    initModalHandlers() {
        // Удаление одного комментария
        const confirmDeleteBtn = document.getElementById('confirmDeleteCommentBtn');
        if (confirmDeleteBtn) {
            confirmDeleteBtn.addEventListener('click', () => {
                if (this.pendingDeleteForm) {
                    this.pendingDeleteForm.submit();
                    this.pendingDeleteForm = null;

                    // Закрываем модальное окно
                    const modal = bootstrap.Modal.getInstance(document.getElementById('deleteCommentModal'));
                    if (modal) modal.hide();
                }
            });
        }

        // Удаление ветки комментариев
        const confirmDeleteBranchBtn = document.getElementById('confirmDeleteBranchBtn');
        if (confirmDeleteBranchBtn) {
            confirmDeleteBranchBtn.addEventListener('click', () => {
                if (this.pendingDeleteBranchForm) {
                    this.pendingDeleteBranchForm.submit();
                    this.pendingDeleteBranchForm = null;

                    // Закрываем модальное окно
                    const modal = bootstrap.Modal.getInstance(document.getElementById('deleteBranchModal'));
                    if (modal) modal.hide();
                }
            });
        }
    }

    /**
     * Показывает модальное окно для удаления комментария
     */
    showDeleteCommentModal(form) {
        this.pendingDeleteForm = form;
        const modal = new bootstrap.Modal(document.getElementById('deleteCommentModal'));
        modal.show();
    }

    /**
     * Показывает модальное окно для удаления ветки комментариев
     */
    showDeleteBranchModal(form) {
        this.pendingDeleteBranchForm = form;
        const modal = new bootstrap.Modal(document.getElementById('deleteBranchModal'));
        modal.show();
    }

    /**
     * Определяет тип объявления на основе URL.
     */
    determinePostType() {
        const path = window.location.pathname;
        if (path.includes('/postidviewsjob/')) {
            return 'postidviewsjob';
        } else if (path.includes('/postidviewsvac/')) {
            return 'postidviewsvac';
        } else if (path.includes('/postidviewsres/')) {
            return 'postidviewsres';
        } else {
            return 'postidviews';
        }
    }

    /**
     * Возвращает правильный URL для AJAX-запроса в зависимости от типа
     */
    getAjaxUrl() {
        switch(this.postType) {
            case 'postidviewsjob':
                // Для услуг
                return `/${this.postType}/${this.hashId}/load-more-personal-comments-job?page=${this.currentPage}&size=${this.size}`;
            case 'postidviewsvac':
                // Для вакансий
                return `/${this.postType}/${this.hashId}/load-more-personal-comments-vacancy?page=${this.currentPage}&size=${this.size}`;
            case 'postidviewsres':
                // Для резюме
                return `/${this.postType}/${this.hashId}/load-more-personal-comments-resume?page=${this.currentPage}&size=${this.size}`;
            default:
                // Для товаров
                return `/${this.postType}/${this.hashId}/load-more-personal-comments?page=${this.currentPage}&size=${this.size}`;
        }
    }

    /**
     * Возвращает URL для удаления комментария
     */
    getDeleteUrl(commentId) {
        switch(this.postType) {
            case 'postidviewsjob':
                return `/${this.postType}/${this.hashId}/commentPersonalJob/${commentId}/delete`;
            case 'postidviewsvac':
                return `/${this.postType}/${this.hashId}/commentPersonalVac/${commentId}/delete`;
            case 'postidviewsres':
                return `/${this.postType}/${this.hashId}/commentPersonalRes/${commentId}/delete`;
            default:
                return `/${this.postType}/${this.hashId}/commentPersonal/${commentId}/delete`;
        }
    }

    /**
     * Возвращает URL для удаления ветки комментариев
     */
    getDeleteBranchUrl(commentId) {
        switch(this.postType) {
            case 'postidviewsjob':
                return `/${this.postType}/${this.hashId}/commentPersonalJob/${commentId}/deleteBranch`;
            case 'postidviewsvac':
                return `/${this.postType}/${this.hashId}/commentPersonalVac/${commentId}/deleteBranch`;
            case 'postidviewsres':
                return `/${this.postType}/${this.hashId}/commentPersonalRes/${commentId}/deleteBranch`;
            default:
                return `/${this.postType}/${this.hashId}/commentPersonal/${commentId}/deleteBranch`;
        }
    }

    init() {
        console.log(`Initializing LoadMoreCommentsPagination for type: ${this.postType}`);
        this.initLoadButton();
        this.setupTabListener();
    }

    setupTabListener() {
        // Слушаем переключение на вкладку личных комментариев
        const privateTab = document.getElementById('private-tab');
        if (privateTab) {
            privateTab.addEventListener('shown.bs.tab', () => {
                console.log('Private tab shown');
                if (!this.initialized) {
                    this.loadInitialData();
                    this.initialized = true;
                }
            });
        }

        // Если вкладка уже активна при загрузке (например, из sessionStorage)
        setTimeout(() => {
            const activeTab = sessionStorage.getItem('activeCommentTab');
            if (activeTab === '#private-comments' && !this.initialized) {
                this.loadInitialData();
                this.initialized = true;
            }
        }, 500);
    }

    initLoadButton() {
        const button = document.getElementById(this.buttonId);
        if (button) {
            button.addEventListener('click', () => this.loadMore());
            console.log('Initialized load more button for personal comments');
        }
    }

    async loadInitialData() {
        console.log('Loading initial personal comments data');
        this.currentPage = 0;
        this.loadedIds.clear();
        this.hasMore = true;

        const container = document.getElementById(this.containerId);
        if (container) container.innerHTML = '';

        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'block';

        this.hideAllLoadedMessage();
        this.hideNoCommentsMessage();

        await this.loadMore(true);
    }

    async loadMore(isInitial = false) {
        if (this.isLoading) return;
        if (!this.hasMore && !isInitial) return;

        this.setLoadingState(true);

        try {
            // Получаем актуальный URL с текущей страницей
            const url = this.getAjaxUrl();
            console.log('Loading comments from:', url);

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            // Проверяем статус ответа
            if (response.status === 403 || response.status === 404) {
                const data = await response.json();
                if (data.error === 'post_unavailable') {
                    console.log('Post unavailable, redirecting...');
                    if (data.redirect) {
                        window.location.href = data.redirect;
                    } else {
                        window.location.href = '/';
                    }
                    return;
                }
                throw new Error(data.message || 'Ошибка загрузки');
            }

            if (!response.ok) {
                console.log(`Ошибка загрузки: ${response.status}`);
                this.setLoadingState(false);
                return;
            }

            const data = await response.json();

            if (data.status === 'success') {
                const comments = data.comments || [];

                if (comments.length > 0) {
                    this.appendComments(comments);
                    this.hasMore = data.hasMore;
                    this.currentPage++;

                    if (!data.hasMore) {
                        this.hideButton();
                        this.showAllLoadedMessage();
                    }
                } else if (isInitial) {
                    this.showNoCommentsMessage();
                    this.hasMore = false;
                    this.hideButton();
                }
            }
        } catch (error) {

        } finally {
            this.setLoadingState(false);
        }
    }

    appendComments(comments) {
        const container = document.getElementById(this.containerId);
        if (!comments?.length || !container) return;

        comments.forEach(comment => {
            if (this.loadedIds.has(comment.id)) return;

            const element = this.createCommentElement(comment);
            container.appendChild(element);
            this.loadedIds.add(comment.id);
        });

        this.initCommentHandlers();

        // Не нужно ничего дополнительно вызывать,
        // делегирование событий работает автоматически!
    }

    createCommentElement(comment) {
        const commentDiv = document.createElement('div');
        commentDiv.className = 'comment-item mb-4';
        commentDiv.id = `comment-${comment.id}`;
        commentDiv.dataset.commentId = comment.id;

        const authorName = comment.author.nameuse || 'Пользователь';
        const authorId = comment.author.id || 0;
        const isSuperUser = comment.isSuperUser || this.hasOnlySuperAccess;
        const isPostOwner = comment.isPostOwner || false;
        const isOnline = comment.author.online || false;

        // Определяем, показывать ли ID автора
        const showAuthorId = (isSuperUser || isPostOwner) && authorId;

        // HTML для статуса онлайн/офлайн
        const onlineStatusHtml = isOnline
            ? '<span class="status-online ms-2"><i class="fas fa-circle"></i><small>онлайн</small></span>'
            : '<span class="status-offline ms-2"><i class="fas fa-circle"></i><small>офлайн</small></span>';

        let commentHtml = `
        <div class="comment-card private-comment mb-3">
            <div class="comment-header d-flex justify-content-between align-items-start">
                <div>
                    <span class="comment-user fw-bold">${this.escapeHtml(authorName)}</span>
                    ${showAuthorId ? `<small class="text-muted ms-2">ID: ${authorId}</small>` : ''}
                    ${onlineStatusHtml}
                    <small class="comment-date text-muted ms-2">
                        <i class="fas fa-clock me-1"></i> ${comment.formattedDate}
                    </small>
                </div>
                <button class="btn btn-sm btn-outline-secondary toggle-branch-btn"
                        id="toggle-btn-${comment.id}"
                        data-comment-id="${comment.id}"
                        aria-expanded="false">
                    <i class="fas fa-chevron-down" id="toggle-icon-${comment.id}"></i>
                </button>
            </div>
            <div class="comment-content mt-2">
                <p class="mb-0">${this.escapeHtml(comment.text)}</p>
            </div>
            <div class="comment-actions mt-2">
    <button class="btn btn-sm btn-outline-primary reply-btn"
            data-comment-id="${comment.id}"
            data-nameuse="${this.escapeHtml(authorName)}"
            data-comment-text="${comment.text.replace(/"/g, '&quot;')}">
        <i class="fas fa-reply me-1"></i> Ответить
    </button>`;

        // Кнопки удаления для администраторов или владельца поста/комментария
        if (this.hasSuperAccess || this.isAdminUser) {
            const deleteUrl = this.getDeleteUrl(comment.id);
            const deleteBranchUrl = this.getDeleteBranchUrl(comment.id);

            commentHtml += `
                <button type="button" class="btn btn-sm btn-danger delete-comment-btn ms-2"
                        data-delete-url="${deleteUrl}"
                        data-csrf="${this.csrfToken}">
                    <i class="fas fa-trash me-1"></i> Удалить
                </button>
                <button type="button" class="btn btn-sm btn-warning delete-branch-btn ms-1"
                        data-delete-url="${deleteBranchUrl}"
                        data-csrf="${this.csrfToken}">
                    <i class="fas fa-trash-alt me-1"></i> Удалить ветку
                </button>`;
        }

        commentHtml += `</div></div>
            <div class="comment-replies ms-4 mt-2" id="replies-${comment.id}" style="display: none;">`;

        if (comment.replies?.length > 0) {
            comment.replies.forEach(reply => {
                commentHtml += this.createReplyElement(reply);
            });
        }

        commentHtml += `</div>`;
        commentDiv.innerHTML = commentHtml;
        return commentDiv;
    }

    createReplyElement(reply) {
        const authorName = reply.author.nameuse || 'Пользователь';
        const authorId = reply.author.id || 0;
        const isSuperUser = reply.isSuperUser || this.hasOnlySuperAccess;
        const isOnline = reply.author.online || false;

        const onlineStatusHtml = isOnline
            ? '<span class="status-online ms-2"><i class="fas fa-circle"></i><small>онлайн</small></span>'
            : '<span class="status-offline ms-2"><i class="fas fa-circle"></i><small>офлайн</small></span>';

        let replyHtml = `
        <div class="comment-card private-comment reply-comment mb-2" id="reply-${reply.id}">
            <div class="comment-header">
                <span class="comment-user fw-bold">${this.escapeHtml(authorName)}</span>
                ${isSuperUser && authorId ? `<small class="text-muted ms-2">ID: ${authorId}</small>` : ''}
                ${onlineStatusHtml}
                <small class="comment-date text-muted ms-2">
                    <i class="fas fa-clock me-1"></i> ${reply.formattedDate}
                </small>
            </div>
            <div class="comment-content mt-2">
                <p class="mb-0">${this.escapeHtml(reply.text)}</p>
            </div>
            <div class="comment-actions mt-2">
    <button class="btn btn-sm btn-outline-primary reply-btn"
            data-comment-id="${reply.parentId || reply.id}"
            data-nameuse="${this.escapeHtml(authorName)}"
            data-comment-text="${reply.text.replace(/"/g, '&quot;')}">
        <i class="fas fa-reply me-1"></i> Ответить
    </button>`;

        if (this.hasSuperAccess || this.isAdminUser) {
            const deleteUrl = this.getDeleteUrl(reply.id);
            replyHtml += `
                <button type="button" class="btn btn-sm btn-danger delete-comment-btn ms-2"
                        data-delete-url="${deleteUrl}"
                        data-csrf="${this.csrfToken}">
                    <i class="fas fa-trash me-1"></i> Удалить
                </button>`;
        }

        replyHtml += `</div></div>`;
        return replyHtml;
    }

    initCommentHandlers() {
        // Проверяем, существуют ли глобальные функции
        if (typeof window.handleReplyClick === 'function') {
            const replyButtons = document.querySelectorAll('.reply-btn');
            replyButtons.forEach(button => {
                button.removeEventListener('click', window.handleReplyClick);
                button.addEventListener('click', window.handleReplyClick);
            });
        } else {
            console.warn('handleReplyClick is not defined globally');
        }

        if (typeof window.handleToggleClick === 'function') {
            const toggleButtons = document.querySelectorAll('.toggle-branch-btn');
            toggleButtons.forEach(button => {
                button.removeEventListener('click', window.handleToggleClick);
                button.addEventListener('click', window.handleToggleClick);
            });
        } else {
            console.warn('handleToggleClick is not defined globally');
        }

        // Добавляем обработчики для кнопок удаления
        const deleteCommentBtns = document.querySelectorAll('.delete-comment-btn');
        deleteCommentBtns.forEach(button => {
            button.removeEventListener('click', (e) => this.handleDeleteCommentClick(e));
            button.addEventListener('click', (e) => this.handleDeleteCommentClick(e));
        });

        const deleteBranchBtns = document.querySelectorAll('.delete-branch-btn');
        deleteBranchBtns.forEach(button => {
            button.removeEventListener('click', (e) => this.handleDeleteBranchClick(e));
            button.addEventListener('click', (e) => this.handleDeleteBranchClick(e));
        });
    }

    /**
     * Обработчик для кнопки удаления комментария
     */
    handleDeleteCommentClick(event) {
        event.preventDefault();
        const button = event.currentTarget;
        const deleteUrl = button.getAttribute('data-delete-url');
        const csrfToken = button.getAttribute('data-csrf');

        // Создаем временную форму для отправки
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = deleteUrl;
        form.style.display = 'none';

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);

        document.body.appendChild(form);

        // Показываем модальное окно
        this.showDeleteCommentModal(form);
    }

    /**
     * Обработчик для кнопки удаления ветки комментариев
     */
    handleDeleteBranchClick(event) {
        event.preventDefault();
        const button = event.currentTarget;
        const deleteUrl = button.getAttribute('data-delete-url');
        const csrfToken = button.getAttribute('data-csrf');

        // Создаем временную форму для отправки
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = deleteUrl;
        form.style.display = 'none';

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);

        document.body.appendChild(form);

        // Показываем модальное окно
        this.showDeleteBranchModal(form);
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    setLoadingState(loading) {
        this.isLoading = loading;
        const button = document.getElementById(this.buttonId);
        if (button) {
            button.disabled = loading;
            button.innerHTML = loading ?
                '<i class="fas fa-spinner fa-spin me-2"></i> Загрузка...' :
                '<i class="fas fa-plus-circle me-2"></i> Показать еще комментарии';
        }
    }

    hideButton() {
        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'none';
    }

    showAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'block';
    }

    hideAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'none';
    }

    showNoCommentsMessage() {
        const messageDiv = document.getElementById(this.noCommentsId);
        if (messageDiv) messageDiv.style.display = 'block';
    }

    hideNoCommentsMessage() {
        const messageDiv = document.getElementById(this.noCommentsId);
        if (messageDiv) messageDiv.style.display = 'none';
    }

    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');
        const input = document.querySelector('input[name="_csrf"]');
        return input ? input.value : '';
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Проверяем, что мы на одной из страниц с комментариями
    if (window.location.pathname.includes('/postidviews/') ||
        window.location.pathname.includes('/postidviewsjob/') ||
        window.location.pathname.includes('/postidviewsvac/') ||
        window.location.pathname.includes('/postidviewsres/')) {
        window.commentsPagination = new LoadMoreCommentsPagination();
    }
});