// Универсальный сайдбар, выезжающий сверху для всех устройств
class MainSidebar {
    constructor() {
        this.sidebar = null;
        this.overlay = null;
        this.toggleBtn = null;
        this.closeBtn = null;
        this.isOpen = false;
        this.sectionsState = {};
        this.isDesktop = window.innerWidth >= 992;
        this.init();
    }

    init() {
        this.createSidebarHTML();
        this.sidebar = document.getElementById('mainSidebar');
        this.overlay = document.getElementById('sidebarOverlay');

        this.toggleBtns = document.querySelectorAll('.sidebar-toggle');
        this.closeBtn = document.getElementById('sidebarCloseBtn');

        if (!this.sidebar) return;

        this.populateSidebar();
        this.bindEvents();
        this.loadSectionsState();
        this.setupResizeHandler();
    }

    createSidebarHTML() {
        if (document.getElementById('mainSidebar')) return;

        if (!document.getElementById('sidebarOverlay')) {
            const overlayHTML = `<div class="sidebar-overlay" id="sidebarOverlay"></div>`;
            document.body.insertAdjacentHTML('beforeend', overlayHTML);
        }

        const sidebarHTML = `
            <div class="main-sidebar" id="mainSidebar">
                <div class="sidebar-header">
                    <div class="sidebar-header-left">
                        <button class="sidebar-close" id="sidebarCloseBtn">
                            <i class="fas fa-times"></i>
                        </button>
                        <div class="sidebar-header-user" id="sidebarHeaderUser" style="display: none;">
                            <div class="sidebar-header-user-avatar">
                                <i class="fas fa-user-circle"></i>
                            </div>
                            <div class="sidebar-header-user-details">
                                <span class="sidebar-header-username" id="sidebarUsername"></span>
                                <div class="sidebar-header-user-rating" id="sidebarUserRating"></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="sidebar-nav" id="sidebarNav">
                    <div class="sidebar-sections-container" id="sidebarSectionsContainer"></div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', sidebarHTML);
    }

    getUserHashId() {
        const metaHashId = document.querySelector('meta[name="user-hash-id"]');
        if (metaHashId && metaHashId.getAttribute('content')) {
            return metaHashId.getAttribute('content');
        }

        const userInfo = document.querySelector('.user-info');
        if (userInfo && userInfo.getAttribute('data-hash-id')) {
            return userInfo.getAttribute('data-hash-id');
        }

        if (window.currentUserHashId) {
            return window.currentUserHashId;
        }

        return null;
    }

    isUserAuthenticated() {
        const logoutButton = document.querySelector('.btn-logout');
        const logoutForm = document.querySelector('form[action="/logout"]');
        const messagesLink = document.getElementById('messagesLink');
        const supportLink = document.querySelector('a[href="/support_service"]');

        return !!(logoutButton || logoutForm || messagesLink || supportLink);
    }

    populateSidebar() {
        if (!this.sidebar) return;

        const container = document.getElementById('sidebarSectionsContainer');
        if (!container) return;

        const isAuthenticated = this.isUserAuthenticated();
        const isDesktop = window.innerWidth >= 992;

        let showCreateBlock = false;
        let isSuperAdmin = false;
        let isAdmin = false;

        if (isAuthenticated && window.currentUserRole) {
            const role = window.currentUserRole;
            showCreateBlock = (role === 'ROLE_USER' || role === 'ROLE_SUPER');
            isSuperAdmin = (role === 'ROLE_SUPER');
            isAdmin = (role === 'ROLE_ADMIN' || role === 'ROLE_SUPER');
        }

        let sections = [];

        sections.push({
            title: 'Основное',
            defaultOpen: false,
            items: [
                { icon: 'fas fa-home', text: 'Главная', href: '/' },
                { icon: 'fas fa-star', text: 'Отзывы', href: '/page_one/reviews' }
            ]
        });

        sections.push({
            title: 'Категории',
            defaultOpen: false,
            items: [
                { icon: 'fas fa-box', text: 'Товары', href: '/page_one/viewsone' },
                { icon: 'fas fa-wrench', text: 'Услуги', href: '/page_one/viewsonejob' },
                { icon: 'fas fa-briefcase', text: 'Вакансии', href: '/page_one/viewsonevac' },
                { icon: 'fas fa-file-alt', text: 'Резюме', href: '/page_one/viewsoneres' }
            ]
        });

        if (isAuthenticated) {
            const hashId = this.getUserHashId();
            const reviewsAboutMeUrl = hashId ? `/page_one/reviews/${hashId}` : '#';

            // Личный кабинет с кнопкой удаления аккаунта внутри
            const accountItems = [
                { icon: 'fas fa-user-circle', text: 'Мой кабинет', href: '/page_two/user_edit' },
                { icon: 'fas fa-list-ul', text: 'Мои объявления', href: '/page_two/redactor', badge: 'blockedBadge', badgeId: 'mainSidebarBlockedBadge' },
                { icon: 'fas fa-heart', text: 'Избранное', href: '/page_one/favorites', bell: 'favBell', bellId: 'mainSidebarFavBell' },
                { icon: 'fas fa-comment-dots', text: 'Отзывы обо мне', href: reviewsAboutMeUrl }
            ];

            // Добавляем кнопку удаления аккаунта в личный кабинет
            const deleteModal = document.getElementById('deleteMyAccountModal');
            if (deleteModal) {
                accountItems.push({ icon: 'fas fa-trash-alt', text: 'Удалить аккаунт', isButton: true, id: 'mainSidebarDeleteAccountBtn' });
            }

            sections.push({
                title: 'Личный кабинет',
                defaultOpen: false,
                items: accountItems
            });

            if (showCreateBlock) {
                sections.push({
                    title: 'Создать',
                    defaultOpen: false,
                    items: [
                        { icon: 'fas fa-tag', text: 'Товар', href: '/page_two/viewstwo' },
                        { icon: 'fas fa-handshake', text: 'Услуга', href: '/page_two/viewstwojob' },
                        { icon: 'fas fa-users', text: 'Вакансия', href: '/page_two/viewstwovac' },
                        { icon: 'fas fa-id-card', text: 'Резюме', href: '/page_two/viewstwores' }
                    ]
                });
            }

            if (isAdmin) {
                let adminItems = [];

                if (isSuperAdmin) {
                    adminItems.push({ icon: 'fas fa-user-plus', text: 'Создать пользователя', href: '/admin-create-user' });
                }

                adminItems.push({ icon: 'fas fa-users', text: 'Управление пользователями', href: '/users' });

                // Убраны дублирующиеся пункты для суперадмина:
                // - Товары (админ)
                // - Услуги (админ)
                // - Вакансии (админ)
                // - Резюме (админ)
                // Они уже есть в секции "Создать"

                adminItems.push({
                    icon: 'fas fa-ban',
                    text: 'Заблокированные',
                    href: '/page_two/redactorblocked',
                    badge: 'blockedPostsBadge',
                    badgeId: 'mainSidebarBlockedPostsBadge'
                });

                adminItems.push({ icon: 'fas fa-gavel', text: 'Модерация', href: '/page_two/moderat' });

                sections.push({
                    title: 'Администрирование',
                    defaultOpen: false,
                    items: adminItems
                });
            }

            sections.push({
                title: 'Общение',
                defaultOpen: false,
                items: [
                    { icon: 'fas fa-envelope', text: 'Сообщения', href: '/user/notifications_message', badge: 'messageBadge', bell: 'messageBell', badgeId: 'mainSidebarUnreadCount', bellId: 'mainSidebarMessageBell' },
                    { icon: 'fas fa-headset', text: 'Поддержка', href: '/support_service', bell: 'supportBell', bellId: 'mainSidebarSupportBell' }
                ]
            });

            sections.push({
                title: 'Информация',
                defaultOpen: false,
                items: [
                    { icon: 'fas fa-info-circle', text: 'О нас', href: '/about' }
                ]
            });

        } else {
            sections.push({
                title: 'Аккаунт',
                defaultOpen: false,
                items: [
                    { icon: 'fas fa-user-plus', text: 'Регистрация', href: '/register' },
                    { icon: 'fas fa-sign-in-alt', text: 'Вход', href: '/login' }
                ]
            });

            sections.push({
                title: 'Информация',
                defaultOpen: false,
                items: [
                    { icon: 'fas fa-info-circle', text: 'О нас', href: '/about' }
                ]
            });
        }

        let sectionsHTML = '';

        for (let i = 0; i < sections.length; i++) {
            const section = sections[i];
            const sectionId = `sidebarSection_${i}`;
            const isOpen = this.sectionsState[section.title] !== undefined ? this.sectionsState[section.title] : section.defaultOpen;
            const openClass = isOpen ? 'open' : '';

            sectionsHTML += `
            <div class="sidebar-section ${openClass}" data-section-title="${section.title}" data-is-desktop="${isDesktop}">
                <div class="sidebar-section-header" data-section-toggle="${sectionId}">
                    <i class="fas fa-chevron-${isOpen ? 'down' : 'right'} section-toggle-icon"></i>
                    <span class="sidebar-section-title">${this.escapeHtml(section.title)}</span>
                </div>
                <div class="sidebar-section-content ${openClass}" id="${sectionId}">
                    <div class="sidebar-grid">
        `;

            for (const item of section.items) {
                if (item.isButton) {
                    sectionsHTML += `
                    <button class="sidebar-item" id="${item.id}">
                        <i class="${item.icon}"></i>
                        <span class="sidebar-item-text">${this.escapeHtml(item.text)}</span>
                    </button>
                `;
                } else {
                    let badgeHtml = '';
                    if (item.badge) {
                        badgeHtml = `<span id="${item.badgeId}" class="badge" style="display: none;">0</span>`;
                    }
                    if (item.bell) {
                        badgeHtml = `<span id="${item.bellId}" class="notification-pulse" style="display: none;"></span>`;
                    }

                    sectionsHTML += `
                    <a href="${this.escapeHtml(item.href)}" class="sidebar-item">
                        <i class="${item.icon}"></i>
                        <span class="sidebar-item-text">${this.escapeHtml(item.text)}</span>
                        ${badgeHtml}
                    </a>
                `;
                }
            }

            sectionsHTML += `
                    </div>
                </div>
            </div>
        `;
        }

        container.innerHTML = sectionsHTML;

        if (isAuthenticated) {
            this.fillUserInfo();
        }

        this.bindDynamicEvents();
        this.bindSectionToggleEvents();
    }

    escapeHtml(str) {
        if (!str) return '';
        return str.replace(/[&<>]/g, function(m) {
            if (m === '&') return '&amp;';
            if (m === '<') return '&lt;';
            if (m === '>') return '&gt;';
            return m;
        });
    }

    bindSectionToggleEvents() {
        const sections = document.querySelectorAll('.sidebar-section');
        const isDesktop = window.innerWidth >= 992;

        sections.forEach(section => {
            const header = section.querySelector('.sidebar-section-header');
            if (!header) return;

            const oldClickHandler = header._clickHandler;
            if (oldClickHandler) {
                header.removeEventListener('click', oldClickHandler);
            }

            const oldMouseEnterHandler = header._mouseEnterHandler;
            if (oldMouseEnterHandler) {
                header.removeEventListener('mouseenter', oldMouseEnterHandler);
            }

            const oldMouseLeaveHandler = header._mouseLeaveHandler;
            if (oldMouseLeaveHandler) {
                header.removeEventListener('mouseleave', oldMouseLeaveHandler);
            }

            if (isDesktop) {
                const mouseEnterHandler = (e) => {
                    if (!section.classList.contains('open')) {
                        this.openSectionOnDesktop(section);
                    }
                };

                const mouseLeaveHandler = (e) => {
                    if (section.classList.contains('open') && !section.classList.contains('pinned')) {
                        this.closeSectionOnDesktop(section);
                    }
                };

                header._mouseEnterHandler = mouseEnterHandler;
                header._mouseLeaveHandler = mouseLeaveHandler;

                header.addEventListener('mouseenter', mouseEnterHandler);
                section.addEventListener('mouseleave', mouseLeaveHandler);

                const clickHandler = (e) => {
                    e.stopPropagation();
                    this.togglePinSection(section);
                };
                header._clickHandler = clickHandler;
                header.addEventListener('click', clickHandler);
            } else {
                const clickHandler = (e) => {
                    e.stopPropagation();
                    this.toggleMobileSection(section);
                };
                header._clickHandler = clickHandler;
                header.addEventListener('click', clickHandler);
            }
        });
    }

    openSectionOnDesktop(section) {
        const content = section.querySelector('.sidebar-section-content');
        const toggleIcon = section.querySelector('.section-toggle-icon');

        if (content && !content.classList.contains('open')) {
            content.classList.add('open');
            section.classList.add('open');
            if (toggleIcon) {
                toggleIcon.className = 'fas fa-chevron-down section-toggle-icon';
            }
        }
    }

    closeSectionOnDesktop(section) {
        const content = section.querySelector('.sidebar-section-content');
        const toggleIcon = section.querySelector('.section-toggle-icon');

        if (content && content.classList.contains('open') && !section.classList.contains('pinned')) {
            content.classList.remove('open');
            section.classList.remove('open');
            if (toggleIcon) {
                toggleIcon.className = 'fas fa-chevron-right section-toggle-icon';
            }
        }
    }

    togglePinSection(section) {
        const content = section.querySelector('.sidebar-section-content');
        const toggleIcon = section.querySelector('.section-toggle-icon');
        const sectionTitle = section.getAttribute('data-section-title');
        const isOpen = content.classList.contains('open');

        if (isOpen) {
            content.classList.remove('open');
            section.classList.remove('open', 'pinned');
            if (toggleIcon) {
                toggleIcon.className = 'fas fa-chevron-right section-toggle-icon';
            }
            this.sectionsState[sectionTitle] = false;
        } else {
            content.classList.add('open');
            section.classList.add('open', 'pinned');
            if (toggleIcon) {
                toggleIcon.className = 'fas fa-chevron-down section-toggle-icon';
            }
            this.sectionsState[sectionTitle] = true;
        }

        this.saveSectionsState();
    }

    toggleMobileSection(section) {
        const content = section.querySelector('.sidebar-section-content');
        const toggleIcon = section.querySelector('.section-toggle-icon');
        const sectionTitle = section.getAttribute('data-section-title');
        const isOpen = content.classList.contains('open');

        if (isOpen) {
            content.classList.remove('open');
            section.classList.remove('open');
            if (toggleIcon) {
                toggleIcon.className = 'fas fa-chevron-right section-toggle-icon';
            }
            this.sectionsState[sectionTitle] = false;
        } else {
            content.classList.add('open');
            section.classList.add('open');
            if (toggleIcon) {
                toggleIcon.className = 'fas fa-chevron-down section-toggle-icon';
            }
            this.sectionsState[sectionTitle] = true;
        }

        this.saveSectionsState();
    }

    saveSectionsState() {
        try {
            localStorage.setItem('sidebarSectionsState', JSON.stringify(this.sectionsState));
        } catch(e) {}
    }

    loadSectionsState() {
        try {
            const saved = localStorage.getItem('sidebarSectionsState');
            if (saved) {
                this.sectionsState = JSON.parse(saved);
            }
        } catch(e) {}
    }

    getCsrfToken() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        if (token) return token;
        const headerToken = document.querySelector('input[name="_csrf"]')?.value;
        return headerToken || '';
    }

    fillUserInfo() {
        const userInfoBlock = document.getElementById('sidebarHeaderUser');
        const usernameSpan = document.getElementById('sidebarUsername');
        const ratingDiv = document.getElementById('sidebarUserRating');

        let username = '';
        const usernameElement = document.querySelector('.user-info span');
        if (usernameElement) {
            username = usernameElement.textContent;
        }

        if (username) {
            usernameSpan.textContent = username;
            userInfoBlock.style.display = 'flex';
        }

        const ratingElement = document.querySelector('.user-rating');
        if (ratingElement && ratingDiv) {
            ratingDiv.innerHTML = ratingElement.innerHTML;
        }
    }

    bindEvents() {
        if (this.toggleBtns && this.toggleBtns.length) {
            this.toggleBtns.forEach(btn => {
                btn.removeEventListener('click', this.openHandler);
                this.openHandler = (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    this.open();
                };
                btn.addEventListener('click', this.openHandler);
            });
        }

        if (this.closeBtn) {
            this.closeBtn.removeEventListener('click', this.closeHandler);
            this.closeHandler = () => this.close();
            this.closeBtn.addEventListener('click', this.closeHandler);
        }

        if (this.overlay) {
            this.overlay.removeEventListener('click', this.overlayCloseHandler);
            this.overlayCloseHandler = () => this.close();
            this.overlay.addEventListener('click', this.overlayCloseHandler);
        }

        document.removeEventListener('keydown', this.escHandler);
        this.escHandler = (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        };
        document.addEventListener('keydown', this.escHandler);
    }

    bindDynamicEvents() {
        const deleteBtn = document.getElementById('mainSidebarDeleteAccountBtn');
        if (deleteBtn) {
            deleteBtn.removeEventListener('click', this.deleteHandler);
            this.deleteHandler = (e) => {
                e.preventDefault();
                e.stopPropagation();

                // Закрываем сайдбар
                this.close();

                // Находим и открываем модальное окно удаления аккаунта
                setTimeout(() => {
                    // Пытаемся найти кнопку с data-bs-target
                    let modalTrigger = document.querySelector('[data-bs-target="#deleteMyAccountModal"]');

                    // Если не нашли, ищем кнопку с ID deleteAccountBtn
                    if (!modalTrigger) {
                        modalTrigger = document.getElementById('deleteAccountBtn');
                    }

                    // Если нашли триггер, кликаем по нему
                    if (modalTrigger) {
                        modalTrigger.click();
                    } else {
                        // Если не нашли кнопку-триггер, пробуем найти само модальное окно и показать его
                        const modal = document.getElementById('deleteMyAccountModal');
                        if (modal) {
                            // Для Bootstrap 5
                            if (typeof bootstrap !== 'undefined' && bootstrap.Modal) {
                                const bsModal = new bootstrap.Modal(modal);
                                bsModal.show();
                            }
                            // Для Bootstrap 4 или jQuery
                            else if (typeof $ !== 'undefined' && $.fn.modal) {
                                $(modal).modal('show');
                            }
                            // Если ничего не подходит, просто показываем вручную
                            else {
                                modal.style.display = 'block';
                                modal.classList.add('show');
                                document.body.classList.add('modal-open');

                                // Добавляем backdrop
                                let backdrop = document.querySelector('.modal-backdrop');
                                if (!backdrop) {
                                    backdrop = document.createElement('div');
                                    backdrop.className = 'modal-backdrop fade show';
                                    document.body.appendChild(backdrop);
                                }
                            }
                        } else {
                            console.warn('Modal deleteMyAccountModal not found');
                        }
                    }
                }, 300);
            };
            deleteBtn.addEventListener('click', this.deleteHandler);
        }

        const allLinks = this.sidebar.querySelectorAll('a');
        allLinks.forEach(link => {
            link.removeEventListener('click', this.linkCloseHandler);
            this.linkCloseHandler = () => {
                this.close();
            };
            link.addEventListener('click', this.linkCloseHandler);
        });
    }

    setupResizeHandler() {
        let resizeTimeout;
        window.addEventListener('resize', () => {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(() => {
                const wasDesktop = this.isDesktop;
                this.isDesktop = window.innerWidth >= 992;

                if (wasDesktop !== this.isDesktop) {
                    const pinnedSections = {};
                    document.querySelectorAll('.sidebar-section.pinned').forEach(section => {
                        const title = section.getAttribute('data-section-title');
                        if (title) pinnedSections[title] = true;
                    });

                    Object.assign(this.sectionsState, pinnedSections);

                    this.populateSidebar();
                    this.bindDynamicEvents();
                    this.bindSectionToggleEvents();
                }
            }, 200);
        });
    }

    open() {
        if (!this.sidebar) return;
        this.sidebar.classList.add('active');
        if (this.overlay) {
            this.overlay.classList.add('active');
        }
        document.body.style.overflow = 'hidden';
        this.isOpen = true;

        // Скрываем хедер при открытом сайдбаре
        const header = document.querySelector('.site-header');
        if (header) {
            header.style.visibility = 'hidden';
            header.style.opacity = '0';
            header.style.transition = 'visibility 0.3s, opacity 0.3s';
        }

        if (this.toggleBtns && this.toggleBtns.length) {
            this.toggleBtns.forEach(btn => {
                btn.classList.add('sidebar-open');
            });
        }
    }

    close() {
        if (!this.sidebar) return;
        this.sidebar.classList.remove('active');
        if (this.overlay) {
            this.overlay.classList.remove('active');
        }
        document.body.style.overflow = '';
        this.isOpen = false;

        // Показываем хедер обратно
        const header = document.querySelector('.site-header');
        if (header) {
            header.style.visibility = '';
            header.style.opacity = '';
        }

        if (this.toggleBtns && this.toggleBtns.length) {
            this.toggleBtns.forEach(btn => {
                btn.classList.remove('sidebar-open');
            });
        }
    }

    updateMessageCount(count) {
        const badge = document.getElementById('mainSidebarUnreadCount');
        const bell = document.getElementById('mainSidebarMessageBell');

        if (count > 0) {
            if (badge) {
                badge.textContent = count;
                badge.style.display = 'inline-block';
            }
            if (bell) bell.style.display = 'inline-block';
        } else {
            if (badge) badge.style.display = 'none';
            if (bell) bell.style.display = 'none';
        }
    }

    updateSupportNotification(hasNotification) {
        const bell = document.getElementById('mainSidebarSupportBell');
        if (bell) {
            bell.style.display = hasNotification ? 'inline-block' : 'none';
        }
    }

    updateBlockedBadge(count) {
        const badge = document.getElementById('mainSidebarBlockedBadge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    }

    updateFavNotification(hasNotification) {
        const bell = document.getElementById('mainSidebarFavBell');
        if (bell) {
            bell.style.display = hasNotification ? 'inline-block' : 'none';
        }
    }
}

let mainSidebar = null;

document.addEventListener('DOMContentLoaded', function() {
    const initSidebar = () => {
        if (!mainSidebar && document.querySelector('.sidebar-toggle')) {
            mainSidebar = new MainSidebar();
            window.mainSidebar = mainSidebar;
        }
    };

    initSidebar();

    const observer = new MutationObserver(function() {
        if (!mainSidebar && document.querySelector('.sidebar-toggle')) {
            initSidebar();
        }
    });

    observer.observe(document.body, { childList: true, subtree: true });
});