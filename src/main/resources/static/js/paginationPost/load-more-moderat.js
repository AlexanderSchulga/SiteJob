class LoadMoreModeratPagination {
    constructor() {
        this.config = {
            products: {
                containerId: 'productsContainer',
                buttonId: 'loadMoreProductsBtn',
                buttonContainerId: 'loadMoreProductsContainer',
                loadedMessageId: 'allLoadedProductsMessage',
                noDataMessageId: 'noProductsMessage',
                type: 'products',
                badgeId: 'productsBadge',
                countId: 'moderProductsCount',
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
                countId: 'moderServicesCount',
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
                countId: 'moderVacanciesCount',
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
                countId: 'moderResumeCount',
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
        this.searchQuery = '';
        this.searchType = 'all';
        this.isSearchMode = false;
        this.initialized = false;
        this.boundHandleBeforeUnload = this.cleanup.bind(this);

        this.init();
    }

    init() {
        console.log('Initializing LoadMoreModeratPagination...');
        this.initTabHandlers();
        this.initLoadButtons();
        this.initSearchHandlers();
        this.initModalHandlers();
        window.addEventListener('beforeunload', this.boundHandleBeforeUnload);

        // Загружаем данные сразу, без setTimeout
        this.loadInitialData(this.activeTab);
    }

    cleanup() {
        console.log('Cleaning up LoadMoreModeratPagination...');
        Object.values(this.config).forEach(config => {
            if (config.loadedIds) config.loadedIds.clear();
        });
        if (this.loadedTabs) this.loadedTabs.clear();
        this.removeEventListeners();
    }

    removeEventListeners() {
        window.removeEventListener('beforeunload', this.boundHandleBeforeUnload);
        Object.values(this.config).forEach(config => {
            const button = document.getElementById(config.buttonId);
            if (button && button._clickHandler) {
                button.removeEventListener('click', button._clickHandler);
                delete button._clickHandler;
            }
        });
        const searchButton = document.getElementById('searchButton');
        if (searchButton && searchButton._clickHandler) {
            searchButton.removeEventListener('click', searchButton._clickHandler);
            delete searchButton._clickHandler;
        }
        const clearButton = document.getElementById('clearSearchButton');
        if (clearButton && clearButton._clickHandler) {
            clearButton.removeEventListener('click', clearButton._clickHandler);
            delete clearButton._clickHandler;
        }
    }

    initSearchHandlers() {
        const searchInput = document.getElementById('searchInput');
        const searchButton = document.getElementById('searchButton');
        const clearButton = document.getElementById('clearSearchButton');
        const searchTypeSelect = document.getElementById('searchType');

        if (searchButton) {
            const handler = () => this.performSearch();
            searchButton._clickHandler = handler;
            searchButton.addEventListener('click', handler);
        }
        if (clearButton) {
            const handler = () => this.clearSearch();
            clearButton._clickHandler = handler;
            clearButton.addEventListener('click', handler);
        }
        if (searchInput) {
            const handler = (e) => {
                if (e.key === 'Enter') this.performSearch();
            };
            searchInput._keypressHandler = handler;
            searchInput.addEventListener('keypress', handler);
        }
        if (searchTypeSelect) {
            const handler = () => { this.searchType = searchTypeSelect.value; };
            searchTypeSelect._changeHandler = handler;
            searchTypeSelect.addEventListener('change', handler);
        }
    }

    initModalHandlers() {
        document.body.addEventListener('click', (e) => {
            const publishBtn = e.target.closest('.publish-btn');
            if (publishBtn) {
                e.preventDefault();
                const form = publishBtn.closest('form');
                const action = form.getAttribute('action');

                const container = publishBtn.closest('.post-card');
                const premiumCheckbox = container ? container.querySelector('.premium-checkbox') : null;
                const priorityInput = container ? container.querySelector('.premium-priority') : null;
                const donateCheckbox = container ? container.querySelector('.donate-checkbox') : null;

                const isPremium = premiumCheckbox ? premiumCheckbox.checked : false;
                const premiumPriority = priorityInput ? parseInt(priorityInput.value) || 0 : 0;
                const isDonate = donateCheckbox ? donateCheckbox.checked : false;

                const postType = this.getPostTypeFromAction(action);
                this.showPublishModal(action, postType, isPremium, premiumPriority, isDonate);
            }

            const blockBtn = e.target.closest('.block-btn');
            if (blockBtn) {
                e.preventDefault();
                const action = blockBtn.getAttribute('data-action');
                const isBlocked = blockBtn.classList.contains('btn-unblock');
                const postType = this.getPostTypeFromAction(action);
                this.showBlockModal(action, isBlocked, postType);
            }

            const deleteBtn = e.target.closest('.delete-btn');
            if (deleteBtn) {
                e.preventDefault();
                const action = deleteBtn.getAttribute('data-action');
                const postType = this.getPostTypeFromAction(action);
                this.showDeleteModal(action, postType);
            }
        });
    }

    getPostTypeFromAction(action) {
        if (action.includes('/job/')) return 'услугу';
        if (action.includes('/vac/')) return 'вакансию';
        if (action.includes('/res/')) return 'резюме';
        return 'товар';
    }

    showPublishModal(action, postType, isPremium, premiumPriority, isDonate) {
        const modalBody = document.getElementById('publishModalBody');
        const modalForm = document.getElementById('publishForm');
        const modal = new bootstrap.Modal(document.getElementById('publishModal'));

        if (!modalBody || !modalForm) {
            return;
        }

        let donateHtml = '';
        if (isDonate) {
            donateHtml = `<div class="alert alert-warning mt-2">
            <i class="fas fa-heart me-1"></i> 
            <strong>Донат!</strong> Объявление будет подсвечено в ленте
        </div>`;
        }

        modalBody.innerHTML = `
    <div class="text-center">
        <i class="fas fa-check-circle fa-3x text-success mb-3"></i>
        <p>Вы уверены, что хотите <strong>опубликовать</strong> этот ${postType}?</p>
        ${isPremium ? `<div class="alert alert-info mt-3">
            <i class="fas fa-gem me-1"></i>
            <strong>Платное объявление!</strong> Приоритет показа: ${premiumPriority}
        </div>` : ''}
        ${donateHtml}
    </div>
    `;

        modalForm.setAttribute('action', action);

        // Удаляем старые скрытые поля
        const existingPremium = modalForm.querySelector('input[name="premium"]');
        const existingPriority = modalForm.querySelector('input[name="premiumPriority"]');
        const existingDonate = modalForm.querySelector('input[name="donate"]');
        const existingCsrf = modalForm.querySelector('input[name="_csrf"]');

        if (existingPremium) existingPremium.remove();
        if (existingPriority) existingPriority.remove();
        if (existingDonate) existingDonate.remove();
        if (existingCsrf) existingCsrf.remove();

        // Добавляем поле CSRF
        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = this.csrfToken;
        modalForm.appendChild(csrfInput);

        // Добавляем поле premium
        const premiumInput = document.createElement('input');
        premiumInput.type = 'hidden';
        premiumInput.name = 'premium';
        premiumInput.value = isPremium ? 'true' : 'false';
        modalForm.appendChild(premiumInput);

        // Добавляем поле premiumPriority
        const priorityInput = document.createElement('input');
        priorityInput.type = 'hidden';
        priorityInput.name = 'premiumPriority';
        priorityInput.value = premiumPriority;
        modalForm.appendChild(priorityInput);

        // ДОБАВЛЯЕМ ПОЛЕ DONATE
        const donateInput = document.createElement('input');
        donateInput.type = 'hidden';
        donateInput.name = 'donate';
        donateInput.value = isDonate ? 'true' : 'false';
        modalForm.appendChild(donateInput);

        modalForm.onsubmit = (e) => {
            e.preventDefault();

            this.submitForm(modalForm, () => {
                modal.hide();
                this.refreshCurrentTab();
            });
        };

        modal.show();
    }

    showBlockModal(action, isBlocked, postType) {
        const modalBody = document.getElementById('blockModalBody');
        const confirmBtn = document.getElementById('blockConfirmBtn');
        const modalForm = document.getElementById('blockForm');
        const modal = new bootstrap.Modal(document.getElementById('blockModal'));

        if (!modalBody || !confirmBtn || !modalForm) {
            return;
        }

        const actionText = isBlocked ? 'разблокировать' : 'заблокировать';
        modalBody.innerHTML = `
            <div class="text-center">
                <i class="fas ${isBlocked ? 'fa-lock-open' : 'fa-lock'} fa-3x ${isBlocked ? 'text-success' : 'text-warning'} mb-3"></i>
                <p>Вы уверены, что хотите <strong>${actionText}</strong> этот ${postType}?</p>
            </div>
        `;

        confirmBtn.className = isBlocked ? 'btn btn-success' : 'btn btn-warning';
        confirmBtn.textContent = isBlocked ? 'Разблокировать' : 'Заблокировать';

        modalForm.setAttribute('action', action);

        const existingCsrf = modalForm.querySelector('input[name="_csrf"]');
        if (existingCsrf) {
            existingCsrf.value = this.csrfToken;
        } else {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = this.csrfToken;
            modalForm.appendChild(csrfInput);
        }

        modalForm.onsubmit = (e) => {
            e.preventDefault();
            this.submitForm(modalForm, () => {
                modal.hide();
                this.refreshCurrentTab();
            });
        };

        modal.show();
    }

    showDeleteModal(action, postType) {
        const modalBody = document.getElementById('deletePostModalBody');
        const modalForm = document.getElementById('deletePostForm');
        const modal = new bootstrap.Modal(document.getElementById('deletePostModal'));

        if (!modalBody || !modalForm) {

            return;
        }

        modalBody.innerHTML = `
            <div class="text-center">
                <i class="fas fa-exclamation-triangle fa-3x text-danger mb-3"></i>
                <p>Вы уверены, что хотите <strong>удалить</strong> этот ${postType}?</p>
                <p class="text-danger small"><strong>Это действие нельзя отменить!</strong></p>
            </div>
        `;

        modalForm.setAttribute('action', action);

        const existingCsrf = modalForm.querySelector('input[name="_csrf"]');
        if (existingCsrf) {
            existingCsrf.value = this.csrfToken;
        } else {
            const csrfInput = document.createElement('input');
            csrfInput.type = 'hidden';
            csrfInput.name = '_csrf';
            csrfInput.value = this.csrfToken;
            modalForm.appendChild(csrfInput);
        }

        modalForm.onsubmit = (e) => {
            e.preventDefault();
            this.submitForm(modalForm, () => {
                modal.hide();
                this.refreshCurrentTab();
            });
        };

        modal.show();
    }

    submitForm(form, onSuccess) {
        fetch(form.action, {
            method: 'POST',
            body: new FormData(form),
            credentials: 'same-origin',
            headers: {
                'X-CSRF-TOKEN': this.csrfToken
            }
        }).then(response => {
            if (response.ok) {
                this.showMessage('Действие выполнено успешно', 'success');
                if (onSuccess) onSuccess();
                this.refreshStatistics();
            } else {
                this.showMessage('Ошибка при выполнении действия', 'danger');
            }
        }).catch(error => {

            this.showMessage('Ошибка при выполнении действия', 'danger');
        });
    }

    async refreshStatistics() {
        try {
            const response = await fetch('/page_two/moderat/statistics', {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'X-CSRF-TOKEN': this.csrfToken
                },
                credentials: 'same-origin'
            });
            if (response.ok) {
                const stats = await response.json();
                this.updateStatistics('products', stats.products || 0);
                this.updateStatistics('services', stats.services || 0);
                this.updateStatistics('vacancies', stats.vacancies || 0);
                this.updateStatistics('resume', stats.resume || 0);
            }
        } catch {
            console.log('Error refreshing statistics');
        }
    }

    performSearch() {
        const searchInput = document.getElementById('searchInput');
        const query = searchInput ? searchInput.value.trim() : '';
        if (query.length < 2) {
            this.showMessage('Введите минимум 2 символа для поиска', 'warning');
            return;
        }
        this.searchQuery = query;
        this.isSearchMode = true;
        const config = this.config[this.activeTab];
        config.currentPage = 0;
        config.hasMore = true;
        config.loadedIds.clear();
        const container = document.getElementById(config.containerId);
        if (container) container.innerHTML = '';
        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'block';
        this.loadMore(this.activeTab, true);
        this.showSearchInfo();
    }

    clearSearch() {
        this.searchQuery = '';
        this.isSearchMode = false;
        const searchInput = document.getElementById('searchInput');
        if (searchInput) searchInput.value = '';
        const searchTypeSelect = document.getElementById('searchType');
        if (searchTypeSelect) {
            searchTypeSelect.value = 'all';
            this.searchType = 'all';
        }
        this.hideSearchInfo();
        this.resetTabState(this.activeTab);
        // Убираем setTimeout и вызываем loadMore сразу
        this.loadMore(this.activeTab, true);
    }

    resetTabState(type) {
        const config = this.config[type];
        if (!config) return;
        config.currentPage = 0;
        config.hasMore = true;
        config.isLoading = false;
        config.loadedIds.clear();
        const container = document.getElementById(config.containerId);
        if (container) container.innerHTML = '';
        const button = document.getElementById(config.buttonId);
        if (button) {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-plus-circle me-2"></i> Показать еще';
            button.dataset.currentPage = '0';
        }
        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'block';
        const messageDiv = document.getElementById(config.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'none';
        const noDataMessage = document.getElementById(config.noDataMessageId);
        if (noDataMessage) noDataMessage.style.display = 'none';
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
        if (searchInfo) searchInfo.style.display = 'none';
    }

    initTabHandlers() {
        const tabButtons = document.querySelectorAll('#categoryTab button[data-bs-toggle="tab"]');
        tabButtons.forEach(button => {
            button.addEventListener('shown.bs.tab', (event) => {
                const targetTab = event.target.getAttribute('data-type');
                this.activeTab = targetTab;
                this.config[this.activeTab].isActive = true;
                Object.keys(this.config).forEach(type => {
                    if (type !== targetTab) this.config[type].isActive = false;
                });
                if (this.isSearchMode) {
                    const config = this.config[targetTab];
                    config.currentPage = 0;
                    config.loadedIds.clear();
                    const container = document.getElementById(config.containerId);
                    if (container) container.innerHTML = '';
                    this.loadMore(targetTab, true);
                } else if (!this.loadedTabs.has(targetTab)) {
                    this.loadInitialData(targetTab);
                } else {
                    this.restoreTabState(targetTab);
                }
            });
        });
    }

    initLoadButtons() {
        Object.values(this.config).forEach(config => {
            const button = document.getElementById(config.buttonId);
            if (button) {
                const handler = () => this.loadMore(config.type);
                button._clickHandler = handler;
                button.addEventListener('click', handler);
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
        // Проверяем, что данные еще не загружены
        if (this.loadedTabs.has(type)) {
            console.log(`Tab ${type} already loaded, restoring state`);
            this.restoreTabState(type);
            return;
        }
        console.log(`Loading initial data for ${type}`);
        this.loadedTabs.add(type);
        await this.loadMore(type, true);
    }

    restoreTabState(type) {
        const config = this.config[type];
        if (!config) return;
        config.isActive = true;
        const container = document.getElementById(config.containerId);
        const itemsCount = container ? container.querySelectorAll(`[data-type="${type}"]`).length : 0;
        if (itemsCount === 0 && this.loadedTabs.has(type)) {
            this.loadInitialData(type);
            return;
        }
        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) {
            if (!config.hasMore || itemsCount < 3) {
                buttonContainer.style.display = 'none';
                this.showAllLoadedMessage(type);
            } else {
                buttonContainer.style.display = 'block';
            }
        }
    }

    async loadMore(type, isInitial = false) {
        const config = this.config[type];
        if (!isInitial && !config.isActive) return;
        if (config.isLoading) return;

        const button = document.getElementById(config.buttonId);
        const container = document.getElementById(config.containerId);
        const buttonContainer = document.getElementById(config.buttonContainerId);
        const noDataMessage = document.getElementById(config.noDataMessageId);

        if (isInitial) {
            config.currentPage = 0;
            config.loadedIds.clear();
            config.hasMore = true;
            if (container) container.innerHTML = '';
            if (noDataMessage) noDataMessage.style.display = 'none';
            if (buttonContainer) buttonContainer.style.display = 'block';
            const messageDiv = document.getElementById(config.loadedMessageId);
            if (messageDiv) messageDiv.style.display = 'none';
        }

        if (!config.hasMore && !isInitial) return;
        if (!container) return;

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

                if (response.status === 403) {
                    this.showMessage('Доступ запрещен', 'danger');
                    if (isInitial) {
                        this.showErrorMessage(type, 'Доступ запрещен');
                    }
                } else {
                    this.showMessage('Ошибка сервера. Попробуйте позже.', 'danger');
                    if (isInitial) {
                        this.showErrorMessage(type, 'Ошибка загрузки данных');
                    }
                }
                return;
            }

            const data = await response.json();

            if (data.status === 'success') {
                const items = data.redact || [];
                if (data.totalResults !== undefined) {
                    config.totalCount = data.totalResults;
                    this.updateStatistics(type, config.totalCount);
                }
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
                    }
                    config.hasMore = data.hasMore;
                    config.currentPage = data.currentPage + 1;
                    if (button) button.dataset.currentPage = config.currentPage;
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
                }
            } else {
                console.log('Server error occurred');
                this.showMessage('Ошибка сервера. Попробуйте позже.', 'danger');
                if (isInitial) {
                    this.showErrorMessage(type, 'Ошибка загрузки данных модерации');
                }
            }
        } catch {
            console.log('Network error occurred');
            this.showMessage('Ошибка при загрузке данных. Проверьте соединение.', 'danger');
            if (isInitial) {
                this.showErrorMessage(type, 'Не удалось загрузить данные модерации');
            }
        } finally {
            this.setLoadingState(button, false, type);
        }
    }

    buildUrl(type, page) {
        let baseUrl = '/page_two/moderat/load-more';
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', 8);
        params.append('type', type);
        if (this.isSearchMode && this.searchQuery) {
            params.append('searchQuery', this.searchQuery);
            params.append('searchType', this.searchType);
        }
        params.append('_', Date.now());
        return baseUrl + '?' + params.toString();
    }

    refreshCurrentTab() {
        const config = this.config[this.activeTab];
        if (!config) return;
        config.currentPage = 0;
        config.loadedIds.clear();
        config.hasMore = true;
        const container = document.getElementById(config.containerId);
        if (container) container.innerHTML = '';
        const buttonContainer = document.getElementById(config.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'block';
        const messageDiv = document.getElementById(config.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'none';
        const noDataMessage = document.getElementById(config.noDataMessageId);
        if (noDataMessage) noDataMessage.style.display = 'none';
        this.loadMore(this.activeTab, true);
    }

    appendData(type, items) {
        const config = this.config[type];
        const container = document.getElementById(config.containerId);
        if (!items || items.length === 0 || !container) return;
        items.forEach(item => {
            try {
                const element = this.createItemElement(type, item);
                container.appendChild(element);
            } catch {
                console.log('Error creating element');
            }
        });
        const newElements = container.querySelectorAll(`[data-type="${type}"]:last-child`);
        newElements.forEach(el => {
            el.classList.add('new-item');
            setTimeout(() => el.classList.remove('new-item'), 2000);
        });
        const noDataMessage = document.getElementById(config.noDataMessageId);
        if (noDataMessage) noDataMessage.style.display = 'none';
    }

    createItemElement(type, item) {
        const col = document.createElement('div');
        col.className = 'col-md-6 col-lg-4 col-xl-3 mb-4';
        col.setAttribute('data-id', item.id);
        col.setAttribute('data-type', type);

        let detailLink = '', toggleModerLink = '', toggleBlockLink = '', deleteLink = '';
        const hashId = item.hashId || item.id;

        switch(type) {
            case 'products':
                detailLink = `/postidviews/${hashId}`;
                toggleModerLink = `/page_two/moderat/${item.hashId}/toggle-moder`;
                toggleBlockLink = `/page_two/moderat/${item.id}/toggle-block`;
                deleteLink = `/page_two/moderat/${item.id}/remove`;
                break;
            case 'services':
                detailLink = `/postidviewsjob/${hashId}`;
                toggleModerLink = `/page_two/moderat/job/${item.hashId}/toggle-moder`;
                toggleBlockLink = `/page_two/moderat/job/${item.id}/toggle-block`;
                deleteLink = `/page_two/moderat/job/${item.id}/remove`;
                break;
            case 'vacancies':
                detailLink = `/postidviewsvac/${hashId}`;
                toggleModerLink = `/page_two/moderat/vac/${item.hashId}/toggle-moder`;
                toggleBlockLink = `/page_two/moderat/vac/${item.id}/toggle-block`;
                deleteLink = `/page_two/moderat/vac/${item.id}/remove`;
                break;
            case 'resume':
                detailLink = `/postidviewsres/${hashId}`;
                toggleModerLink = `/page_two/moderat/res/${item.hashId}/toggle-moder`;
                toggleBlockLink = `/page_two/moderat/res/${item.id}/toggle-block`;
                deleteLink = `/page_two/moderat/res/${item.id}/remove`;
                break;
        }

        const formatDate = (dateString) => {
            if (!dateString) return 'Не указано';
            try {
                const date = new Date(dateString);
                return date.toLocaleDateString('ru-RU', {
                    day: '2-digit', month: '2-digit', year: 'numeric',
                    hour: '2-digit', minute: '2-digit'
                });
            } catch (e) { return 'Не указано'; }
        };

        const userName = item.user ? (item.user.nameuse || item.user.username || 'Неизвестный') : 'Неизвестный';
        const username = item.user ? (item.user.username || '') : '';

        const imageHtml = item.firstImagePath ?
            `<img src="${item.firstImagePath}" class="post-image img-fluid" alt="${item.title || ''}" loading="lazy">` :
            '<div class="post-image-placeholder"><i class="fas fa-image fa-3x text-muted"></i></div>';

        const titleText = item.title ? this.escapeHtml(item.title) : 'Без названия';

        // Для всех типов, кроме резюме, показываем premium и donate
        const showPremiumBlock = type !== 'resume';
        const isPremiumInitial = showPremiumBlock ? (item.premium === true) : false;
        const premiumPriorityInitial = showPremiumBlock ? (item.premiumPriority || 0) : 0;
        const isDonateInitial = showPremiumBlock ? (item.donate === true) : false;

        // Генерируем уникальный ID для формы
        const formId = `publish-form-${type}-${item.id}`;

        col.innerHTML = `
    <div class="post-card position-relative">
<div class="moderation-banner">
    <i class="fas fa-clock me-1"></i>МОДЕРАЦИЯ
    ${item.blocked ? '<span class="badge bg-danger ms-2">БЛОК</span>' : ''}
    ${(showPremiumBlock && isPremiumInitial) ? '<span class="badge bg-premium ms-2"><i class="fas fa-gem me-1"></i>PREMIUM</span>' : ''}
    ${(showPremiumBlock && isDonateInitial) ? '<span class="badge bg-donate ms-2"><i class="fas fa-heart me-1"></i>DONATE</span>' : ''}
</div>

        <div class="post-content">
            ${imageHtml}
            
            <h5 class="post-title" title="${titleText}">${titleText}</h5>
            
            <div class="post-user-info mb-2">
                ${username ? `<small class="text-muted d-block">
                    <strong>Логин:</strong> ${this.escapeHtml(username)}
                </small>` : ''}
                <small class="text-muted d-block">
                    <strong>Автор:</strong> ${this.escapeHtml(userName)}
                </small>
                <small class="text-muted d-block">
                    <strong>Создано:</strong> ${formatDate(item.createdAt)}
                </small>
            </div>
            
${showPremiumBlock ? `
<!-- Блок PREMIUM (карусель + приоритет) -->
<div class="premium-block">
    <div class="d-flex align-items-center justify-content-between mb-2">
        <div class="form-check form-switch">
            <input type="checkbox" 
                   class="form-check-input premium-checkbox" 
                   data-hash="${item.hashId}"
                   data-type="${type}"
                   data-form-id="${formId}"
                   ${isPremiumInitial ? 'checked' : ''}
                   id="premium_${item.id}"
                   style="width: 2.5em; height: 1.3em;">
            <label class="form-check-label fw-bold" for="premium_${item.id}" style="color: #ff6b35;">
                <i class="fas fa-gem me-1"></i> Premium
            </label>
        </div>
    </div>
    <div class="premium-fields mt-2" id="premium-fields-${item.id}" style="display: ${isPremiumInitial ? 'block' : 'none'}">
        <label class="small text-muted">Приоритет (0-100)</label>
        <input type="number" 
               class="form-control form-control-sm premium-priority" 
               data-hash="${item.hashId}"
               data-type="${type}"
               data-form-id="${formId}"
               min="0" 
               max="100" 
               value="${premiumPriorityInitial}">
        <small class="text-muted">Выше число = выше в карусели</small>
    </div>
</div>

<!-- Блок DONATE (только подсветка в ленте) -->
<div class="donate-block">
    <div class="d-flex align-items-center">
        <div class="form-check form-switch">
            <input type="checkbox" 
                   class="form-check-input donate-checkbox" 
                   data-hash="${item.hashId}"
                   data-type="${type}"
                   data-form-id="${formId}"
                   ${isDonateInitial ? 'checked' : ''}
                   id="donate_${item.id}"
                   style="width: 2.5em; height: 1.3em;">
            <label class="form-check-label fw-bold" for="donate_${item.id}" style="color: #ff6b35;">
                <i class="fas fa-heart me-1"></i> Donate
            </label>
        </div>
    </div>
</div>
` : ''}
            
            <div class="action-buttons mt-3">
                <a href="${detailLink}" class="btn btn-primary btn-mod" title="Просмотр">
                    <i class="fas fa-eye"></i>
                    <span> Детальнее</span>
                </a>
                
                <form id="${formId}" action="${toggleModerLink}" method="post" class="d-inline publish-form" data-hash="${item.hashId}" data-type="${type}">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <input type="hidden" name="premium" class="premium-hidden" value="${isPremiumInitial}">
                    <input type="hidden" name="premiumPriority" class="priority-hidden" value="${premiumPriorityInitial}">
                    <input type="hidden" name="donate" class="donate-hidden" value="${isDonateInitial}">
                    <button class="btn btn-success btn-mod publish-btn" type="button" title="Опубликовать">
                        <i class="fas fa-check"></i>
                        <span> ОК</span>
                    </button>
                </form>
                
                <form action="${toggleBlockLink}" method="post" class="d-inline">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button class="btn ${item.blocked ? 'btn-success btn-unblock' : 'btn-warning'} btn-mod block-btn" 
                            type="button" 
                            data-action="${toggleBlockLink}"
                            title="${item.blocked ? 'Разблокировать' : 'Заблокировать'}">
                        <i class="fas ${item.blocked ? 'fa-lock-open' : 'fa-lock'}"></i>
                        <span> ${item.blocked ? 'Разблок' : 'Блок'}</span>
                    </button>
                </form>
                
                <form action="${deleteLink}" method="post" class="d-inline">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button class="btn btn-danger btn-mod delete-btn" type="button" 
                            data-action="${deleteLink}"
                            title="Удалить">
                        <i class="fas fa-trash"></i>
                        <span> Удалить</span>
                    </button>
                </form>
            </div>
        </div>
    </div>
    `;

        if (type !== 'resume') {
            this.attachPostTypeHandlers(col, item.id, type, item.hashId, formId);
        }

        return col;
    }

    attachPostTypeHandlers(container, itemId, type, hashId, formId) {
        // Обработка premium
        const premiumCheckbox = container.querySelector('.premium-checkbox');
        const premiumFields = container.querySelector(`#premium-fields-${itemId}`);
        const priorityInput = container.querySelector('.premium-priority');

        // Обработка donate
        const donateCheckbox = container.querySelector('.donate-checkbox');

        const form = container.querySelector(`#${formId}`);
        if (!form) return;

        const premiumHidden = form.querySelector('.premium-hidden');
        const priorityHidden = form.querySelector('.priority-hidden');
        const donateHidden = form.querySelector('.donate-hidden');

        // Обработчик изменения чекбокса premium
        if (premiumCheckbox && premiumFields && premiumHidden) {
            premiumCheckbox.addEventListener('change', (e) => {
                const isChecked = e.target.checked;
                premiumFields.style.display = isChecked ? 'block' : 'none';
                premiumHidden.value = isChecked;
            });
        }

        // Обработчик изменения приоритета premium
        if (priorityInput && priorityHidden) {
            const updatePriority = () => {
                const value = parseInt(priorityInput.value) || 0;
                const clampedValue = Math.min(100, Math.max(0, value));
                if (priorityInput.value != clampedValue) {
                    priorityInput.value = clampedValue;
                }
                priorityHidden.value = clampedValue;
            };
            priorityInput.addEventListener('change', updatePriority);
            priorityInput.addEventListener('input', updatePriority);
        }

        // Обработчик изменения чекбокса donate
        if (donateCheckbox && donateHidden) {
            donateCheckbox.addEventListener('change', (e) => {
                donateHidden.value = e.target.checked;
            });
        }
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
        if (countElement) countElement.textContent = count;
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
        if (buttonContainer) buttonContainer.style.display = 'none';
    }

    showAllLoadedMessage(type) {
        const config = this.config[type];
        const messageDiv = document.getElementById(config.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'block';
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
            case 'products': return 'товаров для модерации';
            case 'services': return 'услуг для модерации';
            case 'vacancies': return 'вакансий для модерации';
            case 'resume': return 'резюме для модерации';
            default: return 'данных';
        }
    }

    showMessage(message, type = 'danger') {
        document.querySelectorAll('.alert-floating').forEach(el => el.remove());
        const errorDiv = document.createElement('div');
        errorDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed top-0 end-0 m-3 alert-floating`;
        errorDiv.style.zIndex = '9999';
        errorDiv.style.minWidth = '300px';
        errorDiv.innerHTML = `
            <strong>${type === 'success' ? 'Успех!' : 'Ошибка!'}</strong> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(errorDiv);
        setTimeout(() => { if (errorDiv.parentNode) errorDiv.remove(); }, 3000);
    }

    destroy() {
        this.cleanup();
    }
}

document.addEventListener('DOMContentLoaded', function() {
    try {
        window.moderatPagination = new LoadMoreModeratPagination();
    } catch {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger m-3';
        errorDiv.innerHTML = `
            <strong>Ошибка загрузки!</strong> Не удалось инициализировать загрузку данных модерации. 
            <button class="btn btn-sm btn-outline-secondary ms-2" onclick="location.reload()">Обновить страницу</button>
        `;
        const container = document.querySelector('.container');
        if (container) container.insertBefore(errorDiv, container.firstChild);
    }
});

if (typeof window !== 'undefined') {
    window.LoadMoreModeratPagination = LoadMoreModeratPagination;
}