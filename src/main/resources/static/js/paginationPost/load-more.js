class LoadMorePagination {
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
        this._ratingLoading = false;

        this.init();
        this.observeUrlChanges();
        this.loadedCount = 0;
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
            this.loadMoreContainer.style.display = 'flex';
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

            return;
        }

        this.loadMoreInProgress = true;
        this.setLoadingState(true);

        try {
            const nextPage = this.currentPage + 1;
            const url = this.buildUrl(nextPage);


            const controller = new AbortController();
            this.currentController = controller;

            const response = await fetch(url, {
                signal: controller.signal
            });

            const data = await response.json();

            if (data.status === 'success') {
                if (data.posts && data.posts.length > 0) {
                    const newPostsAdded = this.appendPosts(data.posts);

                    if (newPostsAdded > 0) {
                        this.currentPage = nextPage;
                        this.hasMore = data.hasMore;
                    } else {
                        if (data.hasMore) {
                            this.currentPage = nextPage;
                        } else {
                            this.hasMore = false;
                        }
                    }

                    if (!this.hasMore) {
                        this.hideLoadMoreButton();
                    }
                } else {
                    this.hasMore = false;
                    this.hideLoadMoreButton();
                }
            } else {
                this.showError();
            }

        } catch (error) {
            if (error.name === 'AbortError') {
                console.log('Запрос отменен');
            } else {
                this.showError();
            }
        } finally {
            this.setLoadingState(false);
            this.loadMoreInProgress = false;
            this.currentController = null;
        }
    }

    buildUrl(page) {
        const baseUrl = '/page_one/viewsone/load-more';
        const params = new URLSearchParams();
        const urlParams = new URLSearchParams(window.location.search);

        // Собираем ВСЕ параметры из URL в объект filter
        const filter = {};

        // Базовые параметры
        const category = urlParams.get('category');
        if (category && category !== 'all') filter.category = category;

        const search = urlParams.get('search');
        if (search) filter.search = search;

        const sort = urlParams.get('sort') || 'newest';
        filter.sort = sort;

        const priceSort = urlParams.get('priceSort') || 'none';
        filter.priceSort = priceSort;

        const region = urlParams.get('region');
        if (region && region !== 'all') filter.region = region;

        const city = urlParams.get('city');
        if (city && city !== 'all') filter.city = city;

        // ===== ФИЛЬТРЫ НЕДВИЖИМОСТИ =====
        const propertyType = urlParams.get('propertyType');
        if (propertyType) filter.propertyType = propertyType;

        const roomsCount = urlParams.get('roomsCount');
        if (roomsCount) filter.roomsCount = roomsCount;

        const floorsApartment = urlParams.get('floorsApartment');
        if (floorsApartment) filter.floorsApartment = floorsApartment;

        const floorsCount = urlParams.get('floorsCount');
        if (floorsCount) filter.floorsCount = floorsCount;

        const apartmentBuildYear = urlParams.get('apartmentBuildYear');
        if (apartmentBuildYear) filter.apartmentBuildYear = apartmentBuildYear;

        const floorsCountHouse = urlParams.get('floorsCountHouse');
        if (floorsCountHouse) filter.floorsCountHouse = floorsCountHouse;

        const houseBuildYear = urlParams.get('houseBuildYear');
        if (houseBuildYear) filter.houseBuildYear = houseBuildYear;

        const hasElevator = urlParams.get('hasElevator');
        if (hasElevator !== null && hasElevator !== undefined && hasElevator !== '') {
            filter.hasElevator = hasElevator;
        }

        const houseArea = urlParams.get('houseArea');
        if (houseArea) filter.houseArea = houseArea;

        const landArea = urlParams.get('landArea');
        if (landArea) filter.landArea = landArea;

        const distanceToCity = urlParams.get('distanceToCity');
        if (distanceToCity) filter.distanceToCity = distanceToCity;

        const materialUp = urlParams.get('materialUp');
        if (materialUp) filter.materialUp = materialUp;

        const repairUp = urlParams.get('repairUp');
        if (repairUp) filter.repairUp = repairUp;

        const materialHouse = urlParams.get('materialHouse');
        if (materialHouse) filter.materialHouse = materialHouse;

        const repairHouse = urlParams.get('repairHouse');
        if (repairHouse) filter.repairHouse = repairHouse;

        const distanceToCityHouse = urlParams.get('distanceToCityHouse');
        if (distanceToCityHouse) filter.distanceToCityHouse = distanceToCityHouse;

        const landAreaGarage = urlParams.get('landAreaGarage');
        if (landAreaGarage) filter.landAreaGarage = landAreaGarage;

        const landAreaCommercial = urlParams.get('landAreaCommercial');
        if (landAreaCommercial) filter.landAreaCommercial = landAreaCommercial;

        // ===== ФИЛЬТРЫ АВТОТРАНСПОРТА =====
        const vehicleType = urlParams.get('vehicleType');
        if (vehicleType) filter.vehicleType = vehicleType;

        const vehicleOrigin = urlParams.get('vehicleOrigin');
        if (vehicleOrigin) filter.vehicleOrigin = vehicleOrigin;

        const vehicleYear = urlParams.get('vehicleYear');
        if (vehicleYear) filter.vehicleYear = vehicleYear;

        const mileage = urlParams.get('mileage');
        if (mileage) filter.mileage = mileage;

        const numberOfOwners = urlParams.get('numberOfOwners');
        if (numberOfOwners) filter.numberOfOwners = numberOfOwners;


        //--Автозапчасти и автотовары--//
        const autoType = urlParams.get('autoType');
        if (autoType) filter.autoType = autoType;

        const conditionAuto = urlParams.get('conditionAuto');
        if (conditionAuto) filter.conditionAuto = conditionAuto;

        //---Строительные материалы--------------/////
        const constructionsType = urlParams.get('constructionsType');
        if (constructionsType) filter.constructionsType = constructionsType;

        //----Дом и интерьер----////

        const interiorType = urlParams.get('interiorType');
        if (interiorType) filter.interiorType = interiorType;

        const conditionInterior = urlParams.get('conditionInterior');
        if (conditionInterior) filter.conditionInterior = conditionInterior;

        //----Инструменты----

        const toolsType = urlParams.get('toolsType');
        if (toolsType) filter.toolsType = toolsType;

        const conditionTools = urlParams.get('conditionTools');
        if (conditionTools) filter.conditionTools = conditionTools;

        //------Бытовая техника----///

        const appliancesType = urlParams.get('appliancesType');
        if (appliancesType) filter.appliancesType = appliancesType;

        const conditionAppliances = urlParams.get('conditionAppliances');
        if (conditionAppliances) filter.conditionAppliances = conditionAppliances;

        //---Электроника и гаджеты---///

        const electronicsType = urlParams.get('electronicsType');
        if (electronicsType) filter.electronicsType = electronicsType;

        const conditionElectronics = urlParams.get('conditionElectronics');
        if (conditionElectronics) filter.conditionElectronics = conditionElectronics;

        //-----Одежда и обувь----//

        const clothingType = urlParams.get('clothingType');
        if (clothingType) filter.clothingType = clothingType;

        const gender = urlParams.get('gender');
        if (gender) filter.gender = gender;

        const conditionClothing = urlParams.get('conditionClothing');
        if (conditionClothing) filter.conditionClothing = conditionClothing;

        //--Детские товары--//
        const genderChildrenPr = urlParams.get('genderChildrenPr');
        if (genderChildrenPr) filter.genderChildrenPr = genderChildrenPr;

        const conditionChildrenPr = urlParams.get('conditionChildrenPr');
        if (conditionChildrenPr) filter.conditionChildrenPr = conditionChildrenPr;

        //---Товары для спорта----//

        const sportsKind = urlParams.get('sportsKind');
        if (sportsKind) filter.sportsKind = sportsKind;

        const sportsType = urlParams.get('sportsType');
        if (sportsType) filter.sportsType = sportsType;

        //--Детские товары--//
        const genderSports = urlParams.get('genderSports');
        if (genderSports) filter.genderSports = genderSports;

        const conditionSports = urlParams.get('conditionSports');
        if (conditionSports) filter.conditionSports = conditionSports;

        //-----Продукты питания-----//
        const foodKind = urlParams.get('foodKind');
        if (foodKind) filter.foodKind = foodKind;

        //-----Здоровье и красота------//

        const healthKind = urlParams.get('healthKind');
        if (healthKind) filter.healthKind = healthKind;

        const healthType = urlParams.get('healthType');
        if (healthType) filter.healthType = healthType;

        const genderHealth = urlParams.get('genderHealth');
        if (genderHealth) filter.genderHealth = genderHealth;

        //---------Флора и фауна----------//

        const animalsKind = urlParams.get('animalsKind');
        if (animalsKind) filter.animalsKind = animalsKind;


        // Добавляем все параметры фильтра в URL
        Object.keys(filter).forEach(key => {
            if (filter[key] !== null && filter[key] !== undefined && filter[key] !== '') {
                params.append(key, filter[key]);
            }
        });

        // Пагинация
        params.append('page', page);
        params.append('size', 8);

        return baseUrl + (params.toString() ? '?' + params.toString() : '');
    }

    appendPosts(posts) {
        if (!posts || posts.length === 0) {
            return 0;
        }

        const existingIds = new Set();
        const existingLinks = document.querySelectorAll('#postsContainer a[href*="/postidviews"]');

        existingLinks.forEach(link => {
            const href = link.getAttribute('href');
            const hashId = href.split('/postidviews/')[1];
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
                console.log('Дубликат пропущен:');
            }
        });

        if (newPostsAdded > 0) {
            this.container.appendChild(fragment);
            this.initRatingsForNewPosts();
        }

        if (allDuplicates && posts.length > 0) {
            console.log('Все загруженные посты - дубликаты, останавливаем пагинацию');
            this.hasMore = false;
            this.hideLoadMoreButton();
        }

        this.loadedCount += newPostsAdded;

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
        const isMobile = window.innerWidth <= 576;
        col.className = 'col-md-6 col-lg-4 col-xl-3' + (isMobile ? ' mb-2' : ' mb-4');

        const isAdmin = window.currentUser &&
            (window.currentUser.role === 'ROLE_ADMIN' || window.currentUser.role === 'ROLE_SUPER');

        const imageHtml = post.firstImagePath && post.firstImagePath !== '/images/no-image.jpg'
            ? '<img src="' + post.firstImagePath + '" class="post-image img-fluid" loading="lazy" onerror="this.onerror=null; this.src=\'/images/no-image.jpg\'; this.classList.add(\'image-error\');" alt="' + this.escapeHtml(post.title) + '">'
            : '<div class="post-image-placeholder d-flex align-items-center justify-content-center bg-light"><i class="fas fa-image fa-3x text-muted"></i></div>';

        const adminButtons = isAdmin ? `
        <div class="post-buttons">
            <form action="/viewsone/post/${post.hashId}/toggle-block" method="post" class="d-inline w-50">
                <input type="hidden" name="${window.csrfTokenName || '_csrf'}" value="${window.csrfTokenValue || ''}"/>
                <button class="btn btn-mod w-100 toggle-block-btn ${post.blocked ? 'btn-success' : 'btn-secondary'}"
                        type="button"
                        data-post-type="товар"
                        title="${post.blocked ? 'Разблокировать' : 'Заблокировать'}">
                    <i class="${post.blocked ? 'fas fa-unlock' : 'fas fa-ban'}"></i>
                    <span class="d-none d-md-inline">${post.blocked ? ' Разблок.' : ' Блок.'}</span>
                </button>
            </form>
            <form action="/viewsone/post/${post.hashId}/remove" method="post" class="d-inline w-50">
                <input type="hidden" name="${window.csrfTokenName || '_csrf'}" value="${window.csrfTokenValue || ''}"/>
                <button class="btn btn-mod btn-danger w-100 delete-post-btn"
                        type="button"
                        data-post-type="товар"
                        title="Удалить">
                    <i class="fas fa-trash"></i>
                    <span class="d-none d-md-inline"> Удалить</span>
                </button>
            </form>
        </div>
    ` : '';

        col.innerHTML = `
        <div class="${post.premium ? 'post-card-premium' : (post.donate ? 'post-card-donate' : 'post-card')}">
            <div>${imageHtml}</div>
            <div class="post-content">
                <h5 class="post-title">${this.escapeHtml(post.title)}</h5>
                <strong>Цена:</strong>
                <p class="post-anons">${this.escapeHtml(post.price)}</p>
                <a href="/postidviews/${post.hashId}" class="btn btn-edit w-100 mb-2">
                    <i class="fas fa-eye me-1"></i> Детальнее
                </a>
                ${adminButtons}
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
            if (loading) {
                this.loadMoreBtn.style.display = 'inline-flex';
                this.loadMoreBtn.disabled = true;
                this.loadMoreBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Загрузка...';
            } else {
                this.loadMoreBtn.style.display = 'inline-flex';
                this.loadMoreBtn.disabled = false;
                this.loadMoreBtn.innerHTML = '<i class="fas fa-plus-circle"></i> Показать еще';
            }
        }

        if (this.loadingSpinner) {
            this.loadingSpinner.style.display = loading ? 'flex' : 'none';
        }
    }

    hideLoadMoreButton() {
        if (this.loadMoreContainer) {
            this.loadMoreContainer.style.display = 'none';
        }

        if (this.allLoadedMessage) {
            this.allLoadedMessage.style.display = 'flex';
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
    currentPagination = new LoadMorePagination();
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