let regionsData = {};

// Загружаем регионы при старте
const regionsLoadPromise = fetch('/geo/regions.json')
    .then(response => response.json())
    .then(data => {
        regionsData = data;
        populateRegionSelect();
        return true;
    })
    .catch(() => {
        return false;
    });

function populateRegionSelect() {
    const regionSelect = document.getElementById('regionSelect');
    if (!regionSelect) return;

    // Сохраняем выбранное значение из скрытого поля
    const selectedValueInput = document.getElementById('selectedRegionValue');
    const selectedValue = selectedValueInput ? selectedValueInput.value : '';

    // Очищаем select, оставляя только первый placeholder
    regionSelect.innerHTML = '<option value="" disabled>Выберите регион</option>';

    // Добавляем опции из загруженных данных
    for (const [key, name] of Object.entries(regionsData)) {
        const option = document.createElement('option');
        option.value = key;
        // Укорачиваем длинные названия для мобильных
        const displayName = name.length > 45 ? name.substring(0, 42) + '...' : name;
        option.textContent = displayName;
        if (displayName !== name) {
            option.title = name;
        }
        regionSelect.appendChild(option);
    }

    // Восстанавливаем выбранное значение
    if (selectedValue && selectedValue !== '') {
        regionSelect.value = selectedValue;
    }

    // Инициализируем кастомный select для региона
    if (regionSelect.options.length > 1 && !regionSelect.hasAttribute('data-custom-init')) {
        // Небольшая задержка для корректной инициализации
        setTimeout(() => {
            if (regionSelect && !regionSelect.hasAttribute('data-custom-init')) {
                if (typeof CustomSelect !== 'undefined') {
                    new CustomSelect(regionSelect, { searchable: false });
                    regionSelect.setAttribute('data-custom-init', 'true');

                    // После инициализации кастомного селекта, если есть выбранное значение - обновляем отображение
                    if (selectedValue && selectedValue !== '') {
                        // Триггерим событие change для обновления кастомного селекта
                        regionSelect.dispatchEvent(new Event('change'));
                    }
                }
            }
        }, 100);
    }
}

function updateCities() {
    const regionSelect = document.getElementById('regionSelect');
    // Получаем актуальное значение из native select (не из кастомного)
    const regionValue = regionSelect ? regionSelect.value : '';
    const cityContainer = document.getElementById('cityContainer');

    if (!cityContainer) return;

    if (regionValue === 'other' || !regionValue || regionValue === '') {
        cityContainer.innerHTML = `
            <input type="text" name="city" id="cityManualInput" class="form-control" 
                   placeholder="Введите город или населенный пункт"
                   autocomplete="off">
            <div class="form-text">Необязательное поле. Можно оставить пустым</div>
        `;
        const savedCity = document.getElementById('selectedCityValue');
        if (savedCity && savedCity.value && savedCity.value !== '') {
            const manualInput = document.getElementById('cityManualInput');
            if (manualInput) manualInput.value = savedCity.value;
        }
        return;
    }

    // Проверяем, загружены ли данные городов
    if (!citiesByRegion || Object.keys(citiesByRegion).length === 0) {
        cityContainer.innerHTML = `
            <select name="city" id="citySelect" class="form-select" disabled>
                <option value="">Загрузка городов...</option>
            </select>
            <div class="form-text">Необязательное поле</div>
        `;
        if (window.citiesLoadPromise) {
            window.citiesLoadPromise.then(() => updateCities());
        }
        return;
    }

    const cities = citiesByRegion[regionValue] || [];
    const sortedCities = [...cities].sort((a, b) => a.localeCompare(b, 'ru'));

    let html = '<select name="city" id="citySelect" class="form-select" style="width:100%;">';
    html += '<option value="">Выберите город (необязательно)</option>';

    sortedCities.forEach(city => {
        const escapedCity = city.replace(/'/g, "\\'").replace(/"/g, '&quot;');
        const displayCity = city.length > 40 ? city.substring(0, 37) + '...' : city;
        html += `<option value="${escapedCity}" title="${city}">${displayCity}</option>`;
    });

    html += '<option value="manual_input">✏️ Ввести вручную</option>';
    html += '</select>';
    html += '<div class="form-text">Необязательное поле. Если вашего города нет в списке, выберите "Ввести вручную"</div>';

    cityContainer.innerHTML = html;

    const citySelect = document.getElementById('citySelect');
    if (citySelect) {
        const savedCity = document.getElementById('selectedCityValue');
        if (savedCity && savedCity.value && savedCity.value !== '') {
            let cityFound = false;
            for (let i = 0; i < citySelect.options.length; i++) {
                if (citySelect.options[i].value === savedCity.value) {
                    citySelect.selectedIndex = i;
                    cityFound = true;
                    break;
                }
            }
            // Если город не найден в списке и это не 'manual_input'
            if (!cityFound && savedCity.value !== 'manual_input' && savedCity.value !== '') {
                // Проверяем, есть ли опция 'manual_input'
                const manualOption = Array.from(citySelect.options).find(opt => opt.value === 'manual_input');
                if (manualOption) {
                    citySelect.value = 'manual_input';
                    // Триггерим событие change для отображения поля ручного ввода
                    citySelect.dispatchEvent(new Event('change'));
                    // Устанавливаем значение в поле ручного ввода после его появления
                    setTimeout(() => {
                        const manualInput = document.getElementById('cityManualInput');
                        if (manualInput) manualInput.value = savedCity.value;
                    }, 50);
                }
            }
        }

        citySelect.addEventListener('change', function() {
            if (this.value === 'manual_input') {
                const currentContainer = document.getElementById('cityContainer');
                if (currentContainer) {
                    const savedVal = document.getElementById('selectedCityValue');
                    currentContainer.innerHTML = `
                        <input type="text" name="city" id="cityManualInput" class="form-control" 
                               placeholder="Введите город или населенный пункт"
                               autocomplete="off">
                        <div class="form-text">Необязательное поле. Можно оставить пустым</div>
                    `;
                    if (savedVal && savedVal.value && savedVal.value !== 'manual_input') {
                        const manualInput = document.getElementById('cityManualInput');
                        if (manualInput) manualInput.value = savedVal.value;
                    }
                }
            }
        });

        // Инициализируем кастомный select
        setTimeout(() => {
            if (citySelect && citySelect.options.length > 1 && !citySelect.hasAttribute('data-custom-init')) {
                if (typeof CustomSelect !== 'undefined') {
                    new CustomSelect(citySelect, { searchable: sortedCities.length > 8 });
                    citySelect.setAttribute('data-custom-init', 'true');
                }
            }
        }, 150);
    }
}

// Функция для принудительного обновления региона после загрузки всех данных
function initializeRegionAndCity() {
    const selectedRegion = document.getElementById('selectedRegionValue');
    const regionSelect = document.getElementById('regionSelect');

    if (selectedRegion && selectedRegion.value && selectedRegion.value !== '' && regionSelect) {
        // Ждем, пока регионы загрузятся
        if (Object.keys(regionsData).length === 0) {
            regionsLoadPromise.then(() => {
                setRegionValue(selectedRegion.value);
            });
        } else {
            setRegionValue(selectedRegion.value);
        }
    }
}

function setRegionValue(regionValue) {
    const regionSelect = document.getElementById('regionSelect');
    if (!regionSelect) return;

    for (let i = 0; i < regionSelect.options.length; i++) {
        if (regionSelect.options[i].value === regionValue) {
            regionSelect.selectedIndex = i;
            // Триггерим событие change для обновления кастомного селекта и городов
            regionSelect.dispatchEvent(new Event('change'));
            // Обновляем города
            if (typeof updateCities === 'function') {
                setTimeout(() => updateCities(), 100);
            }
            break;
        }
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    const regionSelect = document.getElementById('regionSelect');
    if (regionSelect) {
        // Загружаем регионы
        if (typeof regionsLoadPromise !== 'undefined') {
            regionsLoadPromise.then(() => {
                populateRegionSelect();
                // После заполнения регионов, инициализируем выбор
                initializeRegionAndCity();
                if (typeof citiesLoadPromise !== 'undefined') {
                    citiesLoadPromise.then(() => updateCities());
                } else {
                    updateCities();
                }
            });
        } else {
            fetch('/geo/regions.json')
                .then(response => response.json())
                .then(data => {
                    regionsData = data;
                    populateRegionSelect();
                    initializeRegionAndCity();
                    return typeof citiesLoadPromise !== 'undefined' ? citiesLoadPromise : Promise.resolve();
                })
                .then(() => updateCities())
                .catch(() => {});
        }

        // Обработчик смены региона
        regionSelect.addEventListener('change', function() {
            if (typeof citiesLoadPromise !== 'undefined') {
                citiesLoadPromise.then(() => updateCities());
            } else {
                updateCities();
            }
        });
    }
});