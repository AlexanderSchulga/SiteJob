/**
 * Карусель с эффектом "синус" (центр больше, края меньше)
 * Автопрокрутка каждые 5 секунды, листание тачем/мышью
 * Поддерживает вертикальный скролл страницы
 */

class SineCarousel {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;

        this.track = this.container.querySelector('.carousel-sine__track');
        this.dotsContainer = this.container.querySelector('.carousel-sine__dots');

        this.items = options.items || this.getDefaultItems();
        this.currentIndex = 0;
        this.autoplayInterval = null;
        this.autoplayDelay = options.autoplayDelay || 5000;
        this.isDragging = false;
        this.startX = 0;
        this.startY = 0;
        this.startTranslate = 0;
        this.dragDirection = null; // 'horizontal', 'vertical' или null
        this.touchMoved = false;

        // Для бесконечной прокрутки
        this.itemsWithClones = [];
        this.totalRealSlides = 0;
        this.totalSlides = 0;

        this.init();
    }

    getDefaultItems() {
        return [
            { img: '/images/carousel/carusel_product.jpg', alt: 'Товары' },
            { img: '/images/carousel/carusel_job.jpg', alt: 'Услуги' },
            { img: '/images/carousel/carusel_vac.jpg', alt: 'Вакансии' },
            { img: '/images/carousel/carusel_res.jpg', alt: 'Резюме' }
        ];
    }

    init() {
        if (!this.track) return;

        // Создаем массив с клонами для бесконечной прокрутки
        this.totalRealSlides = this.items.length;

        // Добавляем клоны: сначала последние 2, потом оригиналы, потом первые 2
        const lastTwo = this.items.slice(-2);
        const firstTwo = this.items.slice(0, 2);
        this.itemsWithClones = [...lastTwo, ...this.items, ...firstTwo];

        this.render();
        this.calculateDimensions();
        this.attachEvents();

        window.addEventListener('resize', () => {
            this.calculateDimensions();
            this.updatePosition();
        });

        // Начинаем с первого реального слайда (индекс 2)
        this.currentIndex = 2;
        this.updatePosition();
        this.updateSlidesClasses();
        this.updateActiveDot();
        this.startAutoplay();
    }

    calculateDimensions() {
        const slides = this.track.querySelectorAll('.carousel-sine__slide');
        if (slides.length === 0) return;

        this.slideWidth = slides[0].offsetWidth;
        const containerWidth = this.container.offsetWidth;

        // Шаг прокрутки = ширина слайда
        this.scrollStep = this.slideWidth;

        // Смещение для центрирования
        this.centerOffset = (containerWidth - this.slideWidth) / 2;
        this.totalSlides = slides.length;
    }

    render() {
        this.track.innerHTML = '';

        this.itemsWithClones.forEach((item, idx) => {
            const slide = document.createElement('div');
            slide.className = 'carousel-sine__slide';
            slide.setAttribute('data-index', idx);

            const imgWrapper = document.createElement('div');
            imgWrapper.className = 'carousel-sine__img-wrapper';

            const img = document.createElement('img');
            img.src = item.img;
            img.alt = item.alt || 'Фото';
            img.className = 'carousel-sine__img';
            img.loading = 'lazy';

            img.onerror = function() {
                this.src = '/images/placeholder.jpg';
            };

            imgWrapper.appendChild(img);
            slide.appendChild(imgWrapper);
            this.track.appendChild(slide);
        });

        this.renderDots();
    }

    renderDots() {
        if (!this.dotsContainer) return;
        this.dotsContainer.innerHTML = '';
        this.items.forEach((_, idx) => {
            const dot = document.createElement('button');
            dot.className = 'carousel-sine__dot';
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
        // Touch события с определением направления
        this.container.addEventListener('touchstart', this.onTouchStart.bind(this), { passive: false });
        this.container.addEventListener('touchmove', this.onTouchMove.bind(this), { passive: false });
        this.container.addEventListener('touchend', this.onTouchEnd.bind(this));

        // Mouse события
        this.container.addEventListener('mousedown', this.onMouseDown.bind(this));
        window.addEventListener('mousemove', this.onMouseMove.bind(this));
        window.addEventListener('mouseup', this.onMouseUp.bind(this));

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

        // Определяем направление только если ещё не определили и перемещение > 5px
        if (this.dragDirection === null && (absDeltaX > 5 || absDeltaY > 5)) {
            if (absDeltaX > absDeltaY) {
                this.dragDirection = 'horizontal';
            } else {
                this.dragDirection = 'vertical';
            }
        }

        // Если вертикальный скролл - не мешаем, даем странице скроллиться
        if (this.dragDirection === 'vertical') {
            // Не вызываем preventDefault, позволяем странице скроллиться
            this.touchMoved = true;
            return;
        }

        // Горизонтальный скролл - обрабатываем карусель
        if (this.dragDirection === 'horizontal') {
            e.preventDefault();
            this.touchMoved = true;

            let newTranslate = this.startTranslate + deltaX;

            // Ограничиваем прокрутку
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

        // Если было вертикальное движение - просто выходим, не меняем слайд
        if (this.dragDirection === 'vertical' || !this.touchMoved) {
            this.isDragging = false;
            this.track.style.transition = 'transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1)';
            this.startAutoplay();
            this.dragDirection = null;
            return;
        }

        // Горизонтальное движение - определяем целевой слайд
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

        // Ограничиваем прокрутку
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
        // Смещение для указанного индекса с центрированием
        const scrollOffset = -(index * this.scrollStep);
        return scrollOffset + this.centerOffset;
    }

    updatePosition() {
        const offset = this.getOffsetForIndex(this.currentIndex);
        this.track.style.transform = `translateX(${offset}px)`;
    }

    getRealIndex() {
        let realIndex = this.currentIndex - 2;
        if (realIndex < 0) realIndex = this.totalRealSlides + realIndex;
        if (realIndex >= this.totalRealSlides) realIndex = realIndex - this.totalRealSlides;
        return realIndex;
    }

    goToIndexWithLoop(index) {
        this.currentIndex = index;
        this.updatePosition();

        // Проверяем, не на клонах ли мы
        const realIndex = this.getRealIndex();

        if (this.currentIndex <= 1) {
            // На клонах начала - перепрыгиваем в конец реальных через полсекунды
            setTimeout(() => {
                this.track.style.transition = 'none';
                this.currentIndex = this.totalRealSlides + this.currentIndex;
                this.updatePosition();
                setTimeout(() => {
                    this.track.style.transition = 'transform 0.4s cubic-bezier(0.2, 0.9, 0.4, 1.1)';
                }, 50);
                this.updateSlidesClasses();
                this.updateActiveDot();
            }, 400);
        } else if (this.currentIndex >= this.totalSlides - 2) {
            // На клонах конца - перепрыгиваем в начало реальных
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
        if (index < 0) index = 0;
        if (index >= this.totalRealSlides) index = this.totalRealSlides - 1;

        // Переводим реальный индекс в индекс с клонами
        this.currentIndex = index + 2;
        this.updatePosition();
        this.updateSlidesClasses();
        this.updateActiveDot();
    }

    updateSlidesClasses() {
        const slides = this.track.querySelectorAll('.carousel-sine__slide');

        slides.forEach((slide, idx) => {
            const imgWrapper = slide.querySelector('.carousel-sine__img-wrapper');
            if (!imgWrapper) return;

            imgWrapper.classList.remove('carousel-sine__img-wrapper--center', 'carousel-sine__img-wrapper--side', 'carousel-sine__img-wrapper--far');

            let distance = Math.abs(idx - this.currentIndex);

            if (distance === 0) {
                imgWrapper.classList.add('carousel-sine__img-wrapper--center');
                slide.classList.add('carousel-sine__slide--center');
                slide.classList.remove('carousel-sine__slide--side', 'carousel-sine__slide--far');
            } else if (distance === 1) {
                imgWrapper.classList.add('carousel-sine__img-wrapper--side');
                slide.classList.add('carousel-sine__slide--side');
                slide.classList.remove('carousel-sine__slide--center', 'carousel-sine__slide--far');
            } else {
                imgWrapper.classList.add('carousel-sine__img-wrapper--far');
                slide.classList.add('carousel-sine__slide--far');
                slide.classList.remove('carousel-sine__slide--center', 'carousel-sine__slide--side');
            }
        });
    }

    updateActiveDot() {
        if (!this.dotsContainer) return;
        const dots = this.dotsContainer.querySelectorAll('.carousel-sine__dot');
        const realIndex = this.getRealIndex();

        dots.forEach((dot, idx) => {
            if (idx === realIndex) {
                dot.classList.add('carousel-sine__dot--active');
            } else {
                dot.classList.remove('carousel-sine__dot--active');
            }
        });
    }

    next() {
        let newIndex = this.currentIndex + 1;
        if (newIndex >= this.totalSlides) {
            newIndex = 2;
        }
        this.goToIndexWithLoop(newIndex);
    }

    prev() {
        let newIndex = this.currentIndex - 1;
        if (newIndex < 0) {
            newIndex = this.totalSlides - 3;
        }
        this.goToIndexWithLoop(newIndex);
    }

    startAutoplay() {
        if (this.autoplayInterval) clearInterval(this.autoplayInterval);
        this.autoplayInterval = setInterval(() => {
            this.next();
        }, this.autoplayDelay);
    }

    stopAutoplay() {
        if (this.autoplayInterval) {
            clearInterval(this.autoplayInterval);
            this.autoplayInterval = null;
        }
    }
}

// Инициализация
document.addEventListener('DOMContentLoaded', function() {
    let carouselItems = [];

    if (typeof window.carouselPhotos !== 'undefined' && window.carouselPhotos.length) {
        carouselItems = window.carouselPhotos;
    } else {
        carouselItems = [
            { img: '/images/carousel/carusel_product.jpg', alt: 'Товары' },
            { img: '/images/carousel/carusel_job.jpg', alt: 'Услуги' },
            { img: '/images/carousel/carusel_vac.jpg', alt: 'Вакансии' },
            { img: '/images/carousel/carusel_res.jpg', alt: 'Резюме' }
        ];
    }

    if (document.getElementById('featuredCarousel')) {
        new SineCarousel('featuredCarousel', {
            items: carouselItems,
            autoplayDelay: 5000
        });
    }
});