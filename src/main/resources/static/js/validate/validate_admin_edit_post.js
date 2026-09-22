document.addEventListener('DOMContentLoaded', function() {

    // Только формы товаров в админке (не service/vac/resume)
    const forms = document.querySelectorAll('form[id^="adminProductForm_"]');
    if (!forms.length) return;

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
        if (value === null || value === undefined) return false;
        const trimmed = String(value).trim().toLowerCase();
        if (trimmed === '') return false;
        if (PLACEHOLDER_VALUES.some(v => trimmed === String(v).toLowerCase())) return false;
        if (trimmed.match(/^[-—]+$/)) return false;
        return true;
    }

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
        if (placeholderTexts.some(text => optionText.includes(text))) return false;

        return isValidValue(selectedOption.value);
    }

    function getCategory(form) {
        const catField = form.querySelector('#category');
        if (catField && catField.value) return catField.value;
        const catContainer = form.querySelector('[data-category]');
        if (catContainer) return catContainer.getAttribute('data-category') || '';
        return '';
    }

    // ===== ВСЕ ПОЛЯ ОБЯЗАТЕЛЬНЫЕ =====
    function validateCategoryFields(form, category) {
        const result = { isValid: true, message: '', field: null };

        function validateField(id, name) {
            const el = form.querySelector('#' + id);
            if (!el) return true; // поле отсутствует в этой категории — пропускаем

            let valid = el.tagName === 'SELECT' ? isSelectValid(el) : isValidValue(el.value);

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
                const propertyType = form.querySelector('input[name="propertyType"]');
                if (!propertyType) break;
                const type = propertyType.value;

                if (type === 'apartment') {
                    validateField('materialUp', 'Материал дома');
                    validateField('roomsCount', 'Количество комнат');
                    validateField('floorsApartment', 'Этаж');
                    validateField('floorsCount', 'Количество этажей');
                    validateField('hasElevator', 'Лифт');
                    validateField('apartmentBuildYear', 'Год постройки');
                    validateField('repairUp', 'Ремонт');
                } else if (type === 'house') {
                    validateField('materialHouse', 'Материал дома');
                    validateField('floorsCountHouse', 'Количество этажей');
                    validateField('repairHouse', 'Ремонт');
                    validateField('houseArea', 'Площадь дома');
                    validateField('houseLandArea', 'Площадь земли');
                    validateField('houseBuildYear', 'Год постройки');
                    validateField('distanceToCityHouse', 'Расстояние до города');
                } else if (type === 'land') {
                    validateField('landArea', 'Площадь участка');
                    validateField('distanceToCity', 'Расстояние до города');
                } else if (type === 'garage') {
                    validateField('landAreaGarage', 'Площадь гаража');
                } else if (type === 'commercial') {
                    validateField('landAreaCommercial', 'Площадь помещения');
                }
                break;
            }

            case 'motor': {
                validateField('vehicleType', 'Тип транспорта');
                validateField('vehicleOrigin', 'Регион бренда');
                validateField('vehicleYear', 'Год выпуска');
                validateField('mileage', 'Пробег');
                validateField('numberOfOwners', 'Количество владельцев');
                break;
            }

            case 'auto': {
                validateField('autoType', 'Тип');
                validateField('conditionAuto', 'Состояние');
                break;
            }
            case 'constructions': {
                validateField('constructionsType', 'Тип назначения');
                break;
            }
            case 'interior': {
                validateField('interiorType', 'Тип');
                validateField('conditionInterior', 'Состояние');
                break;
            }
            case 'tools': {
                validateField('toolsType', 'Тип');
                validateField('conditionTools', 'Состояние');
                break;
            }
            case 'appliances': {
                validateField('appliancesType', 'Тип');
                validateField('conditionAppliances', 'Состояние');
                break;
            }
            case 'electronics': {
                validateField('electronicsType', 'Тип');
                validateField('conditionElectronics', 'Состояние');
                break;
            }
            case 'clothing': {
                validateField('clothingType', 'Тип');
                validateField('genderClothing', 'Пол');
                validateField('conditionClothing', 'Состояние');
                break;
            }
            case 'childrenPr': {
                validateField('genderChildrenPr', 'Пол');
                validateField('conditionChildrenPr', 'Состояние');
                break;
            }
            case 'sports': {
                validateField('sportsKind', 'Вид');
                validateField('sportsType', 'Тип');
                validateField('genderSports', 'Пол');
                validateField('conditionSports', 'Состояние');
                break;
            }
            case 'food': {
                validateField('foodKind', 'Вид продуктов');
                break;
            }
            case 'health': {
                validateField('healthKind', 'Вид');
                validateField('healthType', 'Тип');
                validateField('genderHealth', 'Пол');
                break;
            }
            case 'animals': {
                validateField('animalsKind', 'Вид');
                break;
            }
        }

        return result;
    }

    // ===== ОБРАБОТЧИК ОТПРАВКИ =====
    forms.forEach(function(form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            clearErrors(form);

            const category = getCategory(form);
            let isValid = true;

            const title = form.querySelector('#title');
            if (title && !isValidValue(title.value)) {
                showError(title, 'Пожалуйста, заполните название товара');
                isValid = false;
            }

            const price = form.querySelector('#price');
            if (price && (!price.value || parseInt(price.value) <= 0)) {
                showError(price, 'Укажите корректную цену (больше 0)');
                isValid = false;
            }

            const location = form.querySelector('#locationInput');
            if (location && !isValidValue(location.value)) {
                showError(location, 'Укажите местоположение');
                isValid = false;
            }

            if (isValid && category) {
                const v = validateCategoryFields(form, category);
                if (!v.isValid) {
                    isValid = false;
                    if (v.field) showError(v.field, v.message);
                    else alert(v.message);
                }
            }

            if (isValid) {
                // Отключаем пустые/плейсхолдерные поля, чтобы не мешали биндингу
                form.querySelectorAll('select, input:not([type="file"]):not([type="hidden"])').forEach(el => {
                    if (el.tagName === 'SELECT') {
                        if (!isSelectValid(el)) { el.disabled = true; el.removeAttribute('required'); }
                    } else {
                        if (!isValidValue(el.value)) { el.disabled = true; el.removeAttribute('required'); }
                    }
                });

                // Возвращаем обязательные
                ['title', 'price', 'locationInput'].forEach(id => {
                    const el = form.querySelector('#' + id);
                    if (el) el.disabled = false;
                });

                // Отключаем неактивные подтипы realty
                if (category === 'realty') {
                    const pt = form.querySelector('input[name="propertyType"]');
                    const propertyType = pt ? pt.value : '';
                    const subtypeBlocks = {
                        'apartment':  'apartmentFieldsEdit',
                        'house':      'houseFieldsEdit',
                        'land':       'landFieldsEdit',
                        'garage':     'garageFieldsEdit',
                        'commercial': 'commercialFieldsEdit'
                    };
                    Object.keys(subtypeBlocks).forEach(function(subtype) {
                        const block = form.querySelector('#' + subtypeBlocks[subtype]);
                        if (!block) return;
                        if (subtype !== propertyType) {
                            block.querySelectorAll('input, select, textarea').forEach(el => el.disabled = true);
                        }
                    });
                    if (pt) pt.disabled = false;
                }

                form.submit();
            } else {
                const firstError = form.querySelector('.is-invalid');
                if (firstError) {
                    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstError.focus();
                }
            }
        });

        form.querySelectorAll('input, select, textarea').forEach(el => {
            el.addEventListener('change', function() {
                this.classList.remove('is-invalid');
                const fb = this.nextElementSibling;
                if (fb && fb.classList.contains('invalid-feedback')) fb.style.display = 'none';
            });
            el.addEventListener('input', function() {
                if (this.classList.contains('is-invalid')) {
                    this.classList.remove('is-invalid');
                    const fb = this.nextElementSibling;
                    if (fb && fb.classList.contains('invalid-feedback')) fb.style.display = 'none';
                }
            });
        });
    });

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

    function clearErrors(form) {
        form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        form.querySelectorAll('.invalid-feedback').forEach(el => el.style.display = 'none');
    }

});