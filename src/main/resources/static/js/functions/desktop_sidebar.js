class DesktopSidebar {
    constructor() {
        this.sidebar = null;
        this.overlay = null;
        this.toggleBtn = null;
        this.isOpen = false;
        this.init();
    }

    init() {
        this.createSidebarHTML();
        this.sidebar = document.getElementById('desktopSidebar');
        this.overlay = document.getElementById('sidebarOverlay');

        // Используем существующую кнопку .burger-btn
        this.toggleBtn = document.getElementById('burgerToggleBtn');

        if (!this.sidebar || !this.toggleBtn) return;

        this.populateSidebar();
        this.bindEvents();
    }

    createSidebarHTML() {
        if (document.getElementById('desktopSidebar')) return;

        // Оверлей уже может существовать от мобильного сайдбара
        if (!document.getElementById('sidebarOverlay')) {
            const overlayHTML = `<div class="sidebar-overlay" id="sidebarOverlay"></div>`;
            document.body.insertAdjacentHTML('beforeend', overlayHTML);
        }

        const sidebarHTML = `
            <div class="desktop-sidebar" id="desktopSidebar">
                <div class="sidebar-header">
                    <h3><i class="fas fa-bars"></i> Меню</h3>
                    <button class="sidebar-close" id="desktopSidebarCloseBtn">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="sidebar-user-info" id="desktopSidebarUserInfo" style="display: none;">
                    <div class="sidebar-user-avatar">
                        <i class="fas fa-user-circle"></i>
                    </div>
                    <div class="sidebar-user-details">
                        <span class="sidebar-username" id="desktopSidebarUsername"></span>
                        <div class="sidebar-user-rating" id="desktopSidebarUserRating"></div>
                    </div>
                </div>
                <div class="sidebar-nav" id="desktopSidebarNav">
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

        const navContainer = document.getElementById('desktopSidebarNav');
        if (!navContainer) return;

        const isAuthenticated = this.isUserAuthenticated();

        // Получаем роль из глобальной переменной
        let showCreateBlock = false;

        if (isAuthenticated && window.currentUserRole) {
            const role = window.currentUserRole;
            // Показываем блок "Создать" только для USER и SUPER
            showCreateBlock = (role === 'ROLE_USER' || role === 'ROLE_SUPER');
        }

        let navHTML = '';

        if (isAuthenticated) {
            navHTML += `
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Основное</div>
                    <a href="/" class="sidebar-item">
                        <i class="fas fa-home"></i> Главная
                    </a>
                    <a href="/page_one/reviews" class="sidebar-item">
                        <i class="fas fa-star"></i> Отзывы
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Категории</div>
                    <a href="/page_one/viewsone" class="sidebar-item">
                        <i class="fas fa-box"></i> Товары
                    </a>
                    <a href="/page_one/viewsonejob" class="sidebar-item">
                        <i class="fas fa-wrench"></i> Услуги
                    </a>
                    <a href="/page_one/viewsonevac" class="sidebar-item">
                        <i class="fas fa-briefcase"></i> Вакансии
                    </a>
                    <a href="/page_one/viewsoneres" class="sidebar-item">
                        <i class="fas fa-file-alt"></i> Резюме
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Личный кабинет</div>
                    <a href="/page_two/user_edit" class="sidebar-item">
                        <i class="fas fa-user-circle"></i> Мой кабинет
                    </a>
                    <a href="/page_two/redactor" class="sidebar-item">
                        <i class="fas fa-list-ul"></i> Мои объявления
                        <span id="desktopSidebarBlockedBadge" class="badge" style="display: none;"></span>
                    </a>
                    <a href="/page_one/favorites" class="sidebar-item">
                        <i class="fas fa-heart"></i> Избранное
                        <span id="desktopSidebarFavBell" class="notification-pulse" style="display: none;"></span>
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Отзывы</div>
                    <a href="#" class="sidebar-item" id="desktopSidebarMyReviewsLink">
                        <i class="fas fa-comment-dots"></i> Отзывы обо мне
                    </a>
                </div>`;

            // Блок "Создать объявление" — ТОЛЬКО для USER и SUPER
            if (showCreateBlock) {
                navHTML += `
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Создать объявление</div>
                    <a href="/page_two/viewstwo" class="sidebar-item">
                        <i class="fas fa-tag"></i> Товар
                    </a>
                    <a href="/page_two/viewstwojob" class="sidebar-item">
                        <i class="fas fa-handshake"></i> Услуга
                    </a>
                    <a href="/page_two/viewstwovac" class="sidebar-item">
                        <i class="fas fa-users"></i> Вакансия
                    </a>
                    <a href="/page_two/viewstwores" class="sidebar-item">
                        <i class="fas fa-id-card"></i> Резюме
                    </a>
                </div>`;
            }

            navHTML += `
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Общение</div>
                    <a href="/user/notifications_message" class="sidebar-item" id="desktopSidebarMessagesLink">
                        <i class="fas fa-envelope"></i> Сообщения
                        <span id="desktopSidebarMessageBell" class="notification-pulse" style="display: none;"></span>
                        <span id="desktopSidebarUnreadCount" class="badge" style="display: none;">0</span>
                    </a>
                    <a href="/support_service" class="sidebar-item" id="desktopSidebarSupportLink">
                        <i class="fas fa-headset"></i> Поддержка
                        <span id="desktopSidebarSupportBell" class="notification-pulse" style="display: none;"></span>
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Информация</div>
                    <a href="/about" class="sidebar-item">
                        <i class="fas fa-info-circle"></i> О нас
                    </a>
                </div>
            `;

            const deleteModal = document.getElementById('deleteMyAccountModal');
            if (deleteModal) {
                navHTML += `
                    <div class="sidebar-section">
                        <button class="sidebar-item" id="desktopSidebarDeleteAccountBtn">
                            <i class="fas fa-trash-alt"></i> Удалить аккаунт
                        </button>
                    </div>
                `;
            }

            navHTML += `
                <div class="sidebar-logout">
                    <form action="/logout" method="post" id="desktopSidebarLogoutForm" style="width: 100%;">
                        <input type="hidden" name="_csrf" value="${this.getCsrfToken()}" />
                        <button type="submit" class="sidebar-item" style="width: 100%;">
                            <i class="fas fa-sign-out-alt"></i> Выход
                        </button>
                    </form>
                </div>
            `;

        } else {
            navHTML += `
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Основное</div>
                    <a href="/" class="sidebar-item">
                        <i class="fas fa-home"></i> Главная
                    </a>
                    <a href="/page_one/reviews" class="sidebar-item">
                        <i class="fas fa-star"></i> Отзывы
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Категории</div>
                    <a href="/page_one/viewsone" class="sidebar-item">
                        <i class="fas fa-box"></i> Товары
                    </a>
                    <a href="/page_one/viewsonejob" class="sidebar-item">
                        <i class="fas fa-wrench"></i> Услуги
                    </a>
                    <a href="/page_one/viewsonevac" class="sidebar-item">
                        <i class="fas fa-briefcase"></i> Вакансии
                    </a>
                    <a href="/page_one/viewsoneres" class="sidebar-item">
                        <i class="fas fa-file-alt"></i> Резюме
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Аккаунт</div>
                    <a href="/register" class="sidebar-item">
                        <i class="fas fa-user-plus"></i> Регистрация
                    </a>
                    <a href="/login" class="sidebar-item">
                        <i class="fas fa-sign-in-alt"></i> Вход
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Информация</div>
                    <a href="/about" class="sidebar-item">
                        <i class="fas fa-info-circle"></i> О нас
                    </a>
                </div>
            `;
        }

        navContainer.innerHTML = navHTML;

        if (isAuthenticated) {
            this.fillUserInfo();
            this.setupMyReviewsLink();
        }

        this.bindDynamicEvents();
    }

    setupMyReviewsLink() {
        const myReviewsLink = document.getElementById('desktopSidebarMyReviewsLink');
        if (myReviewsLink) {
            const hashId = this.getUserHashId();

            if (hashId) {
                myReviewsLink.href = '/page_one/reviews/' + hashId;
            } else {
                myReviewsLink.href = '#';
                myReviewsLink.style.opacity = '0.6';
                myReviewsLink.style.cursor = 'not-allowed';
                console.warn('Unable to get user hashId for reviews link');
            }
        }
    }

    getCsrfToken() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        if (token) return token;

        const headerToken = document.querySelector('input[name="_csrf"]')?.value;
        return headerToken || '';
    }

    fillUserInfo() {
        const userInfoBlock = document.getElementById('desktopSidebarUserInfo');
        const usernameSpan = document.getElementById('desktopSidebarUsername');
        const ratingDiv = document.getElementById('desktopSidebarUserRating');

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
        if (this.toggleBtn) {
            this.toggleBtn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.toggle();
            });
        }

        const closeBtn = document.getElementById('desktopSidebarCloseBtn');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.close());
        }

        if (this.overlay) {
            this.overlay.addEventListener('click', () => this.close());
        }

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.close();
            }
        });
    }

    bindDynamicEvents() {
        const deleteBtn = document.getElementById('desktopSidebarDeleteAccountBtn');
        if (deleteBtn) {
            deleteBtn.addEventListener('click', () => {
                const modalBtn = document.querySelector('[data-bs-target="#deleteMyAccountModal"]');
                if (modalBtn) {
                    modalBtn.click();
                    this.close();
                }
            });
        }

        // Закрываем сайдбар при клике на любую ссылку
        const allLinks = this.sidebar.querySelectorAll('a:not(.sidebar-logout a), .sidebar-logout button');
        allLinks.forEach(link => {
            link.addEventListener('click', () => {
                setTimeout(() => this.close(), 100);
            });
        });
    }

    toggle() {
        if (this.isOpen) {
            this.close();
        } else {
            this.open();
        }
    }

    open() {
        this.sidebar.classList.add('active');
        if (this.overlay) {
            this.overlay.classList.add('active');
        }
        document.body.style.overflow = 'hidden';
        this.isOpen = true;
    }

    close() {
        this.sidebar.classList.remove('active');
        if (this.overlay) {
            this.overlay.classList.remove('active');
        }
        document.body.style.overflow = '';
        this.isOpen = false;
    }

    updateMessageCount(count) {
        const badge = document.getElementById('desktopSidebarUnreadCount');
        const bell = document.getElementById('desktopSidebarMessageBell');

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
        const bell = document.getElementById('desktopSidebarSupportBell');
        if (bell) {
            bell.style.display = hasNotification ? 'inline-block' : 'none';
        }
    }

    updateBlockedBadge(count) {
        const badge = document.getElementById('desktopSidebarBlockedBadge');
        if (badge) {
            if (count > 0) {
                badge.textContent = count;
                badge.style.display = 'inline-block';
            } else {
                badge.style.display = 'none';
            }
        }
    }
}

let desktopSidebar = null;

// Инициализация десктопного сайдбара только на экранах шире 768px
document.addEventListener('DOMContentLoaded', function() {
    const initDesktopSidebar = () => {
        if (window.innerWidth > 768 && !desktopSidebar) {
            desktopSidebar = new DesktopSidebar();
            window.desktopSidebar = desktopSidebar;
        } else if (window.innerWidth <= 768 && desktopSidebar) {
            // Удаляем десктопный сайдбар при переходе на мобильный
            const sidebar = document.getElementById('desktopSidebar');
            if (sidebar) sidebar.remove();
            desktopSidebar = null;
            window.desktopSidebar = null;
        }
    };

    // Немного задерживаем инициализацию, чтобы убедиться, что DOM готов
    setTimeout(initDesktopSidebar, 100);

    // Следим за изменением размера окна
    window.addEventListener('resize', function() {
        setTimeout(initDesktopSidebar, 50);
    });
});