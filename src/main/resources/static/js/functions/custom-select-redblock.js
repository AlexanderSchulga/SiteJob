/**
 * Кастомный выпадающий список вместо нативного select
 * Полностью переработан для мобильных устройств
 * Версия 2.0 - исправлена проблема с радиокнопками
 */

class CustomSelect {
    constructor(selectElement, options = {}) {
        this.selectElement = selectElement;
        this.options = {
            searchable: options.searchable !== false,
            maxHeight: options.maxHeight || 280,
            ...options
        };
        this.wrapper = null;
        this.trigger = null;
        this.dropdown = null;
        this.isOpen = false;
        this.closeTimeout = null;
        this.isSelecting = false;
        this.isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
        this.preventClose = false;
        this.keyboardOpen = false;
        this.touchStartTime = 0;
        this.scrollPosition = 0;

        this.init();
    }

    init() {
        // Скрываем оригинальный select
        this.selectElement.style.display = 'none';
        this.selectElement.setAttribute('data-custom-replaced', 'true');

        // Создаем обертку
        this.wrapper = document.createElement('div');
        this.wrapper.className = 'custom-select-wrapper';
        this.wrapper.style.position = 'relative';
        this.wrapper.style.width = '100%';

        // Создаем триггер (видимая часть)
        this.trigger = document.createElement('div');
        this.trigger.className = 'custom-select-trigger';
        this.trigger.setAttribute('role', 'button');
        this.trigger.setAttribute('tabindex', '0');
        this.trigger.setAttribute('aria-haspopup', 'listbox');
        this.trigger.setAttribute('aria-expanded', 'false');

        const selectedOption = this.selectElement.options[this.selectElement.selectedIndex];
        const selectedText = selectedOption?.text || 'Выберите...';

        this.trigger.innerHTML = `
            <span class="selected-value">${this.escapeHtml(selectedText)}</span>
            <span class="arrow">
                <svg width="12" height="8" viewBox="0 0 12 8" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M1 1.5L6 6.5L11 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </span>
        `;

        // Создаем выпадающий список
        this.dropdown = document.createElement('div');
        this.dropdown.className = 'custom-dropdown';
        this.dropdown.setAttribute('role', 'listbox');
        this.dropdown.setAttribute('tabindex', '-1');

        // Добавляем поиск если нужно
        if (this.options.searchable && this.selectElement.options.length > 5) {
            this.addSearchInput();
        }

        // Заполняем опции
        this.populateOptions();

        // Собираем DOM
        this.wrapper.appendChild(this.trigger);
        this.wrapper.appendChild(this.dropdown);
        this.selectElement.parentNode.insertBefore(this.wrapper, this.selectElement.nextSibling);

        // Навешиваем обработчики
        this.attachEventHandlers();
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    addSearchInput() {
        const searchDiv = document.createElement('div');
        searchDiv.className = 'custom-dropdown-search';
        searchDiv.innerHTML = `
            <div class="search-icon">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="11" cy="11" r="8"/>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
            </div>
            <input type="text" placeholder="Поиск..." autocomplete="off">
            <button type="button" class="search-clear" style="display: none;">✕</button>
        `;
        this.dropdown.appendChild(searchDiv);

        const searchInput = searchDiv.querySelector('input');
        const clearButton = searchDiv.querySelector('.search-clear');

        // Обработчик ввода
        searchInput.addEventListener('input', (e) => {
            e.stopPropagation();
            const value = e.target.value;
            this.filterOptions(value);
            if (clearButton) {
                clearButton.style.display = value ? 'flex' : 'none';
            }
        });

        // Очистка поиска
        if (clearButton) {
            clearButton.addEventListener('click', (e) => {
                e.stopPropagation();
                searchInput.value = '';
                this.filterOptions('');
                clearButton.style.display = 'none';
                searchInput.focus();
            });
        }

        // Предотвращаем закрытие при клике на поиск
        searchInput.addEventListener('mousedown', (e) => {
            e.stopPropagation();
            this.preventClose = true;
        });

        searchInput.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Для мобильных
        if (this.isMobile) {
            searchInput.addEventListener('touchstart', (e) => {
                e.stopPropagation();
                this.preventClose = true;
                this.keyboardOpen = true;
            });

            searchInput.addEventListener('focus', () => {
                this.preventClose = true;
                this.keyboardOpen = true;
                setTimeout(() => {
                    if (this.isOpen && searchInput) {
                        searchInput.scrollIntoView({ block: 'center', behavior: 'smooth' });
                    }
                }, 100);
            });

            searchInput.addEventListener('blur', () => {
                setTimeout(() => {
                    this.keyboardOpen = false;
                    if (!this.isSelecting) {
                        setTimeout(() => {
                            if (!this.keyboardOpen) {
                                this.preventClose = false;
                            }
                        }, 300);
                    }
                }, 200);
            });
        }
    }

    populateOptions(filterText = '') {
        // Удаляем существующие опции (кроме поиска)
        const optionsToRemove = this.dropdown.querySelectorAll('.custom-dropdown-option');
        optionsToRemove.forEach(opt => opt.remove());

        const searchDiv = this.dropdown.querySelector('.custom-dropdown-search');
        let visibleCount = 0;
        const filterLower = filterText.toLowerCase();

        for (let i = 0; i < this.selectElement.options.length; i++) {
            const option = this.selectElement.options[i];
            const optionText = option.text;
            const optionValue = option.value;

            // Фильтрация
            if (filterText && !optionText.toLowerCase().includes(filterLower)) {
                continue;
            }

            visibleCount++;

            const optionDiv = document.createElement('div');
            optionDiv.className = 'custom-dropdown-option';
            if (option.selected) {
                optionDiv.classList.add('selected');
            }

            // Укорачиваем длинный текст
            let displayText = optionText;
            if (displayText.length > 50) {
                displayText = displayText.substring(0, 47) + '...';
            }
            optionDiv.textContent = displayText;
            if (displayText !== optionText) {
                optionDiv.title = optionText;
            }
            optionDiv.dataset.value = optionValue;
            optionDiv.dataset.index = i;
            optionDiv.dataset.text = optionText;

            optionDiv.addEventListener('click', (e) => {
                e.stopPropagation();
                this.selectOption(parseInt(optionDiv.dataset.index));
                this.closeDropdown();
            });

            // Для мобильных
            if (this.isMobile) {
                optionDiv.addEventListener('touchstart', (e) => {
                    e.stopPropagation();
                    this.touchStartTime = Date.now();
                });

                optionDiv.addEventListener('touchend', (e) => {
                    e.stopPropagation();
                    // Предотвращаем случайные закрытия
                    if (Date.now() - this.touchStartTime < 500) {
                        this.selectOption(parseInt(optionDiv.dataset.index));
                        this.closeDropdown();
                    }
                });
            }

            // Вставляем перед концом dropdown (после поиска)
            if (searchDiv) {
                this.dropdown.insertBefore(optionDiv, searchDiv.nextSibling);
            } else {
                this.dropdown.appendChild(optionDiv);
            }
        }

        // Показываем сообщение если ничего не найдено
        const noResultsMsg = this.dropdown.querySelector('.no-results-message');
        if (visibleCount === 0) {
            if (!noResultsMsg) {
                const msg = document.createElement('div');
                msg.className = 'no-results-message';
                msg.innerHTML = '<span>Ничего не найдено</span>';
                if (searchDiv) {
                    this.dropdown.insertBefore(msg, searchDiv.nextSibling);
                } else {
                    this.dropdown.appendChild(msg);
                }
            }
        } else if (noResultsMsg) {
            noResultsMsg.remove();
        }
    }

    filterOptions(searchText) {
        const options = this.dropdown.querySelectorAll('.custom-dropdown-option');
        const lowerSearch = searchText.toLowerCase();
        let visibleCount = 0;

        options.forEach(option => {
            const text = option.dataset.text || option.textContent;
            if (text.toLowerCase().includes(lowerSearch)) {
                option.style.display = '';
                visibleCount++;
            } else {
                option.style.display = 'none';
            }
        });

        // Обновляем сообщение о отсутствии результатов
        let noResultsMsg = this.dropdown.querySelector('.no-results-message');
        if (visibleCount === 0) {
            if (!noResultsMsg) {
                noResultsMsg = document.createElement('div');
                noResultsMsg.className = 'no-results-message';
                noResultsMsg.innerHTML = '<span>Ничего не найдено</span>';
                const searchDiv = this.dropdown.querySelector('.custom-dropdown-search');
                if (searchDiv) {
                    this.dropdown.insertBefore(noResultsMsg, searchDiv.nextSibling);
                } else {
                    this.dropdown.appendChild(noResultsMsg);
                }
            }
        } else if (noResultsMsg) {
            noResultsMsg.remove();
        }
    }

    selectOption(index) {
        if (index === undefined || index === null) return;

        this.isSelecting = true;
        this.preventClose = false;

        // Обновляем оригинальный select
        this.selectElement.selectedIndex = index;
        const selectedOption = this.selectElement.options[index];

        // Обновляем отображаемый текст
        const selectedSpan = this.trigger.querySelector('.selected-value');
        let displayText = selectedOption.text;
        if (displayText.length > 45) {
            displayText = displayText.substring(0, 42) + '...';
        }
        selectedSpan.textContent = displayText;
        if (displayText !== selectedOption.text) {
            selectedSpan.title = selectedOption.text;
        }

        // Обновляем классы у опций
        const options = this.dropdown.querySelectorAll('.custom-dropdown-option');
        options.forEach((opt, i) => {
            if (i === index) {
                opt.classList.add('selected');
            } else {
                opt.classList.remove('selected');
            }
        });

        // Триггерим событие change
        const changeEvent = new Event('change', { bubbles: true });
        this.selectElement.dispatchEvent(changeEvent);

        setTimeout(() => {
            this.isSelecting = false;
        }, 300);
    }

    attachEventHandlers() {
        // Открытие/закрытие по клику на триггер
        this.trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            e.preventDefault();
            this.toggleDropdown();
        });

        // Клавиатура: Enter и пробел
        this.trigger.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ' || e.key === 'Space') {
                e.preventDefault();
                this.toggleDropdown();
            }
        });

        // Предотвращаем всплытие кликов от wrapper
        this.wrapper.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Предотвращаем закрытие при клике внутри dropdown
        this.dropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Закрытие при клике вне компонента
        document.addEventListener('click', (e) => {
            if (this.preventClose) return;
            if (this.wrapper && !this.wrapper.contains(e.target) && this.isOpen && !this.isSelecting) {
                this.closeDropdown();
            }
        });

        // Закрытие при нажатии Escape
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && this.isOpen) {
                this.closeDropdown();
            }
        });

        // Синхронизация при изменении оригинального select
        this.selectElement.addEventListener('change', () => this.syncFromSelect());

        // Закрытие при изменении размера окна
        window.addEventListener('resize', () => {
            if (this.isOpen) {
                this.positionDropdown();
            }
        });

        // Мобильные оптимизации
        if (this.isMobile) {
            this.setupMobileOptimizations();
        }
    }

    toggleDropdown() {
        if (this.isOpen) {
            this.closeDropdown();
        } else {
            this.openDropdown();
        }
    }

    openDropdown() {
        if (this.closeTimeout) {
            clearTimeout(this.closeTimeout);
            this.closeTimeout = null;
        }

        // Закрываем другие открытые dropdowns
        document.querySelectorAll('.custom-dropdown.show').forEach(d => {
            const parent = d.parentElement;
            if (parent && parent !== this.wrapper) {
                const otherSelect = parent.querySelector('.custom-select-trigger');
                if (otherSelect) otherSelect.classList.remove('active');
                d.classList.remove('show');
                d.style.display = 'none';
                const otherWrapper = parent.querySelector('.custom-select-wrapper');
                if (otherWrapper) otherWrapper.classList.remove('open');
            }
        });

        // Позиционируем и открываем
        this.positionDropdown();

        this.dropdown.classList.add('show');
        this.dropdown.style.display = 'block';
        this.trigger.classList.add('active');
        this.trigger.setAttribute('aria-expanded', 'true');
        this.wrapper.classList.add('open');
        this.isOpen = true;

        // Фокусируемся на поиске если есть
        const searchInput = this.dropdown.querySelector('.custom-dropdown-search input');
        if (searchInput && !this.isMobile) {
            setTimeout(() => {
                if (this.isOpen) {
                    searchInput.focus();
                }
            }, 50);
        }

        // Скроллим к выбранному элементу
        const selected = this.dropdown.querySelector('.custom-dropdown-option.selected');
        if (selected) {
            selected.scrollIntoView({ block: 'nearest' });
        }

        if (this.isMobile) {
            this.scrollPosition = window.scrollY;
            // Блокируем скролл body
            document.body.style.overflow = 'hidden';
        }
    }

    positionDropdown() {
        const rect = this.trigger.getBoundingClientRect();
        const viewportHeight = window.innerHeight;
        const viewportWidth = window.innerWidth;

        // Сбрасываем стили
        this.dropdown.style.top = '';
        this.dropdown.style.bottom = '';
        this.dropdown.style.left = '';
        this.dropdown.style.right = '';
        this.dropdown.style.maxHeight = '';

        // Ширина
        const width = Math.min(rect.width, viewportWidth - 20);
        this.dropdown.style.width = width + 'px';
        this.dropdown.style.minWidth = Math.max(200, width) + 'px';

        // Определяем направление открытия
        const spaceBelow = viewportHeight - rect.bottom;
        const spaceAbove = rect.top;
        const preferredHeight = Math.min(this.options.maxHeight, Math.max(spaceBelow, spaceAbove) - 20);

        if (spaceBelow >= preferredHeight || spaceBelow >= spaceAbove) {
            // Открываем вниз
            let topPosition = rect.height + 4;
            let maxHeight = Math.min(this.options.maxHeight, spaceBelow - 10);
            this.dropdown.style.top = topPosition + 'px';
            this.dropdown.style.maxHeight = Math.max(120, maxHeight) + 'px';
        } else {
            // Открываем вверх
            let topPosition = -Math.min(this.options.maxHeight, spaceAbove - 10) - 4;
            let maxHeight = Math.min(this.options.maxHeight, spaceAbove - 10);
            this.dropdown.style.top = topPosition + 'px';
            this.dropdown.style.maxHeight = Math.max(120, maxHeight) + 'px';
        }

        // Горизонтальное позиционирование
        let leftPosition = rect.left;
        if (leftPosition + width > viewportWidth - 10) {
            leftPosition = viewportWidth - width - 10;
        }
        if (leftPosition < 10) {
            leftPosition = 10;
        }

        this.dropdown.style.left = (leftPosition - rect.left) + 'px';
        this.dropdown.style.right = 'auto';
    }

    closeDropdown() {
        if (this.preventClose || this.isSelecting) return;

        if (this.closeTimeout) {
            clearTimeout(this.closeTimeout);
        }

        this.closeTimeout = setTimeout(() => {
            this.dropdown.classList.remove('show');
            this.dropdown.style.display = 'none';
            this.trigger.classList.remove('active');
            this.trigger.setAttribute('aria-expanded', 'false');
            this.wrapper.classList.remove('open');
            this.isOpen = false;
            this.keyboardOpen = false;
            this.closeTimeout = null;

            if (this.isMobile) {
                document.body.style.overflow = '';
                // Не скроллим обратно, чтобы не мешать
                // window.scrollTo(0, this.scrollPosition);
                this.scrollPosition = 0;
            }
        }, this.isMobile ? 150 : 100);
    }

    syncFromSelect() {
        const selectedIndex = this.selectElement.selectedIndex;
        const selectedOption = this.selectElement.options[selectedIndex];
        if (selectedOption) {
            const selectedSpan = this.trigger.querySelector('.selected-value');
            let displayText = selectedOption.text;
            if (displayText.length > 45) {
                displayText = displayText.substring(0, 42) + '...';
            }
            selectedSpan.textContent = displayText;

            const options = this.dropdown.querySelectorAll('.custom-dropdown-option');
            options.forEach((opt, i) => {
                if (i === selectedIndex) {
                    opt.classList.add('selected');
                } else {
                    opt.classList.remove('selected');
                }
            });
        }
    }

    setupMobileOptimizations() {
        // Предотвращаем прокрутку страницы при скролле внутри dropdown
        this.dropdown.addEventListener('touchstart', (e) => {
            this.preventClose = true;
        }, { passive: true });

        this.dropdown.addEventListener('touchmove', (e) => {
            const target = e.target;
            const isScrollable = this.dropdown.scrollHeight > this.dropdown.clientHeight;

            if (isScrollable) {
                const scrollTop = this.dropdown.scrollTop;
                const scrollHeight = this.dropdown.scrollHeight;
                const height = this.dropdown.clientHeight;

                if ((scrollTop === 0 && e.touches[0].clientY > e.touches[0].clientY) ||
                    (scrollTop + height >= scrollHeight && e.touches[0].clientY < e.touches[0].clientY)) {
                    // На границах - не блокируем
                } else {
                    e.stopPropagation();
                }
            }
        }, { passive: false });

        this.dropdown.addEventListener('touchend', () => {
            setTimeout(() => {
                if (!this.keyboardOpen) {
                    this.preventClose = false;
                }
            }, 300);
        });

        // Обработка клика вне для мобильных
        document.addEventListener('touchstart', (e) => {
            if (this.preventClose) return;
            if (this.wrapper && !this.wrapper.contains(e.target) && this.isOpen) {
                this.closeDropdown();
            }
        });
    }

    destroy() {
        if (this.closeTimeout) {
            clearTimeout(this.closeTimeout);
        }
        if (this.wrapper && this.wrapper.parentNode) {
            this.wrapper.parentNode.removeChild(this.wrapper);
        }
        this.selectElement.style.display = '';
        this.selectElement.removeAttribute('data-custom-replaced');
        this.selectElement.removeAttribute('data-custom-init');

        if (this.isMobile) {
            document.body.style.overflow = '';
        }
    }

    // Публичный метод для обновления опций
    updateOptions() {
        const filterText = this.dropdown.querySelector('.custom-dropdown-search input')?.value || '';
        this.populateOptions(filterText);
    }

    // Публичный метод для получения текущего значения
    getValue() {
        return this.selectElement.value;
    }

    // Публичный метод для установки значения
    setValue(value) {
        for (let i = 0; i < this.selectElement.options.length; i++) {
            if (this.selectElement.options[i].value === value) {
                this.selectOption(i);
                break;
            }
        }
    }
}

// Инициализация всех select на странице с классом .custom-select или по ID
document.addEventListener('DOMContentLoaded', function() {
    let initAttempts = 0;
    const maxAttempts = 20;
    let initializedSelects = new Set();

    function initSelect(selectElement, options = {}) {
        if (!selectElement || selectElement.hasAttribute('data-custom-init')) return false;
        if (selectElement.tagName !== 'SELECT') return false;

        try {
            const optionsCount = selectElement.options.length;
            const isSearchable = options.searchable !== undefined ? options.searchable : (optionsCount > 8);

            new CustomSelect(selectElement, {
                searchable: isSearchable,
                maxHeight: options.maxHeight || 280
            });
            selectElement.setAttribute('data-custom-init', 'true');
            initializedSelects.add(selectElement.id);
            return true;
        } catch (error) {
            console.warn('Failed to init custom select:', error);
            return false;
        }
    }

    // Инициализация select с ID
    const regionSelect = document.getElementById('regionSelect');
    if (regionSelect) {
        initSelect(regionSelect, { searchable: false, maxHeight: 250 });
    }

    const citySelect = document.getElementById('citySelect');
    if (citySelect) {
        initSelect(citySelect, { searchable: true, maxHeight: 280 });
    }

    // Инициализация select с классом
    document.querySelectorAll('select.custom-select-init').forEach(select => {
        initSelect(select);
    });

    // Наблюдатель за динамически добавляемыми select
    const observer = new MutationObserver(function(mutations) {
        for (const mutation of mutations) {
            if (mutation.type === 'childList') {
                for (const node of mutation.addedNodes) {
                    if (node.nodeType === 1) { // Element node
                        // Проверяем сам элемент
                        if (node.tagName === 'SELECT' && !node.hasAttribute('data-custom-init')) {
                            const isRegion = node.id === 'regionSelect';
                            const isCity = node.id === 'citySelect';
                            initSelect(node, {
                                searchable: isCity,
                                maxHeight: isRegion ? 250 : 280
                            });
                        }
                        // Проверяем вложенные элементы
                        if (node.querySelectorAll) {
                            node.querySelectorAll('select').forEach(select => {
                                if (!select.hasAttribute('data-custom-init')) {
                                    const isRegion = select.id === 'regionSelect';
                                    const isCity = select.id === 'citySelect';
                                    initSelect(select, {
                                        searchable: isCity,
                                        maxHeight: isRegion ? 250 : 280
                                    });
                                }
                            });
                        }
                    }
                }
            }
        }
    });

    observer.observe(document.body, { childList: true, subtree: true });

    // Таймер для повторной инициализации
    const interval = setInterval(() => {
        let initialized = false;

        const regionSelectAgain = document.getElementById('regionSelect');
        if (regionSelectAgain && !regionSelectAgain.hasAttribute('data-custom-init')) {
            initSelect(regionSelectAgain, { searchable: false, maxHeight: 250 });
            initialized = true;
        }

        const citySelectAgain = document.getElementById('citySelect');
        if (citySelectAgain && !citySelectAgain.hasAttribute('data-custom-init')) {
            initSelect(citySelectAgain, { searchable: true, maxHeight: 280 });
            initialized = true;
        }

        if (initialized) {
            initAttempts = 0;
        } else if (initAttempts >= maxAttempts) {
            clearInterval(interval);
        } else {
            initAttempts++;
        }
    }, 500);

    // Останавливаем после 15 секунд
    setTimeout(() => {
        clearInterval(interval);
        observer.disconnect();
    }, 15000);
});