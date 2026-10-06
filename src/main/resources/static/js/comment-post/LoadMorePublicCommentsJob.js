class LoadMorePublicCommentsJob {
    constructor() {
        this.containerId = 'publicCommentsContainer';
        this.buttonId = 'loadMorePublicCommentsBtn';
        this.buttonContainerId = 'loadMorePublicCommentsContainer';
        this.loadedMessageId = 'allLoadedPublicCommentsMessage';
        this.noCommentsId = 'noPublicCommentsMessage';

        this.currentPage = 0;
        this.size = 3;
        this.isLoading = false;
        this.hasMore = true;
        this.loadedIds = new Set();
        this.expandedBranches = new Set();

        this.hashId = window.location.pathname.split('/')[2];
        this.currentUserId = document.body.getAttribute('data-current-user-id');
        this.csrfToken = this.getCsrfToken();

        this.hasSuperAccess = document.body.hasAttribute('data-is-super') ||
            document.body.getAttribute('data-user-role') === 'ROLE_SUPER';
        this.isAdminUser = document.body.hasAttribute('data-is-admin') ||
            document.body.getAttribute('data-user-role') === 'ROLE_ADMIN';
        this.hasOnlySuperAccess = document.body.hasAttribute('data-is-super') ||
            document.body.getAttribute('data-user-role') === 'ROLE_SUPER';
        this.hasAdminOrSuperAccess = this.hasSuperAccess || this.isAdminUser;

        this.init();
        this.createModalElements();
    }

    init() {
        console.log('Initializing LoadMorePublicCommentsJob with SLICE...');
        this.initLoadButton();
        this.loadInitialData();
        this.setupCommentForm();
        this.setupGlobalHandlers();
    }

    setupGlobalHandlers() {
        window.handlePublicReplyClick = (event) => this.handleReplyClick(event);
        window.handlePublicToggleClick = (event) => this.handleToggleClick(event);
    }

    initLoadButton() {
        const button = document.getElementById(this.buttonId);
        if (button) {
            button.addEventListener('click', () => this.loadMore());
            console.log('Initialized load more button for public comments');
        }
    }

    async loadInitialData() {
        console.log('Loading initial public comments data with SLICE');
        this.currentPage = 0;
        this.loadedIds.clear();
        this.hasMore = true;

        const container = document.getElementById(this.containerId);
        if (container) container.innerHTML = '';

        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'block';

        this.hideAllLoadedMessage();
        this.hideNoCommentsMessage();

        await this.loadMore(true);
    }

    async loadMore(isInitial = false) {
        if (this.isLoading) return;
        if (!this.hasMore && !isInitial) return;

        this.setLoadingState(true);

        try {
            const url = `/postidviewsjob/${this.hashId}/load-more-public-comments?page=${this.currentPage}&size=${this.size}`;
            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' }
            });

            if (!response.ok) {
                console.log(`Ошибка загрузки: ${response.status}`);
                this.setLoadingState(false);
                return;
            }

            const data = await response.json();

            if (data.status === 'success') {
                const comments = data.comments || [];

                if (comments.length > 0) {
                    this.appendComments(comments);
                    this.hasMore = data.hasMore;
                    this.currentPage++;

                    if (!data.hasMore) {
                        this.hideButton();
                        this.showAllLoadedMessage();
                    }
                } else if (isInitial) {
                    this.showNoCommentsMessage();
                    this.hasMore = false;
                    this.hideButton();
                }
            }
        } catch (error) {

        } finally {
            this.setLoadingState(false);
        }
    }

    appendComments(comments) {
        const container = document.getElementById(this.containerId);
        if (!comments?.length || !container) return;

        comments.forEach(comment => {
            if (this.loadedIds.has(comment.id)) return;

            const element = this.createCommentElement(comment);
            container.appendChild(element);
            this.loadedIds.add(comment.id);
        });

        this.initCommentHandlers();
    }

    createCommentElement(comment) {
        const commentDiv = document.createElement('div');
        commentDiv.className = 'comment-item mb-4';
        commentDiv.id = `public-comment-${comment.id}`;
        commentDiv.dataset.commentId = comment.id;

        const authorName = comment.author.nameuse || 'Пользователь';
        const authorId = comment.author.id || 0;
        const isSuperUser = comment.isSuperUser || this.hasOnlySuperAccess;
        const isOnline = comment.author.online || false;
        const hasAuthor = comment.author && comment.author.nameuse !== 'Удаленный пользователь';

        const onlineStatusHtml = hasAuthor
            ? (isOnline
                ? '<span class="status-online ms-2"><i class="fas fa-circle"></i><small>онлайн</small></span>'
                : '<span class="status-offline ms-2"><i class="fas fa-circle"></i><small>офлайн</small></span>')
            : '';

        let commentHtml = `
        <div class="comment-card public-comment mb-3">
            <div class="comment-header d-flex justify-content-between align-items-start">
                <div>
                    <span class="comment-user fw-bold">${this.escapeHtml(authorName)}</span>
                    ${onlineStatusHtml}
                    ${isSuperUser && authorId ? `<small class="text-muted ms-2">ID: ${authorId}</small>` : ''}
                    <small class="comment-date text-muted ms-2">
                        <i class="fas fa-clock me-1"></i> ${comment.formattedDate}
                    </small>
                </div>
                <button class="btn btn-sm btn-outline-secondary public-toggle-branch-btn"
                        id="public-toggle-btn-${comment.id}"
                        data-comment-id="${comment.id}"
                        aria-expanded="false">
                    <i class="fas fa-chevron-down" id="public-toggle-icon-${comment.id}"></i>
                </button>
            </div>
            <div class="comment-content mt-2">
                <p class="mb-0">${this.escapeHtml(comment.text)}</p>
            </div>
            <div class="comment-actions mt-2">
                <button class="btn btn-sm btn-outline-primary public-reply-btn"
                        data-comment-id="${comment.id}"
                        data-nameuse="${this.escapeHtml(authorName)}"
                        data-comment-text="${this.escapeHtml(comment.text)}">
                    <i class="fas fa-reply me-1"></i> Ответить
                </button>`;

        if (this.hasSuperAccess || this.isAdminUser) {
            const deleteUrl = `/postidviewsjob/${this.hashId}/comment/${comment.id}/delete`;
            commentHtml += `
                <button type="button" class="btn btn-sm btn-danger delete-public-comment-btn ms-2"
                        data-delete-url="${deleteUrl}"
                        data-csrf="${this.csrfToken}">
                    <i class="fas fa-trash me-1"></i> Удалить
                </button>
                <button type="button" class="btn btn-sm btn-warning delete-public-branch-btn ms-1"
                        data-delete-url="${deleteUrl.replace('/delete', '/deleteBranch')}"
                        data-csrf="${this.csrfToken}">
                    <i class="fas fa-trash-alt me-1"></i> Удалить ветку
                </button>`;
        }

        commentHtml += `</div></div>
        <div class="comment-replies ms-4 mt-2" id="public-replies-${comment.id}" style="display: none;">`;

        if (comment.replies?.length > 0) {
            commentHtml += this.renderReplies(comment.replies, 0);
        }

        commentHtml += `</div>`;
        commentDiv.innerHTML = commentHtml;
        return commentDiv;
    }

    renderReplies(nodes, level) {
        let html = '';
        const maxIndent = 4;
        const marginLeft = Math.min(level, maxIndent) * 24;

        nodes.forEach(node => {
            html += `<div class="reply-node" style="margin-left:${marginLeft}px;">`;
            html += this.createReplyElement(node);
            if (node.replies?.length) {
                html += this.renderReplies(node.replies, level + 1);
            }
            html += `</div>`;
        });

        return html;
    }

    createReplyElement(reply) {
        const authorName = reply.author.nameuse || 'Пользователь';
        const authorId = reply.author.id || 0;
        const isSuperUser = this.hasAdminOrSuperAccess;
        const isOnline = reply.author.online || false;
        const hasAuthor = reply.author && reply.author.nameuse !== 'Удаленный пользователь';

        const recipientName = reply.recipientName || null;
        const recipientHtml = recipientName
            ? `<span class="reply-recipient text-muted ms-1">
                   <i class="fas fa-arrow-right mx-1" style="font-size:0.75rem;"></i>
                   <span class="fw-semibold">${this.escapeHtml(recipientName)}</span>
               </span>`
            : '';

        const onlineStatusHtml = hasAuthor
            ? (isOnline
                ? '<span class="status-online ms-2"><i class="fas fa-circle"></i><small>онлайн</small></span>'
                : '<span class="status-offline ms-2"><i class="fas fa-circle"></i><small>офлайн</small></span>')
            : '';

        let replyHtml = `
        <div class="comment-card public-comment reply-comment mb-2" id="public-reply-${reply.id}">
            <div class="comment-header">
                <span class="comment-user fw-bold">${this.escapeHtml(authorName)}</span>
                ${onlineStatusHtml}
                ${recipientHtml}
                ${isSuperUser && authorId ? `<small class="text-muted ms-2">ID: ${authorId}</small>` : ''}
                <small class="comment-date text-muted ms-2">
                    <i class="fas fa-clock me-1"></i> ${reply.formattedDate}
                </small>
            </div>
            <div class="comment-content mt-2">
                <p class="mb-0">${this.escapeHtml(reply.text)}</p>
            </div>
            <div class="comment-actions mt-2">
                <button class="btn btn-sm btn-outline-primary public-reply-btn"
                        data-comment-id="${reply.id}"
                        data-nameuse="${this.escapeHtml(authorName)}"
                        data-comment-text="${this.escapeHtml(reply.text)}">
                    <i class="fas fa-reply me-1"></i> Ответить
                </button>`;

        if (this.hasSuperAccess || this.isAdminUser) {
            const deleteUrl = `/postidviewsjob/${this.hashId}/comment/${reply.id}/delete`;
            replyHtml += `
                <button type="button" class="btn btn-sm btn-danger delete-public-comment-btn ms-2"
                        data-delete-url="${deleteUrl}"
                        data-csrf="${this.csrfToken}">
                    <i class="fas fa-trash me-1"></i> Удалить
                </button>`;
        }

        replyHtml += `</div></div>`;
        return replyHtml;
    }

    initCommentHandlers() {
        const replyButtons = document.querySelectorAll('.public-reply-btn');
        replyButtons.forEach(button => {
            button.removeEventListener('click', window.handlePublicReplyClick);
            button.addEventListener('click', window.handlePublicReplyClick);
        });

        const toggleButtons = document.querySelectorAll('.public-toggle-branch-btn');
        toggleButtons.forEach(button => {
            button.removeEventListener('click', window.handlePublicToggleClick);
            button.addEventListener('click', window.handlePublicToggleClick);
        });

        const deleteCommentBtns = document.querySelectorAll('.delete-public-comment-btn');
        deleteCommentBtns.forEach(button => {
            button.removeEventListener('click', (e) => this.handleDeleteCommentClick(e));
            button.addEventListener('click', (e) => this.handleDeleteCommentClick(e));
        });

        const deleteBranchBtns = document.querySelectorAll('.delete-public-branch-btn');
        deleteBranchBtns.forEach(button => {
            button.removeEventListener('click', (e) => this.handleDeleteBranchClick(e));
            button.addEventListener('click', (e) => this.handleDeleteBranchClick(e));
        });
    }

    handleReplyClick(event) {
        const commentId = event.currentTarget.getAttribute('data-comment-id');
        const nameuse = event.currentTarget.getAttribute('data-nameuse');
        const commentText = event.currentTarget.getAttribute('data-comment-text');
        this.replyToComment(commentId, nameuse, commentText);
    }

    handleToggleClick(event) {
        event.stopPropagation();
        const commentId = event.currentTarget.getAttribute('data-comment-id');
        this.toggleCommentBranch(commentId, event.currentTarget);
    }

    handleDeleteCommentClick(event) {
        event.preventDefault();
        const button = event.currentTarget;
        const deleteUrl = button.getAttribute('data-delete-url');
        const csrfToken = button.getAttribute('data-csrf');

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = deleteUrl;
        form.style.display = 'none';

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);

        document.body.appendChild(form);
        this.showDeleteCommentModal(form);
    }

    handleDeleteBranchClick(event) {
        event.preventDefault();
        const button = event.currentTarget;
        const deleteUrl = button.getAttribute('data-delete-url');
        const csrfToken = button.getAttribute('data-csrf');

        const form = document.createElement('form');
        form.method = 'POST';
        form.action = deleteUrl;
        form.style.display = 'none';

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;
        form.appendChild(csrfInput);

        document.body.appendChild(form);
        this.showDeleteBranchModal(form);
    }

    replyToComment(commentId, nameuse, commentText) {
        const parentInput = document.getElementById('publicParentCommentId');
        if (!parentInput) {
            const form = document.getElementById('public-comment-form');
            const newInput = document.createElement('input');
            newInput.type = 'hidden';
            newInput.name = 'parentCommentId';
            newInput.id = 'publicParentCommentId';
            newInput.value = commentId;
            form.insertBefore(newInput, form.firstChild);
        } else {
            parentInput.value = commentId;
        }

        const decodedText = commentText
            .replace(/&#39;/g, "'")
            .replace(/&quot;/g, '"')
            .replace(/&lt;/g, '<')
            .replace(/&gt;/g, '>')
            .replace(/&amp;/g, '&');

        const displayText = decodedText.length > 50 ? decodedText.substring(0, 50) + '...' : decodedText;

        let replyToDiv = document.getElementById('publicReplyTo');
        if (!replyToDiv) {
            replyToDiv = document.createElement('div');
            replyToDiv.id = 'publicReplyTo';
            replyToDiv.className = 'alert alert-info mb-2';
            const form = document.getElementById('public-comment-form');
            const textarea = form.querySelector('textarea');
            form.insertBefore(replyToDiv, textarea);
        }

        replyToDiv.innerHTML = `
            <div class="d-flex justify-content-between align-items-center">
                <small><strong>Ответ на комментарий:</strong> ${this.escapeHtml(nameuse)}: ${this.escapeHtml(displayText)}</small>
                <button type="button" class="btn-close btn-sm" onclick="window.publicCommentsPaginationJob.cancelReply()" aria-label="Отменить ответ"></button>
            </div>`;
        replyToDiv.style.display = 'block';

        let cancelBtn = document.getElementById('publicCancelReplyBtn');
        if (!cancelBtn) {
            const submitBtn = document.querySelector('#public-comment-form button[type="submit"]');
            cancelBtn = document.createElement('button');
            cancelBtn.type = 'button';
            cancelBtn.id = 'publicCancelReplyBtn';
            cancelBtn.className = 'btn btn-secondary ms-2';
            cancelBtn.innerHTML = '<i class="fas fa-times me-1"></i> Отменить ответ';
            cancelBtn.onclick = () => this.cancelReply();
            submitBtn.parentNode.insertBefore(cancelBtn, submitBtn.nextSibling);
        }
        cancelBtn.style.display = 'inline-block';

        document.getElementById('publicCommentText').focus();
        document.getElementById('publicCommentText').placeholder = 'Ваш ответ...(не менее 3-х символов)';
        document.getElementById('public-comment-form').scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    cancelReply() {
        const parentInput = document.getElementById('publicParentCommentId');
        if (parentInput) parentInput.value = '';

        const replyToDiv = document.getElementById('publicReplyTo');
        if (replyToDiv) replyToDiv.style.display = 'none';

        const cancelBtn = document.getElementById('publicCancelReplyBtn');
        if (cancelBtn) cancelBtn.style.display = 'none';

        const textarea = document.getElementById('publicCommentText');
        if (textarea) {
            textarea.value = '';
            textarea.placeholder = 'Ваш комментарий...';
        }
    }

    toggleCommentBranch(commentId, button) {
        const repliesContainer = document.getElementById(`public-replies-${commentId}`);
        const icon = document.getElementById(`public-toggle-icon-${commentId}`);

        if (repliesContainer) {
            if (repliesContainer.style.display === 'none') {
                repliesContainer.style.display = 'block';
                button.classList.remove('collapsed');
                icon.classList.remove('fa-chevron-down');
                icon.classList.add('fa-chevron-up');
                button.setAttribute('aria-expanded', 'true');
                this.expandedBranches.add(commentId);
            } else {
                repliesContainer.style.display = 'none';
                button.classList.add('collapsed');
                icon.classList.remove('fa-chevron-up');
                icon.classList.add('fa-chevron-down');
                button.setAttribute('aria-expanded', 'false');
                this.expandedBranches.delete(commentId);
            }
        }
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
                '<i class="fas fa-plus-circle me-2"></i> Показать еще комментарии';
        }
    }

    hideButton() {
        const buttonContainer = document.getElementById(this.buttonContainerId);
        if (buttonContainer) buttonContainer.style.display = 'none';
    }

    showAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'block';
    }

    hideAllLoadedMessage() {
        const messageDiv = document.getElementById(this.loadedMessageId);
        if (messageDiv) messageDiv.style.display = 'none';
    }

    showNoCommentsMessage() {
        const messageDiv = document.getElementById(this.noCommentsId);
        if (messageDiv) messageDiv.style.display = 'block';
    }

    hideNoCommentsMessage() {
        const messageDiv = document.getElementById(this.noCommentsId);
        if (messageDiv) messageDiv.style.display = 'none';
    }

    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');
        const input = document.querySelector('input[name="_csrf"]');
        return input ? input.value : '';
    }

    async setupCommentForm() {
        const form = document.getElementById('public-comment-form');
        if (!form) return;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(form);
            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Отправка...';

            try {
                const response = await fetch(form.action, {
                    method: 'POST',
                    body: formData,
                    headers: { 'X-Requested-With': 'XMLHttpRequest' }
                });

                if (response.status === 401) {
                    console.log('Требуется авторизация для комментария');
                    form.reset();
                    this.cancelReply();
                    return;
                }

                const data = await response.json();

                if (data.status === 'success') {
                    form.reset();
                    this.cancelReply();
                    await this.smartRefreshPublicComments();
                    this.showSuccessMessage('Комментарий успешно добавлен!');
                } else if (data.status === 'ignored' || data.status === 'error') {
                    form.reset();
                    this.cancelReply();
                }
            } catch (error) {

                form.reset();
                this.cancelReply();
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            }
        });
    }

    async smartRefreshPublicComments() {
        try {
            const currentPage = this.currentPage;

            const container = document.getElementById(this.containerId);
            if (container) {
                container.innerHTML = '';
            }

            this.loadedIds.clear();
            this.currentPage = 0;
            this.hasMore = true;

            for (let page = 0; page <= currentPage; page++) {
                await this.loadMore(page === 0);
                if (!this.hasMore) {
                    break;
                }
            }
        } catch (error) {

        }
    }

    showSuccessMessage(message) {
        document.querySelectorAll('.alert.alert-success.position-fixed').forEach(el => el.remove());
        const successDiv = document.createElement('div');
        successDiv.className = 'alert alert-success alert-dismissible fade show position-fixed top-0 end-0 m-3';
        successDiv.style.zIndex = '9999';
        successDiv.innerHTML = `<strong>Успех!</strong> ${message}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        document.body.appendChild(successDiv);
        setTimeout(() => successDiv.remove(), 3000);
    }

    createModalElements() {
        if (!document.getElementById('deletePublicCommentModal')) {
            const deleteCommentModal = document.createElement('div');
            deleteCommentModal.innerHTML = `
                <div class="modal fade" id="deletePublicCommentModal" tabindex="-1" aria-labelledby="deletePublicCommentModalLabel" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="deletePublicCommentModalLabel">Удаление комментария</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Закрыть"></button>
                            </div>
                            <div class="modal-body">
                                <p>Вы уверены, что хотите удалить этот комментарий?</p>
                                <p class="text-muted small">Это действие нельзя отменить.</p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                                <button type="button" class="btn btn-danger" id="confirmDeletePublicCommentBtn">Удалить</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(deleteCommentModal.firstElementChild);

            const deleteBranchModal = document.createElement('div');
            deleteBranchModal.innerHTML = `
                <div class="modal fade" id="deletePublicBranchModal" tabindex="-1" aria-labelledby="deletePublicBranchModalLabel" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="deletePublicBranchModalLabel">Удаление ветки комментариев</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Закрыть"></button>
                            </div>
                            <div class="modal-body">
                                <p>Это действие <strong>нельзя отменить</strong>.</p>
                                <p class="text-muted small">Будут удалены все комментарии в этой ветке.</p>
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                                <button type="button" class="btn btn-danger" id="confirmDeletePublicBranchBtn">Удалить всю ветку</button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            document.body.appendChild(deleteBranchModal.firstElementChild);
        }

        this.initModalHandlers();
    }

    initModalHandlers() {
        const confirmDeleteBtn = document.getElementById('confirmDeletePublicCommentBtn');
        if (confirmDeleteBtn) {
            confirmDeleteBtn.addEventListener('click', () => {
                if (this.pendingDeleteForm) {
                    this.pendingDeleteForm.submit();
                    this.pendingDeleteForm = null;
                    const modal = bootstrap.Modal.getInstance(document.getElementById('deletePublicCommentModal'));
                    if (modal) modal.hide();
                }
            });
        }

        const confirmDeleteBranchBtn = document.getElementById('confirmDeletePublicBranchBtn');
        if (confirmDeleteBranchBtn) {
            confirmDeleteBranchBtn.addEventListener('click', () => {
                if (this.pendingDeleteBranchForm) {
                    this.pendingDeleteBranchForm.submit();
                    this.pendingDeleteBranchForm = null;
                    const modal = bootstrap.Modal.getInstance(document.getElementById('deletePublicBranchModal'));
                    if (modal) modal.hide();
                }
            });
        }
    }

    showDeleteCommentModal(form) {
        this.pendingDeleteForm = form;
        const modal = new bootstrap.Modal(document.getElementById('deletePublicCommentModal'));
        modal.show();
    }

    showDeleteBranchModal(form) {
        this.pendingDeleteBranchForm = form;
        const modal = new bootstrap.Modal(document.getElementById('deletePublicBranchModal'));
        modal.show();
    }
}

document.addEventListener('DOMContentLoaded', function() {
    if (window.location.pathname.includes('/postidviewsjob/')) {
        window.publicCommentsPaginationJob = new LoadMorePublicCommentsJob();
    }
});