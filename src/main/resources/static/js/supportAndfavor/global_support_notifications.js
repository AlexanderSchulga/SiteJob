// Глобальная проверка уведомлений поддержки

class GlobalSupportNotifications {
    constructor() {
        this.notificationBellMobile = null;
        this.notificationBellDesktop = null;
        this.interval = null;
        this.isInitialized = false;
        this.userType = null;

        this.init();
    }

    init() {
        this.findNotificationBells();

        if (this.notificationBellMobile || this.notificationBellDesktop) {
            // Определяем тип пользователя из window.currentUserRole
            const role = window.currentUserRole;
            const isSuper = role === 'ROLE_SUPER';
            const isAdmin = role === 'ROLE_ADMIN' || role === 'ROLE_SUPER';

            this.userType = (isSuper || isAdmin) ? 'SUPER' : 'USER';

            console.log('User role User type');

            this.startPolling();
            this.isInitialized = true;
            console.log('Глобальные уведомления поддержки инициализированы для');
        } else {
            console.log('Колокольчики уведомлений поддержки не найдены');
        }
    }

    findNotificationBells() {
        this.notificationBellMobile = document.getElementById('supportNotificationBell');
        this.notificationBellDesktop = document.getElementById('supportNotificationBellDesktop');

        if (!this.notificationBellMobile && !this.notificationBellDesktop) {
            setTimeout(() => {
                this.notificationBellMobile = document.getElementById('supportNotificationBell');
                this.notificationBellDesktop = document.getElementById('supportNotificationBellDesktop');
                if (this.notificationBellMobile || this.notificationBellDesktop) {
                    const role = window.currentUserRole;
                    const isSuper = role === 'ROLE_SUPER';
                    const isAdmin = role === 'ROLE_ADMIN' || role === 'ROLE_SUPER';
                    this.userType = (isSuper || isAdmin) ? 'SUPER' : 'USER';
                    this.startPolling();
                    this.isInitialized = true;
                }
            }, 500);
        }

        return this.notificationBellMobile || this.notificationBellDesktop;
    }

    startPolling() {
        this.checkNotifications();
        this.interval = setInterval(() => {
            this.checkNotifications();
        }, 30000);
    }

    checkNotifications() {
        fetch('/api/support/notificationStatus')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Сетевой ответ был неудовлетворительным');
                }
                return response.json();
            })
            .then(data => {
                this.updateAllNotificationBells(data);
            })
            .catch(() => {
                console.log('Ошибка при проверке уведомлений поддержки');
            });
    }

    updateAllNotificationBells(data) {
        const hasNotification = data.hasAnyNotification;

        // Обновляем мобильный колокольчик
        if (this.notificationBellMobile) {
            if (hasNotification) {
                this.notificationBellMobile.style.display = 'inline-block';
                this.pulseAnimation(this.notificationBellMobile);
                this.updateNotificationTooltip(data, this.notificationBellMobile);
            } else {
                this.notificationBellMobile.style.display = 'none';
            }
        }

        // Обновляем десктопный колокольчик
        if (this.notificationBellDesktop) {
            if (hasNotification) {
                this.notificationBellDesktop.style.display = 'inline-block';
                this.pulseAnimation(this.notificationBellDesktop);
                this.updateNotificationTooltip(data, this.notificationBellDesktop);
            } else {
                this.notificationBellDesktop.style.display = 'none';
            }
        }

        // Обновляем сайдбар, если он существует
        if (window.mainSidebar) {
            window.mainSidebar.updateSupportNotification(hasNotification);
        }
    }

    updateNotificationTooltip(data, bellElement) {
        if (!bellElement) return;

        let tooltipText = '';

        if (this.userType === 'SUPER') {
            const reasons = [];
            if (data.hasUnreadMessages) {
                reasons.push('Непрочитанные сообщения');
            }
            if (data.hasUnassignedTickets) {
                reasons.push('Невзятые тикеты');
            }
            tooltipText = 'SUPER: ' + reasons.join(', ');
        } else {
            if (data.hasUnreadSuperReplies) {
                tooltipText = 'Есть новые ответы от поддержки';
            }
        }

        if (tooltipText) {
            bellElement.setAttribute('title', tooltipText);
            bellElement.setAttribute('data-bs-toggle', 'tooltip');
            bellElement.setAttribute('data-bs-placement', 'bottom');

            // Инициализируем подсказку Bootstrap
            if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
                const tooltip = bootstrap.Tooltip.getInstance(bellElement);
                if (tooltip) {
                    tooltip.dispose();
                }
                new bootstrap.Tooltip(bellElement);
            }
        }
    }

    pulseAnimation(bellElement) {
        if (bellElement) {
            bellElement.classList.add('notification-pulse-active');
            setTimeout(() => {
                bellElement.classList.remove('notification-pulse-active');
            }, 1000);
        }
    }

    stopPolling() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
    }

    destroy() {
        this.stopPolling();
        this.notificationBellMobile = null;
        this.notificationBellDesktop = null;
        this.isInitialized = false;
        this.userType = null;
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    // Проверяем авторизацию по наличию window.currentUserRole
    const isAuthenticated = window.currentUserRole !== undefined;

    if (isAuthenticated) {
        window.globalSupportNotifications = new GlobalSupportNotifications();

        // Инициализируем подсказки Bootstrap для всех элементов
        if (typeof bootstrap !== 'undefined') {
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        }
    }
});

// Также проверяем при изменении DOM
if (typeof MutationObserver !== 'undefined') {
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
            if (mutation.addedNodes.length > 0) {
                const mobileBell = document.getElementById('supportNotificationBell');
                const desktopBell = document.getElementById('supportNotificationBellDesktop');
                if ((mobileBell || desktopBell) && !window.globalSupportNotifications) {
                    if (window.currentUserRole !== undefined) {
                        window.globalSupportNotifications = new GlobalSupportNotifications();
                    }
                }
            }
        });
    });

    observer.observe(document.body, { childList: true, subtree: true });
}