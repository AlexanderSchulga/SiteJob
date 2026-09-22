document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form[action*="/page_two/viewstwo"]');

    if (!form) return;

    // ===== ЗНАЧЕНИЯ-ЗАГЛУШКИ, КОТОРЫЕ НЕ ДОЛЖНЫ УХОДИТЬ В БАЗУ =====
    const PLACEHOLDER_VALUES = [
        '', null, undefined,
        'Выберите...', 'Выберите категорию...',
        'Выберите тип', 'Выберите вид', 'Выберите регион',
        'Выберите тип транспорта',
        'Все типы', 'Все виды', 'Все страны', 'Все товары',
        'Любой', 'Любое', 'Любая', 'Любое количество',
        'Не выбрано', 'Не выбран',
        'all', 'все'
    ];

    function isValidValue(value) {
        if (!value || value === '') return false;
        const trimmed = String(value).trim().toLowerCase();
        if (PLACEHOLDER_VALUES.some(v => trimmed === String(v).toLowerCase())) return false;
        if (trimmed.match(/^[-—]+$/)) return false;
        return true;
    }

    // ===== ПРОВЕРКА SELECT (учитывает первую опцию-заглушку) =====
    function isSelectValid(selectElement) {
        if (!selectElement) return false;
        if (selectElement.tagName !== 'SELECT') return isValidValue(selectElement.value);

        const selectedIndex = selectElement.selectedIndex;
        if (selectedIndex === -1) return false;

        const selectedOption = selectElement.options[selectedIndex];
        if (!selectedOption.value || selectedOption.value === '') return false;

        const optionText = selectedOption.text.toLowerCase();
        const placeholderTexts = [
            'выберите', 'все', 'любой', 'любое', 'любая',
            'не выбрано', 'не выбран', 'все типы', 'все виды',
            'все страны', 'все товары'
        ];

        if (placeholderTexts.some(text => optionText.includes(text))) {
            return false;
        }

        return isValidValue(selectedOption.value);
    }

    // ===== ОБРАБОТЧИК ОТПРАВКИ =====
    form.addEventListener('submit', function(e) {
        e.preventDefault();

        const categorySelect = document.getElementById('category');
        const category = categorySelect ? categorySelect.value : '';

        clearErrors();

        let isValid = true;

        // === ПРОВЕРКА ОБЯЗАТЕЛЬНЫХ ПОЛЕЙ ===
        const title = document.getElementById('title');
        if (!title || !isValidValue(title.value)) {
            showError(title, 'Пожалуйста, заполните название товара');
            isValid = false;
        }

        const price = document.getElementById('price');
        if (!price || !price.value || parseInt(price.value) <= 0) {
            showError(price, 'Укажите корректную цену (больше 0)');
            isValid = false;
        }

        const location = document.getElementById('locationInput');
        if (!location || !isValidValue(location.value)) {
            showError(location, 'Укажите местоположение');
            isValid = false;
        }

        const files = document.getElementById('files');
        if (!files || files.files.length === 0) {
            showError(files, 'Загрузите хотя бы одно изображение');
            isValid = false;
        }

        if (!isValidValue(category)) {
            showError(categorySelect, 'Выберите категорию товара');
            isValid = false;
        }

        // === ПРОВЕРКА ПОЛЕЙ КАТЕГОРИИ ===
        if (isValid && category) {
            const categoryValidation = validateCategoryFields(category);
            if (!categoryValidation.isValid) {
                isValid = false;
                if (categoryValidation.field) {
                    showError(categoryValidation.field, categoryValidation.message);
                } else {
                    alert(categoryValidation.message);
                }
            }
        }

        // === ОТПРАВКА ===
        if (isValid) {
            // Удаляем все значения-заглушки перед отправкой
            document.querySelectorAll('select, input:not([type="file"]):not([type="hidden"])').forEach(el => {
                if (el.tagName === 'SELECT') {
                    if (!isSelectValid(el)) {
                        el.disabled = true;
                        el.removeAttribute('required');
                    }
                } else {
                    if (!isValidValue(el.value)) {
                        el.disabled = true;
                        el.removeAttribute('required');
                    }
                }
            });

            // Включаем обратно обязательные поля
            ['title', 'price', 'locationInput'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.disabled = false;
            });

            form.submit();
        } else {
            const firstError = document.querySelector('.is-invalid');
            if (firstError) {
                firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                firstError.focus();
            }
        }
    });

    // ===== ВАЛИДАЦИЯ КАТЕГОРИЙ =====
    function validateCategoryFields(category) {
        const result = { isValid: true, message: '', field: null };

        // Общие поля для всех категорий
        function validateSelectFields(fields) {
            for (const field of fields) {
                const el = document.getElementById(field.id);
                if (!el) {
                    result.isValid = false;
                    result.message = `Поле "${field.name}" не найдено`;
                    result.field = null;
                    return false;
                }

                let valid = false;
                if (el.tagName === 'SELECT') {
                    valid = isSelectValid(el);
                } else {
                    valid = isValidValue(el.value);
                }

                if (!valid) {
                    result.isValid = false;
                    result.message = `Пожалуйста, заполните поле "${field.name}"`;
                    result.field = el;
                    return false;
                }
            }
            return true;
        }

        // Проверка обязательных полей
        function validateRequiredField(id, name) {
            const el = document.getElementById(id);
            if (!el) {
                result.isValid = false;
                result.message = `Поле "${name}" не найдено`;
                result.field = null;
                return false;
            }

            let valid = false;
            if (el.tagName === 'SELECT') {
                valid = isSelectValid(el);
            } else {
                valid = isValidValue(el.value);
            }

            if (!valid) {
                result.isValid = false;
                result.message = `Пожалуйста, заполните поле "${name}"`;
                result.field = el;
                return false;
            }
            return true;
        }

        switch(category) {
            case 'realty': {
                if (!validateRequiredField('propertyType', 'Тип недвижимости')) break;

                const type = document.getElementById('propertyType').value;

                if (type === 'apartment') {
                    const fields = [
                        { id: 'materialUp', name: 'Материал дома' },
                        { id: 'roomsCount', name: 'Количество комнат' },
                        { id: 'floorsApartment', name: 'Этаж' },
                        { id: 'floorsCount', name: 'Количество этажей' },
                        { id: 'hasElevator', name: 'Лифт' },
                        { id: 'buildYear', name: 'Год постройки' },
                        { id: 'repairUp', name: 'Ремонт' }
                    ];
                    validateSelectFields(fields);
                } else if (type === 'house') {
                    const fields = [
                        { id: 'materialHouse', name: 'Материал дома' },
                        { id: 'floorsCountHouse', name: 'Количество этажей' },
                        { id: 'repairHouse', name: 'Ремонт' },
                        { id: 'houseArea', name: 'Площадь дома' },
                        { id: 'landArea', name: 'Площадь земли' },
                        { id: 'buildYearHouse', name: 'Год постройки' },
                        { id: 'distanceToCityHouse', name: 'Расстояние до города' }
                    ];
                    validateSelectFields(fields);
                } else if (type === 'land') {
                    const fields = [
                        { id: 'landAreaLand', name: 'Площадь участка' },
                        { id: 'distanceToCity', name: 'Расстояние до города' }
                    ];
                    validateSelectFields(fields);
                } else if (type === 'garage') {
                    validateRequiredField('landAreaGarage', 'Площадь гаража');
                } else if (type === 'commercial') {
                    validateRequiredField('landAreaCommercial', 'Площадь помещения');
                }
                break;
            }

            case 'motor': {
                const fields = [
                    { id: 'vehicleType', name: 'Тип транспорта' },
                    { id: 'vehicleOrigin', name: 'Регион бренда' },
                    { id: 'vehicleYear', name: 'Год выпуска' },
                    { id: 'mileage', name: 'Пробег' },
                    { id: 'numberOfOwners', name: 'Количество владельцев' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'auto': {
                const fields = [
                    { id: 'autoType', name: 'Тип' },
                    { id: 'conditionAuto', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'constructions': {
                validateRequiredField('constructionsType', 'Тип назначения');
                break;
            }

            case 'interior': {
                const fields = [
                    { id: 'interiorType', name: 'Тип' },
                    { id: 'conditionInterior', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'tools': {
                const fields = [
                    { id: 'toolsType', name: 'Тип' },
                    { id: 'conditionTools', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'appliances': {
                const fields = [
                    { id: 'appliancesType', name: 'Тип' },
                    { id: 'conditionAppliances', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'electronics': {
                const fields = [
                    { id: 'electronicsType', name: 'Тип' },
                    { id: 'conditionElectronics', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'clothing': {
                const fields = [
                    { id: 'clothingType', name: 'Тип' },
                    { id: 'genderClothing', name: 'Пол' },
                    { id: 'conditionClothing', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'childrenPr': {
                const fields = [
                    { id: 'genderChildrenPr', name: 'Пол' },
                    { id: 'conditionChildrenPr', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'sports': {
                const fields = [
                    { id: 'sportsKind', name: 'Вид' },
                    { id: 'sportsType', name: 'Тип' },
                    { id: 'genderSports', name: 'Пол' },
                    { id: 'conditionSports', name: 'Состояние' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'food': {
                validateRequiredField('foodKind', 'Вид продуктов');
                break;
            }

            case 'health': {
                const fields = [
                    { id: 'healthKind', name: 'Вид' },
                    { id: 'healthType', name: 'Тип' },
                    { id: 'genderHealth', name: 'Пол' }
                ];
                validateSelectFields(fields);
                break;
            }

            case 'animals': {
                validateRequiredField('animalsKind', 'Вид');
                break;
            }

            default:
                break;
        }

        return result;
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====
    function showError(element, message) {
        if (!element) return;

        element.classList.add('is-invalid');

        let feedback = element.nextElementSibling;
        if (!feedback || !feedback.classList.contains('invalid-feedback')) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            element.parentNode.insertBefore(feedback, element.nextElementSibling);
        }
        feedback.textContent = message;
        feedback.style.display = 'block';
    }

    function clearErrors() {
        document.querySelectorAll('.is-invalid').forEach(el => {
            el.classList.remove('is-invalid');
        });
        document.querySelectorAll('.invalid-feedback').forEach(el => {
            el.style.display = 'none';
        });
    }

    // Очищаем ошибки при изменении
    document.querySelectorAll('input, select, textarea').forEach(el => {
        el.addEventListener('change', function() {
            this.classList.remove('is-invalid');
            const feedback = this.nextElementSibling;
            if (feedback && feedback.classList.contains('invalid-feedback')) {
                feedback.style.display = 'none';
            }
        });
        el.addEventListener('input', function() {
            if (this.classList.contains('is-invalid')) {
                this.classList.remove('is-invalid');
                const feedback = this.nextElementSibling;
                if (feedback && feedback.classList.contains('invalid-feedback')) {
                    feedback.style.display = 'none';
                }
            }
        });
    });
});