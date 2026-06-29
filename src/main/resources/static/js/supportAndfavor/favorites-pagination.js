class FavoritesPagination {
    constructor() {
        this.config = {
            products: {
                containerId: 'productsContainer',
                staticContainerId: 'productsContainerStatic',
                buttonId: 'loadMoreProductsBtn',
                buttonContainerId: 'loadMoreProductsContainer',
                loadedMessageId: 'allLoadedProductsMessage',
                counterId: 'productsCounter',
                badgeId: 'productsBadge',
                type: 'products',
                colorClass: 'btn-primary',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                pageSize: 8
            },
            services: {
                containerId: 'servicesContainer',
                staticContainerId: 'servicesContainerStatic',
                buttonId: 'loadMoreServicesBtn',
                buttonContainerId: 'loadMoreServicesContainer',
                loadedMessageId: 'allLoadedServicesMessage',
                counterId: 'servicesCounter',
                badgeId: 'servicesBadge',
                type: 'services',
                colorClass: 'btn-success',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                pageSize: 8
            },
            vacancies: {
                containerId: 'vacanciesContainer',
                staticContainerId: 'vacanciesContainerStatic',
                buttonId: 'loadMoreVacanciesBtn',
                buttonContainerId: 'loadMoreVacanciesContainer',
                loadedMessageId: 'allLoadedVacanciesMessage',
                counterId: 'vacanciesCounter',
                badgeId: 'vacanciesBadge',
                type: 'vacancies',
                colorClass: 'btn-danger',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                pageSize: 8
            },
            resume: {
                containerId: 'resumeContainer',
                staticContainerId: 'resumeContainerStatic',
                buttonId: 'loadMoreResumeBtn',
                buttonContainerId: 'loadMoreResumeContainer',
                loadedMessageId: 'allLoadedResumeMessage',
                counterId: 'resumeCounter',
                badgeId: 'resumeBadge',
                type: 'resume',
                colorClass: 'btn-info',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                pageSize: 8
            }
        };

        this.activeTab = 'products';
        this.loadedTabs = new Set();
        this.csrfToken = this.getCsrfToken();
        this.globalLoadingSpinner = document.getElementById('loadingSpinner');

        this.restoreActiveTab();
    }

    restoreActiveTab() {
        const savedTab = localStorage.getItem('favorites_active_tab');
        if (savedTab && this.config[savedTab]) {
            this.activeTab = savedTab;
            console.log(`Restored active tab from localStorage`);
        } else {
            localStorage.setItem('favorites_active_tab', this.activeTab);
        }
    }

    switchToTab(tabType) {
        if (!this.config[tabType]) {
            console.warn(`Tab not found, switching to products`);
            tabType = 'products';
        }

        const tabButton = document.querySelector(`#favoritesTab button[data-type="${tabType}"]`);
        if (tabButton) {
            const tab = new bootstrap.Tab(tabButton);
            tab.show();
            console.log(`Switched to tab`);
        } else {
            console.warn(`Tab button for not found`);
        }
    }

    saveActiveTab(tabType) {
        localStorage.setItem('favorites_active_tab', tabType);
        this.activeTab = tabType;
    }

    init() {
        console.log('Initializing FavoritesPagination with Slice pagination...');

        this.initTabHandlers();
        this.initLoadButtons();
        this.hideAllStaticContainers();

        setTimeout(() => {
            this.loadInitialData(this.activeTab);

            if (this.activeTab !== 'products') {
                this.switchToTab(this.activeTab);
            }
        }, 100);
    }

    initTabHandlers() {
        const tabButtons = document.querySelectorAll('#favoritesTab button[data-bs-toggle="tab"]');

        tabButtons.forEach(button => {
            button.addEventListener('shown.bs.tab', (event) => {
                const targetTab = event.target.getAttribute('data-type');
                console.log(`Switching to tab`);

                this.saveActiveTab(targetTab);

                this.config[targetTab].isActive = true;

                Object.keys(this.config).forEach(tab => {
                    if (tab !== targetTab) {
                        this.config[tab].isActive = false;
                    }
                });

                this.hideAllStaticContainers();

                if (!this.config[targetTab].isInitialized) {
                    this.loadInitialData(targetTab);
                    this.config[targetTab].isInitialized = true;
                }
            });
        });
    }

    hideAllStaticContainers() {
        Object.values(this.config).forEach(config => {
            const staticContainer = document.getElementById(config.staticContainerId);
            if (staticContainer) {
                staticContainer.style.display = 'none';
            }
        });
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
        console.log(`Loading initial data for with Slice pagination`);
        const config = this.config[type];

        const staticContainer = document.getElementById(config.staticContainerId);
        if (staticContainer) {
            staticContainer.style.display = 'none';
        }

        if (staticContainer && config.loadedIds.size === 0) {
            const staticItems = staticContainer.querySelectorAll('[data-id]');
            const container = document.getElementById(config.containerId);

            if (staticItems.length > 0 && container) {
                staticItems.forEach(item => {
                    const itemId = parseInt(item.dataset.id);
                    if (!config.loadedIds.has(itemId)) {
                        const clonedItem = item.cloneNode(true);
                        container.appendChild(clonedItem);
                        config.loadedIds.add(itemId);
                    }
                });

                const itemCount = container.querySelectorAll('.favorite-card, .favorite-card-premium, .favorite-card-donate').length;
                this.updateCounter(type, itemCount);

                const totalCount = parseInt(document.getElementById(config.badgeId)?.textContent || 0);
                if (itemCount < totalCount) {
                    this.showLoadMoreButton(type);
                } else {
                    this.hideButton(type);
                    this.showAllLoadedMessage(type);
                }

                config.isInitialized = true;
                console.log(`Loaded static items for`);
                return;
            }
        }

        config.currentPage = 0;
        config.loadedIds.clear();
        config.hasMore = true;

        await this.loadMore(type, true);
    }

    async loadMore(type, isInitial = false) {
        const config = this.config[type];

        if (config.isLoading) {
            console.log(` is already loading, skipping`);
            return;
        }

        const button = document.getElementById(config.buttonId);
        const container = document.getElementById(config.containerId);

        if (!config.hasMore && !isInitial) {
            console.log(`No more to load`);
            return;
        }

        console.log(`Loading with Slice, page`);

        this.setLoadingState(button, true, type);

        try {
            const url = this.buildUrl(type, config.currentPage, config.pageSize);
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
            console.log(`Slice response for`);

            if (data.status === 'success') {
                const items = data.favorites || [];
                console.log(`Loaded count:`);

                if (items.length > 0) {
                    const newItems = [];
                    items.forEach(item => {
                        if (!config.loadedIds.has(item.favoriteId)) {
                            config.loadedIds.add(item.favoriteId);
                            newItems.push(item);
                        }
                    });

                    if (newItems.length > 0) {
                        this.appendData(type, newItems);
                        console.log(`Added`);

                        const currentItems = container.querySelectorAll('.favorite-card, .favorite-card-premium, .favorite-card-donate').length;
                        this.updateCounter(type, currentItems);
                    }

                    config.hasMore = data.hasMore;

                    if (data.hasMore) {
                        config.currentPage = data.currentPage + 1;
                    } else {
                        config.currentPage = data.currentPage;
                    }

                    if (button) {
                        button.dataset.currentPage = config.currentPage;
                    }

                    if (!data.hasMore) {
                        this.hideButton(type);
                        this.showAllLoadedMessage(type);
                    }
                } else {
                    console.log(`No available on page`);

                    if (isInitial && config.loadedIds.size === 0) {
                        this.showNoDataMessage(type);
                    }

                    config.hasMore = false;
                    this.hideButton(type);
                    this.showAllLoadedMessage(type);
                }
            } else {
                console.error('Server error:');
                this.showError(data.message || 'Ошибка сервера');

                if (isInitial && config.loadedIds.size === 0) {
                    this.showErrorMessage(type, 'Ошибка загрузки данных');
                }
            }

        } catch (error) {
            console.log('Ошибка загрузки:');
            this.showError('Ошибка при загрузке: ' + error.message);

            if (isInitial && config.loadedIds.size === 0) {
                this.showErrorMessage(type, 'Не удалось загрузить данные');
            }
        } finally {
            this.setLoadingState(button, false, type);
        }
    }

    buildUrl(type, page, size) {
        const baseUrl = '/page_one/favorites/load-more';
        const params = new URLSearchParams();

        params.append('page', page);
        params.append('size', size);
        params.append('type', type);

        return baseUrl + '?' + params.toString();
    }

    appendData(type, items) {
        const config = this.config[type];
        const container = document.getElementById(config.containerId);

        if (!items || items.length === 0 || !container) {
            return;
        }

        items.forEach(item => {
            try {
                const element = this.createFavoriteCard(item);
                container.appendChild(element);

                element.classList.add('new-item');
                setTimeout(() => element.classList.remove('new-item'), 300);
            } catch {
                console.log('Error creating element');
            }
        });

        if (typeof loadAllSellerRatings === 'function') {
            setTimeout(() => loadAllSellerRatings(), 100);
        }
    }

    createFavoriteCard(fav) {
        const col = document.createElement('div');
        col.className = 'col-md-6 col-lg-4 col-xl-3 mb-4';
        col.dataset.id = fav.favoriteId;
        col.dataset.type = fav.type;

        // Определяем класс карточки в зависимости от типа объявления
        let cardClass = 'favorite-card';
        if (fav.premium) {
            cardClass = 'favorite-card-premium';
        } else if (fav.donate) {
            cardClass = 'favorite-card-donate';
        }

        const typeBadgeClass = fav.type === 'product' ? 'bg-primary' :
            fav.type === 'service' ? 'bg-success' : fav.type === 'vacancy' ? 'bg-danger' : 'bg-info';

        const typeIcon = fav.type === 'product' ? 'fa-shopping-bag' :
            fav.type === 'service' ? 'fa-concierge-bell' : fav.type === 'vacancy' ? 'fa-briefcase' : 'fa-file-alt';

        const viewUrl = fav.type === 'product' ? `/postidviews/${fav.hashId}` :
            fav.type === 'service' ? `/postidviewsjob/${fav.hashId}` :
                fav.type === 'vacancy' ? `/postidviewsvac/${fav.hashId}` :
                    `/postidviewsres/${fav.hashId}`;

        let cityDisplay = '';
        if (fav.cityDisplayName) {
            cityDisplay = fav.cityDisplayName;
        } else if (fav.city && fav.city !== 'other' && fav.city !== '' && fav.city !== 'other_none') {
            cityDisplay = fav.city;
        } else {
            cityDisplay = 'Не указан';
        }

        const priceValue = fav.price || fav.cash || '0';
        const priceLabel = fav.type === 'vacancy' || fav.type === 'resume' ? 'Зарплата:' : 'Цена:';
        const priceSuffix = (fav.type !== 'vacancy' && fav.type !== 'resume') ? ' ₽' : '';

        col.innerHTML = `
            <div class="${cardClass}">
                <div class="favorite-image-wrapper">
                    ${fav.firstImagePath ? `<img src="${fav.firstImagePath}" class="favorite-image" alt="${this.escapeHtml(fav.title)}" loading="lazy">` : '<div class="favorite-image-placeholder"><i class="fas fa-image fa-3x text-muted"></i></div>'}
                </div>
                
                <h5 class="favorite-title">${this.escapeHtml(fav.title)}</h5>
                
                <div class="favorite-content">
                    <div class="favorite-info mb-2">
                        <small class="text-muted d-block">
                            <strong>${priceLabel}</strong>
                            <span>${this.escapeHtml(priceValue)}${priceSuffix}</span>
                        </small>
                        <small class="text-muted d-block">
                            <strong>Город:</strong>
                            <span>${this.escapeHtml(cityDisplay)}</span>
                        </small>
                    </div>
                    
                    <div class="favorite-actions">
                        <a href="${viewUrl}" class="btn btn-primary btn-mod">
                            <i class="fas fa-eye"></i> Просмотр
                        </a>
                        <form action="/page_one/favorites/remove/${fav.favoriteId}" method="post" class="d-inline">
                            <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                            <button type="submit" class="btn btn-outline-danger btn-sm">
                                <i class="fas fa-trash"></i>
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        `;

        return col;
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
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

    updateCounter(type, count) {
        const config = this.config[type];
        const counterElement = document.getElementById(config.counterId);

        if (counterElement) {
            const span = counterElement.querySelector('span');
            if (span) {
                span.textContent = count;
            }
        }
    }

    showLoadMoreButton(type) {
        const config = this.config[type];
        const buttonContainer = document.getElementById(config.buttonContainerId);

        if (buttonContainer) {
            buttonContainer.style.display = 'block';

            const messageDiv = document.getElementById(config.loadedMessageId);
            if (messageDiv) {
                messageDiv.style.display = 'none';
            }
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
            message.className = 'col-12 text-center py-5';
            message.innerHTML = `
                <div class="empty-favorites">
                    <i class="far fa-heart fa-3x mb-3 text-muted"></i>
                    <h4>Нет избранного в этой категории</h4>
                    <p class="text-muted">Добавьте что-нибудь в избранное, чтобы оно появилось здесь</p>
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
            `;
            container.appendChild(errorDiv);
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
    window.favoritesPagination = new FavoritesPagination();
    window.favoritesPagination.init();
});