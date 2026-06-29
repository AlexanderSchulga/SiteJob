class AdminUserPagination {
    constructor() {
        // Конфигурация для разных типов контента
        this.config = {
            products: {
                containerId: 'productsContainer',
                staticContainerId: 'productsContainerStatic',
                buttonId: 'loadMoreProductsBtn',
                buttonContainerId: 'loadMoreProductsContainer',
                loadedMessageId: 'allLoadedProductsMessage',
                type: 'products',
                colorClass: 'btn-primary',
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
                staticContainerId: 'servicesContainerStatic',
                buttonId: 'loadMoreServicesBtn',
                buttonContainerId: 'loadMoreServicesContainer',
                loadedMessageId: 'allLoadedServicesMessage',
                type: 'services',
                colorClass: 'btn-success',
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
                staticContainerId: 'vacanciesContainerStatic',
                buttonId: 'loadMoreVacanciesBtn',
                buttonContainerId: 'loadMoreVacanciesContainer',
                loadedMessageId: 'allLoadedVacanciesMessage',
                type: 'vacancies',
                colorClass: 'btn-danger',
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
                staticContainerId: 'resumeContainerStatic',
                buttonId: 'loadMoreResumeBtn',
                buttonContainerId: 'loadMoreResumeContainer',
                loadedMessageId: 'allLoadedResumeMessage',
                type: 'resume',
                colorClass: 'btn-danger',
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
        this.userHashId = this.getUserHashId();

        // Восстанавливаем активную вкладку из localStorage
        this.activeTab = localStorage.getItem('admin_user_active_tab') || 'products';
        this.loadedTabs = new Set();

        this.init();
    }

    init() {
        console.log('Initializing AdminUserPagination...');

        // Скрываем статические контейнеры
        this.hideStaticContainers();

        // Инициализируем обработчики вкладок
        this.initTabHandlers();

        // Инициализируем кнопки загрузки
        this.initLoadButtons();

        // Восстанавливаем активную вкладку
        this.restoreActiveTab();

        // Загружаем данные для активной вкладки
        setTimeout(() => {
            console.log('Loading initial data for active tab');
            this.loadInitialData(this.activeTab);
        }, 100);
    }

    getUserHashId() {
        // Получаем hashId пользователя из URL или скрытого поля
        const path = window.location.pathname;
        const match = path.match(/\/users\/([^\/]+)\/page_two/);
        if (match && match[1]) {
            return match[1];
        }

        // Ищем в скрытом поле или других местах
        const hashIdElement = document.querySelector('[name="userHashId"]');
        if (hashIdElement) {
            return hashIdElement.value;
        }

        console.warn('User hashId not found');
        return '';
    }

    hideStaticContainers() {
        // Скрываем все статические контейнеры
        Object.values(this.config).forEach(config => {
            const staticContainer = document.getElementById(config.staticContainerId);
            if (staticContainer) {
                staticContainer.style.display = 'none';
            }
        });
    }

    restoreActiveTab() {
        // Активируем сохраненную вкладку
        this.config[this.activeTab].isActive = true;

        // Деактивируем другие вкладки
        Object.keys(this.config).forEach(tab => {
            if (tab !== this.activeTab) {
                this.config[tab].isActive = false;
            }
        });

        // Находим кнопку вкладки и активируем ее
        const tabButton = document.querySelector(`#userPostsTab button[data-type="${this.activeTab}"]`);
        if (tabButton) {
            // Удаляем класс active у всех вкладок
            document.querySelectorAll('#userPostsTab .nav-link').forEach(link => {
                link.classList.remove('active');
            });

            // Добавляем класс active к нужной вкладке
            tabButton.classList.add('active');

            // Удаляем класс show active у всех tab-pane
            document.querySelectorAll('#userPostsTabContent .tab-pane').forEach(pane => {
                pane.classList.remove('show', 'active');
            });

            // Добавляем классы к нужному tab-pane
            const tabPane = document.querySelector(`#${this.activeTab}-content`);
            if (tabPane) {
                tabPane.classList.add('show', 'active');
            }
        }
    }

    initTabHandlers() {
        const tabButtons = document.querySelectorAll('#userPostsTab button[data-bs-toggle="tab"]');

        tabButtons.forEach(button => {
            button.addEventListener('shown.bs.tab', (event) => {
                const targetTab = event.target.getAttribute('data-type');
                console.log(`Switching to tab`);

                // Сохраняем позицию прокрутки для предыдущей вкладки
                if (this.activeTab && this.activeTab !== targetTab) {
                    this.saveTabScrollPosition(this.activeTab);
                }

                // Устанавливаем новую активную вкладку и сохраняем в localStorage
                this.activeTab = targetTab;
                localStorage.setItem('admin_user_active_tab', this.activeTab);

                // Обновляем состояния
                Object.keys(this.config).forEach(tab => {
                    this.config[tab].isActive = (tab === targetTab);
                });

                if (!this.config[targetTab].isInitialized) {
                    // Первая загрузка вкладки
                    this.loadInitialData(targetTab);
                    this.config[targetTab].isInitialized = true;
                } else {
                    // Если вкладка уже загружалась, восстанавливаем состояние
                    setTimeout(() => {
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
        const csrfInput = document.querySelector('input[name="_csrf"]');
        return csrfInput ? csrfInput.value : '';
    }

    async loadInitialData(type) {
        console.log(`Loading initial data for`);
        this.loadedTabs.add(type);
        await this.loadMore(type, true);
    }

    async loadMore(type, isInitial = false) {
        const config = this.config[type];

        if (!config.isActive && !isInitial) {
            console.log(`is not active, skipping load`);
            return;
        }

        if (config.isLoading) {
            console.log(`is already loading, skipping`);
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

                // Очищаем контейнер перед загрузкой
                if (container) {
                    container.innerHTML = '';
                }
            } else {
                config.isInitialized = true;
                return;
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
            console.log(`No more to load`);
            return;
        }


        this.setLoadingState(button, true, type);

        try {
            const url = this.buildUrl(type, config.currentPage);
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
                    }

                    config.hasMore = data.hasMore;
                    config.currentPage = data.currentPage + 1;

                    if (button) {
                        button.dataset.currentPage = config.currentPage;
                    }

                    // Если загружено меньше 8 элементов или hasMore = false - скрываем кнопку
                    if (!data.hasMore || items.length < 8) {
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
                console.error('Server error');
                this.showError(data.message || 'Ошибка сервера');
            }

        } catch {
            console.log('Ошибка загрузки');
            this.showError('Ошибка при загрузке');
        } finally {
            this.setLoadingState(button, false, type);
        }
    }

    buildUrl(type, page) {
        const baseUrl = `/users/${this.userHashId}/admin/posts/load-more`;
        const params = new URLSearchParams();

        params.append('page', page);
        params.append('size', 8);
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
                const element = this.createItemElement(type, item);
                container.appendChild(element);
            } catch {
                console.log('Error creating element');
            }
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
        col.className = 'col-md-6 col-lg-4 col-xl-3 mb-4';
        col.setAttribute('data-id', item.hashId);
        col.setAttribute('data-type', type);

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

        let cardClasses = this.getCardClass(item);
        let banners = '';

        if (item.archived) {
            cardClasses += ' archived';
            banners += `
        <div class="status-banner archived-banner">
            <span class="badge">В АРХИВЕ</span>
        </div>`;
        }

        if (item.blocked) {
            cardClasses += ' blocked';
            banners += `
        <div class="status-banner blocked-banner">
            <span class="badge">ЗАБЛОКИРОВАНО</span>
        </div>`;
        }

        if (item.moderation && !item.blocked && !item.archived) {
            cardClasses += ' on-moderation';
            banners += `
        <div class="status-banner moderation-banner">
            <span class="badge">НА МОДЕРАЦИИ</span>
        </div>`;
        }

        const imageHtml = item.firstImagePath ?
            `<img src="${item.firstImagePath}" class="post-image img-fluid" alt="${item.title || ''}" loading="lazy" onerror="this.onerror=null; this.src='/images/no-image.png'">` :
            `<div class="post-image-placeholder d-flex align-items-center justify-content-center bg-light">
                <i class="fas fa-image fa-3x text-muted"></i>
            </div>`;

        const titleText = this.escapeHtml(item.title || 'Без названия');

        // Отображение цены (для вакансий может быть зарплата)
        let priceHtml = '';
        if (type === 'vacancies') {
            priceHtml = item.salary ? `<strong>Зарплата:</strong> ${this.escapeHtml(item.salary)}` : '';
        } else if (type === 'resume') {
            priceHtml = ''; // У резюме обычно нет цены
        } else {
            priceHtml = item.price ? `<strong>Цена:</strong> ${this.escapeHtml(item.price)}` : '';
        }

        let editLink = '';
        let toggleBlockAction = '';
        let removeAction = '';
        let postTypeText = '';

        switch(type) {
            case 'products':
                editLink = `/page_two/admin_edit_user/${item.hashId}/page_two/edit_admin?hashId=${this.userHashId}`;
                toggleBlockAction = `/users/${this.userHashId}/page_two/admin_edit_user/post/${item.id}/toggle-block`;
                removeAction = `/users/${this.userHashId}/page_two/admin_edit_user/post/${item.id}/remove`;
                postTypeText = 'товар';
                break;
            case 'services':
                editLink = `/page_two/admin_edit_user/job/${item.hashId}/page_two/edit_admin?hashId=${this.userHashId}`;
                toggleBlockAction = `/users/${this.userHashId}/page_two/admin_edit_user/job/${item.id}/toggle-block`;
                removeAction = `/users/${this.userHashId}/page_two/admin_edit_user/job/${item.id}/remove`;
                postTypeText = 'услугу';
                break;
            case 'vacancies':
                editLink = `/page_two/admin_edit_user/vac/${item.hashId}/page_two/edit_admin?hashId=${this.userHashId}`;
                toggleBlockAction = `/users/${this.userHashId}/page_two/admin_edit_user/vacancy/${item.id}/toggle-block`;
                removeAction = `/users/${this.userHashId}/page_two/admin_edit_user/vacancy/${item.id}/remove`;
                postTypeText = 'вакансию';
                break;
            case 'resume':
                editLink = `/page_two/admin_edit_user/res/${item.hashId}/page_two/edit_admin?hashId=${this.userHashId}`;
                toggleBlockAction = `/users/${this.userHashId}/page_two/admin_edit_user/resume/${item.id}/toggle-block`;
                removeAction = `/users/${this.userHashId}/page_two/admin_edit_user/resume/${item.id}/remove`;
                postTypeText = 'резюме';
                break;
        }

        const toggleBlockBtnClass = item.blocked ?
            'btn btn-success btn-mod toggle-block-btn' :
            'btn btn-secondary btn-mod toggle-block-btn';

        col.innerHTML = `
        <div class="${cardClasses}">
            ${banners}
            <div class="post-image-wrapper">
                ${imageHtml}
            </div>
            <div class="post-content">
            
            <h6 class="post-title">${titleText}</h6>
            
                ${priceHtml ? `<p class="post-price mb-1"><strong>${priceHtml.includes('Цена') ? 'Цена:' : 'Зарплата:'}</strong> ${priceHtml.replace(/<strong>[^<]*<\/strong>\s*/, '')}</p>` : ''}
                
                <div class="post-user-info mb-2">
                    <small class="text-muted d-block">
                        <strong>Создан:</strong>
                        ${formatDate(item.createdAt)}
                    </small>
                    <small class="text-muted d-block">
                        <strong>Опубликовано:</strong>
                        ${formatDate(item.moderatedAt)}
                    </small>
                </div>
                
                <div class="action-buttons">
                    <a href="${editLink}" class="btn btn-warning btn-mod btn-sm">
                        <i class="fas fa-edit"></i>
                        <span class="d-none d-md-inline"> Редакт</span>
                    </a>
                    
                    <form action="${toggleBlockAction}" method="post" class="d-inline">
                        <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                        <button class="${toggleBlockBtnClass}"
                                type="button"
                                data-post-type="${postTypeText}"
                                data-action="${toggleBlockAction}">
                            <i class="${item.blocked ? 'fas fa-unlock' : 'fas fa-ban'}"></i>
                            <span class="d-none d-md-inline">${item.blocked ? ' Разблок.' : ' Блок.'}</span>
                        </button>
                    </form>
                    
                    <form action="${removeAction}" method="post" class="d-inline">
                        <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                        <button class="btn btn-danger btn-mod delete-post-btn"
                                type="button"
                                data-post-type="${postTypeText}"
                                data-action="${removeAction}">
                            <i class="fas fa-trash"></i>
                            <span class="d-none d-md-inline"> Удалить</span>
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
            message.className = 'col-12 text-center py-4 all-loaded-message';
            message.innerHTML = `
                <div class="alert alert-info">
                    <i class="fas fa-info-circle me-2"></i>
                    Нет ${this.getTypeDisplayName(type)} для отображения
                </div>
            `;
            container.appendChild(message);
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

    showError(message) {
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

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded, initializing admin user pagination...');
    try {
        window.adminUserPagination = new AdminUserPagination();
        console.log('Admin user pagination initialized successfully');
    } catch {
        console.log('Error initializing AdminUserPagination');
    }
});

// Глобальные обработчики для динамических кнопок
document.addEventListener('click', function(e) {
    // Обработка кнопок блокировки/разблокировки
    if (e.target.closest('.toggle-block-btn')) {
        e.preventDefault();
        const button = e.target.closest('.toggle-block-btn');
        const form = button.closest('form');
        const action = form.getAttribute('action');
        const isBlocked = button.classList.contains('btn-success') ? true : false;
        const postType = button.getAttribute('data-post-type') || 'пост';
        const actionText = isBlocked ? 'разблокировать' : 'заблокировать';

        const modalBody = document.getElementById('toggleBlockModalBody');
        if (modalBody) {
            modalBody.innerHTML = `Вы уверены, что хотите <strong>${actionText}</strong> этот ${postType}?`;

            const confirmBtn = document.getElementById('toggleBlockConfirmBtn');
            confirmBtn.className = isBlocked ? 'btn btn-success' : 'btn btn-warning';
            confirmBtn.textContent = isBlocked ? 'Разблокировать' : 'Заблокировать';

            const modalForm = document.getElementById('toggleBlockForm');
            modalForm.setAttribute('action', action);

            const modal = new bootstrap.Modal(document.getElementById('toggleBlockModal'));
            modal.show();
        }
    }

    // Обработка кнопок удаления
    if (e.target.closest('.delete-post-btn')) {
        e.preventDefault();
        const button = e.target.closest('.delete-post-btn');
        const form = button.closest('form');
        const action = form.getAttribute('action');
        const postType = button.getAttribute('data-post-type') || 'пост';

        const modalBody = document.getElementById('deletePostModalBody');
        if (modalBody) {
            modalBody.innerHTML = `Вы уверены, что хотите <strong>удалить</strong> этот ${postType}? Это действие нельзя отменить!`;

            const modalForm = document.getElementById('deletePostForm');
            modalForm.setAttribute('action', action);

            const modal = new bootstrap.Modal(document.getElementById('deletePostModal'));
            modal.show();
        }
    }
});