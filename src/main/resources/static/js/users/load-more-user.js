class LoadMoreUserPosts {
    constructor() {
        this.config = {
            products: {
                containerId: 'productsContainer',
                buttonId: 'loadMoreProductsBtn',
                buttonContainerId: 'loadMoreProductsContainer',
                loadedMessageClass: 'all-loaded-message',
                type: 'products',
                colorClass: 'btn-primary',
                currentPage: 1,
                isLoading: false,
                hasMore: false,
                isActive: true,
                isInitialized: true
            },
            services: {
                containerId: 'servicesContainer',
                buttonId: 'loadMoreServicesBtn',
                buttonContainerId: 'loadMoreServicesContainer',
                loadedMessageClass: 'all-loaded-message',
                type: 'services',
                colorClass: 'btn-success',
                currentPage: 1,
                isLoading: false,
                hasMore: false,
                isActive: false,
                isInitialized: false
            },
            vacancies: {
                containerId: 'vacanciesContainer',
                buttonId: 'loadMoreVacanciesBtn',
                buttonContainerId: 'loadMoreVacanciesContainer',
                loadedMessageClass: 'all-loaded-message',
                type: 'vacancies',
                colorClass: 'btn-danger',
                currentPage: 1,
                isLoading: false,
                hasMore: false,
                isActive: false,
                isInitialized: false
            }
        };

        this.userHash = null;
        this.activeTab = 'products';
        this.loadedPostIds = {
            products: new Set(),
            services: new Set(),
            vacancies: new Set()
        };

        this.init();
    }

    init() {
        console.log('Initializing LoadMoreUserPosts...');

        this.extractUserHash();

        if (!this.userHash) {
            console.error('User Hash not found!');
            return;
        }

        this.initLoadedPostIds();
        this.initHasMore();
        this.initTabHandlers();
        this.initLoadButtons();
        this.restoreActiveTab();
    }

    extractUserHash() {
        const userHashInput = document.getElementById('userHash');
        if (userHashInput) {
            this.userHash = userHashInput.value;
            console.log('Got userHash from input');
            return;
        }

        const button = document.querySelector('[data-user-hash]');
        if (button) {
            this.userHash = button.getAttribute('data-user-hash');
            console.log('Got userHash from data attribute');
            return;
        }

        const pathParts = window.location.pathname.split('/');
        if (pathParts.length >= 3 && pathParts[1] === 'user') {
            this.userHash = pathParts[2];
            console.log('Got userHash from URL');
            return;
        }

        console.warn('User hash not found in any source');
    }

    initLoadedPostIds() {
        Object.keys(this.config).forEach(type => {
            const container = document.getElementById(this.config[type].containerId);
            if (container) {
                const viewLinks = container.querySelectorAll('a[href*="/postidviews"]');
                viewLinks.forEach(link => {
                    const href = link.getAttribute('href');
                    let postId = null;

                    if (href.includes('/postidviews/')) {
                        const hashMatch = href.match(/\/postidviews\/([^/]+)$/);
                        if (hashMatch && hashMatch[1]) {
                            postId = hashMatch[1];
                        }
                    } else if (href.includes('/postidviewsjob/')) {
                        const hashMatch = href.match(/\/postidviewsjob\/([^/]+)$/);
                        if (hashMatch && hashMatch[1]) {
                            postId = hashMatch[1];
                        }
                    } else if (href.includes('/postidviewsvac/')) {
                        const hashMatch = href.match(/\/postidviewsvac\/([^/]+)$/);
                        if (hashMatch && hashMatch[1]) {
                            postId = hashMatch[1];
                        }
                    }

                    if (postId) {
                        this.loadedPostIds[type].add(postId);
                    }
                });
            }
        });
    }

    initHasMore() {
        Object.values(this.config).forEach(config => {
            const buttonContainer = document.getElementById(config.buttonContainerId);
            if (buttonContainer && buttonContainer.style.display !== 'none' &&
                buttonContainer.querySelector('button')) {
                config.hasMore = true;
            } else {
                config.hasMore = false;
            }
        });
    }

    initTabHandlers() {
        const tabButtons = document.querySelectorAll('#categoryTab button[data-bs-toggle="tab"]');

        tabButtons.forEach(button => {
            button.addEventListener('shown.bs.tab', (event) => {
                const targetTab = event.target.getAttribute('data-type');
                this.activeTab = targetTab;

                Object.values(this.config).forEach(config => {
                    config.isActive = (config.type === targetTab);
                });

                localStorage.setItem('userPostsActiveTab', targetTab);
            });
        });
    }

    initLoadButtons() {
        Object.values(this.config).forEach(config => {
            const button = document.getElementById(config.buttonId);
            if (button) {
                const currentPage = button.getAttribute('data-current-page');
                if (currentPage !== null) {
                    const parsed = parseInt(currentPage);
                    if (!isNaN(parsed) && parsed >= 1) {
                        config.currentPage = parsed;
                    } else {
                        config.currentPage = 1;
                        button.setAttribute('data-current-page', '1');
                    }
                }

                button.removeAttribute('onclick');
                button.addEventListener('click', () => this.loadMore(config.type));
            }
        });
    }

    restoreActiveTab() {
        const savedTab = localStorage.getItem('userPostsActiveTab');
        if (savedTab && this.config[savedTab]) {
            const tabElement = document.querySelector(`#categoryTab button[data-type="${savedTab}"]`);
            if (tabElement) {
                const tab = new bootstrap.Tab(tabElement);
                tab.show();
                this.activeTab = savedTab;

                Object.values(this.config).forEach(config => {
                    config.isActive = (config.type === savedTab);
                });
            }
        }
    }

    async loadMore(type) {
        const config = this.config[type];

        if (!this.userHash) {
            console.error('User Hash is not set!');
            this.showError('Ошибка: Хеш пользователя не найден');
            return;
        }

        if (config.isLoading || !config.hasMore) {
            return;
        }

        const button = document.getElementById(config.buttonId);
        const container = document.getElementById(config.containerId);

        if (!button || !container) {
            console.log(`Button or container not found for`);
            return;
        }

        const serverPage = config.currentPage;

        this.setLoadingState(button, true, type);

        try {
            const url = this.buildUrl(type, serverPage);
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'text/html',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const html = await response.text();

            if (html.trim() === '') {
                throw new Error('Empty response from server');
            }

            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            const cards = doc.querySelectorAll('.col-md-6');
            let cardsAdded = 0;

            if (cards && cards.length > 0) {
                cards.forEach(card => {
                    const clonedCard = card.cloneNode(true);
                    container.appendChild(clonedCard);
                    cardsAdded++;
                });

                config.currentPage = serverPage + 1;
                button.setAttribute('data-current-page', config.currentPage.toString());

                if (cardsAdded < 8) {
                    config.hasMore = false;
                    this.hideButton(type);
                    this.showAllLoadedMessage(type);
                }
            } else {
                config.hasMore = false;
                this.hideButton(type);
                this.showAllLoadedMessage(type);
            }

        } catch {
            console.log('Ошибка загрузки');
            this.showError('Ошибка при загрузке');
            config.hasMore = false;
            this.hideButton(type);
        } finally {
            this.setLoadingState(button, false, type);
        }
    }

    buildUrl(type, page) {
        const pageNumber = !isNaN(page) && page >= 0 ? page : 0;
        return `/user/${encodeURIComponent(this.userHash)}/load-more?type=${encodeURIComponent(type)}&page=${pageNumber}&size=8`;
    }

    setLoadingState(button, loading, type) {
        const config = this.config[type];
        config.isLoading = loading;

        if (button) {
            button.disabled = loading;
            button.innerHTML = loading ?
                '<i class="fas fa-spinner fa-spin me-2"></i> Загрузка...' :
                `<i class="fas fa-plus-circle me-2"></i> Показать еще ${this.getTypeDisplayName(type, true)}`;
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
        const container = document.getElementById(this.config[type].containerId);
        if (container) {
            if (!container.querySelector('.all-loaded-message')) {
                const messageDiv = document.createElement('div');
                messageDiv.className = 'col-12 text-center py-4 all-loaded-message';
                messageDiv.innerHTML = `
                    <p class="text-muted">
                        <i class="fas fa-check-circle text-success me-2"></i>
                        Все ${this.getTypeDisplayName(type)} загружены
                    </p>
                `;
                container.appendChild(messageDiv);
            }
        }
    }

    getTypeDisplayName(type, withAccusative = false) {
        switch(type) {
            case 'products': return withAccusative ? 'товары' : 'товары';
            case 'services': return withAccusative ? 'услуги' : 'услуги';
            case 'vacancies': return withAccusative ? 'вакансии' : 'вакансии';
            default: return 'данные';
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
    console.log('DOM loaded, initializing user posts pagination...');
    try {
        window.userPostsPagination = new LoadMoreUserPosts();
        console.log('User posts pagination initialized successfully');
    } catch {
        console.log('Error initializing LoadMoreUserPosts');

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