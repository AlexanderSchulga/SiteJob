class LoadMorePaginationRedactV2 {
    constructor() {
        this.config = {
            products: {
                containerId: 'productsContainer',
                buttonId: 'loadMoreProductsBtn',
                buttonContainerId: 'loadMoreProductsContainer',
                loadedMessageId: 'allLoadedProductsMessage',
                type: 'products',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: true,
                isInitialized: false,
                lastPosition: 0
            },
            services: {
                containerId: 'servicesContainer',
                buttonId: 'loadMoreServicesBtn',
                buttonContainerId: 'loadMoreServicesContainer',
                loadedMessageId: 'allLoadedServicesMessage',
                type: 'services',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                lastPosition: 0
            },
            vacancies: {
                containerId: 'vacanciesContainer',
                buttonId: 'loadMoreVacanciesBtn',
                buttonContainerId: 'loadMoreVacanciesContainer',
                loadedMessageId: 'allLoadedVacanciesMessage',
                type: 'vacancies',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                lastPosition: 0
            },
            resume: {
                containerId: 'resumeContainer',
                buttonId: 'loadMoreResumeBtn',
                buttonContainerId: 'loadMoreResumeContainer',
                loadedMessageId: 'allLoadedResumeMessage',
                type: 'resume',
                currentPage: 0,
                isLoading: false,
                hasMore: true,
                loadedIds: new Set(),
                isActive: false,
                isInitialized: false,
                lastPosition: 0
            }
        };

        this.globalLoadingSpinner = document.getElementById('loadingSpinner');
        this.csrfToken = this.getCsrfToken();
        this.activeTab = localStorage.getItem('redactor_active_tab') || 'products';
        this.loadedTabs = new Set();
        this.isSuper = document.body.hasAttribute('data-is-super') || false;

        this.init();
    }

    init() {
        console.log('Initializing LoadMorePaginationRedactV2...');
        this.initUserRole();
        this.initTabHandlers();
        this.initLoadButtons();
        this.initModalHandlers();
        this.restoreActiveTab();

        setTimeout(() => {
            this.loadInitialData(this.activeTab);
        }, 100);
    }

    initModalHandlers() {
        document.body.addEventListener('click', (e) => {
            const archiveBtn = e.target.closest('.archive-post-btn');
            if (archiveBtn) {
                e.preventDefault();
                const form = archiveBtn.closest('form');
                const action = form.getAttribute('action');
                const postType = archiveBtn.getAttribute('data-post-type') || 'объявление';

                const modalBody = document.getElementById('archivePostModalBody');
                if (modalBody) {
                    modalBody.innerHTML = `Вы уверены, что хотите <strong>отправить в архив</strong> это ${postType}?<br><small class="text-muted">Вы сможете восстановить его через редактирование.</small>`;
                }

                const modalForm = document.getElementById('archivePostForm');
                if (modalForm) {
                    modalForm.setAttribute('action', action);
                    const csrfInput = form.querySelector('input[name="_csrf"]');
                    if (csrfInput) {
                        const modalCsrfInput = modalForm.querySelector('input[name="_csrf"]');
                        if (modalCsrfInput) modalCsrfInput.value = csrfInput.value;
                    }
                }

                const modalElement = document.getElementById('archivePostModal');
                if (modalElement && window.bootstrap) {
                    const modal = new bootstrap.Modal(modalElement);
                    modal.show();
                }
            }
        });
    }

    initUserRole() {
        const isSuperMeta = document.querySelector('meta[name="is-super"]');
        if (isSuperMeta) {
            this.isSuper = isSuperMeta.getAttribute('content') === 'true';
        }
        const superElement = document.querySelector('[data-is-super]');
        if (superElement) {
            this.isSuper = superElement.getAttribute('data-is-super') === 'true';
        }
    }

    restoreActiveTab() {
        this.config[this.activeTab].isActive = true;
        if (this.activeTab !== 'products') {
            this.config.products.isActive = false;
        }

        const tabButton = document.querySelector(`#categoryTab button[data-type="${this.activeTab}"]`);
        if (tabButton) {
            document.querySelectorAll('#categoryTab .nav-link').forEach(link => {
                link.classList.remove('active');
            });
            tabButton.classList.add('active');

            document.querySelectorAll('.tab-pane').forEach(pane => {
                pane.classList.remove('show', 'active');
            });

            const tabPaneId = this.activeTab === 'resume' ? 'resume-content' : `${this.activeTab}-content`;
            const tabPane = document.querySelector(`#${tabPaneId}`);
            if (tabPane) {
                tabPane.classList.add('show', 'active');
            }
        }
    }

    initTabHandlers() {
        const tabButtons = document.querySelectorAll('#categoryTab button[data-bs-toggle="tab"]');

        tabButtons.forEach(button => {
            button.addEventListener('shown.bs.tab', (event) => {
                const targetTab = event.target.getAttribute('data-type');

                if (this.activeTab && this.activeTab !== targetTab) {
                    this.saveTabScrollPosition(this.activeTab);
                }

                this.activeTab = targetTab;
                localStorage.setItem('redactor_active_tab', this.activeTab);

                Object.keys(this.config).forEach(type => {
                    this.config[type].isActive = (type === targetTab);
                });

                if (!this.config[targetTab].isInitialized) {
                    this.loadInitialData(targetTab);
                    this.config[targetTab].isInitialized = true;
                } else {
                    setTimeout(() => {
                        this.restoreTabState(targetTab);
                        this.restoreTabScrollPosition(targetTab);
                    }, 100);
                }
            });
        });
    }

    saveTabScrollPosition(type) {
        const config = this.config[type];
        if (!config) return;
        const container = document.getElementById(config.containerId);
        if (container) {
            config.lastPosition = container.scrollTop || 0;
        }
    }

    restoreTabScrollPosition(type) {
        const config = this.config[type];
        if (!config || config.lastPosition === 0) return;
        const container = document.getElementById(config.containerId);
        if (container) {
            setTimeout(() => {
                container.scrollTop = config.lastPosition;
            }, 50);
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
        const config = this.config[type];
        config.currentPage = 0;
        config.loadedIds.clear();
        config.hasMore = true;
        await this.loadMore(type, true);
    }

    restoreTabState(type) {
        const config = this.config[type];
        if (!config) return;

        const container = document.getElementById(config.containerId);
        const items = container ? container.querySelectorAll(`[data-type="${type}"]`) : [];
        const itemsCount = items.length;

        config.loadedIds.clear();
        items.forEach(item => {
            const hiddenInput = item.querySelector('.item-numeric-id');
            if (hiddenInput) {
                config.loadedIds.add(Number(hiddenInput.value));
            }
        });

        const loadedPages = Math.ceil(itemsCount / 8);
        config.currentPage = loadedPages;

        const button = document.getElementById(config.buttonId);
        if (button) {
            button.dataset.currentPage = config.currentPage;
        }

        if (!config.hasMore) {
            this.hideButton(type);
            this.showAllLoadedMessage(type);
        } else {
            const buttonContainer = document.getElementById(config.buttonContainerId);
            if (buttonContainer) {
                buttonContainer.style.display = 'block';
            }
        }
    }

    /**
     * Очищает кеш карусели после удаления/изменения объявления
     */
    clearCarouselCache() {
        // Очищаем sessionStorage
        sessionStorage.removeItem('paidCarouselItems');
        // Обновляем глобальную переменную
        window.paidCarouselItems = null;
        // Отправляем событие для обновления карусели
        document.dispatchEvent(new CustomEvent('carouselRefresh'));
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

        if (isInitial) {
            const existingItems = container ? container.querySelectorAll(`[data-type="${type}"]`) : [];
            if (existingItems.length === 0) {
                config.currentPage = 0;
                config.loadedIds.clear();
                config.hasMore = true;
                config.isInitialized = true;
            } else {
                config.isInitialized = true;
                if (buttonContainer) buttonContainer.style.display = 'block';
                const messageDiv = document.getElementById(config.loadedMessageId);
                if (messageDiv) messageDiv.style.display = 'none';
                return;
            }

            if (buttonContainer) buttonContainer.style.display = 'block';
            const messageDiv = document.getElementById(config.loadedMessageId);
            if (messageDiv) messageDiv.style.display = 'none';
        }

        if (!config.hasMore && !isInitial) {
            return;
        }

        this.setLoadingState(button, true, type);

        try {
            const url = this.buildUrl(type, config.currentPage);
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin'
            });

            if (!response.ok) {

                if (response.status === 403) {
                    this.showErrorMessage(type, 'Доступ запрещен');
                } else {
                    this.showErrorMessage(type, 'Ошибка сервера. Попробуйте позже.');
                }
                return;
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
                        config.hasMore = data.hasMore;
                        config.currentPage = config.currentPage + 1;
                        if (button) {
                            button.dataset.currentPage = config.currentPage;
                        }
                    } else {
                        config.hasMore = data.hasMore;
                    }

                    if (!config.hasMore) {
                        this.hideButton(type);
                        this.showAllLoadedMessage(type);
                    }
                } else {
                    if (isInitial && config.loadedIds.size === 0) {
                        this.showNoDataMessage(type);
                    }
                    config.hasMore = false;
                    this.hideButton(type);
                    this.showAllLoadedMessage(type);
                }
            } else {
                console.log('Server error occurred');
                if (isInitial && config.loadedIds.size === 0) {
                    this.showErrorMessage(type, 'Ошибка загрузки данных');
                }
            }
        } catch {
            console.log('Network error occurred');
            if (isInitial && config.loadedIds.size === 0) {
                this.showErrorMessage(type, 'Не удалось загрузить данные');
            }
        } finally {
            this.setLoadingState(button, false, type);
        }
    }

    buildUrl(type, page) {
        const baseUrl = '/page_two/redactor/load-more-redact';
        const params = new URLSearchParams();
        params.append('page', page);
        params.append('size', 8);
        params.append('type', type);
        return baseUrl + '?' + params.toString();
    }

    appendData(type, items) {
        const config = this.config[type];
        const container = document.getElementById(config.containerId);

        if (!items || items.length === 0 || !container) return;

        items.forEach(item => {
            const element = this.createItemElement(type, item);
            container.appendChild(element);
        });

        const newElements = container.querySelectorAll(`[data-type="${type}"]:last-child`);
        newElements.forEach(el => {
            el.classList.add('new-item');
            setTimeout(() => el.classList.remove('new-item'), 2000);
        });
    }

    // Определение класса карточки в зависимости от premium/donate
    getCardClass(item) {
        if (item.premium) {
            return 'post-card-premium';
        }
        if (item.donate) {
            return 'post-card-donate';
        }
        return 'post-card';
    }

    createItemElement(type, item) {
        const col = document.createElement('div');
        col.className = 'col-6 col-sm-6 col-md-4 col-lg-3 mb-3';
        col.setAttribute('data-id', item.hashId);
        col.setAttribute('data-type', type);

        const hiddenIdInput = document.createElement('input');
        hiddenIdInput.type = 'hidden';
        hiddenIdInput.className = 'item-numeric-id';
        hiddenIdInput.value = item.id;
        col.appendChild(hiddenIdInput);

        let detailLink = '', editLink = '', removeAction = '', toggleBlockAction = '', archiveAction = '';

        switch(type) {
            case 'products':
                detailLink = `/postidviews/${item.hashId}`;
                editLink = `/page_two/redactor/${item.hashId}/page_two/edit`;
                removeAction = `/page_two/redactor/${item.hashId}/remove`;
                toggleBlockAction = `/page_two/redactor/${item.hashId}/toggle-block`;
                archiveAction = `/page_two/redactor/${item.hashId}/manual-archive`;
                break;
            case 'services':
                detailLink = `/postidviewsjob/${item.hashId}`;
                editLink = `/page_two/redactor/job/${item.hashId}/page_two/edit`;
                removeAction = `/page_two/redactor/job/${item.hashId}/remove`;
                toggleBlockAction = `/page_two/redactor/job/${item.hashId}/toggle-block`;
                archiveAction = `/page_two/redactor/job/${item.hashId}/manual-archive`;
                break;
            case 'vacancies':
                detailLink = `/postidviewsvac/${item.hashId}`;
                editLink = `/page_two/redactor/vac/${item.hashId}/page_two/edit`;
                removeAction = `/page_two/redactor/vac/${item.hashId}/remove`;
                toggleBlockAction = `/page_two/redactor/vac/${item.hashId}/toggle-block`;
                archiveAction = `/page_two/redactor/vac/${item.hashId}/manual-archive`;
                break;
            case 'resume':
                detailLink = `/postidviewsres/${item.hashId}`;
                editLink = `/page_two/redactor/res/${item.hashId}/page_two/edit_res`;
                removeAction = `/page_two/redactor/res/${item.hashId}/remove`;
                toggleBlockAction = `/page_two/redactor/res/${item.hashId}/toggle-block`;
                archiveAction = `/page_two/redactor/res/${item.hashId}/manual-archive`;
                break;
        }

        const cardClass = this.getCardClass(item);

        // Кнопки в 2 строки по 2 кнопки, без иконок
        let actionsHtml = `
            <div class="btn-row">
                <a href="${detailLink}" class="btn btn-primary btn-mod">
                    Просмотр
                </a>
                <a href="${editLink}" class="btn btn-primary btn-mod">
                    Редакт
                </a>
            </div>
            <div class="btn-row">
                <form action="${removeAction}" method="post" class="d-inline w-100" data-post-id="${item.id}" data-post-type="${type}">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button type="button" class="btn btn-primary btn-mod delete-post-btn w-100" data-post-type="${type.slice(0, -1)}" data-post-id="${item.id}">
                        Удалить
                    </button>
                </form>
        `;

        if (!this.isSuper && !item.archived) {
            actionsHtml += `
                <form action="${archiveAction}" method="post" class="d-inline w-100">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button type="button" class="btn btn-primary btn-mod archive-post-btn w-100" data-post-type="${type.slice(0, -1)}">
                        В архив
                    </button>
                </form>
            `;
        } else if (this.isSuper && !item.archived) {
            actionsHtml += `<div class="btn-placeholder"></div>`;
        }

        if (this.isSuper) {
            const blockText = item.blocked ? 'Разблок.' : 'Блок.';
            actionsHtml += `
                <form action="${toggleBlockAction}" method="post" class="d-inline w-100">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button type="button" class="btn btn-primary btn-mod toggle-block-btn w-100" data-post-type="${type.slice(0, -1)}">
                        ${blockText}
                    </button>
                </form>
            `;
        } else if (!this.isSuper && !item.archived) {
            actionsHtml += `</div>`;
        } else {
            actionsHtml += `</div>`;
        }

        if (!this.isSuper && !item.archived) {
            // Уже закрыли выше
        } else if (!this.isSuper && item.archived) {
            actionsHtml = `
            <div class="btn-row">
                <a href="${detailLink}" class="btn btn-primary btn-mod">Просмотр</a>
                <a href="${editLink}" class="btn btn-primary btn-mod">Редакт</a>
            </div>
            <div class="btn-row">
                <form action="${removeAction}" method="post" class="d-inline w-100">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button type="button" class="btn btn-primary btn-mod delete-post-btn w-100" data-post-type="${type.slice(0, -1)}">Удалить</button>
                </form>
                <div class="btn-placeholder"></div>
            </div>
            `;
        } else if (this.isSuper && item.archived) {
            actionsHtml = `
            <div class="btn-row">
                <a href="${detailLink}" class="btn btn-primary btn-mod">Просмотр</a>
                <a href="${editLink}" class="btn btn-primary btn-mod">Редакт</a>
            </div>
            <div class="btn-row">
                <form action="${removeAction}" method="post" class="d-inline w-100">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button type="button" class="btn btn-primary btn-mod delete-post-btn w-100" data-post-type="${type.slice(0, -1)}">Удалить</button>
                </form>
                <div class="btn-placeholder"></div>
            </div>
            `;
        }

        col.innerHTML = `
            <div class="${cardClass}">
                <div class="post-image-wrapper">
                    ${item.firstImagePath ?
            `<img src="${item.firstImagePath}" class="post-image" alt="${this.escapeHtml(item.title)}" loading="lazy">` :
            `<div class="post-image-placeholder d-flex align-items-center justify-content-center bg-light">
                            <i class="fas fa-image fa-3x text-muted"></i>
                        </div>`
        }
                    ${item.archived ? '<div class="archived-banner"><i class="fas fa-archive me-1"></i>В АРХИВЕ</div>' : ''}
                    ${item.blocked ? '<div class="blocked-banner"><i class="fas fa-ban me-1"></i>ЗАБЛОКИРОВАНО</div>' : ''}
                    ${item.moderation ? '<div class="moderation-banner"><i class="fas fa-clock me-1"></i>Ожидает модерации</div>' : ''}
                </div>
                <h5 class="post-title">${this.escapeHtml(item.title)}</h5>

                <div class="action-buttons">
                    ${actionsHtml}
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
                '<span class="spinner-border spinner-border-sm me-2" role="status"></span> Загрузка...' :
                'Показать еще';
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
                    <button class="btn btn-sm btn-outline-secondary mt-2 d-block mx-auto" onclick="location.reload()">
                        <i class="fas fa-redo me-1"></i> Обновить страницу
                    </button>
                </div>
            `;
            container.appendChild(errorDiv);
        }
    }

    getTypeDisplayName(type) {
        switch(type) {
            case 'products': return 'товаров';
            case 'services': return 'услуг';
            case 'vacancies': return 'вакансий';
            case 'resume': return 'резюме';
            default: return 'данных';
        }
    }

    /**
     * Обработчик удаления поста - очищает кеш карусели
     */
    handlePostDelete() {
        this.clearCarouselCache();
    }
}

// Добавляем обработчик для кнопок удаления
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing pagination...');

    // Создаем экземпляр пагинации
    window.pagination = new LoadMorePaginationRedactV2();
    console.log('Pagination initialized successfully');

    // Обработчик для кнопок удаления
    document.body.addEventListener('click', function(e) {
        const deleteBtn = e.target.closest('.delete-post-btn');
        if (deleteBtn) {
            // При клике на удаление - очищаем кеш карусели
            if (window.pagination && window.pagination.clearCarouselCache) {
                window.pagination.clearCarouselCache();
            }
        }
    });

    // Обработчик успешного удаления (после подтверждения)
    document.addEventListener('postDeleted', function() {
        if (window.pagination && window.pagination.clearCarouselCache) {
            window.pagination.clearCarouselCache();
        }
    });
});