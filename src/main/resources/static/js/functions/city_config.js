let citiesByRegion = {};

// Загружаем города при старте
const citiesLoadPromise = fetch('/geo/cities.json')
    .then(response => response.json())
    .then(data => {
        citiesByRegion = data;
        console.log('Cities loaded:', Object.keys(citiesByRegion).length, 'regions');
        updateCities();
        return true;
    })
    .catch(error => {
        return false;
    });

// Глобальная переменная для хранения экземпляра кастомного селекта города
let cityCustomSelectInstance = null;

function updateCities() {
    const regionSelect = document.getElementById('regionSelect');
    const regionValue = regionSelect ? regionSelect.value : '';
    const cityContainer = document.getElementById('cityContainer');

    if (!cityContainer) return;

    // Уничтожаем существующий экземпляр кастомного селекта
    if (cityCustomSelectInstance) {
        try {
            cityCustomSelectInstance.destroy();
        } catch(e) {}
        cityCustomSelectInstance = null;
    }

    if (regionValue === 'other' || !regionValue || regionValue === '') {
        cityContainer.innerHTML = `
            <input type="text" name="city" id="cityManualInput" class="form-control" 
                   placeholder="Введите город или населенный пункт"
                   autocomplete="off">
            <div class="form-text">Необязательное поле. Можно оставить пустым</div>
        `;
        // Восстанавливаем сохранённое значение
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

    // Создаём select
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
        // Восстанавливаем выбранное значение
        const savedCity = document.getElementById('selectedCityValue');
        if (savedCity && savedCity.value && savedCity.value !== '') {
            for (let i = 0; i < citySelect.options.length; i++) {
                if (citySelect.options[i].value === savedCity.value) {
                    citySelect.selectedIndex = i;
                    break;
                }
            }
        }

        // Обработчик смены значения для ручного ввода
        citySelect.addEventListener('change', function(e) {
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
                    if (savedVal && savedVal.value) {
                        const manualInput = document.getElementById('cityManualInput');
                        if (manualInput) manualInput.value = savedVal.value;
                    }
                }
            }
        });

        // Инициализируем кастомный select с задержкой
        setTimeout(() => {
            if (citySelect && citySelect.options.length > 1 && !citySelect.hasAttribute('data-custom-init')) {
                // Включаем поиск только если городов больше 8
                const useSearch = sortedCities.length > 8;
                cityCustomSelectInstance = new CustomSelect(citySelect, {
                    searchable: useSearch,
                    maxHeight: 280
                });
                citySelect.setAttribute('data-custom-init', 'true');
            }
        }, 150);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const regionSelect = document.getElementById('regionSelect');
    if (regionSelect) {
        regionSelect.addEventListener('change', function() {
            // При смене региона сбрасываем инициализацию города
            if (cityCustomSelectInstance) {
                try {
                    cityCustomSelectInstance.destroy();
                } catch(e) {}
                cityCustomSelectInstance = null;
            }

            const citySelect = document.getElementById('citySelect');
            if (citySelect) {
                citySelect.removeAttribute('data-custom-init');
            }

            if (Object.keys(citiesByRegion).length === 0) {
                citiesLoadPromise.then(() => updateCities());
            } else {
                updateCities();
            }
        });

        if (Object.keys(citiesByRegion).length === 0) {
            citiesLoadPromise.then(() => updateCities());
        } else {
            updateCities();
        }
    }
});