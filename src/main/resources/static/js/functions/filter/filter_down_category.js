document.addEventListener('DOMContentLoaded', function() {
    // ====== ПОКАЗ/СКРЫТИЕ ФИЛЬТРОВ В ЗАВИСИМОСТИ ОТ КАТЕГОРИИ ======
    function toggleFilters() {
        const urlParams = new URLSearchParams(window.location.search);
        const categoryParam = urlParams.get('category');

        const realtyFilters = document.getElementById('realtyFilters');
        const vehicleFilters = document.getElementById('vehicleFilters');
        const autoFilters = document.getElementById('autoFilters');
        const constructionsFilters = document.getElementById('constructionsFilters');
        const interiorFilters = document.getElementById('interiorFilters');
        const toolsFilters = document.getElementById('toolsFilters');
        const appliancesFilters = document.getElementById('appliancesFilters');
        const electronicsFilters = document.getElementById('electronicsFilters');
        const clothingFilters = document.getElementById('clothingFilters');
        const childrenPrFilters = document.getElementById('childrenPrFilters');
        const sportsFilters = document.getElementById('sportsFilters');
        const foodFilters = document.getElementById('foodFilters');
        const healthFilters = document.getElementById('healthFilters');
        const animalsFilters = document.getElementById('animalsFilters');

        // Скрываем все фильтры
        if (realtyFilters) realtyFilters.style.display = 'none';
        if (vehicleFilters) vehicleFilters.style.display = 'none';
        if (autoFilters) autoFilters.style.display = 'none';
        if (constructionsFilters) constructionsFilters.style.display = 'none';
        if (interiorFilters) interiorFilters.style.display = 'none';
        if (toolsFilters) toolsFilters.style.display = 'none';
        if (appliancesFilters) appliancesFilters.style.display = 'none';
        if (electronicsFilters) electronicsFilters.style.display = 'none';
        if (clothingFilters) clothingFilters.style.display = 'none';
        if (childrenPrFilters) childrenPrFilters.style.display = 'none';
        if (sportsFilters) sportsFilters.style.display = 'none';
        if (foodFilters) foodFilters.style.display = 'none';
        if (healthFilters) healthFilters.style.display = 'none';
        if (animalsFilters) animalsFilters.style.display = 'none';

        // Показываем соответствующие фильтры
        if (categoryParam === 'realty') {
            if (realtyFilters) {
                realtyFilters.style.display = 'block';
                // Обновляем подфильтры для недвижимости
                setTimeout(updateRealtySubFilters, 50);
            }
        } else if (categoryParam === 'motor') {
            if (vehicleFilters) vehicleFilters.style.display = 'block';
        } else if (categoryParam === 'auto') {
        if (autoFilters) autoFilters.style.display = 'block';
        } else if (categoryParam === 'constructions') {
            if (constructionsFilters) constructionsFilters.style.display = 'block';
        } else if (categoryParam === 'interior') {
            if (interiorFilters) interiorFilters.style.display = 'block';
        } else if (categoryParam === 'tools') {
            if (toolsFilters) toolsFilters.style.display = 'block';
        } else if (categoryParam === 'appliances') {
            if (appliancesFilters) appliancesFilters.style.display = 'block';
        } else if (categoryParam === 'electronics') {
            if (electronicsFilters) electronicsFilters.style.display = 'block';
        } else if (categoryParam === 'clothing') {
            if (clothingFilters) clothingFilters.style.display = 'block';
        } else if (categoryParam === 'childrenPr') {
            if (childrenPrFilters) childrenPrFilters.style.display = 'block';
        } else if (categoryParam === 'sports') {
            if (sportsFilters) sportsFilters.style.display = 'block';
        } else if (categoryParam === 'food') {
            if (foodFilters) foodFilters.style.display = 'block';
        } else if (categoryParam === 'health') {
            if (healthFilters) healthFilters.style.display = 'block';
        } else if (categoryParam === 'animals') {
            if (animalsFilters) animalsFilters.style.display = 'block';
        }
    }

    // ====== ОБНОВЛЕНИЕ ПОДФИЛЬТРОВ НЕДВИЖИМОСТИ ======
    function updateRealtySubFilters() {
        const propertyType = document.getElementById('propertyType')?.value || '';
        const apartmentFilters = document.getElementById('apartmentFilters');
        const houseFilters = document.getElementById('houseFilters');
        const landFilters = document.getElementById('landFilters');
        const garageFilters = document.getElementById('garageFilters');
        const commercialFilters = document.getElementById('commercialFilters');

        // Скрываем все
        if (apartmentFilters) apartmentFilters.style.display = 'none';
        if (houseFilters) houseFilters.style.display = 'none';
        if (landFilters) landFilters.style.display = 'none';
        if (garageFilters) garageFilters.style.display = 'none';
        if (commercialFilters) commercialFilters.style.display = 'none';

        // Показываем соответствующий
        if (propertyType === 'apartment' && apartmentFilters) {
            apartmentFilters.style.display = 'block';
        } else if (propertyType === 'house' && houseFilters) {
            houseFilters.style.display = 'block';
        } else if (propertyType === 'land' && landFilters) {
            landFilters.style.display = 'block';
        } else if (propertyType === 'garage' && garageFilters) {
            garageFilters.style.display = 'block';
        } else if (propertyType === 'commercial' && commercialFilters) {
            commercialFilters.style.display = 'block';
        }
    }

    // ====== ОБРАБОТЧИКИ ======
    // При изменении типа недвижимости
    const propertyTypeSelect = document.getElementById('propertyType');
    if (propertyTypeSelect) {
        propertyTypeSelect.addEventListener('change', function() {
            // ОБНОВЛЯЕМ ПОДФИЛЬТРЫ
            updateRealtySubFilters();

            // ===== ОЧИЩАЕМ ВСЕ ПАРАМЕТРЫ ФИЛЬТРОВ ПРИ СМЕНЕ ТИПА НЕДВИЖИМОСТИ =====
            const url = new URL(window.location.href);

            // Список параметров, которые нужно очистить при смене типа недвижимости
            const paramsToRemove = [
                'roomsCount', 'floorsApartment', 'floorsCount', 'floorsCountHouse',
                'hasElevator', 'apartmentBuildYear', 'houseBuildYear', 'houseArea',
                'landArea', 'distanceToCity', 'materialUp', 'repairUp',
                'materialHouse', 'repairHouse', 'distanceToCityHouse',
                'landAreaGarage', 'landAreaCommercial', 'mileage', 'numberOfOwners'
            ];
            paramsToRemove.forEach(param => url.searchParams.delete(param));

            // Устанавливаем новый propertyType
            if (this.value) {
                url.searchParams.set('propertyType', this.value);
            } else {
                url.searchParams.delete('propertyType');
            }

            // Сбрасываем пагинацию
            url.searchParams.delete('page');

            // Перенаправляем на новый URL
            window.location.href = url.toString();
        });
    }

    // При применении фильтров
    const applyBtn = document.getElementById('applyFilters');
    if (applyBtn) {
        applyBtn.addEventListener('click', function(e) {
            e.preventDefault();
            applyFilters();
        });
    }

    // При сбросе фильтров
    const resetBtn = document.getElementById('resetFilters');
    if (resetBtn) {
        resetBtn.addEventListener('click', function(e) {
            e.preventDefault();
            resetFilters();
        });
    }

// ====== ПРИМЕНЕНИЕ ФИЛЬТРОВ ======
    function applyFilters() {

        const url = new URL(window.location.href);
        const params = new URLSearchParams();

        // Сохраняем основные параметры из URL
        const currentParams = new URLSearchParams(window.location.search);

        // Базовые параметры
        const category = currentParams.get('category');
        if (category && category !== 'all') params.set('category', category);

        const region = currentParams.get('region');
        if (region && region !== 'all') params.set('region', region);

        const city = currentParams.get('city');
        if (city && city !== 'all') params.set('city', city);

        const search = currentParams.get('search');
        if (search) params.set('search', search);

        const sort = currentParams.get('sort');
        if (sort && sort !== 'newest') params.set('sort', sort);

        const priceSort = currentParams.get('priceSort');
        if (priceSort && priceSort !== 'none') params.set('priceSort', priceSort);

        // ===== ФИЛЬТРЫ НЕДВИЖИМОСТИ =====
        const propertyType = document.getElementById('propertyType')?.value;
        if (propertyType && propertyType !== '') params.set('propertyType', propertyType);

        // ===== КВАРТИРА =====
        if (propertyType === 'apartment') {
            const roomsCount = document.getElementById('roomsCount')?.value;
            if (roomsCount && roomsCount !== '') params.set('roomsCount', roomsCount);

            const floorsApartment = document.getElementById('floorsApartment')?.value;
            if (floorsApartment && floorsApartment !== '') params.set('floorsApartment', floorsApartment);

            const floorsCount = document.getElementById('floorsCount')?.value;
            if (floorsCount && floorsCount !== '') params.set('floorsCount', floorsCount);

            const hasElevator = document.getElementById('hasElevator')?.value;
            if (hasElevator && hasElevator !== '') params.set('hasElevator', hasElevator);

            // apartmentBuildYear
            const apartmentBuildYear = document.getElementById('buildYearCount')?.value;
            if (apartmentBuildYear && apartmentBuildYear !== '') params.set('apartmentBuildYear', apartmentBuildYear);

            const materialUp = document.getElementById('materialUp')?.value;
            if (materialUp && materialUp !== '') params.set('materialUp', materialUp);

            const repairUp = document.getElementById('repairUp')?.value;
            if (repairUp && repairUp !== '') params.set('repairUp', repairUp);
        }

        // ===== ДОМ =====
        if (propertyType === 'house') {
            const floorsCountHouse = document.getElementById('floorsCountHouse')?.value;
            if (floorsCountHouse && floorsCountHouse !== '') params.set('floorsCountHouse', floorsCountHouse);

            const houseArea = document.getElementById('houseAreaCount')?.value;
            if (houseArea && houseArea !== '') params.set('houseArea', houseArea);

            const landAreaHouse = document.getElementById('landAreaHouseCount')?.value;
            if (landAreaHouse && landAreaHouse !== '') params.set('landArea', landAreaHouse);

            const houseBuildYear = document.getElementById('houseBuildYearCount')?.value;
            if (houseBuildYear && houseBuildYear !== '') params.set('houseBuildYear', houseBuildYear);

            const materialHouse = document.getElementById('materialHouse')?.value;
            if (materialHouse && materialHouse !== '') params.set('materialHouse', materialHouse);

            const repairHouse = document.getElementById('repairHouse')?.value;
            if (repairHouse && repairHouse !== '') params.set('repairHouse', repairHouse);

            const distanceToCityHouse = document.getElementById('distanceToCityHouse')?.value;
            if (distanceToCityHouse && distanceToCityHouse !== '') params.set('distanceToCityHouse', distanceToCityHouse);

        }

        // ===== УЧАСТОК =====
        if (propertyType === 'land') {
            const landArea = document.getElementById('landArea')?.value;
            if (landArea && landArea !== '') params.set('landArea', landArea);

            const distanceToCity = document.getElementById('distanceToCityCount')?.value;
            if (distanceToCity && distanceToCity !== '') params.set('distanceToCity', distanceToCity);
        }

        // ===== ГАРАЖ =====
        if (propertyType === 'garage') {
            const landAreaGarage = document.getElementById('landAreaGarage')?.value;
            if (landAreaGarage && landAreaGarage !== '') params.set('landAreaGarage', landAreaGarage);
        }

        // ===== КОММЕРЧЕСКАЯ =====
        if (propertyType === 'commercial') {
            const landAreaCommercial = document.getElementById('landAreaCommercial')?.value;
            if (landAreaCommercial && landAreaCommercial !== '') params.set('landAreaCommercial', landAreaCommercial);
        }


        // ===== ФИЛЬТРЫ АВТОТРАНСПОРТА =====
        if (category === 'motor') {
            const vehicleType = document.getElementById('vehicleType')?.value;
            if (vehicleType && vehicleType !== '') params.set('vehicleType', vehicleType);

            const vehicleOrigin = document.getElementById('vehicleOrigin')?.value;
            if (vehicleOrigin && vehicleOrigin !== '') params.set('vehicleOrigin', vehicleOrigin);

            const vehicleYear = document.getElementById('vehicleYearCount')?.value;
            if (vehicleYear && vehicleYear !== '') params.set('vehicleYear', vehicleYear);

            const mileage = document.getElementById('mileage')?.value;
            if (mileage && mileage !== '') params.set('mileage', mileage);

            const numberOfOwners = document.getElementById('numberOfOwners')?.value;
            if (numberOfOwners && numberOfOwners !== '') params.set('numberOfOwners', numberOfOwners);
        }

        // ===== ФИЛЬТРЫ Автозапчасти и автотовары =====
        if (category === 'auto') {
            const autoType = document.getElementById('autoType')?.value;
            if (autoType && autoType !== '') params.set('autoType', autoType);

            const conditionAuto = document.getElementById('conditionAuto')?.value;
            if (conditionAuto && conditionAuto !== '') params.set('conditionAuto', conditionAuto);

        }

        // ===== ФИЛЬТРЫ Строительные материалы =====
        if (category === 'constructions') {
            const constructionsType = document.getElementById('constructionsType')?.value;
            if (constructionsType && constructionsType !== '') params.set('constructionsType', constructionsType);

            const conditionAuto = document.getElementById('conditionAuto')?.value;
            if (conditionAuto && conditionAuto !== '') params.set('conditionAuto', conditionAuto);

        }

        // ===== ФИЛЬТРЫ Дом и интерьер =====
        if (category === 'interior') {
            const interiorType = document.getElementById('interiorType')?.value;
            if (interiorType && interiorType !== '') params.set('interiorType', interiorType);

            const conditionInterior = document.getElementById('conditionInterior')?.value;
            if (conditionInterior && conditionInterior !== '') params.set('conditionInterior', conditionInterior);

        }

        // ===== ФИЛЬТРЫ Инструменты=====
        if (category === 'tools') {
            const toolsType = document.getElementById('toolsType')?.value;
            if (toolsType && toolsType !== '') params.set('toolsType', toolsType);

            const conditionTools = document.getElementById('conditionTools')?.value;
            if (conditionTools && conditionTools !== '') params.set('conditionTools', conditionTools);

        }

        // ===== Бытовая техника=====
        if (category === 'appliances') {
            const appliancesType = document.getElementById('appliancesType')?.value;
            if (appliancesType && appliancesType !== '') params.set('appliancesType', appliancesType);

            const conditionAppliances = document.getElementById('conditionAppliances')?.value;
            if (conditionAppliances && conditionAppliances !== '') params.set('conditionAppliances', conditionAppliances);

        }

        // ===== Электроника и гаджеты=====
        if (category === 'electronics') {
            const electronicsType = document.getElementById('electronicsType')?.value;
            if (electronicsType && electronicsType !== '') params.set('electronicsType', electronicsType);

            const conditionElectronics = document.getElementById('conditionElectronics')?.value;
            if (conditionElectronics && conditionElectronics !== '') params.set('conditionElectronics', conditionElectronics);

        }

        // =====Одежда и обувь=====
        if (category === 'clothing') {
            const clothingType = document.getElementById('clothingType')?.value;
            if (clothingType && clothingType !== '') params.set('clothingType', clothingType);

            const gender = document.getElementById('genderClothing')?.value;
            if (gender && gender !== '') params.set('genderClothing', gender);

            const conditionClothing = document.getElementById('conditionClothing')?.value;
            if (conditionClothing && conditionClothing !== '') params.set('conditionClothing', conditionClothing);

        }


        // =====Детские товары=====
        if (category === 'childrenPr') {
            const genderChildrenPr = document.getElementById('genderChildrenPr')?.value;
            if (genderChildrenPr && genderChildrenPr !== '') params.set('genderChildrenPr', genderChildrenPr);

            const conditionChildrenPr = document.getElementById('conditionChildrenPr')?.value;
            if (conditionChildrenPr && conditionChildrenPr !== '') params.set('conditionChildrenPr', conditionChildrenPr);

        }

        // =====Товары для спорта====
        if (category === 'sports') {
            const sportsKind = document.getElementById('sportsKind')?.value;
            if (sportsKind && sportsKind !== '') params.set('sportsKind', sportsKind);

            const sportsType = document.getElementById('sportsType')?.value;
            if (sportsType && sportsType !== '') params.set('sportsType', sportsType);

            const genderSports = document.getElementById('genderSports')?.value;
            if (genderSports && genderSports !== '') params.set('genderSports', genderSports);

            const conditionSports = document.getElementById('conditionSports')?.value;
            if (conditionSports && conditionSports !== '') params.set('conditionSports', conditionSports);

        }

        // =====Продукты питания====
        if (category === 'food') {
            const foodKind = document.getElementById('foodKind')?.value;
            if (foodKind && foodKind !== '') params.set('foodKind', foodKind);

        }

        // =====Здоровье и красота====
        if (category === 'health') {
            const healthKind = document.getElementById('healthKind')?.value;
            if (healthKind && healthKind !== '') params.set('healthKind', healthKind);

            const healthType = document.getElementById('healthType')?.value;
            if (healthType && healthType !== '') params.set('healthType', healthKind);

            const genderHealth = document.getElementById('genderHealth')?.value;
            if (genderHealth && genderHealth !== '') params.set('genderHealth', healthKind);

        }

        // =====Флора и фауна====
        if (category === 'animals') {
            const animalsKind = document.getElementById('animalsKind')?.value;
            if (animalsKind && animalsKind !== '') params.set('animalsKind', animalsKind);

        }


        // Сбрасываем пагинацию
        params.delete('page');

        // Логируем финальный URL
        const finalUrl = url.pathname + '?' + params.toString();
        console.log('Финальный URL:', finalUrl);
        console.log('=== APPLY FILTERS END ===');


        // Переходим на новый URL
        window.location.href = url.pathname + '?' + params.toString();
    }

// ====== СБРОС ФИЛЬТРОВ ======
    function resetFilters() {
        const url = new URL(window.location.href);
        const params = new URLSearchParams();

        // Сохраняем только базовые параметры из URL
        const currentParams = new URLSearchParams(window.location.search);

        const category = currentParams.get('category');
        if (category && category !== 'all') params.set('category', category);

        const region = currentParams.get('region');
        if (region && region !== 'all') params.set('region', region);

        const city = currentParams.get('city');
        if (city && city !== 'all') params.set('city', city);

        const search = currentParams.get('search');
        if (search) params.set('search', search);

        const sort = currentParams.get('sort');
        if (sort && sort !== 'newest') params.set('sort', sort);

        const priceSort = currentParams.get('priceSort');
        if (priceSort && priceSort !== 'none') params.set('priceSort', priceSort);

        // Сбрасываем все поля фильтров
        document.querySelectorAll('.filter-container select, .filter-container input').forEach(el => {
            const id = el.id;
            if (id && !['category', 'region', 'city', 'searchInput', 'currentSort', 'currentPriceSort'].includes(id)) {
                el.value = '';
            }
        });

        // Удаляем параметры фильтров из URL
        const filterParams = [
            'propertyType', 'roomsCount', 'floorsApartment', 'floorsCount', 'floorsCountHouse',
            'hasElevator', 'apartmentBuildYear', 'houseBuildYear', 'houseArea', 'landArea',
            'distanceToCity', 'distanceToCityHouse', 'landAreaGarage', 'landAreaCommercial',
            'vehicleType', 'vehicleOrigin', 'vehicleYear', 'mileage', 'numberOfOwners',
            'autoType', 'conditionAuto', 'constructionsType', 'interiorType', 'conditionInterior',
            'toolsType', 'conditionTools', 'appliancesType', 'conditionAppliances',
            'electronicsType', 'conditionElectronics', 'clothingType', 'genderClothing',
            'conditionClothing', 'genderChildrenPr', 'conditionChildrenPr', 'sportsKind',
            'sportsType', 'genderSports', 'conditionSports', 'foodKind', 'healthKind',
            'healthType', 'genderHealth', 'animalsKind'
        ];
        filterParams.forEach(param => params.delete(param));

        // Сбрасываем пагинацию
        params.delete('page');

        window.location.href = url.pathname + (params.toString() ? '?' + params.toString() : '');
    }

// ====== ВОССТАНОВЛЕНИЕ ЗНАЧЕНИЙ ФИЛЬТРОВ ИЗ URL ======
    function restoreFilterValues() {
        const urlParams = new URLSearchParams(window.location.search);

        const propertyType = urlParams.get('propertyType') || '';

        // Восстанавливаем propertyType
        const propertyTypeEl = document.getElementById('propertyType');
        if (propertyTypeEl) propertyTypeEl.value = propertyType;

        // ===== КВАРТИРА =====
        const roomsCountEl = document.getElementById('roomsCount');
        if (roomsCountEl) roomsCountEl.value = urlParams.get('roomsCount') || '';

        const floorsApartmentEl = document.getElementById('floorsApartment');
        if (floorsApartmentEl) floorsApartmentEl.value = urlParams.get('floorsApartment') || '';

        const floorsCountEl = document.getElementById('floorsCount');
        if (floorsCountEl) floorsCountEl.value = urlParams.get('floorsCount') || '';

        const hasElevatorEl = document.getElementById('hasElevator');
        if (hasElevatorEl) hasElevatorEl.value = urlParams.get('hasElevator') || '';

        // ИСПРАВЛЕНО: apartmentBuildYear
        const buildYearCountEl = document.getElementById('buildYearCount');
        if (buildYearCountEl) buildYearCountEl.value = urlParams.get('apartmentBuildYear') || '';

        const materialUpEl = document.getElementById('materialUp');
        if (materialUpEl) materialUpEl.value = urlParams.get('materialUp') || '';

        const repairUpEl = document.getElementById('repairUp');
        if (repairUpEl) repairUpEl.value = urlParams.get('repairUp') || '';

        // ===== ДОМ =====
        const floorsCountHouseEl = document.getElementById('floorsCountHouse');
        if (floorsCountHouseEl) floorsCountHouseEl.value = urlParams.get('floorsCountHouse') || '';

        const houseAreaCountEl = document.getElementById('houseAreaCount');
        if (houseAreaCountEl) houseAreaCountEl.value = urlParams.get('houseArea') || '';

        const landAreaHouseCountEl = document.getElementById('landAreaHouseCount');
        if (landAreaHouseCountEl) landAreaHouseCountEl.value = urlParams.get('landArea') || '';

        // ИСПРАВЛЕНО: houseBuildYear
        const houseBuildYearCountEl = document.getElementById('houseBuildYearCount');
        if (houseBuildYearCountEl) houseBuildYearCountEl.value = urlParams.get('houseBuildYear') || '';

        const materialHouseEl = document.getElementById('materialHouse');
        if (materialHouseEl) materialHouseEl.value = urlParams.get('materialHouse') || '';

        const repairHouseEl = document.getElementById('repairHouse');
        if (repairHouseEl) repairHouseEl.value = urlParams.get('repairHouse') || '';

        const distanceToCityHouseEl = document.getElementById('distanceToCityHouse');
        if (distanceToCityHouseEl) distanceToCityHouseEl.value = urlParams.get('distanceToCityHouse') || '';

        // ===== УЧАСТОК =====
        const landAreaEl = document.getElementById('landArea');
        if (landAreaEl) landAreaEl.value = urlParams.get('landArea') || '';

        const distanceToCityCountEl = document.getElementById('distanceToCityCount');
        if (distanceToCityCountEl) distanceToCityCountEl.value = urlParams.get('distanceToCity') || '';

        // ===== ГАРАЖ =====
        const landAreaGarageEl = document.getElementById('landAreaGarage');
        if (landAreaGarageEl) landAreaGarageEl.value = urlParams.get('landAreaGarage') || '';

        // ===== КОММЕРЧЕСКАЯ =====
        const landAreaCommercialEl = document.getElementById('landAreaCommercial');
        if (landAreaCommercialEl) landAreaCommercialEl.value = urlParams.get('landAreaCommercial') || '';

        // ===== АВТОТРАНСПОРТ =====
        const vehicleTypeEl = document.getElementById('vehicleType');
        if (vehicleTypeEl) vehicleTypeEl.value = urlParams.get('vehicleType') || '';

        const vehicleOriginEl = document.getElementById('vehicleOrigin');
        if (vehicleOriginEl) vehicleOriginEl.value = urlParams.get('vehicleOrigin') || '';

        const vehicleYearCountEl = document.getElementById('vehicleYearCount');
        if (vehicleYearCountEl) vehicleYearCountEl.value = urlParams.get('vehicleYear') || '';

        const mileageEl = document.getElementById('mileage');
        if (mileageEl) mileageEl.value = urlParams.get('mileage') || '';

        const numberOfOwnersEl = document.getElementById('numberOfOwners');
        if (numberOfOwnersEl) numberOfOwnersEl.value = urlParams.get('numberOfOwners') || '';

        // ===== АВТОЗАПЧАСТИ =====
        const autoTypeEl = document.getElementById('autoType');
        if (autoTypeEl) autoTypeEl.value = urlParams.get('autoType') || '';

        const conditionAutoEl = document.getElementById('conditionAuto');
        if (conditionAutoEl) conditionAutoEl.value = urlParams.get('conditionAuto') || '';

        // ===== СТРОИТЕЛЬНЫЕ МАТЕРИАЛЫ =====
        const constructionsTypeEl = document.getElementById('constructionsType');
        if (constructionsTypeEl) constructionsTypeEl.value = urlParams.get('constructionsType') || '';

        // ===== ДОМ И ИНТЕРЬЕР =====
        const interiorTypeEl = document.getElementById('interiorType');
        if (interiorTypeEl) interiorTypeEl.value = urlParams.get('interiorType') || '';

        const conditionInteriorEl = document.getElementById('conditionInterior');
        if (conditionInteriorEl) conditionInteriorEl.value = urlParams.get('conditionInterior') || '';

        // ===== ИНСТРУМЕНТЫ =====
        const toolsTypeEl = document.getElementById('toolsType');
        if (toolsTypeEl) toolsTypeEl.value = urlParams.get('toolsType') || '';

        const conditionToolsEl = document.getElementById('conditionTools');
        if (conditionToolsEl) conditionToolsEl.value = urlParams.get('conditionTools') || '';

        // ===== БЫТОВАЯ ТЕХНИКА =====
        const appliancesTypeEl = document.getElementById('appliancesType');
        if (appliancesTypeEl) appliancesTypeEl.value = urlParams.get('appliancesType') || '';

        const conditionAppliancesEl = document.getElementById('conditionAppliances');
        if (conditionAppliancesEl) conditionAppliancesEl.value = urlParams.get('conditionAppliances') || '';

        // ===== ЭЛЕКТРОНИКА =====
        const electronicsTypeEl = document.getElementById('electronicsType');
        if (electronicsTypeEl) electronicsTypeEl.value = urlParams.get('electronicsType') || '';

        const conditionElectronicsEl = document.getElementById('conditionElectronics');
        if (conditionElectronicsEl) conditionElectronicsEl.value = urlParams.get('conditionElectronics') || '';

        // ===== ОДЕЖДА =====
        const clothingTypeEl = document.getElementById('clothingType');
        if (clothingTypeEl) clothingTypeEl.value = urlParams.get('clothingType') || '';

        const genderClothingEl = document.getElementById('genderClothing');
        if (genderClothingEl) genderClothingEl.value = urlParams.get('genderClothing') || '';

        const conditionClothingEl = document.getElementById('conditionClothing');
        if (conditionClothingEl) conditionClothingEl.value = urlParams.get('conditionClothing') || '';

        // ===== ДЕТСКИЕ ТОВАРЫ =====
        const genderChildrenPrEl = document.getElementById('genderChildrenPr');
        if (genderChildrenPrEl) genderChildrenPrEl.value = urlParams.get('genderChildrenPr') || '';

        const conditionChildrenPrEl = document.getElementById('conditionChildrenPr');
        if (conditionChildrenPrEl) conditionChildrenPrEl.value = urlParams.get('conditionChildrenPr') || '';

        // ===== СПОРТ =====
        const sportsKindEl = document.getElementById('sportsKind');
        if (sportsKindEl) sportsKindEl.value = urlParams.get('sportsKind') || '';

        const sportsTypeEl = document.getElementById('sportsType');
        if (sportsTypeEl) sportsTypeEl.value = urlParams.get('sportsType') || '';

        const genderSportsEl = document.getElementById('genderSports');
        if (genderSportsEl) genderSportsEl.value = urlParams.get('genderSports') || '';

        const conditionSportsEl = document.getElementById('conditionSports');
        if (conditionSportsEl) conditionSportsEl.value = urlParams.get('conditionSports') || '';

        // ===== ПРОДУКТЫ =====
        const foodKindEl = document.getElementById('foodKind');
        if (foodKindEl) foodKindEl.value = urlParams.get('foodKind') || '';

        // ===== ЗДОРОВЬЕ =====
        const healthKindEl = document.getElementById('healthKind');
        if (healthKindEl) healthKindEl.value = urlParams.get('healthKind') || '';

        const healthTypeEl = document.getElementById('healthType');
        if (healthTypeEl) healthTypeEl.value = urlParams.get('healthType') || '';

        const genderHealthEl = document.getElementById('genderHealth');
        if (genderHealthEl) genderHealthEl.value = urlParams.get('genderHealth') || '';

        // ===== ФЛОРА И ФАУНА =====
        const animalsKindEl = document.getElementById('animalsKind');
        if (animalsKindEl) animalsKindEl.value = urlParams.get('animalsKind') || '';

        // Обновляем подфильтры
        setTimeout(updateRealtySubFilters, 50);
    }

    // ====== ИНИЦИАЛИЗАЦИЯ ======
    // Показываем/скрываем фильтры при загрузке
    setTimeout(() => {
        toggleFilters();
        restoreFilterValues();
    }, 100);

    // Слушаем изменения URL (для кнопок назад/вперед)
    window.addEventListener('popstate', function() {
        setTimeout(() => {
            toggleFilters();
            restoreFilterValues();
        }, 50);
    });

    // Наблюдаем за изменением категории в дропдауне
    const categoryObserver = new MutationObserver(function() {
        toggleFilters();
    });

    const categoryText = document.getElementById('selectedCategoryText');
    if (categoryText) {
        categoryObserver.observe(categoryText, { characterData: true, childList: true, subtree: true });
    }

    // Также следим за выбором категории через клик
    document.querySelectorAll('.category-option').forEach(option => {
        option.addEventListener('click', function() {
            setTimeout(toggleFilters, 50);
        });
    });

    // ====== ПРИМЕНЕНИЕ ФИЛЬТРОВ ПО КЛАВИШЕ ENTER ======
    document.querySelectorAll('.filter-container select').forEach(select => {
        select.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                applyFilters();
            }
        });
    });


    // ====== АВТОТРАНСПОРТ - АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const vehicleTypeSelect = document.getElementById('vehicleType');
    if (vehicleTypeSelect) {
        vehicleTypeSelect.addEventListener('change', function() {
            // Очищаем параметры недвижимости
            const url = new URL(window.location.href);

            // Устанавливаем vehicleType
            if (this.value) {
                url.searchParams.set('vehicleType', this.value);
            } else {
                url.searchParams.delete('vehicleType');
            }

            // Очищаем остальные параметры авто, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('vehicleOrigin');
            url.searchParams.delete('vehicleYear');
            url.searchParams.delete('mileage');
            url.searchParams.delete('numberOfOwners');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Автозапчасти и автотовары - АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const autoTypeSelect = document.getElementById('autoType');
    if (autoTypeSelect) {
        autoTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('autoType', this.value);
            } else {
                url.searchParams.delete('autoType');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('conditionAuto');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Строительные материалы - АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const constructionsTypeSelect = document.getElementById('constructionsType');
    if (autoTypeSelect) {
        constructionsTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('constructionsType', this.value);
            } else {
                url.searchParams.delete('constructionsType');
            }

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Дом и интерьер - АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const interiorTypeSelect = document.getElementById('interiorType');
    if (interiorTypeSelect) {
        interiorTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('interiorType', this.value);
            } else {
                url.searchParams.delete('interiorType');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('conditionInterior');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Инструменты- АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const toolsTypeSelect = document.getElementById('toolsType');
    if (toolsTypeSelect) {
        toolsTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('toolsType', this.value);
            } else {
                url.searchParams.delete('toolsType');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('conditionTools');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Бытовая техника АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const appliancesTypeSelect = document.getElementById('appliancesType');
    if (appliancesTypeSelect) {
        appliancesTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('appliancesType', this.value);
            } else {
                url.searchParams.delete('appliancesType');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('conditionAppliances');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Электроника и гаджеты АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const electronicsTypeSelect = document.getElementById('electronicsType');
    if (electronicsTypeSelect) {
        electronicsTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('electronicsType', this.value);
            } else {
                url.searchParams.delete('electronicsType');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('conditionElectronics');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Одежда и обувь АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const clothingTypeSelect = document.getElementById('clothingType');
    if (clothingTypeSelect) {
        clothingTypeSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('clothingType', this.value);
            } else {
                url.searchParams.delete('clothingType');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('gender');
            url.searchParams.delete('conditionClothing');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Детские товары АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const genderChildrenPrSelect = document.getElementById('genderChildrenPr');
    if (genderChildrenPrSelect) {
        genderChildrenPrSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('genderChildrenPr', this.value);
            } else {
                url.searchParams.delete('genderChildrenPr');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('conditionChildrenPr');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Товары для спорта АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const sportsKindSelect = document.getElementById('sportsKind');
    if (sportsKindSelect) {
        sportsKindSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('sportsKind', this.value);
            } else {
                url.searchParams.delete('sportsKind');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('sportsType');
            url.searchParams.delete('genderSports');
            url.searchParams.delete('conditionSports');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Продукты питания АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const foodKindSelect = document.getElementById('foodKind');
    if (foodKindSelect) {
        foodKindSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('foodKind', this.value);
            } else {
                url.searchParams.delete('foodKind');
            }

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Здоровье и красота АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const healthKindSelect = document.getElementById('healthKind');
    if (healthKindSelect) {
        healthKindSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('healthKind', this.value);
            } else {
                url.searchParams.delete('healthKind');
            }

            // Очищаем остальные параметры, если нужно
            // (чтобы не было конфликтов)
            url.searchParams.delete('healthType');
            url.searchParams.delete('genderHealth');

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== Флора и фауна АВТОМАТИЧЕСКОЕ ПРИМЕНЕНИЕ ПРИ СМЕНЕ ТИПА ======
    const animalsKindSelect = document.getElementById('animalsKind');
    if (animalsKindSelect) {
        animalsKindSelect.addEventListener('change', function() {
            // Очищаем параметры
            const url = new URL(window.location.href);

            // Устанавливаем
            if (this.value) {
                url.searchParams.set('animalsKind', this.value);
            } else {
                url.searchParams.delete('animalsKind');
            }

            url.searchParams.delete('page');
            window.location.href = url.toString();
        });
    }

    // ====== ПРИНУДИТЕЛЬНОЕ ВОССТАНОВЛЕНИЕ ПОСЛЕ ЗАГРУЗКИ ======
    // Вызываем несколько раз с задержкой для гарантии
    setTimeout(() => {
        restoreFilterValues();
    }, 50);

    setTimeout(() => {
        restoreFilterValues();
    }, 200);

    setTimeout(() => {
        restoreFilterValues();
    }, 500);

});