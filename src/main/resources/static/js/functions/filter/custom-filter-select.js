/**
 * Кастомный выпадающий список для фильтров подкатегорий
 */
class CustomFilterSelect {
    constructor(selectElement, options = {}) {
        this.selectElement = selectElement;
        this.options = {
            searchable: options.searchable || false,
            maxHeight: options.maxHeight || 250,
            ...options
        };
        this.wrapper = null;
        this.trigger = null;
        this.dropdown = null;
        this.isOpen = false;
        this.closeTimeout = null;
        this.isSelecting = false;

        this.init();
    }

    init() {
        this.selectElement.style.display = 'none';
        this.selectElement.setAttribute('data-custom-filter', 'true');

        this.wrapper = document.createElement('div');
        this.wrapper.className = 'custom-filter-select-wrapper';
        this.wrapper.style.position = 'relative';
        this.wrapper.style.width = '100%';
        this.wrapper.style.overflow = 'visible';

        this.trigger = document.createElement('div');
        this.trigger.className = 'custom-filter-select-trigger';
        this.trigger.setAttribute('role', 'button');
        this.trigger.setAttribute('tabindex', '0');

        const selectedOption = this.selectElement.options[this.selectElement.selectedIndex];
        const selectedText = selectedOption?.text || 'Выберите...';

        this.trigger.innerHTML = `
            <span class="filter-selected-value">${this.escapeHtml(selectedText)}</span>
            <span class="filter-dropdown-arrow">▼</span>
        `;

        this.dropdown = document.createElement('div');
        this.dropdown.className = 'custom-filter-dropdown';
        this.dropdown.setAttribute('role', 'listbox');

        this.populateOptions();

        this.wrapper.appendChild(this.trigger);
        this.wrapper.appendChild(this.dropdown);
        this.selectElement.parentNode.insertBefore(this.wrapper, this.selectElement.nextSibling);

        this.trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            e.preventDefault();
            this.toggleDropdown();
        });

        document.addEventListener('click', (e) => {
            if (this.wrapper && !this.wrapper.contains(e.target) && this.isOpen && !this.isSelecting) {
                this.closeDropdown();
            }
        });

        this.selectElement.addEventListener('change', () => this.syncFromSelect());

        // Пересчитываем позицию при скролле
        document.addEventListener('scroll', () => {
            if (this.isOpen) {
                this.positionDropdown();
            }
        });
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    populateOptions() {
        this.dropdown.querySelectorAll('.custom-filter-option').forEach(opt => opt.remove());

        for (let i = 0; i < this.selectElement.options.length; i++) {
            const option = this.selectElement.options[i];
            const optionDiv = document.createElement('div');
            optionDiv.className = 'custom-filter-option';
            if (option.selected) optionDiv.classList.add('selected');

            let displayText = option.text;
            if (displayText.length > 40) {
                displayText = displayText.substring(0, 37) + '...';
            }
            optionDiv.textContent = displayText;
            if (displayText !== option.text) optionDiv.title = option.text;
            optionDiv.dataset.value = option.value;
            optionDiv.dataset.index = i;

            // Убираем любые радиокнопки или чекбоксы
            optionDiv.setAttribute('role', 'option');
            optionDiv.setAttribute('aria-selected', option.selected ? 'true' : 'false');

            optionDiv.addEventListener('click', (e) => {
                e.stopPropagation();
                this.isSelecting = true;
                this.selectOption(parseInt(optionDiv.dataset.index));
                setTimeout(() => {
                    this.isSelecting = false;
                }, 300);
                this.closeDropdown();
            });

            this.dropdown.appendChild(optionDiv);
        }
    }

    selectOption(index) {
        if (index === undefined || index === null) return;

        this.selectElement.selectedIndex = index;
        const selectedOption = this.selectElement.options[index];

        const selectedSpan = this.trigger.querySelector('.filter-selected-value');
        let displayText = selectedOption.text;
        if (displayText.length > 40) {
            displayText = displayText.substring(0, 37) + '...';
        }
        selectedSpan.textContent = displayText;
        if (displayText !== selectedOption.text) {
            selectedSpan.title = selectedOption.text;
        }

        this.dropdown.querySelectorAll('.custom-filter-option').forEach((opt, i) => {
            opt.classList.toggle('selected', i === index);
        });

        this.selectElement.dispatchEvent(new Event('change', { bubbles: true }));
    }

    toggleDropdown() {
        this.isOpen ? this.closeDropdown() : this.openDropdown();
    }

    openDropdown() {
        if (this.closeTimeout) {
            clearTimeout(this.closeTimeout);
            this.closeTimeout = null;
        }

        // Закрываем другие открытые dropdowns
        document.querySelectorAll('.custom-filter-dropdown.show').forEach(d => {
            const parent = d.parentElement;
            if (parent && parent !== this.wrapper) {
                const otherTrigger = parent.querySelector('.custom-filter-select-trigger');
                if (otherTrigger) otherTrigger.classList.remove('active');
                d.classList.remove('show');
                d.style.display = 'none';
            }
        });

        this.positionDropdown();

        this.dropdown.classList.add('show');
        this.dropdown.style.display = 'block';
        this.dropdown.style.zIndex = '99999';
        this.trigger.classList.add('active');
        this.isOpen = true;

        const selected = this.dropdown.querySelector('.custom-filter-option.selected');
        if (selected) selected.scrollIntoView({ block: 'nearest' });
    }

    positionDropdown() {
        const rect = this.trigger.getBoundingClientRect();
        const viewportHeight = window.innerHeight;
        const viewportWidth = window.innerWidth;

        // Сбрасываем предыдущие позиции
        this.dropdown.style.top = '';
        this.dropdown.style.bottom = '';
        this.dropdown.style.left = '';
        this.dropdown.style.right = '';
        this.dropdown.style.maxHeight = '';

        // Ширина dropdown
        const width = Math.min(rect.width, viewportWidth - 20);
        this.dropdown.style.width = width + 'px';
        this.dropdown.style.minWidth = Math.max(180, width) + 'px';

        const spaceBelow = viewportHeight - rect.bottom;
        const spaceAbove = rect.top;
        const optionHeight = 40; // примерная высота одного пункта
        const estimatedHeight = Math.min(this.options.maxHeight, this.selectElement.options.length * optionHeight);
        const needHeight = Math.min(estimatedHeight + 10, this.options.maxHeight);

        // Умное позиционирование: если снизу мало места — открываем вверх
        if (spaceBelow >= needHeight || spaceBelow > spaceAbove) {
            // Открываем вниз
            this.dropdown.style.top = (rect.height + 4) + 'px';
            this.dropdown.style.bottom = 'auto';
            this.dropdown.style.maxHeight = Math.max(120, Math.min(this.options.maxHeight, spaceBelow - 10)) + 'px';
        } else {
            // Открываем вверх
            this.dropdown.style.top = 'auto';
            this.dropdown.style.bottom = (rect.height + 4) + 'px';
            this.dropdown.style.maxHeight = Math.max(120, Math.min(this.options.maxHeight, spaceAbove - 10)) + 'px';
        }

        // Горизонтальное позиционирование
        this.dropdown.style.left = '0';
        this.dropdown.style.right = '0';

        // Убеждаемся что dropdown не выходит за экран
        const dropdownWidth = this.dropdown.offsetWidth || width;
        if (rect.left + dropdownWidth > viewportWidth - 10) {
            this.dropdown.style.left = 'auto';
            this.dropdown.style.right = '0';
        }
        if (rect.left < 10) {
            this.dropdown.style.left = '0';
            this.dropdown.style.right = 'auto';
        }
    }

    closeDropdown() {
        if (this.closeTimeout) clearTimeout(this.closeTimeout);

        this.closeTimeout = setTimeout(() => {
            this.dropdown.classList.remove('show');
            this.dropdown.style.display = 'none';
            this.trigger.classList.remove('active');
            this.isOpen = false;
            this.closeTimeout = null;
        }, 200);
    }

    syncFromSelect() {
        const selectedIndex = this.selectElement.selectedIndex;
        const selectedOption = this.selectElement.options[selectedIndex];
        if (selectedOption) {
            const selectedSpan = this.trigger.querySelector('.filter-selected-value');
            let displayText = selectedOption.text;
            if (displayText.length > 40) {
                displayText = displayText.substring(0, 37) + '...';
            }
            selectedSpan.textContent = displayText;

            this.dropdown.querySelectorAll('.custom-filter-option').forEach((opt, i) => {
                opt.classList.toggle('selected', i === selectedIndex);
            });
        }
    }
}

// Инициализация
let filterSelectsInitialized = false;
let filterSelectsInstances = [];

function initFilterSelects() {
    const selectors = [
        '.filter-group select:not([data-custom-filter])',
        '#realtyFieldsEdit select:not([data-custom-filter])',
        '#vehicleFieldsEdit select:not([data-custom-filter])',
        '#autoFieldsEdit select:not([data-custom-filter])',
        '#constructionsFieldsEdit select:not([data-custom-filter])',
        '#interiorFieldsEdit select:not([data-custom-filter])',
        '#toolsFieldsEdit select:not([data-custom-filter])',
        '#appliancesFieldsEdit select:not([data-custom-filter])',
        '#electronicsFieldsEdit select:not([data-custom-filter])',
        '#clothingFieldsEdit select:not([data-custom-filter])',
        '#childrenPrFieldsEdit select:not([data-custom-filter])',
        '#sportsFieldsEdit select:not([data-custom-filter])',
        '#foodFieldsEdit select:not([data-custom-filter])',
        '#healthFieldsEdit select:not([data-custom-filter])',
        '#animalsFieldsEdit select:not([data-custom-filter])'
    ];

    document.querySelectorAll(selectors.join(',')).forEach(select => {
        // Пропускаем вакансии/резюме и модалки — там свои кастомные селекты
        if (select.closest('#experienceSelect, #workformatSelect, .modal')) return;

        // Пропускаем скрытые подтипы realty (они readonly и не переключаются)
        const hiddenParent = select.closest(
            '#apartmentFieldsEdit, #houseFieldsEdit, #landFieldsEdit, #garageFieldsEdit, #commercialFieldsEdit'
        );
        if (hiddenParent && hiddenParent.style.display === 'none') return;

        if (select.hasAttribute('data-custom-filter')) return;

        const instance = new CustomFilterSelect(select, { maxHeight: 250 });
        filterSelectsInstances.push(instance);
    });

    filterSelectsInitialized = true;
}

// Принудительная переинициализация с задержкой
function forceInitFilterSelects() {
    // Удаляем старые обертки
    document.querySelectorAll('.custom-filter-select-wrapper').forEach(w => w.remove());

    // Снимаем атрибуты с select
    document.querySelectorAll('select[data-custom-filter]').forEach(el => {
        el.removeAttribute('data-custom-filter');
        el.style.display = '';
    });

    // Заново инициализируем
    setTimeout(initFilterSelects, 50);
}

// Запускаем при загрузке
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(initFilterSelects, 100);

    // Также запускаем после полной загрузки всех ресурсов
    window.addEventListener('load', function() {
        setTimeout(initFilterSelects, 200);
    });
});

document.addEventListener('DOMContentLoaded', function() {
    setTimeout(initFilterSelects, 100);

    // Следим за появлением новых select в фильтрах
    const observer = new MutationObserver(function(mutations) {
        let needsInit = false;
        for (const mutation of mutations) {
            if (mutation.addedNodes.length) {
                for (const node of mutation.addedNodes) {
                    if (node.nodeType === 1) {
                        const selects = node.querySelectorAll ? node.querySelectorAll('.filter-group select:not([data-custom-filter])') : [];
                        if (selects.length > 0) {
                            needsInit = true;
                            break;
                        }
                    }
                }
            }
            if (needsInit) break;
        }
        if (needsInit) {
            setTimeout(initFilterSelects, 50);
        }
    });

    observer.observe(document.body, { childList: true, subtree: true });

    // Автоотключение через 10 секунд
    setTimeout(() => {
        observer.disconnect();
    }, 10000);
});