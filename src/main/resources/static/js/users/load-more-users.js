class LoadMorePaginationUsers {
    constructor() {
        // Проверяем, существует ли уже экземпляр
        if (window.loadMorePaginationInstance) {
            return window.loadMorePaginationInstance;
        }

        this.currentPage = 0;
        this.isLoading = false;
        this.hasMore = true;
        this.container = document.querySelector('tbody');
        this.loadMoreBtn = document.getElementById('loadMoreBtn');
        this.loadMoreUser = document.getElementById('loadMoreUser');
        this.loadingSpinner = document.getElementById('loadingSpinner');
        this.allLoadedMessage = document.getElementById('allLoadedMessage');
        this.searchTerm = this.getSearchTerm();
        this.existingUserIds = this.getExistingUserIds();

        // Храним ссылки на инициализированные модальные окна
        this.modalInstances = new Map();

        this.init();
        this.observeUrlChanges();
        this.initializeEventListeners();

        // Инициализируем модальные окна для существующих строк
        this.initializeExistingModals();

        // Сохраняем экземпляр в глобальной переменной
        window.loadMorePaginationInstance = this;
    }

    getSearchTerm() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('search') || '';
    }

    getExistingUserIds() {
        const ids = new Set();
        const existingRows = this.container.querySelectorAll('tr[data-user-id]');
        existingRows.forEach(row => {
            const userId = row.getAttribute('data-user-id');
            if (userId) {
                ids.add(userId.toString());
            }
        });
        return ids;
    }

    observeUrlChanges() {
        this.lastUrl = window.location.href;

        document.addEventListener('click', () => {
            setTimeout(() => {
                if (window.location.href !== this.lastUrl) {
                    this.resetPagination();
                    this.lastUrl = window.location.href;
                }
            }, 100);
        });

        window.addEventListener('popstate', () => {
            this.resetPagination();
            this.lastUrl = window.location.href;
        });
    }

    resetPagination() {
        this.hasMore = true;
        this.currentPage = 0;
        this.searchTerm = this.getSearchTerm();
        this.existingUserIds = this.getExistingUserIds();

        // Очищаем модальные окна
        this.modalInstances.clear();

        if (this.loadMoreUser) {
            this.loadMoreUser.style.display = 'block';
            this.loadMoreUser.style.textAlign = 'center';
            this.loadMoreUser.style.width = '100%';
        }

        if (this.allLoadedMessage) {
            this.allLoadedMessage.style.display = 'none';
        }
    }

    init() {
        if (this.loadMoreBtn) {
            // Удаляем предыдущие обработчики перед добавлением нового
            this.loadMoreBtn.removeEventListener('click', this.loadMoreHandler);
            this.loadMoreHandler = () => this.loadMore();
            this.loadMoreBtn.addEventListener('click', this.loadMoreHandler);
        }
    }

    // Инициализация модальных окон для существующих строк
    initializeExistingModals() {
        const modalElements = document.querySelectorAll('.modal');
        modalElements.forEach(modalElement => {
            if (!this.modalInstances.has(modalElement.id)) {
                try {
                    const modal = new bootstrap.Modal(modalElement, {
                        backdrop: true,
                        keyboard: true,
                        focus: true
                    });
                    this.modalInstances.set(modalElement.id, modal);
                } catch {
                    console.log('Error initializing modal');
                }
            }
        });
    }

    initializeEventListeners() {
        // Обработчик для удаления из черного списка
        document.removeEventListener('click', this.removeFromBlacklistHandler);
        this.removeFromBlacklistHandler = (e) => {
            if (e.target.closest('.btn-remove-blacklist')) {
                const button = e.target.closest('.btn-remove-blacklist');
                const userId = button.getAttribute('data-user-id');
                if (userId) {
                    this.removeFromBlacklist(userId);
                }
            }
        };
        document.addEventListener('click', this.removeFromBlacklistHandler);

        // Обработчик для кнопок закрытия модальных окон
        document.removeEventListener('click', this.closeModalHandler);
        this.closeModalHandler = (e) => {
            if (e.target.classList.contains('btn-close') ||
                e.target.classList.contains('btn-secondary')) {
                const modalElement = e.target.closest('.modal');
                if (modalElement) {
                    const modalId = modalElement.id;
                    const modalInstance = this.modalInstances.get(modalId);
                    if (modalInstance) {
                        modalInstance.hide();
                    } else {
                        // Если экземпляр не найден, скрываем напрямую
                        modalElement.classList.remove('show');
                        modalElement.style.display = 'none';
                        document.body.classList.remove('modal-open');
                        const backdrop = document.querySelector('.modal-backdrop');
                        if (backdrop) {
                            backdrop.remove();
                        }
                    }
                }
            }
        };
        document.addEventListener('click', this.closeModalHandler);
    }

    async showBlockReason(userId) {
        try {
            const response = await fetch(`/users/${userId}/block-info`);
            const data = await response.json();

            if (data.status === 'success') {
                if (data.isBlocked) {
                    // Показываем информацию о блокировке
                    const modalHtml = `
                    <div class="modal fade" id="blockReasonModal${userId}" tabindex="-1">
                        <div class="modal-dialog">
                            <div class="modal-content">
                                <div class="modal-header bg-warning">
                                    <h5 class="modal-title">Информация о блокировке</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body">
                                    <div class="alert alert-warning">
                                        <strong>Пользователь заблокирован</strong>
                                    </div>
                                    
                                    <table class="table table-sm">
                                        <tr>
                                            <th>Причина:</th>
                                            <td>${data.reason || 'Не указана'}</td>
                                        </tr>
                                        <tr>
                                            <th>Дата блокировки:</th>
                                            <td>${data.blockedAt}</td>
                                        </tr>
                                        <tr>
                                            <th>Разблокировка:</th>
                                            <td>${data.blockUntil}</td>
                                        </tr>
                                        <tr>
                                            <th>Осталось дней:</th>
                                            <td><strong>${data.remainingDays}</strong></td>
                                        </tr>
                                        <tr>
                                            <th>Номер блокировки:</th>
                                            <td>${data.blockCount}</td>
                                        </tr>
                                    </table>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
                                </div>
                            </div>
                        </div>
                    </div>
                `;

                    // Удаляем старое модальное окно если есть
                    const oldModal = document.getElementById(`blockReasonModal${userId}`);
                    if (oldModal) oldModal.remove();

                    // Добавляем новое
                    document.body.insertAdjacentHTML('beforeend', modalHtml);

                    // Показываем модальное окно
                    const modal = new bootstrap.Modal(document.getElementById(`blockReasonModal${userId}`));
                    modal.show();
                } else {
                    alert('Пользователь не заблокирован');
                }
            }
        } catch {
            console.log('Ошибка загрузки информации о блокировке');
            alert('Ошибка при загрузке информации о блокировке');
        }
    }

    async loadMore() {
        if (this.isLoading || !this.hasMore) return;

        this.setLoadingState(true);
        const nextPage = this.currentPage + 1;

        try {
            const url = this.buildUrl(nextPage);
            const response = await fetch(url);

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            if (data.users && Array.isArray(data.users)) {
                if (data.users.length > 0) {
                    const newUsersAdded = this.appendUsers(data.users);
                    this.hasMore = data.hasMore !== undefined ? data.hasMore : false;
                    this.currentPage = data.currentPage !== undefined ? data.currentPage : nextPage;

                    if (!this.hasMore || newUsersAdded === 0) {
                        this.hideLoadMoreButton();
                    }
                } else {
                    this.hasMore = false;
                    this.hideLoadMoreButton();
                }
            } else {
                console.error('Invalid response format');
                this.showError('Некорректный формат ответа от сервера');
            }

        } catch {
            console.log('Ошибка загрузки');
            this.showError('Произошла ошибка при загрузке пользователей. Попробуйте еще раз.');
        } finally {
            this.setLoadingState(false);
        }
    }

    buildUrl(page) {
        const baseUrl = '/users/load-more';
        const params = new URLSearchParams();

        if (this.searchTerm) {
            params.append('search', this.searchTerm);
        }

        params.append('page', page);
        params.append('size', 5);
        params.append('sort', 'newest');

        return baseUrl + '?' + params.toString();
    }

    appendUsers(users) {
        if (!users || users.length === 0) {
            this.hasMore = false;
            this.hideLoadMoreButton();
            return 0;
        }

        let newUsersAdded = 0;
        users.forEach(user => {
            if (user && user.id && !this.existingUserIds.has(user.id.toString())) {
                const userRow = this.createUserRow(user);
                this.container.appendChild(userRow);
                this.existingUserIds.add(user.id.toString());
                newUsersAdded++;

                // Инициализируем Bootstrap модальные окна для новой строки
                this.initializeModalsForRow(userRow);
            }
        });

        return newUsersAdded;
    }

    createUserRow(user) {
        const row = document.createElement('tr');
        row.setAttribute('data-user-id', user.id);
        row.setAttribute('data-user-hash-id', user.hashId);
        row.innerHTML = this.generateUserRowHTML(user);
        return row;
    }

    generateUserRowHTML(user) {
        const statusBadge = user.active
            ? '<span class="badge bg-success">Активен</span>'
            : `<span class="badge bg-danger">Заблокирован</span>`;

        const actionButtonsHTML = this.generateActionButtonsHTML(user);
        const modalsHTML = this.generateModalsHTML(user);

        return `
<td class="user-name">
    <a href="/users/${user.hashId}/page_two/admin_edit_user" class="user-link">${user.nameuse || ''}</a>
    <div class="mobile-user-info d-md-none">
        ${user.email ? `<div class="mobile-email"><small class="text-muted">${user.email}</small></div>` : ''}
        ${user.phone ? `<div class="mobile-phone"><small class="text-muted">${user.phone}</small></div>` : ''}
    </div>
</td>
<td class="d-none d-md-table-cell">${user.phone || ''}</td>
<td class="d-none d-lg-table-cell">${user.phonePost || ''}</td>
<td class="d-none d-sm-table-cell">${user.email || ''}</td>
<td class="role-column">
    <span class="badge bg-secondary">${user.role || 'ROLE_USER'}</span>
</td>
<td class="status-column">
    ${statusBadge}
</td>
<td class="actions-column">
    <div class="action-buttons">
        ${actionButtonsHTML}
    </div>
    ${modalsHTML}
</td>
`;
    }

    generateActionButtonsHTML(user) {
        let html = '';

        // Блокировка/разблокировка
        if (user.active) {
            const nextBlockCount = (user.blockCount || 0) + 1;
            let nextDuration;
            switch (nextBlockCount) {
                case 1: nextDuration = 1; break;
                case 2: nextDuration = 5; break;
                case 3: nextDuration = 15; break;
                default: nextDuration = 30;
            }

            html += `
            <button type="button" class="btn btn-warning btn-sm action-btn" 
                    data-bs-toggle="modal" data-bs-target="#blockModal${user.id}"
                    title="Заблокировать на ${nextDuration} дней (Блокировка №${nextBlockCount})">
                <span class="d-none d-sm-inline">Заблокировать (${nextDuration} дн.)</span>
                <span class="d-sm-none">🚫 ${nextDuration}д</span>
            </button>`;
        } else {
            html += `
            <form action="/users/${user.id}/toggle" method="post" class="d-inline">
                ${this.generateCsrfInput()}
                <button type="submit" class="btn btn-success btn-sm action-btn">
                    <span class="d-none d-sm-inline">Разблокировать</span>
                    <span class="d-sm-none">✅</span>
                </button>
                
        <button type="button" class="btn btn-info btn-sm action-btn" 
                onclick="window.loadMorePaginationInstance.showBlockReason(${user.id})"
                title="Просмотреть причину блокировки">
            <span class="d-none d-sm-inline">ℹ️ Причина</span>
            <span class="d-sm-none">ℹ️</span>
        </button>
            </form>`;
        }

        // Кнопки для черного списка
        if (user.inBlacklist) {
            html += `
            <form action="/users/${user.id}/toggle-blacklist" method="post" class="d-inline">
                ${this.generateCsrfInput()}
                <button type="submit" class="btn btn-success btn-sm action-btn">
                    <span class="d-none d-sm-inline">Удалить из ЧС</span>
                    <span class="d-sm-none">📋</span>
                </button>
            </form>`;
        } else {
            html += `
            <button type="button" class="btn btn-dark btn-sm action-btn"
                    data-bs-toggle="modal" data-bs-target="#blacklistModal${user.id}">
                <span class="d-none d-sm-inline">В ЧС</span>
                <span class="d-sm-none">⛔</span>
            </button>`;
        }

        // Кнопка удаления
        if (user.deletable !== false) {
            html += `
            <button type="button" class="btn btn-danger btn-sm action-btn"
                    data-bs-toggle="modal" data-bs-target="#deleteModal${user.id}">
                <span class="d-none d-sm-inline">Удалить</span>
                <span class="d-sm-none">🗑️</span>
            </button>`;
        }

        // Кнопка сброса счетчика блокировок
        if (user.blockCount > 0 && user.active) {
            html += `
            <button type="button" class="btn btn-info btn-sm action-btn"
                    data-bs-toggle="modal" data-bs-target="#resetBlockCountModal${user.id}"
                    title="Сбросить счетчик блокировок">
                <span class="d-none d-sm-inline">Сбросить счетчик</span>
                <span class="d-sm-none">🔄</span>
            </button>`;
        }

        return html;
    }

    generateCsrfInput() {
        const csrfToken = this.getCsrfToken();
        return csrfToken ? `<input type="hidden" name="_csrf" value="${csrfToken}">` : '';
    }

    generateModalsHTML(user) {
        const csrfInput = this.generateCsrfInput();
        const nextBlockCount = (user.blockCount || 0) + 1;
        let nextDuration;
        switch (nextBlockCount) {
            case 1: nextDuration = 1; break;
            case 2: nextDuration = 5; break;
            case 3: nextDuration = 15; break;
            default: nextDuration = 30;
        }

        return `
<!-- Модальное окно блокировки -->
<div class="modal fade" id="blockModal${user.id}" tabindex="-1" aria-labelledby="blockModalLabel${user.id}" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="blockModalLabel${user.id}">Блокировка пользователя</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form action="/users/${user.id}/toggle" method="post">
                ${csrfInput}
                <div class="modal-body">
                    <p>Пользователь: <strong>${user.nameuse}</strong></p>
                    <p>Email: <strong>${user.email}</strong></p>
                    <p>Телефон: <strong>${user.phone}</strong></p>
                    <p>Текущее количество блокировок: <strong>${user.blockCount || 0}</strong></p>
                    <p>Длительность этой блокировки: <strong>${nextDuration} дней</strong></p>
                    <div class="mb-3">
                        <label for="blockReason${user.id}" class="form-label">Причина блокировки:</label>
                        <textarea class="form-control" id="blockReason${user.id}" name="blockReason"
                                rows="3" required placeholder="Укажите причину блокировки пользователя"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                    <button type="submit" class="btn btn-warning">Заблокировать</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Модальное окно удаления -->
<div class="modal fade" id="deleteModal${user.id}" tabindex="-1" aria-labelledby="deleteModalLabel${user.id}" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="deleteModalLabel${user.id}">Удаление пользователя</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form action="/users/${user.id}/delete" method="post">
                ${csrfInput}
                <div class="modal-body">
                    <p>Вы уверены, что хотите удалить пользователя: <strong>${user.nameuse}</strong>?</p>
                    <p>Email: <strong>${user.email}</strong></p>
                    <p>Телефон: <strong>${user.phone}</strong></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                    <button type="submit" class="btn btn-danger">Удалить</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Модальное окно черного списка -->
<div class="modal fade" id="blacklistModal${user.id}" tabindex="-1" aria-labelledby="blacklistModalLabel${user.id}" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="blacklistModalLabel${user.id}">Добавление в черный список</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form action="/users/${user.id}/blacklist" method="post">
                ${csrfInput}
                <div class="modal-body">
                    <p>Пользователь: <strong>${user.nameuse}</strong></p>
                    <p>Email: <strong>${user.email}</strong></p>
                    <p>Телефон: <strong>${user.phone}</strong></p>
                    <div class="mb-3">
                        <label for="blacklistReason${user.id}" class="form-label">Причина добавления в черный список:</label>
                        <textarea class="form-control" id="blacklistReason${user.id}" name="reason"
                                rows="3" required placeholder="Укажите причину добавления в черный список"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                    <button type="submit" class="btn btn-dark">Добавить в ЧС</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Модальное окно сброса счетчика блокировок -->
<div class="modal fade" id="resetBlockCountModal${user.id}" tabindex="-1" aria-labelledby="resetBlockCountModalLabel${user.id}" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="resetBlockCountModalLabel${user.id}">Сброс счетчика блокировок</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form action="/users/${user.id}/reset-block-count" method="post">
                ${csrfInput}
                <div class="modal-body">
                    <div class="alert alert-warning">
                        <strong>Вы собираетесь сбросить счетчик блокировок</strong>
                    </div>
                    <p>Пользователь: <strong>${user.nameuse}</strong></p>
                    <p>Текущее количество блокировок: <strong>${user.blockCount || 0}</strong></p>
                    <p>После сброса следующая блокировка будет считаться как первая (1 день)</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                    <button type="submit" class="btn btn-info">Сбросить счетчик</button>
                </div>
            </form>
        </div>
    </div>
</div>
`;
    }

    // Инициализация модальных окон для строки
    initializeModalsForRow(row) {
        const modalElements = row.querySelectorAll('.modal');
        modalElements.forEach(modalElement => {
            if (!this.modalInstances.has(modalElement.id)) {
                try {
                    const modal = new bootstrap.Modal(modalElement, {
                        backdrop: true,
                        keyboard: true,
                        focus: true
                    });
                    this.modalInstances.set(modalElement.id, modal);

                    // Добавляем обработчик для очистки формы при закрытии
                    modalElement.addEventListener('hidden.bs.modal', () => {
                        const textareas = modalElement.querySelectorAll('textarea');
                        textareas.forEach(textarea => {
                            textarea.value = '';
                        });
                    });
                } catch {
                    console.log('Error initializing modal');
                }
            }
        });
    }

    // Удаление из черного списка
    async removeFromBlacklist(userId) {
        if (!confirm('Вы уверены, что хотите удалить пользователя из черного списка?')) {
            return;
        }

        try {
            const csrfToken = this.getCsrfToken();
            const formData = new FormData();
            formData.append('_csrf', csrfToken);

            const response = await fetch(`/users/${userId}/toggle-blacklist`, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-CSRF-Token': csrfToken
                }
            });

            if (response.ok) {
                window.location.reload();
            } else {
                throw new Error('Ошибка при удалении из черного списка');
            }
        } catch {
            console.log('Ошибка');
            alert('Ошибка при удалении из черного списка');
        }
    }

    getCsrfToken() {
        const metaToken = document.querySelector('meta[name="_csrf"]')?.content;
        if (metaToken) return metaToken;

        const inputToken = document.querySelector('input[name="_csrf"]')?.value;
        if (inputToken) return inputToken;

        const headerToken = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (headerToken) {
            const name = headerToken.replace('-', '_');
            return this.getCookie(name);
        }

        return null;
    }

    getCookie(name) {
        const value = `; ${document.cookie}`;
        const parts = value.split(`; ${name}=`);
        if (parts.length === 2) return parts.pop().split(';').shift();
        return null;
    }

    setLoadingState(loading) {
        this.isLoading = loading;

        if (this.loadMoreBtn) {
            this.loadMoreBtn.style.display = loading ? 'none' : 'block';
        }

        if (this.loadingSpinner) {
            this.loadingSpinner.style.display = loading ? 'block' : 'none';
        }
    }

    hideLoadMoreButton() {
        if (this.loadMoreUser) {
            this.loadMoreUser.style.display = 'none';
            this.loadMoreUser.style.textAlign = 'center';
            this.loadMoreUser.style.width = '100%';
        }

        if (this.allLoadedMessage) {
            this.allLoadedMessage.style.display = 'block';
        }
    }

    showError(message) {
        alert(message);
    }
}

// Инициализация когда DOM загружен - с проверкой существующего экземпляра
document.addEventListener('DOMContentLoaded', function() {
    const loadMoreContainer = document.getElementById('loadMoreUser');
    if (loadMoreContainer && !window.loadMorePaginationInstance) {
        const pagination = new LoadMorePaginationUsers();

        // Добавляем стиль для центрирования кнопки
        if (loadMoreContainer) {
            loadMoreContainer.style.textAlign = 'center';
            loadMoreContainer.style.width = '100%';

            const loadMoreBtn = document.getElementById('loadMoreBtn');
            if (loadMoreBtn) {
                const wrapper = document.createElement('div');
                wrapper.style.display = 'inline-block';
                wrapper.style.margin = '0 auto';
                loadMoreBtn.parentNode.insertBefore(wrapper, loadMoreBtn);
                wrapper.appendChild(loadMoreBtn);
            }
        }
    }
});