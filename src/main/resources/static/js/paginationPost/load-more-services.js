class LoadMoreServicesPagination {
    constructor() {
        this.currentPage = 0;
        this.isLoading = false;
        this.hasMore = true;
        this.loadMoreInProgress = false;
        this.container = document.getElementById('postsContainer');
        this.loadMoreBtn = document.getElementById('loadMoreBtn');
        this.loadMoreContainer = document.getElementById('loadMoreContainer');
        this.loadingSpinner = document.getElementById('loadingSpinner');
        this.allLoadedMessage = document.getElementById('allLoadedMessage');

        this.timeouts = new Set();
        this.currentController = null;
        this.loadedCount = 0;

        this.init();
        this.observeUrlChanges();
    }

    setTimeout(callback, delay) {
        const id = setTimeout(() => {
            this.timeouts.delete(id);
            callback();
        }, delay);
        this.timeouts.add(id);
        return id;
    }

    observeUrlChanges() {
        this.lastUrl = window.location.href;

        this.clickHandler = () => {
            this.setTimeout(() => {
                if (window.location.href !== this.lastUrl) {
                    this.resetPagination();
                    this.lastUrl = window.location.href;
                }
            }, 100);
        };

        this.popStateHandler = () => {
            this.resetPagination();
            this.lastUrl = window.location.href;
        };

        document.addEventListener('click', this.clickHandler);
        window.addEventListener('popstate', this.popStateHandler);
    }

    resetPagination() {
        this.currentPage = 0;
        this.hasMore = true;
        this.loadMoreInProgress = false;

        this.timeouts.forEach(id => clearTimeout(id));
        this.timeouts.clear();

        if (this.currentController) {
            this.currentController.abort();
            this.currentController = null;
        }

        if (this.loadMoreContainer) {
            this.loadMoreContainer.style.display = 'block';
        }

        if (this.allLoadedMessage) {
            this.allLoadedMessage.style.display = 'none';
        }

        if (this.container) {
            this.container.innerHTML = '';
        }
    }

    init() {
        if (this.loadMoreBtn) {
            this.loadMoreBtn.removeEventListener('click', this.boundLoadMore);
            this.boundLoadMore = () => this.loadMore();
            this.loadMoreBtn.addEventListener('click', this.boundLoadMore);
        }
    }

    async loadMore() {
        if (this.isLoading || !this.hasMore || this.loadMoreInProgress) {
            console.log('Loading in progress or not available');
            return;
        }

        this.loadMoreInProgress = true;
        this.setLoadingState(true);

        try {
            const nextPage = this.currentPage + 1;
            const url = this.buildUrl(nextPage);

            console.log('Loading services page');

            const controller = new AbortController();
            this.currentController = controller;

            const response = await fetch(url, {
                signal: controller.signal
            });

            if (!response.ok) {
                console.log('Server error:', response.status);

                if (response.status === 403) {
                    alert('Доступ запрещен');
                } else {
                    this.showError();
                }
                return;
            }

            const data = await response.json();

            if (data.status === 'success') {
                if (data.posts && data.posts.length > 0) {
                    const newPostsAdded = this.appendPosts(data.posts);

                    if (newPostsAdded > 0) {
                        this.currentPage = nextPage;
                        this.hasMore = data.hasMore;
                    } else {
                        if (data.hasMore) {
                            console.log('Duplicates received, but more pages available');
                            this.currentPage = nextPage;
                        } else {
                            this.hasMore = false;
                        }
                    }

                    if (!this.hasMore) {
                        this.hideLoadMoreButton();
                    }
                } else {
                    console.log('No more services');
                    this.hasMore = false;
                    this.hideLoadMoreButton();
                }
            } else {
                console.log('Server error occurred');
                this.showError();
            }

        } catch (error) {
            if (error.name === 'AbortError') {
                console.log('Request cancelled');
            } else {
                console.log('Network error occurred');
                this.showError();
            }
        } finally {
            this.setLoadingState(false);
            this.loadMoreInProgress = false;
            this.currentController = null;
        }
    }

    buildUrl(page) {
        const baseUrl = '/page_one/viewsonejob/load-more-services';
        const params = new URLSearchParams();
        const urlParams = new URLSearchParams(window.location.search);

        const category = urlParams.get('category');
        const search = urlParams.get('search');
        const sort = urlParams.get('sort') || 'newest';
        const priceSort = urlParams.get('priceSort') || 'none';
        const region = urlParams.get('region');
        const city = urlParams.get('city');

        params.append('page', page);
        params.append('size', 8);

        if (category) params.append('category', category);
        if (search) params.append('search', search);
        if (region) params.append('region', region);
        if (city) params.append('city', city);

        params.append('sort', sort);
        if (priceSort) {
            params.append('priceSort', priceSort);
        }

        return baseUrl + (params.toString() ? '?' + params.toString() : '');
    }

    appendPosts(posts) {
        if (!posts || posts.length === 0) {
            return 0;
        }

        const existingIds = new Set();
        const existingLinks = document.querySelectorAll('#postsContainer a[href*="/postidviewsjob/"]');

        existingLinks.forEach(link => {
            const href = link.getAttribute('href');
            const hashId = href.split('/postidviewsjob/')[1];
            if (hashId) {
                existingIds.add(hashId);
            }
        });

        let allDuplicates = true;
        let newPostsAdded = 0;

        const fragment = document.createDocumentFragment();

        posts.forEach(post => {
            if (!existingIds.has(post.hashId)) {
                allDuplicates = false;
                const postElement = this.createPostElement(post);
                fragment.appendChild(postElement);
                existingIds.add(post.hashId);
                newPostsAdded++;
            } else {
                console.log('Duplicate service skipped');
            }
        });

        if (newPostsAdded > 0) {
            this.container.appendChild(fragment);
            this.initRatingsForNewPosts();
        }

        if (allDuplicates && posts.length > 0) {
            console.log('All loaded services are duplicates, stopping pagination');
            this.hasMore = false;
            this.hideLoadMoreButton();
        }

        this.loadedCount += newPostsAdded;
        console.log('New services added');

        return newPostsAdded;
    }

    initRatingsForNewPosts() {
        this.timeouts.forEach(id => clearTimeout(id));
        this.timeouts.clear();

        this.setTimeout(() => {
            const newRatingElements = this.container.querySelectorAll('.rating-stars[data-seller-id]');

            if (newRatingElements.length === 0) return;

            const sellerIds = new Set();
            newRatingElements.forEach(el => {
                const sellerId = el.getAttribute('data-seller-id');
                if (sellerId && sellerId !== 'null' && sellerId !== 'undefined' && sellerId !== '') {
                    sellerIds.add(sellerId);
                }
            });

            if (sellerIds.size === 0) return;

            if (typeof window.clearRatingCache === 'function') {
                window.clearRatingCache(true);
            }

            sellerIds.forEach(sellerId => {
                if (typeof window.loadSellerRating === 'function') {
                    window.loadSellerRating(sellerId);
                }
            });

            if (typeof window.loadAllSellerRatings === 'function') {
                window.loadAllSellerRatings();
            }
        }, 150);
    }

    createPostElement(post) {
        const col = document.createElement('div');
        col.className = 'col-md-6 col-lg-4 col-xl-3 mb-4';

        const isAdmin = window.currentUser &&
            (window.currentUser.role === 'ROLE_ADMIN' || window.currentUser.role === 'ROLE_SUPER');

        // Форматирование изображения
        let imageHtml = '';
        if (post.firstImagePath) {
            imageHtml = `<img src="${this.escapeHtml(post.firstImagePath)}" class="post-image img-fluid" loading="lazy" decoding="async" onerror="this.onerror=null; this.src='/images/no-image.jpg'; this.classList.add('image-error');" alt="${this.escapeHtml(post.title)}">`;
        } else {
            imageHtml = `<div class="post-image-placeholder d-flex align-items-center justify-content-center bg-light"><i class="fas fa-image fa-3x text-muted"></i></div>`;
        }

        // Формирование кнопок админа (полностью как в статике)
        let adminButtonsHtml = '';
        if (isAdmin) {
            const blockBtnClass = post.blocked ? 'btn-success' : 'btn-secondary';
            const blockIcon = post.blocked ? 'fas fa-unlock' : 'fas fa-ban';
            const blockText = post.blocked ? ' Разблок.' : ' Блок.';

            adminButtonsHtml = `
                <div class="post-buttons">
                    <form action="/viewsonejob/job/${this.escapeHtml(post.hashId)}/toggle-block" method="post" class="d-inline w-50">
                        <input type="hidden" name="${window.csrfTokenName || '_csrf'}" value="${window.csrfTokenValue || ''}"/>
                        <button class="btn btn-mod w-100 toggle-block-btn ${blockBtnClass}"
                                type="button"
                                data-post-type="услугу"
                                title="${post.blocked ? 'Разблокировать' : 'Заблокировать'}">
                            <i class="${blockIcon}"></i>
                            <span class="d-none d-md-inline">${blockText}</span>
                        </button>
                    </form>
                    <form action="/viewsonejob/job/${this.escapeHtml(post.hashId)}/remove" method="post" class="d-inline w-50">
                        <input type="hidden" name="${window.csrfTokenName || '_csrf'}" value="${window.csrfTokenValue || ''}"/>
                        <button class="btn btn-mod btn-danger w-100 delete-post-btn"
                                type="button"
                                data-post-type="услугу"
                                title="Удалить">
                            <i class="fas fa-trash"></i>
                            <span class="d-none d-md-inline"> Удалить</span>
                        </button>
                    </form>
                </div>
            `;
        }

        // Полностью соответствующая статике структура
        col.innerHTML = `
            <div class="${post.premium ? 'post-card-premium' : (post.donate ? 'post-card-donate' : 'post-card')}">
                ${imageHtml}
                <div class="post-content">
                    <h5 class="post-title">${this.escapeHtml(post.title)}</h5>
                    <strong>Цена:</strong>
                    <p class="post-anons">${this.escapeHtml(post.price)}</p>
                    <a href="/postidviewsjob/${this.escapeHtml(post.hashId)}" class="btn btn-edit w-100 mb-2">
                        <i class="fas fa-eye me-1"></i> Детальнее
                    </a>
                    ${adminButtonsHtml}
                </div>
            </div>
        `;

        return col;
    }

    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    setLoadingState(loading) {
        this.isLoading = loading;

        if (this.loadMoreBtn) {
            this.loadMoreBtn.style.display = loading ? 'none' : 'flex';
        }

        if (this.loadingSpinner) {
            this.loadingSpinner.style.display = loading ? 'block' : 'none';
        }
    }

    hideLoadMoreButton() {
        if (this.loadMoreContainer) {
            this.loadMoreContainer.style.display = 'none';
        }

        if (this.allLoadedMessage) {
            this.allLoadedMessage.style.display = 'block';
        }
    }

    showError() {
        alert('Произошла ошибка при загрузке. Попробуйте еще раз.');
    }

    destroy() {
        if (this.currentController) {
            this.currentController.abort();
            this.currentController = null;
        }

        this.timeouts.forEach(id => clearTimeout(id));
        this.timeouts.clear();

        if (this.clickHandler) {
            document.removeEventListener('click', this.clickHandler);
        }
        if (this.popStateHandler) {
            window.removeEventListener('popstate', this.popStateHandler);
        }
        if (this.loadMoreBtn && this.boundLoadMore) {
            this.loadMoreBtn.removeEventListener('click', this.boundLoadMore);
        }

        this.container = null;
        this.loadMoreBtn = null;
        this.loadMoreContainer = null;
        this.loadingSpinner = null;
        this.allLoadedMessage = null;
    }
}

let currentPagination = null;

function initPagination() {
    if (currentPagination) {
        currentPagination.destroy();
    }
    currentPagination = new LoadMoreServicesPagination();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initPagination);
} else {
    initPagination();
}

window.addEventListener('beforeunload', () => {
    if (currentPagination) {
        currentPagination.destroy();
        currentPagination = null;
    }
});