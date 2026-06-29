// Мобильный сайдбар
class MobileSidebar {
    constructor() {
        this.sidebar = null;
        this.overlay = null;
        this.toggleBtn = null;
        this.isOpen = false;
        this.isHeaderMode = true; // Флаг: true - в хедере, false - в режиме скролла
        this.init();
    }

    init() {
        this.createSidebarHTML();
        this.sidebar = document.getElementById('mobileSidebar');
        this.overlay = document.getElementById('sidebarOverlay');
        this.toggleBtn = document.getElementById('sidebarToggleBtn');

        if (!this.sidebar) return;

        this.populateSidebar();
        this.bindEvents();
        this.setupHeaderPosition();
        this.checkVisibilityOnScroll();
    }

    createSidebarHTML() {
        if (document.getElementById('mobileSidebar')) return;

        const sidebarHTML = `
            <button class="sidebar-toggle" id="sidebarToggleBtn" title="Меню">
                <i class="fas fa-bars"></i>
            </button>
            <div class="sidebar-overlay" id="sidebarOverlay"></div>
            <div class="mobile-sidebar" id="mobileSidebar">
                <div class="sidebar-header">
                    <h3><i class="fas fa-bars"></i> Меню</h3>
                    <button class="sidebar-close" id="sidebarCloseBtn">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="sidebar-user-info" id="sidebarUserInfo" style="display: none;">
                    <div class="sidebar-user-avatar">
                        <i class="fas fa-user-circle"></i>
                    </div>
                    <div class="sidebar-user-details">
                        <span class="sidebar-username" id="sidebarUsername"></span>
                        <div class="sidebar-user-rating" id="sidebarUserRating"></div>
                    </div>
                </div>
                <div class="sidebar-nav" id="sidebarNav">
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

    setupHeaderPosition() {
        if (!this.toggleBtn) return;

        // Ищем первую строку хедера
        const headerFirstRow = document.querySelector('.site-header .nav-container:first-child .nav-row');

        if (headerFirstRow) {
            // Добавляем бургер в начало первой строки хедера
            headerFirstRow.style.position = 'relative';
            headerFirstRow.insertBefore(this.toggleBtn, headerFirstRow.firstChild);

            // Добавляем класс для позиционирования в хедере
            this.toggleBtn.classList.add('header-position');
            this.isHeaderMode = true;
        }
    }

    populateSidebar() {
        if (!this.sidebar) return;

        const navContainer = document.getElementById('sidebarNav');
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
                        <span id="sidebarBlockedBadge" class="badge" style="display: none;"></span>
                    </a>
                    <a href="/page_one/favorites" class="sidebar-item">
                        <i class="fas fa-heart"></i> Избранное
                        <span id="sidebarFavBell" class="notification-pulse" style="display: none;"></span>
                    </a>
                </div>
                
                <div class="sidebar-section">
                    <div class="sidebar-section-title">Отзывы</div>
                    <a href="#" class="sidebar-item" id="sidebarMyReviewsLink">
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
                    <a href="/user/notifications_message" class="sidebar-item" id="sidebarMessagesLink">
                        <i class="fas fa-envelope"></i> Сообщения
                        <span id="sidebarMessageBell" class="notification-pulse" style="display: none;"></span>
                        <span id="sidebarUnreadCount" class="badge" style="display: none;">0</span>
                    </a>
                    <a href="/support_service" class="sidebar-item" id="sidebarSupportLink">
                        <i class="fas fa-headset"></i> Поддержка
                        <span id="sidebarSupportBell" class="notification-pulse" style="display: none;"></span>
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
                        <button class="sidebar-item" id="sidebarDeleteAccountBtn">
                            <i class="fas fa-trash-alt"></i> Удалить аккаунт
                        </button>
                    </div>
                `;
            }

            navHTML += `
                <div class="sidebar-logout">
                    <form action="/logout" method="post" id="sidebarLogoutForm" style="width: 100%;">
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
        const myReviewsLink = document.getElementById('sidebarMyReviewsLink');
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
        const userInfoBlock = document.getElementById('sidebarUserInfo');
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
        if (this.toggleBtn) {
            this.toggleBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.open();
            });
        }

        const closeBtn = document.getElementById('sidebarCloseBtn');
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
        const deleteBtn = document.getElementById('sidebarDeleteAccountBtn');
        if (deleteBtn) {
            deleteBtn.addEventListener('click', () => {
                const modalBtn = document.querySelector('[data-bs-target="#deleteMyAccountModal"]');
                if (modalBtn) {
                    modalBtn.click();
                    this.close();
                }
            });
        }

        const allLinks = this.sidebar.querySelectorAll('a:not(.sidebar-logout a), .sidebar-logout button');
        allLinks.forEach(link => {
            link.addEventListener('click', () => {
                setTimeout(() => this.close(), 100);
            });
        });
    }

    open() {
        this.sidebar.classList.add('active');
        this.overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
        this.isOpen = true;

        if (this.toggleBtn) {
            this.toggleBtn.classList.add('active');
            const icon = this.toggleBtn.querySelector('i');
            if (icon) {
                icon.className = 'fas fa-times';
            }
        }
    }

    close() {
        this.sidebar.classList.remove('active');
        this.overlay.classList.remove('active');
        document.body.style.overflow = '';
        this.isOpen = false;

        if (this.toggleBtn) {
            this.toggleBtn.classList.remove('active');
            const icon = this.toggleBtn.querySelector('i');
            if (icon) {
                icon.className = 'fas fa-bars';
            }
        }
    }

    switchToScrollMode() {
        if (!this.toggleBtn || !this.isHeaderMode) return;

        // Перемещаем бургер в body для fixed позиционирования
        document.body.appendChild(this.toggleBtn);

        // Меняем классы
        this.toggleBtn.classList.remove('header-position');
        this.toggleBtn.classList.add('scroll-mode');

        this.isHeaderMode = false;
    }

    switchToHeaderMode() {
        if (!this.toggleBtn || this.isHeaderMode) return;

        // Возвращаем бургер обратно в хедер
        const headerFirstRow = document.querySelector('.site-header .nav-container:first-child .nav-row');
        if (headerFirstRow) {
            headerFirstRow.insertBefore(this.toggleBtn, headerFirstRow.firstChild);
            this.toggleBtn.classList.remove('scroll-mode', 'visible');
            this.toggleBtn.classList.add('header-position');
            this.isHeaderMode = true;
        }
    }

    checkVisibilityOnScroll() {
        if (!this.toggleBtn) return;

        let ticking = false;
        let lastScrollY = 0;
        let hideTimeout = null;
        let isHeaderVisible = true;

        const updateHeaderVisibility = () => {
            const siteHeader = document.getElementById('site-header');
            if (!siteHeader) return true;

            const headerRect = siteHeader.getBoundingClientRect();
            const headerHeight = siteHeader.offsetHeight;

            if (siteHeader.classList.contains('sticky')) {
                const scrollY = window.pageYOffset;
                isHeaderVisible = scrollY <= headerHeight + 20;
            } else {
                isHeaderVisible = headerRect.bottom > 0;
            }

            return isHeaderVisible;
        };

        const onScroll = () => {
            if (window.innerWidth > 768) return;
            if (!this.toggleBtn) return;

            if (!ticking) {
                requestAnimationFrame(() => {
                    const currentScrollY = window.pageYOffset;
                    const wasHeaderVisible = updateHeaderVisibility();

                    // При скролле вниз и хедер скрыт - переключаем в режим скролла и показываем бургер
                    if (currentScrollY > lastScrollY && currentScrollY > 50 && !isHeaderVisible) {
                        if (this.isHeaderMode) {
                            this.switchToScrollMode();
                        }
                        // Показываем бургер
                        if (!this.isOpen) {
                            this.toggleBtn.classList.add('visible');
                        }
                        if (hideTimeout) clearTimeout(hideTimeout);
                    }
                    // При скролле вверх, когда хедер становится видимым - возвращаем в хедер
                    else if (currentScrollY < lastScrollY && isHeaderVisible) {
                        if (!this.isHeaderMode) {
                            this.switchToHeaderMode();
                        }
                        // Скрываем бургер, так как он теперь в хедере
                        if (!this.isOpen) {
                            this.toggleBtn.classList.remove('visible');
                        }
                    }

                    // В самом верху страницы - убеждаемся что бургер в хедере
                    if (currentScrollY < 30 && !this.isHeaderMode) {
                        this.switchToHeaderMode();
                        this.toggleBtn.classList.remove('visible');
                    }

                    lastScrollY = currentScrollY;
                    ticking = false;
                });
                ticking = true;
            }
        };

        window.addEventListener('scroll', onScroll);
        window.addEventListener('resize', () => {
            if (window.innerWidth > 768) {
                if (this.toggleBtn) {
                    this.toggleBtn.classList.remove('visible', 'scroll-mode');
                }
            }
        });

        // Инициализация: показываем бургер в хедере
        setTimeout(() => {
            if (window.innerWidth <= 768 && this.toggleBtn && this.isHeaderMode) {
                this.toggleBtn.classList.add('header-position');
            }
        }, 100);
    }

    updateMessageCount(count) {
        const badge = document.getElementById('sidebarUnreadCount');
        const bell = document.getElementById('sidebarMessageBell');

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
        const bell = document.getElementById('sidebarSupportBell');
        if (bell) {
            bell.style.display = hasNotification ? 'inline-block' : 'none';
        }
    }

    updateBlockedBadge(count) {
        const badge = document.getElementById('sidebarBlockedBadge');
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

let mobileSidebar;

document.addEventListener('DOMContentLoaded', function() {
    setTimeout(() => {
        if (window.innerWidth <= 768) {
            mobileSidebar = new MobileSidebar();
            window.mobileSidebar = mobileSidebar;
        }
    }, 100);

    window.addEventListener('resize', function() {
        if (window.innerWidth <= 768 && !mobileSidebar) {
            mobileSidebar = new MobileSidebar();
            window.mobileSidebar = mobileSidebar;
        } else if (window.innerWidth > 768 && mobileSidebar) {
            const sidebar = document.getElementById('mobileSidebar');
            const overlay = document.getElementById('sidebarOverlay');
            const toggle = document.getElementById('sidebarToggleBtn');
            if (sidebar) sidebar.remove();
            if (overlay) overlay.remove();
            if (toggle && toggle.parentNode) toggle.remove();
            mobileSidebar = null;
            window.mobileSidebar = null;
        }
    });
});