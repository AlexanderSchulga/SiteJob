class LoadMoreBlockedPagination {
    constructor() {
        // Конфигурация для разных типов контента
        this.config = {
            products: {
                containerId: 'productsContainer',
                buttonId: 'loadMoreProductsBtn',
                buttonContainerId: 'loadMoreProductsContainer',
                loadedMessageId: 'allLoadedProductsMessage',
                noDataMessageId: 'noProductsMessage',
                type: 'products',
                badgeId: 'productsBadge',
                countId: 'blockedProductsCount',
                colorClass: 'btn-primary',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: true,
                totalCount: 0
            },
            services: {
                containerId: 'servicesContainer',
                buttonId: 'loadMoreServicesBtn',
                buttonContainerId: 'loadMoreServicesContainer',
                loadedMessageId: 'allLoadedServicesMessage',
                noDataMessageId: 'noServicesMessage',
                type: 'services',
                badgeId: 'servicesBadge',
                countId: 'blockedServicesCount',
                colorClass: 'btn-success',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                totalCount: 0
            },
            vacancies: {
                containerId: 'vacanciesContainer',
                buttonId: 'loadMoreVacanciesBtn',
                buttonContainerId: 'loadMoreVacanciesContainer',
                loadedMessageId: 'allLoadedVacanciesMessage',
                noDataMessageId: 'noVacanciesMessage',
                type: 'vacancies',
                badgeId: 'vacanciesBadge',
                countId: 'blockedVacanciesCount',
                colorClass: 'btn-danger',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                totalCount: 0
            },
            resume: {
                containerId: 'resumeContainer',
                buttonId: 'loadMoreResumeBtn',
                buttonContainerId: 'loadMoreResumeContainer',
                loadedMessageId: 'allLoadedResumeMessage',
                noDataMessageId: 'noResumeMessage',
                type: 'resume',
                badgeId: 'resumeBadge',
                countId: 'blockedResumeCount',
                colorClass: 'btn-danger',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                totalCount: 0
            }
        };

        this.globalLoadingSpinner = document.getElementById('loadingSpinner');
        this.csrfToken = this.getCsrfToken();
        this.activeTab = 'products';
        this.loadedTabs = new Set();

        // Поисковые параметры
        this.searchQuery = '';
        this.searchType = 'all';
        this.isSearchMode = false;

        this.init();
    }

    init() {
        console.log('Initializing LoadMoreBlockedPagination with Slice...');

        this.initTabHandlers();
        this.initLoadButtons();
        this.initSearchHandlers();
        this.initModals();

        setTimeout(() => {
            this.loadInitialData(this.activeTab);
        }, 100);
    }

    initModals() {
        this.unblockModal = document.getElementById('unblockModal');
        this.deleteModal = document.getElementById('deleteModal');
        this.unblockForm = document.getElementById('unblockForm');
        this.deleteForm = document.getElementById('deleteForm');
        this.unblockItemInfo = document.getElementById('unblockItemInfo');
        this.deleteItemInfo = document.getElementById('deleteItemInfo');

        if (typeof bootstrap !== 'undefined') {
            if (this.unblockModal) {
                this.unblockModalInstance = new bootstrap.Modal(this.unblockModal);
            }
            if (this.deleteModal) {
                this.deleteModalInstance = new bootstrap.Modal(this.deleteModal);
            }
        }

        console.log('Modals initialized');
    }

    initSearchHandlers() {
        const searchInput = document.getElementById('searchInput');
        const searchButton = document.getElementById('searchButton');
        const clearButton = document.getElementById('clearSearchButton');
        const searchTypeSelect = document.getElementById('searchType');

        if (searchButton) {
            searchButton.addEventListener('click', () => this.performSearch());
        }

        if (clearButton) {
            clearButton.addEventListener('click', () => this.clearSearch());
        }

        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.performSearch();
                }
            });
        }

        if (searchTypeSelect) {
            searchTypeSelect.addEventListener('change', () => {
                this.searchType = searchTypeSelect.value;
            });
        }
    }

    performSearch() {
        const searchInput = document.getElementById('searchInput');
        const query = searchInput ? searchInput.value.trim() : '';

        if (query.length < 2) {
            this.showError('Введите минимум 2 символа для поиска');
            return;
        }


        this.searchQuery = query;
        this.isSearchMode = true;

        const config = this.config[this.activeTab];
        config.currentPage = 0;
        config.hasMore = true;
        config.loadedIds.clear();

        const container = document.getElementById(config.containerId);
        if (container) {
            container.innerHTML = '';
        }

        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) {
            buttonContainer.style.display = 'block';
        }

        this.loadMore(this.activeTab, true);
        this.showSearchInfo();
    }

    clearSearch() {
        console.log('Clearing search');

        this.searchQuery = '';
        this.isSearchMode = false;

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = '';
        }

        const searchTypeSelect = document.getElementById('searchType');
        if (searchTypeSelect) {
            searchTypeSelect.value = 'all';
            this.searchType = 'all';
        }

        this.hideSearchInfo();
        this.resetTabState(this.activeTab);
    }

    resetTabState(type) {
        const config = this.config[type];
        if (!config) return;

        config.currentPage = 0;
        config.hasMore = true;
        config.isLoading = false;
        config.loadedIds.clear();

        const container = document.getElementById(config.containerId);
        if (container) {
            container.innerHTML = '';
        }

        const button = document.getElementById(config.buttonId);
        if (button) {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-plus-circle me-2"></i> Показать еще';
            button.dataset.currentPage = '0';
        }

        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) {
            buttonContainer.style.display = 'block';
        }

        const messageDiv = document.getElementById(config.loadedMessageId);
        if (messageDiv) {
            messageDiv.style.display = 'none';
        }

        const noDataMessage = document.getElementById(config.noDataMessageId);
        if (noDataMessage) {
            noDataMessage.style.display = 'none';
        }
    }

    showSearchInfo() {
        const searchInfo = document.getElementById('searchResultsInfo');
        const searchText = document.getElementById('searchResultsText');

        if (searchInfo && searchText) {
            const typeDisplay = {
                'all': 'по всем полям',
                'title': 'по заголовку',
                'nameuse': 'по имени автора'
            };

            searchText.innerHTML = `<strong>Поиск:</strong> "${this.searchQuery}" (${typeDisplay[this.searchType] || 'по всем полям'})`;
            searchInfo.style.display = 'block';
        }
    }

    hideSearchInfo() {
        const searchInfo = document.getElementById('searchResultsInfo');
        if (searchInfo) {
            searchInfo.style.display = 'none';
        }
    }

    initTabHandlers() {
        const tabButtons = document.querySelectorAll('#categoryTab button[data-bs-toggle="tab"]');

        tabButtons.forEach(button => {
            button.addEventListener('shown.bs.tab', (event) => {
                const targetTab = event.target.getAttribute('data-type');

                this.activeTab = targetTab;
                this.config[this.activeTab].isActive = true;

                Object.keys(this.config).forEach(type => {
                    if (type !== targetTab) {
                        this.config[type].isActive = false;
                    }
                });


                if (!this.loadedTabs.has(targetTab)) {
                    this.loadInitialData(targetTab);
                    this.loadedTabs.add(targetTab);
                } else {
                    setTimeout(() => {
                        this.restoreTabState(targetTab);
                    }, 100);
                }
            });
        });
    }

    restoreTabState(type) {
        const config = this.config[type];
        if (!config) return;


        config.isActive = true;

        const container = document.getElementById(config.containerId);
        const items = container ? container.querySelectorAll(`[data-type="${type}"]`) : [];
        const itemsCount = items.length;

        config.loadedIds.clear();
        items.forEach(item => {
            const hiddenInput = item.querySelector('.item-numeric-id');
            if (hiddenInput) {
                const numericId = hiddenInput.value;
                config.loadedIds.add(Number(numericId));
            }
        });

        const loadedPages = Math.ceil(itemsCount / 8);
        config.currentPage = loadedPages;


        const button = document.getElementById(config.buttonId);
        if (button) {
            button.dataset.currentPage = config.currentPage;
        }

        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) {
            if (!config.hasMore || itemsCount < 8) {
                buttonContainer.style.display = 'none';
                this.showAllLoadedMessage(type);
            } else {
                buttonContainer.style.display = 'block';
            }
        }
    }

    initLoadButtons() {
        Object.values(this.config).forEach(config => {
            const button = document.getElementById(config.buttonId);
            if (button) {
                button.addEventListener('click', () => this.loadMore(config.type));
            }
        });
    }

    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');

        const input = document.querySelector('input[name="_csrf"]');
        if (input) return input.value;

        return '';
    }

    async loadInitialData(type) {

        this.loadedTabs.add(type);
        await this.loadMore(type, true);
    }

    async loadMore(type, isInitial = false) {
        const config = this.config[type];

        if (!isInitial && !config.isActive) {

            return;
        }

        if (config.isLoading) {

            return;
        }


        const button = document.getElementById(config.buttonId);
        const container = document.getElementById(config.containerId);
        const buttonContainer = document.getElementById(config.buttonContainerId);
        const noDataMessage = document.getElementById(config.noDataMessageId);

        if (isInitial) {
            config.currentPage = 0;
            config.loadedIds.clear();
            config.hasMore = true;

            if (container) {
                container.innerHTML = '';

            }

            if (noDataMessage) {
                noDataMessage.style.display = 'none';
            }

            if (buttonContainer) {
                buttonContainer.style.display = 'block';

            }

            const messageDiv = document.getElementById(config.loadedMessageId);
            if (messageDiv) {
                messageDiv.style.display = 'none';
            }
        }

        if (!config.hasMore && !isInitial) {

            return;
        }
        if (!container) {

            return;
        }


        this.setLoadingState(button, true, type);

        try {
            const url = this.buildUrl(type, config.currentPage);

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': this.csrfToken
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();


            if (data.status === 'success') {
                const items = data.redact || [];


                if (items.length > 0) {
                    const newItems = [];
                    items.forEach(item => {
                        if (!config.loadedIds.has(item.id)) {
                            config.loadedIds.add(item.id);
                            newItems.push(item);
                        }
                    });

                    if (newItems.length > 0) {
                        this.appendData(type, newItems);

                    } else {
                        console.log(`No new type on page  - all items are duplicates`);
                    }

                    config.hasMore = data.hasMore;
                    config.currentPage = data.currentPage + 1;

                    if (button) {
                        button.dataset.currentPage = config.currentPage;
                    }

                    if (!data.hasMore) {
                        this.hideButton(type);
                        this.showAllLoadedMessage(type);

                    }
                } else {

                    if (isInitial) {
                        this.showNoDataMessage(type);
                        this.hideButton(type);
                    }

                    config.hasMore = false;
                    this.hideButton(type);
                    this.showAllLoadedMessage(type);
                }
            } else {

                this.showError(data.message || 'Ошибка сервера');

                if (isInitial) {
                    this.showErrorMessage(type, 'Ошибка загрузки данных');
                }
            }

        } catch (error) {

            this.showError('Ошибка при загрузке: ' + error.message);

            if (isInitial) {
                this.showErrorMessage(type, 'Не удалось загрузить данные');
            }
        } finally {
            this.setLoadingState(button, false, type);
        }
    }

    buildUrl(type, page) {
        let baseUrl = '/page_two/redactorblocked/load-more';
        const params = new URLSearchParams();

        params.append('page', page);
        params.append('size', 8);
        params.append('type', type);

        if (this.isSearchMode && this.searchQuery) {
            params.append('searchQuery', this.searchQuery);
            params.append('searchType', this.searchType);
        }

        return baseUrl + '?' + params.toString();
    }

    async refreshStatistics() {
        console.log('Refreshing blocked statistics...');

        try {
            const response = await fetch('/page_two/redactorblocked/statistics', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'X-CSRF-TOKEN': this.csrfToken
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const statistics = await response.json();

            Object.keys(this.config).forEach(type => {
                const count = statistics[type] || 0;
                this.updateStatistics(type, count);
            });

        } catch {
            console.log('Error refreshing statistics:');
        }
    }

    appendData(type, items) {
        const config = this.config[type];
        const container = document.getElementById(config.containerId);

        if (!items || items.length === 0) {
            return;
        }

        if (!container) {
            return;
        }

        items.forEach(item => {
            try {
                const element = this.createItemElement(type, item);
                container.appendChild(element);

            } catch {
                console.log('Error creating elemen');
            }
        });

        const newElements = container.querySelectorAll(`[data-type="${type}"]:last-child`);
        newElements.forEach(el => {
            el.classList.add('new-item');
            setTimeout(() => el.classList.remove('new-item'), 2000);
        });

        const noDataMessage = document.getElementById(config.noDataMessageId);
        if (noDataMessage) {
            noDataMessage.style.display = 'none';
        }
    }

    createItemElement(type, item) {

        const col = document.createElement('div');
        col.className = 'col-md-6 col-lg-4 col-xl-3 mb-4';
        col.setAttribute('data-id', item.id);
        col.setAttribute('data-type', type);

        const numericIdInput = `<input type="hidden" class="item-numeric-id" value="${item.id}">`;

        let detailLink = '';
        let unblockAction = '';
        let deleteAction = '';

        const hashId = item.hashId || item.id;

        switch(type) {
            case 'products':
                detailLink = `/postidviews/${hashId}`;
                unblockAction = `/page_two/redactorblocked/${item.id}/toggle-block`;
                deleteAction = `/page_two/redactorblocked/${item.id}/remove`;
                break;
            case 'services':
                detailLink = `/postidviewsjob/${hashId}`;
                unblockAction = `/page_two/redactorblocked/job/${item.id}/toggle-block`;
                deleteAction = `/page_two/redactorblocked/job/${item.id}/remove`;
                break;
            case 'vacancies':
                detailLink = `/postidviewsvac/${hashId}`;
                unblockAction = `/page_two/redactorblocked/vac/${item.id}/toggle-block`;
                deleteAction = `/page_two/redactorblocked/vac/${item.id}/remove`;
                break;
            case 'resume':
                detailLink = `/postidviewsres/${hashId}`;
                unblockAction = `/page_two/redactorblocked/res/${item.id}/toggle-block`;
                deleteAction = `/page_two/redactorblocked/res/${item.id}/remove`;
                break;
        }

        const formatDate = (dateString) => {
            if (!dateString) return 'Не указано';
            try {
                const date = new Date(dateString);
                return date.toLocaleDateString('ru-RU', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                });
            } catch (e) {
                return 'Не указано';
            }
        };

        const getDeleteDate = (blockedAt) => {
            if (!blockedAt) return 'Неизвестно';
            try {
                const deleteDate = new Date(blockedAt);
                deleteDate.setDate(deleteDate.getDate() + 5);
                return deleteDate.toLocaleDateString('ru-RU', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric'
                });
            } catch (e) {
                return 'Неизвестно';
            }
        };

        const formatPrice = (priceValue) => {
            if (priceValue === null || priceValue === undefined || priceValue === '') {
                return 'Цена не указана';
            }
            const num = Number(priceValue);
            if (isNaN(num)) return String(priceValue);
            return num.toLocaleString('ru-RU') + ' ₽';
        };

        let priceDisplay = '';
        if (type === 'vacancies' || type === 'resume') {
            priceDisplay = formatPrice(item.cash);
        } else {
            priceDisplay = formatPrice(item.price);
        }

        const userName = item.user ? (item.user.nameuse || item.user.username || 'Неизвестный') : 'Неизвестный';
        const username = item.user ? (item.user.username || '') : '';

        const imageHtml = item.firstImagePath ?
            `<img src="${item.firstImagePath}" class="post-image img-fluid" alt="${item.title || ''}" loading="lazy">` :
            '';

        const titleText = item.title ? this.escapeHtml(item.title) : 'Без названия';

        const itemTypeDisplay = {
            'products': 'товар',
            'services': 'услугу',
            'vacancies': 'вакансию',
            'resume': 'резюме'
        }[type] || 'объявление';

        const itemTypeGenitive = {
            'products': 'товара',
            'services': 'услуги',
            'vacancies': 'вакансии',
            'resume': 'резюме'
        }[type] || 'объявления';

        col.innerHTML = `
        <div class="post-card blocked position-relative">
            ${numericIdInput}
            <div class="blocked-banner position-absolute top-0 start-0 w-100 text-center p-2">
                <span class="badge bg-danger">Заблокировано</span>
                <span class="badge bg-danger ms-1">
                    Будет удален: ${getDeleteDate(item.blockedAt)}
                </span>
            </div>

            <div class="post-content pt-5">
                ${imageHtml}
                
                <h5 class="post-title">${titleText}</h5>
                
                <div class="post-user-info mb-2">
                    ${username ? `<small class="text-muted d-block">
                        <strong>Login:</strong> ${this.escapeHtml(username)}
                    </small>` : ''}
                    <small class="text-muted d-block">
                        <strong>Автор:</strong> ${this.escapeHtml(userName)}
                    </small>
                    
                    <small class="text-muted d-block">
                        <strong>Заблокирован:</strong> ${formatDate(item.blockedAt)}
                    </small>
                </div>
                
                <div class="action-buttons">
                    <a href="${detailLink}" class="btn btn-primary btn-mod" title="Просмотр">
                        <i class="fas fa-eye"></i>
                        <span class="d-none d-md-inline"> Детальнее</span>
                    </a>
                    
                    <button class="btn btn-success btn-mod unblock-btn" 
                            data-action="${unblockAction}"
                            data-item-id="${item.id}"
                            data-item-title="${this.escapeHtml(item.title || 'Без названия')}"
                            data-item-type="${itemTypeDisplay}"
                            data-item-type-genitive="${itemTypeGenitive}"
                            title="Разблокировать">
                        <i class="fas fa-unlock"></i>
                        <span class="d-none d-md-inline"> Разблок.</span>
                    </button>
                    
                    <button class="btn btn-danger btn-mod delete-btn"
                            data-action="${deleteAction}"
                            data-item-id="${item.id}"
                            data-item-title="${this.escapeHtml(item.title || 'Без названия')}"
                            data-item-type="${itemTypeDisplay}"
                            data-item-type-genitive="${itemTypeGenitive}"
                            title="Удалить">
                        <i class="fas fa-trash"></i>
                        <span class="d-none d-md-inline"> Удалить</span>
                    </button>
                </div>
            </div>
        </div>
    `;

        setTimeout(() => {
            const unblockBtn = col.querySelector('.unblock-btn');
            const deleteBtn = col.querySelector('.delete-btn');

            if (unblockBtn) {
                unblockBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.showUnblockModal(unblockBtn);
                });
            }

            if (deleteBtn) {
                deleteBtn.addEventListener('click', (e) => {
                    e.preventDefault();
                    this.showDeleteModal(deleteBtn);
                });
            }
        }, 100);

        return col;
    }

    showUnblockModal(button) {
        const action = button.getAttribute('data-action');
        const itemTitle = button.getAttribute('data-item-title');
        const itemType = button.getAttribute('data-item-type');

        if (!this.unblockModalInstance || !this.unblockForm || !this.unblockItemInfo) {
            return;
        }

        this.unblockForm.setAttribute('action', action);

        let csrfInput = this.unblockForm.querySelector('input[name="_csrf"]');
        if (!csrfInput) {
            csrfInput = document.createElement('input');
            csrfInput.setAttribute('type', 'hidden');
            csrfInput.setAttribute('name', '_csrf');
            csrfInput.setAttribute('value', this.csrfToken);
            this.unblockForm.appendChild(csrfInput);
        }

        this.unblockItemInfo.innerHTML = `Вы уверены, что хотите <strong>разблокировать</strong> ${itemType} <strong>"${itemTitle}"</strong>?`;
        this.unblockModalInstance.show();
    }

    showDeleteModal(button) {
        const action = button.getAttribute('data-action');
        const itemTitle = button.getAttribute('data-item-title');
        const itemType = button.getAttribute('data-item-type');

        if (!this.deleteModalInstance || !this.deleteForm || !this.deleteItemInfo) {
            return;
        }

        this.deleteForm.setAttribute('action', action);

        let csrfInput = this.deleteForm.querySelector('input[name="_csrf"]');
        if (!csrfInput) {
            csrfInput = document.createElement('input');
            csrfInput.setAttribute('type', 'hidden');
            csrfInput.setAttribute('name', '_csrf');
            csrfInput.setAttribute('value', this.csrfToken);
            this.deleteForm.appendChild(csrfInput);
        }

        this.deleteItemInfo.innerHTML = `Вы уверены, что хотите <strong>удалить</strong> ${itemType} <strong>"${itemTitle}"</strong>? Это действие нельзя отменить!`;
        this.deleteModalInstance.show();
    }

    refreshCurrentTab() {
        const config = this.config[this.activeTab];
        config.currentPage = 0;
        config.loadedIds.clear();
        config.hasMore = true;

        const container = document.getElementById(config.containerId);
        if (container) {
            container.innerHTML = '';
        }

        this.refreshStatistics().then(() => {
            this.loadMore(this.activeTab, true);
        });
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    updateStatistics(type, count) {
        const config = this.config[type];

        const badge = document.getElementById(config.badgeId);
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'inline-block' : 'none';

        }

        const countElement = document.getElementById(config.countId);
        if (countElement) {
            countElement.textContent = count;
        }

        config.totalCount = count;
    }

    setLoadingState(button, loading, type) {
        const config = this.config[type];
        config.isLoading = loading;

        if (button) {
            button.disabled = loading;
            button.innerHTML = loading ?
                '<i class="fas fa-spinner fa-spin me-2"></i> Загрузка...' :
                '<i class="fas fa-plus-circle me-2"></i> Показать еще';
        }

        if (this.globalLoadingSpinner) {
            this.globalLoadingSpinner.style.display = loading ? 'block' : 'none';
        }
    }

    hideButton(type) {
        const config = this.config[type];
        const buttonContainer = document.getElementById(config.buttonContainerId);

        if (buttonContainer) {
            buttonContainer.style.display = 'none';
        }
    }

    showAllLoadedMessage(type) {
        const config = this.config[type];
        const messageDiv = document.getElementById(config.loadedMessageId);

        if (messageDiv) {
            messageDiv.style.display = 'block';

        }
    }

    showNoDataMessage(type) {
        const config = this.config[type];
        const container = document.getElementById(config.containerId);

        if (container && container.children.length === 0) {
            const message = document.createElement('div');
            message.className = 'col-12 text-center py-4';
            message.innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle me-2"></i>
                    Нет ${this.getTypeDisplayName(type)} для отображения
                </div>
            `;
            container.appendChild(message);

        }
    }

    showErrorMessage(type, message) {
        const config = this.config[type];
        const container = document.getElementById(config.containerId);

        if (container) {
            const errorDiv = document.createElement('div');
            errorDiv.className = 'col-12 text-center py-4';
            errorDiv.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fas fa-exclamation-triangle me-2"></i>
                    ${message}
                </div>
                <button class="btn btn-sm btn-outline-secondary mt-2" onclick="location.reload()">
                    <i class="fas fa-redo me-1"></i> Обновить страницу
                </button>
            `;
            container.appendChild(errorDiv);
        }
    }

    getTypeDisplayName(type) {
        switch(type) {
            case 'products': return 'заблокированных товаров';
            case 'services': return 'заблокированных услуг';
            case 'vacancies': return 'заблокированных вакансий';
            case 'resume': return 'заблокированных резюме';
            default: return 'данных';
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
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing blocked pagination with Slice...');
    try {
        window.blockedPagination = new LoadMoreBlockedPagination();
        console.log('Blocked pagination initialized successfully');
    } catch {

        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger m-3';
        errorDiv.innerHTML = `
            <strong>Ошибка загрузки!</strong> Не удалось инициализировать загрузку данных. 
            <button class="btn btn-sm btn-outline-secondary ms-2" onclick="location.reload()">
                Обновить страницу
            </button>
        `;

        const container = document.querySelector('.container');
        if (container) {
            container.insertBefore(errorDiv, container.firstChild);
        }
    }
});

if (typeof window !== 'undefined') {
    window.LoadMoreBlockedPagination = LoadMoreBlockedPagination;
}