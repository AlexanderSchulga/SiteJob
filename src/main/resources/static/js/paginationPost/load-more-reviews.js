class LoadMoreReviewsPagination {
    constructor() {
        this.containerId = 'reviewsContainer';
        this.buttonId = 'loadMoreReviewsBtn';
        this.buttonContainerId = 'loadMoreReviewsContainer';
        this.loadedMessageId = 'allLoadedReviewsMessage';
        this.noReviewsId = 'noReviewsMessage';

        this.currentPage = 0;
        this.size = 2;
        this.isLoading = false;
        this.hasNext = true;
        this.loadedIds = new Set();

        this.reviewedUserId = document.body.getAttribute('data-reviewed-user-id');
        this.reviewedUserHashId = document.body.getAttribute('data-reviewed-user-hash-id');
        this.currentUserId = document.body.getAttribute('data-current-user-id');
        this.csrfToken = this.getCsrfToken();

        this.hasSuperAccess = document.body.hasAttribute('data-is-super') ||
            document.body.getAttribute('data-user-role') === 'ROLE_SUPER';
        this.isAdminUser = document.body.hasAttribute('data-is-admin') ||
            document.body.getAttribute('data-user-role') === 'ROLE_ADMIN';

        this.pendingDeleteForm = null;
        this.modal = null; // Храним один экземпляр модалки
        this.modalElement = null;

        this.init();
    }

    init() {
        console.log('Initializing LoadMoreReviewsPagination...');
        this.initLoadButton();
        this.initDeleteModal();

        setTimeout(() => {
            this.loadInitialData();
        }, 100);

        this.hideStaticReviews();
    }

    initLoadButton() {
        const button = document.getElementById(this.buttonId);
        if (button) {
            button.addEventListener('click', () => this.loadMore());
        }
    }

    initDeleteModal() {
        this.modalElement = document.getElementById('deleteConfirmModal');
        if (!this.modalElement) {
            console.warn('Modal element not found');
            return;
        }

        // Создаем ОДИН экземпляр модалки
        this.modal = new bootstrap.Modal(this.modalElement, {
            backdrop: 'static',
            keyboard: true
        });

        const confirmBtn = document.getElementById('confirmDeleteBtn');
        if (confirmBtn) {
            // Удаляем старые обработчики
            const newConfirmBtn = confirmBtn.cloneNode(true);
            confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

            newConfirmBtn.addEventListener('click', () => {
                if (this.pendingDeleteForm) {
                    // Убираем aria-hidden перед отправкой
                    if (this.modalElement) {
                        this.modalElement.removeAttribute('aria-hidden');
                    }
                    this.pendingDeleteForm.submit();
                    this.pendingDeleteForm = null;
                }
                // Закрываем модалку
                if (this.modal) {
                    this.modal.hide();
                }
            });
        }

        // Обработчик закрытия модалки - очищаем данные
        this.modalElement.addEventListener('hidden.bs.modal', () => {
            this.pendingDeleteForm = null;
            // Убираем aria-hidden
            if (this.modalElement) {
                this.modalElement.removeAttribute('aria-hidden');
            }
            // Убираем backdrop если остался
            const backdrops = document.querySelectorAll('.modal-backdrop');
            backdrops.forEach(backdrop => backdrop.remove());
            // Убираем class modal-open с body
            document.body.classList.remove('modal-open');
            document.body.style.overflow = '';
            document.body.style.paddingRight = '';
        });

        // Обработчик открытия модалки
        this.modalElement.addEventListener('show.bs.modal', () => {
            // Убираем aria-hidden так как блокирует страницу при отмене несколько раз модалки
            if (this.modalElement) {
                this.modalElement.removeAttribute('aria-hidden');
            }
        });
    }

    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');

        const input = document.querySelector('input[name="_csrf"]');
        if (input) return input.value;

        const csrfInput = document.querySelector('[name^="_csrf"]');
        if (csrfInput) return csrfInput.value;

        return '';
    }

    async loadInitialData() {
        this.currentPage = 0;
        this.loadedIds.clear();
        this.hasNext = true;

        const container = document.getElementById(this.containerId);
        if (container) {
            container.innerHTML = '';
        }

        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) {
            buttonContainer.style.display = 'block';
        }

        this.hideAllLoadedMessage();
        this.hideNoReviewsMessage();

        await this.loadMore(true);
    }

    async loadMore(isInitial = false) {
        if (this.isLoading) return;
        if (!this.hasNext && !isInitial) return;

        this.setLoadingState(true);

        try {
            const url = this.buildUrl(this.currentPage);
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
                const reviews = data.reviews || [];

                if (reviews.length > 0) {
                    this.appendReviews(reviews);
                    this.hasNext = data.hasNext;
                    this.currentPage = data.currentPage + 1;

                    const button = document.getElementById(this.buttonId);
                    if (button) {
                        button.dataset.currentPage = this.currentPage;
                    }

                    if (!data.hasNext) {
                        this.hideButton();
                        this.showAllLoadedMessage();
                    }
                } else {
                    if (isInitial) {
                        this.showNoReviewsMessage();
                    }
                    this.hasNext = false;
                    this.hideButton();
                    this.showAllLoadedMessage();
                }
            } else {
                if (isInitial) {
                    this.showErrorMessage('Ошибка загрузки отзывов');
                }
            }

        } catch {

            if (isInitial) {
                this.showErrorMessage('Не удалось загрузить отзывы');
            }
        } finally {
            this.setLoadingState(false);
        }
    }

    buildUrl(page) {
        return `/page_one/reviews/${this.reviewedUserHashId}/load-more-review?page=${page}&size=${this.size}`;
    }

    appendReviews(reviews) {
        const container = document.getElementById(this.containerId);
        if (!container) return;

        reviews.forEach(review => {
            const element = this.createReviewElement(review);
            container.appendChild(element);
            this.loadedIds.add(review.id);
        });

        this.initReplyHandlers();
        this.initDeleteHandlers();
    }

    createReviewElement(review) {
        const reviewDiv = document.createElement('div');
        reviewDiv.className = 'review-item mb-4';
        reviewDiv.id = `review-${review.id}`;
        reviewDiv.dataset.reviewId = review.id;

        let reviewHtml = `
            <div class="review-card card shadow-sm">
                <div class="card-body">
                    <div class="review-header d-flex justify-content-between align-items-start mb-3">
                        <div class="d-flex align-items-center">
                            <div class="user-avatar-sm me-3">
                                <i class="fas fa-user-circle fa-2x ${review.author.isDeleted ? 'text-muted' : 'text-primary'}"></i>
                            </div>
                            <div>
                                <h6 class="card-subtitle mb-1 fw-bold ${review.author.isDeleted ? 'text-muted' : 'text-primary'}">
                                    ${this.escapeHtml(review.author.displayName)}
                                </h6>
                                <small class="text-muted">
                                    <i class="fas fa-clock me-1"></i>
                                    ${review.formattedDate}
                                </small>
                            </div>
                        </div>
                    </div>
                    
                    <p class="card-text mb-2">${this.escapeHtml(review.text)}</p>
                    
                    <div class="review-actions mt-3 pt-3 border-top">
        `;

        if (review.canReply && this.currentUserId) {
            reviewHtml += `
                <button class="btn btn-sm btn-outline-primary reply-btn"
                        data-review-id="${review.id}"
                        data-nameuse="${this.escapeHtml(review.author.displayName)}"
                        data-review-text="${this.escapeHtml(review.text)}">
                    <i class="fas fa-reply me-1"></i> Ответить
                </button>
            `;
        }

        if (this.hasSuperAccess) {
            reviewHtml += `
                <form action="/page_one/reviews/${this.reviewedUserHashId}/reviecom/${review.id}/delete"
                      method="post" class="d-inline-block ms-2 delete-form"
                      data-delete-message="Вы уверены, что хотите удалить этот отзыв?">
                    <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                    <button type="button" class="btn btn-sm btn-danger delete-btn">
                        <i class="fas fa-trash me-1"></i> Удалить
                    </button>
                </form>
            `;
        }

        reviewHtml += `</div></div></div>`;

        if (review.replies && review.replies.length > 0) {
            reviewHtml += `<div class="review-replies ms-4 mt-3">`;

            review.replies.forEach(reply => {
                reviewHtml += `
                    <div class="review-reply-card card mb-2" id="reply-${reply.id}">
                        <div class="card-body py-3">
                            <div class="review-header d-flex justify-content-between align-items-start mb-2">
                                <div class="d-flex align-items-center">
                                    <div class="user-avatar-sm me-3">
                                        <i class="fas fa-user-circle fa-lg ${reply.author.isDeleted ? 'text-muted' : 'text-success'}"></i>
                                    </div>
                                    <div>
                                        <h6 class="card-subtitle mb-1 fw-bold ${reply.author.isDeleted ? 'text-muted' : 'text-success'}">
                                            ${this.escapeHtml(reply.author.displayName)}
                                        </h6>
                                        <small class="text-muted">
                                            <i class="fas fa-clock me-1"></i>
                                            ${reply.formattedDate}
                                        </small>
                                    </div>
                                </div>
                            </div>
                            
                            <p class="card-text mb-2">${this.escapeHtml(reply.text)}</p>
                            
                            <div class="review-actions">
                `;

                if (reply.canReply && this.currentUserId) {
                    reviewHtml += `
                        <button class="btn btn-sm btn-outline-primary reply-btn"
                                data-review-id="${review.id}"
                                data-nameuse="${this.escapeHtml(reply.author.displayName)}"
                                data-review-text="${this.escapeHtml(reply.text)}">
                            <i class="fas fa-reply me-1"></i> Ответить
                        </button>
                    `;
                }

                if (this.hasSuperAccess) {
                    reviewHtml += `
                        <form action="/page_one/reviews/${this.reviewedUserHashId}/reviecom/${reply.id}/delete"
                              method="post" class="d-inline-block ms-2 delete-form"
                              data-delete-message="Вы уверены, что хотите удалить этот ответ?">
                            <input type="hidden" name="_csrf" value="${this.csrfToken}"/>
                            <button type="button" class="btn btn-sm btn-danger delete-btn">
                                <i class="fas fa-trash me-1"></i> Удалить
                            </button>
                        </form>
                    `;
                }

                reviewHtml += `</div></div></div>`;
            });

            reviewHtml += `</div>`;
        }

        reviewDiv.innerHTML = reviewHtml;
        return reviewDiv;
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    setLoadingState(loading) {
        this.isLoading = loading;
        const button = document.getElementById(this.buttonId);
        if (button) {
            button.disabled = loading;
            button.innerHTML = loading ?
                '<i class="fas fa-spinner fa-spin me-2"></i> Загрузка...' :
                '<i class="fas fa-plus-circle me-2"></i> Показать еще отзывы';
        }
    }

    hideButton() {
        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) {
            buttonContainer.style.display = 'none';
        }
    }

    showAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) {
            messageDiv.style.display = 'block';
        }
    }

    hideAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) {
            messageDiv.style.display = 'none';
        }
    }

    showNoReviewsMessage() {
        const messageDiv = document.getElementById(this.noReviewsId);
        if (messageDiv) {
            messageDiv.style.display = 'block';
        }
    }

    hideNoReviewsMessage() {
        const messageDiv = document.getElementById(this.noReviewsId);
        if (messageDiv) {
            messageDiv.style.display = 'none';
        }
    }

    showErrorMessage(message) {
        const container = document.getElementById(this.containerId);
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

    initReplyHandlers() {
        const replyButtons = document.querySelectorAll('.reply-btn');
        replyButtons.forEach(button => {
            button.removeEventListener('click', this.handleReplyClick);
            button.addEventListener('click', this.handleReplyClick.bind(this));
        });
    }

    handleReplyClick(event) {
        const button = event.currentTarget;
        const reviewId = button.getAttribute('data-review-id');
        const nameuse = button.getAttribute('data-nameuse');
        const reviewText = button.getAttribute('data-review-text');

        startReply(reviewId, nameuse, reviewText);
    }

    initDeleteHandlers() {
        const deleteBtns = document.querySelectorAll('.delete-btn');
        deleteBtns.forEach(button => {
            button.removeEventListener('click', this.handleDeleteClick);
            button.addEventListener('click', this.handleDeleteClick.bind(this));
        });
    }

    handleDeleteClick(event) {
        event.preventDefault();
        event.stopPropagation();

        const button = event.currentTarget;
        const form = button.closest('.delete-form');

        if (form && this.modal) {
            const message = form.getAttribute('data-delete-message') || 'Вы уверены, что хотите удалить этот отзыв?';
            this.pendingDeleteForm = form;

            const messageSpan = document.getElementById('deleteConfirmMessage');
            if (messageSpan) {
                messageSpan.textContent = message;
            }

            // Показываем существующую модалку (не создаем новую)
            this.modal.show();
        }
    }

    hideStaticReviews() {
        const staticReviews = document.querySelector('.reviews-tree');
        if (staticReviews) {
            staticReviews.style.display = 'none';
        }
    }
}

// Глобальные функции для работы с ответами
function startReply(reviewId, nameuse, reviewText) {
    const parentCommentId = document.getElementById('parentCommentId');
    const replyToText = document.getElementById('replyToText');
    const replyTo = document.getElementById('replyTo');
    const cancelReplyBtn = document.getElementById('cancelReplyBtn');
    const reviewForm = document.getElementById('review-form');
    const reviewTextarea = document.getElementById('reviewText');

    if (parentCommentId) parentCommentId.value = reviewId;

    if (replyToText) {
        const truncatedText = reviewText.length > 100 ? reviewText.substring(0, 100) + '...' : reviewText;
        replyToText.textContent = nameuse + ': ' + truncatedText;
    }

    if (replyTo) replyTo.style.display = 'block';
    if (cancelReplyBtn) cancelReplyBtn.style.display = 'inline-block';

    if (reviewForm) reviewForm.scrollIntoView({ behavior: 'smooth' });
    if (reviewTextarea) reviewTextarea.focus();
}

function cancelReply() {
    const parentCommentId = document.getElementById('parentCommentId');
    const replyTo = document.getElementById('replyTo');
    const cancelReplyBtn = document.getElementById('cancelReplyBtn');
    const reviewText = document.getElementById('reviewText');

    if (parentCommentId) parentCommentId.value = '';
    if (replyTo) replyTo.style.display = 'none';
    if (cancelReplyBtn) cancelReplyBtn.style.display = 'none';
    if (reviewText) reviewText.value = '';
}

// Инициализация
document.addEventListener('DOMContentLoaded', function() {
    try {
        window.reviewsPagination = new LoadMoreReviewsPagination();
    } catch {

        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger m-3';
        errorDiv.innerHTML = `
            <strong>Ошибка загрузки!</strong> Не удалось инициализировать загрузку отзывов. 
            <button class="btn btn-sm btn-outline-secondary ms-2" onclick="location.reload()">
                Обновить страницу
            </button>
        `;
        const container = document.querySelector('.reviews-section');
        if (container) {
            container.insertBefore(errorDiv, container.firstChild);
        }
    }
});