/**
 * Карусель для платных объявлений и рекламы
 * Автопрокрутка каждые 4 секунды, листание тачем/мышью, стрелки навигации
 */

class PaidCarousel {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        this.track = this.container.querySelector('.carousel-paid__track');
        this.dotsContainer = this.container.querySelector('.carousel-paid__dots');
        this.prevBtn = this.container.querySelector('.carousel-paid__nav-prev');
        this.nextBtn = this.container.querySelector('.carousel-paid__nav-next');

        this.items = options.items || [];
        this.currentIndex = 0;
        this.autoplayInterval = null;
        this.autoplayDelay = options.autoplayDelay || 4000;
        this.isDragging = false;
        this.startX = 0;
        this.startY = 0;
        this.startTranslate = 0;
        this.dragDirection = null;
        this.touchMoved = false;
        this.slideWidth = 0;
        this.scrollStep = 0;
        this.centerOffset = 0;
        this.totalSlides = 0;
        this.gap = 15;
        this.isInitialized = false;

        this.itemsWithClones = [];
        this.totalRealSlides = 0;

        this.init();
    }

    init() {
        if (!this.track) return;

        // Всегда получаем свежие данные из глобальной переменной
        if (window.paidCarouselItems && window.paidCarouselItems.length > 0) {
            this.items = window.paidCarouselItems;
        } else {
            // Проверяем sessionStorage только если нет глобальных данных
            const saved = sessionStorage.getItem('paidCarouselItems');
            if (saved) {
                try {
                    const parsed = JSON.parse(saved);
                    if (parsed && parsed.length > 0) {
                        this.items = parsed;
                    }
                } catch(e) {}
            }
        }

        // Нормализуем данные - гарантируем минимум 3 слайда (заглушки если нужно)
        this.normalizeItems();

        this.totalRealSlides = this.items.length;

        // Клоны для бесконечной прокрутки (только если есть реальные слайды)
        if (this.totalRealSlides > 0) {
            const lastThree = this.items.slice(-3);
            const firstThree = this.items.slice(0, 3);
            this.itemsWithClones = [...lastThree, ...this.items, ...firstThree];
        } else {
            this.itemsWithClones = [];
        }

        this.render();
        this.calculateDimensions();
        this.attachEvents();

        window.addEventListener('resize', () => {
            this.calculateDimensions();
            this.updatePosition();
        });

        // Если есть слайды - стартуем с индекса 3 (первый реальный)
        if (this.itemsWithClones.length > 0) {
            this.currentIndex = this.totalRealSlides > 0 ? 3 : 0;
            this.updatePosition();
            this.updateSlidesClasses();
            this.updateActiveDot();
            this.startAutoplay();
        }

        this.container.style.display = '';
        this.isInitialized = true;
    }

    /**
     * Нормализует элементы - гарантирует минимум 3 слайда
     * Если реальных объявлений < 3, добавляем заглушки до 3
     * Если объявлений нет - создаем 3 заглушки
     */
    normalizeItems() {
        // Фильтруем реальные объявления (не заглушки)
        const realItems = this.items.filter(item => !item.isPlaceholder);

        // Если есть реальные объявления
        if (realItems.length > 0) {
            // Если реальных меньше 3 - добавляем заглушки до 3
            if (realItems.length < 3) {
                const needed = 3 - realItems.length;
                const placeholders = this.createPlaceholders(needed);
                this.items = [...realItems, ...placeholders];
            } else {
                this.items = realItems;
            }
        } else {
            // Нет реальных объявлений - создаем 3 заглушки
            this.items = this.createPlaceholders(3);
        }
    }

    createPlaceholders(count) {
        const templates = [
            {
                type: 'placeholder',
                title: 'Premium размещение',
                city: 'Станьте партнёром',
                image: null,
                link: null,
                isPlaceholder: true,
                placeholderText: 'Premium размещение',
                placeholderSubtext: 'Станьте партнёром'
            },
            {
                type: 'placeholder',
                title: 'Реклама здесь',
                city: 'Разместите своё объявление',
                image: null,
                link: null,
                isPlaceholder: true,
                placeholderText: 'Реклама здесь',
                placeholderSubtext: 'Разместите своё объявление'
            },
            {
                type: 'placeholder',
                title: 'Ваше объявление',
                city: 'Станьте лучшим',
                image: null,
                link: null,
                isPlaceholder: true,
                placeholderText: 'Ваше объявление',
                placeholderSubtext: 'Станьте лучшим'
            }
        ];

        const result = [];
        for (let i = 0; i < count && i < templates.length; i++) {
            result.push({...templates[i]});
        }
        return result;
    }

    calculateDimensions() {
        const slides = this.track.querySelectorAll('.carousel-paid__slide');
        if (slides.length === 0) return;

        // Сначала устанавливаем видимость и размеры для всех слайдов
        slides.forEach(slide => {
            slide.style.display = 'flex';
        });

        const slide = slides[0];
        const slideWidth = slide.offsetWidth || 200;

        this.slideWidth = slideWidth + this.gap;
        const containerWidth = this.container.offsetWidth || window.innerWidth;
        this.centerOffset = Math.max(0, (containerWidth - slideWidth) / 2);
        this.scrollStep = this.slideWidth;
        this.totalSlides = slides.length;
    }

    createPremiumPlaceholder() {
        const placeholder = document.createElement('div');
        placeholder.className = 'carousel-paid__img-placeholder';
        placeholder.innerHTML = `
            <div class="premium-placeholder-content">
                <i class="fas fa-crown"></i>
                <span>Premium</span>
            </div>
        `;
        return placeholder;
    }

    render() {
        this.track.innerHTML = '';

        if (this.itemsWithClones.length === 0) {
            // Если нет слайдов, создаем заглушки
            this.items = this.createPlaceholders(3);
            this.totalRealSlides = this.items.length;
            const lastThree = this.items.slice(-3);
            const firstThree = this.items.slice(0, 3);
            this.itemsWithClones = [...lastThree, ...this.items, ...firstThree];
        }

        this.itemsWithClones.forEach((item, idx) => {
            const slide = document.createElement('div');
            slide.className = 'carousel-paid__slide';
            slide.setAttribute('data-index', idx);

            const card = document.createElement('div');

            if (item.isPlaceholder) {
                // Заглушка
                card.className = 'carousel-paid__card carousel-paid__card--placeholder';
                card.style.cursor = 'default';

                const imgWrapper = document.createElement('div');
                imgWrapper.className = 'carousel-paid__img-wrapper';
                imgWrapper.innerHTML = `
                    <div class="carousel-paid__img-placeholder carousel-paid__img-placeholder--advert">
                        <div class="premium-placeholder-content">
                            <i class="fas fa-chart-line"></i>
                            <span class="placeholder-title">${this.escapeHtml(item.placeholderText || 'Premium размещение')}</span>
                            <span class="placeholder-subtitle">${this.escapeHtml(item.placeholderSubtext || 'Станьте партнёром')}</span>
                        </div>
                    </div>
                `;

                const content = document.createElement('div');
                content.className = 'carousel-paid__content';
                content.innerHTML = `
                    <div class="carousel-paid__content-title">${this.escapeHtml(item.title)}</div>
                    <div class="carousel-paid__content-location">
                        <i class="fas fa-map-marker-alt"></i> ${this.escapeHtml(item.city)}
                    </div>
                `;

                card.appendChild(imgWrapper);
                card.appendChild(content);
            } else {
                // Обычная карточка
                card.className = 'carousel-paid__card';
                card.style.cursor = 'pointer';

                const imgWrapper = document.createElement('div');
                imgWrapper.className = 'carousel-paid__img-wrapper';

                if (item.image && item.image !== 'null' && item.image !== 'undefined' && item.image !== '') {
                    const img = document.createElement('img');
                    img.className = 'carousel-paid__img';
                    img.src = item.image;
                    img.alt = item.title || 'Объявление';
                    img.onerror = function() {
                        this.style.display = 'none';
                        this.parentElement.innerHTML = `
                            <div class="carousel-paid__img-placeholder">
                                <div class="premium-placeholder-content">
                                    <i class="fas fa-image"></i>
                                    <span>Нет фото</span>
                                </div>
                            </div>
                        `;
                    };
                    imgWrapper.appendChild(img);
                } else {
                    imgWrapper.innerHTML = `
                        <div class="carousel-paid__img-placeholder">
                            <div class="premium-placeholder-content">
                                <i class="fas fa-image"></i>
                                <span>Нет фото</span>
                            </div>
                        </div>
                    `;
                }

                const badge = document.createElement('div');
                badge.className = 'carousel-paid__badge carousel-paid__badge--premium';
                badge.innerHTML = '<i class="fas fa-crown"></i> Реклама';
                imgWrapper.appendChild(badge);

                const content = document.createElement('div');
                content.className = 'carousel-paid__content';
                content.innerHTML = `
                    <div class="carousel-paid__content-title">${this.escapeHtml(item.title) || 'Объявление'}</div>
                    <div class="carousel-paid__content-location">
                        <i class="fas fa-map-marker-alt"></i> ${this.escapeHtml(item.city) || 'Город не указан'}
                    </div>
                `;

                const btn = document.createElement('button');
                btn.className = 'carousel-paid__btn';
                btn.innerHTML = 'Подробнее →';
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (item.link) {
                        window.location.href = item.link;
                    }
                });

                content.appendChild(btn);
                card.appendChild(imgWrapper);
                card.appendChild(content);

                card.addEventListener('click', () => {
                    if (item.link) {
                        window.location.href = item.link;
                    }
                });
            }

            slide.appendChild(card);
            this.track.appendChild(slide);
        });

        this.renderDots();
    }

    escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    renderDots() {
        if (!this.dotsContainer) return;
        this.dotsContainer.innerHTML = '';

        // Показываем точки только для реальных объявлений (не заглушек)
        const realItems = this.items.filter(item => !item.isPlaceholder);
        const itemsToShow = realItems.length > 0 ? realItems : this.items;

        itemsToShow.forEach((_, idx) => {
            const dot = document.createElement('button');
            dot.className = 'carousel-paid__dot';
            dot.setAttribute('data-index', idx);
            dot.addEventListener('click', () => {
                this.stopAutoplay();
                this.goToSlide(idx);
                this.startAutoplay();
            });
            this.dotsContainer.appendChild(dot);
        });
    }

    attachEvents() {
        this.container.addEventListener('touchstart', this.onTouchStart.bind(this), { passive: false });
        this.container.addEventListener('touchmove', this.onTouchMove.bind(this), { passive: false });
        this.container.addEventListener('touchend', this.onTouchEnd.bind(this));

        this.container.addEventListener('mousedown', this.onMouseDown.bind(this));
        window.addEventListener('mousemove', this.onMouseMove.bind(this));
        window.addEventListener('mouseup', this.onMouseUp.bind(this));

        if (this.prevBtn) {
            this.prevBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.stopAutoplay();
                this.prev();
                this.startAutoplay();
            });
        }

        if (this.nextBtn) {
            this.nextBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.stopAutoplay();
                this.next();
                this.startAutoplay();
            });
        }

        this.container.querySelectorAll('img').forEach(img => {
            img.addEventListener('dragstart', (e) => e.preventDefault());
        });

        this.container.addEventListener('mouseenter', () => this.stopAutoplay());
        this.container.addEventListener('mouseleave', () => this.startAutoplay());
    }

    onTouchStart(e) {
        this.stopAutoplay();
        const touch = e.touches[0];
        this.startX = touch.clientX;
        this.startY = touch.clientY;
        this.startTranslate = this.getCurrentTranslate();
        this.isDragging = true;
        this.dragDirection = null;
        this.touchMoved = false;
        this.track.style.transition = 'none';
    }

    onTouchMove(e) {
        if (!this.isDragging) return;

        const touch = e.touches[0];
        const deltaX = touch.clientX - this.startX;
        const deltaY = touch.clientY - this.startY;
        const absDeltaX = Math.abs(deltaX);
        const absDeltaY = Math.abs(deltaY);

        if (this.dragDirection === null && (absDeltaX > 5 || absDeltaY > 5)) {
            this.dragDirection = absDeltaX > absDeltaY ? 'horizontal' : 'vertical';
        }

        if (this.dragDirection === 'vertical') {
            this.touchMoved = true;
            return;
        }

        if (this.dragDirection === 'horizontal') {
            e.preventDefault();
            this.touchMoved = true;
            let newTranslate = this.startTranslate + deltaX;

            const maxTranslate = this.centerOffset;
            const minTranslate = -(this.totalSlides - 1) * this.scrollStep + this.centerOffset;
            newTranslate = Math.min(Math.max(newTranslate, minTranslate), maxTranslate);

            this.track.style.transform = `translateX(${newTranslate}px)`;
        }
    }

    onTouchEnd(e) {
        if (!this.isDragging) {
            this.startAutoplay();
            return;
        }

        if (this.dragDirection === 'vertical' || !this.touchMoved) {
            this.isDragging = false;
            this.track.style.transition = 'transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1)';
            this.startAutoplay();
            this.dragDirection = null;
            return;
        }

        const currentTranslate = this.getCurrentTranslate();
        let targetIndex = Math.round((currentTranslate - this.centerOffset) / -this.scrollStep);
        targetIndex = Math.min(Math.max(targetIndex, 0), this.totalSlides - 1);

        this.goToIndexWithLoop(targetIndex);

        this.isDragging = false;
        this.dragDirection = null;
        this.touchMoved = false;
        this.startAutoplay();
    }

    onMouseDown(e) {
        e.preventDefault();
        this.stopAutoplay();
        this.startX = e.clientX;
        this.startTranslate = this.getCurrentTranslate();
        this.isDragging = true;
        this.container.style.cursor = 'grabbing';
        this.track.style.transition = 'none';
    }

    onMouseMove(e) {
        if (!this.isDragging) return;
        e.preventDefault();
        const deltaX = e.clientX - this.startX;
        let newTranslate = this.startTranslate + deltaX;

        const maxTranslate = this.centerOffset;
        const minTranslate = -(this.totalSlides - 1) * this.scrollStep + this.centerOffset;
        newTranslate = Math.min(Math.max(newTranslate, minTranslate), maxTranslate);

        this.track.style.transform = `translateX(${newTranslate}px)`;
    }

    onMouseUp(e) {
        if (!this.isDragging) {
            this.startAutoplay();
            return;
        }

        this.isDragging = false;
        this.container.style.cursor = '';
        this.track.style.transition = 'transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1)';

        const currentTranslate = this.getCurrentTranslate();
        let targetIndex = Math.round((currentTranslate - this.centerOffset) / -this.scrollStep);
        targetIndex = Math.min(Math.max(targetIndex, 0), this.totalSlides - 1);

        this.goToIndexWithLoop(targetIndex);
        this.startAutoplay();
    }

    getCurrentTranslate() {
        const transform = this.track.style.transform;
        const match = transform.match(/translateX\((-?\d+(?:\.\d+)?)px\)/);
        return match ? parseFloat(match[1]) : this.getOffsetForIndex(this.currentIndex);
    }

    getOffsetForIndex(index) {
        return -(index * this.scrollStep) + this.centerOffset;
    }

    updatePosition() {
        const offset = this.getOffsetForIndex(this.currentIndex);
        this.track.style.transform = `translateX(${offset}px)`;
    }

    getRealIndex() {
        if (this.totalRealSlides === 0) return 0;
        let realIndex = this.currentIndex - 3;
        if (realIndex < 0) realIndex = this.totalRealSlides + realIndex;
        if (realIndex >= this.totalRealSlides) realIndex = realIndex - this.totalRealSlides;
        return realIndex;
    }

    goToIndexWithLoop(index) {
        if (this.totalSlides === 0) return;

        this.currentIndex = index;
        this.updatePosition();

        if (this.currentIndex <= 2 && this.totalRealSlides > 0) {
            setTimeout(() => {
                this.track.style.transition = 'none';
                this.currentIndex = this.currentIndex + this.totalRealSlides;
                this.updatePosition();
                setTimeout(() => {
                    this.track.style.transition = 'transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1)';
                }, 50);
                this.updateSlidesClasses();
                this.updateActiveDot();
            }, 400);
        } else if (this.currentIndex >= this.totalSlides - 3 && this.totalRealSlides > 0) {
            setTimeout(() => {
                this.track.style.transition = 'none';
                this.currentIndex = this.currentIndex - this.totalRealSlides;
                this.updatePosition();
                setTimeout(() => {
                    this.track.style.transition = 'transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1)';
                }, 50);
                this.updateSlidesClasses();
                this.updateActiveDot();
            }, 400);
        }

        this.updateSlidesClasses();
        this.updateActiveDot();
    }

    goToSlide(index) {
        if (this.totalRealSlides === 0) return;
        if (index < 0) index = 0;
        if (index >= this.totalRealSlides) index = this.totalRealSlides - 1;
        this.currentIndex = index + 3;
        this.updatePosition();
        this.updateSlidesClasses();
        this.updateActiveDot();
    }

    updateSlidesClasses() {
        const slides = this.track.querySelectorAll('.carousel-paid__slide');
        slides.forEach((slide, idx) => {
            const distance = Math.abs(idx - this.currentIndex);
            slide.classList.remove('carousel-paid__slide--center', 'carousel-paid__slide--side', 'carousel-paid__slide--far');

            if (distance === 0) {
                slide.classList.add('carousel-paid__slide--center');
            } else if (distance === 1) {
                slide.classList.add('carousel-paid__slide--side');
            } else {
                slide.classList.add('carousel-paid__slide--far');
            }
        });
    }

    updateActiveDot() {
        if (!this.dotsContainer) return;
        const dots = this.dotsContainer.querySelectorAll('.carousel-paid__dot');
        const realIndex = this.getRealIndex();

        dots.forEach((dot, idx) => {
            if (idx === realIndex) {
                dot.classList.add('carousel-paid__dot--active');
            } else {
                dot.classList.remove('carousel-paid__dot--active');
            }
        });
    }

    next() {
        if (this.totalSlides === 0) return;
        this.goToIndexWithLoop(this.currentIndex + 1);
    }

    prev() {
        if (this.totalSlides === 0) return;
        this.goToIndexWithLoop(this.currentIndex - 1);
    }

    startAutoplay() {
        this.stopAutoplay();
        if (this.totalSlides > 0) {
            this.autoplayInterval = setInterval(() => {
                this.next();
            }, this.autoplayDelay);
        }
    }

    stopAutoplay() {
        if (this.autoplayInterval) {
            clearInterval(this.autoplayInterval);
            this.autoplayInterval = null;
        }
    }

    /**
     * Обновляет данные карусели новыми элементами
     * @param {Array} newItems - новые элементы для карусели
     */
    updateItems(newItems) {
        // Останавливаем автопрокрутку
        this.stopAutoplay();

        // Обновляем глобальные данные
        if (newItems && newItems.length > 0) {
            window.paidCarouselItems = newItems;
            sessionStorage.setItem('paidCarouselItems', JSON.stringify(newItems));
            this.items = newItems;
        } else {
            // Если нет данных - очищаем кеш
            window.paidCarouselItems = [];
            sessionStorage.removeItem('paidCarouselItems');
            this.items = [];
        }

        // Нормализуем элементы (добавляем заглушки если нужно)
        this.normalizeItems();
        this.totalRealSlides = this.items.length;

        // Пересоздаем клоны
        if (this.totalRealSlides > 0) {
            const lastThree = this.items.slice(-3);
            const firstThree = this.items.slice(0, 3);
            this.itemsWithClones = [...lastThree, ...this.items, ...firstThree];
        } else {
            this.itemsWithClones = [];
        }

        // Перерисовываем
        this.render();

        // Пересчитываем размеры и обновляем позицию
        setTimeout(() => {
            this.calculateDimensions();
            if (this.itemsWithClones.length > 0) {
                this.currentIndex = this.totalRealSlides > 0 ? 3 : 0;
                this.updatePosition();
                this.updateSlidesClasses();
                this.updateActiveDot();
            }
            this.startAutoplay();
        }, 50);
    }

    /**
     * Принудительно обновляет карусель, запрашивая свежие данные с сервера
     */
    refresh() {
        fetch('/api/paid-carousel')
            .then(response => response.json())
            .then(data => {
                if (data && data.length > 0) {
                    this.updateItems(data);
                } else {
                    this.updateItems([]);
                }
            })
            .catch(err => {
                console.log('Carousel refresh error:', err);
                // При ошибке показываем заглушки
                this.updateItems([]);
            });
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(function() {
        const carouselElement = document.getElementById('paidCarousel');
        if (!carouselElement) return;

        // Если уже есть глобальный экземпляр - пересоздаем
        if (window.paidCarousel) {
            window.paidCarousel.stopAutoplay();
            window.paidCarousel = null;
        }

        // Получаем данные
        let carouselData = window.paidCarouselItems || [];

        // Если нет данных в глобальной переменной, проверяем sessionStorage
        if (!carouselData || carouselData.length === 0) {
            const saved = sessionStorage.getItem('paidCarouselItems');
            if (saved) {
                try {
                    carouselData = JSON.parse(saved);
                } catch(e) {}
            }
        }

        // Создаем новую карусель
        window.paidCarousel = new PaidCarousel('paidCarousel', {
            items: carouselData || [],
            autoplayDelay: 4000
        });

    }, 100);
});

// При восстановлении страницы из bfcache - обновляем
window.addEventListener('pageshow', function(event) {
    setTimeout(function() {
        if (window.paidCarousel && typeof window.paidCarousel.refresh === 'function') {
            window.paidCarousel.refresh();
        }
    }, 100);
});