class LoadMoreSupportPagination {
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

        this.category = document.getElementById('selectedCategory')?.value ||
            new URLSearchParams(window.location.search).get('category');
        this.csrfToken = this.getCsrfToken();

        this.currentUserNameuse = this.getCurrentUserNameuse();
        this.currentUserRole = this.getCurrentUserRole();

        this.isSuperOrAdmin = this.checkSuperOrAdminAccess();
        this.isSuperOnly = this.currentUserRole === 'ROLE_SUPER';
        this.isAdminOnly = this.currentUserRole === 'ROLE_ADMIN';


        this.init();
    }


    // Показать модальное окно подтверждения
    showConfirmModal(message, onConfirm, isDangerAction = true) {
        const modalElement = document.getElementById('confirmActionModal');
        const modalBody = document.getElementById('confirmActionModalBody');
        const confirmButton = document.getElementById('confirmActionButton');
        const modalTitle = document.getElementById('confirmActionModalLabel');

        // Устанавливаем сообщение
        modalBody.textContent = message;

        // Настраиваем кнопку подтверждения в зависимости от типа действия
        if (isDangerAction) {
            confirmButton.className = 'btn btn-danger';
            confirmButton.textContent = 'Удалить';
            modalTitle.textContent = 'Подтверждение удаления';
        } else {
            confirmButton.className = 'btn btn-warning';
            confirmButton.textContent = 'Освободить';
            modalTitle.textContent = 'Подтверждение освобождения';
        }

        // Создаем экземпляр модального окна Bootstrap
        const modal = new bootstrap.Modal(modalElement);

        // Удаляем предыдущие обработчики
        const newConfirmButton = confirmButton.cloneNode(true);
        confirmButton.parentNode.replaceChild(newConfirmButton, confirmButton);

        // Добавляем новый обработчик
        newConfirmButton.addEventListener('click', function() {
            onConfirm();
            modal.hide();
        });

        // Показываем модальное окно
        modal.show();

    }

    // Получение nameuse текущего пользователя
    getCurrentUserNameuse() {
        // Пробуем получить из скрытого поля
        const currentUserData = document.getElementById('currentUserData');
        if (currentUserData) {
            const nameuse = currentUserData.getAttribute('data-user-nameuse');
            if (nameuse) return nameuse;
        }

        // Пробуем получить из других мест
        const userNameElements = document.querySelectorAll('[data-user-nameuse]');
        for (const element of userNameElements) {
            const nameuse = element.getAttribute('data-user-nameuse');
            if (nameuse) return nameuse;
        }

        // Пробуем получить из HTML
        const userSpan = document.querySelector('.user-name');
        if (userSpan && userSpan.textContent) {
            return userSpan.textContent.trim();
        }

        console.warn('User nameuse not found, using fallback');
        return 'Пользователь';
    }

    getCurrentUserRole() {
        const currentUserData = document.getElementById('currentUserData');
        if (currentUserData) {
            return currentUserData.getAttribute('data-user-role');
        }
        return null;
    }

    // Проверка прав доступа SUPER и ADMIN
    checkSuperOrAdminAccess() {
        // Проверяем различные способы передачи данных о правах
        const bodyIsSuper = document.body.hasAttribute('data-is-super') === 'true' ||
            document.body.hasAttribute('data-is-admin') === 'true';

        const headerElement = document.querySelector('header');
        const elementIsSuper = headerElement?.getAttribute('data-is-super') === 'true' ||
            headerElement?.getAttribute('data-is-admin') === 'true';

        const roleElements = document.querySelectorAll('[data-user-role]');
        for (const element of roleElements) {
            const role = element.getAttribute('data-user-role');
            if (role === 'ROLE_SUPER' || role === 'ROLE_ADMIN') {
                return true;
            }
        }
        // Проверяем скрытые поля
        const currentUserData = document.getElementById('currentUserData');
        if (currentUserData) {
            const userRole = currentUserData.getAttribute('data-user-role');
            if (userRole === 'ROLE_SUPER' || userRole === 'ROLE_ADMIN') {
                return true;
            }
        }

        return bodyIsSuper || elementIsSuper;
    }


    init() {
        console.log('Initializing LoadMoreSupportPagination...');

        // Скрываем ID пользователя
        this.currentUserId = null;
        this.currentUserRole = null;

        // Проверяем доступы через атрибуты
        const currentUserData = document.getElementById('currentUserData');
        if (currentUserData) {
            this.currentUserRole = currentUserData.getAttribute('data-user-role');

            // Только для SUPER показываем ID
            if (this.currentUserRole === 'ROLE_SUPER') {
                this.currentUserId = currentUserData.getAttribute('data-user-id');
            }
        }

        this.initLoadButton();
        this.loadCategoryStatus();

        setTimeout(() => {
            this.loadInitialData();
        }, 100);

        this.hideStaticComments();
    }

    initLoadButton() {
        const button = document.getElementById(this.buttonId);
        if (!button) {
            const container = document.getElementById('comments-section');
            if (container) {
                const buttonContainer = document.createElement('div');
                buttonContainer.id = this.buttonContainerId;
                buttonContainer.className = 'text-center mt-4';
                buttonContainer.innerHTML = `
                    <button id="${this.buttonId}" class="btn btn-primary" data-current-page="0">
                        <i class="fas fa-plus-circle me-2"></i>
                        Показать еще сообщения
                    </button>
                `;
                container.appendChild(buttonContainer);

                const loadedMessage = document.createElement('div');
                loadedMessage.id = this.loadedMessageId;
                loadedMessage.className = 'text-center mt-4 mb-5';
                loadedMessage.style.display = 'none';
                loadedMessage.innerHTML = `
                    <p class="text-muted">
                        <i class="fas fa-check-circle text-success me-2"></i>
                        Все сообщения загружены
                    </p>
                `;
                container.appendChild(loadedMessage);

                const noCommentsMessage = document.createElement('div');
                noCommentsMessage.id = this.noCommentsId;
                noCommentsMessage.className = 'no-comments text-center py-5';
                noCommentsMessage.style.display = 'none';
                noCommentsMessage.innerHTML = `
                    <div class="card border-0 bg-light">
                        <div class="card-body py-5">
                            <i class="fas fa-comments fa-4x text-muted mb-4"></i>
                            <h4 class="text-muted mb-3">Сообщений пока нет</h4>
                            <p class="text-muted mb-0">Будьте первым, кто оставит сообщение в этой категории</p>
                        </div>
                    </div>
                `;
                container.appendChild(noCommentsMessage);

                document.getElementById(this.buttonId).addEventListener('click', () => this.loadMore());
            }
        } else {
            button.addEventListener('click', () => this.loadMore());
        }
        console.log('Initialized load more button for support');
    }

    //Загрузка статуса категории
    async loadCategoryStatus() {
        try {
            const response = await fetch('/api/support/notificationStatus');
            if (!response.ok) return;

            const data = await response.json();

            // Обновляем подсветку категорий на основе статуса
            this.updateCategoryHighlights(data);
        } catch {
            console.log('Error loading category status');
        }
    }

    //Обновление подсветки категорий
    updateCategoryHighlights(data) {
        const categoryCards = document.querySelectorAll('.category-card');

        categoryCards.forEach(card => {
            const category = card.getAttribute('data-category');
            let shouldHighlight = false;

            if (this.isSuperOrAdmin) {
                // Для SUPER/ADMIN: подсвечиваем если есть невзятые тикеты в этой категории
                if (data.categoriesWithUnassigned &&
                    data.categoriesWithUnassigned.includes(category)) {
                    shouldHighlight = true;
                }
            } else {
                // Для пользователя: подсвечиваем если есть непрочитанные ответы в этой категории
                if (data.categoriesWithUnreadReplies &&
                    data.categoriesWithUnreadReplies.includes(category)) {
                    shouldHighlight = true;
                }
            }

            // Обновляем стиль карточки
            if (shouldHighlight) {
                card.classList.add('border-warning');
                card.classList.add('category-highlight');

                // Добавляем иконку уведомления если её нет
                if (!card.querySelector('.fa-bell')) {
                    const titleElement = card.querySelector('.card-title');
                    if (titleElement) {
                        const bellIcon = document.createElement('i');
                        bellIcon.className = 'fas fa-bell text-warning ms-2';
                        titleElement.appendChild(bellIcon);
                    }
                }
            } else {
                card.classList.remove('border-warning');
                card.classList.remove('category-highlight');

                // Удаляем иконку уведомления
                const bellIcon = card.querySelector('.fa-bell');
                if (bellIcon) {
                    bellIcon.remove();
                }
            }
        });
    }

    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');

        const input = document.querySelector('input[name="_csrf"]');
        if (input) return input.value;

        const csrfInput = document.querySelector('[name^="_csrf"]');
        if (csrfInput) return csrfInput.value;

        console.warn('CSRF token not found');
        return '';
    }

    async loadInitialData() {
        console.log('Loading initial support data');
        this.currentPage = 0;
        this.loadedIds.clear();
        this.hasMore = true;

        let container = document.getElementById(this.containerId);
        if (!container) {
            const commentsSection = document.querySelector('.comments-section');
            if (commentsSection) {
                container = document.createElement('div');
                container.id = this.containerId;
                commentsSection.insertBefore(container, commentsSection.firstChild);
            }
        }

        if (container) {
            container.innerHTML = '';
        }

        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) {
            buttonContainer.style.display = 'block';
        }

        this.hideAllLoadedMessage();
        this.hideNoCommentsMessage();

        await this.loadMore(true);
    }

    async loadMore(isInitial = false) {
        if (this.isLoading) {
            console.log('Already loading, skipping');
            return;
        }

        if (!this.hasMore && !isInitial) {
            console.log('No more comments to load');
            return;
        }

        if (!this.category) {
            this.showError('Не выбрана категория');
            return;
        }

        console.log(`Loading comments, page`);

        this.setLoadingState(true);

        try {
            const url = this.buildUrl(this.currentPage);
            console.log('Fetching URL');

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('Response:');

            // Сохраняем данные о пользователе из ответа
            if (data.currentUserRole) {
                this.currentUserRole = data.currentUserRole;
            }
            if (data.currentUserId) {
                this.currentUserId = data.currentUserId;
            }

            console.log('API Response data');

            if (data.status === 'success') {
                const comments = data.comments || [];
                console.log(`Loaded comments (filtered)`);

                if (isInitial && comments.length === 0) {
                    this.showNoCommentsMessage();
                    this.hasMore = false;
                    this.hideButton();
                    return;
                }

                if (comments.length > 0) {
                    this.appendComments(comments);

                    // Используем data.hasMore из Slice вместо totalPages
                    this.hasMore = data.hasMore;
                    this.currentPage = this.currentPage + 1;

                    const button = document.getElementById(this.buttonId);
                    if (button) {
                        button.dataset.currentPage = this.currentPage;
                    }

                    if (!data.hasMore) {
                        this.hideButton();
                        this.showAllLoadedMessage();
                        console.log('No more comments to load');
                    }
                } else {
                    console.log('No comments on this page');

                    if (isInitial) {
                        this.showNoCommentsMessage();
                    }

                    this.hasMore = false;
                    this.hideButton();
                    this.showAllLoadedMessage();
                }
            } else {
                console.error('Server error');
                this.showError(data.message || 'Ошибка сервера');

                if (isInitial) {
                    this.showErrorMessage('Ошибка загрузки сообщений');
                }
            }

        } catch {
            console.log('Ошибка загрузки:');
            this.showError('Ошибка при загрузке');

            if (isInitial) {
                this.showErrorMessage('Не удалось загрузить сообщения');
            }
        } finally {
            this.setLoadingState(false);
        }
    }

    buildUrl(page) {
        const baseUrl = '/api/support/comments';
        const params = new URLSearchParams();

        params.append('category', this.category);
        params.append('page', page);
        params.append('size', this.size);

        return baseUrl + '?' + params.toString();
    }

    appendComments(comments) {
        let container = document.getElementById(this.containerId);

        if (!container) {
            const commentsSection = document.querySelector('.comments-section');
            if (commentsSection) {
                container = document.createElement('div');
                container.id = this.containerId;
                commentsSection.insertBefore(container, commentsSection.firstChild);
            }
        }

        if (!comments || comments.length === 0) {
            console.log('No comments to append');
            return;
        }

        if (!container) {
            console.error(`Container not found`);
            return;
        }

        comments.forEach(comment => {
            try {
                const element = this.createCommentElement(comment);
                container.appendChild(element);
                console.log(`Added comment `);

                this.loadedIds.add(comment.id);
            } catch {
                console.log('Error creating comment element:');
            }
        });

        this.initReplyHandlers();
        this.initToggleHandlers();
    }

    createCommentElement(comment) {
        console.log(`Creating element for comment ID`);

        const isAssigned = comment.assignedSuper != null;
        const isAssignedToCurrentUser = isAssigned &&
            comment.assignedSuper.nameuse &&
            comment.assignedSuper.nameuse === this.currentUserNameuse;

        const isOwnComment = comment.user.nameuse &&
            comment.user.nameuse === this.currentUserNameuse;

        const isRootComment = !comment.parentComment;
        const isSuperOrAdminUser = comment.user.role === 'ROLE_SUPER' ||
            comment.user.role === 'ROLE_ADMIN';

// Функция для получения HTML статуса онлайн (только онлайн/офлайн, без подсказок)
        const getOnlineStatusHtml = (user) => {
            if (!user) return '';

            const isOnline = user.online === true;

            return `
    <span class="status-indicator ${isOnline ? 'status-online' : 'status-offline'}">
        <i class="fas fa-circle"></i>
        <small class="status-text">${isOnline ? 'онлайн' : 'офлайн'}</small>
    </span>
    `;
        };

        // Кто может отвечать
        const canReply = this.determineCanReply(comment, isAssigned, isAssignedToCurrentUser,
            isRootComment, isOwnComment, isSuperOrAdminUser);

        // Проверяем, нужно ли показывать статус "ожидайте"
        const shouldShowWaitingStatus =
            !this.isSuperOrAdmin &&          // Только для обычных пользователей
            isOwnComment &&                  // Это тикет пользователя
            isRootComment &&                 // Корневой тикет
            !isAssigned;                     // Тикет не в работе

        let cardClass = 'card';
        if (isAssignedToCurrentUser && this.isSuperOrAdmin) {
            cardClass += ' bg-light border-secondary';
        }

        const commentDiv = document.createElement('div');
        commentDiv.className = 'comment-card mb-3';
        commentDiv.id = `comment-${comment.id}`;
        commentDiv.dataset.commentId = comment.id;
        commentDiv.dataset.isRoot = isRootComment;

        // Сохраняем только если id существует
        if (comment.user.id) {
            commentDiv.dataset.ownerId = comment.user.id;
        }

        if (isAssignedToCurrentUser && this.isSuperOrAdmin) {
            commentDiv.classList.add('assigned-ticket');
        }

        //Добавляем класс для невзятых тикетов (только для SUPER/ADMIN)
        if (this.isSuperOrAdmin && !isAssigned && isRootComment && !isSuperOrAdminUser) {
            commentDiv.classList.add('unassigned-ticket');
            cardClass += ' border-warning';
        }

        let commentHtml = `
<div class="${cardClass}">
    <div class="card-body">
        <div class="comment-header d-flex justify-content-between mb-2">
            <div class="d-flex align-items-center">
                ${comment.replies && comment.replies.length > 0 ? `
                <button class="btn btn-sm btn-outline-secondary me-2 toggle-replies-btn"
                        data-comment-id="${comment.id}"
                        title="Свернуть/развернуть ветку">
                    <i class="fas fa-chevron-down"></i>
                    <span class="replies-count-badge">${comment.replies.length}</span>
                </button>
                ` : ''}
                <div class="user-info">
                    <span class="comment-user fw-bold ${isSuperOrAdminUser ? 'text-danger' : 'text-primary'}">
                        ${this.escapeHtml(comment.user.nameuse)}
                        ${isSuperOrAdminUser ? ` (${comment.user.role === 'ROLE_ADMIN' ? 'ADMIN' : 'SUPER'})` : ''}
                    </span>
                    ${getOnlineStatusHtml(comment.user)}
                    ${isAssigned ? `
                        <span class="badge bg-warning text-dark ms-2">
                            <i class="fas fa-user-check me-1"></i>
                             В работе: ${this.escapeHtml(comment.assignedSuper.nameuse)}
                            ${getOnlineStatusHtml(comment.assignedSuper)}
                        </span>
                      ` : ''}
                    ${(!isAssigned && this.isSuperOrAdmin && isRootComment && !isSuperOrAdminUser) ? `
                        <span class="badge bg-secondary text-white ms-2">
                                <i class="fas fa-clock me-1"></i>
                                    Не взят в работу
                        </span>
                    ` : ''}
                    <!-- НОВЫЙ СТАТУС ДЛЯ ПОЛЬЗОВАТЕЛЕЙ -->
                    ${shouldShowWaitingStatus ? `
                    <span class="badge bg-info text-white ms-2">
                        <i class="fas fa-clock me-1"></i>
                        Ожидайте ответа
                    </span>
                    ` : ''}
                </div>
            </div>
            <small class="comment-date text-muted">
                ${this.formatDate(comment.createdAt)}
            </small>
        </div>
        <p class="comment-text text-start">${this.escapeHtml(comment.text)}</p>
        
        <div class="comment-actions text-end">
`;

        if (canReply && isRootComment) {
            commentHtml += `
    <button class="btn btn-sm btn-primary reply-btn"
            data-comment-id="${comment.id}"
            data-nameuse="${this.escapeHtml(comment.user.nameuse)}"
            data-comment-text="${this.escapeHtml(comment.text)}">
        <i class="fas fa-reply me-1"></i>Ответить
    </button>
    `;
        }

        if (this.isSuperOrAdmin) {
            if (!isAssigned && isRootComment && !isSuperOrAdminUser) {
                commentHtml += `
            <form action="/support_service/${this.category}/comment/${comment.id}/assign"
                  method="post" class="d-inline-block ms-2">
                <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                <button type="submit" class="btn btn-sm btn-take-ticket">
                    <i class="fas fa-hand-paper me-1"></i> Взять тикет
                </button>
            </form>
        `;
            }
            else if (isAssigned && isRootComment) {
                commentHtml += `
            <form action="/support_service/${this.category}/comment/${comment.id}/unassign"
                  method="post" class="d-inline-block ms-2">
                <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                <button type="button" class="btn btn-sm btn-unassign unassign-btn"
                        data-action="/support_service/${this.category}/comment/${comment.id}/unassign"
                        data-ticket-id="${comment.id}"
                        data-type="unassign">
                    <i class="fas fa-hand-peace me-1"></i> Освободить
                </button>
            </form>
        `;
            }

            if (isRootComment && this.isSuperOnly) {
                commentHtml += `
            <form action="/support_service/${this.category}/comment/${comment.id}/delete"
                  method="post" class="d-inline-block ms-2">
                <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                <button type="button" class="btn btn-sm btn-danger delete-btn"
                        data-action="/support_service/${this.category}/comment/${comment.id}/delete"
                        data-ticket-id="${comment.id}"
                        data-type="delete">
                    <i class="fas fa-trash me-1"></i> Удалить ветку
                </button>
            </form>
        `;
            }

            else if (isRootComment && this.isAdminOnly && (!isAssigned || isAssignedToCurrentUser)) {
                commentHtml += `
            <form action="/support_service/${this.category}/comment/${comment.id}/delete"
                  method="post" class="d-inline-block ms-2">
                <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                <button type="button" class="btn btn-sm btn-danger delete-btn"
                        data-action="/support_service/${this.category}/comment/${comment.id}/delete"
                        data-ticket-id="${comment.id}"
                        data-type="delete">
                    <i class="fas fa-trash me-1"></i> Удалить ветку
                </button>
            </form>
        `;
            }
        }

        commentHtml += `
        </div>
    </div>
</div>
`;

        if (comment.replies && comment.replies.length > 0) {
            let replyCardClass = 'card border-start border-3 border-info';
            const isUnreadSuperReply = !comment.isRead && this.isSuperOrAdmin &&
                comment.user.nameuse && comment.user.nameuse === this.currentUserNameuse;

            if (isUnreadSuperReply) {
                replyCardClass += ' bg-light-yellow';
            }

            commentHtml += `
<div class="comment-replies ms-4 mt-2 replies-container"
     id="replies-${comment.id}"
     style="display: block;">
`;

            comment.replies.forEach(reply => {
                const isSuperOrAdminReply = reply.user.role === 'ROLE_SUPER' ||
                    reply.user.role === 'ROLE_ADMIN';

                // Для пользователя: проверяем, является ли это непрочитанным ответом SUPER/ADMIN в его тикете
                const isUnreadSuperReply = !reply.isRead && isSuperOrAdminReply &&
                    !this.isSuperOrAdmin &&
                    comment.user.nameuse &&
                    comment.user.nameuse === this.currentUserNameuse;

                // Может ли пользователь ответить на этот ответ
                const canReplyToThisReply = !this.isSuperOrAdmin &&
                    isSuperOrAdminReply &&
                    comment.user.nameuse &&
                    comment.user.nameuse === this.currentUserNameuse;

                // Определяем класс для карточки ответа
                let replyCardClass = 'card border-start border-3';
                if (isSuperOrAdminReply) {
                    replyCardClass += ' border-primary';
                } else {
                    replyCardClass += ' border-success';
                }

                if (isUnreadSuperReply) {
                    replyCardClass += ' bg-light-warning';
                }

                commentHtml += `
        <div class="comment-card reply-comment mb-2" id="reply-${reply.id}">
            <div class="${replyCardClass}">
                <div class="card-body py-2">
                    <div class="comment-header d-flex justify-content-between mb-2">
                        <div class="user-info">
                            <span class="comment-user fw-bold ${isSuperOrAdminReply ? 'text-danger' : 'text-success'}">
                                ${this.escapeHtml(reply.user.nameuse)}
                                ${isSuperOrAdminReply ? ` (${reply.user.role === 'ROLE_ADMIN' ? 'ADMIN' : 'SUPER'})` : ''}
                            </span>
                            ${getOnlineStatusHtml(reply.user)}
                        </div>
                        <small class="comment-date text-muted">
                            ${this.formatDate(reply.createdAt)}
                            ${isUnreadSuperReply ? ' <i class="fas fa-bell text-warning ms-1"></i>' : ''}
                        </small>
                    </div>
                    <p class="comment-text text-start">${this.escapeHtml(reply.text)}</p>
                    
                    <div class="comment-actions text-end">
            `;

                // Кнопка ответа на ответ для пользователя
                if (canReplyToThisReply) {
                    commentHtml += `
    <button class="btn btn-sm btn-primary reply-btn"
            data-comment-id="${comment.id}"
            data-reply-id="${reply.id}"
            data-nameuse="${this.escapeHtml(reply.user.nameuse)}"
            data-comment-text="${this.escapeHtml(reply.text)}">
        <i class="fas fa-reply me-1"></i>Ответить
    </button>
    `;
                }

                // Кнопка удаления ответа для SUPER/ADMIN (только свои ответы)
                if (this.isSuperOrAdmin && reply.user.id &&
                    reply.user.id.toString() === this.currentUserId) {
                    commentHtml += `
            <form action="/support_service/${this.category}/comment/${reply.id}/delete"
                  method="post" class="d-inline-block ms-2">
                <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                <button type="button" class="btn btn-sm btn-danger delete-btn"
                        data-action="/support_service/${this.category}/comment/${reply.id}/delete"
                        data-ticket-id="${reply.id}"
                        data-type="delete">
                    <i class="fas fa-trash me-1"></i> Удалить
                </button>
            </form>
            `;
                }

                commentHtml += `
                    </div>
                </div>
            </div>
        </div>
        `;
            });

            commentHtml += `
    </div>
    `;
        }

        commentDiv.innerHTML = commentHtml;

        // Добавляем обработчики для кнопок удаления и освобождения
        this.initActionButtons(commentDiv);

        return commentDiv;
    }

    // Инициализация обработчиков для кнопок действий
    initActionButtons(container) {
        // Обработчики для кнопок удаления
        const deleteButtons = container.querySelectorAll('.delete-btn');
        deleteButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();
                const action = button.getAttribute('data-action');
                const ticketId = button.getAttribute('data-ticket-id');

                this.showConfirmModal(
                    `Вы уверены, что хотите удалить ${ticketId ? 'тикет' : 'этот ответ'}? Это действие нельзя отменить.`,
                    () => {
                        // Создаем и отправляем форму
                        const form = button.closest('form');
                        if (form) {
                            form.submit();
                        }
                    },
                    true // опасное действие
                );
            });
        });

        // Обработчики для кнопок освобождения
        const unassignButtons = container.querySelectorAll('.unassign-btn');
        unassignButtons.forEach(button => {
            button.addEventListener('click', (e) => {
                e.preventDefault();

                this.showConfirmModal(
                    'Вы уверены, что хотите освободить этот тикет?',
                    () => {
                        const form = button.closest('form');
                        if (form) {
                            form.submit();
                        }
                    },
                    false // не опасное действие (освобождение)
                );
            });
        });
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    formatDate(dateString) {
        try {
            if (typeof dateString === 'string' && dateString.includes('.')) {
                return dateString;
            }

            const date = new Date(dateString);
            if (isNaN(date.getTime())) {
                console.warn('Invalid date:');
                return 'Дата не указана';
            }

            const day = String(date.getDate()).padStart(2, '0');
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const year = date.getFullYear();
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');

            return `${day}.${month}.${year} ${hours}:${minutes}`;
        } catch {
            return 'Дата не указана';
        }
    }

    setLoadingState(loading) {
        this.isLoading = loading;

        const button = document.getElementById(this.buttonId);
        if (button) {
            button.disabled = loading;
            button.innerHTML = loading ?
                '<i class="fas fa-spinner fa-spin me-2"></i> Загрузка...' :
                '<i class="fas fa-plus-circle me-2"></i> Показать еще сообщения';
        }
    }

    hideButton() {
        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) {
            buttonContainer.style.display = 'none';
            console.log('Hidden load more button');
        }
    }

    showAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) {
            messageDiv.style.display = 'block';
            console.log('Shown "all loaded" message');
        }
    }

    hideAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) {
            messageDiv.style.display = 'none';
        }
    }

    showNoCommentsMessage() {
        const messageDiv = document.getElementById(this.noCommentsId);
        if (messageDiv) {
            messageDiv.style.display = 'block';
            console.log('Shown "no comments" message');
        }
    }

    hideNoCommentsMessage() {
        const messageDiv = document.getElementById(this.noCommentsId);
        if (messageDiv) {
            messageDiv.style.display = 'none';
        }
    }

    showErrorMessage(message) {
        const container = document.getElementById(this.containerId);
        if (container) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'col-12 text-center py-4';
            errorDiv.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    ${message}
                </div>
            `;
            container.appendChild(errorDiv);
            console.log(`Shown error message`);
        }
    }

    showError(message) {
        document.querySelectorAll('.alert.alert-danger.position-fixed').forEach(el => el.remove());

        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger alert-dismissible fade show position-fixed top-0 end-0 m-3';
        errorDiv.style.zIndex = '9999';
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

    initReplyHandlers() {
        const replyButtons = document.querySelectorAll('.reply-btn');
        replyButtons.forEach(button => {
            const newButton = button.cloneNode(true);
            button.parentNode.replaceChild(newButton, button);

            newButton.addEventListener('click', (event) => {
                const button = event.currentTarget;
                const commentId = button.getAttribute('data-comment-id');
                const replyId = button.getAttribute('data-reply-id');
                const nameuse = button.getAttribute('data-nameuse');
                const commentText = button.getAttribute('data-comment-text');

                if (typeof replyToComment === 'function') {
                    replyToComment(commentId, nameuse, nameuse, commentText, replyId);
                } else {
                    this.handleReplyClick(event);
                }
            });
        });
    }

    initToggleHandlers() {
        const toggleButtons = document.querySelectorAll('.toggle-replies-btn');
        toggleButtons.forEach(button => {
            const newButton = button.cloneNode(true);
            button.parentNode.replaceChild(newButton, button);

            newButton.addEventListener('click', (event) => {
                if (typeof toggleReplies === 'function') {
                    toggleReplies(event.currentTarget);
                } else {
                    this.handleToggleClick(event);
                }
            });
        });
    }

    handleReplyClick(event) {
        const button = event.currentTarget;
        const commentId = button.getAttribute('data-comment-id');
        const replyId = button.getAttribute('data-reply-id');
        const nameuse = button.getAttribute('data-nameuse');
        const commentText = button.getAttribute('data-comment-text');

        if (typeof replyToComment === 'function') {
            replyToComment(commentId, nameuse, nameuse, commentText, replyId);
        }
    }

    handleToggleClick(event) {
        const button = event.currentTarget;
        const commentId = button.getAttribute('data-comment-id');
        const repliesContainer = document.getElementById('replies-' + commentId);
        const icon = button.querySelector('i');

        if (repliesContainer.style.display === 'none') {
            repliesContainer.style.display = 'block';
            icon.style.transform = 'rotate(0deg)';
        } else {
            repliesContainer.style.display = 'none';
            icon.style.transform = 'rotate(-90deg)';
        }
    }

    hideStaticComments() {
        const staticComments = document.querySelector('.comments-section .comment-card');
        if (staticComments) {
            staticComments.style.display = 'none';
        }
    }

    determineCanReply(comment, isAssigned, isAssignedToCurrentUser, isRootComment,
                      isOwnComment, isSuperOrAdminUser) {


        //SUPER пользователь может отвечать на ВСЕ тикеты (даже не взятые)
        if (this.isSuperOnly) {
            console.log('SUPER can reply to ANY ticket');
            return true;
        }

        //ADMIN пользователь может отвечать на:
        //    - Тикеты, которые он взял (isAssignedToCurrentUser)
        //    - Свои собственные тикеты (isOwnComment)
        if (this.isAdminOnly) {
            const canAdminReply = isAssignedToCurrentUser || isOwnComment;
            console.log('ADMIN can reply');
            return canAdminReply;
        }

        // Обычный пользователь может отвежать:
        //    - На свои тикеты (isOwnComment) - ВСЕГДА
        //    - На ответы SUPER/ADMIN в своем тикете (обрабатывается в другой логике)
        if (isRootComment) {
            // Пользователь всегда может отвечать на свой собственный тикет
            const canUserReply = isOwnComment;
            console.log('User can reply to ticket:');
            return canUserReply;
        }

        //Для ответов (не корневых комментариев):
        //    Пользователь может отвечать на ответы SUPER/ADMIN в своем тикете
        const canReplyToReply = isSuperOrAdminUser && isOwnComment;
        console.log('User can reply to reply:');
        return canReplyToReply;
    }
}



// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing support pagination with Slice...');

    const urlParams = new URLSearchParams(window.location.search);
    const category = urlParams.get('category');

    if (category) {
        try {
            window.supportPagination = new LoadMoreSupportPagination();
            console.log('Support pagination with Slice initialized successfully');
        } catch {
            console.log('Error initializing LoadMoreSupportPagination:');

            const errorDiv = document.createElement('div');
            errorDiv.className = 'alert alert-danger m-3';
            errorDiv.innerHTML = `
                <strong>Ошибка загрузки!</strong> Не удалось инициализировать загрузку сообщений. 
                <button class="btn btn-sm btn-outline-secondary ms-2" onclick="location.reload()">
                    Обновить страницу
                </button>
            `;

            const container = document.querySelector('.selected-category-section');
            if (container) {
                container.insertBefore(errorDiv, container.firstChild);
            }
        }
    }
});