class SSESupportNotifications {
    constructor() {
        this.eventSource = null;
        this.notificationBell = document.getElementById('supportNotificationBell');
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.reconnectTimeout = 3000;
        this.userType = null;
        this.unreadCategories = new Set();
        this.unassignedCategories = new Set();

        this.init();
    }

    init() {
        this.determineUserType();
        this.connect();

        window.addEventListener('beforeunload', () => this.disconnect());
    }

    determineUserType() {
        const header = document.querySelector('header');
        const isSuper = header?.getAttribute('data-is-super') === 'true';
        const isAdmin = header?.getAttribute('data-is-admin') === 'true';

        this.userType = (isSuper || isAdmin) ? 'SUPER' : 'USER';
    }

    connect() {
        if (this.eventSource) {
            this.eventSource.close();
        }

        this.eventSource = new EventSource('/api/support/sse');

        this.eventSource.onopen = () => {
            this.reconnectAttempts = 0;
        };

        this.eventSource.onerror = () => {
            this.eventSource.close();

            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                setTimeout(() => this.connect(), this.reconnectTimeout);
            }
        };

        this.eventSource.addEventListener('new_ticket', (event) => {
            const data = JSON.parse(event.data);
            this.handleNewTicket(data);
        });

        this.eventSource.addEventListener('new_reply', (event) => {
            const data = JSON.parse(event.data);
            this.handleNewReply(data);
        });

        this.eventSource.addEventListener('unassigned_ticket', (event) => {
            const data = JSON.parse(event.data);
            this.handleUnassignedTicket(data);
        });

        this.eventSource.addEventListener('ticket_assigned', (event) => {
            const data = JSON.parse(event.data);
            this.handleTicketAssigned(data);
        });
    }

    handleNewTicket(data) {
        if (this.userType === 'SUPER') {
            this.showNotification('Новый тикет в поддержку');
            this.unassignedCategories.add(data.category);
            this.updateCategoryHighlights();
        }
        this.updateBell();
    }

    handleNewReply(data) {
        if (this.userType === 'USER') {
            this.showNotification('Новый ответ от поддержки');
            this.unreadCategories.add(data.category);
            this.updateCategoryHighlights();
        } else {
            this.unreadCategories.add(data.category);
            this.updateCategoryHighlights();
        }
        this.updateBell();
    }

    handleUnassignedTicket(data) {
        if (this.userType === 'SUPER') {
            this.unassignedCategories.add(data.category);
            this.updateCategoryHighlights();
            this.updateBell();
        }
    }

    handleTicketAssigned(data) {
        if (this.userType === 'SUPER') {
            this.unassignedCategories.delete(data.category);
            this.updateCategoryHighlights();
        } else if (this.userType === 'USER') {
            this.showNotification('Ваш вопрос взят в работу');
        }
        this.updateBell();
    }

    updateCategoryHighlights() {
        const categoryCards = document.querySelectorAll('.category-card');

        categoryCards.forEach(card => {
            const category = card.getAttribute('data-category');
            let shouldHighlight = false;

            if (this.userType === 'SUPER') {
                if (this.unassignedCategories.has(category)) {
                    shouldHighlight = true;
                }
            } else {
                if (this.unreadCategories.has(category)) {
                    shouldHighlight = true;
                }
            }

            if (shouldHighlight) {
                card.classList.add('border-warning', 'category-highlight');
                if (!card.querySelector('.fa-bell')) {
                    const titleElement = card.querySelector('.card-title');
                    if (titleElement) {
                        const bellIcon = document.createElement('i');
                        bellIcon.className = 'fas fa-bell text-warning ms-2';
                        titleElement.appendChild(bellIcon);
                    }
                }
            } else {
                card.classList.remove('border-warning', 'category-highlight');
                const bellIcon = card.querySelector('.fa-bell');
                if (bellIcon) bellIcon.remove();
            }
        });
    }

    updateBell() {
        if (!this.notificationBell) return;

        const hasNotifications = this.userType === 'SUPER'
            ? this.unassignedCategories.size > 0 || this.unreadCategories.size > 0
            : this.unreadCategories.size > 0;

        this.notificationBell.style.display = hasNotifications ? 'inline-block' : 'none';

        if (hasNotifications) {
            let tooltipText = '';
            if (this.userType === 'SUPER') {
                const reasons = [];
                if (this.unreadCategories.size > 0) reasons.push('Новые сообщения');
                if (this.unassignedCategories.size > 0) reasons.push('Невзятые тикеты');
                tooltipText = 'SUPER: ' + reasons.join(', ');
            } else {
                tooltipText = 'Есть новые ответы от поддержки';
            }

            this.notificationBell.setAttribute('title', tooltipText);
            this.notificationBell.setAttribute('data-bs-toggle', 'tooltip');
            this.notificationBell.setAttribute('data-bs-placement', 'bottom');
        }
    }

    showNotification(message) {
        const toast = document.createElement('div');
        toast.className = 'position-fixed top-0 end-0 p-3';
        toast.style.zIndex = '9999';
        toast.innerHTML = `
            <div class="toast show" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="toast-header bg-primary text-white">
                    <i class="fas fa-headset me-2"></i>
                    <strong class="me-auto">Служба поддержки</strong>
                    <small>только что</small>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.remove();
        }, 5000);

        toast.querySelector('.btn-close').addEventListener('click', () => {
            toast.remove();
        });
    }

    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const isAuthenticated = document.querySelector('[sec\\:authorize="isAuthenticated()"]') !== null ||
        document.querySelector('body').hasAttribute('data-is-authenticated');

    if (isAuthenticated) {
        window.sseSupport = new SSESupportNotifications();

        if (typeof bootstrap !== 'undefined') {
            const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.map(function (tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        }
    }
});