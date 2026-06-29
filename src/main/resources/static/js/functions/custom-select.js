/**
 * Кастомный выпадающий список вместо нативного select
 * Исправлено: фокус не съезжает, клавиатура не исчезает на мобильных
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

        this.init();
    }

    init() {
        this.selectElement.style.display = 'none';

        this.wrapper = document.createElement('div');
        this.wrapper.className = 'custom-select-wrapper';
        this.wrapper.style.position = 'relative';
        this.wrapper.style.width = '100%';

        this.trigger = document.createElement('div');
        this.trigger.className = 'custom-select-trigger';
        this.trigger.setAttribute('role', 'button');
        this.trigger.setAttribute('tabindex', '0');

        const selectedOption = this.selectElement.options[this.selectElement.selectedIndex];
        const selectedText = selectedOption?.text || 'Выберите...';

        this.trigger.innerHTML = `
            <span class="selected-value">${this.escapeHtml(selectedText)}</span>
            <span class="arrow">▼</span>
        `;

        this.dropdown = document.createElement('div');
        this.dropdown.className = 'custom-dropdown';
        this.dropdown.setAttribute('role', 'listbox');

        if (this.options.searchable && this.selectElement.options.length > 5) {
            this.addSearchInput();
        }

        this.populateOptions();

        this.wrapper.appendChild(this.trigger);
        this.wrapper.appendChild(this.dropdown);
        this.selectElement.parentNode.insertBefore(this.wrapper, this.selectElement.nextSibling);

        this.trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            e.preventDefault();
            this.toggleDropdown();
        });

        // Исправление: предотвращаем всплытие событий от wrapper к document
        this.wrapper.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Исправление: не закрываем dropdown при клике внутри него
        this.dropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        document.addEventListener('click', (e) => {
            if (this.preventClose) return;
            if (this.wrapper && !this.wrapper.contains(e.target) && this.isOpen && !this.isSelecting) {
                this.closeDropdown();
            }
        });

        this.selectElement.addEventListener('change', () => this.syncFromSelect());
        window.addEventListener('resize', () => this.closeDropdown());

        this.setupMobileOptimizations();
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    addSearchInput() {
        const searchDiv = document.createElement('div');
        searchDiv.className = 'custom-dropdown-search';
        searchDiv.innerHTML = '<input type="text" placeholder="Поиск..." autocomplete="off">';
        this.dropdown.appendChild(searchDiv);

        const searchInput = searchDiv.querySelector('input');

        // Предотвращаем закрытие при клике/тапе на поиск
        searchInput.addEventListener('mousedown', (e) => {
            e.stopPropagation();
            this.preventClose = true;
        });

        searchInput.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Исправление для мобильных: клавиатура не исчезает
        if (this.isMobile) {
            searchInput.addEventListener('touchstart', (e) => {
                e.stopPropagation();
                this.preventClose = true;
                this.keyboardOpen = true;
                // Не блокируем фокус, но предотвращаем закрытие
                setTimeout(() => {
                    if (this.isOpen && searchInput) {
                        searchInput.focus();
                    }
                }, 50);
            });

            searchInput.addEventListener('focus', (e) => {
                e.stopPropagation();
                this.preventClose = true;
                this.keyboardOpen = true;
                // Прокручиваем к поиску, чтобы он был виден при открытой клавиатуре
                setTimeout(() => {
                    if (this.isOpen && searchInput) {
                        searchInput.scrollIntoView({ block: 'center', behavior: 'smooth' });
                    }
                }, 100);
            });

            searchInput.addEventListener('blur', () => {
                setTimeout(() => {
                    this.keyboardOpen = false;
                    // Не закрываем dropdown сразу при потере фокуса
                    if (!this.isSelecting) {
                        setTimeout(() => {
                            if (!this.keyboardOpen && !this.isSelecting) {
                                this.preventClose = false;
                            }
                        }, 300);
                    }
                }, 200);
            });
        }

        searchInput.addEventListener('input', (e) => {
            e.stopPropagation();
            this.filterOptions(e.target.value);
        });
    }

    populateOptions() {
        let searchInput = null;
        const existingSearch = this.dropdown.querySelector('.custom-dropdown-search');
        if (existingSearch) {
            searchInput = existingSearch.querySelector('input');
            if (searchInput) {
                searchInput.value = '';
            }
        }

        const optionsToRemove = this.dropdown.querySelectorAll('.custom-dropdown-option');
        optionsToRemove.forEach(opt => opt.remove());

        for (let i = 0; i < this.selectElement.options.length; i++) {
            const option = this.selectElement.options[i];
            const optionDiv = document.createElement('div');
            optionDiv.className = 'custom-dropdown-option';
            if (option.selected) {
                optionDiv.classList.add('selected');
            }

            let displayText = option.text;
            if (displayText.length > 45) {
                displayText = displayText.substring(0, 42) + '...';
            }
            optionDiv.textContent = displayText;
            if (displayText !== option.text) {
                optionDiv.title = option.text;
            }
            optionDiv.dataset.value = option.value;
            optionDiv.dataset.index = i;

            optionDiv.addEventListener('click', (e) => {
                e.stopPropagation();
                this.isSelecting = true;
                this.preventClose = false;
                this.selectOption(parseInt(optionDiv.dataset.index));
                setTimeout(() => {
                    this.isSelecting = false;
                }, 300);
                this.closeDropdown();
            });

            // на мобильных предотвращаем прокрутку страницы при тапе
            if (this.isMobile) {
                optionDiv.addEventListener('touchstart', (e) => {
                    e.stopPropagation();
                }, { passive: false });
            }

            this.dropdown.appendChild(optionDiv);
        }
    }

    selectOption(index) {
        if (index === undefined || index === null) return;

        this.selectElement.selectedIndex = index;
        const selectedOption = this.selectElement.options[index];

        const selectedSpan = this.trigger.querySelector('.selected-value');
        let displayText = selectedOption.text;
        if (displayText.length > 45) {
            displayText = displayText.substring(0, 42) + '...';
        }
        selectedSpan.textContent = displayText;
        if (displayText !== selectedOption.text) {
            selectedSpan.title = selectedOption.text;
        }

        const options = this.dropdown.querySelectorAll('.custom-dropdown-option');
        options.forEach((opt, i) => {
            if (i === index) {
                opt.classList.add('selected');
            } else {
                opt.classList.remove('selected');
            }
        });

        const changeEvent = new Event('change', { bubbles: true });
        this.selectElement.dispatchEvent(changeEvent);
    }

    filterOptions(searchText) {
        const options = this.dropdown.querySelectorAll('.custom-dropdown-option');
        const lowerSearch = searchText.toLowerCase();

        options.forEach(option => {
            const text = option.textContent.toLowerCase();
            if (text.includes(lowerSearch)) {
                option.style.display = '';
            } else {
                option.style.display = 'none';
            }
        });
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
            }
        });

        this.positionDropdown();

        this.dropdown.classList.add('show');
        this.dropdown.style.display = 'block';
        this.trigger.classList.add('active');
        this.isOpen = true;
        this.preventClose = false;

        const searchInput = this.dropdown.querySelector('.custom-dropdown-search input');

        if (searchInput) {
            this.filterOptions('');
            searchInput.value = '';

            if (!this.isMobile) {
                setTimeout(() => {
                    if (this.isOpen) {
                        searchInput.focus();
                    }
                }, 100);
            } else {
                // На мобильных не фокусируем автоматически, ждём тапа пользователя
                // Это решает проблему с исчезающей клавиатурой
            }
        }

        const selected = this.dropdown.querySelector('.custom-dropdown-option.selected');
        if (selected) {
            selected.scrollIntoView({ block: 'nearest' });
        }

        if (this.isMobile) {
            this.scrollPosition = window.scrollY;
        }
    }

    positionDropdown() {
        const rect = this.trigger.getBoundingClientRect();
        const viewportHeight = window.innerHeight;
        const viewportWidth = window.innerWidth;

        this.dropdown.style.top = '';
        this.dropdown.style.bottom = '';
        this.dropdown.style.left = '';
        this.dropdown.style.right = '';

        const width = Math.min(rect.width, viewportWidth - 20);
        this.dropdown.style.width = width + 'px';
        this.dropdown.style.minWidth = Math.max(200, width) + 'px';

        const spaceBelow = viewportHeight - rect.bottom;
        const spaceAbove = rect.top;

        const hasSearch = this.dropdown.querySelector('.custom-dropdown-search') !== null;

        if (this.isMobile && hasSearch) {
            let topPosition = rect.height + 4;
            let maxHeight = Math.min(this.options.maxHeight, spaceBelow - 20);

            if (maxHeight < 150) {
                maxHeight = Math.max(120, spaceBelow - 30);
            }

            this.dropdown.style.top = topPosition + 'px';
            this.dropdown.style.maxHeight = Math.max(120, maxHeight) + 'px';
        }
        else if (spaceBelow >= this.options.maxHeight || spaceBelow > spaceAbove) {
            let topPosition = rect.height + 4;
            let maxHeight = Math.min(this.options.maxHeight, spaceBelow - 10);
            this.dropdown.style.top = topPosition + 'px';
            this.dropdown.style.maxHeight = Math.max(120, maxHeight) + 'px';
        } else {
            let topPosition = -Math.min(this.options.maxHeight, spaceAbove - 10) - 4;
            let maxHeight = Math.min(this.options.maxHeight, spaceAbove - 10);
            this.dropdown.style.top = topPosition + 'px';
            this.dropdown.style.maxHeight = Math.max(120, maxHeight) + 'px';
        }

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
        if (this.preventClose) return;

        if (this.closeTimeout) {
            clearTimeout(this.closeTimeout);
        }

        this.closeTimeout = setTimeout(() => {
            this.dropdown.classList.remove('show');
            this.dropdown.style.display = 'none';
            this.trigger.classList.remove('active');
            this.isOpen = false;
            this.closeTimeout = null;
            this.keyboardOpen = false;

            if (this.isMobile && this.scrollPosition !== undefined) {
                // Исправление: не скроллим обратно, чтобы не мешать пользователю
                // window.scrollTo(0, this.scrollPosition);
                this.scrollPosition = undefined;
            }
        }, this.isMobile ? 200 : 200);
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
        if (!this.isMobile) {
            return;
        }

        //  предотвращаем прокрутку страницы при скролле внутри dropdown
        this.dropdown.addEventListener('touchstart', (e) => {
            this.preventClose = true;
        }, { passive: true });

        this.dropdown.addEventListener('touchend', (e) => {
            setTimeout(() => {
                if (!this.keyboardOpen) {
                    this.preventClose = false;
                }
            }, 300);
        });

        this.dropdown.addEventListener('touchmove', (e) => {
            const scrollTop = this.dropdown.scrollTop;
            const scrollHeight = this.dropdown.scrollHeight;
            const height = this.dropdown.clientHeight;

            if (scrollHeight > height) {
                e.stopPropagation();
            }
        }, { passive: false });

        const searchInput = this.dropdown.querySelector('.custom-dropdown-search input');
        if (searchInput) {
            searchInput.addEventListener('focus', () => {
                this.preventClose = true;
                this.keyboardOpen = true;
                if (this.closeTimeout) {
                    clearTimeout(this.closeTimeout);
                    this.closeTimeout = null;
                }
                this.scrollPosition = window.scrollY;
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

        this.dropdown.addEventListener('click', (e) => {
            e.stopPropagation();
            this.preventClose = true;
            setTimeout(() => {
                if (this.isOpen && !this.keyboardOpen) {
                    this.preventClose = false;
                }
            }, 300);
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
        this.selectElement.removeAttribute('data-custom-init');
    }
}

// Инициализация всех select на странице
document.addEventListener('DOMContentLoaded', function() {
    let initAttempts = 0;
    const maxAttempts = 20;
    let citySelectInitialized = false;

    function tryInitRegion() {
        const regionSelect = document.getElementById('regionSelect');
        if (regionSelect && regionSelect.options && regionSelect.options.length > 1 && !regionSelect.hasAttribute('data-custom-init')) {
            new CustomSelect(regionSelect, { searchable: false, maxHeight: 250 });
            regionSelect.setAttribute('data-custom-init', 'true');
            return true;
        }
        return false;
    }

    function tryInitCity() {
        const citySelect = document.getElementById('citySelect');
        if (citySelect && citySelect.options && citySelect.options.length > 1 && !citySelect.hasAttribute('data-custom-init')) {
            const optionsCount = Array.from(citySelect.options).length;
            new CustomSelect(citySelect, { searchable: optionsCount > 8, maxHeight: 280 });
            citySelect.setAttribute('data-custom-init', 'true');
            citySelectInitialized = true;
            return true;
        }
        return false;
    }

    tryInitRegion();

    const observer = new MutationObserver(function(mutations) {
        let changed = false;
        if (tryInitRegion()) changed = true;
        if (tryInitCity()) changed = true;

        if (changed) {
            initAttempts = 0;
        } else if (initAttempts < maxAttempts) {
            initAttempts++;
        } else {
            observer.disconnect();
            clearInterval(interval);
        }
    });

    observer.observe(document.body, { childList: true, subtree: true });

    const interval = setInterval(() => {
        let changed = false;
        if (tryInitRegion()) changed = true;
        if (tryInitCity()) changed = true;

        if (changed) {
            initAttempts = 0;
        } else if (initAttempts >= maxAttempts) {
            clearInterval(interval);
        } else {
            initAttempts++;
        }
    }, 300);

    setTimeout(() => {
        clearInterval(interval);
        observer.disconnect();
    }, 10000);
});