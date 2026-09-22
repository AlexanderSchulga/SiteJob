// ====== УПРАВЛЕНИЕ ОТОБРАЖЕНИЕМ ПОДКАТЕГОРИЙ ======
document.addEventListener('DOMContentLoaded', function() {
    const categorySelect = document.getElementById('category');
    if (categorySelect) {
        // Получаем все элементы подкатегорий
        const realtyFields = document.getElementById('realtyFields');
        const motorFields = document.getElementById('motorFields');
        const autoFields = document.getElementById('autoFields');
        const constructionsFields = document.getElementById('constructionsFields');
        const interiorFields = document.getElementById('interiorFields');
        const toolsFields = document.getElementById('toolsFields');
        const appliancesFields = document.getElementById('appliancesFields');
        const electronicsFields = document.getElementById('electronicsFields');
        const clothingFields = document.getElementById('clothingFields');
        const childrenPrFields = document.getElementById('childrenPrFields');
        const sportsFields = document.getElementById('sportsFields');
        const foodFields = document.getElementById('foodFields');
        const healthFields = document.getElementById('healthFields');
        const animalsFields = document.getElementById('animalsFields');

        function toggleSubcategories() {
            const category = categorySelect.value;

            // Скрываем все
            if (realtyFields) realtyFields.style.display = 'none';
            if (motorFields) motorFields.style.display = 'none';
            if (autoFields) autoFields.style.display = 'none';
            if (constructionsFields) constructionsFields.style.display = 'none';
            if (interiorFields) interiorFields.style.display = 'none';
            if (toolsFields) toolsFields.style.display = 'none';
            if (appliancesFields) appliancesFields.style.display = 'none';
            if (electronicsFields) electronicsFields.style.display = 'none';
            if (clothingFields) clothingFields.style.display = 'none';
            if (childrenPrFields) childrenPrFields.style.display = 'none';
            if (sportsFields) sportsFields.style.display = 'none';
            if (foodFields) foodFields.style.display = 'none';
            if (healthFields) healthFields.style.display = 'none';
            if (animalsFields) animalsFields.style.display = 'none';

            // Показываем нужную категорию
            switch (category) {
                case 'realty':
                    if (realtyFields) realtyFields.style.display = 'block';
                    break;
                case 'motor':
                    if (motorFields) motorFields.style.display = 'block';
                    break;
                case 'auto':
                    if (autoFields) autoFields.style.display = 'block';
                    break;
                case 'constructions':
                    if (constructionsFields) constructionsFields.style.display = 'block';
                    break;
                case 'interior':
                    if (interiorFields) interiorFields.style.display = 'block';
                    break;
                case 'tools':
                    if (toolsFields) toolsFields.style.display = 'block';
                    break;
                case 'appliances':
                    if (appliancesFields) appliancesFields.style.display = 'block';
                    break;
                case 'electronics':
                    if (electronicsFields) electronicsFields.style.display = 'block';
                    break;
                case 'clothing':
                    if (clothingFields) clothingFields.style.display = 'block';
                    break;
                case 'childrenPr':
                    if (childrenPrFields) childrenPrFields.style.display = 'block';
                    break;
                case 'sports':
                    if (sportsFields) sportsFields.style.display = 'block';
                    break;
                case 'food':
                    if (foodFields) foodFields.style.display = 'block';
                    break;
                case 'health':
                    if (healthFields) healthFields.style.display = 'block';
                    break;
                case 'animals':
                    if (animalsFields) animalsFields.style.display = 'block';
                    break;
                default:
                    // Если категория не выбрана или неизвестна - ничего не показываем
                    break;
            }

            setTimeout(function() {
                if (typeof initFilterSelects === 'function') {
                    initFilterSelects();
                }
            }, 100);
        }

        categorySelect.addEventListener('change', toggleSubcategories);
        document.addEventListener('categoryChanged', function(e) {
            if (e.detail && e.detail.value) {
                categorySelect.value = e.detail.value;
                toggleSubcategories();
            }
        });
        setTimeout(toggleSubcategories, 100);
    }

    // ====== УПРАВЛЕНИЕ ТИПАМИ НЕДВИЖИМОСТИ ======
    const propertyTypeSelect = document.getElementById('propertyType');
    if (propertyTypeSelect) {
        function toggleRealtySubtypes() {
            const type = propertyTypeSelect.value;
            const apartmentFields = document.getElementById('apartmentFields');
            const houseFields = document.getElementById('houseFields');
            const landFields = document.getElementById('landFields');
            const garageFields = document.getElementById('garageFields');
            const commercialFields = document.getElementById('commercialFields');

            // Скрываем все
            if (apartmentFields) apartmentFields.style.display = 'none';
            if (houseFields) houseFields.style.display = 'none';
            if (landFields) landFields.style.display = 'none';
            if (garageFields) garageFields.style.display = 'none';
            if (commercialFields) commercialFields.style.display = 'none';

            // Отключаем все поля
            document.querySelectorAll(
                '#apartmentFields input, #apartmentFields select, ' +
                '#houseFields input, #houseFields select, ' +
                '#landFields input, #landFields select, ' +
                '#garageFields input, #garageFields select, ' +
                '#commercialFields input, #commercialFields select'
            ).forEach(el => el.disabled = true);

            // Показываем нужный тип
            switch (type) {
                case 'apartment':
                    if (apartmentFields) {
                        apartmentFields.style.display = 'flex';
                        apartmentFields.querySelectorAll('input, select').forEach(el => el.disabled = false);
                    }
                    break;
                case 'house':
                    if (houseFields) {
                        houseFields.style.display = 'flex';
                        houseFields.querySelectorAll('input, select').forEach(el => el.disabled = false);
                    }
                    break;
                case 'land':
                    if (landFields) {
                        landFields.style.display = 'flex';
                        landFields.querySelectorAll('input, select').forEach(el => el.disabled = false);
                    }
                    break;
                case 'garage':
                    if (garageFields) {
                        garageFields.style.display = 'flex';
                        garageFields.querySelectorAll('input, select').forEach(el => el.disabled = false);
                    }
                    break;
                case 'commercial':
                    if (commercialFields) {
                        commercialFields.style.display = 'flex';
                        commercialFields.querySelectorAll('input, select').forEach(el => el.disabled = false);
                    }
                    break;
                default:
                    break;
            }
        }

        propertyTypeSelect.addEventListener('change', toggleRealtySubtypes);
        setTimeout(toggleRealtySubtypes, 100);
    }
});